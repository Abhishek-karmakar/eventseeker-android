package com.wcities.eventseeker;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.facebook.Session;
import com.facebook.SessionState;
import com.wcities.eventseeker.DrawerListFragment.DrawerListFragmentListener;
import com.wcities.eventseeker.adapter.DateWiseMyEventListAdapter;
import com.wcities.eventseeker.api.UserInfoApi.Type;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.LoadDateWiseMyEvents;
import com.wcities.eventseeker.asynctask.LoadDateWiseMyEvents.MyEventsLoadedListener;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.interfaces.FbPublishListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.DateWiseEventList;
import com.wcities.eventseeker.viewdata.DateWiseEventList.LIST_ITEM_TYPE;

public class MyEventsListFragment extends FbPublishEventListFragment implements LoadItemsInBackgroundListener, 
		FbPublishListener, MyEventsLoadedListener, OnClickListener {
	
	private static final String TAG = MyEventsListFragment.class.getSimpleName();
	
	private Type loadType;
	private String wcitiesId;
	
	private LoadDateWiseMyEvents loadEvents;
	private DateWiseMyEventListAdapter eventListAdapter;
	private DateWiseEventList dateWiseEvtList;
	
	private ScrollView scrlVRootNoItemsFoundWithAction;
	
	/**
	 * Using its instance variable since otherwise calling getResources() directly from fragment from 
	 * callback methods is dangerous in a sense that it may throw java.lang.IllegalStateException: 
	 * Fragment MyEventsListFragment not attached to Activity, if user has already left this fragment & 
	 * then changed the orientation.
	 */
	private Resources res;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (wcitiesId == null) {
			wcitiesId = ((EventSeekr)FragmentUtil.getActivity(this).getApplication()).getWcitiesId();
		}
		res = getResources();
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
			
	        eventListAdapter = new DateWiseMyEventListAdapter(FragmentUtil.getActivity(this),  
	        		dateWiseEvtList, null, this, this);

			loadItemsInBackground();
			
		} else {
			eventListAdapter.updateContext(FragmentUtil.getActivity(this));
			onEventsLoaded();
		}

		setListAdapter(eventListAdapter);
        getListView().setDivider(null);
        //getListView().setBackgroundResource(R.drawable.story_space);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_my_events_list, null);
		scrlVRootNoItemsFoundWithAction = (ScrollView) v.findViewById(R.id.scrlVRootNoItemsFoundWithAction);
		v.findViewById(R.id.btnAction).setOnClickListener(this);
		return v;
	}
	
	@Override
	public void loadItemsInBackground() {
		double[] latLon = DeviceUtil.getLatLon(FragmentUtil.getActivity(this));
		loadEvents = new LoadDateWiseMyEvents(dateWiseEvtList, eventListAdapter, wcitiesId, loadType, 
				latLon[0], latLon[1], this);
		eventListAdapter.setLoadDateWiseEvents(loadEvents);
        AsyncTaskUtil.executeAsyncTask(loadEvents, true);
	}
	
	public Type getLoadType() {
		return loadType;
	}

	@Override
	public void call(Session session, SessionState state, Exception exception) {
		eventListAdapter.call(session, state, exception);
	}

	@Override
	public void onPublishPermissionGranted() {
		eventListAdapter.onPublishPermissionGranted();
	}

	@Override
	public void onEventsLoaded() {
		if (dateWiseEvtList.getCount() == 1 && dateWiseEvtList.getItemViewType(0) == LIST_ITEM_TYPE.NO_EVENTS 
				&& dateWiseEvtList.getItem(0).getEvent().getId() == AppConstants.INVALID_ID 
				&& wcitiesId != null) {
			scrlVRootNoItemsFoundWithAction.setVisibility(View.VISIBLE);
			((TextView)scrlVRootNoItemsFoundWithAction.findViewById(R.id.txtNoItemsHeading)).setText(
					"Search Artists");
			((TextView)scrlVRootNoItemsFoundWithAction.findViewById(R.id.txtNoItemsMsg)).setText(
					"No events currently in your area. Follow more artists to start your personalized event calendar.");
			((Button)scrlVRootNoItemsFoundWithAction.findViewById(R.id.btnAction)).setText(
					"Search Artists");
			((ImageView)scrlVRootNoItemsFoundWithAction.findViewById(R.id.imgNoItems)).setImageDrawable(
					res.getDrawable(R.drawable.no_my_events));
			/**
			 * try-catch is used to handle case where even before we get call back to this function, user leaves 
			 * this screen.
			 */
			try {
				getListView().setVisibility(View.GONE);
				
			} catch (IllegalStateException e) {
				Log.e(TAG, "" + e.getMessage());
				e.printStackTrace();
			}
		}	
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.btnAction:
			((DrawerListFragmentListener)FragmentUtil.getActivity(this)).onDrawerItemSelected(
					MainActivity.INDEX_NAV_ITEM_FOLLOWING);
			break;

		default:
			break;
		}
	}
}
