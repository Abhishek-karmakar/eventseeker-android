package com.wcities.eventseeker;

import android.os.Bundle;

import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.util.FragmentUtil;

public class DiscoverActivityTab extends BaseActivityTab {
	
	private static final String TAG = DiscoverActivityTab.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Log.d(TAG, "onCreate()");
		setContentView(R.layout.activity_base_tab);
		
		setCommonUI();
		removeToolbarElevation();
		
		if (isOnCreateCalledFirstTime) {
			//Log.d(TAG, "add login fragment tab");
			DiscoverFragmentTab discoverFragmentTab = new DiscoverFragmentTab();
			addFragment(R.id.content_frame, discoverFragmentTab, FragmentUtil.getTag(discoverFragmentTab), false);
		}
	}

	@Override
	public String getScreenName() {
		return ScreenNames.DISCOVER;
	}

	@Override
	protected String getScrnTitle() {
		return getResources().getString(R.string.title_discover);
	}
	
	@Override
	protected int getDrawerItemPos() {
		return INDEX_NAV_ITEM_DISCOVER;
	}
}
