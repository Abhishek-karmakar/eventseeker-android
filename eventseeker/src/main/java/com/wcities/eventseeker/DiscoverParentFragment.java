package com.wcities.eventseeker;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
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
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.LoadFeaturedEvts;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Category;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.ReplaceFragmentListener;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class DiscoverParentFragment extends FragmentLoadableFromBackStack {

	public static final String TAG = DiscoverParentFragment.class.getName();

	protected ReplaceFragmentListener mListener;
	private ProgressBar progressBar;
	private boolean featuredEventsLoaded;

	protected EvtCategoriesListAdapter evtCategoriesListAdapter;

	protected LoadFeaturedEvts loadFeaturedEvts;
	protected List<Category> evtCategories;
	protected List<Event> featuredEvts;

	protected double lat, lon;

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
		//Log.d(TAG, "onCreate");
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

		GridView grdEvtCategories = (GridView) v.findViewById(R.id.grdEvtCategories);
		if(((EventSeekr)FragmentUtil.getActivity(this).getApplicationContext()).isTabletAndInPortraitMode()) {
			grdEvtCategories.setNumColumns(DEFAULT_NUM_OF_COLUMNS_FOR_TABLET_IN_PORTRAIT_MODE);
		}

		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT
				|| (((EventSeekr) FragmentUtil.getActivity(this).getApplicationContext()).isTablet())) {
			//((ExpandableGridView) grdEvtCategories).setExpanded(true);
		}

		grdEvtCategories.setAdapter(evtCategoriesListAdapter);
		
		if (featuredEvts.isEmpty()) {
			/**
			 * 12-06-2014 : added wcitiesId in Featured event call as per Rohit/Sameer's mail
			 */
			String wcitiesId = FragmentUtil.getApplication(this).getWcitiesId();
			loadFeaturedEvts = new LoadFeaturedEvts(Api.OAUTH_TOKEN, lat, lon, wcitiesId) {
				
				@Override
				protected void onPreExecute() {
					super.onPreExecute();
					featuredEventsLoaded = false;
					progressBar.setVisibility(View.VISIBLE);
				}
				
				@Override
				protected void onPostExecute(List<Event> result) {
					super.onPostExecute(result);
					featuredEvts = result;
					featuredEventsLoaded = true;
					progressBar.setVisibility(View.GONE);
					notifyDataSetChanged();
				}
			};
			loadFeaturedEvts.execute();
		}

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
		double[] latLon = DeviceUtil.getLatLon((EventSeekr) ((Activity) mListener).getApplication());
		lat = latLon[0];
		lon = latLon[1];
	}

	private void buildEvtCategories() {
		evtCategories = new ArrayList<Category>();
		int categoryIdStart = AppConstants.CATEGORY_ID_START;
		String[] categoryNames = getResources().getStringArray(R.array.evt_category_titles);
		for (int i = 0; i < AppConstants.TOTAL_CATEGORIES; i++) {
			evtCategories.add(new Category(categoryIdStart + i,
					categoryNames[i]));
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

	@Override
	public String getScreenName() {
		return "Discover Screen";
	}
}
