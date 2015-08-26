package com.wcities.eventseeker;

import android.os.Bundle;

import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.util.FragmentUtil;

public class NavigationActivityTab extends BaseActivityTab {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_base_tab);
		
		setCommonUI();
		
		if (isOnCreateCalledFirstTime) {
			//Log.d(TAG, "add login fragment tab");
			NavigationFragmentTab navigationFragmentTab = new NavigationFragmentTab();
			navigationFragmentTab.setArguments(getIntent().getExtras());
			addFragment(R.id.content_frame, navigationFragmentTab, FragmentUtil.getTag(navigationFragmentTab), false);
		}
	}

	@Override
	public String getScreenName() {
		return ScreenNames.NAVIGATION_SELECTION;
	}

	@Override
	protected String getScrnTitle() {
		return getResources().getString(R.string.title_navigation);
	}
}
