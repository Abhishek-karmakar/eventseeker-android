package com.wcities.eventseeker;

import android.os.Bundle;

import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.util.FragmentUtil;

public class ArtistsNewsActivityTab extends BaseActivityTab {
	
	private static final String TAG = ArtistsNewsActivityTab.class.getSimpleName();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Log.d(TAG, "onCreate()");
		setContentView(R.layout.activity_base_tab);
		
		setCommonUI();
		
		if (isOnCreateCalledFirstTime) {
			ArtistsNewsListFragmentTab artistsNewsListFragmentTab = new ArtistsNewsListFragmentTab();
			addFragment(R.id.content_frame, artistsNewsListFragmentTab, FragmentUtil.getTag(artistsNewsListFragmentTab), false);
		}
		
		if (savedInstanceState != null) {
			currentContentFragmentTag = savedInstanceState.getString(BundleKeys.CURRENT_CONTENT_FRAGMENT_TAG);
		}
	}

	@Override
	public String getScreenName() {
		return ScreenNames.ARTISTS_NEWS_SCREEN;
	}

	@Override
	protected String getScrnTitle() {
		return getResources().getString(R.string.title_artists_news);
	}
	
	@Override
	protected int getDrawerItemPos() {
		return AppConstants.INDEX_NAV_ITEM_ARTISTS_NEWS;
	}
}
