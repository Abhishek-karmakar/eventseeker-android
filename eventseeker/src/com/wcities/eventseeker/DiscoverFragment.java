package com.wcities.eventseeker;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.viewpagerindicator.CirclePageIndicator;

public class DiscoverFragment extends DiscoverParentFragment implements
		OnPageChangeListener {

	public static final String TAG = DiscoverFragment.class.getName();

	private ViewPager viewPager;
	private FeaturedEventsFragmentPagerAdapter featuredEventsFragmentPagerAdapter;

	private int viewPagerSelectedPos;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = super.onCreateView(inflater, container, savedInstanceState);

		// txtFeaturedEvtsTitle = (TextView)
		// v.findViewById(R.id.txtFeaturedEvtsTitle);
		// updateCityName();
		
		featuredEventsFragmentPagerAdapter = new FeaturedEventsFragmentPagerAdapter(
				this, getChildFragmentManager());

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

		/*
		 * imgPrev = (ImageView) v.findViewById(R.id.imgPrev); imgNext =
		 * (ImageView) v.findViewById(R.id.imgNext);
		 * imgPrev.setOnClickListener(this); imgNext.setOnClickListener(this);
		 */

		/**
		 * Following call to onPageSelected() is required due to ViewPager api
		 * bug where it doesn't call onPageSelected() for first item (at
		 * index=0) on its own. It is needed only on orientation change because
		 * for first launch, we have a work around placed within getItem() of
		 * DiscoverFragmentPagerAdapter.
		 */
		/*
		 * if (viewPager.getCurrentItem() == 0) {
		 * onPageSelected(viewPager.getCurrentItem()); }
		 */

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

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
	}

	/*
	 * @Override public void onActivityResult(int requestCode, int resultCode,
	 * Intent data) { if (requestCode == CHANGE_LOCATION_REQUEST) { if
	 * (resultCode == Activity.RESULT_OK) { Bundle extras = data.getExtras();
	 * double newLat = extras.getDouble(BundleKeys.LAT); double newLon =
	 * extras.getDouble(BundleKeys.LON);
	 * 
	 * if (lat != newLat || lon != newLon) { lat = newLat; lon = newLon;
	 * 
	 * updateCityName(); featuredEvts.clear();
	 * discoverFragmentPagerAdapter.notifyDataSetChanged(); new
	 * LoadFeaturedEvts().execute(); } } } }
	 */

	/*
	 * private void updateCityName() { Geocoder geocoder = new
	 * Geocoder(((Activity)mListener).getApplicationContext(),
	 * Locale.getDefault()); try { List<Address> addresses =
	 * geocoder.getFromLocation(lat, lon, 1);
	 * 
	 * if (addresses != null && !addresses.isEmpty()) { Address address =
	 * addresses.get(0); cityName = address.getLocality();
	 * 
	 * String commonTitle =
	 * getResources().getString(R.string.title_featured_evts); String city =
	 * commonTitle + cityName; txtFeaturedEvtsTitle.setText(city);
	 * txtFeaturedEvtsTitle.setSelected(true);
	 * 
	 * } else { Log.w(TAG, "No relevant address found."); }
	 * 
	 * } catch (IOException e) { e.printStackTrace(); } }
	 */

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
			/*
			 * if (index == discoverActivity.get().viewPager.getCurrentItem()) {
			 * discoverActivity.get().onPageSelected(index); }
			 */

			FeaturedEventsFragment featuredEventsFragment = FeaturedEventsFragment
					.newInstance(discoverFragment.get().featuredEvts.get(index));
			return featuredEventsFragment;
		}

		@Override
		public int getCount() {
			// Log.i(TAG, "count = " +
			// discoverActivity.get().featuredEvts.size());
			return discoverFragment.get().featuredEvts.size();
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
