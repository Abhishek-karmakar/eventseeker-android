package com.wcities.eventseeker;

import android.os.Bundle;
import android.view.Menu;

import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.util.FragmentUtil;

public class ArtistDetailsActivityTab extends BaseActivityTab {

	private static final String TAG = ArtistDetailsActivityTab.class.getSimpleName();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getWindow().requestFeature(android.view.Window.FEATURE_ACTIVITY_TRANSITIONS);
	    setupFloatingWindow();

	    //Log.d(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_base_tab_double_line_toolbar);
		
		setCommonUI();
		
		if (isOnCreateCalledFirstTime) {
			//Log.d(TAG, "add event details fragment tab");
			ArtistDetailsFragmentTab artistDetailsFragmentTab = new ArtistDetailsFragmentTab();
			artistDetailsFragmentTab.setArguments(getIntent().getExtras());
			addFragment(R.id.content_frame, artistDetailsFragmentTab, FragmentUtil.getTag(artistDetailsFragmentTab), false);
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		setDrawerLockMode(true);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}
	
	@Override
	public String getScreenName() {
		return ScreenNames.ARTIST_DETAILS;
	}

	@Override
	protected String getScrnTitle() {
		ArtistDetailsFragmentTab artistDetailsFragmentTab = (ArtistDetailsFragmentTab) getSupportFragmentManager()
				.findFragmentByTag(FragmentUtil.getTag(ArtistDetailsFragmentTab.class));
		if (artistDetailsFragmentTab != null) {
			return artistDetailsFragmentTab.getTitle();
		}
		return "";
	}
}
