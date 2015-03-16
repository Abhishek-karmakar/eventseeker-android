package com.wcities.eventseeker;

import android.os.Bundle;

import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.util.FragmentUtil;

public class EventDetailsActivityTab extends BaseActivityTab {
	
	private String title = "";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getWindow().requestFeature(android.view.Window.FEATURE_CONTENT_TRANSITIONS);
	    getWindow().requestFeature(android.view.Window.FEATURE_ACTIVITY_TRANSITIONS);
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_base_tab);
		
		setCommonUI();
		
		if (isOnCreateCalledFirstTime) {
			//Log.d(TAG, "add login fragment tab");
			EventDetailsFragmentTab eventDetailsFragmentTab = new EventDetailsFragmentTab();
			eventDetailsFragmentTab.setArguments(getIntent().getExtras());
			addFragment(R.id.content_frame, eventDetailsFragmentTab, FragmentUtil.getTag(eventDetailsFragmentTab), false);
		}
	}
	
	@Override
	public String getScreenName() {
		return ScreenNames.EVENT_DETAILS;
	}

	@Override
	protected String getScrnTitle() {
		return title;
	}
}
