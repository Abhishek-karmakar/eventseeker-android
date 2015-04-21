package com.wcities.eventseeker;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.util.FragmentUtil;

public class SearchActivityTab extends BaseActivityTab {
	
	private static final String TAG = SearchActivityTab.class.getSimpleName();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(android.view.Window.FEATURE_ACTIVITY_TRANSITIONS);
        super.onCreate(savedInstanceState);
        
		//Log.d(TAG, "onCreate()");
		setContentView(R.layout.activity_base_tab_with_tabs);
		
		setCommonUI();
		
		if (isOnCreateCalledFirstTime) {
			//Log.d(TAG, "add event details fragment tab");
			SearchFragmentTab searchFragmentTab = new SearchFragmentTab();
			searchFragmentTab.setArguments(getIntent().getExtras());
			addFragment(R.id.content_frame, searchFragmentTab, FragmentUtil.getTag(searchFragmentTab), false);
		}
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
		Fragment fragment = getSupportFragmentManager().findFragmentByTag(FragmentUtil.getTag(SearchFragmentTab.class));
		if (fragment != null) {
			((SearchFragmentTab) fragment).onQueryTextUpdated(query);
		}
		return true;
	}
}
