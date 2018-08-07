package opendoc.batch.util;

/**
 *  title : 커맨드 실행기
 *  Subject : parameter를 받아 해당 명령을 java runtime으로 수행
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

public class CommandMgr {

	/**
	 * 명령어와 파라미터를 받아서 runtime으로 처리한다.
	 * 
	 * @param command 수행될 명령어 command
	 * @param param 해당 명령어의 조건값
	 */
	public void CommandExecute(String type, String param) throws Exception
	{
		try
		{
			String command = "";
			if(type.indexOf("rm") > -1)
			{
				command += type + " -f " + param;
			}
			else if (type.indexOf("mkdir") > -1)
			{
				command += type + " " + param;
			}
			LogMgr.log("Command == "+command);

			Runtime.getRuntime().exec(command);
		}
		catch(Exception e)
		{
			LogMgr.log("CommandExecute Error == "+e.getMessage());
		}
	}
}
