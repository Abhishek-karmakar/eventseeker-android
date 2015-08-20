package com.wcities.eventseeker.bosch;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bosch.myspin.serversdk.MySpinException;
import com.bosch.myspin.serversdk.MySpinServerSDK;
import com.bosch.myspin.serversdk.OnCarDataChangeListener;
import com.bosch.myspin.serversdk.PhoneCallStateListener;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.app.EventSeekr.ProximityUnit;
import com.wcities.eventseeker.bosch.BoschDrawerListFragment.BoschDrawerListFragmentListener;
import com.wcities.eventseeker.bosch.interfaces.BoschEditTextListener;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.interfaces.ArtistListener;
import com.wcities.eventseeker.interfaces.ConnectionFailureListener;
import com.wcities.eventseeker.interfaces.EventListener;
import com.wcities.eventseeker.interfaces.FragmentLoadedFromBackstackListener;
import com.wcities.eventseeker.interfaces.ReplaceFragmentListener;
import com.wcities.eventseeker.interfaces.VenueListener;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.viewdata.SharedElement;

import java.util.List;

public class BoschMainActivity extends ActionBarActivity implements ReplaceFragmentListener, 
		EventListener, ArtistListener, VenueListener, FragmentLoadedFromBackstackListener, 
		BoschDrawerListFragmentListener, ConnectionFailureListener, MySpinServerSDK.ConnectionStateListener {

	private static final String TAG = BoschMainActivity.class.getSimpleName();

	protected static final int INDEX_NAV_ITEM_HOME = 0;
	protected static final int INDEX_NAV_ITEM_CHANGE_CITY = 1;
	protected static final int INDEX_NAV_ITEM_SEARCH = 2;
	protected static final int INDEX_NAV_ITEM_FAVORITES = 3;

	private static final int MILLIS_TO_CHK_BOSCH_STOPPED = 200;

	public static int appTaskId = AppConstants.INVALID_ID;

	private LinearLayout lnrLayoutRootNavDrawer;
	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle mDrawerToggle;

	private String currentContentFragmentTag, mTitle;
	private int drawerItemSelectedPosition = AppConstants.INVALID_INDEX;

	private TextView txtActionBarTitle;
	private FrameLayout frmLayoutContentFrame;
	private AlertDialog dialog;

	private Runnable periodicCheckForBoschConnection;
	private android.os.Handler handler;

	private boolean onStopCalled;
	
	private PhoneCallStateListener phoneCallStateListener = new PhoneCallStateListener() {
		
		@Override
		public void onPhoneCallStateChanged(int arg0) {
			//Toast.makeText(BoschMainActivity.this, "onPhoneCallStateChanged() - arg0 = " + arg0, Toast.LENGTH_SHORT).show();
			//Log.d(TAG, "onPhoneCallStateChanged() - arg0 = " + arg0);
			if (arg0 == phoneCallStateListener.PHONECALLSTATE_REJECTED ||
					arg0 == phoneCallStateListener.PHONECALLSTATE_ENDED) {
				selfStart();
			}
		}
	};

	private BroadcastReceiver keyboardVisibilityStatusBR = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Fragment fragment = getSupportFragmentManager().findFragmentByTag(currentContentFragmentTag);
			if (fragment instanceof OnKeyboardVisibilityStateChangedListener) {						
				((OnKeyboardVisibilityStateChangedListener) fragment).onKeyboardVisibilityStateChanged(
						intent.getBooleanExtra(MySpinServerSDK.EXTRA_KEYBOARD_VISIBILITY, false));
			}
		}
	};

	public interface OnCarStationaryStatusChangedListener {
		public void onCarStationaryStatusChanged(boolean isStationary);
	}
	
	public interface OnDisplayModeChangedListener {
		public void onDisplayModeChanged(boolean isNightModeEnabled);
	}

	public interface OnKeyboardVisibilityStateChangedListener {
		public void onKeyboardVisibilityStateChanged(boolean isKeyboardVisible);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Log.d(TAG, "onCreate() BoschActivity, taskId = " + getTaskId());
		//Toast.makeText(getApplicationContext(), "onCreate() BoschActivity, taskId = " + getTaskId(), Toast.LENGTH_SHORT).show();

		setContentView(R.layout.activity_bosch_main);

		handler = new android.os.Handler(Looper.getMainLooper());

		/**
		 * Note: the below try catch is added to avoid crash, When User is connected to Bosch then 
		 * sometime app crashes with following Exception:
		 * Caused by: java.lang.NullPointerException
		 * at com.bosch.myspin.serversdk.MySpinServerSDK.registerForPhoneCallStateEvents(SourceFile:494)
		 */
		try {
			MySpinServerSDK.sharedInstance().registerForPhoneCallStateEvents(phoneCallStateListener);
			
		} catch (NullPointerException e) {
			e.printStackTrace();

		} catch (MySpinException e) {
			e.printStackTrace();
		}

		EventSeekr.setConnectionFailureListener(this);
		
		initializeCurrentProximityUnitForBosch();
			
		lnrLayoutRootNavDrawer = (LinearLayout) findViewById(R.id.rootNavigationDrawerBosch);
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerToggle = new ActionBarDrawerToggle(this, // host this
				mDrawerLayout, // DrawerLayout object
				R.drawable.slctr_btn_side_nav,//ic_nav_drawer_off, // nav drawer icon to replace 'Up' caret
				R.string.drawer_open, // "open drawer" description
				R.string.drawer_close // "close drawer" description
		);

		mDrawerLayout.setDrawerListener(mDrawerToggle);

		getSupportActionBar().setDisplayOptions( ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_HOME
			| ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_CUSTOM);
		/**
		 * setIcon null throws NullPointerException while expanding
		 * searchView in SearchFragment. So need to set any transparent icon
		 * rather than null.
		 */
		
		getSupportActionBar().setIcon(R.drawable.placeholder);
		getSupportActionBar().setCustomView(R.layout.bosch_actionbar_titleview);
		
		if (savedInstanceState != null) {
			currentContentFragmentTag = savedInstanceState.getString(BundleKeys.CURRENT_CONTENT_FRAGMENT_TAG);
		}

		txtActionBarTitle = (TextView) findViewById(R.id.txtActionBarTitle);
		
		BoschDrawerListFragment boschDrawerListFragment = (BoschDrawerListFragment) getSupportFragmentManager()
			.findFragmentByTag(BoschDrawerListFragment.class.getSimpleName());
		if (boschDrawerListFragment == null) {
			addDrawerListFragment();
		}
		getSupportFragmentManager().executePendingTransactions();
		
		frmLayoutContentFrame = (FrameLayout) findViewById(R.id.content_frame);
		
		if (currentContentFragmentTag == null) {
			selectItem(INDEX_NAV_ITEM_HOME);
		}
		updateColors();
	}

	private void initializeCurrentProximityUnitForBosch() {
		EventSeekr eventSeeker = (EventSeekr) getApplication();
		
		ProximityUnit previousProximityUnit = eventSeeker.getSavedProximityUnit();
		ProximityUnit currentProximityUnit = eventSeeker.getCurrentProximityUnit();
		
		if (currentProximityUnit != previousProximityUnit) {
			/*int convertedSearchedDistance = eventSeeker.getSearchDistance();
			if (currentProximityUnit == ProximityUnit.MI) {
				convertedSearchedDistance = ProximityUnit.convertKmToMi(convertedSearchedDistance);
			} else {
				convertedSearchedDistance = ProximityUnit.convertMiToKm(convertedSearchedDistance);				
			}
		
			eventSeeker.setSearchDistance(convertedSearchedDistance);*/
			eventSeeker.updateSavedProximityUnit(currentProximityUnit);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		//Log.d(TAG, "onResume() BoschActivity");
		//Toast.makeText(getApplicationContext(), "onResume() BoschActivity connected = " + MySpinServerSDK.sharedInstance().isConnected(), Toast.LENGTH_SHORT).show();
		try {
			/**
			 * This will set Locale to English just for Bosch System. This is done as if in Mobile app the Locale 
			 * set to be different then some string which are common for Mobile app and Bosch app will appear in 
			 * Language as per the default Locale set in Mobile app which shouldn't be happened.
			 */
			((EventSeekr) getApplication()).updateLocaleForBosch();

			MySpinServerSDK.sharedInstance().registerCarDataChangedListener(new OnCarDataChangeListener() {
				
				@Override
				public void onLocationUpdate(Location arg0) {}
				
				@Override
				public void onDayNightModeChanged(boolean isNightModeEnabled) {
					AppConstants.IS_NIGHT_MODE_ENABLED = isNightModeEnabled;					
					updateColors();
					
					Fragment fragment = getSupportFragmentManager().findFragmentByTag(currentContentFragmentTag);
					if (fragment instanceof OnDisplayModeChangedListener) {
						((OnDisplayModeChangedListener) fragment).onDisplayModeChanged(isNightModeEnabled);
					}
					
					//Log.i(TAG, "IS_NIGHT_MODE_ENABLED : " + AppConstants.IS_NIGHT_MODE_ENABLED);	
				}

				@Override
				public void onCarStationaryStatusChanged(boolean isCarStationary) {
					AppConstants.IS_CAR_STATIONARY = isCarStationary;					

					Fragment fragment = getSupportFragmentManager().findFragmentByTag(currentContentFragmentTag);
					if (fragment instanceof OnCarStationaryStatusChangedListener) {						
						((OnCarStationaryStatusChangedListener) fragment).onCarStationaryStatusChanged(isCarStationary);
					}
					
					//Log.i(TAG, "IS_CAR_STATIONARY : " + AppConstants.IS_CAR_STATIONARY);
				}
				
			});
			
			/**
			 * calling 'updateColors' to set the apps UI in Day or Night mode.
			 */
			updateColors();
			
		} catch (MySpinException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		//Log.d(TAG, "onStart() BoschActivity");
		//Toast.makeText(getApplicationContext(), "onStart() BoschActivity", Toast.LENGTH_SHORT).show();

		onStopCalled = false;
		// When this activity gets started register for mySPIN connection events in
		// order to adapt views for the according connection state.
		try {
			MySpinServerSDK.sharedInstance().registerConnectionStateListener(this);

		} catch (MySpinException e) {
			e.printStackTrace();
		}

		registerReceiver(keyboardVisibilityStatusBR, new IntentFilter(MySpinServerSDK.EVENT_KEYBOARD_VISIBILITY_CHANGED));
		
		DeviceUtil.registerLocationListener(this);
		EventSeekr.setConnectionFailureListener(this);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		//Log.d(TAG, "onStop() BoschActivity");
		//Toast.makeText(getApplicationContext(), "onStop() BoschActivity", Toast.LENGTH_SHORT).show();
		onStopCalled = true;
		// When this activity gets stopped unregister for mySPIN connection events.
		try {
			MySpinServerSDK.sharedInstance().unregisterConnectionStateListener(this);

		} catch (MySpinException e) {
			e.printStackTrace();
		}
		//MySpinServerSDK.sharedInstance().unregisterBlockStatusListener();
		unregisterReceiver(keyboardVisibilityStatusBR);
		DeviceUtil.unregisterLocationListener((EventSeekr) getApplication());
		EventSeekr.resetConnectionFailureListener(this);
		((EventSeekr) getApplication()).resetDefaultLocale();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(BundleKeys.CURRENT_CONTENT_FRAGMENT_TAG, currentContentFragmentTag);
	}
	
	@Override
	protected void onDestroy() {
		//Log.d(TAG, "onDestroy() BoschActivity");
		//Toast.makeText(getApplicationContext(), "onDestroy() BoschActivity", Toast.LENGTH_SHORT).show();
		EventSeekr.resetConnectionFailureListener(this);
		/**
		 * sometime app crashes with following Exception:
		 * Caused by: java.lang.NullPointerException
		 * at com.bosch.myspin.serversdk.MySpinServerSDK.unregisterForPhoneCallStateEvents(SourceFile:634)
		 */
		try {
			MySpinServerSDK.sharedInstance().unregisterForPhoneCallStateEvents();

		} catch (NullPointerException e) {
			e.printStackTrace();
		}
		try {
			// calling 'super.onDestroy()' in try catch as some times it gives no view found error while destroying
			// and then the app gets crashed. So, the try catch catches the exception and helps to continue the 
			// application without crashing.
			super.onDestroy();
			
		} catch (Exception e) {
			Log.e(TAG, "Error Destroying Activity : " + e.toString());
		}
	}

	@Override
	public void onConnectionStateChanged(boolean isConnected) {
		//Log.d(TAG, "onConnectionStateChanged() isConnected = " + isConnected + ", appTaskId = " + appTaskId);
		//Toast.makeText(getApplicationContext(), "onConnectionStateChanged() isConnected = " + isConnected + ", appTaskId = " + appTaskId, Toast.LENGTH_SHORT).show();
		if (!isConnected) {
			handleNotConnected();
		}
	}

	private void handleNotConnected() {
		bringTaskToFront(appTaskId);

		/**
		 * for some slow devices like galaxy 10" tab, moveTaskToFront() doesn't work resulting
		 * in bosch screen being displayed on android device, hence following periodic check is added
		 */
		periodicCheckForBoschConnection = new Runnable() {

			@Override
			public void run() {
				//Toast.makeText(getApplicationContext(), "handler onStopCalled = " + onStopCalled, Toast.LENGTH_SHORT).show();
				if (!onStopCalled) {
					bringTaskToFront(appTaskId);
					handler.postDelayed(this, MILLIS_TO_CHK_BOSCH_STOPPED);
				}
			}
		};
		handler.postDelayed(periodicCheckForBoschConnection, MILLIS_TO_CHK_BOSCH_STOPPED);
	}

	private void bringTaskToFront(int taskId) {
		ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		/**
		 * Q: Why have we used MOVE_TASK_WITH_HOME flag?
		 * A: When user is connected to bosch if device gets some other third party app started (for example get a call
		 * covering entire screen), then bosch gets disconnected. After this third party app finishes,
		 * mobile/tablet app starts from back stack, but if we go on pressing back button until we reach first screen &
		 * then on first screen of mobile/tablet app pressing back once more, it starts displaying task holding
		 * this bosch activity on mobile/tablet.
		 * To prevent this, we use MOVE_TASK_WITH_HOME flag so as to bring back task containing mobile/tablet activities
		 * along with "home" activity, such that "home" is positioned immediately behind the mobile/tablet app task
		 * & hence pressing back in above mentioned case won't display bosch activity on mobile, instead it
		 * would land us to home screen as expected.
		 */
		if (taskId != AppConstants.INVALID_ID) {
			activityManager.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_WITH_HOME);

		} else {
			moveTaskToBack(true);
		}
	}
	
	private void updateColors() {
		if (AppConstants.IS_NIGHT_MODE_ENABLED) {
			frmLayoutContentFrame.setBackgroundColor(getResources().getColor(android.R.color.black));
			
			getSupportActionBar().setBackgroundDrawable(
				getResources().getDrawable(R.drawable.ic_action_bar_night_mode));

			txtActionBarTitle.setTextColor(getResources().getColor(android.R.color.white));
			
		} else {		
			frmLayoutContentFrame.setBackgroundColor(getResources().getColor(android.R.color.white));
			
			getSupportActionBar().setBackgroundDrawable(
				getResources().getDrawable(R.drawable.ic_action_bar));
			
			txtActionBarTitle.setTextColor(getResources().getColor(R.color.eventseeker_bosch_theme_grey));
		}
		
		/**
		 * Following 2 statements are required due to ICS bug, which doesn't update actionbar background 
		 * otherwise via setBackgroundDrawable() called above.
		 */
		getSupportActionBar().setTitle(".");
		getSupportActionBar().setTitle("");
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Log.d(TAG, "onPostCreate");
		// Sync the toggle state after onRestoreInstanceState has occurred.
		if (mDrawerToggle != null) {
			mDrawerToggle.syncState();
		}
	}	
	
	/** Swaps fragments in the main content view */
	private void selectItem(int position) {
		//Log.d(TAG, "selectItem(), pos = " + position);
		
		if (position != INDEX_NAV_ITEM_CHANGE_CITY || AppConstants.IS_CAR_STATIONARY) {
			// change city is not allowed while driving
			drawerItemSelectedPosition = position;
			setDrawerIndicatorEnabled(true);
		}
			
		BoschDrawerListFragment boschDrawerListFragment = (BoschDrawerListFragment) getSupportFragmentManager()
			.findFragmentByTag(BoschDrawerListFragment.class.getSimpleName());
		
		if (boschDrawerListFragment == null) {
			return;
		}
		
	    switch (position) {
	    
		case INDEX_NAV_ITEM_HOME:
			BoschHomeFragment boschHomeFragment = new BoschHomeFragment();
			replaceContentFrameByFragment(boschHomeFragment, false);
			break;
			
		case INDEX_NAV_ITEM_CHANGE_CITY:
			if (!AppConstants.IS_CAR_STATIONARY) {
				showBoschDialog(R.string.dialog_city_cannot_be_changed_while_driving);
			
			} else {
				BoschChangeCityFragment boschChangeCityFragment = new BoschChangeCityFragment();
				replaceContentFrameByFragment(boschChangeCityFragment, false);
			}
			break;
			
		case INDEX_NAV_ITEM_SEARCH:
			BoschSearchFragment boschSearchfragment = new BoschSearchFragment();
			replaceContentFrameByFragment(boschSearchfragment, false);
			break;
			
		case INDEX_NAV_ITEM_FAVORITES:
			BoschFavoritesFragment boschFavoritesFragment = new BoschFavoritesFragment();
			replaceContentFrameByFragment(boschFavoritesFragment, false);
			break;
	    }
	    
    	mDrawerLayout.closeDrawer(lnrLayoutRootNavDrawer);
	}

	private void addDrawerListFragment() {
		// Log.d(TAG, "addDrawerListFragment");
		FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
		BoschDrawerListFragment boschDrawerListFragment = new BoschDrawerListFragment();
		fragmentTransaction.add(R.id.rootNavigationDrawerBosch, boschDrawerListFragment, 
				BoschDrawerListFragment.class.getSimpleName());
		fragmentTransaction.commit();
	}

	private void setDrawerIndicatorEnabled(boolean enable) {
		// Log.d(TAG, "setDrawerIndicatorEnabled");
		if (mDrawerToggle != null) {
			mDrawerToggle.setDrawerIndicatorEnabled(enable);
		}
	}
	
	/**
	 * for updating the action bar title from within the Fragment(Just used in Bosch related fragments)
	 */
	public void replaceContentFrameByFragment(Fragment replaceBy, boolean addToBackStack) {
		FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
		
		String fragmentTag = replaceBy.getClass().getSimpleName();
		fragmentTransaction.replace(R.id.content_frame, replaceBy, fragmentTag);
		
		if (addToBackStack) {
			fragmentTransaction.addToBackStack(null);
		}
		fragmentTransaction.commitAllowingStateLoss();

		currentContentFragmentTag = fragmentTag;

		/**
		 * For fragments not having setHasOptionsMenu(true),
		 * onPrepareOptionsMenu() is not called on adding/replacing such
		 * fragments. But if user visits any such fragment by selecting it from
		 * drawer initially when just fbLoginFragment is visible (for which
		 * search action item is disabled from onPrepareOptionsMenu()), then
		 * these menus' visibility don't change due to onPrepareOptionsMenu()
		 * not being called up. Hence the following code.
		 */
		invalidateOptionsMenu();
	}
	
	public void updateTitleForFragment(String newTitle, String fragmentTag) {
		if (newTitle != null && currentContentFragmentTag.equals(fragmentTag)) {
			mTitle = newTitle;
			txtActionBarTitle.setText(mTitle);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Log.d(TAG, "onCreateOptionsMenu");
		getMenuInflater().inflate(R.menu.bosch_main, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
        // Handle your other action bar items...
		//Log.d(TAG, "onOptionsItemSelected");
		//Toast.makeText(this, "onOptionsItemSelected()", Toast.LENGTH_SHORT).show();
		switch (item.getItemId()) {

		case android.R.id.home:
			if (mDrawerToggle != null && mDrawerToggle.isDrawerIndicatorEnabled()) {
				if (mDrawerLayout.isDrawerOpen(lnrLayoutRootNavDrawer)) {
					mDrawerLayout.closeDrawer(lnrLayoutRootNavDrawer);

				} else {
					if (currentContentFragmentTag != null) {
						Fragment fragment = getSupportFragmentManager().findFragmentByTag(currentContentFragmentTag);
						if (fragment instanceof BoschEditTextListener && 
								((BoschEditTextListener)fragment).getEditText() != null) {
							((BoschEditTextListener)fragment).getEditText().clearFocus();
						}
					}
					mDrawerLayout.openDrawer(lnrLayoutRootNavDrawer);
				}

			} else {
				onBackPressed();
			}
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		//Toast.makeText(this, "onKeyDown() - keycode = " + keyCode, Toast.LENGTH_SHORT).show();
		if (keyCode == KeyEvent.KEYCODE_BACK) {
	        return true;
	    }		
		return super.onKeyDown(keyCode, event);
	}
    
	private void selectNonDrawerItem(Fragment replaceBy, boolean addToBackStack) {
		// Log.d(TAG, "selectNonDrawerItem");
		drawerItemSelectedPosition = AppConstants.INVALID_INDEX;
		setDrawerIndicatorEnabled(!addToBackStack);
		replaceContentFrameByFragment(replaceBy, addToBackStack);
	}
	
	@Override
	public void onDrawerItemSelected(int pos) {
		// Log.d(TAG, "onDrawerItemSelected");
		// process only if different selection is made, otherwise just close the drawer.
		if (drawerItemSelectedPosition != pos) {
			getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			selectItem(pos);

		} else {
			mDrawerLayout.closeDrawer(lnrLayoutRootNavDrawer);
		}
	}


	@Override
	public void onFragmentResumed(Fragment fragment, int drawerPosition, String actionBarTitle) {
		// Log.d(TAG, "onFragmentResumed(int position, String title, String fragmentTag)");
		drawerItemSelectedPosition = drawerPosition;
		if (drawerItemSelectedPosition != AppConstants.INVALID_INDEX) {
			setDrawerIndicatorEnabled(true);
			
		} else {
			setDrawerIndicatorEnabled(false);
		}
		currentContentFragmentTag = fragment.getClass().getSimpleName();
		updateTitleForFragment(actionBarTitle, currentContentFragmentTag);
		// Log.d(TAG, "got the current tag as : " + fragmentTag);
	}
	
	@Override
	public void replaceByFragment(String fragmentTag, Bundle args) {
		Fragment fragment = null;
		boolean addToBackStack = true;
		
		if (fragmentTag.equals(BoschDiscoverFragment.class.getSimpleName())) {
			fragment = new BoschDiscoverFragment();

		} else if (fragmentTag.equals(BoschDiscoverByCategoryFragment.class.getSimpleName())) {
			fragment = new BoschDiscoverByCategoryFragment();
		
		} if (fragmentTag.equals(BoschInfoFragment.class.getSimpleName())) {
			fragment = new BoschInfoFragment();
		
		} if (fragmentTag.equals(BoschEventArtistsFragment.class.getSimpleName())) {
			fragment = new BoschEventArtistsFragment();
		
		} if (fragmentTag.equals(BoschArtistEventsFragment.class.getSimpleName())) {
			fragment = new BoschArtistEventsFragment();
			
		} if (fragmentTag.equals(BoschFeaturedEventsFragment.class.getSimpleName())) {
			fragment = new BoschFeaturedEventsFragment();
		
		} if (fragmentTag.equals(BoschSearchResultFragment.class.getSimpleName())) {
			fragment = new BoschSearchResultFragment();
			
		} if (fragmentTag.equals(BoschVenueEventsFragment.class.getSimpleName())) {
			fragment = new BoschVenueEventsFragment();

		} if (fragmentTag.equals(BoschNavigateFragment.class.getSimpleName())) {
			fragment = new BoschNavigateFragment();
			
		}
	
		if(fragment != null) {
			fragment.setArguments(args);
			selectNonDrawerItem(fragment, addToBackStack);
		}
		
	}
	
	public void showBoschDialog(int msgId) {
		showBoschDialog(getResources().getString(msgId));
	}

	public void showBoschDialog(String msg) {
		if (dialog != null && dialog.isShowing()) {
			return;
		}
		
		View view = LayoutInflater.from(this).inflate(R.layout.bosch_element_alert_dialog, null);

		((TextView)view.findViewById(R.id.txtTitle)).setText(msg);
		TextView textOk = (TextView) view.findViewById(R.id.textOk);

		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
		textOk.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
		dialog = alertDialog.setCustomTitle(view).setCancelable(false).create();
		/*Remove positive button because of UI issue, positive button come in right hand side
		* so added textview in center*/
		/*	.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.dismiss();
			}
		})*/
		dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

		try {
			MySpinServerSDK.sharedInstance().registerDialog(dialog);
		} catch (MySpinException e) {
			Log.e(TAG, "Error : " + e.toString());
			e.printStackTrace();
		}
		dialog.show();
	}
	
	public void dismissDialog() {
		if (dialog != null && dialog.isShowing()) {
			dialog.dismiss();
		}
	}
	
	private void selfStart() {
		if (MySpinServerSDK.sharedInstance().isConnected()) {
			//Log.d(TAG, "selfStart() BoschActivity");
			//Toast.makeText(getApplicationContext(), "selfStart() BoschActivity", Toast.LENGTH_SHORT).show();
			Intent intent = new Intent(getApplicationContext(), BoschMainActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(intent);
		}
	}

	@Override
	public void onEventSelected(Event event) {
		
		Bundle args = new Bundle();
		args.putSerializable(BundleKeys.EVENT, event);
		
		BoschEventDetailsFragment boscheventDetailsFragment = new BoschEventDetailsFragment();
		boscheventDetailsFragment.setArguments(args);
		
		selectNonDrawerItem(boscheventDetailsFragment, true);
		
	}

	@Override
	public void onArtistSelected(Artist artist) {
		Bundle args = new Bundle();
		args.putSerializable(BundleKeys.ARTIST, artist);

		BoschArtistDetailsFragment boschArtistDetailsFragment = new BoschArtistDetailsFragment();
		boschArtistDetailsFragment.setArguments(args);
		
		selectNonDrawerItem(boschArtistDetailsFragment, true);
	}

	@Override
	public void onVenueSelected(Venue venue) {
		Bundle args = new Bundle();
		args.putSerializable(BundleKeys.VENUE, venue);
		
		BoschVenueDetailsFragment boschVenueDetailsFragment = new BoschVenueDetailsFragment();
		boschVenueDetailsFragment.setArguments(args);
		
		selectNonDrawerItem(boschVenueDetailsFragment, true);		
	}

	@Override
	public void onFragmentResumed(Fragment fragment) {
		//This will get deprecated
	}
	
	@Override
	public void onBackPressed() {
		if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
			try {
				/**
				 * try catch is added to handle the IllegalStateException
				 */
				super.onBackPressed();
				
			} catch (IllegalStateException e) {
				e.printStackTrace();
			}
			
		} else {
			/**
			 * Here if we allow back press (super.onBackPressed();) then it can display unexpected result 
			 * in following 2 cases:
			 * 
			 * 1 - If orientation is changed before pressing back button when starting screen of app's bosch 
			 * version is visible, then pressing back button again displays the same screen rather than moving 
			 * out of app. Because in back stack we have MainActivity which restarts due to orientation change & 
			 * in turn restarting this BoschMainActivity as well.
			 * 
			 * 2 - Let's say user was browsing eventseeker app on android device & finally was looking at 
			 * event details screen. After this he connected to bosch. So there are these android version app 
			 * screens lying in the backstack. In this case pressing back button beyond the first screen 
			 * of bosch version app, pops up those android version screens from back stack on bosch IVI system.
			 */
			moveTaskToBack(true);
		}
	}

	@Override
	public void onConnectionFailure() {
		View view = LayoutInflater.from(this).inflate(R.layout.bosch_element_alert_dialog, null);
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
		dialog = alertDialog.setCustomTitle(view).setCancelable(false).create();

		((TextView)view.findViewById(R.id.txtTitle)).setText(getResources().getString(R.string.connection_lost));
		TextView textOk = (TextView) view.findViewById(R.id.textOk);
		textOk.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});

		/*	.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				onDrawerItemSelected(INDEX_NAV_ITEM_HOME);
				dialog.dismiss();
			}
		})*/

		dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

		try {
			MySpinServerSDK.sharedInstance().registerDialog(dialog);
		} catch (MySpinException e) {
			Log.e(TAG, "Error : " + e.toString());
			e.printStackTrace();
		}
		
		dialog.show();
	}

	public void setMarqueeEnabledActionBar(boolean isEnabled) {
		txtActionBarTitle.setSelected(isEnabled);
	}

	@Override
	public void onEventSelected(Event event, List<SharedElement> sharedElements) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onArtistSelected(Artist artist, List<SharedElement> sharedElements) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onVenueSelected(Venue venue, List<SharedElement> sharedElements) {
		// TODO Auto-generated method stub
		
	}
}
