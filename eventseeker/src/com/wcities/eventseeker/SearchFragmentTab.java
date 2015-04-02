package com.wcities.eventseeker;

import java.util.List;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.wcities.eventseeker.adapter.SwipeTabsAdapter;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.custom.fragment.FragmentRetainingChildFragmentManager;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.TabBar;

public class SearchFragmentTab extends FragmentRetainingChildFragmentManager implements OnClickListener {

	private SwipeTabsAdapter mTabsAdapter;
	private TabBar tabBar;
	
	private String searchQuery;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_custom_tabs_tab, null);
		
		ViewPager viewPager = (ViewPager) v.findViewById(R.id.tabContentFrame);
		SwipeTabsAdapter oldAdapter = mTabsAdapter;
		tabBar = new TabBar(childFragmentManager());
		
		mTabsAdapter = new SwipeTabsAdapter(this, viewPager, tabBar);
		
		List<Button> tabBarButtons = ((BaseActivityTab) FragmentUtil.getActivity(this)).getTabBarButtons();
		
		Button btnArtists = (Button) tabBarButtons.get(0);
		btnArtists.setText(R.string.artists);
		btnArtists.setOnClickListener(this);
		
		Button btnEvents = (Button) tabBarButtons.get(1);
		btnEvents.setText(R.string.events);
		btnEvents.setOnClickListener(this);
		
		Button btnVenues = (Button) tabBarButtons.get(2);
		btnVenues.setText(R.string.venues);
		btnVenues.setOnClickListener(this);
		
		Bundle args;
		if (searchQuery != null) {
			//Log.d(TAG, "searchQuery = " + searchQuery);
			args = new Bundle();
    		args.putString(BundleKeys.QUERY, searchQuery);
    		
    	} else {
    		searchQuery = getArguments().getString(BundleKeys.QUERY);
    		args = getArguments();
    	}
		
		TabBar.Tab tabArtists = new TabBar.Tab(btnArtists, FragmentUtil.getTag(SearchArtistsFragmentTab.class), 
				SearchArtistsFragmentTab.class, args);
		mTabsAdapter.addTab(tabArtists, oldAdapter);

		TabBar.Tab tabEvents = new TabBar.Tab(btnEvents, FragmentUtil.getTag(SearchEventsFragmentTab.class), 
				SearchEventsFragmentTab.class, args);
		mTabsAdapter.addTab(tabEvents, oldAdapter);
		
		TabBar.Tab tabVenues = new TabBar.Tab(btnVenues, FragmentUtil.getTag(SearchVenuesFragmentTab.class), 
				SearchVenuesFragmentTab.class, args);
		mTabsAdapter.addTab(tabVenues, oldAdapter);
		
		return v;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.btnTab1:
			tabBar.select(tabBar.getTabByTag(FragmentUtil.getTag(SearchArtistsFragmentTab.class)));
			break;
			
		case R.id.btnTab2:
			tabBar.select(tabBar.getTabByTag(FragmentUtil.getTag(SearchEventsFragmentTab.class)));
			break;
			
		case R.id.btnTab3:
			tabBar.select(tabBar.getTabByTag(FragmentUtil.getTag(SearchVenuesFragmentTab.class)));
			break;

		default:
			break;
		}
	}
}
