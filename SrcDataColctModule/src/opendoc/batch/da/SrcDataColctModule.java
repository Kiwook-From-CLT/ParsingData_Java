package opendoc.batch.da;



import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
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
import opendoc.batch.util.DateTimeUtility;
import opendoc.batch.util.LogMgr;
import opendoc.batch.ftp.DatabaseFileTrnsmis;
//import opendoc.batch.da.SrcDataRecptnModuleMain;


/**
 *  title : 공통 데이터 수집 모듈
 *  Subject : 서울시 시스템별 DB 에서 정보를 읽어와 파일로 생성후 운영서버로 전송하는 기능
 *  
 *  <pre>
 *  <b>History:</b> 
 *     KiWook.K, 1.0, 2013/08/16  초기 작성
 *  </pre>
 *  
 * @author KiWook.K
 * @version 1.0, 2013/08/16 초기 작성
 * @see   None
 */
public class SrcDataColctModule
{
	private static Properties db_props   = null;
	private static Properties sql_props  = null;

	// DB 설정 정보를 저장할 변수 선언
	private static String jdbcdriver	 = "";  // jdbcDriver 변수	
	private static String DB_DRIVER      = ""; 	// DB 드라이버 클래스파일 경로
	private static String DB_CONNECTION  = ""; 	// DB 접속 경로
	private static String DB_USERNAME    = ""; 	// DB UserId
	private static String DB_PASSWORD    = ""; 	// DB User Password

	// SFTP 설정 정보를 저장할 변수 선언
	private static String host 		     = "";      // upLoad할 stfp의 IP    
	private static String userId 	     = "";	    // upLoad할 stfp의 계정 ID
	private static String password 	     = "";	    // upLoad할 stfp의 계정 Password
	private static String workDirectory  = ""; 	    // 접근할 폴더가 위치할 경로(문서정보)
	private static String fileName       = "";      // 파일 경로를 포함한 파일명  
	private static int port    		     = 0;		// upLoad할 stfp의 포트 번호   
	
	private static String copyAtchFileDirectory = "";
	private static HashMap<Integer,String> hm_colName = null;

	static ArrayList<String> dbArr = new ArrayList<String>();

	static int cnt = 0;

	private static String CONFIG_DB_XML_FILE_PATH_RESOURCE    = "conn_properties.xml";		// db config xml
	private static String CONFIG_QUERY_XML_FILE_PATH_RESOURCE = "batch_da_properties.xml"; // query config xml

	//private static String CONFIG_DB_XML_FILE_PATH    = "env/connection.properties";		// db config xml
	//private static String CONFIG_QUERY_XML_FILE_PATH = "env/query.properties";   // query config xml
	
	private static String CONFIG_DB_XML_FILE_PATH    = "/app11/tisms/Ksign/SrcDataColctModule/env/conn_properties.xml";      // db config xml
    private static String CONFIG_QUERY_XML_FILE_PATH = "/app11/tisms/Ksign/SrcDataColctModule/env/batch_da_properties.xml"; // query config xml
	//test
	//private static String propertiePath = "env/test.properties";
	//private static String sqlpropertiePath = "env/query.properties";
	
	private static ConvertParam convert = new ConvertParam();				      // 보안성 체크를 위한 클래스 변수 (추후 수정 필요) 
	private static CpyToTmp fileCopy = new CpyToTmp();                            // 파일 copy 를 위한 클래스 변수 
	private static CompFileMgr mkFile = new CompFileMgr(); 						  // copy 한 파일들을 하나의 파일로 압축하기 위한 클래스 변수 
	private static DatabaseFileTrnsmis fileUpLoad = new DatabaseFileTrnsmis();    // 압축파일을 SFTP 전송하기 위한 클래스 변수
	private static CommandMgr comm = new CommandMgr();     						  // Java runtime 명령어를 수행하기 위한 클래스 변수

	private static String chkFlag = "";				                              // 첨부파일의 업로드가 정상적으로 완료되었는지 체크하는 flag. complete 면 완료. 에러시 fail
	
	private static String date = DateTimeUtility.setOperationDate();              // 날짜를 받아오는 전역변수 (현재 날짜보다 하루 늦음)
	private static String Directory = "";
	
	private static String logFileDirectory ="";            // 로그 파일의 경로
    private static String logFileName ="";                // 로그 파일명
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
		//Reading properties file in Java example
		db_props = new Properties();
		sql_props = new Properties();

		//db_props.loadFromXML(getDbConfigInputStream());
		//sql_props.loadFromXML(getBatchConfigInputStream());
		db_props.loadFromXML(getDbConfigFileInputStream());		
		sql_props.loadFromXML(getBatchConfigFileInputStream());	
		

		//reading proeprty
		jdbcdriver = db_props.getProperty("jdbcdriver");      // Oracle / msSql / msSql     

		// properties 파일에서 DB 와 SFTP 설정 정보를 읽어온다.
		DB_DRIVER      = db_props.getProperty("jdbc."+jdbcdriver+".dbdriver");
		DB_CONNECTION  = db_props.getProperty("jdbc."+jdbcdriver+".dbconnection");
		DB_USERNAME    = db_props.getProperty("jdbc."+jdbcdriver+".dbusername");
		DB_PASSWORD    = db_props.getProperty("jdbc."+jdbcdriver+".dbpassword");
		host 		   = db_props.getProperty("sftp.host");				  		 
		port 		   = Integer.parseInt(db_props.getProperty("sftp.port"));      
		userId 		   = db_props.getProperty("sftp.userId");			   			 
		password 	   = db_props.getProperty("sftp.password");           		 
		workDirectory  = db_props.getProperty("sftp.workDirectory");
		copyAtchFileDirectory = db_props.getProperty("local.copy.directory");
		Directory = db_props.getProperty("directory");
		logFileDirectory = db_props.getProperty("logFileDirectory");
        logFileName = db_props.getProperty("logFileName");
        
        LogMgr.log("jdbc inform     : " + jdbcdriver);
		LogMgr.log("jdbc driver     : " + DB_DRIVER);
		LogMgr.log("jdbc connection : " + DB_CONNECTION);
		LogMgr.log("jdbc username   : " + DB_USERNAME);
		LogMgr.log("jdbc password   : " + DB_PASSWORD);

		boolean resExport = false;    // 데이터를 정상적으로 읽어오는지 체크한다.
		
		resExport = exportFile("SAM");   // DB에서 데이터를 읽어와서 SAM 파일 작성      

		if(resExport == false)             // 문서정보 DB 에서 select 된 데이터가 없으면 모듈 종료.
		{
			LogMgr.log("Process End.... No data selected....");
			System.exit(999);
		} 

		exportFile("FILEINFO");  // DB에서 첨부파일 정보를 읽어와서 SAM 파일 작성
		String resFlag = "";
		
		if(!(chkFlag.equals("") || chkFlag.equals(null)))
		{
			resFlag = convert.convertString(mkChkFile(chkFlag));		// 첨부파일이 제대로 생성되었는지 체크하는 Flag 파일 경로 생성
		}
		
		
		SFTPUpload(dbArr, cnt, resFlag);      // 생성된 파일을 SFTP로 업로드
		

		//SrcDataRecptnModuleMain.SrcDataRecptnModuleMain();       // 생성된 파일을 읽어 운영 Db Table에 Insert 작업하는 함수 호출
	}

	/**
	 * DB 구분자를 parameter 로 받아서 DB 별로 데이터를 읽어와 dat 파일로 생성한다 
	 * 
	 * @param flag
	 * 문서정보, 첨부파일정보 DB 의 구분자 ( SAM = 문서정보DB, FILEINFO = 첨부파일정보 DB )
	 */
	private static boolean exportFile(String flag)
	{  
		File file = null; 
		BufferedWriter bw = null;
		int intCopyVal = 0;               // file Copy returnValue
		ArrayList AtchFilePath = new ArrayList();
		

		// if ( 문서 DB 정보 )
		if(flag.equals("SAM"))
		{
			fileName = getFileName();
			dbArr.add(cnt, fileName);
			cnt++;
		}      
		// else ( 첨부파일 DB 정보 )
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

			// if ( 문서 DB 정보 )
			if(flag.equals("SAM"))
			{
				rslist = selectRecordData("SAM");
			}

			// else ( 첨부파일 DB 정보 )
			else
			{
				rslist = selectRecordData("FILEINFO");
			}

			if(rslist.isEmpty() == true)
			{
				return false;
			}

			//selectRecordData("SAM");
			String fileUpLoadPath = "";
			boolean isSpace = false;               // 파일명에 공백이 있는지 체크하는 Flag
			
			fileUpLoadPath = workDirectory + "/" + date;
			comm.CommandExecute("mkdir", copyAtchFileDirectory+date);                   // 일별로 date 폴더 생성한다.
			sleep(1000);  // 대기시간을 준다
			
			fileUpLoad.init(host, userId, password, port);

			file = new File(fileName);
			File filePath = null; 		// 첨부파일 경로         
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file),"UTF-8"));
			

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
						if(j == 5) 				// 첨부파일 원본을 임시폴더로 copy 하기 위한 경로생성
						{   
							String path = "";
							path=convert.convertString(hashdata.get(j))+convert.convertString(hashdata.get(3));
							//path = convert.convertString(hashdata.get(j))+convert.convertString(hashdata.get(2));            		   
							//path = convert.convertString(hashdata.get(3))+convert.convertString(hashdata.get(2))+"."+convert.convertString(hashdata.get(5));
							
							copyPath[1] = convert.convertString(hashdata.get(3));
							
							filePath = new File(path);
							
							
							copyPath[0] = path; // 해당 컬럼의 첨부파일 path를 배열에 담는다.            	   
						}						
					}
					sb.append((String)hashdata.get(j));  // DB 에서 읽어온 컬럼을 파일에 Write

					if(j!=hashdata.size()) 				// Write 되면 뒤에 구분자 생성
						sb.append(delimeter);
				}  

				LogMgr.log(flag);
				if(flag.equals("FILEINFO"))
				{
					AtchFilePath.add(copyPathcnt ,copyPath); // 원본파일 path와 임시파일 path를 arraylist에 담는다.
					copyPathcnt++;
				}            
			} 

			if(loopi>0) 
			{
				bw.write(sb.toString());
			}

			// 데이터파일 생성 이후에 첨부파일을 Sftp로 전송하는 작업을 수행한다.
			breakpoint:                              // copy 작업이 에러날 경우 다음 loop 문 exit
				if(flag.equals("FILEINFO"))
				{	
					for(int ai = 0 ; ai < AtchFilePath.size() ; ai++)
					{ 
						String[] resPath = (String[]) AtchFilePath.get(ai);

						isSpace = spaceCheck(resPath[0]);			// 원본 파일 path의 경로생성

						if(isSpace == true)
							intCopyVal += fileCopy.copyChannel(resPath[0], copyAtchFileDirectory+date+"/"+resPath[1]);
						else
							
							intCopyVal += fileCopy.copyToTmp(resPath[0], copyAtchFileDirectory+date+"/"+resPath[1]);

						if(intCopyVal != 0)
						{
							LogMgr.log("첨부파일 Copy 작업중 오류가 발생하였습니다. Copy 작업이 중지되었습니다.");        		 
							break breakpoint;
						}
					}         
				}

			if(flag.equals("FILEINFO") && rslist.size() != 0 && intCopyVal == 0)
			{        	 
				String zipFilePath = "";

				//zipFilePath = convert.convertString(mkFile.getZip(copyAtchFileDirectory+date+"/", date));
				zipFilePath = convert.convertString(mkFile.getZip(copyAtchFileDirectory, date));
				
				filePath = new File(zipFilePath);
				chkFlag = convert.convertString(fileUpLoad.attachedFileUpload(fileUpLoadPath, filePath));
				comm.CommandExecute("rm", zipFilePath);        	 
			}

			fileUpLoad.disconnection();    // 업로드 완료 되면 sftp 접속 종료

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

	/**
	 * 실제 DB 의 정보를 Select 하여 ArrayList로 리턴한다
	 * 
	 * @param flag 
	 *  문서정보, 첨부파일정보 DB 의 구분자 ( SAM = 문서정보DB, FILEINFO = 첨부파일정보 DB )
	 * @return
	 *  DB 정보를 읽어와서 ArrayList로 리턴
	 * @throws SQLException
	 *  SQL 구문에 오류가 있으면 Exception 처리
	 */
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
				selectSQL = sql_props.getProperty("selectDocInfo_Oracle");
			}
			else if(jdbcdriver.equals("msSql"))
			{
				selectSQL = sql_props.getProperty("selectDocInfo_msSql");
			}
			else if(jdbcdriver.equals("mySql"))
			{
				selectSQL = sql_props.getProperty("selectDocInfo_mySql");
			}
			//selectSQL = sql_props.getProperty("selectSQL");
			LogMgr.log("Doc Info sql : " + selectSQL);

		} 
		else
		{
			if(jdbcdriver.equals("Oracle"))    	  
			{
				selectSQL = sql_props.getProperty("selectFileInfo_Oracle");    		  
			}
			else if(jdbcdriver.equals("msSql"))
			{
				selectSQL = sql_props.getProperty("selectFileInfo_msSql");
			}
			else if(jdbcdriver.equals("mySql"))
			{
				selectSQL = sql_props.getProperty("selectFileInfo_mySql");
			}
			//selectSQL = sql_props.getProperty("selectFileTable");
			
			LogMgr.log("File Info sql : " + selectSQL);
		}	

		


		try 
		{
			dbcon = getDBConnection();

			pstat = dbcon.prepareStatement(selectSQL);
			//pstat.setInt(1, 1001);

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

	/**
	 * 데이터베이스 Connection 객체를 생성하여 리턴한다
	 * 
	 * @return
	 * database connection return
	 */

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



	/**
	 * 문서정보 DB 정보를 읽은 FileName을 사용자가 설정한 경로와 파일명을 설정하여 리턴한다.
	 * @return
	 * 사용자가 설정한 경로명 + /SAM + 작성일자 + .dat
	 * 
	 * ex)
	 * 	  C:/project/temp/SAM20130802.dat
	 */
	private static String getFileName()
	{
		String directory = Directory;		
		//String directory = "/home/opendoc/moduletest/datFile/";
		return directory+"SAM" + getDateString("yyyyMMdd") + ".dat";    
	}

	/**
	 * 첨부파일 DB 정보를 읽은 FileName을 사용자가 설정한 경로와 파일명을 설정하여 리턴한다.
	 * @return
	 * 사용자가 설정한 경로명 + /FILEINFO + 작성일자 + .dat
	 * 
	 * ex)
	 * 	  C:/project/temp/FILEINFO20130802.dat
	 */
	private static String getDataFileName()
	{
		String directory = Directory;
		//String directory = "/home/opendoc/moduletest/datFile/";
		return directory+"FILEINFO" + getDateString("yyyyMMdd") + ".dat";
	}

	/**
	 * 날짜 포맷을 설정한다
	 * 
	 * @param format
	 * 원하는 날짜 형식 포맷
	 * 
	 * @return
	 * 변경된 날짜 포맷을 리턴
	 * 
	 * ex)
	 *     getDateString("yyyyMMddHHmmss");         // 20100917201627
	 *     getDateString("yyyy-MM-dd HH:mm:ss");    // 2010-09-17 20:16:27
	 */

	public static String getDateString(String format) 
	{
		java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat(format, java.util.Locale.KOREA);
		return formatter.format(new java.util.Date());
	}


	/**
	 * properties에 지정된 경로로 sftp 파일 업로드
	 * 
	 * @param filePath
	 *  업로드할 파일의 경로
	 * @param flag
	 *  DB 정보인지 첨부파일 인지 구분자
	 *  (DB 정보 : dbData, 첨부파일 : fileInfo)
	 */
	public static void SFTPUpload(ArrayList<String> dbPath, int cnt, String resFlagPath)  throws FileNotFoundException, SftpException
	{
	    String logFilePath = logFileDirectory+logFileName+"_"+date+".log";        
	    File logFile = new File(logFilePath);
		String resultDbArr = ""; 
		String dbInfoUpLoadPath = "";
		File resFile = new File(convert.convertString(resFlagPath));      // 첨부파일 전송 여부를 체크하는 Flag 파일 생성
		String date = getDateString("yyyyMMdd");
		
		dbInfoUpLoadPath = convert.convertString(workDirectory + "/" + date);

		File[] dbInfoPath = new File[cnt]; 

		for(int i = 0 ; i < dbPath.size() ; i++){
			resultDbArr = dbPath.get(i);

			//dbInfoUpLoadPath[i] = workDirectory; 

			dbInfoPath[i] = new File(resultDbArr);
		} 

		DatabaseFileTrnsmis dbInfoUpLoad = new DatabaseFileTrnsmis();

		dbInfoUpLoad.init(host, userId, password, port);
		dbInfoUpLoad.dbInfoUpload(dbInfoUpLoadPath, dbInfoPath, resFile);
		LogMgr.log("DB 정보 파일과 첨부파일의 SFTP 전송이 완료되었습니다.  오류 사항은 상단에 있습니다.");
        dbInfoUpLoad.LogFileUpload(dbInfoUpLoadPath,logFile);
		
		try{
			if(!(resFlagPath.equals("") || resFlagPath.equals(null)))
			{
				comm.CommandExecute("rm", resFlagPath);   // 임시로 생성된 Flag 파일 삭제
			}			
		}
		catch (Exception e) { LogMgr.log("IO Exceprion === "+ e.getMessage()); }
		
		dbInfoUpLoad.disconnection();    // 업로드 완료 되면 sftp 접속 종료                  
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
	protected static FileInputStream getDbConfigFileInputStream() throws IOException {
		FileInputStream in = new FileInputStream(CONFIG_DB_XML_FILE_PATH);
		//FileInputStream in = new FileInputStream(propertiePath);
		if( in!=null && in.available()>0)
		{
			return in;
		} else{
			return null;
		}
	}
	protected static FileInputStream getBatchConfigFileInputStream() throws IOException {
		FileInputStream in = new FileInputStream(CONFIG_QUERY_XML_FILE_PATH);
		//FileInputStream in = new FileInputStream(sqlpropertiePath);
		if( in!=null && in.available()>0)
		{
			return in;
		} else{
			return null;
		}
	}

	/**
	 * Thread 대기시간을 준다.
	 * @param time
	 */
	public static void sleep(int time){
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) { }
	}	
	
	
	/**
	 * 원본 파일의 fullPath를 받아와서 공백의 여부를 체크한다.
	 * @param fullPath
	 * @return
	 */
	public static boolean spaceCheck(String fullPath)
	{
	    for(int i = 0 ; i < fullPath.length() ; i++)
	    {
	        if(fullPath.charAt(i) == ' ')
	            return true;
	    }
	    return false;
	}
	
	
	/**
	 * 첨부파일 전송 결과에 따라 내용없는 파일을 생성한다. 
	 * complete.txt 면 첨부파일 전송 완료.
	 * fail.txt 면 첨부파일 전송 에러. 
	 * 
	 * @return 운영서버에서 체크될 파일의 full-path
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static String mkChkFile(String resultFlag) throws FileNotFoundException, IOException
	{
		String directory = "";
		String flagFilePath = "";
		//Directory = convert.convertString(db_props.getProperty("directory"));
		directory = Directory;
		flagFilePath = directory+resultFlag+".txt";
		File resFlag = new File(flagFilePath);
		
		//LogMgr.log(":::::::::::::::::::::::::::::::::: 플래그 파일명 "+flagFilePath);
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
}
