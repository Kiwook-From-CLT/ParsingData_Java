package opendoc.batch.secure;

import java.io.FileInputStream;
import java.io.IOException;

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
