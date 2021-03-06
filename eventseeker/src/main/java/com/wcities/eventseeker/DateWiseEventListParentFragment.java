package com.wcities.eventseeker;

import android.os.Bundle;
import android.widget.BaseAdapter;

import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.LoadDateWiseEvents;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Category;
import com.wcities.eventseeker.custom.fragment.PublishEventListFragment;
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.DateWiseEventList;

import java.util.Calendar;
import java.util.List;

public abstract class DateWiseEventListParentFragment extends PublishEventListFragment 
		implements LoadItemsInBackgroundListener {

	private static final String TAG = DateWiseEventListParentFragment.class.getName();

	protected LoadDateWiseEvents loadEvents;

	protected int categoryPosition;
	protected String startDate, endDate;
	protected double lat, lon;
	protected List<Category> categories;

	protected DateWiseEventList dateWiseEvtList;

	protected DateWiseEventParentAdapterListener eventListAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/**
		 * Retain instance state only if this fragment has activity as its
		 * parent & not any other fragment as parent. Because we can't retain
		 * fragments that are nested in other fragments.
		 */
		if (getParentFragment() == null) {
			setRetainInstance(true);
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (dateWiseEvtList == null) {
			if (getArguments() != null) {
				// Bundle has these values when this fragment is called from
				// DiscoverByCategoryFragment.
				Bundle bundle = getArguments();
				categories = (List<Category>) bundle.get(BundleKeys.CATEGORIES);
				categoryPosition = bundle.getInt(BundleKeys.CATEGORY_POSITION);
			}

			double[] latLon = DeviceUtil.getLatLon(FragmentUtil
					.getApplication(this));
			lat = latLon[0];
			lon = latLon[1];

			Calendar c = Calendar.getInstance();
			startDate = ConversionUtil.getDay(c);
			
			c.add(Calendar.YEAR, 1);
			endDate = ConversionUtil.getDay(c);

			dateWiseEvtList = new DateWiseEventList();
			dateWiseEvtList.addDummyItem();

			eventListAdapter = getAdapterInstance();

			loadItemsInBackground();

		} else {
			eventListAdapter.updateContext(FragmentUtil.getActivity(this));
		}

		setListAdapter(((BaseAdapter) eventListAdapter));

		getListView().setDivider(null);
		getListView().setBackgroundResource(R.drawable.story_space);
	}

	protected void resetWith(String newStartDate, String newEndDate) {
		// if user selection has changed then only reset the list
		if (!startDate.equals(newStartDate)) {
			startDate = newStartDate;
			endDate = newEndDate;
			reset();
		}
	}

	protected void reset() {
		eventListAdapter.setEventsAlreadyRequested(0);
		eventListAdapter.setMoreDataAvailable(true);

		loadEvents.cancel(true);

		dateWiseEvtList.reset();
		((BaseAdapter) eventListAdapter).notifyDataSetChanged();

		loadItemsInBackground();
	}
	

	protected LoadDateWiseEvents getLoadDateWiseEvents() {
		return new LoadDateWiseEvents(Api.OAUTH_TOKEN, dateWiseEvtList, eventListAdapter, lat, lon, startDate, endDate, 
				categories.get(categoryPosition).getId(), 
				((EventSeekr)FragmentUtil.getActivity(this).getApplicationContext()).getWcitiesId());
	}

	protected abstract DateWiseEventParentAdapterListener getAdapterInstance();

}
