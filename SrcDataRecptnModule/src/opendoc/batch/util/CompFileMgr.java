package opendoc.batch.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CompFileMgr {

	public String getUnZip(String dir, String date){

		String shellFile = "";		
		String filePath = dir+"/"+date+".jar"; 		   // 압축파일이 생성된 Full-Path
		String result = "true";
		CommandMgr comm = new CommandMgr();            // Java runtime 명령어를 수행하기 위한 클래스 변수

		shellFile = getShellFile(dir, date);       // Shell File 내부의 명령어를 작성하기 위한 함수 호출 

		Process p = null;		  
		try{
			p = new ProcessBuilder("/bin/bash",shellFile).start();
			ProcessMgr gb1 = new ProcessMgr(p.getInputStream());
			ProcessMgr gb2 = new ProcessMgr(p.getErrorStream());
			gb1.start();
			gb2.start();

			while (true) {
				if (!gb1.isAlive() && !gb2.isAlive())   //두개의 스레드가 정지할면 프로세스 종료때까지 기다린다.
				{
					p.waitFor();
					break;
				}		   
			}

			comm.CommandExecute("rm", shellFile); // 임시로 생성된 Shell 파일 삭제

		}catch(Exception e){
			LogMgr.log("Compress File Process Error == "+e.getMessage());
			result = "false";
		}finally{
			if(p != null) p.destroy();		   
		}			  
		return result;
	}	


	/**
	 * linux system 에서 사용할 임시 shell 파일을 생성해서 리턴한다.
	 * @param dir  : 압축대상파일이 있는 경로 ex) /home/opendoc/TestColct/cpFile/
	 * @return filename
	 */
	public String getShellFile(String dir, String date){
		final String XML_KEY_VALUE	= "getUnzipFile";
		final String fn = "mkAbsFile.sh";
		String str= "#!/bin/bash";

		BufferedWriter bw = null;
		File f = new File(fn);

		LogMgr.log(XML_KEY_VALUE+" 호출.["+dir+"]");

		try {
			bw = new BufferedWriter(new FileWriter(f));
			str += "\n";
			str += "cd " +dir +" \n";			
			str += "jar -xvf "+date+".jar \n";
			str += "rm -rf META-INF \n";
			str += "rm -f "+date+".jar \n";
			bw.write(str);
			bw.flush();
			bw.close();
		} catch (IOException e) {
			LogMgr.log(e.getMessage());
		} finally {
			if(bw!=null) bw=null;
			if(f !=null)  f=null;
		}
		return fn;		
	}
}
