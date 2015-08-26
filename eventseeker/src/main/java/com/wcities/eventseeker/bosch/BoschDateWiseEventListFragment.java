package com.wcities.eventseeker.bosch;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.LoadDateWiseEvents;
import com.wcities.eventseeker.bosch.BoschMainActivity.OnDisplayModeChangedListener;
import com.wcities.eventseeker.bosch.adapter.BoschDateWiseEventListAdapter;
import com.wcities.eventseeker.bosch.interfaces.BoschAsyncTaskListener;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Category;
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.DateWiseEventList;

import java.util.Calendar;

public class BoschDateWiseEventListFragment extends ListFragment implements LoadItemsInBackgroundListener, 
		OnDisplayModeChangedListener, BoschAsyncTaskListener {

	private LoadDateWiseEvents loadEvents;
	private DateWiseEventList dateWiseEvtList;
	private DateWiseEventParentAdapterListener eventListAdapter;

	private Category selectedCategory;
	private double lat, lon;
	private String startDate, endDate;
	private View rltProgressBar;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.bosch_list, null);
		rltProgressBar = view.findViewById(R.id.rltProgressBar);
		return view;
	}

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

			double[] latLon = DeviceUtil.getLatLon(FragmentUtil.getApplication(this));
			lat = latLon[0];
			lon = latLon[1];

			Calendar c = Calendar.getInstance();
			startDate = ConversionUtil.getDay(c);
			c.add(Calendar.YEAR, 1);
			endDate = ConversionUtil.getDay(c);

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
		loadEvents = new LoadDateWiseEvents(Api.OAUTH_TOKEN_BOSCH_APP, dateWiseEvtList, eventListAdapter, lat, lon, startDate, endDate, 
				selectedCategory.getId(), ((EventSeekr)FragmentUtil.getActivity(this).getApplication()).getWcitiesId());
		((BoschDateWiseEventListAdapter) eventListAdapter).setLoadDateWiseEvents(loadEvents);
		loadEvents.setBoschAsyncTaskListener(this);
		AsyncTaskUtil.executeAsyncTask(loadEvents, true);
	}
	
	public void scrollUp() {
		getListView().setSelection(getListView().getFirstVisiblePosition() - 1);
	}
	
	public void scrollDown() {
		getListView().setSelection(getListView().getFirstVisiblePosition() + 1);
	}
	
	@Override
	public void onDisplayModeChanged(boolean isNightModeEnabled) {
		if (eventListAdapter != null) {
			((BaseAdapter)eventListAdapter).notifyDataSetChanged();
		}
	}

	@Override
	public void onTaskCompleted() {
	}
}
