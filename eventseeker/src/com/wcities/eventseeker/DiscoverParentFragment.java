package com.wcities.eventseeker;

import java.io.IOException;
import java.io.Serializable;
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

import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.EventApi;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Category;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.custom.view.ExpandableGridView;
import com.wcities.eventseeker.interfaces.ReplaceFragmentListener;
import com.wcities.eventseeker.jsonparser.EventApiJSONParser;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public abstract class DiscoverParentFragment extends
		FragmentLoadableFromBackStack {

	public static final String TAG = DiscoverParentFragment.class.getName();

	protected ReplaceFragmentListener mListener;
	// private TextView txtFeaturedEvtsTitle;
	// private ImageView imgPrev, imgNext;

	protected EvtCategoriesListAdapter evtCategoriesListAdapter;

	protected LoadFeaturedEvts loadFeaturedEvts;
	protected List<Category> evtCategories;
	protected List<Event> featuredEvts;

	protected double lat, lon;

	protected static final int FEATURED_EVTS_LIMIT = 5;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		// Log.d(TAG, "onAttach");
		try {
			mListener = (ReplaceFragmentListener) activity;

		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement ReplaceFragmentListener");
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

		View v = inflater.inflate(R.layout.fragment_discover, container, false);

		// txtFeaturedEvtsTitle = (TextView)
		// v.findViewById(R.id.txtFeaturedEvtsTitle);
		// updateCityName();

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
			notifyDataSetChanged();
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
						FragmentUtil.getActivity(DiscoverParentFragment.this))
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
					mListener.replaceByFragment(
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

	protected abstract void notifyDataSetChanged();

}
