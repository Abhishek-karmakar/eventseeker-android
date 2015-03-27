package com.wcities.eventseeker;

import android.os.Bundle;
import android.view.Menu;
import android.view.View;

import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.interfaces.MapListener;
import com.wcities.eventseeker.util.FragmentUtil;

public class EventDetailsActivityTab extends BaseActivityTab implements MapListener {
	
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
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
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
}
