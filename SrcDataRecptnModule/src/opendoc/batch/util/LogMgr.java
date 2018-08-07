package opendoc.batch.util;

/**
*  로그를 관리한다.
*  <pre>
*  Table : TH_DF_CNVR_LOG,TH_DF_INTRFC_LOG
*  </pre>
*
*  <pre>
*  <b>History:</b> 
*     dss, 1.0, 2013/08/16  초기 작성
*  </pre>
*  
* @author dss
* @version 1.0, 2013/08/16 초기 작성
* @see   None
*/
public class LogMgr
{
	private static final String LOGTITLE = "LogMgr";

	public LogMgr(){};

	/*
	* Meta Data 로그를 DB에 저장
	*/
	public static void logMetaDataSaveDB(String message,boolean isSucss) 
	{
		System.out.println(DateTimeUtility.getDate() + " ["+LOGTITLE+"] Meta data Log is saved? : "+isSucss+" , " + message);
	}

	/*
	* Meta 첨부문서 Data 로그를 DB에 저장
	*/
	public static void logMetaDataAttchDocSaveDB(String message,boolean isSucss) 
	{
		System.out.println(DateTimeUtility.getDate() + " ["+LOGTITLE+"] Meta attached document data log is saved? : "+isSucss+" , " + message);
	}

	/*
	* Interface Data 로그를 DB에 저장
	*/
	public static void logIfDataSaveDB(String message,boolean isSucss) 
	{
		System.out.println(DateTimeUtility.getDate() + " ["+LOGTITLE+"] Interface data log is saved? : "+isSucss+" , " + message);
	}

	/*
	* Interface 첨부문서 Data 로그를 DB에 저장
	*/
	public static void logIfDataAttchDocSaveDB(String message,boolean isSucss) 
	{
		System.out.println(DateTimeUtility.getDate() + " ["+LOGTITLE+"] Interface attached document data log is saved? : "+isSucss+" , " + message);
	}
	/*
	* 로그출력
	*/
	public static void log(String str) 
	{
		System.out.println(DateTimeUtility.getDate() + " ["+LOGTITLE+"] " + str);
	}
}
