package com.wcities.eventseeker;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.wcities.eventseeker.analytics.GoogleAnalyticsTracker;
import com.wcities.eventseeker.analytics.IGoogleAnalyticsTracker;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;

/**
 * Using ActionBarActivity (extended by BaseActivity) instead of Activity so as to use support library toolbar as actionbar even for lower apis
 * by calling setSupportActionBar(toolbar) & also there is common code to both mobile & tablet which can be kept in BaseActivity
 */
public abstract class BaseActivityTab extends BaseActivity implements IGoogleAnalyticsTracker {
	
	private Toolbar toolbar;
	private LinearLayout lnrLayoutRootNavDrawer;
	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle mDrawerToggle;
	
	private boolean isOnCreateCalledFirstTime = true;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// check whether the current device is Tablet and if it is in Landscape mode
		EventSeekr eventSeekr = ((EventSeekr) getApplication());
		eventSeekr.checkAndSetIfInLandscapeMode();
		
		if (savedInstanceState != null) {
			isOnCreateCalledFirstTime = savedInstanceState.getBoolean(BundleKeys.IS_ON_CREATE_CALLED_FIRST_TIME);
		}
		
		//Log.d(TAG, "isOnCreateCalledFirstTime = " + isOnCreateCalledFirstTime);
		if (isOnCreateCalledFirstTime) {
			GoogleAnalyticsTracker.getInstance().sendScreenView((EventSeekr) getApplication(), getScreenName());
			isOnCreateCalledFirstTime = false;
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		getSupportActionBar().setTitle(getScrnTitle());
		mDrawerToggle.setDrawerIndicatorEnabled(getDrawerItemPos() != AppConstants.INVALID_INDEX);
	}
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		if (mDrawerToggle != null) {
			// Sync the toggle state after onRestoreInstanceState has occurred.
			mDrawerToggle.syncState();
		}
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(BundleKeys.IS_ON_CREATE_CALLED_FIRST_TIME, isOnCreateCalledFirstTime);
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case android.R.id.home:
			if (mDrawerLayout.isDrawerOpen(lnrLayoutRootNavDrawer)) {
				mDrawerLayout.closeDrawer(lnrLayoutRootNavDrawer);
				
			} else if (mDrawerToggle.isDrawerIndicatorEnabled()) {
				mDrawerLayout.openDrawer(lnrLayoutRootNavDrawer);

			} else {
				onBackPressed();
			}
			return true;
			
		default:
			break;
		}

		return super.onOptionsItemSelected(item);
	}
	
	protected void setCommonUI() {
		toolbar = (Toolbar) findViewById(R.id.toolbarForActionbar);
	    setSupportActionBar(toolbar);
	    
	    lnrLayoutRootNavDrawer = (LinearLayout) findViewById(R.id.rootNavigationDrawer);
	    mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
	    mDrawerToggle = new ActionBarDrawerToggle(this, // host Activity
				mDrawerLayout, // DrawerLayout object
				R.string.drawer_open, // "open drawer" description
				R.string.drawer_close // "close drawer" description
		) {
	    	
			// Called when a drawer has settled in a completely closed state.
			public void onDrawerClosed(View view) {
				super.onDrawerClosed(view);
			}

			// Called when a drawer has settled in a completely open state.
			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
			}
			
			@Override
			public void onDrawerSlide(View drawerView, float slideOffset) {
				super.onDrawerSlide(drawerView, slideOffset);
			}
		};
		mDrawerLayout.setDrawerListener(mDrawerToggle);
		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP);
	}
	
	/**
	 * Activities corresponding to navigation drawer items should override this method to return valid position.
	 * @return
	 */
	protected int getDrawerItemPos() {
		return AppConstants.INVALID_INDEX;
	}
	
	protected abstract String getScrnTitle();
}
