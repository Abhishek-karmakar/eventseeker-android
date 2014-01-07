package com.wcities.eventseeker.bosch;

import java.util.Calendar;

import android.os.Bundle;
import android.os.Build.VERSION_CODES;
import android.support.v4.app.ListFragment;
import android.widget.BaseAdapter;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.adapter.DateWiseEventListAdapter;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.LoadDateWiseEvents;
import com.wcities.eventseeker.bosch.adapter.BoschDateWiseEventListAdapter;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Category;
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.DateWiseEventList;

public class BoschDateWiseEventListFragment extends ListFragment implements LoadItemsInBackgroundListener {

	private LoadDateWiseEvents loadEvents;
	private DateWiseEventList dateWiseEvtList;
	private DateWiseEventParentAdapterListener eventListAdapter;

	private Category selectedCategory;
	private double lat, lon;
	private String startDate;
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (dateWiseEvtList == null) {
			if (getArguments() != null) {
				// Bundle has these values when this fragment is called from
				// BoschDiscoverByCategoryFragment.
				Bundle bundle = getArguments();
				selectedCategory = (Category) bundle.get(BundleKeys.CATEGORY);
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

			eventListAdapter = new BoschDateWiseEventListAdapter(FragmentUtil.getActivity(this), dateWiseEvtList, 
					null, this);

			loadItemsInBackground();

		} else {
			eventListAdapter.updateContext(FragmentUtil.getActivity(this));
		}

		setListAdapter(((BaseAdapter) eventListAdapter));

		getListView().setDivider(null);
	}
	
	@Override
	public void loadItemsInBackground() {
		loadEvents = new LoadDateWiseEvents(dateWiseEvtList, eventListAdapter, lat, lon, startDate, 
				selectedCategory.getId(), ((EventSeekr)FragmentUtil.getActivity(this).getApplication()).getWcitiesId());
		((BoschDateWiseEventListAdapter) eventListAdapter).setLoadDateWiseEvents(loadEvents);
		AsyncTaskUtil.executeAsyncTask(loadEvents, true);
	}
	
	public void scrollUp() {
		getListView().setSelection(getListView().getFirstVisiblePosition() - 1);
	}
	
	public void scrollDown() {
		getListView().setSelection(getListView().getFirstVisiblePosition() + 1);
	}
}
