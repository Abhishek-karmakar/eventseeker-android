package com.wcities.eventseeker.bosch;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi.Type;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.LoadMyEvents1;
import com.wcities.eventseeker.bosch.adapter.BoschLazyLoadingEventListAdapter;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.interfaces.BoschOnChildFragmentDisplayModeChangedListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;

import java.util.ArrayList;
import java.util.List;

public class BoschMyEventsListFragment extends ListFragment implements OnClickListener, LoadItemsInBackgroundListener, 
	BoschOnChildFragmentDisplayModeChangedListener {
	
	private String wcitiesId;
	
	private LoadMyEvents1 loadEvents;
	private List<Event> eventList;
	private BoschLazyLoadingEventListAdapter eventListAdapter;
	
	private Type loadType;
	private double[] latLon;

	public static String getTag(Type loadType) {
		return BoschMyEventsListFragment.class.getSimpleName() + loadType.name(); 
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (wcitiesId == null) {
			wcitiesId = ((EventSeekr)FragmentUtil.getActivity(this).getApplication()).getWcitiesId();
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.bosch_common_list_layout, null);
		
        view.findViewById(R.id.btnUp).setOnClickListener(this);
		view.findViewById(R.id.btnDown).setOnClickListener(this);
		
		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if (eventList == null) {
			Bundle args = getArguments();
			loadType = (Type) args.getSerializable(BundleKeys.LOAD_TYPE);
			
			eventList = new ArrayList<Event>();
			eventList.add(null);
			
	        eventListAdapter = new BoschLazyLoadingEventListAdapter(FragmentUtil.getActivity(this), 
	        		eventList, null, this);

			loadItemsInBackground();
			
		} else {
			eventListAdapter.updateContext(FragmentUtil.getActivity(this));
		}

		setListAdapter(eventListAdapter);
        getListView().setDivider(null);
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.btnUp:
			getListView().setSelection(getListView().getFirstVisiblePosition() - 1);
			break;
			
		case R.id.btnDown:
			getListView().setSelection(getListView().getFirstVisiblePosition() + 1);
			break;

		default:
			break;
		}
	}

	@Override
	public void loadItemsInBackground() {
		if (latLon == null) {
			latLon = DeviceUtil.getLatLon(FragmentUtil.getApplication(this));
		}
		loadEvents = new LoadMyEvents1(Api.OAUTH_TOKEN_BOSCH_APP, eventList, eventListAdapter, wcitiesId, loadType, latLon[0], 
				latLon[1]);
		eventListAdapter.setLoadDateWiseEvents(loadEvents);
        AsyncTaskUtil.executeAsyncTask(loadEvents, true);
	}

	@Override
	public void onChildFragmentDisplayModeChanged() {
		if (eventListAdapter != null) {
			eventListAdapter.notifyDataSetChanged();
		}
	}
}
