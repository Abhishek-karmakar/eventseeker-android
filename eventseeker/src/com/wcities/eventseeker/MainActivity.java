package com.wcities.eventseeker;

import java.util.List;
import java.util.Set;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bosch.myspin.serversdk.MySpinException;
import com.bosch.myspin.serversdk.MySpinServerSDK;
import com.ford.syncV4.proxy.SyncProxyALM;
import com.ford.syncV4.transport.TransportType;
import com.wcities.eventseeker.ChangeLocationFragment.ChangeLocationFragmentListener;
import com.wcities.eventseeker.ConnectAccountsFragment.ConnectAccountsFragmentListener;
import com.wcities.eventseeker.ConnectAccountsFragment.Service;
import com.wcities.eventseeker.DrawerListFragment.DrawerListFragmentListener;
import com.wcities.eventseeker.GetStartedFragment.GetStartedFragmentListener;
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
import com.wcities.eventseeker.interfaces.ArtistListener;
import com.wcities.eventseeker.interfaces.EventListener;
import com.wcities.eventseeker.interfaces.FragmentLoadedFromBackstackListener;
import com.wcities.eventseeker.interfaces.MapListener;
import com.wcities.eventseeker.interfaces.ReplaceFragmentListener;
import com.wcities.eventseeker.interfaces.VenueListener;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.GPlusUtil;

public class MainActivity extends ActionBarActivity implements
		DrawerListFragmentListener, GetStartedFragmentListener,
		ReplaceFragmentListener, EventListener, ArtistListener, VenueListener,
		FragmentLoadedFromBackstackListener, MapListener,
		ConnectAccountsFragmentListener, SearchView.OnQueryTextListener,
		ChangeLocationFragmentListener {

	private static final String TAG = MainActivity.class.getName();

	private static final int INDEX_NAV_ITEM_DISCOVER = DrawerListFragment.SECT_1_HEADER_POS + 1;
	private static final int INDEX_NAV_ITEM_MY_EVENTS = INDEX_NAV_ITEM_DISCOVER + 1;
	protected static final int INDEX_NAV_ITEM_FOLLOWING = INDEX_NAV_ITEM_MY_EVENTS + 1;
	private static final int INDEX_NAV_ITEM_ARTISTS_NEWS = INDEX_NAV_ITEM_FOLLOWING + 1;
	private static final int INDEX_NAV_ITEM_FRIENDS_ACTIVITY = INDEX_NAV_ITEM_ARTISTS_NEWS + 1;
	protected static final int INDEX_NAV_ITEM_CONNECT_ACCOUNTS = DrawerListFragment.SECT_2_HEADER_POS + 1;
	private static final int INDEX_NAV_ITEM_CHANGE_LOCATION = INDEX_NAV_ITEM_CONNECT_ACCOUNTS + 1;
	protected static final int INDEX_NAV_ITEM_INVITE_FRIENDS = DrawerListFragment.SECT_3_HEADER_POS + 1;
	private static final int INDEX_NAV_ITEM_RATE_APP = INDEX_NAV_ITEM_INVITE_FRIENDS + 1;
	private static final int INDEX_NAV_ITEM_ABOUT_US = INDEX_NAV_ITEM_RATE_APP + 1;
	private static final int INDEX_NAV_ITEM_EULA = INDEX_NAV_ITEM_ABOUT_US + 1;
	private static final int INDEX_NAV_ITEM_REP_CODE = INDEX_NAV_ITEM_EULA + 1;
	
	private static final String DRAWER_LIST_FRAGMENT_TAG = "drawerListFragment";

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
	
	public static MainActivity getInstance() {
		return instance;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//Log.d(TAG, "deviceId = " + DeviceUtil.getDeviceId((EventSeekr) getApplication()));
		
		try {
			MySpinServerSDK.sharedInstance().registerApplication(getApplication());
		} catch (MySpinException e) {
			e.printStackTrace();
		}
		
		if (MySpinServerSDK.sharedInstance().isConnected()) {
			startActivity(new Intent(getApplicationContext(), BoschMainActivity.class));
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

		isDrawerIndicatorEnabled = true;
		if (savedInstanceState != null) {

			mTitle = savedInstanceState.getString(BundleKeys.ACTION_BAR_TITLE);
			currentContentFragmentTag = savedInstanceState
					.getString(BundleKeys.CURRENT_CONTENT_FRAGMENT_TAG);
			drawerItemSelectedPosition = savedInstanceState
					.getInt(BundleKeys.DRAWER_ITEM_SELECTED_POSITION);
			isDrawerIndicatorEnabled = savedInstanceState
					.getBoolean(BundleKeys.IS_DRAWER_INDICATOR_ENABLED);
		}
		
		lnrLayoutRootNavDrawer = (LinearLayout) findViewById(R.id.rootNavigationDrawer);

		//Log.d(TAG, "lnrLayoutRootNavDrawer : " + lnrLayoutRootNavDrawer);

		//Log.d(TAG, "isTabletAndInLandscapeMode : " + isTabletAndInLandscapeMode);
		if (!isTabletAndInLandscapeMode) {

			mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
			mDrawerToggle = new ActionBarDrawerToggle(this, // host Activity
					mDrawerLayout, // DrawerLayout object
					R.drawable.sidenav, // nav drawer icon to replace 'Up' caret
					R.string.drawer_open, // "open drawer" description
					R.string.drawer_close // "close drawer" description
			) {

				/**
				 * Called when a drawer has settled in a completely closed
				 * state.
				 */
				/*
				 * public void onDrawerClosed(View view) {
				 * getSupportActionBar().setTitle(mTitle); }
				 */

				/** Called when a drawer has settled in a completely open state. */
				public void onDrawerOpened(View drawerView) {
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
				}
			};

			setDrawerIndicatorEnabled(isDrawerIndicatorEnabled);
			Log.i(TAG, "isDrawerIndicatorEnabled : " + isDrawerIndicatorEnabled);
			// Set the drawer toggle as the DrawerListener
			mDrawerLayout.setDrawerListener(mDrawerToggle);
			
			getSupportActionBar().setDisplayOptions(
					ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_HOME
					| ActionBar.DISPLAY_HOME_AS_UP );
		} else {
			
			int displayOptions;
			
			if(getSupportFragmentManager().getBackStackEntryCount() > 0) {
				displayOptions = ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_HOME
						| ActionBar.DISPLAY_HOME_AS_UP;
			} else {
				displayOptions = ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_HOME;	
			}
			
			getSupportActionBar().setDisplayOptions(displayOptions);
		}
		
		/**
		 * setIcon null throws NullPointerException while expanding
		 * searchView in SearchFragment. So need to set any transparent icon
		 * rather than null.
		 */
		getSupportActionBar().setIcon(R.drawable.ic_actionbar_app_icon);
		
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
			if (getIntent().hasExtra(BundleKeys.EVENT)) {
				onEventSelectedFromOtherTask((Event) getIntent()
						.getSerializableExtra(BundleKeys.EVENT), false);

			} else {
				GetStartedFragment getStartedFragment = new GetStartedFragment();
				selectNonDrawerItem(getStartedFragment, AppConstants.FRAGMENT_TAG_GET_STARTED, getResources()
								.getString(R.string.title_get_started), false);
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
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (intent.hasExtra(BundleKeys.EVENT)) {
			onEventSelectedFromOtherTask((Event) intent.getSerializableExtra(BundleKeys.EVENT), true);
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
				}
			}
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
		DeviceUtil.removeDeviceLocationListener();
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
		boolean disableSearch = currentContentFragmentTag
				.equals(AppConstants.FRAGMENT_TAG_CHANGE_LOCATION)
				|| currentContentFragmentTag
						.equals(AppConstants.FRAGMENT_TAG_GET_STARTED)
				|| currentContentFragmentTag
						.equals(AppConstants.FRAGMENT_TAG_FULL_SCREEN_ADDRESS_MAP);
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
			
			if(AppConstants.FRAGMENT_TAG_LOGIN_SYNCING.equals(currentContentFragmentTag)) {
				return true;
			} else if (!isTabletAndInLandscapeMode) {
				if (mDrawerToggle.isDrawerIndicatorEnabled()) {
					if (mDrawerLayout.isDrawerOpen(lnrLayoutRootNavDrawer)) {
						mDrawerLayout.closeDrawer(lnrLayoutRootNavDrawer);

					} else {
						mDrawerLayout.openDrawer(lnrLayoutRootNavDrawer);
					}

				} else {
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
								| ActionBar.DISPLAY_SHOW_CUSTOM  | ActionBar.DISPLAY_SHOW_HOME);
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
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		// Log.d(TAG, "onSaveInstanceState()");
	
		outState.putString(BundleKeys.ACTION_BAR_TITLE, mTitle);
		outState.putString(BundleKeys.CURRENT_CONTENT_FRAGMENT_TAG,
				currentContentFragmentTag);
		outState.putInt(BundleKeys.DRAWER_ITEM_SELECTED_POSITION,
				drawerItemSelectedPosition);
		outState.putBoolean(BundleKeys.IS_DRAWER_INDICATOR_ENABLED,
				isDrawerIndicatorEnabled);
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
			if (AppConstants.FRAGMENT_TAG_WEB_VIEW.equals(currentContentFragmentTag)) {
				WebViewFragment webViewFragment = (WebViewFragment) getSupportFragmentManager()
						.findFragmentByTag(AppConstants.FRAGMENT_TAG_WEB_VIEW);
				if (webViewFragment.onKeyDown()) {
					return true;
					
				} else {
					return super.onKeyDown(keyCode, event);
				}
				
			} else if (AppConstants.FRAGMENT_TAG_LOGIN_SYNCING.equals(currentContentFragmentTag)) {
				return true;
				
			} else {
				if (isTabletAndInLandscapeMode && getSupportFragmentManager().getBackStackEntryCount() == 1) {
					/**
					 * Following line is to reset display options when coming back to initial screen.
					 * This is required only in case of tablet in landscape orientation, since we don't 
					 * have navigation drawer in this case resulting in different behavior observed for 
					 * home_as_up icon.
					 * DISPLAY_SHOW_CUSTOM is used for discover screen on tablet.
					 * e.g. - Launch --> Discover --> Event Details --> hardware back. In this case tabs & 
					 * back should disappear from actionbar.
					 */
					getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_HOME
							| ActionBar.DISPLAY_SHOW_CUSTOM);
				}
				return super.onKeyDown(keyCode, event);
			}
		}
		
		if (!isTabletAndInLandscapeMode) {
			if (keyCode == KeyEvent.KEYCODE_MENU) {
				if (mDrawerToggle.isDrawerIndicatorEnabled()) {
					if (mDrawerLayout.isDrawerOpen(lnrLayoutRootNavDrawer)) {
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
				if ((mBtAdapter.isEnabled() && mBtAdapter.getBondedDevices()
						.isEmpty() != true)) {
					Log.i(TAG, "pairedDevices");
					// Get a set of currently paired devices
					Set<BluetoothDevice> pairedDevices = mBtAdapter
							.getBondedDevices();

					// Check if there is a paired device with the name "SYNC"
					if (pairedDevices.size() > 0) {
						// Log.i(TAG, "pairedDevices > 0");
						for (BluetoothDevice device : pairedDevices) {
							// Log.i(TAG, "device.getName() = " +
							// device.getName());
							if (device.getName().trim().equals("SYNC")) {
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
							Intent startIntent = new Intent(this,
									AppLinkService.class);
							startService(startIntent);

						} else {
							// if the service is already running and proxy is
							// up,
							// set this as current UI activity
							AppLinkService.getInstance().setCurrentActivity(
									this);
							// Log.i(TAG, " proxyAlive == true success");
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
		Log.d(TAG, "addDrawerListFragment");
		FragmentTransaction fragmentTransaction = getSupportFragmentManager()
				.beginTransaction();
		DrawerListFragment drawerListFragment = new DrawerListFragment();
		fragmentTransaction.add(R.id.rootNavigationDrawer, drawerListFragment,
				DRAWER_LIST_FRAGMENT_TAG);
		fragmentTransaction.commit();
	}

	/*
	 * private void getOverflowMenu() { try { ViewConfiguration config =
	 * ViewConfiguration.get(this); Field menuKeyField =
	 * ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey"); if
	 * (menuKeyField != null) { menuKeyField.setAccessible(true);
	 * menuKeyField.setBoolean(config, false); }
	 * 
	 * } catch (Exception e) { e.printStackTrace(); } }
	 */

	private void onFragmentResumed(int position, String title,
			String fragmentTag) {
		Log.d(TAG, "onFragmentResumed() - " + fragmentTag);
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
		}
		mTitle = title;
		updateTitle();

		currentContentFragmentTag = fragmentTag;
	}
	
	private void onFragmentCalledFromOtherTaskResumed(int position,
			String title, String fragmentTag) {
		// Log.d(TAG, "onFragmentResumed() - " + fragmentTag);
		drawerItemSelectedPosition = position;
		if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
			setDrawerIndicatorEnabled(true);
		} 
		mTitle = title;
		updateTitle();

		currentContentFragmentTag = fragmentTag;
	}

	/** Swaps fragments in the main content view */
	private void selectItem(int position) {
		Log.d(TAG, "selectItem() + pos : " + position);
		//if (position != INDEX_NAV_ITEM_LATEST_NEWS) {
			drawerItemSelectedPosition = position;
				
			setDrawerIndicatorEnabled(true);
				
			if(isTabletAndInLandscapeMode){
				
				getSupportActionBar().setIcon(R.drawable.ic_actionbar_app_icon);
				getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_HOME);

			}
			
			boolean isDrawerListFragmentFound = updateDrawerListCheckedItem(position);
			if (!isDrawerListFragmentFound) {
				return;
			}
		//}

	    switch (position) {
	    
		case INDEX_NAV_ITEM_DISCOVER:
			DiscoverParentFragment discoverFragment; 
			if(isTablet) {
				discoverFragment = new DiscoverFragmentTab();
			} else {
				discoverFragment = new DiscoverFragment();
			}
			replaceContentFrameByFragment(discoverFragment, AppConstants.FRAGMENT_TAG_DISCOVER, getResources()
							.getString(R.string.title_discover), false);
			break;

		case INDEX_NAV_ITEM_MY_EVENTS:
			MyEventsFragment fragment = new MyEventsFragment();
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
		case INDEX_NAV_ITEM_CONNECT_ACCOUNTS:
	    	ConnectAccountsFragment connectAccountsFragment = new ConnectAccountsFragment();
	    	replaceContentFrameByFragment(connectAccountsFragment, AppConstants.FRAGMENT_TAG_CONNECT_ACCOUNTS, 
	    			getResources().getString(R.string.title_connect_accounts), false);
	    	break;
			
		case INDEX_NAV_ITEM_CHANGE_LOCATION:
			ChangeLocationFragment changeLocationFragment = new ChangeLocationFragment();
			replaceContentFrameByFragment(changeLocationFragment, AppConstants.FRAGMENT_TAG_CHANGE_LOCATION, 
					getResources().getString(R.string.title_change_location), false);
			break;
			
		case INDEX_NAV_ITEM_INVITE_FRIENDS:
			inviteFriends();
			break;
			
		case INDEX_NAV_ITEM_RATE_APP:
			rateApp();
			break;
			
		case INDEX_NAV_ITEM_ABOUT_US:
			AboutUsFragment aboutUsFragment = new AboutUsFragment();
			replaceContentFrameByFragment(aboutUsFragment, AppConstants.FRAGMENT_TAG_ABOUT_US, 
					getResources().getString(R.string.title_about_us), false);
			break;
			
		case INDEX_NAV_ITEM_EULA:
			EULAFragment eulaFragment = new EULAFragment();
			replaceContentFrameByFragment(eulaFragment, AppConstants.FRAGMENT_TAG_ABOUT_US, 
					getResources().getString(R.string.title_eula), false);
			break;
			
		case INDEX_NAV_ITEM_REP_CODE:
			RepCodeFragment repCodeFragment = new RepCodeFragment();
			replaceContentFrameByFragment(repCodeFragment, AppConstants.FRAGMENT_TAG_REP_CODE, 
					getResources().getString(R.string.title_rep_code), false);
			break;

		default:
			break;
		}
	    
	    if(!isTabletAndInLandscapeMode){
	    	mDrawerLayout.closeDrawer(lnrLayoutRootNavDrawer);
	    }
	}
	
	/**
	 * @param position
	 * @return true if DrawerListFragment instance is existing (not null)
	 */
	private boolean updateDrawerListCheckedItem(int position) {
		if (position == AppConstants.INVALID_INDEX) {
			return false;
		}
		
		DrawerListFragment drawerListFragment = (DrawerListFragment) getSupportFragmentManager()
				.findFragmentByTag(DRAWER_LIST_FRAGMENT_TAG);
		if (drawerListFragment == null) {
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
		String url = "https://play.google.com/store/apps/details?id="
				+ getPackageName();
		Intent intent = new Intent(android.content.Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		intent.putExtra(Intent.EXTRA_TEXT, "Checkout eventseeker" + " " + url);
		try {
			startActivityForResult(intent, AppConstants.REQ_CODE_INVITE_FRIENDS);

		} catch (ActivityNotFoundException e) {
			Toast.makeText(getApplicationContext(),
					"Error, this action cannot be completed at this time.",
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
			Toast.makeText(getApplicationContext(),
					"Error, this action cannot be completed at this time.",
					Toast.LENGTH_SHORT).show();
		}
	}

	private void replaceContentFrameByFragment(Fragment replaceBy,
			String replaceByFragmentTag, String newTitle, boolean addToBackStack) {
		//Log.d(TAG, "replaceContentFrameByFragment() - newTitle = " + newTitle);
		mTitle = newTitle;
		updateTitle();

		FragmentTransaction fragmentTransaction = getSupportFragmentManager()
				.beginTransaction();
		fragmentTransaction.replace(R.id.content_frame, replaceBy,
				replaceByFragmentTag);

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
		if (!currentContentFragmentTag
				.equals(AppConstants.FRAGMENT_TAG_ARTIST_DETAILS)
				&& !currentContentFragmentTag
						.equals(AppConstants.FRAGMENT_TAG_EVENT_DETAILS)
				&& !currentContentFragmentTag
						.equals(AppConstants.FRAGMENT_TAG_VENUE_DETAILS)) {
			supportInvalidateOptionsMenu();
		}
		
		//Log.d(TAG, "back stack count = " + getSupportFragmentManager().getBackStackEntryCount());
	}

	/*
	 * private void revertCheckedDrawerItemStateIfAny() {
	 * drawerItemSelectedPosition = AppConstants.INVALID_INDEX;
	 * DrawerListFragment drawerListFragment = (DrawerListFragment)
	 * getSupportFragmentManager() .findFragmentByTag(DRAWER_LIST_FRAGMENT_TAG);
	 * 
	 * try { int previousCheckedItemPos =
	 * drawerListFragment.getListView().getCheckedItemPosition(); if
	 * (previousCheckedItemPos != -1) {
	 * drawerListFragment.getListView().setItemChecked(previousCheckedItemPos,
	 * false); }
	 * 
	 * } catch (IllegalStateException e) {
	 *//**
	 * This exception is thrown when this function call hierarchy starts from
	 * onCreate(), since content view for drawerListFragment is not created yet.
	 * In this case, to accomplish this we have a callback
	 * 'onDrawerListFragmentViewCreated()' which does the same task of marking
	 * right drawer item as selected, if any.
	 */
	/*
	 * Log.i(TAG, "IllegalSTateException"); e.printStackTrace(); } }
	 */

	private void updateTitle() {
		/*
		 * if (mDrawerLayout.isDrawerOpen(lnrLayoutRootNavDrawer)) {
		 * getSupportActionBar().setTitle(AppConstants.NAVIGATION_DRAWER_TITLE);
		 * 
		 * } else {
		 */
		getSupportActionBar().setTitle(mTitle);
		// }
	}
	
	private void selectNonDrawerItem(Fragment replaceBy, String replaceByFragmentTag, String newTitle, 
		boolean addToBackStack) {
		//Log.d(TAG, "onDrawerItemSelected(), newTitle = " + newTitle + ", addToBackStack = " + addToBackStack);
		
		drawerItemSelectedPosition = AppConstants.INVALID_INDEX;
		// revertCheckedDrawerItemStateIfAny();
		setDrawerIndicatorEnabled(!addToBackStack);
		
		if (isTabletAndInLandscapeMode) {
			getSupportActionBar().setIcon(R.drawable.ic_actionbar_app_icon);			
			if (addToBackStack) {
				getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_HOME
						| ActionBar.DISPLAY_HOME_AS_UP);
			}
		}
		// getSupportActionBar().setDisplayHomeAsUpEnabled(addToBackStack);
		replaceContentFrameByFragment(replaceBy, replaceByFragmentTag,
				newTitle, addToBackStack);
	}

	@Override
	public void replaceByFragment(String fragmentTag, Bundle args) {
	
		if (fragmentTag.equals(AppConstants.FRAGMENT_TAG_DISCOVER_BY_CATEGORY)) {
			int categoryPosition = args.getInt(BundleKeys.CATEGORY_POSITION);
			List<Category> categories = (List<Category>) args.getSerializable(BundleKeys.CATEGORIES);

			DiscoverByCategoryFragment discoverByCategoryFragment = new DiscoverByCategoryFragment();
			discoverByCategoryFragment.setArguments(args);
			selectNonDrawerItem(discoverByCategoryFragment, fragmentTag, 
					categories.get(categoryPosition).getName(), true);
			
		} else if (fragmentTag.equals(AppConstants.FRAGMENT_TAG_WEB_VIEW)) {
			WebViewFragment webViewFragment = new WebViewFragment();
			webViewFragment.setArguments(args);
			selectNonDrawerItem(webViewFragment, fragmentTag, 
					getResources().getString(R.string.title_web), true);
			
		} else if (fragmentTag.equals(AppConstants.FRAGMENT_TAG_CONNECT_ACCOUNTS)) {
			selectItem(INDEX_NAV_ITEM_CONNECT_ACCOUNTS);
			
		} else if (fragmentTag.equals(AppConstants.FRAGMENT_TAG_TWITTER_SYNCING)) {
			//Log.d(TAG, "FRAGMENT_TAG_TWITTER_SYNCING");
			if (currentContentFragmentTag.equals(AppConstants.FRAGMENT_TAG_TWITTER)) {
				onBackPressed();
			}
			TwitterSyncingFragment twitterSyncingFragment = new TwitterSyncingFragment();
			twitterSyncingFragment.setArguments(args);
			selectNonDrawerItem(twitterSyncingFragment, fragmentTag, getResources().getString(
					R.string.title_twitter), true);
		}
	}

	@Override
	public void onDrawerListFragmentViewCreated() {
		/*
		 * if (drawerItemSelectedPosition != AppConstants.INVALID_INDEX) {
		 * DrawerListFragment drawerListFragment = (DrawerListFragment)
		 * getSupportFragmentManager()
		 * .findFragmentByTag(DRAWER_LIST_FRAGMENT_TAG);
		 * drawerListFragment.getListView
		 * ().setItemChecked(drawerItemSelectedPosition, true); }
		 */
	}

	@Override
	public void onDrawerItemSelected(int pos) {
		// Log.d(TAG, "onDrawerItemSelected(), pos = " + pos);
		// process only if different selection is made, otherwise just close the
		// drawer.
		if (drawerItemSelectedPosition != pos) {
			getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			selectItem(pos);

		} else {
			if (!isTabletAndInLandscapeMode) {
				mDrawerLayout.closeDrawer(lnrLayoutRootNavDrawer);
			}
		}
	}

	@Override
	public void replaceGetStartedFragmentBy(String fragmentTag) {
		//Log.d(TAG, "replaceGetStartedFragmentBy(), tag = " + fragmentTag);
		if (fragmentTag.equals(AppConstants.FRAGMENT_TAG_MY_EVENTS)) {
			selectItem(INDEX_NAV_ITEM_MY_EVENTS);
			
		} else if (fragmentTag.equals(AppConstants.FRAGMENT_TAG_DISCOVER)) {
			selectItem(INDEX_NAV_ITEM_DISCOVER);

		} else if (fragmentTag.equals(AppConstants.FRAGMENT_TAG_CONNECT_ACCOUNTS)) {
			selectItem(INDEX_NAV_ITEM_CONNECT_ACCOUNTS);
		}
	}

	@Override
	public void onArtistSelected(Artist artist) {

		ArtistDetailsFragment artistDetailsFragment = new ArtistDetailsFragment();
		Bundle args = new Bundle();
		args.putSerializable(BundleKeys.ARTIST, artist);
		artistDetailsFragment.setArguments(args);
		selectNonDrawerItem(artistDetailsFragment,
				AppConstants.FRAGMENT_TAG_ARTIST_DETAILS, getResources()
						.getString(R.string.title_artist_details), true);
	}

	@Override
	public void onVenueSelected(Venue venue) {
		VenueDetailsFragment venueDetailsFragment = new VenueDetailsFragment();
		Bundle args = new Bundle();
		args.putSerializable(BundleKeys.VENUE, venue);
		venueDetailsFragment.setArguments(args);
		selectNonDrawerItem(venueDetailsFragment,
				AppConstants.FRAGMENT_TAG_VENUE_DETAILS, getResources()
						.getString(R.string.title_venue_details), true);
	}

	@Override
	public void onEventSelected(Event event) {

		EventDetailsFragment eventDetailsFragment = new EventDetailsFragment();
		Bundle args = new Bundle();
		args.putSerializable(BundleKeys.EVENT, event);
		eventDetailsFragment.setArguments(args);
		selectNonDrawerItem(eventDetailsFragment,
				AppConstants.FRAGMENT_TAG_EVENT_DETAILS, getResources()
						.getString(R.string.title_event_details), true);
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
				AppConstants.FRAGMENT_TAG_EVENT_DETAILS, getResources()
						.getString(R.string.title_event_details), addToBackStack);
	}

	@Override
	public void onMapClicked(Bundle args) {
		FullScreenAddressMapFragment fragment = new FullScreenAddressMapFragment();
		fragment.setArguments(args);
		selectNonDrawerItem(fragment,
				AppConstants.FRAGMENT_TAG_FULL_SCREEN_ADDRESS_MAP,
				args.getString(BundleKeys.VENUE_NAME), true);
	}

	@Override
	public void onServiceSelected(Service service, Bundle args, boolean addToBackStack) {
		//Log.d(TAG, "onServiceSelected()");
		switch (service) {
		
		case Facebook:
			LoginSyncingFragment loginSyncingFragment = new LoginSyncingFragment();
			loginSyncingFragment.setArguments(args);
			selectNonDrawerItem(loginSyncingFragment, AppConstants.FRAGMENT_TAG_LOGIN_SYNCING, getResources()
					.getString(R.string.title_facebook), addToBackStack);
			break;
			
		case GooglePlus:
			loginSyncingFragment = new LoginSyncingFragment();
			loginSyncingFragment.setArguments(args);
			selectNonDrawerItem(loginSyncingFragment, AppConstants.FRAGMENT_TAG_LOGIN_SYNCING, getResources()
					.getString(R.string.title_google_plus), addToBackStack);
			break;
			
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

		if (fragment instanceof DiscoverParentFragment) {
			onFragmentResumed(INDEX_NAV_ITEM_DISCOVER, getResources()
					.getString(R.string.title_discover),
					AppConstants.FRAGMENT_TAG_DISCOVER);

		} else if (fragment instanceof MyEventsFragment) {
			onFragmentResumed(INDEX_NAV_ITEM_MY_EVENTS, getResources()
					.getString(R.string.title_my_events),
					AppConstants.FRAGMENT_TAG_MY_EVENTS);

		} else if (fragment instanceof ArtistsNewsListFragment) {
			onFragmentResumed(INDEX_NAV_ITEM_ARTISTS_NEWS, getResources()
					.getString(R.string.title_artists_news),
					AppConstants.FRAGMENT_TAG_ARTISTS_NEWS_LIST);

		} else if (fragment instanceof FriendsActivityFragment) {
			onFragmentResumed(INDEX_NAV_ITEM_FRIENDS_ACTIVITY, getResources()
					.getString(R.string.title_friends_activity),
					AppConstants.FRAGMENT_TAG_FRIENDS_ACTIVITY);

		} else if (fragment instanceof FollowingParentFragment) {
			onFragmentResumed(INDEX_NAV_ITEM_FOLLOWING, getResources()
					.getString(R.string.title_following),
					AppConstants.FRAGMENT_TAG_FOLLOWING);

		} else if (fragment instanceof ConnectAccountsFragment) {
			onFragmentResumed(INDEX_NAV_ITEM_CONNECT_ACCOUNTS, getResources()
					.getString(R.string.title_connect_accounts),
					AppConstants.FRAGMENT_TAG_CONNECT_ACCOUNTS);

		} else if (fragment instanceof ChangeLocationFragment) {
			onFragmentResumed(INDEX_NAV_ITEM_CHANGE_LOCATION, getResources()
					.getString(R.string.title_change_location),
					AppConstants.FRAGMENT_TAG_CHANGE_LOCATION);

		} else if (fragment instanceof AboutUsFragment) {
			onFragmentResumed(INDEX_NAV_ITEM_ABOUT_US, getResources()
					.getString(R.string.title_about_us),
					AppConstants.FRAGMENT_TAG_ABOUT_US);

		} else if (fragment instanceof EULAFragment) {
			onFragmentResumed(INDEX_NAV_ITEM_EULA,
					getResources().getString(R.string.title_eula),
					AppConstants.FRAGMENT_TAG_EULA);

		} else if (fragment instanceof RepCodeFragment) {
			onFragmentResumed(INDEX_NAV_ITEM_REP_CODE, getResources()
					.getString(R.string.title_rep_code),
					AppConstants.FRAGMENT_TAG_REP_CODE);

		} else if (fragment instanceof FullScreenAddressMapFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, fragment.getArguments()
					.getString(BundleKeys.VENUE_NAME),
					AppConstants.FRAGMENT_TAG_FULL_SCREEN_ADDRESS_MAP);

		} else if (fragment instanceof SearchFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, getResources()
					.getString(R.string.title_search_results),
					AppConstants.FRAGMENT_TAG_SEARCH);
			// Log.d(TAG, "fragment = " + fragment + ", query = " +
			// ((SearchFragment) fragment).getSearchQuery());
			/**
			 * on some devices onCreateOptionsMenu is called after onFragmentResumed, 
			 * So the search item might be null at this point
			 */
			if(searchItem != null) {
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
			List<Category> categories = (List<Category>) args
					.getSerializable(BundleKeys.CATEGORIES);

			onFragmentResumed(AppConstants.INVALID_INDEX,
					categories.get(categoryPosition).getName(),
					AppConstants.FRAGMENT_TAG_DISCOVER_BY_CATEGORY);

		} else if (fragment instanceof EventDetailsFragment) {
			if (fragment.getArguments().containsKey(
					BundleKeys.IS_CALLED_FROM_OTHER_TASK)) {
				onFragmentCalledFromOtherTaskResumed(
						AppConstants.INVALID_INDEX,
						getResources().getString(R.string.title_event_details),
						AppConstants.FRAGMENT_TAG_EVENT_DETAILS);

			} else {
				onFragmentResumed(AppConstants.INVALID_INDEX, getResources()
						.getString(R.string.title_event_details),
						AppConstants.FRAGMENT_TAG_EVENT_DETAILS);
			}

		} else if (fragment instanceof ArtistDetailsFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, getResources()
					.getString(R.string.title_artist_details),
					AppConstants.FRAGMENT_TAG_ARTIST_DETAILS);

		} else if (fragment instanceof VenueDetailsFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, getResources()
					.getString(R.string.title_venue_details),
					AppConstants.FRAGMENT_TAG_VENUE_DETAILS);

		} else if (fragment instanceof DeviceLibraryFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, getResources()
					.getString(R.string.title_device_library),
					AppConstants.FRAGMENT_TAG_DEVICE_LIBRARY);

		} else if (fragment instanceof LoginSyncingFragment) {
			String title = (fragment.getArguments().getSerializable(BundleKeys.LOGIN_TYPE) == LoginType.facebook) ? 
					getResources().getString(R.string.title_facebook) : getResources().getString(R.string.title_google_plus);
			onFragmentResumed(AppConstants.INVALID_INDEX, title, AppConstants.FRAGMENT_TAG_LOGIN_SYNCING);

		} else if (fragment instanceof TwitterFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, getResources()
					.getString(R.string.title_twitter),
					AppConstants.FRAGMENT_TAG_TWITTER);

		} else if (fragment instanceof RdioFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, getResources()
					.getString(R.string.title_rdio),
					AppConstants.FRAGMENT_TAG_RDIO);

		} else if (fragment instanceof LastfmFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, getResources()
					.getString(R.string.title_lastfm),
					AppConstants.FRAGMENT_TAG_LASTFM);

		} else if (fragment instanceof PandoraFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, getResources()
					.getString(R.string.title_pandora),
					AppConstants.FRAGMENT_TAG_PANDORA);
			
		} else if (fragment instanceof TwitterSyncingFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, getResources().getString(R.string.title_twitter),
					AppConstants.FRAGMENT_TAG_TWITTER_SYNCING);

		} else if (fragment instanceof GooglePlayMusicFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, getResources().getString(R.string.title_google_play),
					AppConstants.FRAGMENT_TAG_GOOGLE_PLAY_MUSIC);
		}
	}

	public void hideSoftKeypad() {
		Log.d(TAG, "hideSoftKeypad()");
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(searchView.getApplicationWindowToken(), 0);
	}

	@Override
	public boolean onQueryTextChange(String arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		Log.d(TAG, "onQueryTextSubmit(), query = " + query);
		if (query == null || query.length() == 0) {
			return true;
		}

		searchQuery = query;
		hideSoftKeypad();

		SearchFragment searchFragment;
		if (!currentContentFragmentTag.equals(AppConstants.FRAGMENT_TAG_SEARCH)) {
			searchFragment = new SearchFragment();
			Bundle args = new Bundle();
			args.putString(BundleKeys.QUERY, query);
			searchFragment.setArguments(args);
			selectNonDrawerItem(searchFragment,
					AppConstants.FRAGMENT_TAG_SEARCH,
					getResources().getString(R.string.title_search_results),
					true);

		} else {
			searchFragment = (SearchFragment) getSupportFragmentManager()
					.findFragmentByTag(AppConstants.FRAGMENT_TAG_SEARCH);
			searchFragment.onQueryTextSubmit(searchQuery);
		}

		return true;
	}

	@Override
	public void onLocationChanged() {
		onDrawerItemSelected(INDEX_NAV_ITEM_DISCOVER);
	}

	private void setDrawerIndicatorEnabled(boolean enable) {
		isDrawerIndicatorEnabled = enable;
		if(mDrawerToggle != null) {
			mDrawerToggle.setDrawerIndicatorEnabled(enable);
		}
	}

	@Override
	public void onFragmentResumed(Fragment fragment, int drawerPosition, String actionBarTitle) {
		//Added right now just for Bosch Main Activity
	}
	
	@Override
	public void onBackPressed() {
		/**
		 * this added as after the Syncing screen when the onbackpressed occurs, on Connect account screen back arrow
		 * is retained in tablet landscape mode. So, to resolve the issue below statements are added.
		 */
		if (isTabletAndInLandscapeMode && currentContentFragmentTag.equals(AppConstants.FRAGMENT_TAG_LOGIN_SYNCING)) {
			getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_HOME);
		}
		super.onBackPressed();
	}
	
}
