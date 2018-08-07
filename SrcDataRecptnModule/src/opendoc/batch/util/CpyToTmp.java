package opendoc.batch.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import opendoc.batch.secure.ConvertParam;

public class CpyToTmp {

	ConvertParam convert = new ConvertParam();
	
	public int copyToTmp (String fromDir, String toDir){

		int rtnValue = 0;
		String cmd = "";
		cmd = convert.convertString("cp -f "+ fromDir + " " + toDir);

		Runtime run = Runtime.getRuntime();
		Process p = null;
		LogMgr.log(cmd);

		try{
			p = run.exec(cmd);
			ProcessMgr gb1 = new ProcessMgr(p.getInputStream());
			ProcessMgr gb2 = new ProcessMgr(p.getErrorStream());
			gb1.start();
			gb2.start();

			while (true) {
				if (!gb1.isAlive() && !gb2.isAlive())	//두개의 스레드가 정지하면 프로세스 종료때까지 기다린다. 
				{    
					p.waitFor();
					rtnValue = p.exitValue();
					break;
				}
			}
		}catch(Exception e)
		{			
			LogMgr.log("Copy to DA Folder Process Error == "+e.getMessage());
			rtnValue = 1;
		}finally
		{
			if(p != null) 
				p.destroy();
		}

		return rtnValue;		
	}	

	/**
	 * 지정한 파일(1개)을 os에 상관없이 source에서 target으로의 channel을 이용하여 파일 복사
	 * @param source 복사할 파일명을 포함한 절대 경로 
	 * @param target 복사될 파일명을 포함한 절대 경로
	 * @return
	 */
	public int copyChannel(String source, String target) {
		final String XML_KEY_VALUE	= "copyChannel";
		int rtnValue = 0;// 정상

		LogMgr.log(XML_KEY_VALUE+" 호출.");
		String strSource = "";
		String strTarget = "";

		strSource = convert.convertString(source);
		strTarget = convert.convertString(target);


		//복사 대상이 되는 파일 생성 
		File sourceFile = new File( strSource );

		if (sourceFile.exists()) {
			//스트림, 채널 선언
			FileInputStream inputStream = null;
			FileOutputStream outputStream = null;
			FileChannel fcin = null;
			FileChannel fcout = null;

			try {
				//스트림 생성
				inputStream  = new FileInputStream(sourceFile);
				outputStream = new FileOutputStream(strTarget);

				//채널 생성
				fcin  = inputStream.getChannel();
				fcout = outputStream.getChannel();

				//채널을 통한 스트림 전송
				long size = fcin.size();
				fcin.transferTo(0, size, fcout);

				rtnValue = 0;
			} catch (Exception e) {
				rtnValue = -1;
				LogMgr.log(XML_KEY_VALUE + " error : " + e.getMessage());
			} finally {
				try{
					fcout.close();
					fcin.close();
					outputStream.close();
					inputStream.close();
				} catch(IOException ioe){
					//
				}
			}
		}else{
			rtnValue = -1;
			LogMgr.log(XML_KEY_VALUE+" 지정된 파일["+source+"]을 찾을 수 없습니다.");
		}
		sourceFile = null;
		return rtnValue;
	}
}
