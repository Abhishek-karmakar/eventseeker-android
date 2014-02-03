package com.wcities.eventseeker.bosch;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.adapter.SwipeTabsAdapter;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.TabBar;

public class BoschSearchResultFragment extends FragmentLoadableFromBackStack implements OnClickListener {

	private static final String TAG = BoschSearchResultFragment.class.getName();

	private static final String FRAGMENT_TAG_ARTISTS = "artists";
	private static final String FRAGMENT_TAG_EVENTS = "events";
	private static final String FRAGMENT_TAG_VENUES = "venues";
	
	private SwipeTabsAdapter mTabsAdapter;
	
	private String searchQuery;
	private TabBar tabBar;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View vTabBar = inflater.inflate(R.layout.fragment_bosch_custom_tabs, null);
		
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
		
		return vTabBar;
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

}