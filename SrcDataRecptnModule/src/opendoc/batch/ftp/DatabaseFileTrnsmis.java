package opendoc.batch.ftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Vector;

import opendoc.batch.secure.ConvertParam;
import opendoc.batch.util.CommandMgr;
import opendoc.batch.util.LogMgr;
import opendoc.batch.util.ProcessMgr;


import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

/**
*  title : SFTP 송수신 모듈
*  Subject : 파일을 SFTP 방식으로 송수한 하는 모듈
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

public class DatabaseFileTrnsmis{

	private Session session = null;
	private Channel channel = null;
	private ChannelSftp channelSftp = null;	
	private String resFlagPath = "";
	private static ConvertParam convert = new ConvertParam();				      // 보안성 체크를 위한 클래스 변수 (추후 수정 필요)
	
	/**
	* 서버와 연결에 필요한 값들을 가져와 초기화 시킴
	*
	* @param host
	* 서버 주소
	* @param userName
	* 접속에 사용될 아이디
	* @param password
	* 비밀번호
	* @param port
	* 포트번호
	*/
	
	public void init(String host, String userName, String password, int port) 
	{
		
		System.out.println("SFTP upLoad Start...");
		
		JSch jsch = new JSch();
		
		try 
		{
			session = jsch.getSession(userName, host, port);
			
			session.setPassword(password);
			java.util.Properties config = new java.util.Properties();
			
			config.put("StrictHostKeyChecking", "no");
			
			session.setConfig(config);			
			session.connect();
			
			channel = session.openChannel("sftp");			
			channel.connect();			
			
		} 
		catch (JSchException e) 
		{
			e.printStackTrace();
		}

		channelSftp = (ChannelSftp) channel;
	}
	

	/**
	* DB 정보 파일을 업로드 한다.
	*
	* @param dir
	* 저장시킬 주소(서버)
	* @param file
	* 저장할 파일
	*/
	public void dbInfoUpload(String dir, File[] file, File resFilePath) 
	{		
		FileInputStream in = null;		
		try 
		{
			int i = 0;
			
			boolean mainFolderIndex = false;       // SFTP 서버에 메인 파일 폴더의 존재 유무를 체크하기 위한 Flag			
			boolean subFolderIndex = false;        // SFTP 서버에 서브 파일 폴더의 존재 유무를 체크하기 위한 Flag
			boolean dateFolderIndex = false;       // SFTP 서버에 오늘날짜 폴더의 존재 유무를 체크하기 위한 Flag
			boolean idFolderIndex = false;         // SFTP 서버에 "META" 폴더의 존재 유무를 체크하기 위한 Flag
			
			String[] dirPath = dir.split("/");     // Parameter로 넘어온 디렉토리 경로를 "/"로 나눈다(메인 파일폴더와 서브 파일 폴더로 구분된다). 
			Vector list = new Vector();            // 특정 path 하위의 폴더 및 파일 리스트를 검색 후 담기 위한 Vector 변수 
			String res = "";                       // Vector의 결과값을 담아서 parameter로 넘어온 dirPath와 비교하기 위한 String 변수
			
			
			list = channelSftp.ls(channelSftp.getHome()); // SFTP 홈 디렉토리 아래의 전체 파일 및 폴더 리스트를 가져온다.
			
			for(int j = 0 ; j < list.size() ; j++)
			{
				res = list.elementAt(j).toString();				
				
				if(res.indexOf(dirPath[0]) > -1 && res.substring(0,1).equals("d"))                 // Parameter 로 넘어온 메인 파일 폴더명이 이미 존재하면...
				{
					mainFolderIndex = true;                      // Flag 값을 true로 변경
				}			
			}
			
			if(mainFolderIndex == false)                         // Flag 값이 False 이면 파일 폴더가 존재하지 않는다.
			{
				channelSftp.mkdir(dirPath[0]);                   // 폴더 생성.
			}			
			channelSftp.cd(dirPath[0]);                          // 폴더 이동.
			
			
			// 메인 폴더의 존재 여부 체크 시작
			list = channelSftp.ls(channelSftp.getHome()+"/"+dirPath[0]); // SFTP 홈 디렉토리 아래의 전체 파일 및 폴더 리스트를 가져온다.
			
			for(int j = 0 ; j < list.size() ; j++)
			{
				res = list.elementAt(j).toString();				
				
				if(res.indexOf(dirPath[1]) > -1 && res.substring(0,1).equals("d"))                 // Parameter 로 넘어온 메인 파일 폴더명이 이미 존재하면...
				{
					subFolderIndex = true;                      // Flag 값을 true로 변경
				}			
			}
			
			if(subFolderIndex == false)                         // Flag 값이 False 이면 파일 폴더가 존재하지 않는다.
			{
				channelSftp.mkdir(dirPath[1]);                   // 폴더 생성.
			}			
			channelSftp.cd(dirPath[1]);                          // 폴더 이동.
									
			// 메인 폴더의 존재 여부 체크 종료						
			
			
			String date = getDateString("yyyyMMdd");
			
			// 서브 폴더의 존재 여부 체크 시작 (날짜 기준 : 'yyyymmdd')
			list = channelSftp.ls(channelSftp.getHome()+"/"+dirPath[0]+"/"+dirPath[1]);  // 메인 파일 폴더 밑의 모든 파일 및 폴더 리스트를 가져온다. 
					
			for(int j = 0 ; j < list.size() ; j++)
			{
				res = list.elementAt(j).toString();
				
				if(res.indexOf(dirPath[2]) > -1 && res.substring(0,1).equals("d"))   // 서브 파일폴더명과 동일한 폴더가 이미 존재하면...
				{
					dateFolderIndex = true;                               			 // Flag 값을 true로 변경.
				}			
			}
						
			if(dateFolderIndex == false)                                   // Flag 값이 False 이면 파일 폴더가 존재하지 않는다.
			{
				channelSftp.mkdir(dirPath[2]);                            // 폴더 생성
			}						
			
			channelSftp.cd(dirPath[2]);                                   // 폴더 이동
			
			// 서브 폴더의 존재 여부 체크 종료
			
			// 문서 ID 폴더의 존재 여부 체크 시작 ( 문서 ID 별로 폴더 생성 )
			
			list = channelSftp.ls(channelSftp.getHome()+"/"+dirPath[0]+"/"+dirPath[1]+"/"+dirPath[2]);   
			int cnt = 0; 
			String metaFolder = "META";							
			for(int j = 0 ; j < list.size() ; j++)
			{
				res = list.elementAt(j).toString();
				//System.out.println(" detail 1 == " + res);
				if(res.indexOf(metaFolder) > -1 && res.substring(0,1).equals("d"))  // 서브 파일폴더명과 동일한 폴더가 이미 존재하면...
				{
					idFolderIndex = true;                                      // Flag 값을 true로 변경.
					//	System.out.println(" detail 2 ");
				}	
			}
						
			if(idFolderIndex == false) 
			{
				//System.out.println(" detail 3 ");
				channelSftp.mkdir(metaFolder);
			}				
			channelSftp.cd(metaFolder);
			//System.out.println(" Step 4 complete");
			// 문서 ID 폴더의 존재 여부 체크 종료
			
			// 지정된 폴더로 파일 업로드
			for(i = 0 ; i < file.length ; i++)
			{							
				in = new FileInputStream(file[i]);						  
				channelSftp.put(in, file[i].getName());					  // 서버로 파일 전송	
				LogMgr.log("Upload File == " + file[i].getName());
			}
			
			in = new FileInputStream(resFilePath);
			channelSftp.put(in, resFilePath.getName());
		} 
		catch (SftpException e) 
		{
			e.printStackTrace();
		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		finally 
		{
			try 
			{
				in.close();
			} 
			catch (IOException e) 
			{
				System.out.println("SFTP upLoad Fail...");
				e.printStackTrace();
			}
		}		
	}
	
	/**
	* 첨부 파일을 업로드 한다.
	* 
	* @param dir
	* 저장시킬 주소(서버)
	* @param file
	* 저장할 파일
	*/
	public String attachedFileUpload(String dir, File file) 
	{	
		FileInputStream in = null;	
		String rtnStr = "";
		
		try 
		{
			boolean mainFolderIndex = false;       // SFTP 서버에 메인 파일 폴더의 존재 유무를 체크하기 위한 Flag  
			boolean subFolderIndex = false;        // SFTP 서버에 서브 파일 폴더의 존재 유무를 체크하기 위한 Flag
			boolean dateFolderIndex = false;       // SFTP 서버에 오늘날짜 폴더의 존재 유무를 체크하기 위한 Flag
			
			
			String[] dirPath = dir.split("/");     // Parameter로 넘어온 디렉토리 경로를 "/"로 나눈다(메인 파일폴더와 서브 파일 폴더로 구분된다). 
			Vector list = new Vector();            // 특정 path 하위의 폴더 및 파일 리스트를 검색 후 담기 위한 Vector 변수 
			String res = "";                       // Vector의 결과값을 담아서 parameter로 넘어온 dirPath와 비교하기 위한 String 변수     
			
			// 메인 폴더의 존재 여부 체크 시작
			channelSftp.cd(channelSftp.getHome());
			
			list = channelSftp.ls(channelSftp.getHome()); // SFTP 홈 디렉토리 아래의 전체 파일 및 폴더 리스트를 가져온다.
			//System.out.println(" Step 1 Start"+  channelSftp.getHome());
			
			for(int j = 0 ; j < list.size() ; j++)
			{
				res = list.elementAt(j).toString();
				
				if(res.indexOf(dirPath[0]) > -1 && res.substring(0,1).equals("d"))    // Parameter 로 넘어온 메인 파일 폴더명이 이미 존재하면...
				{
					mainFolderIndex = true;                    						  // Flag 값을 true로 변경
				}			
			}
			
			if(mainFolderIndex == false)                         // Flag 값이 False 이면 파일 폴더가 존재하지 않는다.
			{				
				channelSftp.mkdir(dirPath[0]);                   // 폴더 생성.
			}			
			channelSftp.cd(dirPath[0]);                          // 폴더 이동.
			//LogMgr.log("1-depth : "+dirPath[0]);
			// 메인 폴더의 존재 여부 체크 종료		
			
			
			list = channelSftp.ls(channelSftp.getHome()+"/"+dirPath[0]); // SFTP 홈 디렉토리 아래의 전체 파일 및 폴더 리스트를 가져온다.
			//System.out.println(" Step 1 Start"+  channelSftp.getHome());
			
			for(int j = 0 ; j < list.size() ; j++)
			{
				res = list.elementAt(j).toString();
				
				if(res.indexOf(dirPath[1]) > -1 && res.substring(0,1).equals("d"))    // Parameter 로 넘어온 메인 파일 폴더명이 이미 존재하면...
				{
					subFolderIndex = true;                    						  // Flag 값을 true로 변경
				}			
			}
			
			if(subFolderIndex == false)                         // Flag 값이 False 이면 파일 폴더가 존재하지 않는다.
			{				
				channelSftp.mkdir(dirPath[1]);                   // 폴더 생성.
			}			
			channelSftp.cd(dirPath[1]);                          // 폴더 이동.
			//LogMgr.log("2-depth : "+dirPath[1]);
			// 메인 폴더의 존재 여부 체크 종료						
			
			String date = getDateString("yyyyMMdd");
			
			// 서브 폴더의 존재 여부 체크 시작 (날짜 기준 : 'yyyymmdd')
			list = channelSftp.ls(channelSftp.getHome()+"/"+dirPath[0]+"/"+dirPath[1]);  // 메인 파일 폴더 밑의 모든 파일 및 폴더 리스트를 가져온다. 
			//System.out.println(list.toString());		
			for(int j = 0 ; j < list.size() ; j++)
			{
				res = list.elementAt(j).toString();
				
				if(res.indexOf(dirPath[2]) > -1 && res.substring(0,1).equals("d"))   // 서브 파일폴더명과 동일한 폴더가 이미 존재하면...
				{
					dateFolderIndex = true;                               			 // Flag 값을 true로 변경.
				}			
			}
						
			if(dateFolderIndex == false)                                   // Flag 값이 False 이면 파일 폴더가 존재하지 않는다.
			{
				channelSftp.mkdir(dirPath[2]);                            // 폴더 생성
			}						
			
			channelSftp.cd(dirPath[2]);                                   // 폴더 이동
			//LogMgr.log("4-depth : "+date);
			// 서브 폴더의 존재 여부 체크 종료
			
			// 첨부파일 업로드 작업 수행
			in = new FileInputStream(file);				
			channelSftp.put(in, file.getName());	
			
			rtnStr = convert.convertString("complete");
			
			LogMgr.log("Upload File = " + file.getName());
		} 
		
		catch (SftpException e) 
		{
			LogMgr.log("SFTP Error == "+e.getMessage());
			rtnStr = convert.convertString("fail");
		} 
		catch (FileNotFoundException e) 
		{
			LogMgr.log("File Not Found Error == "+e.getMessage());
			rtnStr = convert.convertString("fail");
		} 
		finally 
		{
			try 
			{
				in.close();
				rtnStr = convert.convertString("complete");
			} 
			catch (IOException e) 
			{
				LogMgr.log("SFTP upLoad Fail...");
				LogMgr.log("I/O Error == "+e.getMessage());
				rtnStr = convert.convertString("fail");				
			}	
		}	
		
		return rtnStr;
	}
	
	
	
	/**
	* 서버와의 연결을 끊는다.
	*/
	public void disconnection() throws FileNotFoundException, SftpException
	{		
		channelSftp.quit();
		channelSftp.disconnect();		
		channel.disconnect();
		session.disconnect();
	}

	
   public static String getDateString(String format) 
   {
      java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat(format, java.util.Locale.KOREA);
      return formatter.format(new java.util.Date());
   }


/*  SFTP 다운로드 (현재는 사용하지 않는다)	*/
	/**
	* 하나의 파일을 다운로드 한다.
	*
	* @param dir
	* 저장할 경로(서버)
	* @param downloadFileName
	* 다운로드할 파일
	* @param path
	* 저장될 공간
	*//*
	
	public void download(String dir, String downloadFileName, String path) 
	{
		InputStream in = null;
		FileOutputStream out = null;
		
		try 
		{
			channelSftp.cd(dir);
			in = channelSftp.get(downloadFileName);
		} 
		catch (SftpException e) 
		{			
			e.printStackTrace();
		}
		try 
		{
			out = new FileOutputStream(new File(path));
			int i;
	
			while ((i = in.read()) != -1) 
			{
				out.write(i);
			}
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		} 
		finally 
		{
			try 
			{
				out.close();
				in.close();
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
	}
	
*/	

}

