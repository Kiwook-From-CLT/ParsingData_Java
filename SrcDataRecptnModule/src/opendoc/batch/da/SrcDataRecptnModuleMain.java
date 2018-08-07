package opendoc.batch.da;

import java.io.File;
import java.util.Properties;
import java.io.CharArrayReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.UnsupportedEncodingException;
import java.sql.*;

import oracle.sql.CLOB;

import opendoc.batch.secure.ConvertParam;
import opendoc.batch.util.LogMgr;



public class SrcDataRecptnModuleMain
{
   
   private static Properties db_props = null;
   private static Properties sql_props = null;
   
   private static String DB_DRIVER     = "";   
   private static String DB_CONNECTION = "";
   private static String DB_USERNAME   = "";
   private static String DB_PASSWORD   = "";
   private static String jdbcdriver    = "";
   private static String CLOB_query    = "";
   private static String CLOBfilepath  = "";   
   
   private static String CONFIG_DB_XML_FILE_PATH_RESOURCE    = "conn_properties.xml";		// db config xml
   private static String CONFIG_QUERY_XML_FILE_PATH_RESOURCE = "batch_da_propertiess.xml"; // query config xml
	
   
   private static Statement ST = null;
   private static ConvertParam convert = new ConvertParam(); 
   
   public static void SrcDataRecptnModuleMain() throws FileNotFoundException, IOException 
   { 
	  //Reading properties file in Java example
	  db_props = new Properties();
	  sql_props = new Properties();
	  
	  
	  db_props.loadFromXML(getDbConfigFileInputStream());		 
	  sql_props.loadFromXML(getBatchConfigFileInputStream());
	  

      //reading proeprty
      jdbcdriver = db_props.getProperty("destinationjdbcdriver");  		// Oracle / msSql / msSql
      
      
      String directory = "";
      directory = convert.convertString(db_props.getProperty("directory"));   

      DB_DRIVER     = db_props.getProperty("destination."+jdbcdriver+".dbdriver");
      DB_CONNECTION = db_props.getProperty("destination."+jdbcdriver+".dbconnection");
      DB_USERNAME   = db_props.getProperty("destination."+jdbcdriver+".dbusername");
      DB_PASSWORD   = db_props.getProperty("destination."+jdbcdriver+".dbpassword");
      CLOB_query    = sql_props.getProperty("selectClob");
      
      LogMgr.log("destination inform     : " + jdbcdriver);
      LogMgr.log("destination driver     : " + DB_DRIVER);
      LogMgr.log("destination connection : " + DB_CONNECTION);
      LogMgr.log("destination username   : " + DB_USERNAME);
      LogMgr.log("destination password   : " + DB_PASSWORD);

      LogMgr.log("directory inform: " + directory);

      final File folder = new File(directory);
      int filecnt = listFilesForFolder(folder);
      LogMgr.log("Load Files Total Count : " + Integer.toString(filecnt));
   }


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
               LogMgr.log(temp);
               if((temp.substring(0,4).equals("CLOB")) && (temp.substring(temp.lastIndexOf('.') + 1, temp.length()).toLowerCase()).equals("dat"))
               {
            	   CLOBfilepath = folder.getAbsolutePath() + "\\" + fileEntry.getName();
            	   LogMgr.log("CLOBFile = " + CLOBfilepath);                   
                   ifilecnt++;
               }
               else if ((temp.substring(0,3).equals("SAM")) && (temp.substring(temp.lastIndexOf('.') + 1, temp.length()).toLowerCase()).equals("dat")) 
               {
            	  String filepath = folder.getAbsolutePath() + "\\" + fileEntry.getName();
            	  LogMgr.log("docInfoFile = " + filepath);
                  insertSamFileData(filepath,"SAM");
                  ifilecnt++;                 
               }
               else if((temp.substring(0,8).equals("FILEINFO")) && (temp.substring(temp.lastIndexOf('.') + 1, temp.length()).toLowerCase()).equals("dat"))
               {
            	   String filepath = folder.getAbsolutePath() + "\\" + fileEntry.getName();
            	   LogMgr.log("atchFile = " + filepath);
                   insertSamFileData(filepath,"FILEINFO");
                   ifilecnt++;                   
               }               
            }
         }
      }
      return ifilecnt;
   }// end of listFilesForFolder(final File folder)

      

   public static void insertSamFileData(String filepath, String flag)
   {
      Connection dbcon = null;
      PreparedStatement pstmt = null;
      
      long startTime = 0l;

      String insertSQL = "";
  
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

      LogMgr.log("SQL = " +insertSQL);
      
      String  sCOL_NAME = "";
      
      if(flag.equals("SAM"))
      {
    	  sCOL_NAME = sql_props.getProperty("insertDocInfoColumnName");
      }
      
      else
      {
    	  sCOL_NAME = sql_props.getProperty("insertFileInfoColumnName");
      }
       	  
      String[] sFIELD_NAME = sCOL_NAME.split(",");
    
      try 
      {
         dbcon = getDBConnection();

         dbcon.setAutoCommit(false); 
         pstmt = dbcon.prepareStatement(insertSQL);
         
         int batchSize = 10000;

         startTime = System.currentTimeMillis();
         String path = "";
         path = convert.convertString(filepath);
         BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path),"UTF-8"));         
         BufferedReader ClobReader = new BufferedReader(new InputStreamReader(new FileInputStream(CLOBfilepath),"UTF-8"));
         
         long icount = 0l;
         String delimeter = "/tag/";         
         String doc_id = "";
         int[] index = new int[batchSize];
         String[] tmpStr = new String[batchSize];
         int result = 1;
         int size = 0;
         int tmpIndex = 0;
         
         ST = dbcon.createStatement();
         
         while(reader.ready())         
         {
        	doc_id = "";
            icount++;
            String[] data = reader.readLine().split(delimeter);
            
            for(int ai=0;ai<data.length;ai++) 
            {
            	if(ai == 1 && flag.equals("SAM")) 
            	{
            		doc_id = data[ai];
            	}
            	
            	size = data[ai].getBytes("UTF8").length;
            	
            	if(size > 2000)
            	{             		
            		tmpStr[tmpIndex] = data[ai];
            		data[ai] = "";
            		index[tmpIndex] = ai;            		
            		tmpIndex++;
            	}
            	
            	if(data[ai].indexOf("'") != -1)  
            	{
            		data[ai] = data[ai].replaceAll("'", "\"");            		
            	}
            	
            	if(data[ai].equals("null"))      /
            	{
            		data[ai] = "";
            	}
            	pstmt.setString(ai+1,data[ai]);            	
            }
                        
            pstmt.addBatch();
            
            pstmt.executeBatch() ; 
            
            if(flag.equals("SAM"))
            {            	
                String selectClobQuery = CLOB_query; //select query
                
                String[] resReader = ClobReader.readLine().split("/tag/");
                String content = resReader[1];
            	LogMgr.log(doc_id +"        ,      "+content);
                                
                PreparedStatement ps = null;
                ResultSet rs = null;
                ps = dbcon.prepareStatement(selectClobQuery);
        	    ps.setString(1,doc_id);
        	    rs = ps.executeQuery();

        	    if(rs.next()) {
        	            
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
          
            if(size > 2000)     
            {
            	 String query = "";
            	 
            	 for(int i = 0 ; i < index.length ; i++)
            	 {
            		 int sIndex = index[i];
            		 
            		 if(flag.equals("SAM"))
            		 {
            			 query = "update tn_da_doc_info_1 set "+sFIELD_NAME[sIndex]+"='"+tmpStr[i] +"'";
            		 }
            		 else
            		 {
            			 query = "update tn_da_atch_doc_info_1 set "+sFIELD_NAME[sIndex]+"='"+tmpStr[i] +"'";
            		 }
            		                 
            		 result += ST.executeUpdate(query);   
	            }
            }	                   
         }// end of while(reader.ready())
         
         dbcon.commit();
         dbcon.setAutoCommit(true);
         dbcon.close();

         LogMgr.log("Load sam file total record count : " + icount);
      } // end of try(line 105)
      catch(SQLException seqe)     
      {
         try 
         { 
        	 dbcon.rollback();     
         } 
         catch(SQLException se) { }
         LogMgr.log("Error1 : " + seqe.getMessage());
      } 
      catch(IOException ioe)      
      {
         try 
         { 
        	 dbcon.rollback(); 
         }
         catch(SQLException se) { }
         LogMgr.log("Error2 : " + ioe.getMessage());
      } 
      finally 
      {
         try 						
         {
            if(pstmt!=null) pstmt.close();
            if(dbcon!=null) dbcon.close();

            long endTime = System.currentTimeMillis();
            long elapsedTime = (endTime - startTime)/1000;//in seconds

            LogMgr.log("Load sam file total elapsed time.(second) : " + elapsedTime);
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

}