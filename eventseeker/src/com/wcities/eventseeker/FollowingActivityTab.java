package com.wcities.eventseeker;

import android.os.Bundle;

import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.util.FragmentUtil;

public class FollowingActivityTab extends BaseActivityTab {
	
	private static final String TAG = FollowingActivityTab.class.getSimpleName();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Log.d(TAG, "onCreate()");
		setContentView(R.layout.activity_base_tab);
		
		setCommonUI();
		
		if (isOnCreateCalledFirstTime) {
			FollowingFragmentTab followingFragmentTab = new FollowingFragmentTab();
			addFragment(R.id.content_frame, followingFragmentTab, FragmentUtil.getTag(followingFragmentTab), false);
		}
		
		if (savedInstanceState != null) {
			currentContentFragmentTag = savedInstanceState.getString(BundleKeys.CURRENT_CONTENT_FRAGMENT_TAG);
		}
	}

	@Override
	public String getScreenName() {
		return ScreenNames.FOLLOWING_SCREEN;
	}

	@Override
	protected String getScrnTitle() {
		return getResources().getString(R.string.title_following);
	}
	
	@Override
	protected int getDrawerItemPos() {
		return AppConstants.INDEX_NAV_ITEM_FOLLOWING;
	}
}
