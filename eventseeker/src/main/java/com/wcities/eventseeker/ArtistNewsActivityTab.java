package com.wcities.eventseeker;

import android.os.Bundle;

import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.util.FragmentUtil;

public class ArtistNewsActivityTab extends BaseActivityTab {
	
	private static final String TAG = ArtistNewsActivityTab.class.getSimpleName();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Log.d(TAG, "onCreate()");
		setContentView(R.layout.activity_base_tab);
		
		setCommonUI();
		
		if (isOnCreateCalledFirstTime) {
			Bundle args = new Bundle();
			args.putSerializable(BundleKeys.ARTIST, (Artist) getIntent().getSerializableExtra(BundleKeys.ARTIST));

			ArtistNewsListFragmentTab artistNewsListFragmentTab = new ArtistNewsListFragmentTab();
			artistNewsListFragmentTab.setArguments(args);
			addFragment(R.id.content_frame, artistNewsListFragmentTab, FragmentUtil.getTag(artistNewsListFragmentTab), false);
		}
		
		if (savedInstanceState != null) {
			currentContentFragmentTag = savedInstanceState.getString(BundleKeys.CURRENT_CONTENT_FRAGMENT_TAG);
		}
	}

	@Override
	public String getScreenName() {
		return ScreenNames.ARTIST_MORE_NEWS_SCREEN;
	}

	@Override
	protected String getScrnTitle() {
		return getResources().getString(R.string.title_artists_news);
	}
}
