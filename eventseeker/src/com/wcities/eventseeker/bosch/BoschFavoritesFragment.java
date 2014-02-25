package com.wcities.eventseeker.bosch;

import java.util.Iterator;
import java.util.List;

import android.location.Address;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.adapter.SwipeTabsAdapter;
import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.api.UserInfoApi.Type;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.bosch.BoschMainActivity.OnDisplayModeChangedListener;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.BoschOnChildFragmentDisplayModeChangedListener;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.GeoUtil;
import com.wcities.eventseeker.util.GeoUtil.GeoUtilListener;
import com.wcities.eventseeker.util.ViewUtil;
import com.wcities.eventseeker.viewdata.TabBar;

public class BoschFavoritesFragment extends FragmentLoadableFromBackStack implements GeoUtilListener, 
		OnClickListener, OnDisplayModeChangedListener {
	
	private SwipeTabsAdapter mTabsAdapter;
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
		btnArtists.setText("Artists");
		btnArtists.setOnClickListener(this);
		
		Button btnMyEvents = (Button) vTabBar.findViewById(R.id.btnTab2);
		btnMyEvents.setText("My Events");
		btnMyEvents.setOnClickListener(this);
		
		Button btnRecommendedEvents = (Button) vTabBar.findViewById(R.id.btnTab3);
		btnRecommendedEvents.setText("Recommended Events");
		btnRecommendedEvents.setOnClickListener(this);
		
		TabBar.Tab tabArtists = new TabBar.Tab(btnArtists, BoschMyArtistsListFragment.class.getSimpleName(), 
				BoschMyArtistsListFragment.class, null);
		mTabsAdapter.addTab(tabArtists, oldAdapter);

		Bundle args = new Bundle();
		args.putSerializable(BundleKeys.LOAD_TYPE, UserInfoApi.Type.myevents);
		TabBar.Tab tabMyEvents = new TabBar.Tab(btnMyEvents, BoschMyEventsListFragment.getTag(Type.myevents), 
				BoschMyEventsListFragment.class, args);
		mTabsAdapter.addTab(tabMyEvents, oldAdapter);
		
		args = new Bundle();
		args.putSerializable(BundleKeys.LOAD_TYPE, UserInfoApi.Type.recommendedevent);
		TabBar.Tab tabRecommendedEvents = new TabBar.Tab(btnRecommendedEvents, BoschMyEventsListFragment
				.getTag(Type.recommendedevent), BoschMyEventsListFragment.class, args);
		mTabsAdapter.addTab(tabRecommendedEvents, oldAdapter);
		
		return vTabBar;
	}

	@Override
	public void onResume() {
		cityName = EventSeekr.getCityName();
		if (cityName == null) {
			GeoUtil.getCityName(this, FragmentUtil.getActivity(this));
		}
		super.onResume(BoschMainActivity.INDEX_NAV_ITEM_FAVORITES, buildTitle());
	}
	
	private String buildTitle() {
		return (cityName == null || cityName.length() == 0) ? "My Events" : cityName + " - My Events";
	}

	@Override
	public void onAddressSearchCompleted(String strAddress) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onCitySearchCompleted(final String city) {
		FragmentUtil.getActivity(this).runOnUiThread(new Runnable() {

			@Override
			public void run() {		
				cityName = city;
				if (city != null && city.length() != 0) {
					((BoschMainActivity)FragmentUtil.getActivity(BoschFavoritesFragment.this))
						.updateTitleForFragment(buildTitle(), BoschFavoritesFragment.class.getSimpleName());
				}								
			}
		});
	}

	@Override
	public void onLatlngSearchCompleted(Address address) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.btnTab1:
			tabBar.select(tabBar.getTabByTag(BoschMyArtistsListFragment.class.getSimpleName()));
			break;
			
		case R.id.btnTab2:
			tabBar.select(tabBar.getTabByTag(BoschMyEventsListFragment.getTag(Type.myevents)));
			break;
			
		case R.id.btnTab3:
			tabBar.select(tabBar.getTabByTag(BoschMyEventsListFragment.getTag(Type.recommendedevent)));
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

	private void updateColors() {
		ViewUtil.updateViewColor(getResources(), lnrTabBar);
	}
}
