package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.List;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wcities.eventseeker.adapter.RVSearchVenuesAdapterTab;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.asynctask.LoadVenues;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.interfaces.FullScrnProgressListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.interfaces.SearchFragmentChildListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.ItemDecorationItemOffset;

public class SearchVenuesFragmentTab extends Fragment implements SearchFragmentChildListener, LoadItemsInBackgroundListener, 
		AsyncTaskListener<Void>, FullScrnProgressListener {

	private static final String TAG = SearchVenuesFragmentTab.class.getSimpleName();
	
	private static final int GRID_COLS_PORTRAIT = 2;
	private static final int GRID_COLS_LANDSCAPE = 3;
	
	private RecyclerView recyclerVVenues;
	private RelativeLayout rltLytProgressBar;
	private TextView txtNoItemsFound;
	
	private RVSearchVenuesAdapterTab<String> rvSearchVenuesAdapterTab;
	
	private double[] latLng;
	private List<Venue> venueList;
	private String query;
	private LoadVenues loadVenues;
	
	private Handler handler;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		handler = new Handler(Looper.getMainLooper());
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_search_items_tab, null);
		
		recyclerVVenues = (RecyclerView) v.findViewById(R.id.recyclerVItems);
		int spanCount = (FragmentUtil.getResources(this).getConfiguration().orientation == 
				Configuration.ORIENTATION_PORTRAIT) ? GRID_COLS_PORTRAIT : GRID_COLS_LANDSCAPE;
		GridLayoutManager gridLayoutManager = new GridLayoutManager(FragmentUtil.getActivity(this), spanCount);
		recyclerVVenues.setHasFixedSize(true);
		recyclerVVenues.setLayoutManager(gridLayoutManager);
		
		rltLytProgressBar = (RelativeLayout) v.findViewById(R.id.rltLytProgressBar);
		// Applying background here since overriding background doesn't work from xml with <include> layout
		rltLytProgressBar.setBackgroundResource(R.drawable.ic_no_content_background_overlay);
		
		txtNoItemsFound = (TextView) v.findViewById(R.id.txtNoItemsFound);
		
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if (venueList == null) {
			venueList = new ArrayList<Venue>();
			
			Bundle args = getArguments();
			if (args != null && args.containsKey(BundleKeys.QUERY)) {
				venueList.add(null);
				query = args.getString(BundleKeys.QUERY);
			}
			
			rvSearchVenuesAdapterTab = new RVSearchVenuesAdapterTab<String>(this);
			
		} else {
			rvSearchVenuesAdapterTab.onActivityCreated();
		}
		
		Resources res = FragmentUtil.getResources(this);
		recyclerVVenues.addItemDecoration(new ItemDecorationItemOffset(res.getDimensionPixelSize(
				R.dimen.rv_item_l_r_offset_search_items_tab), res.getDimensionPixelSize(R.dimen.rv_item_t_b_offset_search_items_tab)));
		recyclerVVenues.setAdapter(rvSearchVenuesAdapterTab);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		//Log.d(TAG, "onDestroy()");
		if (loadVenues != null && loadVenues.getStatus() != Status.FINISHED) {
			loadVenues.cancel(true);
		}
	}
	
	public List<Venue> getVenueList() {
		return venueList;
	}
	
	public void displayNoItemsFound() {
		txtNoItemsFound.setText(R.string.no_venue_found);
		txtNoItemsFound.setVisibility(View.VISIBLE);
	}
	
	private void refresh(String newQuery) {
		//Log.d(TAG, "refresh()");
		// if user selection has changed then only reset the list
		if (query == null || !query.equals(newQuery)) {
			//Log.d(TAG, "query == null || !query.equals(newQuery)");

			query = newQuery;
			rvSearchVenuesAdapterTab.reset();
			
			if (loadVenues != null && loadVenues.getStatus() != Status.FINISHED) {
				loadVenues.cancel(true);
			}
			
			txtNoItemsFound.setVisibility(View.INVISIBLE);
			
			venueList.clear();
			venueList.add(null);
			rvSearchVenuesAdapterTab.notifyDataSetChanged();
			
			loadItemsInBackground();
		}
	}
	
	@Override
	public void displayFullScrnProgress() {
		rltLytProgressBar.setVisibility(View.VISIBLE);
	}

	@Override
	public void loadItemsInBackground() {
		if (latLng == null) {
			latLng = DeviceUtil.getLatLon(FragmentUtil.getApplication(this));
		}
		loadVenues = new LoadVenues(Api.OAUTH_TOKEN, rvSearchVenuesAdapterTab, venueList, latLng, this);
		rvSearchVenuesAdapterTab.setLoadVenues(loadVenues);
        AsyncTaskUtil.executeAsyncTask(loadVenues, true, query);
	}
	
	@Override
	public void onQueryTextSubmit(String query) {
		refresh(query);
	}

	@Override
	public void onTaskCompleted(Void... params) {
		handler.post(new Runnable() {
			
			@Override
			public void run() {
				//Log.d(TAG, "onEventsLoaded()");
				// remove full screen progressbar
				rltLytProgressBar.setVisibility(View.INVISIBLE);
			}
		});
	}
}
