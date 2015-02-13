package com.wcities.eventseeker;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Set;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Toast;

import com.bosch.myspin.serversdk.MySpinException;
import com.bosch.myspin.serversdk.MySpinServerSDK;
import com.ford.syncV4.proxy.SyncProxyALM;
import com.ford.syncV4.transport.TransportType;
import com.nineoldandroids.animation.ObjectAnimator;
import com.wcities.eventseeker.ChangeLocationFragment.ChangeLocationFragmentListener;
import com.wcities.eventseeker.ConnectAccountsFragment.ConnectAccountsFragmentListener;
import com.wcities.eventseeker.ConnectAccountsFragment.Service;
import com.wcities.eventseeker.DrawerListFragment.DrawerListFragmentListener;
import com.wcities.eventseeker.GeneralDialogFragment.DialogBtnClickListener;
import com.wcities.eventseeker.SettingsFragment.OnSettingsItemClickedListener;
import com.wcities.eventseeker.SettingsFragment.SettingsItem;
import com.wcities.eventseeker.api.UserInfoApi.LoginType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.applink.service.AppLinkService;
import com.wcities.eventseeker.bosch.BoschMainActivity;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Category;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.core.registration.Registration.RegistrationListener;
import com.wcities.eventseeker.gcm.GcmBroadcastReceiver.NotificationType;
import com.wcities.eventseeker.interfaces.ArtistListener;
import com.wcities.eventseeker.interfaces.ConnectionFailureListener;
import com.wcities.eventseeker.interfaces.CustomSharedElementTransitionDestination;
import com.wcities.eventseeker.interfaces.CustomSharedElementTransitionSource;
import com.wcities.eventseeker.interfaces.EventListener;
import com.wcities.eventseeker.interfaces.FragmentHavingFragmentInRecyclerView;
import com.wcities.eventseeker.interfaces.FragmentLoadedFromBackstackListener;
import com.wcities.eventseeker.interfaces.MapListener;
import com.wcities.eventseeker.interfaces.OnLocaleChangedListener;
import com.wcities.eventseeker.interfaces.ReplaceFragmentListener;
import com.wcities.eventseeker.interfaces.VenueListener;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FbUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.GPlusUtil;
import com.wcities.eventseeker.util.VersionUtil;
import com.wcities.eventseeker.util.ViewUtil;
import com.wcities.eventseeker.viewdata.SharedElement;

public class MainActivity extends ActionBarActivity implements
		DrawerListFragmentListener, OnLocaleChangedListener, OnSettingsItemClickedListener,
		ReplaceFragmentListener, EventListener, ArtistListener, VenueListener,
		FragmentLoadedFromBackstackListener, MapListener, RegistrationListener, 
		ConnectAccountsFragmentListener, SearchView.OnQueryTextListener,
		ChangeLocationFragmentListener, ConnectionFailureListener, DialogBtnClickListener {

	private static final String TAG = MainActivity.class.getSimpleName();

	public static final int INDEX_NAV_ITEM_DISCOVER = DrawerListFragment.SECT_1_HEADER_POS + 1;
	public static final int INDEX_NAV_ITEM_MY_EVENTS = INDEX_NAV_ITEM_DISCOVER + 1;
	public static final int INDEX_NAV_ITEM_FOLLOWING = INDEX_NAV_ITEM_MY_EVENTS + 1;
	public static final int INDEX_NAV_ITEM_ARTISTS_NEWS = INDEX_NAV_ITEM_FOLLOWING + 1;
	public static final int INDEX_NAV_ITEM_FRIENDS_ACTIVITY = INDEX_NAV_ITEM_ARTISTS_NEWS + 1;
	public static final int INDEX_NAV_ITEM_SETTINGS = DrawerListFragment.SECT_2_HEADER_POS + 1;
	public static final int INDEX_NAV_ITEM_LOG_OUT = INDEX_NAV_ITEM_SETTINGS + 1;
	
	private static final String DRAWER_LIST_FRAGMENT_TAG = "drawerListFragment";
	
	private static final String DIALOG_FRAGMENT_TAG_CONNECTION_LOST = "ConnectionLost";
	private static final int MIN_MILLIS_TO_CHK_BOSCH_CONNECTION = 500;

	private static MainActivity instance = null;
	private boolean activityOnTop, hasOtherActivityFinished;

	private DrawerLayout mDrawerLayout;
	private LinearLayout lnrLayoutRootNavDrawer;
	private ActionBarDrawerToggle mDrawerToggle;
	private MenuItem searchItem;
	private SearchView searchView;

	private String mTitle;
	private String currentContentFragmentTag;
	private int drawerItemSelectedPosition = AppConstants.INVALID_INDEX;
	private String searchQuery;
	
	private boolean isTabletAndInLandscapeMode;/** This will check whether current device is tablet and if it is in 
	Landscape mode, it is used for the side navigation List to be shown permanently in landscape mode and not in 
	portrait mode
	**/
	private boolean isDrawerIndicatorEnabled;
	private boolean isTablet;
	/** it will check whether current device is tablet and according to that we will 
	select same tab layout file for portrait and landscape mode**/
	
	private long timeIntervalInMillisToCheckForBoschConnection = MIN_MILLIS_TO_CHK_BOSCH_CONNECTION;
	private Runnable periodicCheckForBoschConnection;
	private Handler handler;

	private boolean isCalledFromTwitterSection;

	private View vStatusBar, vStatusBarLayered, vDrawerStatusBar;
	private Toolbar toolbar;
	
	//private List<SharedElement> sharedElements = new ArrayList<SharedElement>();
	private boolean exitAnimCalled;
	private int prevBackStackEntryCount;
	private CustomSharedElementTransitionSource prevCustomSharedElementTransitionSource;
	private Fragment prevFragment;
	
	public static MainActivity getInstance() {
		return instance;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Log.d(TAG, "onCreate()");
		super.onCreate(savedInstanceState);	
		
		if (!((EventSeekr)getApplication()).isTablet()) {
			setRequestedOrientation(Configuration.ORIENTATION_PORTRAIT);
		}
		setContentView(R.layout.activity_main);
		
		vStatusBar = findViewById(R.id.vStatusBar);
		
		if (VersionUtil.isApiLevelAbove18()) {
			int statusBarHeight = ViewUtil.getStatusBarHeight(getResources());
			
			LinearLayout.LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, statusBarHeight);
			vStatusBar.setLayoutParams(params);
			
			vStatusBarLayered = findViewById(R.id.vStatusBarLayered);
			FrameLayout.LayoutParams frmParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 
					statusBarHeight);
			vStatusBarLayered.setLayoutParams(frmParams);
			
			vDrawerStatusBar = findViewById(R.id.vDrawerStatusBar);
			vDrawerStatusBar.setLayoutParams(params);
			
		} else {
			vStatusBar.setVisibility(View.GONE);
		}
		
		/**
		 * Locale changes are Activity specific i.e. after the Activity gets destroyed, the Locale changes
		 * associated with that activity will also get destroyed. So, if Activity was destroyed due to
		 * configuration changes(like orientation change) then the Newer Activity will initialize itself with
		 * the Device specific Locale. So, each and every time when activity gets initialized it should
		 * also initialize its Locale from SharedPref.
		 */
		((EventSeekr) getApplication()).setDefaultLocale();
		
		VersionUtil.updateCheckes((EventSeekr) getApplication());

		//Log.d(TAG, "deviceId = " + DeviceUtil.getDeviceId((EventSeekr) getApplication()));
		
		try {
			MySpinServerSDK.sharedInstance().registerApplication(getApplication());
		} catch (MySpinException e) {
			e.printStackTrace();
		}
		
		if (EventSeekr.isConnectedWithBosch()) {
			startBoschMainActivity();
		}
		
		/**
		 * check whether the current device is Tablet and if it is in Landscape
		 * mode
		 */
		EventSeekr eventSeekr = ((EventSeekr) getApplication());
		eventSeekr.checkAndSetIfInLandscapeMode();
		isTabletAndInLandscapeMode = eventSeekr.isTabletAndInLandscapeMode();
		isTablet = eventSeekr.isTablet();
		
		//Log.d(TAG, "isTablet : " + isTablet); 
		/**
		 * if user moves away quickly to any other screen resulting in fragment
		 * replacement & if we are adding this fragment into backstack, then
		 * orientation change made now will result in getActivity() returning
		 * null for all fragments existing in back stack. To resolve this we
		 * don't use getActivity() or getParentFragment().getActivity() call
		 * directly from fragment; rather we keep activity as instance variable
		 * of all such fragments & we keep this reference updated in all the
		 * back stack fragments by below call.
		 */
		FragmentUtil.updateActivityReferenceInAllFragments(getSupportFragmentManager(), this);
		
		/**
		 * Used to notify shared element transition source fragment when it's popped from back stack, 
		 * because when we are using custom shared element transition we add fragment instead of replacing it
		 * so that transition looks proper with initially transparent background of new fragment & old fragment being 
		 * visible behind it. Due to adding fragment, no lifecycle methods will be called while popping it
		 * back from backstack & hence we need this manual callback handling.
		 */
		getSupportFragmentManager().addOnBackStackChangedListener(new OnBackStackChangedListener() {
			
			@Override
			public void onBackStackChanged() {
				int newBackStackEntryCount = getSupportFragmentManager().getBackStackEntryCount();
				//Log.d(TAG, "onBackStackChanged(), newBackStackEntryCount = " + newBackStackEntryCount);
				Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);

				if (newBackStackEntryCount < prevBackStackEntryCount) {
					// pop action
					if (fragment instanceof CustomSharedElementTransitionSource) {
						((CustomSharedElementTransitionSource) fragment).onPoppedFromBackStack();
						
					} else if (fragment instanceof FragmentHavingFragmentInRecyclerView) {
						/**
						 * not useful for now since both fragments implementing FragmentHavingFragmentInRecyclerView 
						 * are also implementing CustomSharedElementTransitionSource 
						 */
						((FragmentHavingFragmentInRecyclerView) fragment).onPoppedFromBackStackFHFIR();
					}
					
				} else {
					//Log.d(TAG, "onBackStackChanged() prevFragment = " + prevFragment);
					if (prevFragment instanceof CustomSharedElementTransitionSource) {
						//Log.d(TAG, "call onPushedToBackStack() manually");
						/**
						 * To prevent extra action items & keep right toolbar, statusbar colors on back press, when 
						 * user by mistake clicks "multiple times the same item"/"single time multiple items" at the same time.
						 * e.g. - 
						 * 1) On discover screen, pressing 2 events simultaneously opens up 2 event details screens
						 * with 2 share action items (since 1st event details screen's onPushedToBackStack() was not called up) 
						 * & then pressing back once changes toolbar color to blue instead of transparent, because first event 
						 * details screen's onPushedToBackStack() was not called up resulting in no action from its onPoppedFromBackStack()
						 * 2) On event details screen clicking venue name multiple times, sometimes it opens up multiple 
						 * venue details screen instances.
						 * 3) Opening event/venue details screen from notification/widget
						 */
						((CustomSharedElementTransitionSource) prevFragment).onPushedToBackStack();
						
					} else if (prevFragment instanceof FragmentHavingFragmentInRecyclerView) {
						/**
						 * not useful for now since both fragments implementing FragmentHavingFragmentInRecyclerView 
						 * are also implementing CustomSharedElementTransitionSource 
						 */
						((FragmentHavingFragmentInRecyclerView) fragment).onPushedToBackStackFHFIR();
					}
				}
				prevBackStackEntryCount = newBackStackEntryCount;
				prevFragment = fragment;
			}
		});

		toolbar = (Toolbar) findViewById(R.id.toolbarForActionbar);
	    setSupportActionBar(toolbar);
	    
	    if (VersionUtil.isApiLevelAbove18()) {
		    FrameLayout.LayoutParams params = (android.widget.FrameLayout.LayoutParams) toolbar.getLayoutParams();
		    params.setMargins(0, ViewUtil.getStatusBarHeight(getResources()), 0, 0);
	    }
	    
		isDrawerIndicatorEnabled = true;
		if (savedInstanceState != null) {
			mTitle = savedInstanceState.getString(BundleKeys.ACTION_BAR_TITLE);
			currentContentFragmentTag = savedInstanceState.getString(BundleKeys.CURRENT_CONTENT_FRAGMENT_TAG);
			//Log.d(TAG, "savedInstanceState != null, currentContentFragmentTag = " + currentContentFragmentTag);

			drawerItemSelectedPosition = savedInstanceState.getInt(BundleKeys.DRAWER_ITEM_SELECTED_POSITION);
			isDrawerIndicatorEnabled = savedInstanceState.getBoolean(BundleKeys.IS_DRAWER_INDICATOR_ENABLED);
		}
		
		lnrLayoutRootNavDrawer = (LinearLayout) findViewById(R.id.rootNavigationDrawer);

		//Log.d(TAG, "lnrLayoutRootNavDrawer : " + lnrLayoutRootNavDrawer);

		//Log.d(TAG, "isTabletAndInLandscapeMode : " + isTabletAndInLandscapeMode);
		if (!isTabletAndInLandscapeMode) {

			mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
			mDrawerToggle = new ActionBarDrawerToggle(this, // host Activity
					mDrawerLayout, // DrawerLayout object
					R.string.drawer_open, // "open drawer" description
					R.string.drawer_close // "close drawer" description
			) {
				/**
				 * Called when a drawer has settled in a completely closed
				 * state.
				 */
				
				public void onDrawerClosed(View view) {
					super.onDrawerClosed(view);
					
					//getSupportActionBar().setTitle(mTitle);
					Fragment fragment = getSupportFragmentManager().findFragmentByTag(currentContentFragmentTag);
					//Log.d(TAG, "on drawer close fragment = " + fragment);
					if (fragment instanceof DrawerListener) {
						((DrawerListener) fragment).onDrawerClosed(view);
					}
				}

				/** Called when a drawer has settled in a completely open state. */
				public void onDrawerOpened(View drawerView) {
					super.onDrawerOpened(drawerView);
					
					// getSupportActionBar().setTitle(AppConstants.NAVIGATION_DRAWER_TITLE);
					/**
					 * On some devices drawer is partially overlapped by map. To
					 * negate this effect following workaround is required.
					 */
					if (currentContentFragmentTag
							.equals(AppConstants.FRAGMENT_TAG_CHANGE_LOCATION)
							|| currentContentFragmentTag
									.equals(AppConstants.FRAGMENT_TAG_FULL_SCREEN_ADDRESS_MAP)) {
						lnrLayoutRootNavDrawer.getParent().requestLayout();
						// ((View)lnrLayoutRootNavDrawer.getParent()).invalidate();
					}
					
					Fragment fragment = getSupportFragmentManager().findFragmentByTag(currentContentFragmentTag);
					if (fragment instanceof DrawerListener) {
						((DrawerListener) fragment).onDrawerOpened(drawerView);
					}
				}
				
				@Override
				public void onDrawerSlide(View drawerView, float slideOffset) {
					super.onDrawerSlide(drawerView, slideOffset);
					
					Fragment fragment = getSupportFragmentManager().findFragmentByTag(currentContentFragmentTag);
					if (fragment instanceof DrawerListener) {
						((DrawerListener) fragment).onDrawerSlide(drawerView, slideOffset);
					}
				}
			};

			setDrawerIndicatorEnabled(isDrawerIndicatorEnabled);
			//Log.i(TAG, "isDrawerIndicatorEnabled : " + isDrawerIndicatorEnabled);
			// Set the drawer toggle as the DrawerListener
			mDrawerLayout.setDrawerListener(mDrawerToggle);
			getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP);
			
		} else {
			int displayOptions;
			
			if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
				displayOptions = ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP;
				
			} else {
				displayOptions = ActionBar.DISPLAY_SHOW_TITLE;	
			}
			getSupportActionBar().setDisplayOptions(displayOptions);
		}
		/**
		 * setIcon null throws NullPointerException while expanding
		 * searchView in SearchFragment. So need to set any transparent icon
		 * rather than null.
		 */
		//getSupportActionBar().setIcon(R.drawable.ic_actionbar_app_icon);
		
		DrawerListFragment drawerListFragment = (DrawerListFragment) getSupportFragmentManager()
				.findFragmentByTag(DRAWER_LIST_FRAGMENT_TAG);
		//Log.d(TAG, "drawerListFragment : " + drawerListFragment);
		if (drawerListFragment == null) {
			addDrawerListFragment();
		}
		getSupportFragmentManager().executePendingTransactions();

		// getOverflowMenu();
		if (currentContentFragmentTag == null) {
			//Log.d(TAG, "currentContentFragmentTag = null");
			/**
			 * Above null check is required for widget, because if user
			 * navigates to any other screen from this event details, we want to
			 * have same screen appear on orientation change & not event details
			 * again. Note that otherwise without above check on orientation
			 * change it is always going to start event details screen as long
			 * as it has been called from widget, due to its bundle having event
			 * clicked.
			 */
			if (eventSeekr.getWcitiesId() == null) {
				selectNonDrawerItem(new LauncherFragment(), AppConstants.FRAGMENT_TAG_LAUNCHER, 
						getResources().getString(R.string.title_launcher), false);
				
			} else if (getIntent().hasExtra(BundleKeys.EVENT)) {
				// this can be from notification click or widget click
				onEventSelectedFromOtherTask((Event) getIntent().getSerializableExtra(BundleKeys.EVENT), false);
				
			} else if (getIntent().hasExtra(BundleKeys.ARTIST)) {
				onArtistSelectedFromOtherTask((Artist) getIntent().getSerializableExtra(BundleKeys.ARTIST), false);

			} else if (getIntent().hasExtra(BundleKeys.SETTINGS_ITEM_ORDINAL)) {
				SettingsItem settingsItem = SettingsItem.getSettingsItemByOrdinal(
						getIntent().getExtras().getInt(BundleKeys.SETTINGS_ITEM_ORDINAL));
				onSettingsItemClicked(settingsItem, null);
				
			} else if (getIntent().hasExtra(BundleKeys.NOTIFICATION_TYPE)) {
				onNotificationClicked((NotificationType) getIntent().getSerializableExtra(BundleKeys.NOTIFICATION_TYPE));

			} else {
				selectItem(INDEX_NAV_ITEM_DISCOVER, null);
			}
			
		} else {
			//Log.d(TAG, "currentContentFragmentTag != null");
		}

		if (AppConstants.FORD_SYNC_APP) {
			instance = this;
			startSyncProxyService();
		}
		
		//Log.d(TAG, "onCreate done");
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		//Log.d(TAG, "onStart()");
		
		EventSeekr.setConnectionFailureListener(this);
		DeviceUtil.registerLocationListener(this);
		/**
		 * Due to myspin bug sometimes it doesn't detect connected state instantly. To compensate for this 
		 * we run a delayed task to recheck on connected state & refresh UI.
		 */
		HandlerThread hThread = new HandlerThread("HandlerThread");
		hThread.start();
		
		handler = new Handler(hThread.getLooper());
		timeIntervalInMillisToCheckForBoschConnection = MIN_MILLIS_TO_CHK_BOSCH_CONNECTION;
		periodicCheckForBoschConnection = new Runnable() {
			
			@Override
			public void run() {
				//Log.d(TAG, "Periodic chk, isConnected = " + MySpinServerSDK.sharedInstance().isConnected());
				if (MySpinServerSDK.sharedInstance().isConnected()) {
					startBoschMainActivity();
					
				} else {
					timeIntervalInMillisToCheckForBoschConnection = (timeIntervalInMillisToCheckForBoschConnection*2 > 10*60*1000) ? 
							MIN_MILLIS_TO_CHK_BOSCH_CONNECTION : timeIntervalInMillisToCheckForBoschConnection*2;
					handler.postDelayed(this, timeIntervalInMillisToCheckForBoschConnection);
				}
			}
		};
		
		handler.postDelayed(periodicCheckForBoschConnection, timeIntervalInMillisToCheckForBoschConnection);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		//Log.d(TAG, "onNewIntent()");
		super.onNewIntent(intent);
		
		EventSeekr eventSeekr = (EventSeekr) getApplication();
		if (eventSeekr.getWcitiesId() == null) {
			
		} else if (intent.hasExtra(BundleKeys.EVENT)) {
			onEventSelectedFromOtherTask((Event) intent.getSerializableExtra(BundleKeys.EVENT), true);
			
		} else if (intent.hasExtra(BundleKeys.ARTIST)) {
			onArtistSelectedFromOtherTask((Artist) intent.getSerializableExtra(BundleKeys.ARTIST), true);

		} else if (intent.hasExtra(BundleKeys.SETTINGS_ITEM_ORDINAL)) {
			SettingsItem settingsItem = SettingsItem.getSettingsItemByOrdinal(
					intent.getExtras().getInt(BundleKeys.SETTINGS_ITEM_ORDINAL));
			onSettingsItemClicked(settingsItem, null);
			
		} else if (intent.hasExtra(BundleKeys.NOTIFICATION_TYPE)) {
			onNotificationClicked((NotificationType) intent.getSerializableExtra(BundleKeys.NOTIFICATION_TYPE));
		}
	}
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		if (mDrawerToggle != null) {
			// Sync the toggle state after onRestoreInstanceState has occurred.
			mDrawerToggle.syncState();
		}
		updateTitle();
	}

	@Override
	protected void onResume() {
		super.onResume();
		//Log.d(TAG, "onResume()");
		boolean isLockscreenVisible = false;
		if (AppConstants.FORD_SYNC_APP) {
			activityOnTop = true;
			// check if lockscreen should be up
			AppLinkService serviceInstance = AppLinkService.getInstance();
			if (serviceInstance != null) {
				if (serviceInstance.getLockScreenStatus() == true) {
					if (LockScreenActivity.getInstance() == null) {
						Intent i = new Intent(this, LockScreenActivity.class);
						i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						i.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
						startActivity(i);
					}
					isLockscreenVisible = true;
				}
			}
		}
		if (!isLockscreenVisible) {
			/**
			 * This is required because if user is connected to ford & then goes to change language 
			 * from sync tdk, then lock screen is destroyed showing actual app screen on device.
			 * In this case locale should be set for the device (not what is there on TDK).
			 */
			//Log.d(TAG, "onResume()");
			((EventSeekr) getApplication()).setDefaultLocale();
			//Log.d(TAG, "onResume()");
		}
	}

	@Override
	protected void onPause() {
		//Log.d(TAG, "onPause()");
		if (AppConstants.FORD_SYNC_APP) {
			activityOnTop = false;
		}
		super.onPause();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		//Log.d(TAG, "onStop()");
		
		EventSeekr.resetConnectionFailureListener(this);
		DeviceUtil.unregisterLocationListener((EventSeekr) getApplication());
		handler.removeCallbacks(periodicCheckForBoschConnection);
	}

	@Override
	protected void onDestroy() {
		//Log.d(TAG, "onDestroy()");
		if (AppConstants.FORD_SYNC_APP) {
			//Log.v(TAG, "onDestroy main");
			endSyncProxyInstance();
			instance = null;
			AppLinkService serviceInstance = AppLinkService.getInstance();
			if (serviceInstance != null) {
				serviceInstance.setCurrentActivity(null);
			}
		}

		//Log.d(TAG, "View : " + findViewById(R.id.rootNavigationDrawer));
		super.onDestroy();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (!isTabletAndInLandscapeMode) {
			mDrawerToggle.onConfigurationChanged(newConfig);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);

		searchItem = menu.findItem(R.id.action_search);
		searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
		searchView.setQueryHint(getResources().getString(R.string.menu_search));
		searchView.setOnQueryTextListener(this);
		
		ImageView v = (ImageView) searchView.findViewById(R.id.search_button);
		// null check is for safety purpose
		if (v != null) {
			v.setImageResource(R.drawable.search);
		}
		
		if (AppConstants.FRAGMENT_TAG_SEARCH.equals(currentContentFragmentTag)) {
			/**
			 * on some devices onCreateOptionsMenu is called after onFragmentResumed, 
			 * so we need to expand actionview here after initializing the searchItem
			 */
			MenuItemCompat.expandActionView(searchItem);
		}
		
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean disableSearch = currentContentFragmentTag.equals(AppConstants.FRAGMENT_TAG_LOGIN)
				|| currentContentFragmentTag.equals(AppConstants.FRAGMENT_TAG_SIGN_UP)
				|| currentContentFragmentTag.equals(AppConstants.FRAGMENT_TAG_CHANGE_LOCATION)
				|| currentContentFragmentTag.equals(AppConstants.FRAGMENT_TAG_FULL_SCREEN_ADDRESS_MAP)
				|| currentContentFragmentTag.equals(AppConstants.FRAGMENT_TAG_LOGIN_SYNCING);
		menu.findItem(R.id.action_search).setVisible(!disableSearch);
		
		if (currentContentFragmentTag.equals(AppConstants.FRAGMENT_TAG_SEARCH)) {
			searchView.setQuery(searchQuery, false);
			searchView.clearFocus();
			
		} else if (currentContentFragmentTag.equals(AppConstants.FRAGMENT_TAG_CHANGE_LOCATION)) {
			searchView.setQuery("", false);			
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		//Log.d(TAG, "onOptionsItemSelected() itemId = " + item.getItemId());
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        /*if (mDrawerToggle.onOptionsItemSelected()) {
        	return true;
        }*/
        // Handle your other action bar items...
		switch (item.getItemId()) {

		case android.R.id.home:
			if (AppConstants.FRAGMENT_TAG_LOGIN_SYNCING.equals(currentContentFragmentTag)) {
				return true;
				
			} else if (!isTabletAndInLandscapeMode) {
				if (mDrawerToggle.isDrawerIndicatorEnabled()) {
					//Log.d(TAG, "isDrawerIndicatorEnabled");
					if (isDrawerOpen()) {
						closeDrawer();

					} else {
						mDrawerLayout.openDrawer(lnrLayoutRootNavDrawer);
					}

				} else if (isDrawerOpen()) {
					/**
					 * If user is on any screen apart from first level screens accessible from navigation drawer
					 * & if user has opened drawer by swipe action (although drawer indicator is not enabled & 
					 * back arrow is there on top left corner), then clicking on toolbar back icon should close 
					 * drawer first instead of moving back to previous screen in the back stack.
					 */
					closeDrawer();
					
				} else if ((getSupportActionBar().getDisplayOptions() & ActionBar.DISPLAY_HOME_AS_UP) != 0) {
					/**
					 * above condition is to prevent back action when user clicks actionbar's home button on
					 * launcher screen. In that case it should have no action; otherwise execute onBackPressed()
					 * as long as home as up indicator is shown.
					 */
					//Log.d(TAG, "onBackPressed");
					onBackPressed();
				}
				
			} else {
				/**
				 * in some higher version of android even after setting the Display option as 'DISPLAY_SHOW_TITLE',
				 * the title was taking the touch event and was providing the action which is provided by 
				 * 'DISPLAY_HOME_AS_UP' Display option, i.e. it shouldn't execute this 'case' but it was. So,
				 * by using the below condition, we check whether the title is clicked on the page from navigation
				 * drawer item and if the click is not on the page of navigation drawer item, then it should do the 
				 * following functionality.
				 */
				if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
					onBackPressed();
					if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
						/**
						 * Following line is to reset display options when coming back to initial screen.
						 * This is required only in case of tablet in landscape orientation, since we don't 
						 * have navigation drawer in this case resulting in different behavior observed for 
						 * home_as_up icon.
						 * DISPLAY_SHOW_CUSTOM is used for discover screen on tablet.
						 * e.g. - Launch --> Discover --> Event Details --> home as up. In this case tabs & back should 
						 * disappear from actionbar.
						 */
						getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE 
								| ActionBar.DISPLAY_SHOW_CUSTOM);
					}
				}
			}
			return true;
			
			/*
			 * case R.id.action_search: SearchFragment searchFragment = new
			 * SearchFragment(); selectNonDrawerItem(searchFragment,
			 * AppConstants.FRAGMENT_TAG_SEARCH,
			 * getResources().getString(R.string.title_search_results), true);
			 * return true;
			 */

		default:
			closeDrawer();
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		//Log.d(TAG, "onSaveInstanceState()");
	
		outState.putString(BundleKeys.ACTION_BAR_TITLE, mTitle);
		outState.putString(BundleKeys.CURRENT_CONTENT_FRAGMENT_TAG, currentContentFragmentTag);
		outState.putInt(BundleKeys.DRAWER_ITEM_SELECTED_POSITION, drawerItemSelectedPosition);
		outState.putBoolean(BundleKeys.IS_DRAWER_INDICATOR_ENABLED, isDrawerIndicatorEnabled);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult(), requestCode = " + requestCode + ", resultCode = " + resultCode);
		switch (requestCode) {
		
		case AppConstants.REQ_CODE_INVITE_FRIENDS:
		case AppConstants.REQ_CODE_RATE_APP:
			hasOtherActivityFinished = true;
			break;
			
		case AppConstants.REQ_CODE_GOOGLE_PLUS_RESOLVE_ERR:
		case AppConstants.REQ_CODE_GET_GOOGLE_PLAY_SERVICES:
			//Log.d(TAG, "current frag tag = " + currentContentFragmentTag);
			Fragment fragment = getSupportFragmentManager().findFragmentByTag(currentContentFragmentTag);
			if (fragment != null) {
				fragment.onActivityResult(requestCode, resultCode, data);
			}
			break;
			
		case AppConstants.REQ_CODE_SPOTIFY:
			fragment = getSupportFragmentManager().findFragmentByTag(currentContentFragmentTag);
			if (fragment instanceof ConnectAccountsFragment) {
				fragment.onActivityResult(requestCode, resultCode, data);
			}
			break;
			
		/*case REQ_CODE_GOOGLE_ACCOUNT_CHOOSER:
			if (resultCode == RESULT_OK) {
				final String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
				AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {

					@Override
					protected Void doInBackground(Void... params) {
						try {
				        	String authToken = GoogleAuthUtil.getToken(MainActivity.this, accountName, "sj");
				            
				            if (!TextUtils.isEmpty(authToken)) {
				            	Bundle args = new Bundle();
								args.putString(BundleKeys.AUTH_TOKEN, authToken);
								GooglePlayMusicFragment googlePlayMusicFragment = new GooglePlayMusicFragment();
								googlePlayMusicFragment.setArguments(args);
								selectNonDrawerItem(googlePlayMusicFragment, AppConstants.FRAGMENT_TAG_GOOGLE_PLAY_MUSIC, 
										getResources().getString(R.string.title_google_play), true);
				            }
				            
				        } catch (UserRecoverableAuthException e) {
				            startActivityForResult(e.getIntent(), REQ_CODE_GOOGLE_ACCOUNT_CHOOSER);
				            e.printStackTrace();
				            
				        } catch (IOException e) {
							e.printStackTrace();
							
						} catch (GoogleAuthException e) {
							e.printStackTrace();
						}
						return null;
					}
				};
				asyncTask.execute();
				
				
			} else {
				
			}
			break;*/

		default:
			if (GPlusUtil.isGPlusPublishPending) {
				/**
				 * This check is required to direct onActivityResult() calls from MainActivity & handle it at right 
				 * place, because google plus share intent doesn't return right request code in onActivityResult() 
				 * method.
				 */
				Log.d(TAG, "current frag tag = " + currentContentFragmentTag);
				fragment = getSupportFragmentManager().findFragmentByTag(currentContentFragmentTag);
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
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// Log.d(TAG, "onKeyDown()");
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (AppConstants.FRAGMENT_TAG_LOGIN_SYNCING.equals(currentContentFragmentTag)) {
				return true;
				
			} else {
				if (isTabletAndInLandscapeMode) {
					if (AppConstants.FRAGMENT_TAG_WEB_VIEW.equals(currentContentFragmentTag)) {
						WebViewFragment webViewFragment = (WebViewFragment) getSupportFragmentManager()
								.findFragmentByTag(AppConstants.FRAGMENT_TAG_WEB_VIEW);
						if (webViewFragment.onKeyDown()) {
							return true;
							
						} else {
							return super.onKeyDown(keyCode, event);
						}
					}
					
					if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
						/**
						 * Following line is to reset display options when coming back to initial screen.
						 * This is required only in case of tablet in landscape orientation, since we don't 
						 * have navigation drawer in this case resulting in different behavior observed for 
						 * home_as_up icon.
						 * DISPLAY_SHOW_CUSTOM is used for discover screen on tablet.
						 * e.g. - Launch --> Discover --> Event Details --> hardware back. In this case tabs & 
						 * back should disappear from actionbar.
						 */
						getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_CUSTOM);
					}
					
					return super.onKeyDown(keyCode, event);
					
				} else {
					if (isDrawerOpen()) {
						/**
						 * If user is on any screen apart from first level screens accessible from navigation drawer
						 * & if user has opened drawer by swipe action (although drawer indicator is not enabled & 
						 * back arrow is there on top left corner), then clicking on device back button should close 
						 * drawer first instead of moving back to previous screen in the back stack.
						 */
						mDrawerLayout.closeDrawer(lnrLayoutRootNavDrawer);
						return true;
						
					} else if (AppConstants.FRAGMENT_TAG_WEB_VIEW.equals(currentContentFragmentTag)) {
						WebViewFragment webViewFragment = (WebViewFragment) getSupportFragmentManager()
								.findFragmentByTag(AppConstants.FRAGMENT_TAG_WEB_VIEW);
						if (webViewFragment.onKeyDown()) {
							return true;
							
						} else {
							return super.onKeyDown(keyCode, event);
						}
						
					} else if (AppConstants.FRAGMENT_TAG_CHANGE_LOCATION.equals(currentContentFragmentTag)) {
						ChangeLocationFragment changeLocationFragment = (ChangeLocationFragment) getSupportFragmentManager()
								.findFragmentByTag(AppConstants.FRAGMENT_TAG_CHANGE_LOCATION);
						onLocationChanged(changeLocationFragment.getArguments());
						return true;
						
					} else {
						return super.onKeyDown(keyCode, event);
					}
				}
			}
		}
		
		if (!isTabletAndInLandscapeMode) {
			if (keyCode == KeyEvent.KEYCODE_MENU) {
				if (mDrawerToggle.isDrawerIndicatorEnabled()) {
					if (isDrawerOpen()) {
						mDrawerLayout.closeDrawer(lnrLayoutRootNavDrawer);

					} else {
						mDrawerLayout.openDrawer(lnrLayoutRootNavDrawer);
					}
					return true;

				} else {
					return super.onKeyDown(keyCode, event);
				}

			} else {
				return super.onKeyDown(keyCode, event);
			}

		} else {
			return super.onKeyDown(keyCode, event);
		}
	}
	
	private void startBoschMainActivity() {
		Intent intent = new Intent(getApplicationContext(), BoschMainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivity(intent);
	}
	
	public boolean isActivityonTop() {
		return activityOnTop;
	}

	public void startSyncProxyService() {
		// Log.i(TAG, "startSyncProxyService()");
		if (AppConstants.DEBUG) {
			if (AppLinkService.getInstance() == null) {
				// Log.i(TAG, "getInstance() == null");
				Intent startIntent = new Intent(this, AppLinkService.class);
				startService(startIntent);

			} else {
				// if the service is already running and proxy is up,
				// set this as current UI activity
				AppLinkService.getInstance().setCurrentActivity(this);
				// Log.i(TAG, " proxyAlive == true success");
			}

		} else {
			boolean isSYNCpaired = false;
			// Get the local Bluetooth adapter
			BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();

			// BT Adapter exists, is enabled, and there are paired devices with
			// the
			// name SYNC
			// Ideally start service and start proxy if already connected to
			// sync
			// but, there is no way to tell if a device is currently connected
			// (pre
			// OS 4.0)

			if (mBtAdapter != null) {
				// Log.i(TAG, "mBtAdapter is not null");
				if ((mBtAdapter.isEnabled() && mBtAdapter.getBondedDevices().isEmpty() == false)) {
					Log.i(TAG, "pairedDevices");
					// Get a set of currently paired devices
					Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

					// Check if there is a paired device with the name "SYNC"
					if (pairedDevices.size() > 0) {
						// Log.i(TAG, "pairedDevices > 0");
						for (BluetoothDevice device : pairedDevices) {
							// Log.i(TAG, "device.getName() = " +
							// device.getName());
							if (device.getName().toString().contains("SYNC")) {
								// Log.i(TAG, "found SYNC");
								isSYNCpaired = true;
								break;
							}
						}

					} else {
						Log.i(TAG, "A No Paired devices with the name sync");
					}

					if (isSYNCpaired == true) {
						if (AppLinkService.getInstance() == null) {
							// Log.i(TAG, "start service");
							Intent startIntent = new Intent(this, AppLinkService.class);
							startService(startIntent);

						} else {
							//if the service is already running and proxy is up, set this as current UI activity
    		        		AppLinkService serviceInstance = AppLinkService.getInstance();
    		        		serviceInstance.setCurrentActivity(this);
    		        		SyncProxyALM proxyInstance = serviceInstance.getProxy();
    		        		if (proxyInstance != null) {
    		        			serviceInstance.reset();
    		        			
    		        		} else {
    		        			Log.i("TAG", "proxy is null");	
    		        			serviceInstance.startProxy();
    		        		}    		        		
    		        		Log.i("TAG", " proxyAlive == true success");
						}
					}
				}
			}
		}
	}

	// upon onDestroy(), dispose current proxy and create a new one to enable
	// auto-start
	// call resetProxy() to do so
	public void endSyncProxyInstance() {
		AppLinkService serviceInstance = AppLinkService.getInstance();
		if (serviceInstance != null) {
			SyncProxyALM proxyInstance = serviceInstance.getProxy();
			// if proxy exists, reset it
			if (proxyInstance != null) {
				if (proxyInstance.getCurrentTransportType() == TransportType.BLUETOOTH) {
					serviceInstance.reset();

				} else {
					Log.e(TAG, "endSyncProxyInstance. No reset required if transport is TCP");
				}
				// if proxy == null create proxy
			} else {
				serviceInstance.startProxy();
			}
		}
	}

	private void addDrawerListFragment() {
		//Log.d(TAG, "addDrawerListFragment");
		FragmentTransaction fragmentTransaction = getSupportFragmentManager()
				.beginTransaction();
		DrawerListFragment drawerListFragment = new DrawerListFragment();
		fragmentTransaction.add(R.id.rootNavigationDrawer, drawerListFragment,
				DRAWER_LIST_FRAGMENT_TAG);
		fragmentTransaction.commit();
	}

	private void onFragmentResumed(int position, String title, String fragmentTag) {
		//Log.d(TAG, "onFragmentResumed() for title = " + title + ", position = " + position + ", fragmentTag = " + fragmentTag);
		drawerItemSelectedPosition = position;
		if (drawerItemSelectedPosition != AppConstants.INVALID_INDEX) {
			setDrawerIndicatorEnabled(true);
			
			/**
			 * This check is included since otherwise this function gets called up even just before we start
			 * invite friends activity as in following case:
			 * Suppose user 
			 * 1) browses discover -> featured event click -> event details screen.
			 * 2) Opens side navigation by swiping from left to right
			 * 3) Selects invite friends.
			 * In this case as we clear backstack from onDrawerItemSelected() method, onFragmentResumed() is 
			 * called up first for discover fragment followed by starting activity for inviting friends.
			 * At this point we should not be marking discover item as checked on navigation drawer & 
			 * hence the following condition is used. Only when other activity finishes we can allow fragments 
			 * of MainActivity to mark corresponding items checked.
			 * Above explanation applies to rate app activity as well similar to invite friends activity.
			 */
			if (hasOtherActivityFinished) {
				hasOtherActivityFinished = false;
				updateDrawerListCheckedItem(drawerItemSelectedPosition);
			}
			
		} else {
			/**
			 * 1) This is for redirection to sync accounts screen after signup where we add settings fragment first
			 * followed by sync accounts screen. Since these transactions occur fast (as we are adding sync accounts 
			 * from onCreate() of settings fragment) we cannot even use condition 
			 * "getSupportFragmentManager().getBackStackEntryCount() > 0" because it gives result 0 instead of 1
			 * as it returns right value only after some delay.
			 * 2) Even when more than 1 fragments are added to backstack, then returning to this activity from
			 * third party app results in onFragmentResumed() being called for all these fragments sequentially
			 * where first it calls for fragment at index 0 followed by other. For 0th fragment it enables drawer indicator
			 * from above if block & afterwards to negate this effect for current fragment we need to also disable it
			 * if current fragment does not need.
			 * e.g. - Discover -> event details (added to fragment, not replaced) -> click save event (google+) -> back (w/o saving)
			 */
			setDrawerIndicatorEnabled(false);
		}
		
		mTitle = title;
		updateTitle();

		currentContentFragmentTag = fragmentTag;
	}
	
	private void onFragmentCalledFromOtherTaskResumed(int position, String title, String fragmentTag) {
		//Log.d(TAG, "onFragmentCalledFromOtherTaskResumed() - " + fragmentTag);
		drawerItemSelectedPosition = position;
		if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
			setDrawerIndicatorEnabled(true);
			
		} else {
			setDrawerIndicatorEnabled(false);
		}
		mTitle = title;
		updateTitle();

		currentContentFragmentTag = fragmentTag;
	}

	/** Swaps fragments in the main content view */
	private void selectItem(int position, Bundle args) {
		//Log.d(TAG, "selectItem() + pos : " + position);
		//if (position != INDEX_NAV_ITEM_LATEST_NEWS) {
			drawerItemSelectedPosition = position;
				
			setDrawerIndicatorEnabled(true);
				
			if (isTabletAndInLandscapeMode) {
				//getSupportActionBar().setIcon(R.drawable.ic_actionbar_app_icon);
				getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
			}
			
			boolean isDrawerListFragmentFound = updateDrawerListCheckedItem(position);
			Log.d(TAG, "isDrawerListFragmentFound = " + isDrawerListFragmentFound);
			if (!isDrawerListFragmentFound) {
				return;
			}
		//}

	    switch (position) {
	    
		case INDEX_NAV_ITEM_DISCOVER:
			//DiscoverParentFragment discoverFragment;
			DiscoverFragment discoverFragment;
			/*if(isTablet) {
				discoverFragment = new DiscoverFragmentTab();
			} else {*/
				discoverFragment = new DiscoverFragment();
			//}
			if (args != null) {
				discoverFragment.setArguments(args);
			}
			replaceContentFrameByFragment(discoverFragment, AppConstants.FRAGMENT_TAG_DISCOVER, "", false);
			break;

		case INDEX_NAV_ITEM_MY_EVENTS:
			MyEventsFragment fragment = new MyEventsFragment();
			if (args != null) {
				fragment.setArguments(args);
			}
			replaceContentFrameByFragment(fragment, AppConstants.FRAGMENT_TAG_MY_EVENTS, getResources()
							.getString(R.string.title_my_events), false);
			break;
			
		case INDEX_NAV_ITEM_FOLLOWING:
			FollowingParentFragment followingFragment;
			if(!isTablet) {
				followingFragment = new FollowingFragment();
			} else {
				followingFragment = new FollowingFragmentTab();
			}
			replaceContentFrameByFragment(followingFragment, AppConstants.FRAGMENT_TAG_FOLLOWING, 
					getResources().getString(R.string.title_following), false);
			break;

		case INDEX_NAV_ITEM_ARTISTS_NEWS:
			ArtistsNewsListFragment artistsNewsFragment = new ArtistsNewsListFragment();
			replaceContentFrameByFragment(artistsNewsFragment, AppConstants.FRAGMENT_TAG_ARTISTS_NEWS_LIST, getResources()
							.getString(R.string.title_artists_news), false);
			break;
			
		case INDEX_NAV_ITEM_FRIENDS_ACTIVITY:
			FriendsActivityFragment friendsActivityFragment = new FriendsActivityFragment();
			replaceContentFrameByFragment(friendsActivityFragment, AppConstants.FRAGMENT_TAG_FRIENDS_ACTIVITY, getResources()
							.getString(R.string.title_friends_activity), false);
			break;

		case INDEX_NAV_ITEM_SETTINGS:
			SettingsFragment settingsFragment = new SettingsFragment();
			if (args != null) {
				settingsFragment.setArguments(args);
			}
			replaceContentFrameByFragment(settingsFragment, AppConstants.FRAGMENT_TAG_SETTINGS, getResources()
					.getString(R.string.title_settings_mobile_app), false);
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
			getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			selectNonDrawerItem(new LauncherFragment(), AppConstants.FRAGMENT_TAG_LAUNCHER, 
					getResources().getString(R.string.title_launcher), false);
			break;
			
		default:
			break;
		}
	    
	    if (!isTabletAndInLandscapeMode) {
	    	mDrawerLayout.closeDrawer(lnrLayoutRootNavDrawer);
	    }
	}
	
	/**
	 * @param position
	 * @return true if DrawerListFragment instance is existing (not null)
	 */
	private boolean updateDrawerListCheckedItem(int position) {
		//Log.d(TAG, "updateDrawerListCheckedItem()");
		if (position == AppConstants.INVALID_INDEX) {
			return false;
		}
		
		DrawerListFragment drawerListFragment = (DrawerListFragment) getSupportFragmentManager()
				.findFragmentByTag(DRAWER_LIST_FRAGMENT_TAG);
		if (drawerListFragment == null) {
			//Log.d(TAG, "drawerListFragment == null");
			return false;
		}
		try {
			drawerListFragment.getListView().setItemChecked(position, true);
			
		} catch (IllegalStateException e) {
			// this occurs when call sequence starts from onCreate()
			Log.i(TAG, "Drawer listview is not yet ready.");
		}
		return true;
	}

	public boolean isTabletAndInLandscapeMode() {
		return isTabletAndInLandscapeMode;
	}

	public boolean isTablet() {
		return isTablet;
	}

	private void inviteFriends() {
		//Log.d(TAG, "inviteFriends()");
		hasOtherActivityFinished = false;
		String url = "https://play.google.com/store/apps/details?id=" + getPackageName();
		Intent intent = new Intent(android.content.Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		intent.putExtra(Intent.EXTRA_TEXT, "Checkout eventseeker" + " " + url);
		try {
			startActivityForResult(intent, AppConstants.REQ_CODE_INVITE_FRIENDS);

		} catch (ActivityNotFoundException e) {
			Toast.makeText(getApplicationContext(), "Error, this action cannot be completed at this time.",
					Toast.LENGTH_SHORT).show();
		}
	}

	private void rateApp() {
		hasOtherActivityFinished = false;
		Uri uri = Uri.parse("market://details?id=" + getPackageName());
		Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
		try {
			startActivityForResult(goToMarket, AppConstants.REQ_CODE_RATE_APP);

		} catch (ActivityNotFoundException e) {
			Toast.makeText(getApplicationContext(), R.string.error_this_action_couldnt_be_completed_at_this_time,
					Toast.LENGTH_SHORT).show();
		}
	}

	private void replaceContentFrameByFragment(Fragment replaceBy, String replaceByFragmentTag, 
			String newTitle, boolean addToBackStack) {
		//Log.d(TAG, "replaceContentFrameByFragment() - newTitle = " + newTitle);
		mTitle = newTitle;
		updateTitle();

		/*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			if (replaceByFragmentTag.equals(AppConstants.FRAGMENT_TAG_LOGIN)) {
				Slide slide = new Slide(Gravity.BOTTOM);
				slide.setDuration(1000);
		        replaceBy.setEnterTransition(slide);
		        
		        slide = new Slide(Gravity.BOTTOM);
		        slide.setDuration(1000);
		        replaceBy.setReturnTransition(slide);
			}
		}*/
		FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
		Bundle args = replaceBy.getArguments();
		if (args != null && args.containsKey(BundleKeys.FRAGMENT_TRANSACTION_ANIM_IDS)) {
			int[] anims = args.getIntArray(BundleKeys.FRAGMENT_TRANSACTION_ANIM_IDS);
			fragmentTransaction.setCustomAnimations(anims[0], anims[1], anims[2], anims[3]);
		}
		
		//Log.d(TAG, "currentContentFragmentTag = " + currentContentFragmentTag);
		Fragment currentFragment = getSupportFragmentManager().findFragmentByTag(currentContentFragmentTag);
		
		if (replaceByFragmentTag.equals(AppConstants.FRAGMENT_TAG_LOGIN_SYNCING) || 
				((args != null && args.containsKey(BundleKeys.SHARED_ELEMENTS))) || 
				(currentFragment instanceof FragmentHavingFragmentInRecyclerView && addToBackStack)) {
			
			//Log.d(TAG, "replaceContentFrameByFragment() add replaceByFragmentTag = " + replaceByFragmentTag);
			
			/**
			 * 3rd condition with instanceOf check is required to handle situation where user clicks 
			 * for example 2 events simultaneously on discover screen. In such case for first event details screen
			 * currentFragment would be discover & for 2nd event details screen, current fragment would be 
			 * first event details screen. In this case event details screen is also instanceOf CustomSharedElementTransitionSource
			 * hence it won't cause any issues, but if that is not the case it might throw 
			 * ClassCastException w/o instanceOf check
			 */
			if (args != null && args.containsKey(BundleKeys.SHARED_ELEMENTS) 
					&& currentFragment instanceof CustomSharedElementTransitionSource) {
				prevCustomSharedElementTransitionSource = (CustomSharedElementTransitionSource) currentFragment;
			}
				
			/**
			 * add fragment instead of replacing so that behind its transparent background 
			 * a) sign in/sign up screen remains visible OR
			 * b) screen transition executes intuitively when there are some shared elements
			 * c) If we replace or remove-add anything on artist/venue details fragment, it crashes with 
			 * "IllegalArgumentException: no view found for id R.id.vPagerVideos/R.id.frmLayoutMapContainer for 
			 * VideoFragment/AddressMapFragment" on coming back to artist/venue details screen. Couldn't find its solution. 
			 * Probably it's happening with any Fragment within RecyclerView.
			 * 'addToBackStack' is added to prevent addition when selecting any navigationDrawer item
			 */
			fragmentTransaction.add(R.id.content_frame, replaceBy, replaceByFragmentTag);
			
		} else {
			/**
			 * currentFragment null check below is for signup scenario where after signup completion we start
			 * settings screen which in turn starts sync accounts screen from its onCreate() itself & hence 
			 * when this sync account screen starts we get null value for currentFragment instead of 
			 * SettingsFragment which we can't pass to remove(), otherwise it will result in NullPointerException
			 */
			if (drawerItemSelectedPosition == AppConstants.INVALID_INDEX && currentContentFragmentTag != null
					&& currentFragment != null) {
				//Log.d(TAG, "replaceContentFrameByFragment() remove add replaceByFragmentTag = " + replaceByFragmentTag);
				/**
				 * Instead of using replace function, we use combination of remove & add, since we just want to 
				 * replace top fragment whereas replace empties out entire container followed by adding new
				 * fragment. Note that on using replace() in this case, addToBackStack() will consider 
				 * transaction of last fragment(at index 0) replaced by new fragment, so pressing back afterwards 
				 * won't display right fragment.
				 * e.g.: 
				 * 1) replace() - fragment A
				 * 2) add() - fragment B, add to back stack
				 * 3) replace() - fragment C, add to back stack
				 * Press back will display fragment A (not B which we want), press back once more will also display same fragment A & 
				 * pressing back now will move app to backstack (as we are calling moveTaskToBack() from onBackPressed())
				 * 
				 * When drawerItemSelectedPosition is valid for items from navigation drawer (drawerItemSelectedPosition != AppConstants.INVALID_INDEX), 
				 * we already pop entire backstack from onDrawerItemSelected() function & hence we can use replace() method
				 * directly from else block. Otherwise using this method in such case is causing issue: 
				 * e.g. - if we use remove & add after login process completion, then app doesn't unlock
				 * the drawer (it doesn't call onDestroy() of LauncherFragment).
				 */
				fragmentTransaction.remove(currentFragment);
				fragmentTransaction.add(R.id.content_frame, replaceBy, replaceByFragmentTag);
				
			} else {
				//Log.d(TAG, "replaceContentFrameByFragment() replace replaceByFragmentTag = " + replaceByFragmentTag);
				fragmentTransaction.replace(R.id.content_frame, replaceBy, replaceByFragmentTag);
			}
		}
		
		//if (!sharedElements.isEmpty()) {
			//getWindow().setAllowEnterTransitionOverlap(true);
			//DiscoverFragment discoverFragment = (DiscoverFragment) getSupportFragmentManager().findFragmentByTag(AppConstants.FRAGMENT_TAG_DISCOVER);
			//discoverFragment.setSharedElementReturnTransition(TransitionInflater.from(this).inflateTransition(R.transition.change_image_transform));
			//discoverFragment.setExitTransition(TransitionInflater.from(this).inflateTransition(android.R.transition.explode));

			//replaceBy.setSharedElementEnterTransition(TransitionInflater.from(this).inflateTransition(R.transition.change_image_transform));
			//replaceBy.setSharedElementReturnTransition(TransitionInflater.from(this).inflateTransition(R.transition.change_image_transform));
			
			//replaceBy.setEnterTransition(TransitionInflater.from(this).inflateTransition(android.R.transition.explode));

			//addSharedElements(fragmentTransaction);

			//sharedElements.clear();
		//}
		
		if (addToBackStack) {
			fragmentTransaction.addToBackStack(null);
		}
		
		fragmentTransaction.commitAllowingStateLoss();

		// if moving away from search screen, collapse search actionview.
		if (AppConstants.FRAGMENT_TAG_SEARCH.equals(currentContentFragmentTag)) {
			MenuItemCompat.collapseActionView(searchItem);
			// searchItem.collapseActionView();
		}
		currentContentFragmentTag = replaceByFragmentTag;

		/**
		 * For fragments not having setHasOptionsMenu(true),
		 * onPrepareOptionsMenu() is not called on adding/replacing such
		 * fragments. But if user visits any such fragment by selecting it from
		 * drawer initially when just fbLoginFragment is visible (for which
		 * search action item is disabled from onPrepareOptionsMenu()), then
		 * these menus' visibility don't change due to onPrepareOptionsMenu()
		 * not being called up. Hence the following code.
		 * 
		 * If condition is there to prevent momentary display of 2 search icons
		 * placed side by side in case of these 3 fragments.
		 */
		if (!currentContentFragmentTag.equals(AppConstants.FRAGMENT_TAG_ARTIST_DETAILS)
				&& !currentContentFragmentTag.equals(AppConstants.FRAGMENT_TAG_EVENT_DETAILS)
				&& !currentContentFragmentTag.equals(AppConstants.FRAGMENT_TAG_VENUE_DETAILS)) {
			supportInvalidateOptionsMenu();
		}
		
		//Log.d(TAG, "back stack count = " + getSupportFragmentManager().getBackStackEntryCount());
	}
	
	/*private void addSharedElements(FragmentTransaction ft) {
		for (Iterator<SharedElement> iterator = sharedElements.iterator(); iterator.hasNext();) {
			View sharedElement = ((SharedElement)iterator.next()).getView();
			ft.addSharedElement(sharedElement, ViewCompat.getTransitionName(sharedElement));
		}
	}*/

	private void updateTitle() {
		/*
		 * if (isDrawerOpen()) {
		 * getSupportActionBar().setTitle(AppConstants.NAVIGATION_DRAWER_TITLE);
		 * 
		 * } else {
		 */
		//Log.d(TAG, "title = " + mTitle);
		getSupportActionBar().setTitle(mTitle);
		// }
	}
	
	public void updateTitle(String title) {
		mTitle = title;
		updateTitle();
	}
	
	private void selectNonDrawerItem(Fragment replaceBy, String replaceByFragmentTag, String newTitle, 
			boolean addToBackStack) {
		//Log.d(TAG, "selectNonDrawerItem(), newTitle = " + newTitle + ", addToBackStack = " + addToBackStack);
		
		drawerItemSelectedPosition = AppConstants.INVALID_INDEX;
		// revertCheckedDrawerItemStateIfAny();
		setDrawerIndicatorEnabled(!addToBackStack);
		
		if (isTabletAndInLandscapeMode) {
			//getSupportActionBar().setIcon(R.drawable.ic_actionbar_app_icon);			
			if (addToBackStack) {
				getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP);
			}
		}
		// getSupportActionBar().setDisplayHomeAsUpEnabled(addToBackStack);
		replaceContentFrameByFragment(replaceBy, replaceByFragmentTag, newTitle, addToBackStack);
	}

	@Override
	public void replaceByFragment(String fragmentTag, Bundle args) {
		if (fragmentTag.equals(AppConstants.FRAGMENT_TAG_DISCOVER_BY_CATEGORY)) {
			int categoryPosition = args.getInt(BundleKeys.CATEGORY_POSITION);
			List<Category> categories = (List<Category>) args.getSerializable(BundleKeys.CATEGORIES);

			DiscoverByCategoryFragment discoverByCategoryFragment = new DiscoverByCategoryFragment();
			discoverByCategoryFragment.setArguments(args);
			selectNonDrawerItem(discoverByCategoryFragment, fragmentTag, categories.get(categoryPosition).getName(), true);
			
		} else if (fragmentTag.equals(AppConstants.FRAGMENT_TAG_WEB_VIEW)) {
			WebViewFragment webViewFragment = new WebViewFragment();
			webViewFragment.setArguments(args);
			selectNonDrawerItem(webViewFragment, fragmentTag, getResources().getString(R.string.title_web), true);
			
		} else if (fragmentTag.equals(AppConstants.FRAGMENT_TAG_LOGIN)) {
			LoginFragment loginFragment = new LoginFragment();
			args = new Bundle();
			args.putIntArray(BundleKeys.FRAGMENT_TRANSACTION_ANIM_IDS, new int[]{R.anim.launcher_to_login, 
					R.anim.fade_out, R.anim.fade_in, R.anim.login_to_launcher});
			loginFragment.setArguments(args);
			selectNonDrawerItem(loginFragment, AppConstants.FRAGMENT_TAG_LOGIN, getResources()
				.getString(R.string.title_login), true);
			
		} else if (fragmentTag.equals(AppConstants.FRAGMENT_TAG_SIGN_UP)) {
			SignUpFragment signUpFragment = new SignUpFragment();
			args = new Bundle();
			args.putIntArray(BundleKeys.FRAGMENT_TRANSACTION_ANIM_IDS, new int[]{R.anim.launcher_to_signup, 
					R.anim.fade_out, R.anim.fade_in, R.anim.signup_to_launcher});
			signUpFragment.setArguments(args);
			selectNonDrawerItem(signUpFragment, AppConstants.FRAGMENT_TAG_SIGN_UP, getResources()
				.getString(R.string.title_signup), true);

		} else if (fragmentTag.equals(AppConstants.FRAGMENT_TAG_FOLLOW_MORE_ARTISTS)) {
			FollowMoreArtistsFragment followMoreArtistsFragment = new FollowMoreArtistsFragment();
			followMoreArtistsFragment.setArguments(args);
			selectNonDrawerItem(followMoreArtistsFragment, AppConstants.FRAGMENT_TAG_FOLLOW_MORE_ARTISTS, getResources()
				.getString(R.string.title_find_more_artist), true);

		} else if (fragmentTag.equals(AppConstants.FRAGMENT_TAG_POPULAR_ARTISTS)) {
			PopularArtistsFragment popularArtistsFragment = new PopularArtistsFragment();
			popularArtistsFragment.setArguments(args);
			selectNonDrawerItem(popularArtistsFragment, AppConstants.FRAGMENT_TAG_POPULAR_ARTISTS, getResources()
					.getString(R.string.title_popular), true);

		} else if (fragmentTag.equals(AppConstants.FRAGMENT_TAG_SPORTS_ARTISTS)) {
			SportsArtistsFragment sportsArtistsFragment = new SportsArtistsFragment();
			sportsArtistsFragment.setArguments(args);
			selectNonDrawerItem(sportsArtistsFragment, AppConstants.FRAGMENT_TAG_SPORTS_ARTISTS, getResources()
					.getString(R.string.title_popular_sports), true);
			
		} else if (fragmentTag.equals(AppConstants.FRAGMENT_TAG_MUSIC_ARTISTS)) {
			MusicArtistsFragment musicArtistsFragment = new MusicArtistsFragment();
			musicArtistsFragment.setArguments(args);
			selectNonDrawerItem(musicArtistsFragment, AppConstants.FRAGMENT_TAG_MUSIC_ARTISTS, getResources()
					.getString(R.string.title_categories), true);
			
		} else if (fragmentTag.equals(AppConstants.FRAGMENT_TAG_RECOMMENDED_ARTISTS)) {
			RecommendedArtistsFragment recommendedArtistsFragment = new RecommendedArtistsFragment();
			recommendedArtistsFragment.setArguments(args);
			selectNonDrawerItem(recommendedArtistsFragment, AppConstants.FRAGMENT_TAG_RECOMMENDED_ARTISTS, getResources()
					.getString(R.string.title_recommended), true);
			
		} else if (fragmentTag.equals(AppConstants.FRAGMENT_TAG_SELECTED_ARTIST_CATEGORY_FRAGMENT)) {
			SelectedArtistCategoryFragment selectedArtistCategoryFragment = new SelectedArtistCategoryFragment();
			selectedArtistCategoryFragment.setArguments(args);
			selectNonDrawerItem(selectedArtistCategoryFragment, AppConstants.FRAGMENT_TAG_SELECTED_ARTIST_CATEGORY_FRAGMENT, 
					getResources().getString(args.getInt(BundleKeys.SCREEN_TITLE)), true);
			
		} else if (fragmentTag.equals(AppConstants.FRAGMENT_TAG_TWITTER_SYNCING)) {
			//Log.d(TAG, "FRAGMENT_TAG_TWITTER_SYNCING");
			if (currentContentFragmentTag.equals(AppConstants.FRAGMENT_TAG_TWITTER)) {
				try {
					/**
					 * added this try catch as the app was crashing when user presses the twitter button to sync and
					 * after that if he immediately presses home then after around 2-3 sec, app was crashing with
					 * following error : java.lang.IllegalStateException: Can not perform this action after 
					 * onSaveInstanceState
					 */
					isCalledFromTwitterSection = true;
					onBackPressed();
					
				} catch (IllegalStateException e) {
					e.printStackTrace();
					/**
					 * return from here otherwise app will again crash at below lines.
					 */
					return;
				}
			}
			TwitterSyncingFragment twitterSyncingFragment = new TwitterSyncingFragment();
			twitterSyncingFragment.setArguments(args);
			selectNonDrawerItem(twitterSyncingFragment, fragmentTag, getResources().getString(
					R.string.title_twitter), true);
			
		} else if (fragmentTag.equals(AppConstants.FRAGMENT_TAG_NAVIGATION)) {
			NavigationFragment navigationFragment = new NavigationFragment();
			navigationFragment.setArguments(args);
			selectNonDrawerItem(navigationFragment, fragmentTag, getResources().getString(
					R.string.title_navigation), true);
		}
	}

	@Override
	public void onDrawerListFragmentViewCreated() {
		if (drawerItemSelectedPosition != AppConstants.INVALID_INDEX) {
			DrawerListFragment drawerListFragment = (DrawerListFragment) getSupportFragmentManager()
					.findFragmentByTag(DRAWER_LIST_FRAGMENT_TAG);
			drawerListFragment.getListView().setItemChecked(drawerItemSelectedPosition, true);
		}
	}
	
	@Override
	public void onDrawerItemSelected(int pos, Bundle args) {
		//Log.d(TAG, "onDrawerItemSelected(), pos = " + pos);
		/**
		 * process only if 
		 * 1) different selection is made or 
		 * 2) recommended tab is supposed to be selected by default;
		 * otherwise just close the drawer
		 */
		if (drawerItemSelectedPosition != pos || (args != null && args.containsKey(BundleKeys.SELECT_RECOMMENDED_EVENTS))) {
			getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			selectItem(pos, args);

		} else {
			if (!isTabletAndInLandscapeMode) {
				mDrawerLayout.closeDrawer(lnrLayoutRootNavDrawer);
			}
		}
	}

	@Override
	public void onArtistSelected(Artist artist) {
		onArtistSelected(artist, null);
	}
	
	@Override
	public void onArtistSelected(Artist artist, List<SharedElement> sharedElements) {
		ArtistDetailsFragment artistDetailsFragment = new ArtistDetailsFragment();
		Bundle args = new Bundle();
		args.putSerializable(BundleKeys.ARTIST, artist);
		if (sharedElements != null) {
			args.putSerializable(BundleKeys.SHARED_ELEMENTS, (Serializable) sharedElements);
		}
		artistDetailsFragment.setArguments(args);
		selectNonDrawerItem(artistDetailsFragment,
				AppConstants.FRAGMENT_TAG_ARTIST_DETAILS, "", true);
	}
	
	public void onArtistSelectedFromOtherTask(Artist artist, boolean addToBackStack) {
		ArtistDetailsFragment artistDetailsFragment = new ArtistDetailsFragment();
		Bundle args = new Bundle();
		args.putSerializable(BundleKeys.ARTIST, artist);
		// this is required to handle drawer indicator when this fragment is
		// resumed
		args.putBoolean(BundleKeys.IS_CALLED_FROM_OTHER_TASK, true);
		artistDetailsFragment.setArguments(args);
		selectNonDrawerItem(artistDetailsFragment, AppConstants.FRAGMENT_TAG_ARTIST_DETAILS, "", addToBackStack);
	}

	@Override
	public void onVenueSelected(Venue venue) {
		onVenueSelected(venue, null);
	}
	
	@Override
	public void onVenueSelected(Venue venue, List<SharedElement> sharedElements) {
		VenueDetailsFragment venueDetailsFragment = new VenueDetailsFragment();
		Bundle args = new Bundle();
		args.putSerializable(BundleKeys.VENUE, venue);
		if (sharedElements != null) {
			args.putSerializable(BundleKeys.SHARED_ELEMENTS, (Serializable) sharedElements);
		}
		venueDetailsFragment.setArguments(args);
		selectNonDrawerItem(venueDetailsFragment, AppConstants.FRAGMENT_TAG_VENUE_DETAILS, "", true);
	}

	@Override
	public void onEventSelected(Event event) {
		onEventSelected(event, null);
	}
	
	@Override
	public void onEventSelected(Event event, List<SharedElement> sharedElements) {
		EventDetailsFragment eventDetailsFragment = new EventDetailsFragment();
		Bundle args = new Bundle();
		args.putSerializable(BundleKeys.EVENT, event);
		if (sharedElements != null) {
			//this.sharedElements = sharedElements;
			//args.putString(BundleKeys.SHARED_IMG_TRANSITION_NAME, sharedElements.get(0).getTransitionName());
			args.putSerializable(BundleKeys.SHARED_ELEMENTS, (Serializable) sharedElements);
		}
		eventDetailsFragment.setArguments(args);
		selectNonDrawerItem(eventDetailsFragment, AppConstants.FRAGMENT_TAG_EVENT_DETAILS, "", true);
	}

	public void onEventSelectedFromOtherTask(Event event, boolean addToBackStack) {
		EventDetailsFragment eventDetailsFragment = new EventDetailsFragment();
		Bundle args = new Bundle();
		args.putSerializable(BundleKeys.EVENT, event);
		// this is required to handle drawer indicator when this fragment is
		// resumed
		args.putBoolean(BundleKeys.IS_CALLED_FROM_OTHER_TASK, true);
		eventDetailsFragment.setArguments(args);
		selectNonDrawerItem(eventDetailsFragment,
				AppConstants.FRAGMENT_TAG_EVENT_DETAILS, "", addToBackStack);
	}
	
	/**
	 * Handles navigation to drawerList Items only
	 * @param notificationType
	 */
	public void onNotificationClicked(NotificationType notificationType) {
		if (notificationType.getNavDrawerIndex() != AppConstants.INVALID_INDEX) {
			Bundle args = null;
			if (notificationType == NotificationType.RECOMMENDED_EVENTS) {
				args = new Bundle();
				args.putBoolean(BundleKeys.SELECT_RECOMMENDED_EVENTS, true);
			}
			onDrawerItemSelected(notificationType.getNavDrawerIndex(), args);
		}
	}
	
	@Override
	public void onSettingsItemClicked(SettingsItem settingsItem, Bundle args) {
		switch (settingsItem) {
				
			case SYNC_ACCOUNTS:
		    	ConnectAccountsFragment connectAccountsFragment = new ConnectAccountsFragment();
		    	selectNonDrawerItem(connectAccountsFragment, AppConstants.FRAGMENT_TAG_CONNECT_ACCOUNTS, 
		    			getResources().getString(R.string.navigation_drawer_item_sync_accounts), true);
		    	break;
				
			case CHANGE_LOCATION:
				ChangeLocationFragment changeLocationFragment = new ChangeLocationFragment();
				if (args != null) {
					changeLocationFragment.setArguments(args);
		    	}
				selectNonDrawerItem(changeLocationFragment, AppConstants.FRAGMENT_TAG_CHANGE_LOCATION, 
						getResources().getString(R.string.title_change_location), false);
				break;

			// TODO: comment following for disabling language
			case LANGUAGE:
				LanguageFragment languageFragment = new LanguageFragment();
				selectNonDrawerItem(languageFragment, AppConstants.FRAGMENT_TAG_LANGUAGE, 
						getResources().getString(R.string.title_language), true);
				break;
				
			case INVITE_FRIENDS:
				inviteFriends();
				break;
				
			case RATE_APP:
				rateApp();
				break;
				
			case ABOUT:
				AboutUsFragment aboutUsFragment = new AboutUsFragment();
				selectNonDrawerItem(aboutUsFragment, AppConstants.FRAGMENT_TAG_ABOUT_US, 
						getResources().getString(R.string.title_about_us), true);
				break;
				
			case EULA:
				EULAFragment eulaFragment = new EULAFragment();
				selectNonDrawerItem(eulaFragment, AppConstants.FRAGMENT_TAG_ABOUT_US, 
						getResources().getString(R.string.title_eula), true);
				break;
				
			case REPCODE:
				RepCodeFragment repCodeFragment = new RepCodeFragment();
				selectNonDrawerItem(repCodeFragment, AppConstants.FRAGMENT_TAG_REP_CODE, 
						getResources().getString(R.string.title_rep_code), true);
				break;
				
			default:
				break;
		}
	}

	@Override
	public void onMapClicked(Bundle args) {
		String uri;
		try {
			uri = "geo:"+ args.getDouble(BundleKeys.LAT) + "," + args.getDouble(BundleKeys.LON) + "?q=" 
					+ URLEncoder.encode(args.getString(BundleKeys.VENUE_NAME), AppConstants.CHARSET_NAME);
			startActivity(new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(uri)));

		} catch (UnsupportedEncodingException e) {
			// venue name could not be encoded, hence instead search on lat-lon.
			e.printStackTrace();
			uri = "geo:"+ args.getDouble(BundleKeys.LAT) + "," + args.getDouble(BundleKeys.LON) + "?q=" 
					+ args.getDouble(BundleKeys.LAT) + "," + args.getDouble(BundleKeys.LON);
			startActivity(new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(uri)));
			
		} catch (ActivityNotFoundException e) {
			// if user has uninstalled the google maps app
			e.printStackTrace();
			FullScreenAddressMapFragment fragment = new FullScreenAddressMapFragment();
			fragment.setArguments(args);
			selectNonDrawerItem(fragment, AppConstants.FRAGMENT_TAG_FULL_SCREEN_ADDRESS_MAP,
					args.getString(BundleKeys.VENUE_NAME), true);
		}
	}

	@Override
	public void onServiceSelected(Service service, Bundle args, boolean addToBackStack) {
		//Log.d(TAG, "onServiceSelected()");
		switch (service) {
		
		case GooglePlay:
			GooglePlayMusicFragment googlePlayMusicFragment = new GooglePlayMusicFragment();
			googlePlayMusicFragment.setArguments(args);
			selectNonDrawerItem(googlePlayMusicFragment, AppConstants.FRAGMENT_TAG_GOOGLE_PLAY_MUSIC, 
					getResources().getString(R.string.title_google_play), addToBackStack);
			break;

		case DeviceLibrary:
			DeviceLibraryFragment deviceLibraryFragment = new DeviceLibraryFragment();
			deviceLibraryFragment.setArguments(args);
			selectNonDrawerItem(deviceLibraryFragment,
					AppConstants.FRAGMENT_TAG_DEVICE_LIBRARY, getResources()
							.getString(R.string.title_device_library), addToBackStack);
			break;
			
		case Twitter:
			TwitterFragment twitterFragment = new TwitterFragment();
			twitterFragment.setArguments(args);
			selectNonDrawerItem(twitterFragment,
					AppConstants.FRAGMENT_TAG_TWITTER, getResources()
							.getString(R.string.title_twitter), addToBackStack);
			break;
			
		case Spotify:
			Intent intent = new Intent(getApplicationContext(), SpotifyActivity.class);
			intent.putExtras(args);
			startActivityForResult(intent, AppConstants.REQ_CODE_SPOTIFY);
			break;

		case Rdio:
			RdioFragment rdioFragment = new RdioFragment();
			rdioFragment.setArguments(args);
			selectNonDrawerItem(rdioFragment, AppConstants.FRAGMENT_TAG_RDIO,
					getResources().getString(R.string.title_rdio), addToBackStack);
			break;

		case Lastfm:
			LastfmFragment lastfmFragment = new LastfmFragment();
			lastfmFragment.setArguments(args);
			selectNonDrawerItem(lastfmFragment,
					AppConstants.FRAGMENT_TAG_LASTFM,
					getResources().getString(R.string.title_lastfm), addToBackStack);
			break;

		case Pandora:
			PandoraFragment pandoraFragment = new PandoraFragment();
			pandoraFragment.setArguments(args);
			selectNonDrawerItem(pandoraFragment,
					AppConstants.FRAGMENT_TAG_PANDORA, getResources()
							.getString(R.string.title_pandora), addToBackStack);
			break;

		default:
			break;
		}
	}

	@Override
	public void onFragmentResumed(Fragment fragment) {
		if (fragment instanceof LauncherFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, getResources().getString(R.string.title_launcher),
					AppConstants.FRAGMENT_TAG_LAUNCHER);

		} else if (fragment instanceof LoginFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, getResources().getString(R.string.title_login),
					AppConstants.FRAGMENT_TAG_LOGIN);

		} else if (fragment instanceof SignUpFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, getResources().getString(R.string.title_signup),
					AppConstants.FRAGMENT_TAG_SIGN_UP);

		//} else if (fragment instanceof DiscoverParentFragment) {
		} else if (fragment instanceof DiscoverFragment) {
			onFragmentResumed(INDEX_NAV_ITEM_DISCOVER, ((DiscoverFragment)fragment).getCurrentTitle(), 
					AppConstants.FRAGMENT_TAG_DISCOVER);

		} else if (fragment instanceof MyEventsFragment) {
			onFragmentResumed(INDEX_NAV_ITEM_MY_EVENTS, getResources().getString(R.string.title_my_events),
					AppConstants.FRAGMENT_TAG_MY_EVENTS);

		} else if (fragment instanceof ArtistsNewsListFragment) {
			onFragmentResumed(INDEX_NAV_ITEM_ARTISTS_NEWS, getResources().getString(R.string.title_artists_news),
					AppConstants.FRAGMENT_TAG_ARTISTS_NEWS_LIST);

		} else if (fragment instanceof FriendsActivityFragment) {
			onFragmentResumed(INDEX_NAV_ITEM_FRIENDS_ACTIVITY, getResources().getString(R.string.title_friends_activity),
					AppConstants.FRAGMENT_TAG_FRIENDS_ACTIVITY);

		} else if (fragment instanceof FollowingParentFragment) {
			onFragmentResumed(INDEX_NAV_ITEM_FOLLOWING, getResources().getString(R.string.title_following),
					AppConstants.FRAGMENT_TAG_FOLLOWING);
			
		} else if (fragment instanceof SettingsFragment) {
			onFragmentResumed(INDEX_NAV_ITEM_SETTINGS, getResources().getString(R.string.title_settings_mobile_app),
					AppConstants.FRAGMENT_TAG_SETTINGS);

		} else if (fragment instanceof ConnectAccountsFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, getResources().getString(R.string.navigation_drawer_item_sync_accounts),
					AppConstants.FRAGMENT_TAG_CONNECT_ACCOUNTS);

		} else if (fragment instanceof ChangeLocationFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, getResources().getString(R.string.title_change_location),
					AppConstants.FRAGMENT_TAG_CHANGE_LOCATION);
			
		} 
		// TODO: comment following for disabling language
		else if (fragment instanceof LanguageFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, getResources().getString(R.string.title_language),
					AppConstants.FRAGMENT_TAG_LANGUAGE);

		} else if (fragment instanceof AboutUsFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, getResources().getString(R.string.title_about_us),
					AppConstants.FRAGMENT_TAG_ABOUT_US);

		} else if (fragment instanceof EULAFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, getResources().getString(R.string.title_eula),
					AppConstants.FRAGMENT_TAG_EULA);

		} else if (fragment instanceof RepCodeFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, getResources().getString(R.string.title_rep_code),
					AppConstants.FRAGMENT_TAG_REP_CODE);

		} else if (fragment instanceof FullScreenAddressMapFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, fragment.getArguments().getString(BundleKeys.VENUE_NAME),
					AppConstants.FRAGMENT_TAG_FULL_SCREEN_ADDRESS_MAP);

		} else if (fragment instanceof SearchFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, getResources().getString(R.string.title_search_results),
					AppConstants.FRAGMENT_TAG_SEARCH);
			// Log.d(TAG, "fragment = " + fragment + ", query = " +
			// ((SearchFragment) fragment).getSearchQuery());
			/**
			 * on some devices onCreateOptionsMenu is called after onFragmentResumed, 
			 * So the search item might be null at this point
			 */
			if (searchItem != null) {
				MenuItemCompat.expandActionView(searchItem);
			}
			// searchItem.expandActionView();
			searchQuery = ((SearchFragment) fragment).getSearchQuery();

			// call to onPrepareOptionsMenu() will execute following 2
			// statements, so no need to do it here.
			/*
			 * searchView.setQuery(searchQuery, false); searchView.clearFocus();
			 */

		} else if (fragment instanceof DiscoverByCategoryFragment) {
			Bundle args = fragment.getArguments();
			int categoryPosition = args.getInt(BundleKeys.CATEGORY_POSITION);
			List<Category> categories = (List<Category>) args.getSerializable(BundleKeys.CATEGORIES);

			onFragmentResumed(AppConstants.INVALID_INDEX, categories.get(categoryPosition).getName(),
					AppConstants.FRAGMENT_TAG_DISCOVER_BY_CATEGORY);
			
		} else if (fragment instanceof EventDetailsFragment) {
			if (fragment.getArguments().containsKey(BundleKeys.IS_CALLED_FROM_OTHER_TASK)) {
				onFragmentCalledFromOtherTaskResumed(AppConstants.INVALID_INDEX, ((EventDetailsFragment)fragment).getCurrentTitle(),
						AppConstants.FRAGMENT_TAG_EVENT_DETAILS);

			} else {
				onFragmentResumed(AppConstants.INVALID_INDEX, ((EventDetailsFragment)fragment).getCurrentTitle(),
						AppConstants.FRAGMENT_TAG_EVENT_DETAILS);
			}

		} else if (fragment instanceof ArtistDetailsFragment) {
			if (fragment.getArguments().containsKey(BundleKeys.IS_CALLED_FROM_OTHER_TASK)) {
				onFragmentCalledFromOtherTaskResumed(AppConstants.INVALID_INDEX, ((ArtistDetailsFragment)fragment).getCurrentTitle(),
						AppConstants.FRAGMENT_TAG_ARTIST_DETAILS);

			} else {
				onFragmentResumed(AppConstants.INVALID_INDEX, ((ArtistDetailsFragment)fragment).getCurrentTitle(),
						AppConstants.FRAGMENT_TAG_ARTIST_DETAILS);
			}

		} else if (fragment instanceof VenueDetailsFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, ((VenueDetailsFragment)fragment).getCurrentTitle(),
					AppConstants.FRAGMENT_TAG_VENUE_DETAILS);

		} else if (fragment instanceof DeviceLibraryFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, getResources().getString(R.string.title_device_library),
					AppConstants.FRAGMENT_TAG_DEVICE_LIBRARY);

		} else if (fragment instanceof LoginSyncingFragment) {
			Bundle args = fragment.getArguments();
			boolean isForSignUp = args.getBoolean(BundleKeys.IS_FOR_SIGN_UP);
			String title = isForSignUp ? getResources().getString(R.string.title_signup) 
					: getResources().getString(R.string.title_login);
			onFragmentResumed(AppConstants.INVALID_INDEX, title, AppConstants.FRAGMENT_TAG_LOGIN_SYNCING);

		} else if (fragment instanceof TwitterFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, getResources().getString(R.string.title_twitter),
					AppConstants.FRAGMENT_TAG_TWITTER);

		} /*else if (fragment instanceof SpotifyFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, getResources().getString(R.string.title_spotify),
					AppConstants.FRAGMENT_TAG_SPOTIFY);

		}*/ else if (fragment instanceof RdioFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, getResources().getString(R.string.title_rdio),
					AppConstants.FRAGMENT_TAG_RDIO);

		} else if (fragment instanceof LastfmFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, getResources().getString(R.string.title_lastfm),
					AppConstants.FRAGMENT_TAG_LASTFM);

		} else if (fragment instanceof PandoraFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, getResources().getString(R.string.title_pandora),
					AppConstants.FRAGMENT_TAG_PANDORA);
			
		} else if (fragment instanceof TwitterSyncingFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, getResources().getString(R.string.title_twitter),
					AppConstants.FRAGMENT_TAG_TWITTER_SYNCING);

		} else if (fragment instanceof GooglePlayMusicFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, getResources().getString(R.string.title_google_play),
					AppConstants.FRAGMENT_TAG_GOOGLE_PLAY_MUSIC);

		} else if (fragment instanceof FollowMoreArtistsFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, getResources().getString(R.string.title_find_more_artist),
					AppConstants.FRAGMENT_TAG_FOLLOW_MORE_ARTISTS);

		} else if (fragment instanceof PopularArtistsFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, getResources().getString(R.string.title_popular),
					AppConstants.FRAGMENT_TAG_POPULAR_ARTISTS);

		} else if (fragment instanceof SportsArtistsFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, getResources().getString(R.string.title_popular_sports),
					AppConstants.FRAGMENT_TAG_SPORTS_ARTISTS);
			
		} else if (fragment instanceof RecommendedArtistsFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, getResources().getString(R.string.title_recommended),
					AppConstants.FRAGMENT_TAG_RECOMMENDED_ARTISTS);

		} else if (fragment instanceof MusicArtistsFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, getResources().getString(R.string.title_categories),
					AppConstants.FRAGMENT_TAG_MUSIC_ARTISTS);

		} else if (fragment instanceof SelectedArtistCategoryFragment) {
			Bundle args = fragment.getArguments();
			onFragmentResumed(AppConstants.INVALID_INDEX, getResources().getString(args.getInt(BundleKeys.SCREEN_TITLE)),
					AppConstants.FRAGMENT_TAG_SELECTED_ARTIST_CATEGORY_FRAGMENT);
			
		} else if (fragment instanceof NavigationFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, getResources().getString(R.string.title_navigation), 
					AppConstants.FRAGMENT_TAG_NAVIGATION);
			
		} else if (fragment instanceof WebViewFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, getResources().getString(R.string.title_web), 
					AppConstants.FRAGMENT_TAG_WEB_VIEW);
		}
	}

	public void hideSoftKeypad() {
		//Log.d(TAG, "hideSoftKeypad()");
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(searchView.getApplicationWindowToken(), 0);
	}

	@Override
	public boolean onQueryTextChange(String arg0) {
		return false;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		//Log.d(TAG, "onQueryTextSubmit(), query = " + query + ", currentContentFragmentTag = " + currentContentFragmentTag);
		closeDrawer();
		if (query == null || query.length() == 0) {
			return true;
		}

		searchQuery = query;
		hideSoftKeypad();

		SearchFragment searchFragment;
		if (!currentContentFragmentTag.equals(AppConstants.FRAGMENT_TAG_SEARCH)) {
			Fragment fragment = getSupportFragmentManager().findFragmentByTag(currentContentFragmentTag);
			if (fragment instanceof FragmentHavingFragmentInRecyclerView) {
				/**
				 * to mark isOnPushedToBackStackCalled true so that onPoppedFromBackStack() can apply 
				 * currentFragmentTag, title, etc changes
				 */
				((FragmentHavingFragmentInRecyclerView)fragment).onPushedToBackStackFHFIR();
			}
			searchFragment = new SearchFragment();
			Bundle args = new Bundle();
			args.putString(BundleKeys.QUERY, query);
			searchFragment.setArguments(args);
			selectNonDrawerItem(searchFragment, AppConstants.FRAGMENT_TAG_SEARCH,
					getResources().getString(R.string.title_search_results), true);

		} else {
			searchFragment = (SearchFragment) getSupportFragmentManager()
					.findFragmentByTag(AppConstants.FRAGMENT_TAG_SEARCH);
			searchFragment.onQueryTextSubmit(searchQuery);
		}

		return true;
	}

	@Override
	public void onLocationChanged(Bundle args) {
		onDrawerItemSelected(INDEX_NAV_ITEM_DISCOVER, args);
	}

	public void setDrawerIndicatorEnabled(boolean enable) {
		//Log.d(TAG, "enable = " + enable);
		isDrawerIndicatorEnabled = enable;
		if (mDrawerToggle != null) {
			mDrawerToggle.setDrawerIndicatorEnabled(enable);
		}
	}
	
	public void setDrawerLockMode(boolean lock) {
		//Log.d(TAG, "setDrawerLockMode = " + lock);
		if (mDrawerLayout != null) {
			if (lock) {
				mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
				
			} else {
				mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
			}
		}
	}
	
	@Override
	public void onFragmentResumed(Fragment fragment, int drawerPosition, String actionBarTitle) {
		//Added right now just for Bosch Main Activity
	}
	
	@Override
	public void onBackPressed() throws IllegalStateException {
		/**
		 * this added as after the Syncing screen when the onbackpressed occurs, on Connect account screen back arrow
		 * is retained in tablet landscape mode. So, to resolve the issue below statements are added.
		 */
		if (isTabletAndInLandscapeMode && currentContentFragmentTag.equals(AppConstants.FRAGMENT_TAG_LOGIN_SYNCING)) {
			getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
		}
		
		if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
			//Log.d(TAG, "onBackPressed()");
			try {
				/**
				 * This try catch will handle IllegalStateException which may occur if onBackPressed() on Super
				 * has been called after the onSaveInstanceState().
				 */
				if (!isTabletAndInLandscapeMode) {
					Fragment currentFrag = getSupportFragmentManager().findFragmentByTag(currentContentFragmentTag);
					if (currentFrag instanceof CustomSharedElementTransitionDestination && 
							currentFrag.getArguments() != null && currentFrag.getArguments().containsKey(
									BundleKeys.SHARED_ELEMENTS)) {
						//Log.d(TAG, "CustomSharedElementTransitionDestination, exitAnimCalled = " + exitAnimCalled);
						if (exitAnimCalled) {
				    		exitAnimCalled = false;
				    		super.onBackPressed();
				    		
				    	} else {
				    		exitAnimCalled = true;
				    		((CustomSharedElementTransitionDestination)currentFrag).exitAnimation();
				    	}
						
					} else {
						//Log.d(TAG, "!CustomSharedElementTransitionDestination");
						super.onBackPressed();
					}
					
				} else {
					super.onBackPressed();
				}
				
			} catch (IllegalStateException e) {
				if (isCalledFromTwitterSection) {
					isCalledFromTwitterSection = false;
					throw e;
					
				} else {
					e.printStackTrace();					
				}
			}
			
		} else {
			//Log.d(TAG, "moveTaskToBack()");
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
		}
	}

	@Override
	public void onConnectionFailure() {
		GeneralDialogFragment generalDialogFragment = GeneralDialogFragment.newInstance(this, 
				getResources().getString(R.string.no_internet_connectivity),
				getResources().getString(R.string.connection_lost), "Ok", null, false);
		generalDialogFragment.show(getSupportFragmentManager(), DIALOG_FRAGMENT_TAG_CONNECTION_LOST);		
	}

	@Override
	public void doPositiveClick(String dialogTag) {
		if (dialogTag.equals(DIALOG_FRAGMENT_TAG_CONNECTION_LOST)) {
			DialogFragment dialogFragment = (DialogFragment) getSupportFragmentManager()
					.findFragmentByTag(DIALOG_FRAGMENT_TAG_CONNECTION_LOST);
			if (dialogFragment != null) {
				dialogFragment.dismiss();
			}
		}
	}

	@Override
	public void doNegativeClick(String dialogTag) {}

	@Override
	public void onLocaleChanged() {
		DrawerListFragment drawerListFragment = (DrawerListFragment) getSupportFragmentManager()
				.findFragmentByTag(DRAWER_LIST_FRAGMENT_TAG);
		if (drawerListFragment != null) {
			drawerListFragment.refreshDrawerList();
		}
		/**
		 * refresh the current screen's title only if it is Language fragment.
		 */
		if (currentContentFragmentTag.equals(AppConstants.FRAGMENT_TAG_LANGUAGE)) {
			mTitle = getResources().getString(R.string.title_language);
			updateTitle();
		}
		/**
		 * 	refresh the SearchView	
		 */
		searchView.setQueryHint(getResources().getString(R.string.menu_search));
	}

	/**
	 * TODO: used by ProxyService only, remove if not needed
	 */
	public void onProxyClosed() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRegistration(LoginType loginType, Bundle args, boolean addToBackStack) {
		LoginSyncingFragment loginSyncingFragment = new LoginSyncingFragment();
		loginSyncingFragment.setArguments(args);
		String title = args.getBoolean(BundleKeys.IS_FOR_SIGN_UP) ? getResources().getString(R.string.title_signup) 
				: getResources().getString(R.string.title_login);
		selectNonDrawerItem(loginSyncingFragment, AppConstants.FRAGMENT_TAG_LOGIN_SYNCING, title, addToBackStack);
	}
	
	public boolean isDrawerOpen() {
		if (mDrawerLayout != null) {
			return mDrawerLayout.isDrawerOpen(lnrLayoutRootNavDrawer);
			
		} else {
			return false;
		}
	}
	
	private void closeDrawer() {
		if (isDrawerOpen() && mDrawerLayout != null) {
			mDrawerLayout.closeDrawer(lnrLayoutRootNavDrawer);
		}
	}

	public void hideDrawerList() {
		lnrLayoutRootNavDrawer.setVisibility(View.GONE);
	}
	
	public void unHideDrawerList() {
		lnrLayoutRootNavDrawer.setVisibility(View.VISIBLE);
	}
	
	/**
	 * @param viewVisibility
	 * @param colorRes Required only for viewVisibility = View.VISIBLE
	 */
	public void setVStatusBarVisibility(int viewVisibility, int colorRes) {
		//Log.d(TAG, "setVStatusBarVisibility(), viewVisibility = " + viewVisibility);
		if (VersionUtil.isApiLevelAbove18() && vStatusBar != null) {
			vStatusBar.setVisibility(viewVisibility);
			
			if (viewVisibility == View.VISIBLE) {
				vStatusBar.setBackgroundColor(getResources().getColor(colorRes));
			}
			
			int drawerStatusBarVisibility = (viewVisibility == View.GONE) ? View.VISIBLE : View.GONE; 
			vDrawerStatusBar.setVisibility(drawerStatusBarVisibility);
		}
	}
	
	/**
	 * @param viewVisibility
	 * @param colorRes Required only for viewVisibility = View.VISIBLE
	 */
	public void setVStatusBarLayeredVisibility(int viewVisibility, int colorRes) {
		//Log.d(TAG, "setVStatusBarLayeredVisibility(), viewVisibility = " + viewVisibility);
		if (VersionUtil.isApiLevelAbove18() && vStatusBarLayered != null) {
			vStatusBarLayered.setVisibility(viewVisibility);
			
			if (viewVisibility == View.VISIBLE) {
				vStatusBarLayered.setBackgroundColor(getResources().getColor(colorRes));
			}
		}
	}
	
	public void setToolbarBg(int color) {
		//Log.d(TAG, "setToolbarBg(), color = " + color);
		toolbar.setBackgroundColor(color);
	}
	
	public void setToolbarElevation(float elevation) {
		ViewCompat.setElevation(toolbar, elevation);
	}
	
	public void animateToolbarElevation(float start, float end) {
		ObjectAnimator elevateAnim = ObjectAnimator.ofFloat(toolbar, "elevation", start, end);
		elevateAnim.setDuration(100);
		elevateAnim.start();
	}
	
	public void updateToolbarOnDrawerSlide(float slideOffset) {
        int newAlpha = (int) (slideOffset * 255);
        int color = getResources().getColor(R.color.colorPrimary);
        toolbar.setBackgroundColor(Color.argb(newAlpha, Color.red(color), Color.green(color), Color.blue(color)));
    }
	
	public void onSharedElementAnimStart() {
		if (prevCustomSharedElementTransitionSource != null) {
			prevCustomSharedElementTransitionSource.hideSharedElements();
			prevCustomSharedElementTransitionSource = null;
		}
	}
	
	public void expandSearchView() {
		searchView.setIconified(false);
	}
}
