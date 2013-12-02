package com.wcities.eventseeker;

import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.viewpagerindicator.CirclePageIndicator;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.EventApi;
import com.wcities.eventseeker.api.InfoApi;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Category;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.custom.view.ExpandableGridView;
import com.wcities.eventseeker.jsonparser.EventApiJSONParser;
import com.wcities.eventseeker.jsonparser.InfoApiJSONParser;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class DiscoverFragment extends FragmentLoadableFromBackStack implements
		OnPageChangeListener {

	public static final String TAG = DiscoverFragment.class.getName();

	private DiscoverFragmentListener mListener;
	// private TextView txtFeaturedEvtsTitle;
	// private ImageView imgPrev, imgNext;
	private ViewPager viewPager;
	private EvtCategoriesListAdapter evtCategoriesListAdapter;
	private FeaturedEventsFragmentPagerAdapter featuredEventsFragmentPagerAdapter;

	private LoadFeaturedEvts loadFeaturedEvts;
	private List<Category> evtCategories;
	private List<Event> featuredEvts;
	private int viewPagerSelectedPos;

	private double lat, lon;

	private static final int FEATURED_EVTS_LIMIT = 5;

	// Container Activity must implement this interface
	public interface DiscoverFragmentListener {
		public void replaceSelfByFragment(String fragmentTag, Bundle args);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		// Log.d(TAG, "onAttach");
		try {
			mListener = (DiscoverFragmentListener) activity;

		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement DiscoverFragmentListener");
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Log.i(TAG, "onCreate");
		setHasOptionsMenu(true);
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		if (evtCategories == null) {
			buildEvtCategories();
			featuredEvts = new ArrayList<Event>();
			generateLatLon();
			evtCategoriesListAdapter = new EvtCategoriesListAdapter();
		}
		featuredEventsFragmentPagerAdapter = new FeaturedEventsFragmentPagerAdapter(
				this, getChildFragmentManager());

		View v = inflater.inflate(R.layout.fragment_discover, container, false);

		// txtFeaturedEvtsTitle = (TextView)
		// v.findViewById(R.id.txtFeaturedEvtsTitle);
		// updateCityName();

		viewPager = (ViewPager) v.findViewById(R.id.viewPager);
		GridView grdEvtCategories = (GridView) v
				.findViewById(R.id.grdEvtCategories);
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT
				|| (((EventSeekr) FragmentUtil.getActivity(this)
						.getApplicationContext()).isTablet())) {
			((ExpandableGridView) grdEvtCategories).setExpanded(true);
		}
		grdEvtCategories.setAdapter(evtCategoriesListAdapter);

		/*
		 * if (evtCategories.isEmpty()) { new LoadEvtCategories().execute(); }
		 */

		if (featuredEvts.isEmpty()) {
			loadFeaturedEvts = new LoadFeaturedEvts();
			loadFeaturedEvts.execute();
		}

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
		/**
		 * Cancel pending AsyncTask for loading events; otherwise running
		 * AsyncTask LoadFeaturedEvts after this can throw IllegalStateException
		 * saying "Fragment is not currently in the fragmentmanager" while
		 * invoking notifyDataSetChanged() on discoverFragmentPagerAdapter from
		 * onPostExecute() of LoadFeaturedEvts.
		 */
		if (loadFeaturedEvts != null
				&& loadFeaturedEvts.getStatus() != Status.FINISHED) {
			Log.d(TAG, "cancel loading events");
			loadFeaturedEvts.cancel(true);
		}
		if (viewPager != null) {
			viewPagerSelectedPos = viewPager.getCurrentItem();
		}
		super.onDestroyView();
	}

	private void generateLatLon() {
		double[] latLon = DeviceUtil.getLatLon((Activity) mListener);
		lat = latLon[0];
		lon = latLon[1];
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_discover, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		/*
		 * case R.id.action_chg_location:
		 * mListener.replaceSelfByFragment(AppConstants
		 * .FRAGMENT_TAG_CHANGE_LOCATION, null); return true;
		 */

		default:
			break;
		}

		return false;
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

	private void buildEvtCategories() {
		evtCategories = new ArrayList<Category>();
		int categoryIdStart = AppConstants.CATEGORY_ID_START;
		String[] categoryNames = new String[] { "Concerts", "Theater",
				"Sport Events", "Arts & Museums", "Dance",
				"Clubbing & Nightlife", "Educational", "Festivals & Fairs",
				"Family", "Community", "Business & Tech", "Tours" };
		for (int i = 0; i < AppConstants.TOTAL_CATEGORIES; i++) {
			evtCategories.add(new Category(categoryIdStart + i,
					categoryNames[i]));
		}
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

	private class LoadFeaturedEvts extends AsyncTask<Void, Void, List<Event>> {

		@Override
		protected List<Event> doInBackground(Void... params) {
			List<Event> events = new ArrayList<Event>();
			EventApi eventApi = new EventApi(Api.OAUTH_TOKEN, lat, lon);
			eventApi.setLimit(FEATURED_EVTS_LIMIT);
			try {
				JSONObject jsonObject = eventApi.getFeaturedEvents();
				EventApiJSONParser jsonParser = new EventApiJSONParser();
				events = jsonParser.getFeaturedEventList(jsonObject);

			} catch (ClientProtocolException e) {
				e.printStackTrace();

			} catch (IOException e) {
				e.printStackTrace();

			} catch (JSONException e) {
				e.printStackTrace();
			}

			return events;
		}

		@Override
		protected void onPostExecute(List<Event> result) {
			featuredEvts = result;
			featuredEventsFragmentPagerAdapter.notifyDataSetChanged();
			// Log.d(TAG, "featured events updated");
		}
	}

	private class EvtCategoriesListAdapter extends BaseAdapter {

		private final HashMap<Integer, Integer> categoryImgs = new HashMap<Integer, Integer>() {
			{
				put(900, R.drawable.cat_900);
				put(901, R.drawable.cat_901);
				put(902, R.drawable.cat_902);
				put(903, R.drawable.cat_903);
				put(904, R.drawable.cat_904);
				put(905, R.drawable.cat_905);
				put(906, R.drawable.cat_906);
				put(907, R.drawable.cat_907);
				put(908, R.drawable.cat_908);
				put(909, R.drawable.cat_909);
				put(910, R.drawable.cat_910);
				put(911, R.drawable.cat_911);
			}
		};

		@Override
		public int getCount() {
			return evtCategories.size();
		}

		@Override
		public Object getItem(int position) {
			return evtCategories.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(final int position, View convertView,
				ViewGroup parent) {
			GridItemCategoryHolder holder;
			if (convertView == null) {
				convertView = LayoutInflater.from(
						FragmentUtil.getActivity(DiscoverFragment.this))
						.inflate(R.layout.grid_category, null);

				holder = new GridItemCategoryHolder();
				holder.imgCategory = (ImageView) convertView
						.findViewById(R.id.imgCategory);
				holder.txtCategory = (TextView) convertView
						.findViewById(R.id.txtCategory);

				convertView.setTag(holder);

			} else {
				holder = (GridItemCategoryHolder) convertView.getTag();
			}

			holder.txtCategory.setText(evtCategories.get(position).getName());
			int catId = evtCategories.get(position).getId();
			Drawable drawable = (categoryImgs.containsKey(catId)) ? getResources()
					.getDrawable(categoryImgs.get(catId)) : null;
			holder.imgCategory.setImageDrawable(drawable);
			convertView.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Bundle args = new Bundle();
					args.putInt(BundleKeys.CATEGORY_POSITION, position);
					args.putSerializable(BundleKeys.CATEGORIES,
							(ArrayList<Category>) evtCategories);
					mListener.replaceSelfByFragment(
							AppConstants.FRAGMENT_TAG_DISCOVER_BY_CATEGORY,
							args);
				}
			});
			return convertView;
		}

		private class GridItemCategoryHolder {
			private TextView txtCategory;
			private ImageView imgCategory;
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(BundleKeys.EVT_CATEGORIES,
				(Serializable) evtCategories);
		outState.putSerializable(BundleKeys.FEATURED_EVTS,
				(Serializable) featuredEvts);
		outState.putDouble(BundleKeys.LAT, lat);
		outState.putDouble(BundleKeys.LON, lon);
	}

	@Override
	public void onPageScrollStateChanged(int arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPageSelected(int arg0) {
		// Log.d(TAG, "onPageSelected() - " + arg0);
		viewPagerSelectedPos = arg0;
	}

}
