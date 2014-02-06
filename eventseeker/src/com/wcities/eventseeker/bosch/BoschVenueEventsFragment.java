package com.wcities.eventseeker.bosch;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.LoadDateWiseVenueEventsList;
import com.wcities.eventseeker.bosch.adapter.BoschDateWiseEventListAdapter;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.custom.fragment.ListFragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.DateWiseEventList;

public class BoschVenueEventsFragment extends ListFragmentLoadableFromBackStack implements OnClickListener, 
	LoadItemsInBackgroundListener{

	private static final String TAG = BoschVenueEventsFragment.class.getName();

	private Venue venue;

	private DateWiseEventList dateWiseEvtList;
	private DateWiseEventParentAdapterListener adapter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		venue = (Venue) getArguments().getSerializable(BundleKeys.VENUE);
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
		
		if (dateWiseEvtList == null) {
			dateWiseEvtList = new DateWiseEventList();
			dateWiseEvtList.addDummyItem();
			
	        adapter = new BoschDateWiseEventListAdapter(
	        	((BoschMainActivity) FragmentUtil.getActivity(this)), dateWiseEvtList, null, this);

	        loadItemsInBackground();			
		} else {
			adapter.updateContext(FragmentUtil.getActivity(this));
		}

		setListAdapter((BaseAdapter)adapter);
        getListView().setDivider(null);
	}

	@Override
	public void onResume() {
		super.onResume(AppConstants.INVALID_INDEX, getResources().getString(R.string.title_events));
	}
	
	@Override
	public void loadItemsInBackground() {
		LoadDateWiseVenueEventsList loadEvents = new LoadDateWiseVenueEventsList(dateWiseEvtList, adapter, 
			((EventSeekr)FragmentUtil.getActivity(this).getApplicationContext()).getWcitiesId(), venue.getId());
        adapter.setLoadDateWiseEvents(loadEvents);
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
}
