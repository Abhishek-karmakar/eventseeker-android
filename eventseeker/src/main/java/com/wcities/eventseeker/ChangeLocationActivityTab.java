package com.wcities.eventseeker;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;

import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.util.FragmentUtil;

public class ChangeLocationActivityTab extends BaseActivityTab {
	
	private static final String TAG = ChangeLocationActivityTab.class.getSimpleName();
	
	private SearchView searchView;
	private String searchQuery = "";
	protected boolean isSearchViewIconified = true;
	
	private ChangeLocationFragmentTab changeLocationFragmentTab;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Log.d(TAG, "onCreate()");
		setContentView(R.layout.activity_base_tab);
		
		setCommonUI();
		
		if (isOnCreateCalledFirstTime) {
			//Log.d(TAG, "add settings fragment tab");
			changeLocationFragmentTab = new ChangeLocationFragmentTab();
			addFragment(R.id.content_frame, changeLocationFragmentTab, FragmentUtil.getTag(changeLocationFragmentTab), false);
		
		} else {
			changeLocationFragmentTab = (ChangeLocationFragmentTab) 
					getSupportFragmentManager().findFragmentByTag(FragmentUtil.getTag(ChangeLocationFragmentTab.class));
		}
		
		if (savedInstanceState != null) {
			isSearchViewIconified = savedInstanceState.getBoolean(BundleKeys.IS_SEARCHVIEW_ICONIFIED);
			searchQuery = savedInstanceState.getString(BundleKeys.SEARCH_QUERY);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.fragment_change_location, menu);
    	
		MenuItem searchItem = menu.findItem(R.id.action_search_view);
    	searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setQueryHint(getResources().getString(R.string.menu_search));
        searchView.setOnQueryTextListener(this);
        
        ImageView v = (ImageView) searchView.findViewById(R.id.search_button);
		// null check is for safety purpose
		if (v != null) {
			v.setImageResource(R.drawable.search);
		}
		
		// Get the SearchView and set the searchable configuration
	    SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
	    // Assumes current activity is the searchable activity
	    searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
		
        if (searchQuery != null && !searchQuery.equals("")) {
        	searchView.setQuery(searchQuery, false);
        }
        if (!isSearchViewIconified) {
        	searchView.setIconified(false);
        }
        return true;
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		//Log.d(TAG, "onNewIntent()");
		super.onNewIntent(intent);
		setIntent(intent);
		
		if (Intent.ACTION_VIEW.equals(intent.getAction())) {
        	//Log.d(TAG, "onNewIntent() - view");
        	String query = intent.getStringExtra(SearchManager.EXTRA_DATA_KEY);
        	searchView.setQuery(query, false);
        	onQueryTextSubmit(query);
        }
	}

	@Override
	public String getScreenName() {
		return ScreenNames.CHANGE_LOCATION;
	}

	@Override
	protected String getScrnTitle() {
		return getResources().getString(R.string.title_change_location);
	}

	@Override
	public void onBackPressed() {
		/**
		 * 10-04-2015: This change is mentioned in Android Edits sheet on Google Drive.
		 * If User has come from Discover Screen, then he must return there, else if he has
		 * come from Settings Screen, then he must return there. 
		 */
		Bundle args = getIntent().getExtras();
		if (args == null) {
			super.onBackPressed();
			
		} else {
			/**
			 * Args are used on going back to discover screen from here after no events been found
			 * earlier on discover screen showing change location button.
			 */
			onDrawerItemSelected(AppConstants.INDEX_NAV_ITEM_DISCOVER, args);
			finish();
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			onBackPressed();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (searchView != null) {
			outState.putBoolean(BundleKeys.IS_SEARCHVIEW_ICONIFIED, searchView.isIconified());
		}
		outState.putString(BundleKeys.SEARCH_QUERY, searchQuery);
	}
	
	@Override
	public boolean onQueryTextSubmit(String query) {
		//Log.d(TAG, "onQueryTextSubmit() - query = " + query);
		if (changeLocationFragmentTab != null) {
			this.searchQuery = query;
			// to remove autocomplete suggestions
			searchView.clearFocus();
			boolean result = changeLocationFragmentTab.onQueryTextSubmit(query);
			hideSoftKeypad();
			return result;
		}
		return false;
	}
	
	@Override
	public boolean onQueryTextChange(String query) {
		//Log.d(TAG, "onQueryTextChange() - query = " + query);
		if (changeLocationFragmentTab != null) {
			this.searchQuery = query;
			return changeLocationFragmentTab.onQueryTextChange(query);
		}
		return false;
	}
	
	private void hideSoftKeypad() {
    	InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    	imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
    }
}
