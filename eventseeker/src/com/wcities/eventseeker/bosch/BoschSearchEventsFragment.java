
package com.wcities.eventseeker.bosch;

import java.util.Calendar;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.LoadDateWiseEvents;
import com.wcities.eventseeker.bosch.adapter.BoschDateWiseEventListAdapter;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.interfaces.BoschOnChildFragmentDisplayModeChangedListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.DateWiseEventList;

public class BoschSearchEventsFragment extends ListFragment implements LoadItemsInBackgroundListener,
	OnClickListener, BoschOnChildFragmentDisplayModeChangedListener {

	private static final String TAG = BoschSearchEventsFragment.class.getName();
	
	private static final int MILES_LIMIT = 10000;

	private String query;
	
	private DateWiseEventList eventList;
	
	private LoadDateWiseEvents loadEvents;

	private BoschDateWiseEventListAdapter eventLstAdptr;
	private double[] latLon;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		
		View view = inflater.inflate(R.layout.bosch_common_list_layout, null);

		view.findViewById(R.id.btnUp).setOnClickListener(this);
		view.findViewById(R.id.btnDown).setOnClickListener(this);

		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if (eventList == null) {
			eventList = new DateWiseEventList();
			eventLstAdptr = new BoschDateWiseEventListAdapter(FragmentUtil.getActivity(this), eventList, null, this);

			Bundle args = getArguments();
	
			if (args != null && args.containsKey(BundleKeys.QUERY)) {
				eventList.addDummyItem();
				query = args.getString(BundleKeys.QUERY);
				loadItemsInBackground();
			}
		} else {
			eventLstAdptr.updateContext(FragmentUtil.getActivity(this));
		}

		setListAdapter(eventLstAdptr);
		
		getListView().setDivider(null);
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
		
		loadEvents = new LoadDateWiseEvents(eventList, eventLstAdptr, query, latLon[0], latLon[1], MILES_LIMIT, 
			((EventSeekr)FragmentUtil.getActivity(this).getApplication()).getWcitiesId(), startDate, endDate);
		eventLstAdptr.setLoadDateWiseEvents(loadEvents);
		AsyncTaskUtil.executeAsyncTask(loadEvents, true);
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
			case R.id.btnUp:
				getListView().smoothScrollByOffset(-1);
				break;
			
			case R.id.btnDown:
				getListView().smoothScrollByOffset(1);
				break;
				
		}
	}

	@Override
	public void onChildFragmentDisplayModeChanged() {
		if (eventLstAdptr != null) {
			eventLstAdptr.notifyDataSetChanged();
		}
	}
}