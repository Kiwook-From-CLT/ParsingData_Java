package opendoc.batch.da;

import java.io.File;
import java.util.Properties;
import java.io.CharArrayReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.UnsupportedEncodingException;
import java.sql.*;

import com.jcraft.jsch.SftpException;

import oracle.sql.CLOB;

import opendoc.batch.ftp.DatabaseFileTrnsmis;
import opendoc.batch.secure.ConvertParam;
import opendoc.batch.util.CommandMgr;
import opendoc.batch.util.CompFileMgr;
import opendoc.batch.util.CpyToTmp;
import opendoc.batch.util.CpyToWin;
import opendoc.batch.util.DateTimeUtility;
import opendoc.batch.util.LogMgr;
/**
 *  FTP로 전송된 각 시스템별 문서정보, 첨부파일 정보 DB 데이터 파일을 운영서버 DB로 Insert 작업 수행.  
 *  <pre>
 *  Table : TN_DA_DOC_INFO, TN_DA_ATCH_DOC_INFO
 *  </pre>
 *
 *  <pre>
 *  <b>History:</b> 
 *     KiWook.K, 1.0, 2013/08/18  초기 작성
 *  </pre>
 *  
 * @author KiWook.K
 * @version 1.0, 2013/08/18 초기 작성
 * @see   None
 */
public class SrcDataRecptnModuleParam
{	
	private static Properties db_props  = null;
	private static Properties sql_props = null;

	// DB 에 접속하기 위한 정보를 가져오기 위한 변수 선언
	private static String DB_DRIVER     = "";   
	private static String DB_CONNECTION = "";
	private static String DB_USERNAME   = "";
	private static String DB_PASSWORD   = "";
	private static String jdbcdriver    = "";

	// SFTP 설정 정보를 저장할 변수 선언
	private static String host 		       = "";      // upLoad할 stfp의 IP    
	private static String userId 	       = "";	  // upLoad할 stfp의 계정 ID
	private static String password 	       = "";	  // upLoad할 stfp의 계정 Password		
	private static String fileName         = "";      // 파일 경로를 포함한 파일명
	private static String localDaDataDirectory  = ""; 	  // Local DADATA 폴더 경로
	private static String daDataDirectory  = ""; 	  // DADATA 폴더 경로
	private static String srcDataDirectory  = ""; 	  // NT 로 보낼 원본 파일 경로
	
	// DB insert 작업을 수행할 Statement 객체 선언
	private static Statement ST = null;

	private static String CONFIG_DB_XML_FILE_PATH_RESOURCE    = "conn_properties.xml";	   // db config xml
	private static String CONFIG_QUERY_XML_FILE_PATH_RESOURCE = "batch_da_properties.xml"; // query config xml

	//private static String CONFIG_DB_XML_FILE_PATH    = "env/conn_properties.xml";		// db config xml
	//private static String CONFIG_QUERY_XML_FILE_PATH = "env/batch_da_properties.xml";   // query config xml

	private static String CONFIG_DB_XML_FILE_PATH    = "env/connection.properties";
	private static String CONFIG_QUERY_XML_FILE_PATH = "env/query.properties";
	
	private static ConvertParam convert = new ConvertParam();				      // 보안성 체크를 위한 클래스 변수 (추후 수정 필요) 
	private static CpyToTmp fileCopy = new CpyToTmp();                            // 파일 copy 를 위한 클래스 변수 
	private static CompFileMgr mkFile = new CompFileMgr(); 						  // copy 한 파일들을 하나의 파일로 압축하기 위한 클래스 변수 
	private static CommandMgr comm = new CommandMgr();     						  // Java runtime 명령어를 수행하기
	private static CpyToWin winCopy = new CpyToWin();     						  // NT 서버로 Copy 하기 위한 클래스 변수

	private static String docInfoFilePath = "";       // 문서정보 SAM 파일명이 포함된 Path
	private static String atchInfoFilePath = "";      // 첨부파일정보 SAM 파일명이 포함된 Path
	private static String compFilePath = "";			// 첨부파일을 압축한 파일명이 포함된 Path	     
	private static String CLOBfilepath  = "";        // Clob 정보 저장하는 파일명이 포함된 Path 
	
	private static String CompFileName = "";    // 첨부파일을 압축한 파일명
	private static String winFilePath = "";     // NT 서버에 저장되어 있는 파일경로
	private static int addCnt = 0;				// 로그정보 남기기 위한 변수
	private static String date = "";               // 날짜를 받아오는 전역변수 (현재 날짜보다 하루 늦음)

	private static String CLOB_query    = "";	
	private static boolean CLobFlag 	= false;                                    // ClobFile의 존재여부 체크 변수(기본값 N, CLOB 파일 존재하면 Y)  
	private static String systemId = "";
	private static String LogInfo = "";
	/**
	 * 로컬의 파일을 읽어와서 DB에 Insert 하는 작업.
	 * @param xmlPath
	 *  사용자가 properties 파일을 저장한 절대 경로
	 * @throws FileNotFoundException   
	 *  File Exception 시 처리 된다.    
	 * @throws IOException
	 *  I/O Exception 발생시 처리 된다.
	 */
	public static void main(String args[]) throws FileNotFoundException, IOException, SftpException, Exception
	{	
		//Reading properties file in Java example
		db_props = new Properties();
		sql_props = new Properties();

		date = args[0];
		
		//db_props.loadFromXML(getDbConfigFileInputStream());		 // dos 모드에서는 사용할 경우(jar 파일을 이용)
		//sql_props.loadFromXML(getBatchConfigFileInputStream());
		
		db_props.load(getDbConfigFileInputStream());		
		sql_props.load(getBatchConfigFileInputStream());	

		String directory = "";
		directory = convert.convertString(db_props.getProperty("datFileDirectory"));    // DB를 읽어온 파일의 경로

		//jdbc driver 값을 읽어서 해당 jdbc의 접속 정보를 설정한다.
		jdbcdriver     = db_props.getProperty("jdbcdriver");      // Oracle / msSql / msSql
		DB_DRIVER      = db_props.getProperty("jdbc."+jdbcdriver+".dbdriver");
		DB_CONNECTION  = db_props.getProperty("jdbc."+jdbcdriver+".dbconnection");
		DB_USERNAME    = db_props.getProperty("jdbc."+jdbcdriver+".dbusername");
		DB_PASSWORD    = db_props.getProperty("jdbc."+jdbcdriver+".dbpassword");

		host 		   = db_props.getProperty("sftp.host");
		userId 		   = db_props.getProperty("sftp.userId");			   			 
		password 	   = db_props.getProperty("sftp.password");
		srcDataDirectory = db_props.getProperty("srcDirectory");
		daDataDirectory = db_props.getProperty("desDirectory");
		winFilePath		= db_props.getProperty("sftp.winFilePath");
		localDaDataDirectory = db_props.getProperty("localCpyDirectory");
		
		CLOB_query      = sql_props.getProperty("selectClob");
		systemId        = db_props.getProperty("systemId");

		
		LogMgr.log("jdbc inform        : " + jdbcdriver);
		LogMgr.log("jdbc driver         : " + DB_DRIVER);
		LogMgr.log("jdbc connection   : " + DB_CONNECTION);
		LogMgr.log("jdbc username    : " + DB_USERNAME);
		LogMgr.log("jdbc password    : " + DB_PASSWORD);

		LogMgr.log("directory inform : " + directory);

		File folder = null;
		String[] sysId = systemId.split(",");	    // 시스템 ID별로 배열생성
		
		String destinationFilePath = "";            // dadata 폴더로 복사할 패스
		String destinationFileFullPath ="";         // dadata 폴더로 복사할 파일명이 포함된 fullPath
		int filecnt = 0;
		int result = 0;         // 문서정보와 첨부파일정보 SAM 파일이 운영DB에 정상적으로 insert 되었는지 체크하는 변수
		
		for(int i = 0 ; i < sysId.length ; i++)     // 등록된 시스템 갯수만큼 반복문 수행
		{
			folder = new File(directory+sysId[i]);
			//daDataDirectory = daDataDirectory+sysId[i];
			filecnt = listFilesForFolder(folder);			
			destinationFilePath = convert.convertString(localDaDataDirectory+sysId[i]);
			comm.CommandExecute("mkdir", destinationFilePath);
			
			destinationFileFullPath = localDaDataDirectory+sysId[i]+"/"+CompFileName;		
			fileCopy.copyToTmp(compFilePath, destinationFileFullPath);
			
			
			String srcFilePath = srcDataDirectory+sysId[i]+"/"+date;
			String desFilePath = daDataDirectory+sysId[i]+"/"+date;
			winCopy.copyToWin(srcFilePath, desFilePath, host, userId, password);
			
			String unZipFlag = "";
			unZipFlag = convert.convertString(mkFile.getUnZip(destinationFilePath, date));
			
			
			if(unZipFlag.equals("true"))
			{	
				if(!(docInfoFilePath.equals("") || docInfoFilePath.equals(null)))
				{
					result += insertSamFileData(docInfoFilePath,"SAM", sysId[i]);
					filecnt++;
				}
				
				if(!(atchInfoFilePath.equals("") || atchInfoFilePath.equals(null)))
				{
					result += insertSamFileData(atchInfoFilePath,"FILEINFO", sysId[i]);
					filecnt++;
				}
				
				insertLogData(sysId[i], result);
			}
			
			LogMgr.log(sysId[i]+" == Load Files Total Count : " + Integer.toString(filecnt));
			
		}		
	}

	/**
	 * 파일 경로를 넘겨 받아 파일인지, 디렉토리인지 여부를 구분하여 처리 하는 Method
	 * @param folder
	 * 설정 파일에서 읽어온 파일 경로
	 * @return
	 * 모든 로직이 수행된 후 읽어온 파일의 갯수를 int 형으로 return
	 * @throws FileNotFoundException
	 * 파일이 없을 경우 발생하는 Exception 처리
	 * @throws UnsupportedEncodingException
	 * Encoding 오류날 경우 발생하는 Exception 처리(Default : UTF-8). 
	 */
	public static int listFilesForFolder(final File folder) throws FileNotFoundException, UnsupportedEncodingException
	{
		int ifilecnt = 0;
		for (final File fileEntry : folder.listFiles()) 
		{
			if (fileEntry.isDirectory())
			{
				//LogMgr.log("path ==== "+fileEntry.getPath());
				listFilesForFolder(fileEntry);
			} 
			else
			{
				if (fileEntry.isFile()) 
				{
					String temp = fileEntry.getName();               
					
					// if ( 첨부파일 압축경로 )
						if((temp.substring(temp.lastIndexOf('.') + 1, temp.length()).toLowerCase()).equals("jar"))
						{	
							if((temp.substring(0,8).equals(date)))
							{								
								compFilePath = folder.getAbsolutePath() + "/" + fileEntry.getName();	
								CompFileName = fileEntry.getName();
								LogMgr.log("CompAttachFile Path == " + compFilePath);
							}
						}
						// if ( CLOB 데이터 )
						else if((temp.substring(0,4).equals("CLOB")) && (temp.substring(temp.lastIndexOf('.') + 1, temp.length()).toLowerCase()).equals("dat"))
						{
							CLOBfilepath = folder.getAbsolutePath() + "/" + fileEntry.getName();
							CLobFlag = true;
							LogMgr.log("CLOB File == " + CLOBfilepath);                   
							ifilecnt++;
						}
						// if( 문서 DB 정보 )
						else if ((temp.substring(0,3).equals("SAM")) && (temp.substring(temp.lastIndexOf('.') + 1, temp.length()).toLowerCase()).equals("dat")) 
						{	
							if((temp.substring(3,11).equals(date)))
							{	
								docInfoFilePath = folder.getAbsolutePath() + "/" + fileEntry.getName();            	              	            	  
								ifilecnt++;
								LogMgr.log("Loaded DocInfo File == " + docInfoFilePath);
							}
						}
						// if ( 첨부파일 DB 정보 )
						else if((temp.substring(0,8).equals("FILEINFO")) && (temp.substring(temp.lastIndexOf('.') + 1, temp.length()).toLowerCase()).equals("dat"))
						{	
							if((temp.substring(8,16).equals(date)))
							{							
								atchInfoFilePath = folder.getAbsolutePath() + "/" + fileEntry.getName();            	              	            	  
								ifilecnt++;
								LogMgr.log("Loaded AttachInfo File == " + atchInfoFilePath);
							}
						}	
														
				}
			}
		}
		return ifilecnt;
	}// end of listFilesForFolder(final File folder)


	/**
	 * listFilesForFolder() 내부에서 호출 되는 Method
	 * filepath에 존재하는 파일을 읽어 DB Insert 작업 수행
	 * 
	 * @param filepath
	 * 실제 파일 경로
	 */
	// db insert sam file
	public static int insertSamFileData(String filepath, String flag, String sysId)
	{
		Connection dbcon = null;
		PreparedStatement pstmt = null;
		int resInsert = 0;
		
		long startTime = 0l;

		// 문서DB 정보인지 첨부파일 DB 정보인지 구분해서 쿼리문 입력
		String insertSQL = "";

		// if ( 문서 DB 정보 )
		if(flag.equals("SAM"))
		{
			if(jdbcdriver.equals("Oracle"))
			{
				insertSQL = sql_props.getProperty("insertDocInfo_Oracle");				
			}
			else if(jdbcdriver.equals("msSql"))
			{
				insertSQL = sql_props.getProperty("insertDocInfo_msSql");
			}
			else if(jdbcdriver.equals("mySql"))
			{
				insertSQL = sql_props.getProperty("insertDocInfo_mySql");
			}
		}

		// else ( 첨부파일 DB 정보 )
		else
		{
			if(jdbcdriver.equals("Oracle"))    	  
			{
				insertSQL = sql_props.getProperty("insertFileInfo_Oracle");    		  
			}
			else if(jdbcdriver.equals("msSql"))
			{
				insertSQL = sql_props.getProperty("insertFileInfo_msSql");
			}
			else if(jdbcdriver.equals("mySql"))
			{
				insertSQL = sql_props.getProperty("insertFileInfo_mySql");
			}
		}

		// db 컬럼 정의(flag 값을 참조해서 문서DB 인지 첨부파일 DB인지 구분)
		String  sCOL_NAME = "";

		// if ( 문서 DB 정보 )
		if(flag.equals("SAM"))
		{
			sCOL_NAME = sql_props.getProperty("insertDocInfoColumnName");
		}

		// else ( 첨부파일 DB 정보 )
		else
		{
			sCOL_NAME = sql_props.getProperty("insertFileInfoColumnName");
		}

		String[] sFIELD_NAME = sCOL_NAME.split(",");

		LogMgr.log("FIELD_NAME length : " + sFIELD_NAME.length);
		
		try 
		{	
			dbcon = getDBConnection();

			dbcon.setAutoCommit(false); // auto Commit을 막는다. 성능향상과 추후 update Statement를 수행하기 위함.

			//query for inserting batch data
			pstmt = dbcon.prepareStatement(insertSQL);

			int batchSize = 10000;

			startTime = System.currentTimeMillis();
			String path = "";
			path = convert.convertString(filepath);
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path),"UTF-8"));

			long icount = 0l;
			String delimeter = "/tag/";         

			int[] index = new int[batchSize];
			String[] tmpStr = new String[batchSize];
			int result = 1;
			int size = 0;
			int tmpIndex = 0;
			int logCnt = 0;
			String doc_id = "";
			
			ST = dbcon.createStatement();
			
			String batchDate = "";
			String registId = "";
			String registDt = "";
			
			while(reader.ready())         
			{
				doc_id = "";
				icount++;
				String[] data = reader.readLine().split(delimeter);
				//LogMgr.log("data length : " + data.length);
		
				for(int ai=0;ai<data.length;ai++) 
				{
					if(data[ai].equals("") || data[ai].equals(null))
					{
						data[ai] = " ";
					}
					
					if(ai == 1 && flag.equals("SAM")) // 변수에 문서 id를 저장한다(clob 데이터 insert 시 사용됨).
					{
						doc_id = data[ai];
					}
					size = data[ai].getBytes("UTF8").length;

					if(size > 2000) // 2000 byte 넘으면 null 값을 넣는다. 추후 Statement로 update 처리 
					{             		
						tmpStr[tmpIndex] = data[ai];
						data[ai] = "";
						index[tmpIndex] = ai;            		
						tmpIndex++;
					}

					if(data[ai].indexOf("'") != -1)  // 텍스트 내부에 ' 문자 처리. " 로 변환한다.
					{
						data[ai] = data[ai].replaceAll("'", "\"");            		
					}

					if(data[ai].equals(null) || data[ai].equals("null")) // 파일을 읽은 컬럼이 null 값일 경우 처리 
					{
						data[ai] = " ";
					}

					if(ai == 6 && data[ai].equals(null) || data[ai].equals("null")) // clob 컬럼 처리(문서관리시스템은 clob 컬럼 없음)
					{
						data[ai] = " ";
					}
					
							
					if(flag.equals("FILEINFO"))
					{					
						pstmt.setString(ai+1,data[ai]);
						
						if(ai == data.length-1)
						{	 
							pstmt.setString(ai+2,sysId);
							pstmt.setString(ai+3,winFilePath+sysId+"\\");
						}
					}
					else
						pstmt.setString(ai+1,data[ai]);
				}
				
				pstmt.addBatch();            

				if (icount % batchSize == 0) 
				{            	
					pstmt.executeBatch();
				}

				pstmt.executeBatch() ; // 한건 배치 돌림. 

				// clob data insert 시작
				if(CLobFlag == true)
				{
					BufferedReader ClobReader = new BufferedReader(new InputStreamReader(new FileInputStream(CLOBfilepath),"UTF-8"));

					if(flag.equals("SAM"))
					{            	
						String selectClobQuery = CLOB_query; //select query

						String[] resReader = ClobReader.readLine().split("/tag/");
						String content = resReader[1];
						//LogMgr.log(doc_id +"        ,      "+content);

						PreparedStatement ps = null;
						ResultSet rs = null;
						ps = dbcon.prepareStatement(selectClobQuery);
						ps.setString(1,doc_id);
						rs = ps.executeQuery();

						if(rs.next()) {
							//rs의 메소드 중 getClob를 통해 데이터를 가져온다.

							java.sql.Clob clob = rs.getClob("BDT_CN");

							Writer writer = ((CLOB)clob).getCharacterOutputStream();

							Reader src = new CharArrayReader(content.toCharArray());

							char[] buffer = new char[1024];

							int read = 0;

							while( (read = src.read(buffer, 0, 1024)) != -1){
								writer.write(buffer, 0, read);
							}
							src.close();
							writer.close();
						}

						rs.close();
						ps.close();
					}
				}
				// Clob data insert 종료 

				if(size > 2000)        // preparedstatement 에서 null 처리된 항목들의 update 작업 수행
				{
					String query = "";

					for(int i = 0 ; i < index.length ; i++)
					{
						int sIndex = index[i];

						// if ( 문서 DB 정보 )
						if(flag.equals("SAM"))
						{
							query = "update tn_da_doc_info set "+sFIELD_NAME[sIndex]+"='"+tmpStr[i] +"'";
						}
						// else ( 첨부파일 DB 정보 )
						else
						{
							query = "update tn_da_atch_doc_info set "+sFIELD_NAME[sIndex]+"='"+tmpStr[i] +"'";
						}

						result += ST.executeUpdate(query);   
					}
				}
			}// end of while(reader.ready())
			
			if(flag.equals("SAM") && addCnt == 0) // 로그정보에 남겨놓기 위한 정보 저장
			{	
				LogInfo = sysId;	
				addCnt = 1;
			}
			
			dbcon.commit();
			dbcon.setAutoCommit(true);
			dbcon.close();

			LogMgr.log("Load sam file total record count : " + icount);
			resInsert = 1;   // DB insert 가 정상적으로 되었는지 체크하는 변수값 증가.
		} // end of try(line 105)
		catch(SQLException seqe)     // SQL Exception 발생시 db 롤백
		{
			try 
			{ 
				dbcon.rollback();     
			} 
			catch(SQLException se) { }
			LogMgr.log("Error1 : " + seqe.getMessage());
			resInsert = 0;	// DB insert 가 정상적으로 되었는지 체크하는 변수값 증가.
		} 
		catch(IOException ioe)       // I/O Exception 발생시 db 롤백
		{
			try 
			{ 
				dbcon.rollback(); 
			}
			catch(SQLException se) { }
			LogMgr.log("Error2 : " + ioe.getMessage());
			resInsert = 0;	// DB insert 가 정상적으로 되었는지 체크하는 변수값 증가.
		} 
		finally 
		{
			try 						// 정상적으로 Insert가 이루어진 후에 preparedStatement 와 DB Connection 종료. 
			{
				if(pstmt!=null) pstmt.close();
				if(dbcon!=null) dbcon.close();

				long endTime = System.currentTimeMillis();
				long elapsedTime = (endTime - startTime)/1000;//in seconds

				LogMgr.log("Load sam file total elapsed time.(second) : " + elapsedTime);				
			}
			catch (SQLException seqe) { }
		} 
		
		return resInsert;
	}// end of insertSamFileData(String filepath)

	// database connection return
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
	} // end of getDBConnection()

	protected static InputStream getDbConfigInputStream() throws IOException {
		InputStream in = SrcDataRecptnModule.class.getResourceAsStream(CONFIG_DB_XML_FILE_PATH_RESOURCE);

		if( in!=null && in.available()>0)
		{
			return in;
		} else{
			return null;
		}
	}
	protected static InputStream getBatchConfigInputStream() throws IOException {
		InputStream in = SrcDataRecptnModule.class.getResourceAsStream(CONFIG_QUERY_XML_FILE_PATH_RESOURCE);
		if( in!=null && in.available()>0)
		{
			return in;
		} else{
			return null;
		}
	}
	protected static FileInputStream getDbConfigFileInputStream() throws IOException {
		FileInputStream in = new FileInputStream(CONFIG_DB_XML_FILE_PATH);

		if( in!=null && in.available()>0)
		{
			return in;
		} else{
			return null;
		}
	}
	protected static FileInputStream getBatchConfigFileInputStream() throws IOException {
		FileInputStream in = new FileInputStream(CONFIG_QUERY_XML_FILE_PATH);
		if( in!=null && in.available()>0)
		{
			return in;
		} else{
			return null;
		}
	}
	
	
	protected static void insertLogData(String sysId, int result) throws SQLException
	{
		Connection dbcon = null;
		PreparedStatement pstmt = null;
		String insertSQL = "";
		
		try 
		{	
			dbcon=getDBConnection();
			dbcon.setAutoCommit(false);
			
			insertSQL = convert.convertString(sql_props.getProperty("insertLogData"));
			pstmt = dbcon.prepareStatement(insertSQL);
			
			String srcDocId = "";
			String batchDate = "";
			String intrfcCode = "";
			String reWork = "";
			String sttdeCode = "";
			String errorCode = "";
			String registId = "";
			String registDt = "";
			
			if(result != 0)
			{
				srcDocId = convert.convertString(LogInfo);
				
				if(sysId.equals("100001"))
				{
					intrfcCode = convert.convertString("G");
				}
				else 
					intrfcCode = convert.convertString("R");
			
				reWork = convert.convertString("N");        // 재작업 여부. Defulat = "N"
				
				sttdeCode = convert.convertString("A001");
				errorCode = convert.convertString("");
				
			
			} 
			else              // DB insert 작업에 오류 발생하면 에러 코드 삽입
			{
				srcDocId = convert.convertString(sysId);
				batchDate = convert.convertString(DateTimeUtility.getDateString("yyyyMMdd"));
				if(sysId.equals("100001"))
				{
					intrfcCode = convert.convertString("G");
				}
				else 
					intrfcCode = convert.convertString("R");
				
				reWork = convert.convertString("N");
				
				sttdeCode = convert.convertString("0012");
				errorCode = convert.convertString("01");
				
				registId = convert.convertString("");
				registDt = convert.convertString("");
			}
			
			pstmt.setString(1, srcDocId);			
			pstmt.setString(2, intrfcCode);
			pstmt.setString(3, reWork);
			pstmt.setString(4, sttdeCode);
			pstmt.setString(5, errorCode);
			
			pstmt.addBatch();
			pstmt.executeBatch();
			
			dbcon.commit();
			dbcon.setAutoCommit(true);
			dbcon.close();
		}
		catch(SQLException se)
		{
			LogMgr.log("SQL Exception == "+se.getMessage());
		}
	}
} // end of class