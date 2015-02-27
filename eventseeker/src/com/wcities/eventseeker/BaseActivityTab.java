package com.wcities.eventseeker;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.LinearLayout;

import com.wcities.eventseeker.app.EventSeekr;

/**
 * Using ActionBarActivity (extended by BaseActivity) instead of Activity so as to use support library toolbar as actionbar even for lower apis
 * by calling setSupportActionBar(toolbar) & also there is common code to both mobile & tablet which can be kept in BaseActivity
 */
public abstract class BaseActivityTab extends BaseActivity {
	
	private Toolbar toolbar;
	private LinearLayout lnrLayoutRootNavDrawer;
	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle mDrawerToggle;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// check whether the current device is Tablet and if it is in Landscape mode
		EventSeekr eventSeekr = ((EventSeekr) getApplication());
		eventSeekr.checkAndSetIfInLandscapeMode();
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
	
	protected void setUI() {
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
	}
}
