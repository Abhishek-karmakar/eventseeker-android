package com.wcities.eventseeker.bosch;

import java.util.Iterator;
import java.util.List;

import android.location.Address;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.adapter.SwipeTabsAdapter;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.bosch.BoschMainActivity.OnDisplayModeChangedListener;
import com.wcities.eventseeker.bosch.custom.fragment.BoschFragmentLoadableFromBackStack;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.interfaces.BoschOnChildFragmentDisplayModeChangedListener;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.GeoUtil;
import com.wcities.eventseeker.util.GeoUtil.GeoUtilListener;
import com.wcities.eventseeker.util.ViewUtil;
import com.wcities.eventseeker.viewdata.TabBar;

public class BoschSearchResultFragment extends BoschFragmentLoadableFromBackStack implements OnClickListener, 
		OnDisplayModeChangedListener, GeoUtilListener {

	private static final String TAG = BoschSearchResultFragment.class.getName();

	private static final String FRAGMENT_TAG_ARTISTS = "artists";
	private static final String FRAGMENT_TAG_EVENTS = "events";
	private static final String FRAGMENT_TAG_VENUES = "venues";
	
	private SwipeTabsAdapter mTabsAdapter;
	
	private String searchQuery;
	private TabBar tabBar;
	private LinearLayout lnrTabBar;

	private String cityName;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View vTabBar = inflater.inflate(R.layout.fragment_bosch_custom_tabs, null);
		
		lnrTabBar = (LinearLayout) vTabBar.findViewById(R.id.tabBar);
		
		int orientation = getResources().getConfiguration().orientation;

		ViewPager viewPager = (ViewPager) vTabBar.findViewById(R.id.tabContentFrame);
		
		SwipeTabsAdapter oldAdapter = mTabsAdapter;
		
		tabBar = new TabBar(getChildFragmentManager());
		mTabsAdapter = new SwipeTabsAdapter(this, viewPager, tabBar, orientation);
		
		Button btnArtists = (Button) vTabBar.findViewById(R.id.btnTab1);
		btnArtists.setText("ARTISTS");
		btnArtists.setOnClickListener(this);
		
		Button btnEvents = (Button) vTabBar.findViewById(R.id.btnTab2);
		btnEvents.setText("EVENTS");
		btnEvents.setOnClickListener(this);
		
		Button btnVenues = (Button) vTabBar.findViewById(R.id.btnTab3);
		btnVenues.setText("VENUES");
		btnVenues.setOnClickListener(this);
		
		Bundle args;
		if (searchQuery != null) {
			Log.d(TAG, "searchQuery = " + searchQuery);
			args = new Bundle();
    		args.putString(BundleKeys.QUERY, searchQuery);
    		
    	} else {
    		searchQuery = getArguments().getString(BundleKeys.QUERY);
    		args = getArguments();
    	}

		TabBar.Tab tabArtists = new TabBar.Tab(btnArtists, FRAGMENT_TAG_ARTISTS, BoschSearchArtistsFragment.class, 
				args);
		mTabsAdapter.addTab(tabArtists, oldAdapter);

		TabBar.Tab tabEvents = new TabBar.Tab(btnEvents, FRAGMENT_TAG_EVENTS, BoschSearchEventsFragment.class, 
					args);			
		mTabsAdapter.addTab(tabEvents, oldAdapter);
		
		TabBar.Tab tabVenues = new TabBar.Tab(btnVenues, FRAGMENT_TAG_VENUES, BoschSearchVenuesFragment.class, 
				args);
		mTabsAdapter.addTab(tabVenues, oldAdapter);
		
		updateColors();
		return vTabBar;
	}
	
	private void updateColors() {
		ViewUtil.updateViewColor(getResources(), lnrTabBar);
	}

	@Override
	public void onResume() {
		/*****************
		 * 16-09-2014: Now city name is not required in the ActionBar as per the conversation with Amir Sir 
		 * and suggested in the EventSeeker Bosch app issues mail(on 09-09-2014, issue no.23). 
		 * Hence, the below 2 lines are commented and if in future it is required then just uncomment these 
		 * commented lines.
		 ****************** 
		cityName = EventSeekr.getCityName();
		if (cityName == null) {
			GeoUtil.getCityName(this, FragmentUtil.getActivity(this));
		}*/
		super.onResume(AppConstants.INVALID_INDEX, buildTitle());
	}

	private String buildTitle() {
		String title = "Displaying Results for '" + searchQuery + "'";
		return (cityName == null || cityName.length() == 0) ? title : cityName + " - " + title;
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.btnTab1:
			tabBar.select(tabBar.getTabByTag(FRAGMENT_TAG_ARTISTS));
			break;
			
		case R.id.btnTab2:
			tabBar.select(tabBar.getTabByTag(FRAGMENT_TAG_EVENTS));
			break;
			
		case R.id.btnTab3:
			tabBar.select(tabBar.getTabByTag(FRAGMENT_TAG_VENUES));
			break;

		default:
			break;
		}
	}

	@Override
	public void onDisplayModeChanged(boolean isNightModeEnabled) {
		updateColors();
		
		if (mTabsAdapter != null) {
			List<Fragment> pageFragments = mTabsAdapter.getTabFragments();

			for (Iterator<Fragment> iterator = pageFragments.iterator(); iterator.hasNext();) {
				BoschOnChildFragmentDisplayModeChangedListener fragment = 
					(BoschOnChildFragmentDisplayModeChangedListener) iterator.next();
				fragment.onChildFragmentDisplayModeChanged();
			}
		}
	}

	@Override
	public void onAddressSearchCompleted(String strAddress) {}

	@Override
	public void onCitySearchCompleted(final String city) {
		FragmentUtil.getActivity(this).runOnUiThread(new Runnable() {

			@Override
			public void run() {				
				if (city != null && city.length() != 0) {
					cityName = city;
					((BoschMainActivity)FragmentUtil.getActivity(BoschSearchResultFragment.this))
						.updateTitleForFragment(buildTitle(), BoschSearchResultFragment.class.getSimpleName());
				}
			}
		});
	}

	@Override
	public void onLatlngSearchCompleted(Address address) {}

}