package com.wcities.eventseeker;

import android.os.Bundle;

import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.interfaces.MapListener;
import com.wcities.eventseeker.util.FragmentUtil;

public class VenueDetailsActivityTab extends BaseActivityTab implements MapListener {
	
	private static final String TAG = VenueDetailsActivityTab.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
	    getWindow().requestFeature(android.view.Window.FEATURE_ACTIVITY_TRANSITIONS);
	    setupFloatingWindow();

	    //Log.d(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_base_tab);
		
		setCommonUI();
		allowContentBehindToolbar();
		
		if (isOnCreateCalledFirstTime) {
			//Log.d(TAG, "add event details fragment tab");
			VenueDetailsFragmentTab venueDetailsFragmentTab = new VenueDetailsFragmentTab();
			venueDetailsFragmentTab.setArguments(getIntent().getExtras());
			addFragment(R.id.content_frame, venueDetailsFragmentTab, FragmentUtil.getTag(venueDetailsFragmentTab), false);
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		setDrawerLockMode(true);
	}
	
	@Override
	public String getScreenName() {
		return ScreenNames.VENUE_DETAILS;
	}

	@Override
	protected String getScrnTitle() {
		VenueDetailsFragmentTab venueDetailsFragmentTab = (VenueDetailsFragmentTab) getSupportFragmentManager()
				.findFragmentByTag(FragmentUtil.getTag(VenueDetailsFragmentTab.class));
		if (venueDetailsFragmentTab != null) {
			return venueDetailsFragmentTab.getTitle();
		}
		return "";
	}

	@Override
	public void onMapClicked(Bundle args) {
		super.onMapClicked(args);
	}
}
