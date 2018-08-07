package opendoc.batch.da;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.File;
import java.io.BufferedWriter;

import com.jcraft.jsch.SftpException;

import oracle.sql.CLOB;
import oracle.sql.ConverterArchive;


import opendoc.batch.secure.ConvertParam;
import opendoc.batch.util.CommandMgr;
import opendoc.batch.util.CompFileMgr;
import opendoc.batch.util.CpyToTmp;
import opendoc.batch.util.LogMgr;
import opendoc.batch.ftp.DatabaseFileTrnsmis;

/**
 *  title : Data collecting module 
 *  Subject : Collecting raw data from clients' database server, making them as a file, and transmitting the data file and attachment files to the our server .
 *
 *  <pre>
 *  <b>History:</b> 
 *	   Kiwook.K, 1.1, 2015/01/21  Updated
 *     Kiwook.K, 1.0, 2013/08/16  Initial version
 *  </pre>
 *  
 * @author KiWook.K
 * @version 1.1, 2015/01/21  Updated
 * @see   None
 */
public class SrcDataColctModuleParam
{
	private static Properties db_props   = null;
	private static Properties sql_props  = null;

	// Variables for DB configuration   
	private static String DB_DRIVER      = ""; 	// DB driver class file path
	private static String DB_CONNECTION  = ""; 	// DB connection info
	private static String DB_USERNAME    = ""; 	// DB UserId
	private static String DB_PASSWORD    = ""; 	// DB User Password

	// Variables for SFTP configuration
	private static String host 		     = "";    		 // IP address to upload file     
	private static String userId 	     = "";			 // ID of SFTP
	private static String password 	     = "";			 // Password of the ID
	private static String workDirectory  = ""; 			 // File path to upload file	
	private static String fileName       = "";       	 // Full file path with data file name  
	private static int port    		     = 0;		     // Port number of SFTP   		
	private static String jdbcdriver	        = "";    // jdbcDriver 
	private static String copyAtchFileDirectory = "";	 // Attachment files' path
	private static HashMap<Integer,String> hm_colName = null;

	static ArrayList<String> dbArr = new ArrayList<String>();

	static int cnt = 0;

	private static String CONFIG_DB_XML_FILE_PATH_RESOURCE    = "conn_properties.xml";		// db config xml
	private static String CONFIG_QUERY_XML_FILE_PATH_RESOURCE = "batch_da_properties.xml";  // query config xml

	
	private static ConvertParam convert = new ConvertParam();				      // 보안성 체크를 위한 클래스 변수 (추후 수정 필요) 
	private static CpyToTmp fileCopy = new CpyToTmp();                             // 파일 copy 를 위한 클래스 변수 
	private static CompFileMgr mkFile = new CompFileMgr(); 						  // copy 한 파일들을 하나의 파일로 압축하기 위한 클래스 변수 
	private static DatabaseFileTrnsmis fileUpLoad = new DatabaseFileTrnsmis();     // 압축파일을 SFTP 전송하기 위한 클래스 변수
	private static CommandMgr comm = new CommandMgr();     						  // Java runtime 명령어를 수행하기 위한 클래스 변수

	private static String paramDate = "";                             			  // 폴더 생성 날짜	
	private static boolean chkParam = false; 								      // parameter 를 입력 받았는지 체크하는 flag.
	
	private static String chkFlag = "";				// 첨부파일의 업로드가 정상적으로 완료되었는지 체크하는 flag. complete 면 완료. 에러시 fail
	
	/** 
	 * DB의 View(문서정보, 첨부파일)를 읽어 파일로 생성 후 지정된 서버로 sftp 방식으로 전송한다.
	 * @param xmlPath
	 *  사용자가 properties 파일을 저장한 절대 경로
	 * @throws FileNotFoundException   
	 *  File Exception 시 처리 된다.    
	 * @throws IOException
	 *  I/O Exception 발생시 처리 된다.
	 */
	public static void main(String args[]) throws FileNotFoundException, IOException, NullPointerException, InvalidPropertiesFormatException, SftpException
	{
		if(args.length != 0)
		{	
			paramDate = args[0];     // 폴더 생성 날짜	
			chkParam  = true;        // flag 값을 true로 변경
		}
		else
		{
			paramDate = "";  // 쿼리 날짜		   		
		}

		//Reading properties file in Java example
		db_props = new Properties();
		sql_props = new Properties();

		db_props.loadFromXML(getDbConfigInputStream());		 
		sql_props.loadFromXML(getBatchConfigInputStream());
		

		//reading proeprty
		jdbcdriver = db_props.getProperty("jdbcdriver");      // Oracle / msSql / msSql     

		// reading information of database and SFTP from properties file
		DB_DRIVER      		  = db_props.getProperty("jdbc."+jdbcdriver+".dbdriver");
		DB_CONNECTION  		  = db_props.getProperty("jdbc."+jdbcdriver+".dbconnection");
		DB_USERNAME    		  = db_props.getProperty("jdbc."+jdbcdriver+".dbusername");
		DB_PASSWORD    		  = db_props.getProperty("jdbc."+jdbcdriver+".dbpassword");
		host 		   		  = db_props.getProperty("sftp.host");				  		 
		port 		          = Integer.parseInt(db_props.getProperty("sftp.port"));      
		userId 		          = db_props.getProperty("sftp.userId");			   			 
		password 	          = db_props.getProperty("sftp.password");           		 
		workDirectory         = db_props.getProperty("sftp.workDirectory");
		copyAtchFileDirectory = db_props.getProperty("local.copy.directory");

		LogMgr.log("jdbc inform     : " + jdbcdriver);
		LogMgr.log("jdbc driver     : " + DB_DRIVER);
		LogMgr.log("jdbc connection : " + DB_CONNECTION);
		LogMgr.log("jdbc username   : " + DB_USERNAME);
		LogMgr.log("jdbc password   : " + DB_PASSWORD);

		boolean resExport = false;    // Boolean for result of data export
		
		resExport = exportFile("SAM");   // Call a functon that creats data file for storing exported data (If it is done without exception returmn true)     

		if(resExport == false)           // If there is no data, program will be terminated
		{
			LogMgr.log("Process End.... No data selected....");
			System.exit(999);
		} 

		exportFile("FILEINFO");  // Making data file using data from database
		
		String resFlag = "";		
		
		if(!(chkFlag.equals("") || chkFlag.equals(null)))
		{
			resFlag = convert.convertString(mkChkFile(chkFlag));		
		}
		SFTPUpload(dbArr, cnt, resFlag);      
	}

	/**
	 * There are two types of information, Document information and Attachment information. Document information has title, contents, date of creation, and etc 
	 * Attachment information has files those are included in a document such as images, word file, things like that 
	 * 
	 * @param flag
	 * SAM = Document info DB, FILEINFO = Attachment info DB
	 */
	private static boolean exportFile(String flag)
	{  
		File file = null; 
		BufferedWriter bw = null;
		int intCopyVal = 0;               // file Copy returnValue
		ArrayList AtchFilePath = new ArrayList();

		
		if(flag.equals("SAM"))
		{
			fileName = getFileName();
			dbArr.add(cnt, fileName);
			cnt++;
		}   

		else
		{
			fileName = getDataFileName();
			dbArr.add(cnt, fileName);
			cnt++;
		}

		long startTime = System.currentTimeMillis();

		try
		{ 
			ArrayList<Object> rslist = null;

			if(flag.equals("SAM"))
			{
				rslist = selectRecordData("SAM");
			}

			else
			{
				rslist = selectRecordData("FILEINFO");
			}

			if(rslist.isEmpty() == true)
			{
				return false;
			}

			boolean isSpace = false;               
			String fileUpLoadPath = "";
			fileUpLoadPath = workDirectory + "/" + paramDate;                       // Creating upload file path with current date 
			comm.CommandExecute("mkdir", copyAtchFileDirectory+paramDate);          // Creating temp file folder. Physical files in the servers are not allowed directly copy from original path to the other server due to security issue
			sleep(1000);       		

			fileUpLoad.init(host, userId, password, port);

			file = new File(fileName);
			File filePath = null; 		         
			bw = new BufferedWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(file),"UTF-8"));
			

			StringBuffer sb = new StringBuffer();
			int loopi=0;         
			long sgdmseq=0l;
			int copyPathcnt = 0;
			String delimeter = "/tag/";        

			ArrayList resArr = new ArrayList();


			for(int i=0;i<rslist.size();i++) 
			{
				String[] copyPath = new String[2];

				if( loopi == 10000 ) 
				{
					loopi=0;
					sb.append("\n");
					bw.write(sb.toString());
					sb.setLength(0);
				}

				++loopi;
				++sgdmseq;

				HashMap<Integer,String> hashdata = (HashMap<Integer,String>)rslist.get(i);

				if(loopi>1) 
					sb.append("\n");

				for(int j=1;j<=hashdata.size();j++) 
				{  
					if(flag.equals("FILEINFO"))
					{
						if(flag.equals("FILEINFO"))
						{
							if(j == 5) 				
							{   
								String path = "";
								path=convert.convertString(hashdata.get(j))+convert.convertString(hashdata.get(3));
								
								copyPath[1] = convert.convertString(hashdata.get(3));
								
								filePath = new File(path);
								
								
								copyPath[0] = path;            	   
							}						
						}
					}
					sb.append((String)hashdata.get(j));  

					if(j!=hashdata.size()) 				
						sb.append(delimeter);
				} // end of for  

				if(flag.equals("FILEINFO"))
				{
					AtchFilePath.add(copyPathcnt ,copyPath);
					copyPathcnt++;
				} 
			} 

			if(loopi>0) 
			{
				bw.write(sb.toString());
			}

			breakpoint:                              
				if(flag.equals("FILEINFO"))
				{
					for(int ai = 0 ; ai < AtchFilePath.size() ; ai++)
					{ 
						String[] resPath = (String[]) AtchFilePath.get(ai);
						
						isSpace = spaceCheck(resPath[0]);			

						if(isSpace == true)
							intCopyVal += fileCopy.copyChannel(resPath[0], copyAtchFileDirectory+paramDate+"/"+resPath[1]);
						else
							intCopyVal += fileCopy.copyToTmp(resPath[0], copyAtchFileDirectory+paramDate+"/"+resPath[1]);
						
						if(intCopyVal != 0)
						{
							LogMgr.log("File Copy Error.");        		 
							break breakpoint;
						}
					}         
				}
			if(flag.equals("FILEINFO") && rslist.size() != 0 && intCopyVal == 0)
			{        	 
				String zipFilePath = "";
				
				zipFilePath = convert.convertString(mkFile.getZip(copyAtchFileDirectory, paramDate));
				
				filePath = new File(zipFilePath);
				chkFlag = fileUpLoad.attachedFileUpload(fileUpLoadPath, filePath);
				comm.CommandExecute("rm", zipFilePath);        	 
			}

			fileUpLoad.disconnection();    

			long endTime = System.currentTimeMillis();
			long elapsedTime = (endTime - startTime)/1000;//in seconds

			if(flag.equals("SAM"))
			{
				LogMgr.log("Export sam file total record count : " + sgdmseq);
				LogMgr.log("Export sam file total elapsed time.(second) : " + elapsedTime);
			}
			else
			{
				LogMgr.log("Export attachedFiles total record count : " + sgdmseq);
				LogMgr.log("Export attachedFiles total elapsed time.(second) : " + elapsedTime);
			} 
		} // end of try
		catch(SQLException sqle) { LogMgr.log(sqle.getMessage()); return false; } 
		catch(IOException ioe) { LogMgr.log(ioe.getMessage()); return false; } 
		catch(Exception e) { LogMgr.log(e.getMessage());  return false; }
		finally
		{
			try
			{
				bw.close();   				
				return true;
			}
			catch(Exception ex){}
		}
		return true;
	}

	
	private static ArrayList<Object> selectRecordData(String flag)  throws SQLException 
	{
		Connection dbcon = null;
		PreparedStatement pstat = null;
		ArrayList<Object> recordData = new ArrayList<Object>();

		hm_colName = new HashMap<Integer,String>();
		hm_colName.clear();

		String selectSQL = "";

		if(flag.equals("SAM"))
		{
			if(jdbcdriver.equals("Oracle"))    	  
			{
				selectSQL = sql_props.getProperty("selectDocInfoParam_Oracle");   	
			}
			else if(jdbcdriver.equals("msSql"))
			{
				selectSQL = sql_props.getProperty("selectDocInfo_msSql");
			}
			else if(jdbcdriver.equals("mySql"))
			{
				selectSQL = sql_props.getProperty("selectDocInfo_mySql");
			}

		} 
		else
		{
			if(jdbcdriver.equals("Oracle"))    	  
			{
				selectSQL = sql_props.getProperty("selectFileInfoParam_Oracle");    		  
			}
			else if(jdbcdriver.equals("msSql"))
			{
				selectSQL = sql_props.getProperty("selectFileInfo_msSql");
			}
			else if(jdbcdriver.equals("mySql"))
			{
				selectSQL = sql_props.getProperty("selectFileInfo_mySql");
			}

		}	

		LogMgr.log("sql : " + selectSQL);


		try 
		{
			dbcon = getDBConnection();

			pstat = dbcon.prepareStatement(selectSQL);
			String startDate = paramDate+"000000";
			String endDate = paramDate+"235959";
			
			pstat.setString(1, startDate);  // Start (ex: 20130826000000)
			pstat.setString(2, endDate);    // End   (ex: 20130826235959)

			// execute select SQL stetement
			ResultSet rs = pstat.executeQuery();

			ResultSetMetaData rsmd = rs.getMetaData();
			int columncount = rsmd.getColumnCount();

			while (rs.next())
			{
				HashMap<Integer,String> Recordlist = new HashMap<Integer,String>();
				Recordlist.clear();

				for(int i=1;i<=columncount;i++) 
				{
					if(hm_colName.size() < columncount) 
					{
						hm_colName.put(i,rsmd.getColumnName(i));
					}
					Recordlist.put(i,rs.getString(i));
				}
				recordData.add(Recordlist);
			}
		} 
		catch (SQLException e) 
		{
			LogMgr.log(e.getMessage());
		} 
		finally 
		{
			try 
			{
				if (pstat != null) 
					pstat.close();
				if (dbcon != null) 
					dbcon.close();
			} catch(Exception ex) {}
		}
		return recordData;
	}

	
	private static Connection getDBConnection() 
	{
		Connection dbConnection = null;

		try 
		{
			Class.forName(DB_DRIVER);
		} 
		catch (ClassNotFoundException e) 
		{
			LogMgr.log("[database driver loading error]" + e.getMessage());
		}


		try 
		{
			dbConnection = DriverManager.getConnection(DB_CONNECTION, DB_USERNAME ,DB_PASSWORD);
		} 
		catch (SQLException e) 
		{
			LogMgr.log("[database connection error] : " + e.getMessage());
		}
		return dbConnection;
	}

	private static String getFileName()
	{
		String directory = db_props.getProperty("directory");
		return directory+"/SAM" +paramDate + ".dat";    
	}

	private static String getDataFileName()
	{
		String directory = db_props.getProperty("directory");
		
		return directory+"/FILEINFO" + paramDate + ".dat";
	}

	public static void SFTPUpload(ArrayList<String> dbPath, int cnt, String resFlagPath)  throws FileNotFoundException, SftpException
	{
		String resultDbArr = ""; 
		String dbInfoUpLoadPath = "";
		File resFile = new File(convert.convertString(resFlagPath));     
		

		dbInfoUpLoadPath =  workDirectory + "/" + paramDate;


		File[] dbInfoPath = new File[cnt]; 

		for(int i = 0 ; i < dbPath.size() ; i++)
		{
			resultDbArr = dbPath.get(i);
			dbInfoPath[i] = new File(resultDbArr);
		} 

		DatabaseFileTrnsmis dbInfoUpLoad = new DatabaseFileTrnsmis();

		dbInfoUpLoad.init(host, userId, password, port);
		dbInfoUpLoad.dbInfoUpload(dbInfoUpLoadPath, dbInfoPath, resFile);
		
		try
		{
			if(!(resFlagPath.equals("") || resFlagPath.equals(null)))
			{
				comm.CommandExecute("rm", resFlagPath);   
			}			
		}
		catch (Exception e) { LogMgr.log("IO Exceprion === "+ e.getMessage()); }
		
		dbInfoUpLoad.disconnection();                      
	}

	protected static InputStream getDbConfigInputStream() throws IOException {
		InputStream in = SrcDataColctModule.class.getResourceAsStream(CONFIG_DB_XML_FILE_PATH_RESOURCE);

		if( in!=null && in.available()>0)
		{
			return in;
		} else{
			return null;
		}
	}
	protected static InputStream getBatchConfigInputStream() throws IOException {
		InputStream in = SrcDataColctModule.class.getResourceAsStream(CONFIG_QUERY_XML_FILE_PATH_RESOURCE);
		if( in!=null && in.available()>0)
		{
			return in;
		} else{
			return null;
		}
	}
	

	public static void sleep(int time){
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) { }
	}
	
	
	/**
	 * Creating empty file that shows result of transmitting attachment files. This file will be deleted during executing recptn module  
	 * complete.txt: job done without error 
	 * fail.txt: job doesn't complete with error
	 */
	public static String mkChkFile(String resultFlag) throws FileNotFoundException, IOException
	{
		String Directory = "";
		String flagFilePath = "";
		Directory = convert.convertString(db_props.getProperty("directory"));
		flagFilePath = Directory+resultFlag+".txt";
		File resFlag = new File(flagFilePath);
		
		try
		{
			BufferedWriter bw = new BufferedWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(resFlag),"UTF-8"));
			bw.write("");
			bw.close();
		}
		catch (FileNotFoundException fe) { LogMgr.log(fe.getMessage()); }
		catch (IOException ioe) { LogMgr.log(ioe.getMessage()); }		
		
		return flagFilePath;
	}
	
	public static boolean spaceCheck(String fullPath)
	{
	    for(int i = 0 ; i < fullPath.length() ; i++)
	    {
	        if(fullPath.charAt(i) == ' ')
	            return true;
	    }
	    return false;
	}
}
