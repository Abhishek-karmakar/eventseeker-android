package com.wcities.eventseeker;

import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.Session;
import com.facebook.SessionState;
import com.wcities.eventseeker.SearchFragment.SearchFragmentChildListener;
import com.wcities.eventseeker.adapter.DateWiseMyEventListAdapter;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.LoadDateWiseEvents;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.interfaces.FbPublishListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.DateWiseEventList;

public class SearchEventsFragmentTab extends SearchEventsParentFragment implements 
		SearchFragmentChildListener, LoadItemsInBackgroundListener, FbPublishListener {

	private static final String TAG = SearchEventsFragmentTab.class.getName();

	private DateWiseMyEventListAdapter eventListAdapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (eventList == null) {
			eventList = new DateWiseEventList();
			eventListAdapter = new DateWiseMyEventListAdapter(FragmentUtil.getActivity(this),  
	        		eventList, null, this, this);

			Bundle args = getArguments();
			if (args != null && args.containsKey(BundleKeys.QUERY)) {
				eventList.addDummyItem();
				query = args.getString(BundleKeys.QUERY);
				loadItemsInBackground();
			}

		} else {
			eventListAdapter.updateContext(FragmentUtil.getActivity(this));
		}

		setListAdapter(eventListAdapter);
	}
	
	@Override
	public void loadItemsInBackground() {
		double[] latLon = DeviceUtil.getLatLon(FragmentUtil.getActivity(this));
		loadEvents = new LoadDateWiseEvents(eventList, eventListAdapter, query,	latLon[0], latLon[1], MILES_LIMIT, 
				((EventSeekr)FragmentUtil.getActivity(this).getApplicationContext()).getWcitiesId());
		
		eventListAdapter.setLoadDateWiseEvents(loadEvents);
		AsyncTaskUtil.executeAsyncTask(loadEvents, true);
	}

	private void refresh(String newQuery) {
		// if user selection has changed then only reset the list
		if (query == null || !query.equals(newQuery)) {
			query = newQuery;
			eventListAdapter.setEventsAlreadyRequested(0);
			eventListAdapter.setMoreDataAvailable(true);

			if (loadEvents != null && loadEvents.getStatus() != Status.FINISHED) {
				loadEvents.cancel(true);
			}

			eventList.reset();
			eventListAdapter.notifyDataSetChanged();

			loadItemsInBackground();
		}
	}

	@Override
	public void onQueryTextSubmit(String query) {
		refresh(query);
	}
	
	@Override
	public void call(Session session, SessionState state, Exception exception) {
		eventListAdapter.call(session, state, exception);
	}

	@Override
	public void onPublishPermissionGranted() {
		eventListAdapter.onPublishPermissionGranted();
	}
}
