package com.wcities.eventseeker;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import android.animation.ObjectAnimator;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.util.Pair;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wcities.eventseeker.DrawerListFragmentTab.DrawerListFragmentTabListener;
import com.wcities.eventseeker.SettingsFragmentTab.OnSettingsItemClickedListener;
import com.wcities.eventseeker.analytics.GoogleAnalyticsTracker;
import com.wcities.eventseeker.analytics.IGoogleAnalyticsTracker;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.constants.Enums.SettingsItem;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.interfaces.ActivityDestroyedListener;
import com.wcities.eventseeker.interfaces.ArtistListenerTab;
import com.wcities.eventseeker.interfaces.EventListenerTab;
import com.wcities.eventseeker.interfaces.OnLocaleChangedListener;
import com.wcities.eventseeker.interfaces.VenueListenerTab;
import com.wcities.eventseeker.util.FbUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.GPlusUtil;

/**
 * Using ActionBarActivity (extended by BaseActivity) instead of Activity so as to use support library toolbar as actionbar even for lower apis
 * by calling setSupportActionBar(toolbar) & also there is common code to both mobile & tablet which can be kept in BaseActivity
 */
public abstract class BaseActivityTab extends BaseActivity implements IGoogleAnalyticsTracker, ActivityDestroyedListener, 
		DrawerListFragmentTabListener, OnLocaleChangedListener, OnSettingsItemClickedListener, OnQueryTextListener, 
		EventListenerTab, VenueListenerTab, ArtistListenerTab {
	
	private static final String TAG = BaseActivityTab.class.getSimpleName(); 
	
	private static final int SEARCHVIEW_MAX_WIDTH = 5000; 
	
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
	private TextView txtToolbarTitle, txtToolbarSubTitle;

	private List<Button> tabBarBtn;
	
	protected String currentContentFragmentTag;
	private String searchQuery = "";
	
	protected boolean isOnCreateCalledFirstTime = true, isSearchViewIconified = true;
	
	private MenuItem searchItem;
	private SearchView searchView;
	
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
			isSearchViewIconified = savedInstanceState.getBoolean(BundleKeys.IS_SEARCHVIEW_ICONIFIED);
			searchQuery = savedInstanceState.getString(BundleKeys.SEARCH_QUERY);
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

		updateTitle(getScrnTitle());
		
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
	
	/**
	 * To disable search action item 
	 * 1) override onCreateOptionsMenu() from child activity 
	 * 2) return true.  (returning false will not call onOptionsItemSelected() of extending activity. e.g.
	 * in case of LoginActivityTab & SignUpActivityTab, we have overridden onOptionsItemSelected() which
	 * won't get called up if we return false from their onCreateOptionsMenu().
	 * 3) Don't call super.onCreateOptionsMenu().
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_base_tab, menu);
		setSearchView(menu);
		return super.onCreateOptionsMenu(menu);
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
		if (searchView != null) {
			outState.putBoolean(BundleKeys.IS_SEARCHVIEW_ICONIFIED, searchView.isIconified());
		}
		outState.putString(BundleKeys.SEARCH_QUERY, searchQuery);
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
	
	public void updateTitle(String title) {
		//Log.d(TAG, "updateTitle()");
		if (txtToolbarTitle != null) {
			// for double line toolbar used on some floating windows like event details
			txtToolbarTitle.setText(title);
			
		} else {
			getSupportActionBar().setTitle(title);
		}
	}
	
	public void updateSubTitle(String subTitle) {
		//Log.d(TAG, "updateSubTitle()");
		if (txtToolbarSubTitle != null) {
			// for double line toolbar used on some floating windows like event details
			txtToolbarSubTitle.setText(subTitle);
			
		} else {
			getSupportActionBar().setSubtitle(subTitle);
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
			
		case INDEX_NAV_ITEM_MY_EVENTS:
			intent = new Intent(getApplicationContext(), MyEventsActivityTab.class);
			break;

		case INDEX_NAV_ITEM_FOLLOWING:
			intent = new Intent(getApplicationContext(), FollowingActivityTab.class);
			break;
			
		case INDEX_NAV_ITEM_FRIENDS_ACTIVITY:
			intent = new Intent(getApplicationContext(), FriendsActivityActivityTab.class);
			break;
			
		case INDEX_NAV_ITEM_ARTISTS_NEWS:
			intent = new Intent(getApplicationContext(), ArtistsNewsActivityTab.class);
			break;

		case INDEX_NAV_ITEM_SETTINGS:
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
	
	protected void setupFloatingWindow() {
        // configure this Activity as a floating window, dimming the background
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = getResources().getDimensionPixelSize(R.dimen.floating_window_w);
        params.height = getResources().getDimensionPixelSize(R.dimen.floating_window_ht);
        params.dimAmount = 0.5f;
        params.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        getWindow().setAttributes(params);
    }
	
	private void setSearchView(Menu menu) {
		//Log.d(TAG, "setSearchView()");
		searchItem = menu.findItem(R.id.action_search);
		searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
		searchView.setQueryHint(getResources().getString(R.string.menu_search));
		searchView.setOnQueryTextListener(this);
		
		ImageView v = (ImageView) searchView.findViewById(R.id.search_button);
		// null check is for safety purpose
		if (v != null) {
			v.setImageResource(R.drawable.search);
		}
		
		if (this instanceof SearchActivityTab || this instanceof MyEventsActivityTab) {
			/**
			 * For screens having toolbar tabs, fill entire toolbar when searchview is expanded, because otherwise 
			 * it just compresses tabs in portrait orientation which doesn't look good.
			 */
			searchView.setMaxWidth(SEARCHVIEW_MAX_WIDTH);
		}
		
		/**
		 * retain searchQuery on orientation change.
		 * For all screens except search we need to retain only when isSearchViewIconified = false, 
		 * but for SearchActivityTab, even if isSearchViewIconified = true, we want searchQuery be retained.
		 */
		searchView.setQuery(searchQuery, false);
		if (!isSearchViewIconified) {
			//Log.d(TAG, "!isSearchViewIconified");
			// retain searchview & softkeypad's open status on orientation change
			searchView.setIconified(false);
		}
	}
	
	protected void setCommonUI() {
		//Log.d(TAG, "setCommonUI()");
		toolbar = (Toolbar) findViewById(R.id.toolbarForActionbar);
	    setSupportActionBar(toolbar);
	    
	    txtToolbarTitle = (TextView) toolbar.findViewById(R.id.txtToolbarTitle);
	    txtToolbarSubTitle = (TextView) toolbar.findViewById(R.id.txtToolbarSubTitle);
	    
	    initTabBar();
	    
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
	
	private void initTabBar() {
		LinearLayout tabBar = (LinearLayout) toolbar.findViewById(R.id.tabBar);
		if (tabBar != null) {
			tabBarBtn = new ArrayList<Button>();
			tabBarBtn.add((Button) tabBar.findViewById(R.id.btnTab1));
			tabBarBtn.add((Button) tabBar.findViewById(R.id.btnTab2));
			tabBarBtn.add((Button) tabBar.findViewById(R.id.btnTab3));
		}
	}

	public List<Button> getTabBarButtons() {
		return tabBarBtn;
	}
	
	/**
	 * Activities corresponding to navigation drawer items should override this method to return valid position.
	 * @return
	 */
	protected int getDrawerItemPos() {
		return AppConstants.INVALID_INDEX;
	}
	
	protected void allowContentBehindToolbar() {
		findViewById(R.id.content_frame).setPadding(0, 0, 0, 0);
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
	
	protected void animateToolbarElevation(float start, float end) {
		ObjectAnimator elevateAnim = ObjectAnimator.ofFloat(toolbar, "elevation", start, end);
		elevateAnim.setDuration(100);
		elevateAnim.start();
	}
	
	protected void setToolbarBg(int color) {
		//Log.d(TAG, "setToolbarBg(), color = " + color);
		toolbar.setBackgroundColor(color);
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
			drawerListFragmentTab.updateCheckedDrawerItem(getDrawerItemPos());
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
		 * refresh the current screen's title only if it is Language Activity.
		 */
		if (currentContentFragmentTag.equals(FragmentUtil.getTag(LanguageFragmentTab.class))) {
			String title = getResources().getString(R.string.title_language);
			updateTitle(title);
		}
		/**
		 * 	refresh the SearchView	
		 */
		//searchView.setQueryHint(getResources().getString(R.string.menu_search));
	}
	
	@Override
	public void onEventSelected(Event event, ImageView imageView, TextView textView) {
		Intent intent = new Intent(getApplication(), EventDetailsActivityTab.class);
		intent.putExtra(BundleKeys.EVENT, event);
		intent.putExtra(BundleKeys.TRANSITION_NAME_SHARED_IMAGE, ViewCompat.getTransitionName(imageView));
		intent.putExtra(BundleKeys.TRANSITION_NAME_SHARED_TEXT, ViewCompat.getTransitionName(textView));
		
		/**
		 * ActivityOptionsCompat is used since it's helper for accessing features in ActivityOptions 
		 * introduced in API level 16 in a backwards compatible fashion.
		 */
		ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, 
				Pair.create((View)imageView, ViewCompat.getTransitionName(imageView)),
				Pair.create((View)textView, ViewCompat.getTransitionName(textView)));
        ActivityCompat.startActivity(this, intent, options.toBundle());
	}
	
	@Override
	public void onVenueSelected(Venue venue, ImageView imageView, TextView textView) {
		Intent intent = new Intent(getApplication(), VenueDetailsActivityTab.class);
		intent.putExtra(BundleKeys.VENUE, venue);
		Pair<View, String> pairImg = null, pairTxt = null;
		
		if (imageView != null) {
			intent.putExtra(BundleKeys.TRANSITION_NAME_SHARED_IMAGE, ViewCompat.getTransitionName(imageView));
			pairImg = Pair.create((View)imageView, ViewCompat.getTransitionName(imageView));
		}
		if (textView != null) {
			intent.putExtra(BundleKeys.TRANSITION_NAME_SHARED_TEXT, ViewCompat.getTransitionName(textView));
			pairTxt = Pair.create((View)textView, ViewCompat.getTransitionName(textView));
		}
		
		/**
		 * ActivityOptionsCompat is used since it's helper for accessing features in ActivityOptions 
		 * introduced in API level 16 in a backwards compatible fashion.
		 */
		ActivityOptionsCompat options = null;
		if (pairImg != null && pairTxt != null) {
			options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, pairImg, pairTxt);
			
		} else if (pairImg != null) {
			options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, pairImg);
			
		} else if (pairTxt != null) {
			options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, pairTxt);
		} 
		
		if (options != null) {
			ActivityCompat.startActivity(this, intent, options.toBundle());
			
		} else {
			startActivity(intent);
		}
	}
	
	@Override
	public void onArtistSelected(Artist artist, ImageView imageView, TextView textView) {
		Intent intent = new Intent(getApplication(), ArtistDetailsActivityTab.class);
		intent.putExtra(BundleKeys.ARTIST, artist);
		intent.putExtra(BundleKeys.TRANSITION_NAME_SHARED_IMAGE, ViewCompat.getTransitionName(imageView));
		intent.putExtra(BundleKeys.TRANSITION_NAME_SHARED_TEXT, ViewCompat.getTransitionName(textView));
		
		/**
		 * ActivityOptionsCompat is used since it's helper for accessing features in ActivityOptions 
		 * introduced in API level 16 in a backwards compatible fashion.
		 */
		ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, 
				Pair.create((View)imageView, ViewCompat.getTransitionName(imageView)),
				Pair.create((View)textView, ViewCompat.getTransitionName(textView)));
        ActivityCompat.startActivity(this, intent, options.toBundle());
	}
	
	protected void onMapClicked(Bundle args) {
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
		}
	}
	
	protected boolean isDrawerOpen() {
		return mDrawerLayout.isDrawerOpen(lnrLayoutRootNavDrawer);
	}
	
	protected abstract String getScrnTitle();
	
	@Override
	public void onSettingsItemClicked(SettingsItem settingsItem, Bundle args) {
		Intent intent;
		switch (settingsItem) {
		
			case SYNC_ACCOUNTS:
		    	intent = new Intent(getApplicationContext(), ConnectAccountsActivityTab.class);
		    	startActivity(intent);
		    	break;
		
			case CHANGE_LOCATION:
				intent = new Intent(getApplicationContext(), ChangeLocationActivityTab.class);
				if (args != null) {
					intent.putExtras(args);
		    	}
				startActivity(intent);
				break;

			case LANGUAGE:
				intent = new Intent(getApplicationContext(), LanguageActivityTab.class);
				if (args != null) {
					intent.putExtras(args);
		    	}
				startActivity(intent);
				break;

			case INVITE_FRIENDS:
				inviteFriends();
				break;
				
			case RATE_APP:
				rateApp();
				break;
				
			case ABOUT:
				intent = new Intent(getApplicationContext(), AboutUsActivityTab.class);
				startActivity(intent);
				break;
				
			case EULA:
				intent = new Intent(getApplicationContext(), EULAActivityTab.class);
				startActivity(intent);
				break;
				
			case REPCODE:
				intent = new Intent(getApplicationContext(), RepcodeActivityTab.class);
				startActivity(intent);
				break;

			default:
				break;
		}
	}
	
	@Override
	public boolean onQueryTextChange(String query) {
		//Log.d(TAG, "onQueryTextChange() - query = " + query);
		searchQuery = query;
		return false;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		if (query == null || query.length() == 0) {
			return true;
		}
		
		if (!(this instanceof SearchActivityTab)) {
			/**
			 * Clear searchView content & collapse searchView so that soft keypad doesn't pop-up on coming
			 * back to this screen from search & on user trying to search again on this screen he doesn't see 
			 * the old searched query again
			 */
			searchQuery = "";
			searchView.setQuery(searchQuery, false);
			searchView.setIconified(true);
			
			Intent intent = new Intent(getApplicationContext(), SearchActivityTab.class);
			intent.putExtra(BundleKeys.QUERY, query);
			startActivity(intent);
			
		} else {
			//Log.d(TAG, "else");
			// Calling setIconified() only once doesn't collapse the searchview, hence calling it twice.
			searchView.setIconified(true);
			searchView.setIconified(true);
			searchQuery = query;
			searchView.setQuery(searchQuery, false);
			((SearchActivityTab) this).onQueryTextUpdated(query);
		}
		
		return true;
	}
	
	public void expandSearchView() {
		searchView.setIconified(false);
	}
}
