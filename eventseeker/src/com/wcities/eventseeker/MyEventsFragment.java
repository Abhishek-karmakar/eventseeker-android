package com.wcities.eventseeker;

import java.util.Iterator;
import java.util.List;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout.DrawerListener;
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
import com.wcities.eventseeker.adapter.SwipeTabsAdapter.SwipeTabsAdapterListener;
import com.wcities.eventseeker.analytics.GoogleAnalyticsTracker;
import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.CustomSharedElementTransitionSource;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.VersionUtil;
import com.wcities.eventseeker.util.ViewUtil;
import com.wcities.eventseeker.viewdata.TabBar;

public class MyEventsFragment extends FragmentLoadableFromBackStack implements OnClickListener, DrawerListener,
		SwipeTabsAdapterListener, CustomSharedElementTransitionSource {

	private static final String TAG = MyEventsFragment.class.getName();

	private static final String FRAGMENT_TAG_FOLLOWING = "following";
	private static final String FRAGMENT_TAG_RECOMMENDED = "recommended";
	private static final String FRAGMENT_TAG_SAVED = "saved";
	
	private SwipeTabsAdapter mTabsAdapter;
	private TabBar tabBar;
	
	private int lastGaEventSentForPos;
	
	private boolean isOnPushedToBackStackCalled;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		//Log.d(TAG, "onCreateView()");
		View v = inflater.inflate(R.layout.fragment_custom_tabs, null);
		
		int orientation = getResources().getConfiguration().orientation;

		ViewPager viewPager = (ViewPager) v.findViewById(R.id.tabContentFrame);
		SwipeTabsAdapter oldAdapter = mTabsAdapter;
		tabBar = new TabBar(getChildFragmentManager());
		mTabsAdapter = new SwipeTabsAdapter(this, viewPager, tabBar, orientation, this);
		
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
			actionBar.setCustomView(vTabBar);
		}
		
		Button btnFollowing = (Button) vTabBar.findViewById(R.id.btnTab1);
		btnFollowing.setText(R.string.following);
		btnFollowing.setOnClickListener(this);
		
		Button btnRecommended = (Button) vTabBar.findViewById(R.id.btnTab2);
		btnRecommended.setText(R.string.recommended);
		btnRecommended.setOnClickListener(this);
		
		Button btnSaved = (Button) vTabBar.findViewById(R.id.btnTab3);
		btnSaved.setText(R.string.saved_event);
		btnSaved.setOnClickListener(this);
		
		//vTabBar.findViewById(R.id.btnTab3).setVisibility(View.GONE);
		//vTabBar.findViewById(R.id.vDivider2).setVisibility(View.GONE);
		
		Bundle args = new Bundle();
		args.putSerializable(BundleKeys.LOAD_TYPE, UserInfoApi.Type.myevents);
		TabBar.Tab tabFollowing = new TabBar.Tab(btnFollowing, FRAGMENT_TAG_FOLLOWING, 
				MyEventsListFragment.class, args);
		mTabsAdapter.addTab(tabFollowing, oldAdapter);

		args = new Bundle();
		args.putSerializable(BundleKeys.LOAD_TYPE, UserInfoApi.Type.recommendedevent);
		TabBar.Tab tabRecommended = new TabBar.Tab(btnRecommended, FRAGMENT_TAG_RECOMMENDED, 
				MyEventsListFragment.class, args);
		mTabsAdapter.addTab(tabRecommended, oldAdapter);

		args = new Bundle();
		args.putSerializable(BundleKeys.LOAD_TYPE, UserInfoApi.Type.mysavedevents);
		TabBar.Tab tabSaved = new TabBar.Tab(btnSaved, FRAGMENT_TAG_SAVED, 
				MyEventsListFragment.class, args);
		mTabsAdapter.addTab(tabSaved, oldAdapter);
		
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (getArguments() != null && getArguments().containsKey(BundleKeys.SELECT_RECOMMENDED_EVENTS)) {
			tabBar.select(tabBar.getTabByTag(FRAGMENT_TAG_RECOMMENDED));
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
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
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		ma.setToolbarElevation(ma.getResources().getDimensionPixelSize(R.dimen.action_bar_elevation));
		ma.setVStatusBarVisibility(View.VISIBLE, R.color.colorPrimaryDark);
		ma.setVStatusBarLayeredVisibility(View.GONE, AppConstants.INVALID_ID);
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		ActionBar actionBar = ((ActionBarActivity)FragmentUtil.getActivity(this)).getSupportActionBar();
		actionBar.setDisplayShowCustomEnabled(false);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		List<Fragment> pageFragments = mTabsAdapter.getTabFragments();
		for (Iterator<Fragment> iterator = pageFragments.iterator(); iterator.hasNext();) {
			Fragment fragment = iterator.next();
			fragment.onActivityResult(requestCode, resultCode, data);
		}
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.btnTab1:
			tabBar.select(tabBar.getTabByTag(FRAGMENT_TAG_FOLLOWING));
			break;
			
		case R.id.btnTab2:
			tabBar.select(tabBar.getTabByTag(FRAGMENT_TAG_RECOMMENDED));
			break;

		case R.id.btnTab3:
			tabBar.select(tabBar.getTabByTag(FRAGMENT_TAG_SAVED));
			break;
			
		default:
			break;
		}
	}

	@Override
	public String getScreenName() {
		return "My Events Screen";
	}

	@Override
	public void onSwipeTabSelected(int position) {
		/**
		 * Following condition is required to prevent multiple event sending for same tab selection 
		 * in case if just orientation is changed when any tab other than first (index=0) is selected.
		 */
		if (position != lastGaEventSentForPos) {
			String label;
			if (position == 0) {
				label = "My Events Tab";
				
			} else if (position == 1) {
				label = "Recommended Events Tab";
				
			} else {
				label = "My Saved Events Tab";
			}
			GoogleAnalyticsTracker.getInstance().sendEvent(FragmentUtil.getApplication(this), getScreenName(), 
					label);
			lastGaEventSentForPos = position;
		}
	}

	@Override
	public void onDrawerOpened(View arg0) {
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		ma.setToolbarElevation(ma.getResources().getDimensionPixelSize(R.dimen.action_bar_elevation));
	}
	
	@Override
	public void onDrawerClosed(View view) {
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		ma.setToolbarElevation(0);
	}
	
	@Override
	public void onDrawerSlide(View drawerView, float slideOffset) {
		
	}

	@Override
	public void onDrawerStateChanged(int arg0) {
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
}
