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
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
	private ProgressBar progressBar;
	private boolean featuredEventsLoaded;

	protected EvtCategoriesListAdapter evtCategoriesListAdapter;

	protected LoadFeaturedEvts loadFeaturedEvts;
	protected List<Category> evtCategories;
	protected List<Event> featuredEvts;

	protected double lat, lon;

	protected static final int FEATURED_EVTS_LIMIT = 5;

	private static final int DEFAULT_NUM_OF_COLUMNS_FOR_TABLET_IN_PORTRAIT_MODE = 3;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		// Log.d(TAG, "onAttach");
		try {
			mListener = (ReplaceFragmentListener) activity;

		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement ReplaceFragmentListener");
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
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		if (evtCategories == null) {
			buildEvtCategories();
			featuredEvts = new ArrayList<Event>();
			generateLatLon();
			evtCategoriesListAdapter = new EvtCategoriesListAdapter();
		}

		View v = inflater.inflate(R.layout.fragment_discover, container, false);
		progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
		int progressVisibility = featuredEventsLoaded ? View.GONE : View.VISIBLE;
		progressBar.setVisibility(progressVisibility);

		// txtFeaturedEvtsTitle = (TextView)
		// v.findViewById(R.id.txtFeaturedEvtsTitle);
		// updateCityName();

		GridView grdEvtCategories = (GridView) v.findViewById(R.id.grdEvtCategories);
		if(((EventSeekr)FragmentUtil.getActivity(this).getApplicationContext()).isTabletAndInPortraitMode()) {
			grdEvtCategories.setNumColumns(DEFAULT_NUM_OF_COLUMNS_FOR_TABLET_IN_PORTRAIT_MODE);
		}

		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT
				|| (((EventSeekr) FragmentUtil.getActivity(this).getApplicationContext()).isTablet())) {
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
		if (loadFeaturedEvts != null && loadFeaturedEvts.getStatus() != Status.FINISHED) {
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
		protected void onPreExecute() {
			super.onPreExecute();
			featuredEventsLoaded = false;
			progressBar.setVisibility(View.VISIBLE);
		}

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
			//Log.i(TAG, "onPostExecute");
			featuredEventsLoaded = true;
			progressBar.setVisibility(View.GONE);
			notifyDataSetChanged();
		}
	}

	private class EvtCategoriesListAdapter extends BaseAdapter {

		private final HashMap<Integer, Integer> categoryImgs = new HashMap<Integer, Integer>() {
			{
				put(AppConstants.CATEGORY_ID_START, R.drawable.cat_900);
				put(AppConstants.CATEGORY_ID_START + 1, R.drawable.cat_901);
				put(AppConstants.CATEGORY_ID_START + 2, R.drawable.cat_902);
				put(AppConstants.CATEGORY_ID_START + 3, R.drawable.cat_903);
				put(AppConstants.CATEGORY_ID_START + 4, R.drawable.cat_904);
				put(AppConstants.CATEGORY_ID_START + 5, R.drawable.cat_905);
				put(AppConstants.CATEGORY_ID_START + 6, R.drawable.cat_906);
				put(AppConstants.CATEGORY_ID_START + 7, R.drawable.cat_907);
				put(AppConstants.CATEGORY_ID_START + 8, R.drawable.cat_908);
				put(AppConstants.CATEGORY_ID_START + 9, R.drawable.cat_909);
				put(AppConstants.CATEGORY_ID_START + 10, R.drawable.cat_910);
				put(AppConstants.CATEGORY_ID_START + 11, R.drawable.cat_911);
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
				convertView = LayoutInflater.from(FragmentUtil.getActivity(DiscoverParentFragment.this))
						.inflate(R.layout.grid_category, null);

				holder = new GridItemCategoryHolder();
				holder.imgCategory = (ImageView) convertView.findViewById(R.id.imgCategory);
				holder.txtCategory = (TextView) convertView.findViewById(R.id.txtCategory);

				convertView.setTag(holder);

			} else {
				holder = (GridItemCategoryHolder) convertView.getTag();
			}

			holder.txtCategory.setText(evtCategories.get(position).getName());
			int catId = evtCategories.get(position).getId();
			Drawable drawable = (categoryImgs.containsKey(catId)) ? getResources().getDrawable(categoryImgs.get(catId)) : null;
			holder.imgCategory.setImageDrawable(drawable);
			convertView.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Bundle args = new Bundle();
					args.putInt(BundleKeys.CATEGORY_POSITION, position);
					args.putSerializable(BundleKeys.CATEGORIES, (ArrayList<Category>) evtCategories);
					mListener.replaceByFragment( AppConstants.FRAGMENT_TAG_DISCOVER_BY_CATEGORY, args);
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
		outState.putSerializable(BundleKeys.EVT_CATEGORIES, (Serializable) evtCategories);
		outState.putSerializable(BundleKeys.FEATURED_EVTS, (Serializable) featuredEvts);
		outState.putDouble(BundleKeys.LAT, lat);
		outState.putDouble(BundleKeys.LON, lon);
	}

	protected abstract void notifyDataSetChanged();

}
