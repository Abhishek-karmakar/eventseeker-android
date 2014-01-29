package com.wcities.eventseeker.bosch;

import java.io.IOException;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bosch.myspin.serversdk.MySpinException;
import com.bosch.myspin.serversdk.MySpinServerSDK;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.bosch.BoschDrawerListFragment.BoschDrawerListFragmentListener;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Category;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.interfaces.ArtistListener;
import com.wcities.eventseeker.interfaces.EventListener;
import com.wcities.eventseeker.interfaces.FragmentLoadedFromBackstackListener;
import com.wcities.eventseeker.interfaces.ReplaceFragmentListener;
import com.wcities.eventseeker.interfaces.VenueListener;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.GeoUtil;
import com.wcities.eventseeker.util.GeoUtil.GeoUtilListener;

public class BoschMainActivity extends ActionBarActivity implements GeoUtilListener, ReplaceFragmentListener, 
	EventListener, ArtistListener, VenueListener, FragmentLoadedFromBackstackListener, 
	BoschDrawerListFragmentListener  {

	private static final String TAG = BoschMainActivity.class.getName();

	private static final int INDEX_NAV_ITEM_HOME = 0;

	private LinearLayout lnrLayoutRootNavDrawer;
	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle mDrawerToggle;

	private String currentContentFragmentTag, mTitle, currentCityName, currentCategoryName;
	private int drawerItemSelectedPosition = AppConstants.INVALID_INDEX;

	private TextView txtActionBarTitle;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (!MySpinServerSDK.sharedInstance().isConnected()) {
			finish();
		} else {

			setRequestedOrientation(getResources().getConfiguration().orientation);

			setContentView(R.layout.activity_bosch_main);

			lnrLayoutRootNavDrawer = (LinearLayout) findViewById(R.id.rootNavigationDrawerBosch);
			mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
			mDrawerToggle = new ActionBarDrawerToggle(this, // host this
					mDrawerLayout, // DrawerLayout object
					R.drawable.sidenav, // nav drawer icon to replace 'Up' caret
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

			txtActionBarTitle = (TextView) findViewById(R.id.txtActionBarTitle);

			BoschDrawerListFragment boschDrawerListFragment = (BoschDrawerListFragment) getSupportFragmentManager()
				.findFragmentByTag(BoschDrawerListFragment.class.getSimpleName());
			
			if (boschDrawerListFragment == null) {
				addDrawerListFragment();
			}
			
			getSupportFragmentManager().executePendingTransactions();

			// Log.d(TAG, "initial currentContentFragmentTag : " + currentContentFragmentTag);

			selectItem(INDEX_NAV_ITEM_HOME);

		}		
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Log.d(TAG, "onPostCreate");
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}	
	
	@Override
	protected void onDestroy() {
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
		super.onDestroy();
	}
	
	/** Swaps fragments in the main content view */
	private void selectItem(int position) {
		// Log.d(TAG, "selectItem");
		drawerItemSelectedPosition = position;
		setDrawerIndicatorEnabled(true);
			
		BoschDrawerListFragment boschDrawerListFragment = 
				(BoschDrawerListFragment) getSupportFragmentManager()
				.findFragmentByTag(BoschDrawerListFragment.class.getSimpleName());
		if (boschDrawerListFragment == null) {
			return;
		}
		
	    switch (position) {
	    
		case INDEX_NAV_ITEM_HOME:
			PlanTravelFragment planTravelFragment = new PlanTravelFragment();
			replaceContentFrameByFragment(planTravelFragment, getResources().getString(
					R.string.title_plan_travel), false);
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
	 * @param title
	 */
	public void updateBoschActionBarTitle(String title) {
		updateTitle(title);
	}

	public void replaceContentFrameByFragment(Fragment replaceBy, String newTitle, boolean addToBackStack) {
		
		// Log.d(TAG, "replaceContentFrameByFragment");
		
		updateTitle(newTitle);
		
		FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
		
		String fragmentTag = replaceBy.getClass().getSimpleName();
		fragmentTransaction.replace(R.id.content_frame, replaceBy, fragmentTag);
		
		if (addToBackStack) {
			fragmentTransaction.addToBackStack(null);
		}
		fragmentTransaction.commitAllowingStateLoss();

		currentContentFragmentTag = fragmentTag;

		// Log.d(TAG, "got the current tag as : " + fragmentTag);
		
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
	
	public void updateTitle(String newTitle) {
		// Log.d(TAG, "updateTitle");
		if (newTitle != null) {
			mTitle = newTitle;
			txtActionBarTitle.setText(mTitle);
		}
		// Log.d(TAG, "updateTitle over");
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
		// Log.d(TAG, "onOptionsItemSelected");
		switch (item.getItemId()) {

		case android.R.id.home:
			if (mDrawerToggle.isDrawerIndicatorEnabled()) {
				if (mDrawerLayout.isDrawerOpen(lnrLayoutRootNavDrawer)) {
					mDrawerLayout.closeDrawer(lnrLayoutRootNavDrawer);

				} else {
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
		// Log.d(TAG, "onKeyDown");
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
	}
    
	private void selectNonDrawerItem(Fragment replaceBy, String newTitle, boolean addToBackStack) {
		// Log.d(TAG, "selectNonDrawerItem");
		drawerItemSelectedPosition = AppConstants.INVALID_INDEX;
		setDrawerIndicatorEnabled(!addToBackStack);
		replaceContentFrameByFragment(replaceBy, newTitle, addToBackStack);
	}
	
	private void onFragmentResumed(int position, String title, String fragmentTag) {
		// Log.d(TAG, "onFragmentResumed(int position, String title, String fragmentTag)");
		drawerItemSelectedPosition = position;
		if (drawerItemSelectedPosition != AppConstants.INVALID_INDEX) {
			setDrawerIndicatorEnabled(true);
		}
		updateTitle(title);

		currentContentFragmentTag = fragmentTag;
		
		// Log.d(TAG, "got the current tag as : " + fragmentTag);
	}
	
	private String getCityName() {
		// Log.d(TAG, "getCityName");
		String cityName = "";
		double[] latLng = DeviceUtil.getLatLon(getApplicationContext());
		List<Address> addresses = null;
        Geocoder geocoder = new Geocoder(this);
		try {
			addresses = geocoder.getFromLocation(latLng[0], latLng[1], 1);
			
			if (addresses != null && !addresses.isEmpty()) {
				Address address = addresses.get(0);
				//Log.i(TAG, "address=" + address);
				cityName = address.getLocality();
				
			} else {
        		Log.w(TAG, "No relevant address found.");
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Alternative way to find lat-lon
		if (addresses == null || addresses.isEmpty()) {
			GeoUtil.getCityFromLocation(latLng[0], latLng[1], this);
		}
		
		return cityName;
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

	public void onFragmentResumed(Fragment fragment) {
		
		// Log.d(TAG, "onFragmentResumed(Fragment fragment) : " + fragment.getClass().getSimpleName());
		
		if (fragment instanceof PlanTravelFragment) {
		
			onFragmentResumed(INDEX_NAV_ITEM_HOME, getResources().getString(R.string.title_plan_travel),
					fragment.getClass().getSimpleName());
			
		} else if (fragment instanceof BoschDiscoverFragment) {
			
			onFragmentResumed(AppConstants.INVALID_INDEX, currentCityName, fragment.getClass().getSimpleName());
		
		} else if (fragment instanceof BoschDiscoverByCategoryFragment) {
		
			onFragmentResumed(AppConstants.INVALID_INDEX, BoschDiscoverByCategoryFragment.prepareTitle(
				currentCityName, currentCategoryName), fragment.getClass().getSimpleName());

		} else if (fragment instanceof BoschEventArtistsFragment) {
			
			onFragmentResumed(AppConstants.INVALID_INDEX, 
				getResources().getString(R.string.title_artists), fragment.getClass().getSimpleName());

		} else if (fragment instanceof BoschFeaturedEventsFragment) {
			
			onFragmentResumed(AppConstants.INVALID_INDEX, 
				getResources().getString(R.string.title_featured), fragment.getClass().getSimpleName());
		
		}
		
		// Log.d(TAG, "fragment has been set on the screen");
	}

	@Override
	public void replaceByFragment(String fragmentTag, Bundle args) {
		
		// Log.d(TAG, "replaceByFragment");
		
		if (fragmentTag.equals(BoschDiscoverFragment.class.getSimpleName())) {
		
			currentCityName = getCityName();
			
			BoschDiscoverFragment boschDiscoverFragment = new BoschDiscoverFragment();
			selectNonDrawerItem(boschDiscoverFragment, currentCityName, true);
			
		} else if (fragmentTag.equals(BoschDiscoverByCategoryFragment.class.getSimpleName())) {
			
			if (currentCityName == null) {
				currentCityName = getCityName();
			}
			currentCategoryName = ((Category)args.getSerializable(BundleKeys.CATEGORY)).getName();
			
			BoschDiscoverByCategoryFragment boschDiscoverByCategoryFragment = new BoschDiscoverByCategoryFragment();
			boschDiscoverByCategoryFragment.setArguments(args);
			selectNonDrawerItem(boschDiscoverByCategoryFragment, BoschDiscoverByCategoryFragment.prepareTitle(
					currentCityName, currentCategoryName), true);
		
		} if (fragmentTag.equals(BoschInfoFragment.class.getSimpleName())) {

			BoschInfoFragment boschInfoFragment = new BoschInfoFragment();
			boschInfoFragment.setArguments(args);
			selectNonDrawerItem(boschInfoFragment, null, true);
			
		} if (fragmentTag.equals(BoschEventArtistsFragment.class.getSimpleName())) {
			
			BoschEventArtistsFragment boschEventArtistsFragment = new BoschEventArtistsFragment();
			boschEventArtistsFragment.setArguments(args);
			selectNonDrawerItem(boschEventArtistsFragment, getResources().getString(R.string.title_artists), true);

		} if (fragmentTag.equals(BoschArtistEventsFragment.class.getSimpleName())) {
			
			BoschArtistEventsFragment boschArtistEventsFragment = new BoschArtistEventsFragment();
			boschArtistEventsFragment.setArguments(args);
			selectNonDrawerItem(boschArtistEventsFragment, getResources().getString(R.string.title_artists), true);
			
		} if (fragmentTag.equals(BoschFeaturedEventsFragment.class.getSimpleName())) {
			
			BoschFeaturedEventsFragment boschFeaturedEventsFragment = new BoschFeaturedEventsFragment();
			boschFeaturedEventsFragment.setArguments(args);
			selectNonDrawerItem(boschFeaturedEventsFragment, getResources().getString(R.string.title_featured), 
				true);
			
		} 
	}

	@Override
	public void onAddressSearchCompleted(String strAddress) {}

	@Override
	public void onLatlngSearchCompleted(Address address) {}

	@Override
	public void onCitySearchCompleted(String city) {
		// Log.d(TAG, "onCitySearchCompleted");
		if (BoschDiscoverFragment.class.getSimpleName().equals(currentContentFragmentTag) && 
				city != null && city.length() != 0) {
			currentCityName = city;
			updateTitle(city);
			
		} else if (BoschDiscoverByCategoryFragment.class.getSimpleName().equals(currentContentFragmentTag) && 
				city != null && city.length() != 0) {
			currentCityName = city;
			updateTitle(BoschDiscoverByCategoryFragment.prepareTitle(currentCityName, currentCategoryName));
		}
	}

	@Override
	public void onEventSelected(Event event) {
		
		Bundle args = new Bundle();
		args.putSerializable(BundleKeys.EVENT, event);
		
		BoschEventDetailsFragment boscheventDetailsFragment = new BoschEventDetailsFragment();
		boscheventDetailsFragment.setArguments(args);
		
		selectNonDrawerItem(boscheventDetailsFragment, getResources().getString(R.string.title_event_details),
			true);
			
	}

	public void showBoschDialog(String msg) {
		
		View view = LayoutInflater.from(this).inflate(R.layout.bosch_element_alert_dialog, null);
		
		((TextView)view.findViewById(R.id.txtTitle)).setText(msg);

		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
		Dialog dialog = alertDialog
				.setCustomTitle(view)
				.setCancelable(false)
				.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.dismiss();
			}
		}).create();

		try {
			MySpinServerSDK.sharedInstance().registerDialog(dialog);
		} catch (MySpinException e) {
			Log.e(TAG, "Error : " + e.toString());
			e.printStackTrace();
		}	
		
		dialog.show();
		
	}

	@Override
	public void onArtistSelected(Artist artist) {

		Bundle args = new Bundle();
		args.putSerializable(BundleKeys.ARTIST, artist);

		BoschArtistDetailsFragment boschArtistDetailsFragment = new BoschArtistDetailsFragment();
		boschArtistDetailsFragment.setArguments(args);
		
		selectNonDrawerItem(boschArtistDetailsFragment, getResources().getString(R.string.title_artist_details), 
			true);
	}

	@Override
	public void onVenueSelected(Venue venue) {
		
	}

}
