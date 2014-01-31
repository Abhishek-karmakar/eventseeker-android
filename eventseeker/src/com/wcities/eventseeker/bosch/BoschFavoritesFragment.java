package com.wcities.eventseeker.bosch;

import android.location.Address;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.adapter.SwipeTabsAdapter;
import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.api.UserInfoApi.Type;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.GeoUtil;
import com.wcities.eventseeker.util.GeoUtil.GeoUtilListener;
import com.wcities.eventseeker.viewdata.TabBar;

public class BoschFavoritesFragment extends FragmentLoadableFromBackStack implements GeoUtilListener, 
		OnClickListener {
	
	private SwipeTabsAdapter mTabsAdapter;
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
		String cityName = GeoUtil.getCityName(this, (EventSeekr) FragmentUtil.getActivity(this).getApplication());
		super.onResume(BoschMainActivity.INDEX_NAV_ITEM_FAVORITES, buildTitle(cityName));
	}
	
	private String buildTitle(String cityName) {
		return (cityName == null || cityName.length() == 0) ? "My Events" : cityName + " - My Events";
	}

	@Override
	public void onAddressSearchCompleted(String strAddress) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onCitySearchCompleted(String city) {
		if (city != null && city.length() != 0) {
			((BoschMainActivity)FragmentUtil.getActivity(this)).updateTitleForFragment(buildTitle(city), 
					getClass().getSimpleName());
		}
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
}
