package com.wcities.eventseeker.bosch;

import java.io.IOException;
import java.util.List;

import android.app.ActivityManager;
import android.content.Intent;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bosch.myspin.serversdk.MySpinServerSDK;
import com.wcities.eventseeker.ChangeLocationFragment;
import com.wcities.eventseeker.DiscoverByCategoryFragment;
import com.wcities.eventseeker.MainActivity;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.WebViewFragment;
import com.wcities.eventseeker.bosch.BoschDrawerListFragment.BoschDrawerListFragmentListener;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Category;
import com.wcities.eventseeker.interfaces.FragmentLoadedFromBackstackListener;
import com.wcities.eventseeker.interfaces.ReplaceFragmentListener;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.GeoUtil;
import com.wcities.eventseeker.util.GeoUtil.GeoUtilListener;

public class BoschMainActivity extends ActionBarActivity implements BoschDrawerListFragmentListener, 
		FragmentLoadedFromBackstackListener, ReplaceFragmentListener, GeoUtilListener {

	private static final String TAG = BoschMainActivity.class.getSimpleName();
	
	private static final int INDEX_NAV_ITEM_HOME = 0;

	private LinearLayout lnrLayoutRootNavDrawer;
	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle mDrawerToggle;

	private String currentContentFragmentTag, mTitle, currentCityName, currentCategoryName;
	private int drawerItemSelectedPosition = AppConstants.INVALID_INDEX;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bosch_main);
		
		if (!MySpinServerSDK.sharedInstance().isConnected()) {
			//Log.d(TAG, "bosch not Connected");
			//Toast.makeText(this, "bosch not Connected", Toast.LENGTH_LONG).show();
			finish();
			 
		} /*else {
			Toast.makeText(this, "bosch is Connected", Toast.LENGTH_LONG).show();
		}*/
		
		lnrLayoutRootNavDrawer = (LinearLayout) findViewById(R.id.rootNavigationDrawer);
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerToggle = new ActionBarDrawerToggle(this, // host Activity
				mDrawerLayout, // DrawerLayout object
				R.drawable.sidenav, // nav drawer icon to replace 'Up' caret
				R.string.drawer_open, // "open drawer" description
				R.string.drawer_close // "close drawer" description
		);
		
		mDrawerLayout.setDrawerListener(mDrawerToggle);
		
		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_HOME 
				| ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_CUSTOM);
		/**
		 * setIcon null throws NullPointerException while expanding
		 * searchView in SearchFragment. So need to set any transparent icon
		 * rather than null.
		 */
		getSupportActionBar().setIcon(R.drawable.placeholder);
		getSupportActionBar().setCustomView(R.layout.bosch_actionbar_titleview);

		BoschDrawerListFragment boschDrawerListFragment = (BoschDrawerListFragment) getSupportFragmentManager()
				.findFragmentByTag(BoschDrawerListFragment.class.getSimpleName());
		if (boschDrawerListFragment == null) {
			addDrawerListFragment();
		}
		getSupportFragmentManager().executePendingTransactions();
		
		selectItem(INDEX_NAV_ITEM_HOME);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.bosch_main, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle your other action bar items...
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
			break;
		}

		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
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
	
	private void setDrawerIndicatorEnabled(boolean enable) {
		if (mDrawerToggle != null) {
			mDrawerToggle.setDrawerIndicatorEnabled(enable);
		}
	}
	
	private void addDrawerListFragment() {
		FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
		BoschDrawerListFragment boschDrawerListFragment = new BoschDrawerListFragment();
		fragmentTransaction.add(R.id.rootNavigationDrawer, boschDrawerListFragment, 
				BoschDrawerListFragment.class.getSimpleName());
		fragmentTransaction.commit();
	}
	
	/** Swaps fragments in the main content view */
	private void selectItem(int position) {
		drawerItemSelectedPosition = position;
		setDrawerIndicatorEnabled(true);
			
		BoschDrawerListFragment boschDrawerListFragment = (BoschDrawerListFragment) getSupportFragmentManager()
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
	
	private void selectNonDrawerItem(Fragment replaceBy, String newTitle, boolean addToBackStack) {
		drawerItemSelectedPosition = AppConstants.INVALID_INDEX;
		setDrawerIndicatorEnabled(!addToBackStack);
		replaceContentFrameByFragment(replaceBy, newTitle, addToBackStack);
	}
	
	private void replaceContentFrameByFragment(Fragment replaceBy, String newTitle, boolean addToBackStack) {
		updateTitle(newTitle);

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
		supportInvalidateOptionsMenu();
	}
	
	private void onFragmentResumed(int position, String title, String fragmentTag) {
		drawerItemSelectedPosition = position;
		if (drawerItemSelectedPosition != AppConstants.INVALID_INDEX) {
			setDrawerIndicatorEnabled(true);
		}
		updateTitle(title);

		currentContentFragmentTag = fragmentTag;
	}
	
	private void updateTitle(String newTitle) {
		mTitle = newTitle;
		((TextView)findViewById(R.id.txtActionBarTitle)).setText(mTitle);
	}
	
	private String getCityName() {
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
		// process only if different selection is made, otherwise just close the drawer.
		if (drawerItemSelectedPosition != pos) {
			getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			selectItem(pos);

		} else {
			mDrawerLayout.closeDrawer(lnrLayoutRootNavDrawer);
		}
	}

	@Override
	public void onFragmentResumed(Fragment fragment) {
		if (fragment instanceof PlanTravelFragment) {
			onFragmentResumed(INDEX_NAV_ITEM_HOME, getResources().getString(R.string.title_plan_travel),
					fragment.getClass().getSimpleName());
			
		} else if (fragment instanceof BoschDiscoverFragment) {
			onFragmentResumed(AppConstants.INVALID_INDEX, currentCityName, fragment.getClass().getSimpleName());
		}
	}

	@Override
	public void replaceByFragment(String fragmentTag, Bundle args) {
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
		}
	}

	@Override
	public void onAddressSearchCompleted(String strAddress) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onLatlngSearchCompleted(Address address) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onCitySearchCompleted(String city) {
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
}
