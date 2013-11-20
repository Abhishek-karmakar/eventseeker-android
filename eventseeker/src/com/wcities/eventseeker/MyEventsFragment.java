package com.wcities.eventseeker;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.wcities.eventseeker.adapter.SwipeTabsAdapter;
import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.TabBar;

public class MyEventsFragment extends FragmentLoadableFromBackStack implements OnClickListener {

	private static final String TAG = MyEventsFragment.class.getName();

	private static final String FRAGMENT_TAG_FOLLOWING = "following";
	private static final String FRAGMENT_TAG_RECOMMENDED = "recommended";
	
	private SwipeTabsAdapter mTabsAdapter;
	private TabBar tabBar;
	
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
		mTabsAdapter = new SwipeTabsAdapter(this, viewPager, tabBar, orientation);
		
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
		btnFollowing.setText("FOLLOWING");
		btnFollowing.setOnClickListener(this);
		
		Button btnRecommended = (Button) vTabBar.findViewById(R.id.btnTab2);
		btnRecommended.setText("RECOMMENDED");
		btnRecommended.setOnClickListener(this);
		
		vTabBar.findViewById(R.id.btnTab3).setVisibility(View.GONE);
		vTabBar.findViewById(R.id.vDivider2).setVisibility(View.GONE);
		
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
		
		return v;
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		ActionBar actionBar = ((ActionBarActivity)FragmentUtil.getActivity(this)).getSupportActionBar();
		actionBar.setCustomView(null);
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
			
		default:
			break;
		}
	}
}
