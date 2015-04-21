package com.wcities.eventseeker;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.viewpagerindicator.CirclePageIndicator;

public class DiscoverFragment1 extends DiscoverParentFragment implements OnPageChangeListener {

	public static final String TAG = DiscoverFragment.class.getName();

	private ViewPager viewPager;
	private FeaturedEventsFragmentPagerAdapter featuredEventsFragmentPagerAdapter;

	private int viewPagerSelectedPos;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = super.onCreateView(inflater, container, savedInstanceState);

		/*featuredEventsFragmentPagerAdapter = new FeaturedEventsFragmentPagerAdapter(
				this, getChildFragmentManager());*/

		viewPager = (ViewPager) v.findViewById(R.id.viewPager);
		viewPager.setAdapter(featuredEventsFragmentPagerAdapter);
		viewPager.setOnPageChangeListener(this);

		CirclePageIndicator indicator = (CirclePageIndicator) v
				.findViewById(R.id.pageIndicator);
		if (indicator != null) {
			indicator.setViewPager(viewPager);
		}
		// Log.d(TAG, "viewPagerSelectedPos = " + viewPagerSelectedPos);
		viewPager.setCurrentItem(viewPagerSelectedPos);

		return v;
	}

	@Override
	public void onDestroyView() {
		// Log.d(TAG, "onDestroyView()");
		if (viewPager != null) {
			viewPagerSelectedPos = viewPager.getCurrentItem();
		}
		super.onDestroyView();
	}

	private static class FeaturedEventsFragmentPagerAdapter extends
			FragmentStatePagerAdapter {

		private WeakReference<DiscoverFragment> discoverFragment;

		public FeaturedEventsFragmentPagerAdapter(
				DiscoverFragment discoverFragment, FragmentManager fm) {
			super(fm);
			this.discoverFragment = new WeakReference<DiscoverFragment>(
					discoverFragment);
		}

		@Override
		public Fragment getItem(int index) {
			// Log.d(TAG, "getItem() for index = " + index);
			/**
			 * When viewpager data source changes & notifyDataSetChanged() is
			 * refreshing viewpager, onPageSelected() method is not called up
			 * for first time unless we scroll in any direction. Hence to update
			 * left & right arrow buttons, we need to call this call back method
			 * explicitly for currently visible index.
			 */
			/*FeaturedEventsFragment featuredEventsFragment = FeaturedEventsFragment
					.newInstance(discoverFragment.get().featuredEvts.get(index));
			return featuredEventsFragment;*/
			return null;
		}

		@Override
		public int getCount() {
			// Log.i(TAG, "count = " +
			//return discoverFragment.get().featuredEvts.size();
			return 0;
		}

		@Override
		public int getItemPosition(Object object) {
			/**
			 * This update of right left arrows is required on user changing
			 * location & hence featured events are to be requeried (basically
			 * invalidating previous views & hence updating arrows' visibility
			 * accordingly).
			 */
			// discoverActivity.get().updateRightLeftArrows(discoverActivity.get().viewPager.getCurrentItem());
			return POSITION_NONE;
		}
		
		/**
		 * W/o following blank function, app crashes with NullPointerException in v4 support library
		 * on orientation change on the screen where this adapter is used.
		 */
		@Override
		public void restoreState(Parcelable arg0, ClassLoader arg1) {
			
		}
	}

	@Override
	public void onPageScrollStateChanged(int arg0) {
	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
	}

	@Override
	public void onPageSelected(int arg0) {
	}

	@Override
	protected void notifyDataSetChanged() {
		featuredEventsFragmentPagerAdapter.notifyDataSetChanged();
	}

}
