package com.wcities.eventseeker;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wcities.eventseeker.adapter.DateWiseMyEventListAdapter;
import com.wcities.eventseeker.adapter.DateWiseMyEventListAdapter.DateWiseMyEventListAdapterListener;
import com.wcities.eventseeker.api.UserInfoApi.Type;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.LoadDateWiseMyEvents;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.DateWiseEventList;

public class MyEventsListFragment extends ListFragment implements DateWiseMyEventListAdapterListener {
	
	private Type loadType;
	private String wcitiesId;
	
	private LoadDateWiseMyEvents loadEvents;
	private DateWiseMyEventListAdapter eventListAdapter;
	private DateWiseEventList dateWiseEvtList;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (wcitiesId == null) {
			wcitiesId = ((EventSeekr)FragmentUtil.getActivity(this).getApplication()).getWcitiesId();
		}
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		//Log.d(TAG, "onActivityCreated()");
		
		if (dateWiseEvtList == null) {
			Bundle args = getArguments();
			loadType = (Type) args.getSerializable(BundleKeys.LOAD_TYPE);
			
			dateWiseEvtList = new DateWiseEventList();
			dateWiseEvtList.addDummyItem();
			
	        eventListAdapter = new DateWiseMyEventListAdapter(FragmentUtil.getActivity(this), dateWiseEvtList, null, this);

			loadEventsInBackground();
			
		} else {
			eventListAdapter.updateContext(FragmentUtil.getActivity(this));
		}

		setListAdapter(eventListAdapter);
        getListView().setDivider(null);
        getListView().setBackgroundResource(R.drawable.story_space);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = super.onCreateView(inflater, container, savedInstanceState);

		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			int pad = getResources().getDimensionPixelSize(R.dimen.tab_bar_margin_fragment_custom_tabs);
			v.setPadding(pad, 0, pad, 0);
		}
		return v;
	}
	
	@Override
	public void loadEventsInBackground() {
		double[] latLon = DeviceUtil.getLatLon(FragmentUtil.getActivity(this));
		loadEvents = new LoadDateWiseMyEvents(dateWiseEvtList, eventListAdapter, wcitiesId, loadType, latLon[0], latLon[1]);
		eventListAdapter.setLoadDateWiseEvents(loadEvents);
        AsyncTaskUtil.executeAsyncTask(loadEvents, true);
	}
}
