package com.wcities.eventseeker;

import android.os.Bundle;

import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.util.FragmentUtil;

public class RecommendedArtistsActivityTab extends BaseActivityTab {
	
	private static final String TAG = RecommendedArtistsActivityTab.class.getSimpleName();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Log.d(TAG, "onCreate()");
		setContentView(R.layout.activity_base_tab);
		
		setCommonUI();
		
		if (isOnCreateCalledFirstTime) {
			RecommendedArtistsFragmentTab recommendedArtistsFragmentTab = new RecommendedArtistsFragmentTab();
			addFragment(R.id.content_frame, recommendedArtistsFragmentTab, FragmentUtil.getTag(recommendedArtistsFragmentTab), false);
		}
		
		if (savedInstanceState != null) {
			currentContentFragmentTag = savedInstanceState.getString(BundleKeys.CURRENT_CONTENT_FRAGMENT_TAG);
		}
	}

	@Override
	public String getScreenName() {
		return ScreenNames.RECOMMENDED_ARTISTS_SCREEN;
	}

	@Override
	protected String getScrnTitle() {
		return getResources().getString(R.string.title_recommended);
	}
}
