package com.wcities.eventseeker.exception;

import android.content.Intent;
import android.util.Log;

import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.BundleKeys;

import java.lang.Thread.UncaughtExceptionHandler;

public class DefaultUncaughtExceptionHandler implements	UncaughtExceptionHandler {
	
	private static final String TAG = DefaultUncaughtExceptionHandler.class.getSimpleName();
	
	private EventSeekr eventSeekr;
	
	public DefaultUncaughtExceptionHandler(EventSeekr eventSeekr) {
		this.eventSeekr = eventSeekr;
	}

	@Override
	public void uncaughtException(Thread arg0, Throwable arg1) {
		try {
			//Log.d(TAG, "uncaughtException() " + Log.getStackTraceString(arg1));
			
		    Intent intent = new Intent(eventSeekr, ReportActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		    		.putExtra(BundleKeys.STR_CRASH_LOG, Log.getStackTraceString(arg1));
		    eventSeekr.startActivity(intent);
		    
		} finally {
			System.exit(1);
		}
	}
}
