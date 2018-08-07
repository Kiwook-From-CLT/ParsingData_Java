package opendoc.batch.da;

import java.io.File;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.sql.SQLException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.UnsupportedEncodingException;
import java.sql.*;

import opendoc.batch.secure.ConvertParam;

/**
 * FTP로 받은 파일을 읽어 DB에 넣는 작업 수행 
 * @author Ki Wook, Kwon
 *
 */
public class SrcDataRecptnModule
{
   private static Properties props = null;     // property 파일에서 설정정보를 읽어오기 위한 객체

   // DB 에 접속하기 위한 정보를 가져오기 위한 변수 선언
   private static String DB_DRIVER     = "";   
   private static String DB_CONNECTION = "";
   private static String DB_USERNAME   = "";
   private static String DB_PASSWORD   = "";
   private static String jdbcdriver   = "";
   
   // DB insert 작업을 수행할 Statement 객체 선언
   private static Statement ST = null;
   
   static ConvertParam convert = new ConvertParam();
   /**
    * 로컬의 파일을 읽어와서 DB에 Insert 하는 작업.
    * @param xmlPath
    *  사용자가 properties 파일을 저장한 절대 경로
    * @throws FileNotFoundException   
    *  File Exception 시 처리 된다.    
    * @throws IOException
    *  I/O Exception 발생시 처리 된다.
    */
   public static void main(String xmlPath) throws FileNotFoundException, IOException 
   { 
      props = new Properties();    
      String propsPath = "";
      propsPath = convert.convertString(xmlPath);
      props.loadFromXML(convert.getFileInputStream(propsPath));       // 설정파일 path 지정 파일을 읽어오는 작업 수행.

      //reading proeprty
      jdbcdriver = props.getProperty("jdbcdriver");  		// Oracle / msSql / msSql
      
      
      String directory = "";
      directory = convert.convertString(props.getProperty("directory"));    // DB를 읽어온 파일의 경로

      //jdbc driver 값을 읽어서 해당 jdbc의 접속 정보를 설정한다.
      DB_DRIVER     = props.getProperty("destination."+jdbcdriver+".dbdriver");
      DB_CONNECTION = props.getProperty("destination."+jdbcdriver+".dbconnection");
      DB_USERNAME   = props.getProperty("destination."+jdbcdriver+".dbusername");
      DB_PASSWORD   = props.getProperty("destination."+jdbcdriver+".dbpassword");

      System.out.println("jdbc inform     : " + jdbcdriver);
      System.out.println("jdbc driver     : " + DB_DRIVER);
      System.out.println("jdbc connection : " + DB_CONNECTION);
      System.out.println("jdbc username   : " + DB_USERNAME);
      System.out.println("jdbc password   : " + DB_PASSWORD);

      System.out.println("directory inform: " + directory);

      final File folder = new File(directory);
      int filecnt = listFilesForFolder(folder);
      System.out.println("Load Files Total Count : " + Integer.toString(filecnt));
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
            listFilesForFolder(fileEntry);
         } 
         else
         {
            if (fileEntry.isFile()) 
            {
               String temp = fileEntry.getName();               
               
               // if( 문서 DB 정보 )
               if ((temp.substring(0,3).equals("SAM")) && (temp.substring(temp.lastIndexOf('.') + 1, temp.length()).toLowerCase()).equals("dat")) 
               {
            	  String filepath = folder.getAbsolutePath() + "\\" + fileEntry.getName();            	              	            	  
                  insertSamFileData(filepath,"SAM");
                  ifilecnt++;
                  System.out.println("File = " + filepath);
               }
               // if ( 첨부파일 DB 정보 )
               else if((temp.substring(0,8).equals("FILEINFO")) && (temp.substring(temp.lastIndexOf('.') + 1, temp.length()).toLowerCase()).equals("dat"))
               {
            	   String filepath = folder.getAbsolutePath() + "\\" + fileEntry.getName();            	              	            	  
                   insertSamFileData(filepath,"FILEINFO");
                   ifilecnt++;
                   System.out.println("File = " + filepath);
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
   public static void insertSamFileData(String filepath, String flag)
   {
      Connection dbcon = null;
      PreparedStatement pstmt = null;
      
      long startTime = 0l;

      // 문서DB 정보인지 첨부파일 DB 정보인지 구분해서 쿼리문 입력
      String insertSQL = "";
      
      // if ( 문서 DB 정보 )
      if(flag.equals("SAM"))
      {
    	  if(jdbcdriver.equals("Oracle"))
    	  {
    		  insertSQL = props.getProperty("insertDocInfo_Oracle");
    	  }
    	  else if(jdbcdriver.equals("msSql"))
    	  {
    		  insertSQL = props.getProperty("insertDocInfo_msSql");
    	  }
    	  else if(jdbcdriver.equals("mySql"))
    	  {
    		  insertSQL = props.getProperty("insertDocInfo_mySql");
    	  }
    	  
      }
      
      // else ( 첨부파일 DB 정보 )
      else
      {
    	  if(jdbcdriver.equals("Oracle"))    	  
    	  {
    		  insertSQL = props.getProperty("insertFileInfo_Oracle");    		  
    	  }
    	  else if(jdbcdriver.equals("msSql"))
    	  {
    		  insertSQL = props.getProperty("insertFileInfo_msSql");
    	  }
    	  else if(jdbcdriver.equals("mySql"))
    	  {
    		  insertSQL = props.getProperty("insertFileInfo_mySql");
    	  }
    	  
      }

      // db 컬럼 정의(flag 값을 참조해서 문서DB 인지 첨부파일 DB인지 구분)
      String  sCOL_NAME = "";
      
      // if ( 문서 DB 정보 )
      if(flag.equals("SAM"))
      {
    	  sCOL_NAME = props.getProperty("insertDocInfoColumnName");
      }
      
      // else ( 첨부파일 DB 정보 )
      else
      {
    	  sCOL_NAME = props.getProperty("insertFileInsoColumnName");
      }
       	  
      String[] sFIELD_NAME = sCOL_NAME.split(",");
      
	  System.out.println("FIELD_NAME length : " + sFIELD_NAME.length);

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
         
         ST = dbcon.createStatement();
         
         while(reader.ready())         
         {
        	
            icount++;
            String[] data = reader.readLine().split(delimeter);
            //System.out.println("data length : " + data.length);
            
            for(int ai=0;ai<data.length;ai++) 
            {
         
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
            	
            	pstmt.setString(ai+1,data[ai]);            	
            }
                        
            pstmt.addBatch();            
            
            if (icount % batchSize == 0) 
            {            	
               pstmt.executeBatch();
            }
            
            pstmt.executeBatch() ; // 한건 배치 돌림. 
           
            if(size > 2000)        // preparedstatement 에서 null 처리된 항목들의 update 작업 수행
            {
            	 String query = "";
            	 
            	 for(int i = 0 ; i < index.length ; i++)
            	 {
            		 int sIndex = index[i];
            		 
            		 // if ( 문서 DB 정보 )
            		 if(flag.equals("SAM"))
            		 {
            			 query = "update xmlload1 set "+sFIELD_NAME[sIndex]+"='"+tmpStr[i] +"'";
            		 }
            		 // else ( 첨부파일 DB 정보 )
            		 else
            		 {
            			 query = "update filetest1 set "+sFIELD_NAME[sIndex]+"='"+tmpStr[i] +"'";
            		 }
            		                 
            		 result += ST.executeUpdate(query);   
	            }
            }	                   
         }// end of while(reader.ready())
         
         dbcon.commit();
         dbcon.setAutoCommit(true);
         dbcon.close();

         System.out.println("Load sam file total record count : " + icount);
      } // end of try(line 105)
      catch(SQLException seqe)     // SQL Exception 발생시 db 롤백
      {
         try 
         { 
        	 dbcon.rollback();     
         } 
         catch(SQLException se) { }
         System.out.println("Error1 : " + seqe.getMessage());
      } 
      catch(IOException ioe)       // I/O Exception 발생시 db 롤백
      {
         try 
         { 
        	 dbcon.rollback(); 
         }
         catch(SQLException se) { }
         System.out.println("Error2 : " + ioe.getMessage());
      } 
      finally 
      {
         try 						// 정상적으로 Insert가 이루어진 후에 preparedStatement 와 DB Connection 종료. 
         {
            if(pstmt!=null) pstmt.close();
            if(dbcon!=null) dbcon.close();

            long endTime = System.currentTimeMillis();
            long elapsedTime = (endTime - startTime)/1000;//in seconds

            System.out.println("Load sam file total elapsed time.(second) : " + elapsedTime);
         }
         catch (SQLException seqe) { }
      } 
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
         System.out.println("[database driver loading error]" + e.getMessage());
      }

      try 
      {
         dbConnection = DriverManager.getConnection(DB_CONNECTION, DB_USERNAME ,DB_PASSWORD);
      } 
      catch (SQLException e) 
      {
         System.out.println("[database connection error] : " + e.getMessage());
      }
      return dbConnection;
   } // end of getDBConnection()

} // end of class