package com.wcities.eventseeker;

import java.util.Iterator;
import java.util.List;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.wcities.eventseeker.adapter.SwipeTabsAdapter;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.CustomSharedElementTransitionSource;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.VersionUtil;
import com.wcities.eventseeker.util.ViewUtil;
import com.wcities.eventseeker.viewdata.TabBar;

public class SearchFragment extends FragmentLoadableFromBackStack implements OnClickListener, 
		CustomSharedElementTransitionSource {
	
	private static final String TAG = SearchFragment.class.getName();

	private static final String FRAGMENT_TAG_ARTISTS = "artists";
	private static final String FRAGMENT_TAG_EVENTS = "events";
	private static final String FRAGMENT_TAG_VENUES = "venues";
	
	private SwipeTabsAdapter mTabsAdapter;
	
	private String searchQuery;
	private TabBar tabBar;
	
	private boolean isOnPushedToBackStackCalled;
	
	//private SearchView searchView;
	
	public interface SearchFragmentChildListener {
		public void onQueryTextSubmit(String query);
    }
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_custom_tabs, null);
		
		int orientation = getResources().getConfiguration().orientation;

		ViewPager viewPager = (ViewPager) v.findViewById(R.id.tabContentFrame);
		SwipeTabsAdapter oldAdapter = mTabsAdapter;
		tabBar = new TabBar(getChildFragmentManager());
		
		mTabsAdapter = new SwipeTabsAdapter(this, viewPager, tabBar, orientation);
		
		/**
		 * add extra top margin (equal to statusbar height) since we are removing vStatusBar from onStart() 
		 * even though we want search screen to have this statusbar. We had to mark VStatusBar as GONE from 
		 * onStart() so that on transition from any search child fragment (SearchArtists/SearchEvents/SearchVenues)
		 * to corresponding details screen doesn't cause jumping effect on search screen, as we remove vStatusBar 
		 * on detail screen when this search screen is visible in the background
		 */
		if (VersionUtil.isApiLevelAbove18()) {
			Resources res = FragmentUtil.getResources(this);
			LinearLayout lnrLytTabBar = (LinearLayout) v.findViewById(R.id.tabBar);
			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) lnrLytTabBar.getLayoutParams();
			lp.topMargin = res.getDimensionPixelSize(R.dimen.common_t_mar_pad_for_all_layout) 
					+ ViewUtil.getStatusBarHeight(res);
			lnrLytTabBar.setLayoutParams(lp);
		}
		
		View vTabBar;
		if (orientation == Configuration.ORIENTATION_PORTRAIT) {
			vTabBar = v;
			
		} else {
			ActionBar actionBar = ((ActionBarActivity)FragmentUtil.getActivity(this)).getSupportActionBar();
			actionBar.setDisplayShowCustomEnabled(true);
			
			vTabBar = inflater.inflate(R.layout.custom_actionbar_tabs, null);
			//LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER);
			actionBar.setCustomView(vTabBar);
		}
		
		Button btnArtists = (Button) vTabBar.findViewById(R.id.btnTab1);
		btnArtists.setText(R.string.artists);
		btnArtists.setOnClickListener(this);
		
		Button btnEvents = (Button) vTabBar.findViewById(R.id.btnTab2);
		btnEvents.setText(R.string.events);
		btnEvents.setOnClickListener(this);
		
		Button btnVenues = (Button) vTabBar.findViewById(R.id.btnTab3);
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

		TabBar.Tab tabArtists = new TabBar.Tab(btnArtists, FRAGMENT_TAG_ARTISTS, SearchArtistsFragment.class, args);
		mTabsAdapter.addTab(tabArtists, oldAdapter);

		TabBar.Tab tabEvents;
		
		if(((MainActivity)FragmentUtil.getActivity(this)).isTablet()) {
			tabEvents = new TabBar.Tab(btnEvents, FRAGMENT_TAG_EVENTS, SearchEventsFragmentTab.class, args);
			
		} else {
			tabEvents = new TabBar.Tab(btnEvents, FRAGMENT_TAG_EVENTS, SearchEventsFragment.class, args);			
		}	
		mTabsAdapter.addTab(tabEvents, oldAdapter);
		
		TabBar.Tab tabVenues = new TabBar.Tab(btnVenues, FRAGMENT_TAG_VENUES, SearchVenuesFragment.class, args);
		mTabsAdapter.addTab(tabVenues, oldAdapter);
		
		return v;
	}
	
	@Override
	public void onStart() {
		//Log.d(TAG, "onStart()");
		super.onStart();
		
		if (!isOnTop()) {
			return;
		}
		
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		ma.setToolbarElevation(0);

		/**
		 * Even though we want status bar in this case, mark it gone to have smoother transition to detail fragment
		 * & prevent jumping effect on search screen, caused due to removal of status bar on detail screen when this 
		 * search screen is visible in background.
		 */
		ma.setVStatusBarVisibility(View.GONE, AppConstants.INVALID_ID);
		ma.setVStatusBarLayeredVisibility(View.VISIBLE, R.color.colorPrimaryDark);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		//Log.d(TAG, "onStop()");
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		ma.setToolbarElevation(ma.getResources().getDimensionPixelSize(R.dimen.action_bar_elevation));
		ma.setVStatusBarVisibility(View.VISIBLE, R.color.colorPrimaryDark);
		ma.setVStatusBarLayeredVisibility(View.GONE, AppConstants.INVALID_ID);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (((EventSeekr)FragmentUtil.getActivity(this).getApplication()).isTablet()) {
			List<Fragment> pageFragments = mTabsAdapter.getTabFragments();
			for (Iterator<Fragment> iterator = pageFragments.iterator(); iterator.hasNext();) {
				Fragment fragment = iterator.next();
				if (fragment instanceof SearchEventsFragmentTab) {
					fragment.onActivityResult(requestCode, resultCode, data);
				}
			}
		}
	}
	
	public String getSearchQuery() {
		return searchQuery;
	}

	public boolean onQueryTextSubmit(String query) {
		//Log.d(TAG, "onQueryTextSubmit(), query = " + query);
		searchQuery = query;
		//hideSoftKeypad();
		
		if (mTabsAdapter != null) {
			//Log.d(TAG, "mTabsAdapter != null");
			int count = 0;
			List<Fragment> pageFragments = mTabsAdapter.getTabFragments();
			
			// Refresh fragments which are already instantiated
			for (Iterator<Fragment> iterator = pageFragments.iterator(); iterator.hasNext();) {
				//Log.d(TAG, "next fragment");
				count++;
				SearchFragmentChildListener fragment = (SearchFragmentChildListener) iterator.next();
				fragment.onQueryTextSubmit(query);
			}
			
			// Update args for remaining fragments which are yet to be instantiated by getItem() of SwipeTabsAdapter.
			Bundle args = new Bundle();
			args.putString(BundleKeys.QUERY, query);
			for (; count < tabBar.getNumberOfTabs(); count++) {
				tabBar.setArgs(args, count);
			}
		}

		return true;
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
	public String getScreenName() {
		return "Search Screen";
	}

	@Override
	public void addViewsToBeHidden(View... views) {
		// TODO Auto-generated method stub
	}

	@Override
	public void hideSharedElements() {
		((CustomSharedElementTransitionSource) mTabsAdapter.getSelectedFragment()).hideSharedElements();		
	}

	@Override
	public void onPushedToBackStack() {
		/**
		 * Not calling onStop() to prevent toolbar color changes occurring in between
		 * the transition
		 */
		//onStop();
		isOnPushedToBackStackCalled = true;
	}

	@Override
	public void onPoppedFromBackStack() {
		if (isOnPushedToBackStackCalled) {
			isOnPushedToBackStackCalled = false;
			
			// to update statusbar visibility
			onStart();
			// to call onFragmentResumed(Fragment) of MainActivity (to update title, current fragment tag, etc.)
			onResume();
			
			((CustomSharedElementTransitionSource) mTabsAdapter.getSelectedFragment()).onPoppedFromBackStack();
		}
	}

	@Override
	public boolean isOnTop() {
		return !isOnPushedToBackStackCalled;
	}
}
