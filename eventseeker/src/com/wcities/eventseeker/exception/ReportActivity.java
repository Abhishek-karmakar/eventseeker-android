package com.wcities.eventseeker.exception;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.wcities.eventseeker.constants.BundleKeys;

public class ReportActivity extends Activity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent sendIntent = new Intent(Intent.ACTION_SEND);
	    sendIntent.setType("text/plain");
	    sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Crash log");
	    sendIntent.putExtra(Intent.EXTRA_TEXT, getIntent().getStringExtra(BundleKeys.STR_CRASH_LOG));
	    sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {"ankur@wcities.com"});
	    sendIntent.putExtra(Intent.EXTRA_CC, new String[] {"amir@wcities.com"});
	    
		startActivity(sendIntent);
		finish();
	}
}
