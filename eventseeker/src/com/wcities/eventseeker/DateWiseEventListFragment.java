package com.wcities.eventseeker;

import java.util.Calendar;
import java.util.List;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;

import com.wcities.eventseeker.adapter.DateWiseEventListAdapter;
import com.wcities.eventseeker.adapter.DateWiseEventListAdapter.DateWiseEventListAdapterListener;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.LoadDateWiseEvents;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Category;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.DateWiseEventList;

public class DateWiseEventListFragment extends ListFragment implements DateWiseEventListAdapterListener {
	
	private static final String TAG = DateWiseEventListFragment.class.getName();
	
	private LoadDateWiseEvents loadEvents;
	private DateWiseEventListAdapter eventListAdapter;
	
	private String wcitiesId;
	private double lat, lon;
	
	private int categoryPosition;
	private String startDate;
	private List<Category> categories;
	
	private DateWiseEventList dateWiseEvtList;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/**
		 * Retain instance state only if this fragment has activity as its parent & not any other fragment as parent.
		 * Because we can't retain fragments that are nested in other fragments. 
		 */
		if (getParentFragment() == null) {
			setRetainInstance(true);
		}
		
		if (wcitiesId == null) {
			wcitiesId = ((EventSeekr)FragmentUtil.getActivity(this).getApplication()).getWcitiesId();
		}
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		Log.d(TAG, "onActivityCreated()");
		super.onActivityCreated(savedInstanceState);
		
		if (dateWiseEvtList == null) {
			if (getArguments() != null) {
				// Bundle has these values when this fragment is called from DiscoverByCategoryActivity.
				Bundle bundle = getArguments();
				categories = (List<Category>) bundle.get(BundleKeys.CATEGORIES);
				categoryPosition = bundle.getInt(BundleKeys.CATEGORY_POSITION);
			}
			
			double[] latLon = DeviceUtil.getLatLon(FragmentUtil.getActivity(this));
			lat = latLon[0];
			lon = latLon[1];
			
			Calendar c = Calendar.getInstance();
			int year = c.get(Calendar.YEAR);
			int month = c.get(Calendar.MONTH);
			int day = c.get(Calendar.DAY_OF_MONTH);
			startDate = ConversionUtil.getDay(year, month, day);
			
			dateWiseEvtList = new DateWiseEventList();
			dateWiseEvtList.addDummyItem();
			
	        eventListAdapter = new DateWiseEventListAdapter(FragmentUtil.getActivity(this), dateWiseEvtList, null, this);

			loadEventsInBackground();
			
		} else {
			eventListAdapter.updateContext(FragmentUtil.getActivity(this));
		}

		setListAdapter(eventListAdapter);
        getListView().setDivider(null);
        getListView().setBackgroundResource(R.drawable.story_space);
	}
	
	@Override
	public void loadEventsInBackground() {
		loadEvents = new LoadDateWiseEvents(dateWiseEvtList, eventListAdapter, lat, lon, startDate, 
				categories.get(categoryPosition).getId());
		eventListAdapter.setLoadDateWiseEvents(loadEvents);
        loadEvents.execute();
	}
	
	protected void resetWith(String newStartDate) {
		// if user selection has changed then only reset the list
		if (!startDate.equals(newStartDate)) {
			startDate = newStartDate;
			reset();
		}
	}
	
	private void reset() {
		eventListAdapter.setEventsAlreadyRequested(0);
		eventListAdapter.setMoreDataAvailable(true);
		
		loadEvents.cancel(true);
		
		dateWiseEvtList.reset();
		eventListAdapter.notifyDataSetChanged();
		
		loadEventsInBackground();
	}
}
