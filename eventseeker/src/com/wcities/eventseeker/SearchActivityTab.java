package com.wcities.eventseeker;

import android.os.Bundle;

import com.wcities.eventseeker.constants.ScreenNames;

public class SearchActivityTab extends BaseActivityTab {
	
	private static final String TAG = SearchActivityTab.class.getSimpleName();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(android.view.Window.FEATURE_ACTIVITY_TRANSITIONS);
        super.onCreate(savedInstanceState);
        
		//Log.d(TAG, "onCreate()");
		setContentView(R.layout.activity_base_tab);
		
		setCommonUI();
	}

	@Override
	public String getScreenName() {
		return ScreenNames.SEARCH;
	}

	@Override
	protected String getScrnTitle() {
		return getResources().getString(R.string.title_search_results);
	}
	
	public boolean onQueryTextUpdated(String query) {
		return true;
	}
}
