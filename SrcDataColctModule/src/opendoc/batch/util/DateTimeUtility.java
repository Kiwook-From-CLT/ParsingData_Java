package opendoc.batch.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;


public class DateTimeUtility
{
	/*
	 * 현재 시점으로 년월일시분초 문자열을 리턴 : yyyy-MM-dd hh:mm:ss.sss
	 */
	public static String getDate()
	{
		SimpleDateFormat dayTime =  new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.sss");
		return dayTime.format(new Date(System.currentTimeMillis()));
	}

	public static String getDateString(String format) 
	{
		java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat(format, java.util.Locale.KOREA);
		String rtnDate = "";
		rtnDate = setOperationDate();
		return formatter.format(new java.util.Date());
	}
	
	public static String setOperationDate()
	{
		SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd"); 
		GregorianCalendar cal = new GregorianCalendar();

		cal.add(GregorianCalendar.DATE, 0);   //현재날짜에 하루전 날짜를 가져온다(아직 미구현 필요성 못느낌).
		Date date = cal.getTime();             //연산된 날짜를 생성. 
		String setDate = fmt.format(date);
		
		return setDate;
	}
}
