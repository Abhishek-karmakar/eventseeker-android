package com.wcities.eventseeker.util;

import android.content.Context;
import android.content.res.Resources;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.constants.Enums;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

public class ConversionUtil {
	
	private static final String TAG = ConversionUtil.class.getName();

	private static final long SECONDS_PER_MINUTE = 60;
	private static final long SECONDS_PER_HOUR = SECONDS_PER_MINUTE*60;
	private static final long SECONDS_PER_DAY = SECONDS_PER_HOUR*24;
	private static final long SECONDS_PER_WEEK = SECONDS_PER_DAY*7;
	private static final long SECONDS_PER_MONTH = SECONDS_PER_DAY*30;
	private static final long SECONDS_PER_YEAR = SECONDS_PER_DAY*365;

    public static final long MILLI_SECONDS_PER_DAY = SECONDS_PER_DAY*1000;

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
	/*public static String getTime(Date date) {
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
	}*/
	
	public static String getTime(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		
		int hr = calendar.get(Calendar.HOUR_OF_DAY);
		
		int min = calendar.get(Calendar.MINUTE);
		String strMin = (min < 10) ? "0" + min : min + "";
		String time = hr + ":" + strMin;
		
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
	 * @param date
	 * @return day in the form MARCH 17.
	 */
	public static String getDayForFriendsActivity(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		
		DateFormatSymbols dateFormatSymbols = new DateFormatSymbols();
		String time = dateFormatSymbols.getMonths()[calendar.get(Calendar.MONTH)].toUpperCase() 
				+ " " + calendar.get(Calendar.DATE);
		return time;
	}
	
	/**
	 * 
	 * @param date
	 * @param parseTime
	 * @param parseYear Applicable only for English language; otherwise it's true for all non-English languages
	 * @param amPmCaps Applicable only for English language; otherwise it's 24-hr clock format, so no am-pm needed
	 * @param spaceBeforeAmPm Applicable only for English language; otherwise it's 24-hr clock format, so no am-pm needed
	 * @return datetime in format
     * Friday October 17, 2015 10:00pm for English language
     * & Friday 17/10/2015 22:00 for non-English languages.
     * It's configurable based on arguments passed.
	 */
	public static String getDateTime(Context context, Date date, boolean parseTime, boolean parseYear, boolean amPmCaps,
			boolean spaceBeforeAmPm) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);

        // For all non-english versions, use 24-hr clock and dd/mm/yyyy format
        boolean isEnglishLocale = Enums.Locales.isDefaultLocale(context, Enums.Locales.ENGLISH);

        String dateFormatStr = "EEEE ";
        if (isEnglishLocale) {
            dateFormatStr += "MMMM dd";
            if (parseYear) {
                dateFormatStr += ", yyyy";
            }

        } else {
            dateFormatStr += "dd/MM/yyyy";
        }
				
		if (parseTime) {
			if (isEnglishLocale && !parseYear) {
                dateFormatStr += ",";
			}

            if (isEnglishLocale) {
                dateFormatStr += " h:mm";

                if (spaceBeforeAmPm) {
                    dateFormatStr += " ";
                }

                dateFormatStr += "a";

            } else {
                dateFormatStr += " H:mm";
            }
		}

        DateFormat df = new SimpleDateFormat(dateFormatStr);
        String time = df.format(calendar.getTime());
        if (!amPmCaps) {
            time = time.replace("AM", "am").replace("PM", "pm");
        }
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
	 * @param calendar
	 * @return date in the form 2013-06-30 (yyyy-MM-dd).
	 */
	public static String getDay(Calendar calendar) {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		return df.format(calendar.getTime());
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

	/**
	 * @param date
	 * @return time difference in the form "<number> seconds/minutes/hours/days/weeks/months/years ago"
	 */
	public static String getTimeDiffFromCurrentTime(Date date, Resources res) {
		String strTimeDiff = null;
		long timeDiffInSeconds = (System.currentTimeMillis() - date.getTime()) / 1000;
		
		if (timeDiffInSeconds < SECONDS_PER_MINUTE) {
			strTimeDiff = (timeDiffInSeconds > 1) ? String.format(res.getString(R.string.sec_gt_one), timeDiffInSeconds)
				: res.getString(R.string.sec_not_gt_one);
			
		} else if (timeDiffInSeconds < SECONDS_PER_HOUR) {
			long mins = timeDiffInSeconds / SECONDS_PER_MINUTE;
			strTimeDiff = (mins > 1) ? String.format(res.getString(R.string.min_gt_one), mins)
				: res.getString(R.string.min_not_gt_one);
			
		} else if (timeDiffInSeconds < SECONDS_PER_DAY) {
			long hours = timeDiffInSeconds / SECONDS_PER_HOUR;
			strTimeDiff = (hours > 1) ? String.format(res.getString(R.string.hr_gt_one), hours)
				: res.getString(R.string.hr_not_gt_one);
			
		} else if (timeDiffInSeconds < SECONDS_PER_WEEK) {
			long days = timeDiffInSeconds / SECONDS_PER_DAY;
			strTimeDiff = (days > 1) ? String.format(res.getString(R.string.day_gt_one), days)
				: res.getString(R.string.day_not_gt_one);
			
		} else if (timeDiffInSeconds < SECONDS_PER_MONTH) {
			long weeks = timeDiffInSeconds / SECONDS_PER_WEEK;
			strTimeDiff = (weeks > 1) ? String.format(res.getString(R.string.week_gt_one), weeks)
				: res.getString(R.string.week_not_gt_one);
			
		} else if (timeDiffInSeconds < SECONDS_PER_YEAR) {
			long months = timeDiffInSeconds / SECONDS_PER_MONTH;
			strTimeDiff = (months > 1) ? String.format(res.getString(R.string.mon_gt_one), months)
				: res.getString(R.string.mon_not_gt_one);
			
		} else {
			long years = timeDiffInSeconds / SECONDS_PER_YEAR;
			strTimeDiff = (years > 1) ? String.format(res.getString(R.string.yr_gt_one), years)
				: res.getString(R.string.yr_not_gt_one);
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
	
	public static String removeBuggyTextsFromDesc(String src) {
		String dest = src.replace("amp;", "");
		return dest;
	}
	
	public static String decodeHtmlEntities(JSONObject jsonObject, String key) throws JSONException {
		return jsonObject.getString(key).replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">");
	}
	
	public static String parseForPhone(String src) {
		return src.replaceAll("[^\\d+]", "");
	}

	public static int doubleToIntRoundOff(double d) {
		int floor = (int) Math.floor(d);
		return (d - floor) > 0.5 ? (int) Math.ceil(d) : floor;
	}

	public static String formatFloatingNumber(int numAfterDecimal, double numberToBeFormatted) {
		return String.format(Locale.ENGLISH, "%."+ numAfterDecimal +"f", numberToBeFormatted);
	}
	
}
