package com.wcities.eventseeker;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.LoadDateWiseVenueEventsList;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.DateWiseEventList;

public abstract class VenueEventsParentListFragment extends FbPublishEventListFragment implements 
		LoadItemsInBackgroundListener {
	
	private static final String TAG = VenueEventsParentListFragment.class.getName();

	protected Venue venue;

	protected DateWiseEventList dateWiseEvtList;
	protected DateWiseEventParentAdapterListener adapter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		venue = (Venue) getArguments().getSerializable(BundleKeys.VENUE);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		int orientation = getResources().getConfiguration().orientation;

		View v = super.onCreateView(inflater, container, savedInstanceState);
		
		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		lp.addRule(RelativeLayout.ABOVE, R.id.lnrLayoutBtns);
		
		if (orientation == Configuration.ORIENTATION_LANDSCAPE 
				|| ((MainActivity)FragmentUtil.getActivity(this)).isTablet()) {
			lp.leftMargin = lp.rightMargin = getResources().getDimensionPixelSize(R.dimen.tab_bar_margin_fragment_custom_tabs);
		}
		
		v.setLayoutParams(lp);
		
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if (dateWiseEvtList == null) {
			dateWiseEvtList = new DateWiseEventList();
			dateWiseEvtList.addDummyItem();
			
	        adapter = getAdapterInstance();

	        loadItemsInBackground();
			
		} else {
			adapter.updateContext(FragmentUtil.getActivity(this));
		}

		setListAdapter((BaseAdapter)adapter);
        getListView().setDivider(null);
        getListView().setBackgroundResource(R.drawable.story_space);
	}
	
	@Override
	public void loadItemsInBackground() {
		LoadDateWiseVenueEventsList loadEvents = new LoadDateWiseVenueEventsList(dateWiseEvtList, adapter, 
				((EventSeekr)FragmentUtil.getActivity(this).getApplicationContext()).getWcitiesId(), venue.getId());
        adapter.setLoadDateWiseEvents(loadEvents);
		//loadEvents.execute();
        AsyncTaskUtil.executeAsyncTask(loadEvents, true);
	}
	
	protected abstract DateWiseEventParentAdapterListener getAdapterInstance();
}
