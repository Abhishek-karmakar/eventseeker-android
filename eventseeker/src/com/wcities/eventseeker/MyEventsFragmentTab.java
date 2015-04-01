package com.wcities.eventseeker;

import java.util.Iterator;
import java.util.List;

import android.content.Intent;
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

import com.wcities.eventseeker.adapter.SwipeTabsAdapter;
import com.wcities.eventseeker.adapter.SwipeTabsAdapter.SwipeTabsAdapterListener;
import com.wcities.eventseeker.analytics.GoogleAnalyticsTracker;
import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.TabBar;

public class MyEventsFragmentTab extends FragmentLoadableFromBackStack implements OnClickListener,
		SwipeTabsAdapterListener {

	private static final String TAG = MyEventsFragmentTab.class.getName();

	private static final String LABEL_MY_EVENTS_TAB = "My Events Tab";
	private static final String LABEL_RECOMMENDED_EVENTS_TAB = "Recommended Events Tab";
	private static final String LABEL_SAVED_EVENTS_TAB = "My Saved Events Tab";

	private static final String FRAGMENT_TAG_FOLLOWING = "following";
	private static final String FRAGMENT_TAG_RECOMMENDED = "recommended";
	private static final String FRAGMENT_TAG_SAVED = "saved";

	
	private SwipeTabsAdapter mTabsAdapter;
	private TabBar tabBar;
	
	private int lastGaEventSentForPos;

	private List<Button> tabBarButtons;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		//Log.d(TAG, "onCreateView()");
		View v = inflater.inflate(R.layout.fragment_custom_tabs_tab, null);
		
		int orientation = getResources().getConfiguration().orientation;

		ViewPager viewPager = (ViewPager) v.findViewById(R.id.tabContentFrame);
		SwipeTabsAdapter oldAdapter = mTabsAdapter;
		tabBar = new TabBar(getChildFragmentManager());
		mTabsAdapter = new SwipeTabsAdapter(this, viewPager, tabBar, orientation, this);
		
		tabBarButtons = ((MyEventsActivityTab) FragmentUtil.getActivity(this)).getTabBarButtons();
		
		Button btnFollowing = tabBarButtons.get(0);
		btnFollowing.setText(R.string.following);
		btnFollowing.setOnClickListener(this);
		
		Button btnRecommended = tabBarButtons.get(1);
		btnRecommended.setText(R.string.recommended);
		btnRecommended.setOnClickListener(this);
		
		Button btnSaved = tabBarButtons.get(2);
		btnSaved.setText(R.string.saved_event);
		btnSaved.setOnClickListener(this);
		
		//vTabBar.findViewById(R.id.btnTab3).setVisibility(View.GONE);
		//vTabBar.findViewById(R.id.vDivider2).setVisibility(View.GONE);
		
		Bundle args = new Bundle();
		args.putSerializable(BundleKeys.LOAD_TYPE, UserInfoApi.Type.myevents);
		TabBar.Tab tabFollowing = new TabBar.Tab(btnFollowing, FRAGMENT_TAG_FOLLOWING, 
				MyEventsGridFragmentTab.class, args);
		mTabsAdapter.addTab(tabFollowing, oldAdapter);

		args = new Bundle();
		args.putSerializable(BundleKeys.LOAD_TYPE, UserInfoApi.Type.recommendedevent);
		TabBar.Tab tabRecommended = new TabBar.Tab(btnRecommended, FRAGMENT_TAG_RECOMMENDED, 
				MyEventsGridFragmentTab.class, args);
		mTabsAdapter.addTab(tabRecommended, oldAdapter);

		args = new Bundle();
		args.putSerializable(BundleKeys.LOAD_TYPE, UserInfoApi.Type.mysavedevents);
		TabBar.Tab tabSaved = new TabBar.Tab(btnSaved, FRAGMENT_TAG_SAVED, 
				MyEventsGridFragmentTab.class, args);
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
		return ScreenNames.MY_EVENTS_SCREEN;
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
				label = LABEL_MY_EVENTS_TAB;
				
			} else if (position == 1) {
				label = LABEL_RECOMMENDED_EVENTS_TAB;
				
			} else {
				label = LABEL_SAVED_EVENTS_TAB;
			}
			GoogleAnalyticsTracker.getInstance().sendEvent(FragmentUtil.getApplication(this), 
					getScreenName(), label);
			lastGaEventSentForPos = position;
		}
	}

	public void onEventAttendingUpdated() {
		List<Fragment> pageFragments = mTabsAdapter.getTabFragments();
		for (Iterator<Fragment> iterator = pageFragments.iterator(); iterator.hasNext();) {
			MyEventsGridFragmentTab myEventsGridFragmentTab = (MyEventsGridFragmentTab) iterator.next();
			myEventsGridFragmentTab.onEventAttendingUpdated();
		}
	}
}
