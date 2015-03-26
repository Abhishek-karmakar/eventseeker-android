package com.wcities.eventseeker;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.interfaces.MapListener;
import com.wcities.eventseeker.interfaces.VenueListenerTab;
import com.wcities.eventseeker.util.FragmentUtil;

public class EventDetailsActivityTab extends BaseActivityTab implements MapListener, VenueListenerTab {
	
	private static final String TAG = EventDetailsActivityTab.class.getSimpleName();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
	    getWindow().requestFeature(android.view.Window.FEATURE_ACTIVITY_TRANSITIONS);
	    setupFloatingWindow();

	    //Log.d(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_base_tab_floating);
		
		setCommonUI();
		
		if (isOnCreateCalledFirstTime) {
			//Log.d(TAG, "add event details fragment tab");
			EventDetailsFragmentTab eventDetailsFragmentTab = new EventDetailsFragmentTab();
			eventDetailsFragmentTab.setArguments(getIntent().getExtras());
			addFragment(R.id.content_frame, eventDetailsFragmentTab, FragmentUtil.getTag(eventDetailsFragmentTab), false);
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		setDrawerLockMode(true);
	}
	
	protected View getViewById(int id) {
		return findViewById(id);
	}
	
	@Override
	public String getScreenName() {
		return ScreenNames.EVENT_DETAILS;
	}

	@Override
	protected String getScrnTitle() {
		EventDetailsFragmentTab eventDetailsFragmentTab = (EventDetailsFragmentTab) getSupportFragmentManager()
				.findFragmentByTag(FragmentUtil.getTag(EventDetailsFragmentTab.class));
		if (eventDetailsFragmentTab != null) {
			return eventDetailsFragmentTab.getTitle();
		}
		return "";
	}

	@Override
	public void onMapClicked(Bundle args) {
		super.onMapClicked(args);
	}

	@Override
	public void onVenueSelected(Venue venue, ImageView imageView, TextView textView) {
		super.onVenueSelected(venue, imageView, textView);
	}
}
