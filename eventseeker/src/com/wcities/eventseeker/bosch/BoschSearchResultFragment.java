package com.wcities.eventseeker.bosch;

import java.util.Iterator;
import java.util.List;

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
import com.wcities.eventseeker.bosch.BoschMainActivity.OnDisplayModeChangedListener;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.BoschOnChildFragmentDisplayModeChangedListener;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.ViewUtil;
import com.wcities.eventseeker.viewdata.TabBar;

public class BoschSearchResultFragment extends FragmentLoadableFromBackStack implements OnClickListener, 
		OnDisplayModeChangedListener {

	private static final String TAG = BoschSearchResultFragment.class.getName();

	private static final String FRAGMENT_TAG_ARTISTS = "artists";
	private static final String FRAGMENT_TAG_EVENTS = "events";
	private static final String FRAGMENT_TAG_VENUES = "venues";
	
	private SwipeTabsAdapter mTabsAdapter;
	
	private String searchQuery;
	private TabBar tabBar;
	private LinearLayout lnrTabBar;
	
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
		if (AppConstants.IS_NIGHT_MODE_ENABLED) {
			lnrTabBar.setBackgroundResource(R.drawable.tab_bar_rounded_corners_night_mode);
		} else {
			lnrTabBar.setBackgroundResource(R.drawable.tab_bar_rounded_corners);			
		}
		ViewUtil.updateFontColor(getResources(), lnrTabBar);
	}

	@Override
	public void onResume() {
		super.onResume();
		((BoschMainActivity) FragmentUtil.getActivity(this)).onFragmentResumed(this, 
			AppConstants.INVALID_INDEX, getResources().getString(R.string.title_search_results));
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

}