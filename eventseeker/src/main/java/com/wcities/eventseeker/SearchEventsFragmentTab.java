package com.wcities.eventseeker;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.wcities.eventseeker.adapter.RVSearchEventsAdapterTab;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.LoadEvents;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.custom.fragment.PublishEventFragment;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.interfaces.FullScrnProgressListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.interfaces.SearchFragmentChildListener;
import com.wcities.eventseeker.interfaces.SwipeTabVisibilityListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.ItemDecorationItemOffset;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class SearchEventsFragmentTab extends PublishEventFragment implements SearchFragmentChildListener, FullScrnProgressListener, LoadItemsInBackgroundListener, 
		AsyncTaskListener<Void>, SwipeTabVisibilityListener {
	
	private static final String TAG = SearchEventsFragmentTab.class.getSimpleName();

	private static final int GRID_COLS_PORTRAIT = 2;
	private static final int GRID_COLS_LANDSCAPE = 3;
	private static final int MILES_LIMIT = 10000;
	
	private RecyclerView recyclerVEvents;
	private RelativeLayout rltLytProgressBar;
	private TextView txtNoItemsFound;
	private ImageView imgPrgOverlay;
	
	private RVSearchEventsAdapterTab rvSearchEventsAdapterTab;
	
	private double[] latLon;
	private List<Event> eventList;
	private String query;
	private LoadEvents loadEvents;
	
	private Handler handler;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		handler = new Handler(Looper.getMainLooper());
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_search_items_tab, null);
		recyclerVEvents = (RecyclerView) v.findViewById(R.id.recyclerVItems);
		int spanCount = (FragmentUtil.getResources(this).getConfiguration().orientation == 
				Configuration.ORIENTATION_PORTRAIT) ? GRID_COLS_PORTRAIT : GRID_COLS_LANDSCAPE;
		GridLayoutManager gridLayoutManager = new GridLayoutManager(FragmentUtil.getActivity(this), spanCount);
		recyclerVEvents.setHasFixedSize(true);
		recyclerVEvents.setLayoutManager(gridLayoutManager);
		
		rltLytProgressBar = (RelativeLayout) v.findViewById(R.id.rltLytProgressBar);
		// Applying background here since overriding background doesn't work from xml with <include> layout
		imgPrgOverlay = (ImageView) rltLytProgressBar.findViewById(R.id.imgPrgOverlay);
		
		txtNoItemsFound = (TextView) v.findViewById(R.id.txtNoItemsFound);
		
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (eventList == null) {
			eventList = new ArrayList<Event>();
			
			Bundle args = getArguments();
			if (args != null && args.containsKey(BundleKeys.QUERY)) {
				eventList.add(null);
				query = args.getString(BundleKeys.QUERY);
			}
			
			rvSearchEventsAdapterTab = new RVSearchEventsAdapterTab(this);
			
		} else {
			rvSearchEventsAdapterTab.onActivityCreated();
		}
		
		Resources res = FragmentUtil.getResources(this);
		recyclerVEvents.addItemDecoration(new ItemDecorationItemOffset(res.getDimensionPixelSize(
				R.dimen.rv_item_l_r_offset_search_items_tab), res.getDimensionPixelSize(R.dimen.rv_item_t_b_offset_search_items_tab)));
		recyclerVEvents.setAdapter(rvSearchEventsAdapterTab);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (loadEvents != null && loadEvents.getStatus() != Status.FINISHED) {
			loadEvents.cancel(true);
		}
	}
	
	public List<Event> getEventList() {
		return eventList;
	}
	
	public void setEvent(Event event) {
		this.event = event;
	}
	
	public void displayNoItemsFound() {
		txtNoItemsFound.setText(R.string.no_event_found);
		txtNoItemsFound.setVisibility(View.VISIBLE);
	}
	
	private void refresh(String newQuery) {
		//Log.d(TAG, "refresh()");
		// if user selection has changed then only reset the list
		if (query == null || !query.equals(newQuery)) {
			//Log.d(TAG, "query == null || !query.equals(newQuery)");

			query = newQuery;
			rvSearchEventsAdapterTab.reset();
			
			if (loadEvents != null && loadEvents.getStatus() != Status.FINISHED) {
				loadEvents.cancel(true);
			}
			
			txtNoItemsFound.setVisibility(View.INVISIBLE);
			
			eventList.clear();
			eventList.add(null);
			rvSearchEventsAdapterTab.notifyDataSetChanged();
			
			loadItemsInBackground();
		}
	}

	@Override
	public void displayFullScrnProgress() {
		/**
		 * Since we are using the transparent Progressbar layout.So, we need to set background white, else
		 * in portrait mode in 10" devices the background will get visible.
		 */
		handler.post(new Runnable() {

			@Override
			public void run() {
				rltLytProgressBar.setBackgroundColor(Color.WHITE);
				rltLytProgressBar.setVisibility(View.VISIBLE);
				imgPrgOverlay.setVisibility(View.VISIBLE);
			}
		});
	}
	
	@Override
	public void loadItemsInBackground() {
		if (latLon == null) {
			latLon = DeviceUtil.getLatLon(FragmentUtil.getApplication(this));
		}
		
		Calendar c = Calendar.getInstance();
		String startDate = ConversionUtil.getDay(c);
		c.add(Calendar.YEAR, 1);
		String endDate = ConversionUtil.getDay(c);
		
		loadEvents = new LoadEvents(Api.OAUTH_TOKEN, eventList, rvSearchEventsAdapterTab, query,
				latLon[0], latLon[1], MILES_LIMIT, ((EventSeekr)FragmentUtil.getActivity(this).getApplication()).getWcitiesId(), 
				startDate, endDate, this);
		rvSearchEventsAdapterTab.setLoadDateWiseEvents(loadEvents);
		AsyncTaskUtil.executeAsyncTask(loadEvents, true);
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
				// free up memory
				imgPrgOverlay.setBackgroundResource(0);
				imgPrgOverlay.setVisibility(View.GONE);
				rltLytProgressBar.setVisibility(View.GONE);
			}
		});
	}

	@Override
	public void onPublishPermissionGranted() {
		rvSearchEventsAdapterTab.onPublishPermissionGranted();
	}

	@Override
	public void onSuccess(LoginResult loginResult) {
		Log.d(TAG, "onSuccess()");
		rvSearchEventsAdapterTab.onSuccess(loginResult);
	}

	@Override
	public void onCancel() {
		Log.d(TAG, "onCancel()");
	}

	@Override
	public void onError(FacebookException e) {
		Log.d(TAG, "onError()");
	}

	@Override
	public void onInvisible() {
		if (rvSearchEventsAdapterTab != null) {
			rvSearchEventsAdapterTab.setVisible(false);
			rvSearchEventsAdapterTab.notifyDataSetChanged();
		}
	}

	@Override
	public void onVisible() {
		if (rvSearchEventsAdapterTab != null) {
			rvSearchEventsAdapterTab.setVisible(true);
			/**
			 * need to call this because it doesn't call onBindViewHolder() automatically if 
			 * next or previous tab is selected. Calls it only for tab selection changing from position 1 to 3 or 
			 * 3 to 1
			 */
			rvSearchEventsAdapterTab.notifyDataSetChanged();
		}
	}
}
