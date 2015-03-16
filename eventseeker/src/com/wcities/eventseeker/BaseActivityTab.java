package com.wcities.eventseeker;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.wcities.eventseeker.DrawerListFragmentTab.DrawerListFragmentTabListener;
import com.wcities.eventseeker.SettingsFragmentTab.OnSettingsItemClickedListener;
import com.wcities.eventseeker.analytics.GoogleAnalyticsTracker;
import com.wcities.eventseeker.analytics.IGoogleAnalyticsTracker;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.constants.Enums.SettingsItem;
import com.wcities.eventseeker.interfaces.ActivityDestroyedListener;
import com.wcities.eventseeker.interfaces.OnLocaleChangedListener;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.GPlusUtil;

/**
 * Using ActionBarActivity (extended by BaseActivity) instead of Activity so as to use support library toolbar as actionbar even for lower apis
 * by calling setSupportActionBar(toolbar) & also there is common code to both mobile & tablet which can be kept in BaseActivity
 */
public abstract class BaseActivityTab extends BaseActivity implements IGoogleAnalyticsTracker, ActivityDestroyedListener, 
		DrawerListFragmentTabListener, OnLocaleChangedListener, OnSettingsItemClickedListener {
	
	private static final String TAG = BaseActivityTab.class.getSimpleName(); 
	
	protected static final int INDEX_NAV_ITEM_DISCOVER = 0;
	protected static final int INDEX_NAV_ITEM_MY_EVENTS = INDEX_NAV_ITEM_DISCOVER + 1;
	protected static final int INDEX_NAV_ITEM_FOLLOWING = INDEX_NAV_ITEM_MY_EVENTS + 1;
	protected static final int INDEX_NAV_ITEM_ARTISTS_NEWS = INDEX_NAV_ITEM_FOLLOWING + 1;
	protected static final int INDEX_NAV_ITEM_FRIENDS_ACTIVITY = INDEX_NAV_ITEM_ARTISTS_NEWS + 1;
	protected static final int INDEX_NAV_ITEM_SETTINGS = DrawerListFragment.DIVIDER_POS + 1;
	protected static final int INDEX_NAV_ITEM_LOG_OUT = INDEX_NAV_ITEM_SETTINGS + 1;

	private Toolbar toolbar;
	private LinearLayout lnrLayoutRootNavDrawer;
	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle mDrawerToggle;
	
	protected String currentContentFragmentTag;
	
	protected boolean isOnCreateCalledFirstTime = true;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Log.d(TAG, "onCreate()");
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
			if (GPlusUtil.isGPlusPublishPending) {
				/**
				 * This check is required to direct onActivityResult() calls from this activity & handle it at right 
				 * place, because google plus share intent doesn't return right request code in onActivityResult() 
				 * method.
				 */
				Log.d(TAG, "current frag tag = " + currentContentFragmentTag);
				Fragment fragment = getSupportFragmentManager().findFragmentByTag(currentContentFragmentTag);
				if (fragment != null) {
					fragment.onActivityResult(requestCode, resultCode, data);
				}
				
			} else {
				// pass it to the fragments
				super.onActivityResult(requestCode, resultCode, data);
			}
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
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (mDrawerLayout.isDrawerOpen(lnrLayoutRootNavDrawer)) {
				mDrawerLayout.closeDrawer(lnrLayoutRootNavDrawer);
				return true;
				
			} else {
				//Log.d(TAG, "super.onKeyDown()");
				return super.onKeyDown(keyCode, event);
			}
		}
		return super.onKeyDown(keyCode, event);
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
	
	private void addDrawerListFragment() {
		//Log.d(TAG, "addDrawerListFragment");
		FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
		DrawerListFragmentTab drawerListFragmentTab = new DrawerListFragmentTab();
		fragmentTransaction.add(R.id.rootNavigationDrawer, drawerListFragmentTab, FragmentUtil.getTag(drawerListFragmentTab));
		fragmentTransaction.commit();
	}
	
	private void selectItem(int position, Bundle args) {
		//Log.d(TAG, "selectItem() + pos : " + position);
		Intent intent = null;
		switch (position) {
	    
		case INDEX_NAV_ITEM_DISCOVER:
			//DiscoverParentFragment discoverFragment;
			intent = new Intent(getApplicationContext(), DiscoverActivityTab.class);
			break;			
			
		case INDEX_NAV_ITEM_SETTINGS:
			//SettingsFragment SettingsFragment;
			intent = new Intent(getApplicationContext(), SettingsActivityTab.class);
			break;
			
		case INDEX_NAV_ITEM_LOG_OUT:
			EventSeekr eventSeekr = (EventSeekr) getApplication();
			if (eventSeekr.getFbUserId() != null) {
				FbUtil.callFacebookLogout(eventSeekr);
				
			} else if (eventSeekr.getGPlusUserId() != null) {
				GPlusUtil.callGPlusLogout(EventSeekr.mGoogleApiClient, eventSeekr);
				
			} else if (eventSeekr.getFirstName() != null) {
				eventSeekr.removeEmailSignupInfo();
				
			} else if (eventSeekr.getEmailId() != null) {
				eventSeekr.removeEmailLoginInfo();
			}

			intent = new Intent(getApplicationContext(), LauncherActivityTab.class);
			break;
		default:
			break;
		}
		if (intent != null) {
			if (args != null) {
				intent.putExtras(args);
			}
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
			startActivity(intent);
		    
		    mDrawerLayout.closeDrawer(lnrLayoutRootNavDrawer);
		}
	}
	
	protected void setCommonUI() {
		//Log.d(TAG, "setCommonUI()");
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
		
		DrawerListFragmentTab drawerListFragmentTab = (DrawerListFragmentTab) getSupportFragmentManager()
				.findFragmentByTag(FragmentUtil.getTag(DrawerListFragmentTab.class));
		//Log.d(TAG, "drawerListFragmentTab : " + drawerListFragmentTab);
		if (drawerListFragmentTab == null) {
			addDrawerListFragment();
		}
		getSupportFragmentManager().executePendingTransactions();
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
	
	protected void removeToolbarElevation() {
		ViewCompat.setElevation(toolbar, 0);
	}
	
	protected void addFragment(int containerViewId, Fragment fragment, String tag, boolean addToBackStack) {
		FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
		fragmentTransaction.add(containerViewId, fragment, tag);
		if (addToBackStack) {
			fragmentTransaction.addToBackStack(null);
		}
		/**
		 * 10-03-2015:
		 * 'commitAllowingStateLoss()' is called instead of 'commit()' as if this call gets triggered at 
		 * the same moment if user presses 'Home' button of device then the app crashes with 
		 * 'IllegalStateException'. This happened once but since that this wasn't getting reproduced. So,
		 * avoid such scenarios used 'commitAllowingStateLoss()'
		 */
		fragmentTransaction.commitAllowingStateLoss();
		currentContentFragmentTag = tag;
	}
	
	protected void replaceFragment(int containerViewId, Fragment fragment, String tag, boolean addToBackStack) {
		FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
		fragmentTransaction.replace(containerViewId, fragment, tag);
		if (addToBackStack) {
			fragmentTransaction.addToBackStack(null);
		}
		/**
		 * 10-03-2015:
		 * 'commitAllowingStateLoss()' is called instead of 'commit()' as if this call gets triggered at 
		 * the same moment if user presses 'Home' button of device then the app crashes with 
		 * 'IllegalStateException'. This happened once but since that this wasn't getting reproduced. So,
		 * avoid such scenarios used 'commitAllowingStateLoss()'
		 */
		fragmentTransaction.commitAllowingStateLoss();
		currentContentFragmentTag = tag;
	}
	
	@Override
	public void onOtherActivityDestroyed() {
		//Log.d(TAG, "is root onOtherActivityDestroyed = " + isTaskRoot());
		mDrawerToggle.setDrawerIndicatorEnabled(isTaskRoot());
	}
	
	@Override
	public void onDrawerListFragmentViewCreated() {
		if (getDrawerItemPos() != AppConstants.INVALID_INDEX) {
			DrawerListFragmentTab drawerListFragmentTab = (DrawerListFragmentTab) getSupportFragmentManager()
					.findFragmentByTag(FragmentUtil.getTag(DrawerListFragmentTab.class));
			drawerListFragmentTab.getListView().setItemChecked(getDrawerItemPos(), true);
		}
	}
	
	@Override
	public void onDrawerItemSelected(int pos, Bundle args) {
		/**
		 * process only if 
		 * 1) different selection is made or 
		 * 2) recommended tab is supposed to be selected by default;
		 * otherwise just close the drawer
		 */
		if (getDrawerItemPos() != pos || (args != null && args.containsKey(BundleKeys.SELECT_RECOMMENDED_EVENTS))) {
			selectItem(pos, args);

		} else {
			mDrawerLayout.closeDrawer(lnrLayoutRootNavDrawer);
		}
	}
	
	@Override
	public void onLocaleChanged() {
		DrawerListFragmentTab drawerListFragmentTab = (DrawerListFragmentTab) getSupportFragmentManager()
				.findFragmentByTag(FragmentUtil.getTag(DrawerListFragmentTab.class));
		if (drawerListFragmentTab != null) {
			drawerListFragmentTab.refreshDrawerList();
		}
		// TODO: shift these lines into language settings activity
		/**
		 * refresh the current screen's title only if it is Language fragment.
		 */
		/*if (currentContentFragmentTag.equals(AppConstants.FRAGMENT_TAG_LANGUAGE)) {
			mTitle = getResources().getString(R.string.title_language);
			updateTitle();
		}*/
		/**
		 * 	refresh the SearchView	
		 */
		//searchView.setQueryHint(getResources().getString(R.string.menu_search));
	}
	
	protected boolean isDrawerOpen() {
		return mDrawerLayout.isDrawerOpen(lnrLayoutRootNavDrawer);
	}
	
	protected abstract String getScrnTitle();
	
	@Override
	public void onSettingsItemClicked(SettingsItem settingsItem, Bundle args) {
		switch (settingsItem) {
		
			case SYNC_ACCOUNTS:
		    	Intent intent = new Intent(getApplicationContext(), ConnectAccountsActivityTab.class);
		    	startActivity(intent);
		    	break;
		
			case INVITE_FRIENDS:
				inviteFriends();
				break;
				
			case RATE_APP:
				rateApp();
				break;
			
			default:
				break;
		}
	}
}
