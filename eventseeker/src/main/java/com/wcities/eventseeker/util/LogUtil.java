package com.wcities.eventseeker.util;

import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class LogUtil {
	
	private static final String TAG = LogUtil.class.getSimpleName();
	private static final String FILENAME = "eventseeker.log";

	public static void write(String tag, String text) {
		FileWriter fileWriter = null;
		File file = new File(Environment.getExternalStorageDirectory(), FILENAME);
		try {
			file.createNewFile();
			fileWriter = new FileWriter(file, true);
			fileWriter.append(tag + " - "  + text + "\n");
			
		} catch (IOException e) {
			e.printStackTrace();
			
		} finally {
			try {
				if (fileWriter != null) {
					fileWriter.flush();
					fileWriter.close();
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void write(String TAG, Exception e) {
		PrintWriter printWriter = null;
		File file = new File(Environment.getExternalStorageDirectory(), FILENAME);
		try {
			file.createNewFile();
			printWriter = new PrintWriter(new FileOutputStream(file, true));
			e.printStackTrace(printWriter);
			
		} catch (IOException io) {
			io.printStackTrace();
			
		} finally {
			if (printWriter != null) {
				printWriter.flush();
				printWriter.close();
			}
		}
	}
}
