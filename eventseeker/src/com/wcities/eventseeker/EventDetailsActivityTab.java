package com.wcities.eventseeker;

import android.os.Bundle;
import android.util.Log;

import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.util.FragmentUtil;

public class EventDetailsActivityTab extends BaseActivityTab {
	
	private static final String TAG = EventDetailsActivityTab.class.getSimpleName();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
	    getWindow().requestFeature(android.view.Window.FEATURE_ACTIVITY_TRANSITIONS);
	    setupFloatingWindow();

	    //Log.d(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_base_tab_floating);
		
		setCommonUI();
		
		if (isOnCreateCalledFirstTime) {
			//Log.d(TAG, "add event details fragment tab");
			EventDetailsFragmentTab eventDetailsFragmentTab = new EventDetailsFragmentTab();
			eventDetailsFragmentTab.setArguments(getIntent().getExtras());
			addFragment(R.id.content_frame, eventDetailsFragmentTab, FragmentUtil.getTag(eventDetailsFragmentTab), false);
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		setDrawerLockMode(true);
	}
	
	@Override
	public String getScreenName() {
		return ScreenNames.EVENT_DETAILS;
	}

	@Override
	protected String getScrnTitle() {
		EventDetailsFragmentTab eventDetailsFragmentTab = (EventDetailsFragmentTab) getSupportFragmentManager().findFragmentByTag(FragmentUtil.getTag(EventDetailsFragmentTab.class));
		if (eventDetailsFragmentTab != null) {
			return eventDetailsFragmentTab.getTitle();
		}
		return "";
	}
}
