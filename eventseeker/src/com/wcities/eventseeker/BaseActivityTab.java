package com.wcities.eventseeker;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.wcities.eventseeker.analytics.GoogleAnalyticsTracker;
import com.wcities.eventseeker.analytics.IGoogleAnalyticsTracker;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.interfaces.ActivityDestroyedListener;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.GPlusUtil;

/**
 * Using ActionBarActivity (extended by BaseActivity) instead of Activity so as to use support library toolbar as actionbar even for lower apis
 * by calling setSupportActionBar(toolbar) & also there is common code to both mobile & tablet which can be kept in BaseActivity
 */
public abstract class BaseActivityTab extends BaseActivity implements IGoogleAnalyticsTracker, ActivityDestroyedListener {
	
	private static final String TAG = BaseActivityTab.class.getSimpleName(); 
	
	protected static final int INDEX_NAV_ITEM_DISCOVER = 0;

	private Toolbar toolbar;
	private LinearLayout lnrLayoutRootNavDrawer;
	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle mDrawerToggle;
	
	protected String currentContentFragmentTag;
	
	protected boolean isOnCreateCalledFirstTime = true;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// check whether the current device is Tablet and if it is in Landscape mode
		EventSeekr eventSeekr = ((EventSeekr) getApplication());
		eventSeekr.checkAndSetIfInLandscapeMode();
		eventSeekr.setActivityDestroyedListener(this);
		
		if (savedInstanceState != null) {
			isOnCreateCalledFirstTime = savedInstanceState.getBoolean(BundleKeys.IS_ON_CREATE_CALLED_FIRST_TIME);
			currentContentFragmentTag = savedInstanceState.getString(BundleKeys.CURRENT_CONTENT_FRAGMENT_TAG);
		}
		
		//Log.d(TAG, "isOnCreateCalledFirstTime = " + isOnCreateCalledFirstTime);
		if (isOnCreateCalledFirstTime) {
			GoogleAnalyticsTracker.getInstance().sendScreenView((EventSeekr) getApplication(), getScreenName());
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		// mark it false here instead of from onCreate() so that child class can use it from its onCreate()
		isOnCreateCalledFirstTime = false;

		getSupportActionBar().setTitle(getScrnTitle());
		
		/**
		 * Use isTaskRoot() instead of navigation drawer item index to set hamburger/back icon, because it's possible that
		 * notification comes for some valid navigation drawer item but other activity is already open in which case we should 
		 * have back icon & not hamburger. 
		 */
		//Log.d(TAG, "is root = " + isTaskRoot());
		mDrawerToggle.setDrawerIndicatorEnabled(isTaskRoot());
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
	protected void onDestroy() {
		super.onDestroy();
		((EventSeekr)getApplication()).onActivityDestroyed();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult(), requestCode = " + requestCode + ", resultCode = " + resultCode);
		switch (requestCode) {
			
		case AppConstants.REQ_CODE_GOOGLE_PLUS_RESOLVE_ERR:
		case AppConstants.REQ_CODE_GET_GOOGLE_PLAY_SERVICES:
			//Log.d(TAG, "current frag tag = " + currentContentFragmentTag);
			if (currentContentFragmentTag != null) {
				Fragment fragment = getSupportFragmentManager().findFragmentByTag(currentContentFragmentTag);
				if (fragment != null) {
					fragment.onActivityResult(requestCode, resultCode, data);
				}
			}
			break;
			
		default:
			/*if (GPlusUtil.isGPlusPublishPending) {
				*//**
				 * This check is required to direct onActivityResult() calls from MainActivity & handle it at right 
				 * place, because google plus share intent doesn't return right request code in onActivityResult() 
				 * method.
				 *//*
				Log.d(TAG, "current frag tag = " + currentContentFragmentTag);
				fragment = getSupportFragmentManager().findFragmentByTag(currentContentFragmentTag);
				if (fragment != null) {
					fragment.onActivityResult(requestCode, resultCode, data);
				}
				
			} else {*/
				// pass it to the fragments
				super.onActivityResult(requestCode, resultCode, data);
			//}
			break;
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
		outState.putString(BundleKeys.CURRENT_CONTENT_FRAGMENT_TAG, currentContentFragmentTag);
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
	
	@Override
	public void onBackPressed() {
		if (isTaskRoot()) {
			/**
			 * Here if we allow back press (super.onBackPressed();) then it can display unexpected result 
			 * in following case:
			 * 
			 * Let's say user was browsing eventseeker app on bosch connected mode & finally was looking at 
			 * event details screen. After this he disconnnects from bosch. So there are these bosch version app 
			 * screens lying in the backstack. In this case pressing back button beyond the first screen 
			 * of android version app, pops up those bosch version screens from back stack on android device.
			 */
			moveTaskToBack(true);
			
		} else {
			super.onBackPressed();
		}
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
	
	protected void setDrawerLockMode(boolean lock) {
		if (lock) {
			mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
			
		} else {
			mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
		}
	}
	
	protected void addFragment(int containerViewId, Fragment fragment, String tag, boolean addToBackStack) {
		FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
		fragmentTransaction.add(containerViewId, fragment, tag);
		if (addToBackStack) {
			fragmentTransaction.addToBackStack(null);
		}
		fragmentTransaction.commit();
		currentContentFragmentTag = tag;
	}
	
	@Override
	public void onOtherActivityDestroyed() {
		//Log.d(TAG, "is root onOtherActivityDestroyed = " + isTaskRoot());
		mDrawerToggle.setDrawerIndicatorEnabled(isTaskRoot());
	}
	
	protected abstract String getScrnTitle();
}
