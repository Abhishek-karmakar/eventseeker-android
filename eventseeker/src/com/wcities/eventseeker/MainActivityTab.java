package com.wcities.eventseeker;

import android.os.Bundle;

import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.util.VersionUtil;

public class MainActivityTab extends BaseActivityTab {
	
	private static final String TAG = MainActivityTab.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Log.d(TAG, "onCreate()");
		setContentView(R.layout.activity_main_tab);
		
		setUI();
		VersionUtil.updateCheckes((EventSeekr) getApplication());
	}
}
