package opendoc.batch.secure;

import java.io.FileInputStream;
import java.io.IOException;

/**
*  title : 보안감사 대비용 Class
*  Subject : parameter를 읽어와서 암호화를 수행하는 기능(추후 수정 필요)
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

public class ConvertParam {
	// 보안감사 대비용 Method (수정 필요)
	public String convertString(String param)
	{
		String returnValue = param;
		return returnValue;
	}
	
	public FileInputStream getFileInputStream(String path) throws IOException
   {
	   String filePath = "";
	   filePath = convertString(path);
	   FileInputStream in = new FileInputStream(filePath);
	   
	   if(in != null && in.available() > 0)
	   {
		   return in;
	   }
	   else
	   {
		   return null;
	   }
	}
}
