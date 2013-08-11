package cn.buding.common.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * provide some data formats and util functions.
 */
public class DateUtil {
	public static final String DateFormatString = "yyyy-MM-dd HH:mm:ss.S";
	public static final String DateFormatString2 = "yyyy-MM-dd HH:mm:ss";
	public static final String DateFormatString3 = "yyyy-MM-dd HH:mm:ss.SSS";

	public static SimpleDateFormat yyyyMMddHHmmssS =
			new SimpleDateFormat(DateFormatString);
	public static SimpleDateFormat yyyyMMddHHmmss =
			new SimpleDateFormat(DateFormatString2);
	public static SimpleDateFormat yyyyMMddHHmmssSSS =
			new SimpleDateFormat(DateFormatString3);
	public static SimpleDateFormat yyyyMMddHHmm =
			new SimpleDateFormat("yyyy-MM-dd HH:mm");

	public static SimpleDateFormat yyyy_MM_dd =
			new SimpleDateFormat("yyyy-MM-dd");
	public static SimpleDateFormat yyyyMMdd = new SimpleDateFormat("yyyyMMdd");
	public static SimpleDateFormat MMdd = new SimpleDateFormat("MM-dd");
	public static SimpleDateFormat HHmm = new SimpleDateFormat("HH:mm");

	public static SimpleDateFormat MMddHHmm =
			new SimpleDateFormat("MM-dd HH:mm");

	private static final String[] WeekDay =
			{ "周日", "周一", "周二", "周三", "周四", "周五", "周六" };

	public static String getWeekDay(Date date) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		return WeekDay[c.get(Calendar.DAY_OF_WEEK) - 1];
	}

	public static boolean isSameDay(Date d1, Date d2) {
		return d1.getYear() == d2.getYear() && d1.getMonth() == d2.getMonth()
				&& d1.getDate() == d2.getDate();
	}

	public static boolean isSameYear(Date d1, Date d2) {
		return d1.getYear() == d2.getYear();
	}

	public static boolean isDayToday(Date day) {
		Date today = new Date();
		return day.getYear() == today.getYear()
				&& day.getMonth() == today.getMonth()
				&& day.getDay() == today.getDay();
	}

	public static boolean isDayTomorrow(Date day) {
		Calendar today = Calendar.getInstance();
		today.add(Calendar.DAY_OF_MONTH, 1);
		return isSameDay(today.getTime(), day);
	}

}
