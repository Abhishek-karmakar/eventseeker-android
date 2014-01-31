package com.wcities.eventseeker.adapter;

import java.util.ArrayList;
import java.util.List;

import android.content.res.Configuration;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Log;

import com.bosch.myspin.serversdk.MySpinServerSDK;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.TabBar;
import com.wcities.eventseeker.viewdata.TabBar.Tab;

public class SwipeTabsAdapter extends FragmentPagerAdapter implements TabBar.Tab.TabListener, 
		ViewPager.OnPageChangeListener {

	private static final String TAG = SwipeTabsAdapter.class.getName();
	
	private Fragment fragment;
	private final ViewPager mViewPager;
	private TabBar tabBar;
	private List<Fragment> tabFragments;
	private int orientation;

	public SwipeTabsAdapter(Fragment fragment, ViewPager pager, TabBar tabBar, int orientation) {
		super(fragment.getChildFragmentManager());
		this.fragment = fragment;
		mViewPager = pager;
		this.tabBar = tabBar;
		tabFragments = new ArrayList<Fragment>();
		mViewPager.setAdapter(this);
		mViewPager.setOnPageChangeListener(this);
		this.orientation = orientation;
	}

	public void addTab(Tab tab, SwipeTabsAdapter oldAdapterToRetainState) {
		//Log.d(TAG, "addTab()");
		tab.setListener(this);
		if (oldAdapterToRetainState == null) {
			tabBar.addTab(tab);

		} else {
			tabBar.addTab(tab, oldAdapterToRetainState.tabBar);
			/**
			 * It is possible that there are 3 tabs total but oldAdapterToRetainState.tabFragments.size() is only 2
			 * before orientation change since user had not swiped or selected 2nd tab yet resulting in getItem()
			 * being called only for positions 0 & 1. In this case we only need to retain these 2 fragments.
			 */
			if (tabFragments.size() < oldAdapterToRetainState.tabFragments.size()) {
				tabFragments.add(oldAdapterToRetainState.tabFragments.get(tabFragments.size()));
			}
		}
		notifyDataSetChanged();
		
		// For bosch we need tab backgrounds as in portrait orientation of pure android version 
		if ((orientation == Configuration.ORIENTATION_PORTRAIT || MySpinServerSDK.sharedInstance().isConnected()) 
				&& tabBar.getNumberOfTabs() > 1) {
			updateTabsBgForPortrait();
		}
	}
	
	private void updateTabsBgForPortrait() {
		switch (tabBar.getNumberOfTabs()) {
		
		case 2:
			tabBar.getTab(0).setButtonBg(R.drawable.left_most_tab_indicator_holo);
			tabBar.getTab(1).setButtonBg(R.drawable.right_most_tab_indicator_holo);
			break;
			
		case 3:
			tabBar.getTab(0).setButtonBg(R.drawable.left_most_tab_indicator_holo);
			if (!MySpinServerSDK.sharedInstance().isConnected()) {
				tabBar.getTab(1).setButtonBg(R.drawable.tab_indicator_holo);
				
			} else {
				tabBar.getTab(1).setButtonBg(R.drawable.tab_indicator_holo_bosch);
			}
			tabBar.getTab(2).setButtonBg(R.drawable.right_most_tab_indicator_holo);
			break;

		default:
			break;
		} 
	}
	
	@Override
	public int getCount() {
		//Log.d(TAG, "getCount() = " + tabBar.getNumberOfTabs());
		return tabBar.getNumberOfTabs();
	}

	@Override
	public Fragment getItem(int position) {
		Log.d(TAG, "getItem(), position = " + position);
		Tab tab = tabBar.getTab(position);
		Fragment tabFragment = Fragment.instantiate(FragmentUtil.getActivity(fragment), tab.getClss().getName(), 
				tab.getArgs());
		tabFragments.add(tabFragment);
		return tabFragment;
	}
	
	@Override
    public int getItemPosition(Object object) {
    	return POSITION_NONE;
    }

	public List<Fragment> getTabFragments() {
		return tabFragments;
	}

	@Override
	public void onPageScrolled(int position, float positionOffset,
			int positionOffsetPixels) {
	}

	@Override
	public void onPageSelected(int position) {
		tabBar.select(position);
	}

	@Override
	public void onPageScrollStateChanged(int state) {
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		int pos = tabBar.getPos(tab);
		if (pos != TabBar.TAB_NOT_FOUND) {
			mViewPager.setCurrentItem(pos);
		}
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
	}
}
