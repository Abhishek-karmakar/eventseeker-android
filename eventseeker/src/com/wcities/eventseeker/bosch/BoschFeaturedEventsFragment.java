package com.wcities.eventseeker.bosch;

import java.util.List;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.asynctask.LoadFeaturedEvts;
import com.wcities.eventseeker.asynctask.LoadFeaturedEvts.OnLoadFeaturedEventsListener;
import com.wcities.eventseeker.bosch.BoschMainActivity.OnDisplayModeChangedListener;
import com.wcities.eventseeker.bosch.adapter.BoschEventListAdapter;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.custom.fragment.ListFragmentLoadableFromBackStack;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class BoschFeaturedEventsFragment extends ListFragmentLoadableFromBackStack implements
		OnLoadFeaturedEventsListener, OnClickListener, OnDisplayModeChangedListener {

	private static final String TAG = BoschFeaturedEventsFragment.class.getName();
	private ProgressBar prgbr;
	private View rltContent;
	private boolean isLoadingFeaturedEvents, isUILoading;
	private BoschEventListAdapter adapter;
	private List<Event> eventList;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		double latlon[] = DeviceUtil.getLatLon(FragmentUtil.getActivity(this));
		AsyncTaskUtil.executeAsyncTask(new LoadFeaturedEvts(this, latlon[0], latlon[1]), true);		
		isUILoading = true;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.bosch_common_list_layout, null);

		prgbr = (ProgressBar) view.findViewById(R.id.prgbr);

		rltContent = view.findViewById(R.id.rltContent);

		view.findViewById(R.id.btnUp).setOnClickListener(this);
		view.findViewById(R.id.btnDown).setOnClickListener(this);

		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		isUILoading = false;
		
		if (isLoadingFeaturedEvents) {
			
			prgbr.setVisibility(View.VISIBLE);
			rltContent.setVisibility(View.INVISIBLE);		
		
		} else if (adapter != null) {
			initailizeAdapter();
		}
	}
	
	@Override
	public void onResume() {
		super.onResume(AppConstants.INVALID_INDEX, getResources().getString(R.string.title_featured));
	}
	
	
	@Override
	public void onPreLoadingFeaturedEvents() {
		isLoadingFeaturedEvents = true;
	}

	@Override
	public void onPostLoadingFeaturedEvents(List<Event> result) {
		isLoadingFeaturedEvents = false;
		
		try {
			/**
			 * added code in try catch as some times when device gets disconnected when execution is in 
			 * between of below 'if' block than app gets crash due to null pointer.
			 */
			if (!isUILoading) {
				prgbr.setVisibility(View.GONE);
				rltContent.setVisibility(View.VISIBLE);		
				
				eventList = result;
				
				initailizeAdapter();
			}
			
		} catch (Exception e) {
			Log.e(TAG, "Error : " + e.toString() + " in onPostLoadingFeaturedEvents()");
		}
	}

	private void initailizeAdapter() {
		adapter = new BoschEventListAdapter(FragmentUtil.getActivity(this), eventList, this);
		setListAdapter(adapter);

		getListView().setDivider(null);
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
	public void onDisplayModeChanged(boolean isNightModeEnabled) {
		if (adapter != null) {
			adapter.notifyDataSetChanged();
		}
	}
}
