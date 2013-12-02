package com.wcities.eventseeker;

import android.os.Bundle;

import com.wcities.eventseeker.adapter.DateWiseMyEventListAdapter;
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class DateWiseEventListFragmentTab extends DateWiseEventListParentFragment {

	private static final String TAG = DateWiseEventListFragmentTab.class.getName();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void loadEventsInBackground() {
		loadEvents = getLoadDateWiseEvents();
		((DateWiseMyEventListAdapter) eventListAdapter).setLoadDateWiseEvents(loadEvents);
		AsyncTaskUtil.executeAsyncTask(loadEvents, true);
	}

	@Override
	protected DateWiseEventParentAdapterListener getAdapterInstance() {
		return new DateWiseMyEventListAdapter(
				FragmentUtil.getActivity(this),
				dateWiseEvtList, 
				null, 
				this);
	}
}
