package com.wcities.eventseeker;

import android.os.Bundle;

import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.util.FragmentUtil;

public class MyEventsActivityTab extends BaseActivityTab {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Log.d(TAG, "onCreate()");
		setContentView(R.layout.activity_base_tab_with_tabs);
		
		setCommonUI();
		
		if (isOnCreateCalledFirstTime) {
			//Log.d(TAG, "add settings fragment tab");
			MyEventsFragmentTab myEventsFragmentTab = new MyEventsFragmentTab();
			if (getIntent().hasExtra(BundleKeys.NOTIFICATION_ARGS)) {
				myEventsFragmentTab.setArguments(getIntent().getBundleExtra(BundleKeys.NOTIFICATION_ARGS));
			}
			addFragment(R.id.content_frame, myEventsFragmentTab, FragmentUtil.getTag(myEventsFragmentTab), false);
		}
	}

	@Override
	public String getScreenName() {
		return ScreenNames.MY_EVENTS_SCREEN;
	}

	@Override
	protected String getScrnTitle() {
		return getResources().getString(R.string.title_my_events);
	}
	
	@Override
	protected int getDrawerItemPos() {
		return AppConstants.INDEX_NAV_ITEM_MY_EVENTS;
	}
}
