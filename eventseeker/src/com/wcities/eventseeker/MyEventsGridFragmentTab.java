package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.List;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.facebook.Session;
import com.facebook.SessionState;
import com.wcities.eventseeker.adapter.RVMyEventsAdapterTab;
import com.wcities.eventseeker.adapter.RVMyEventsAdapterTab.RVMyEventsAdapterTabListener;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi.Type;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.LoadMyEvents;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.custom.fragment.PublishEventFragment;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.ItemDecorationItemOffset;

public class MyEventsGridFragmentTab extends PublishEventFragment implements LoadItemsInBackgroundListener, 
		AsyncTaskListener<Void>, RVMyEventsAdapterTabListener {
	
	private static final String TAG = MyEventsGridFragmentTab.class.getSimpleName();
	
	private static final int GRID_COLUMNS_PORTRAIT = 2;
	private static final int GRID_COLUMNS_LANDSCAPE = 3;

	private LoadMyEvents loadEvents;
	private List<Event> eventList;
	private double lat, lon;
	
	private RecyclerView recyclerVEvents;
	private LinearLayoutManager layoutManager;
	private RelativeLayout rltLytProgressBar, rltLytNoEvts;
	
	private RVMyEventsAdapterTab rvCatEventsAdapterTab;
	
	private Handler handler;

	private Type loadType;

	private String wcitiesId;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (wcitiesId == null) {
			wcitiesId = ((EventSeekr)FragmentUtil.getActivity(this).getApplication()).getWcitiesId();
		}
		handler = new Handler(Looper.getMainLooper());
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		//Log.d(TAG, "onCreateView()");		
		View v = inflater.inflate(R.layout.fragment_my_events_tab, container, false);
		
		// use a linear layout manager
		layoutManager = new LinearLayoutManager(FragmentUtil.getActivity(this));
		layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
		
		recyclerVEvents = (RecyclerView) v.findViewById(R.id.recyclerVEvents);
		int spanCount = (FragmentUtil.getResources(this).getConfiguration().orientation == 
				Configuration.ORIENTATION_PORTRAIT) ? GRID_COLUMNS_PORTRAIT : GRID_COLUMNS_LANDSCAPE;
		GridLayoutManager gridLayoutManager = new GridLayoutManager(FragmentUtil.getActivity(this), spanCount);
		recyclerVEvents.setHasFixedSize(true);
		recyclerVEvents.setLayoutManager(gridLayoutManager);
		
		rltLytProgressBar = (RelativeLayout) v.findViewById(R.id.rltLytProgressBar);
		// Applying background here since overriding background doesn't work from xml with <include> layout
		rltLytProgressBar.setBackgroundResource(R.drawable.bg_no_content_overlay_tab);
		
		rltLytNoEvts = (RelativeLayout) v.findViewById(R.id.rltLytNoEvts);
		if (eventList != null && eventList.isEmpty()) {
			// retain no events layout visibility on orientation change
			rltLytNoEvts.setVisibility(View.VISIBLE);
		}
		
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if (eventList == null) {
			Bundle args = getArguments();
			loadType = (Type) args.getSerializable(BundleKeys.LOAD_TYPE);
			
			double[] latLon = DeviceUtil.getLatLon(FragmentUtil.getApplication(this));
			lat = latLon[0];
			lon = latLon[1];
			
			eventList = new ArrayList<Event>();
			eventList.add(null);
			
			rvCatEventsAdapterTab = new RVMyEventsAdapterTab(eventList, null, this, this, this);
			
		} else {
			// to update values which should change on orientation change
			rvCatEventsAdapterTab.onActivityCreated();
		}
		
		Resources res = FragmentUtil.getResources(this);
		recyclerVEvents.addItemDecoration(new ItemDecorationItemOffset(res.getDimensionPixelSize(
				R.dimen.rv_item_l_r_offset_discover_tab), res.getDimensionPixelSize(R.dimen.rv_item_t_b_offset_discover_tab)));
		recyclerVEvents.setAdapter(rvCatEventsAdapterTab);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (loadEvents != null && loadEvents.getStatus() != Status.FINISHED) {
			loadEvents.cancel(true);
		}
	}
	
	private void resetEventList() {
		if (rvCatEventsAdapterTab == null) {
			return;
		}
		//Log.d(TAG, "resetEventList()");
		rvCatEventsAdapterTab.reset();

		if (loadEvents != null) {
			loadEvents.cancel(true);
		}

		eventList.clear();
		eventList.add(null);
		
		rvCatEventsAdapterTab.notifyDataSetChanged();
		/**
		 * Although we expect rvCatEventsAdapterTab to call loadItemsInBackground() due to 1st null item in 
		 * eventList, it won't work always. For eg - If user changes categories fast one by one then loadDateWiseEvents
		 * in rvCatEventsAdapterTab won't be null & also it won't be in FINISHED state. Hence it won't call
		 * loadItemsInBackground() for latest category selection in this case, so better we call loadItemsInBackground()
		 * from here itself.
		 */
		loadItemsInBackground();
	}
		
	@Override
	public void setEvent(Event event) {
		this.event = event;
	}
	
	public void setCenterProgressBarVisibility(int visibility) {
		rltLytProgressBar.setVisibility(visibility);
		
		if (visibility == View.VISIBLE) {
			rltLytNoEvts.setVisibility(View.INVISIBLE);
		}
	}
	
	@Override
	public void loadItemsInBackground() {
		loadEvents = new LoadMyEvents(Api.OAUTH_TOKEN, eventList, rvCatEventsAdapterTab, wcitiesId, loadType, 
				lat, lon, this);
		rvCatEventsAdapterTab.setLoadDateWiseEvents(loadEvents);
        AsyncTaskUtil.executeAsyncTask(loadEvents, true);
	}

	@Override
	public void onTaskCompleted(Void... params) {
		handler.post(new Runnable() {
			
			@Override
			public void run() {
				//Log.d(TAG, "onEventsLoaded()");
				// to remove full screen progressbar
				setCenterProgressBarVisibility(View.INVISIBLE);
				if (eventList.isEmpty()) {
					rltLytNoEvts.setVisibility(View.VISIBLE);
				}
			}
		});
	}

	@Override
	public void onPublishPermissionGranted() {
		rvCatEventsAdapterTab.onPublishPermissionGranted();
	}

	@Override
	public void call(Session session, SessionState state, Exception exception) {
		rvCatEventsAdapterTab.call(session, state, exception);
	}
	
	public void onEventAttendingUpdated() {
		/**
		 * we will get this notification only while saving events. If user is saving event from following/recommended
		 * tab then saved events should refresh to display updated list
		 */
		if (loadType == Type.mysavedevents) {
			resetEventList();
		}
	}
}
