package com.wcities.eventseeker.util;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import android.content.res.Resources;
import android.util.Log;

public class ConversionUtil {
	
	private static final String TAG = ConversionUtil.class.getName();

	private static final long SECONDS_PER_MINUTE = 60;
	private static final long SECONDS_PER_HOUR = 60*60;
	private static final long SECONDS_PER_DAY = 60*60*24;
	private static final long SECONDS_PER_WEEK = 60*60*24*7;
	private static final long SECONDS_PER_MONTH = 60*60*24*30;
	private static final long SECONDS_PER_YEAR = 60*60*24*365;

	public static int toPx(Resources res, float dp) {
		// Get the screen's density scale
		final float scale = res.getDisplayMetrics().density;
		
		// Convert the dps to pixels, based on density scale
		return (int) (dp * scale + 0.5f);
	}
	
	/**
	 * @param date
	 * @return time considering 12-hour clock with AM/PM attached at the end
	 */
	public static String getTime(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		
		int hr = calendar.get(Calendar.HOUR);
		if (hr == 0) {
			hr = 12;
		}
		
		int min = calendar.get(Calendar.MINUTE);
		String strMin = (min < 10) ? "0" + min : min + "";
		
		String am_pm = ((calendar.get(Calendar.AM_PM) == Calendar.AM) ? "AM" : "PM");
		String time = hr + ":" + strMin + am_pm;
		return time;
	}
	
	/**
	 * @param date
	 * @return time considering 12-hour clock with AM/PM attached at index 1 of the array
	 */
	public static String[] getTimeInArray(Date date) {
		String[] time = new String[2];
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		
		int hr = calendar.get(Calendar.HOUR);
		if (hr == 0) {
			hr = 12;
		}
		
		int min = calendar.get(Calendar.MINUTE);
		String strMin = (min < 10) ? "0" + min : min + "";
		time[0] = hr + ":" + strMin;
		
		String am_pm = ((calendar.get(Calendar.AM_PM) == Calendar.AM) ? "AM" : "PM");
		time[1] = am_pm;
		return time;
	}
	
	/**
	 * @param date
	 * @return day in the form FRIDAY, MARCH 15.
	 */
	public static String getDay(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		
		DateFormatSymbols dateFormatSymbols = new DateFormatSymbols();
		String time = dateFormatSymbols.getWeekdays()[calendar.get(Calendar.DAY_OF_WEEK)].toUpperCase() 
				+ ", " + dateFormatSymbols.getMonths()[calendar.get(Calendar.MONTH)].toUpperCase() 
				+ " " + calendar.get(Calendar.DATE);
		return time;
	}
	
	/**
	 * @param year
	 * @param month
	 * @param day
	 * @return date in the form 2013-06-30 (yyyy-MM-dd).
	 */
	public static String getDay(int year, int month, int day) {
		Calendar calendar = new GregorianCalendar(year, month, day);
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		return df.format(calendar.getTime());
	}
	
	/**
	 * @param year
	 * @param month
	 * @param day
	 * @return date in the form (dd MMMM hh:mm a).
	 */
	public static String getDateTime(com.wcities.eventseeker.core.Date date) {
		String pattern = date.isStartTimeAvailable() ? "dd MMMM hh:mm a" : "dd MMMM";
		DateFormat df = new SimpleDateFormat(pattern);
		return df.format(date.getStartDate());
	}
	
	/**
	 * @param date
	 * @return time difference in the form "<number> seconds/minutes/hours/days/weeks/months/years ago"
	 */
	public static String getTimeDiffFromCurrentTime(Date date) {
		String strTimeDiff = null;
		long timeDiffInSeconds = (System.currentTimeMillis() - date.getTime()) / 1000;
		
		if (timeDiffInSeconds < SECONDS_PER_MINUTE) {
			strTimeDiff = (timeDiffInSeconds > 1) ? timeDiffInSeconds + " seconds ago" : timeDiffInSeconds + " second ago";
			
		} else if (timeDiffInSeconds < SECONDS_PER_HOUR) {
			long mins = timeDiffInSeconds / SECONDS_PER_MINUTE;
			strTimeDiff = (mins > 1) ? mins + " minutes ago" : mins + " minute ago";
			
		} else if (timeDiffInSeconds < SECONDS_PER_DAY) {
			long hours = timeDiffInSeconds / SECONDS_PER_HOUR;
			strTimeDiff = (hours > 1) ? hours + " hours ago" : hours + " hour ago";
			
		} else if (timeDiffInSeconds < SECONDS_PER_WEEK) {
			long days = timeDiffInSeconds / SECONDS_PER_DAY;
			strTimeDiff = (days > 1) ? days + " days ago" : days + " day ago";
			
		} else if (timeDiffInSeconds < SECONDS_PER_MONTH) {
			long weeks = timeDiffInSeconds / SECONDS_PER_WEEK;
			strTimeDiff = (weeks > 1) ? weeks + " weeks ago" : weeks + " week ago";
			
		} else if (timeDiffInSeconds < SECONDS_PER_YEAR) {
			long months = timeDiffInSeconds / SECONDS_PER_MONTH;
			strTimeDiff = (months > 1) ? months + " months ago" : months + " month ago";
			
		} else {
			long years = timeDiffInSeconds / SECONDS_PER_YEAR;
			strTimeDiff = (years > 1) ? years + " years ago" : years + " year ago";
		}
		return strTimeDiff;
	}
	
	public static String getYoutubeScreenShot(final String youtubeUrl, final String size) {
		final String startTag = "/";
		final String endTag = "?";
		final String endTag2 = "&";
		final int startIndex = youtubeUrl.lastIndexOf(startTag);

		if (startIndex >= 0) {
			int endIndex = youtubeUrl.indexOf(endTag, startIndex + startTag.length());
			if (endIndex == -1) {
				endIndex = youtubeUrl.indexOf(endTag2, startIndex + startTag.length());
				if (endIndex == -1) {
					endIndex = youtubeUrl.length();
				}
			}

			final String videoPart = youtubeUrl.substring(startIndex + startTag.length(), endIndex);
			final String imageUrl = "http://img.youtube.com/vi/" + videoPart + "/" + size + ".jpg";
			return imageUrl;
		}
		return null;
	}
	
	public static float stringToFloat(String in) {
		if (in.contains(",")) {
			String[] priceComponents = in.split(",");
			return Float.parseFloat(priceComponents[0] + "." + priceComponents[1]);
			
		} else {
			// it contains '.'
			try {
				return Float.parseFloat(in);
				
			} catch (NumberFormatException e) {
				return 0;
			}
		}
	}
}