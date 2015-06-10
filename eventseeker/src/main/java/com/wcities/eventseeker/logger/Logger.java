package com.wcities.eventseeker.logger;

import android.content.Context;
import android.util.Log;

import com.wcities.eventseeker.constants.AppConstants;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Logger {
	private static final String TAG = Logger.class.getSimpleName();
	
	private static final String LOG_FILE_NAME = "es.log";
	private static final String LOG_DIR_NAME = "log";
	
	private static File logFile;

	/**
	 * Will initiate the Logger file if 'AppConstants.GENERATE_LOG_FILE' is set 'true'
	 * @param context
	 */
	public static void init(Context context) {
		if (AppConstants.GENERATE_LOG_FILE) {
			logFile = new File(context.getExternalFilesDir(LOG_DIR_NAME), LOG_FILE_NAME);
			if (!logFile.exists()) {
				try {
					logFile.createNewFile();

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static void d(String tag, String msg) {
		Log.d(tag, msg);
		generateHardCopy(tag, msg);
	}
	
	public static void e(String tag, String msg) {
		Log.e(tag, msg);
		generateHardCopy(tag, msg);
	}
	
	public static void i(String tag, String msg) {
		Log.i(tag, msg);
		generateHardCopy(tag, msg);
	}
	
	public static void v(String tag, String msg) {
		Log.v(tag, msg);
		generateHardCopy(tag, msg);
	}
	
	public static void w(String tag, String msg) {
		Log.w(tag, msg);
		generateHardCopy(tag, msg);
	}

	private static void generateHardCopy(String tag, String msg) {
		if (AppConstants.GENERATE_LOG_FILE) {
			if (!logFile.exists()) {
				Log.e(TAG, "LOG FILE DOESN'T EXIST!");
				return;
			}
			try {
				FileWriter fileWriter = new FileWriter(logFile, true);
				fileWriter.append("\n" + System.currentTimeMillis() + " :: " + tag + " : " + msg);
				fileWriter.close();
	
			} catch (IOException e) {
				Log.e(TAG, e.getMessage());
				e.printStackTrace();
			}
		}		
	}
}
