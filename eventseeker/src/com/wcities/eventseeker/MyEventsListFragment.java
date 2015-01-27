package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.List;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ScrollView;

import com.facebook.Session;
import com.facebook.SessionState;
import com.wcities.eventseeker.DrawerListFragment.DrawerListFragmentListener;
import com.wcities.eventseeker.adapter.MyEventListAdapter;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi.Type;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.LoadMyEventsNewUI;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.custom.fragment.PublishEventListFragment;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.interfaces.PublishListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class MyEventsListFragment extends PublishEventListFragment implements LoadItemsInBackgroundListener, 
		PublishListener, /*MyEventsLoadedListener, */OnClickListener {
	
	private static final String TAG = MyEventsListFragment.class.getSimpleName();
	
	private Type loadType;
	private String wcitiesId;
	
	private LoadMyEventsNewUI loadEvents;
	private MyEventListAdapter eventListAdapter;
	private List<Event> eventList;
	
	private ScrollView scrlVRootNoItemsFoundWithAction;
	private double[] latLon;
	
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
		
		if (eventList == null) {
			Bundle args = getArguments();
			loadType = (Type) args.getSerializable(BundleKeys.LOAD_TYPE);
			
			eventList = new ArrayList<Event>();
			eventList.add(null);
			
	        eventListAdapter = new MyEventListAdapter(FragmentUtil.getActivity(this),  
	        		eventList, null, this, this, FragmentUtil.getScreenName(this));

			loadItemsInBackground();
			
		} else {
			eventListAdapter.updateContext(FragmentUtil.getActivity(this));
			//onEventsLoaded();
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
		if (latLon == null) {
			latLon = DeviceUtil.getLatLon(FragmentUtil.getApplication(this));
		}
		loadEvents = new LoadMyEventsNewUI(Api.OAUTH_TOKEN, eventList, eventListAdapter, wcitiesId, loadType, 
				latLon[0], latLon[1], null);
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

	/*@Override
	public void onEventsLoaded() {
		if (eventList.size() == 1 && eventList.getItemViewType(0) == LIST_ITEM_TYPE.NO_EVENTS 
				&& eventList.getItem(0).getEvent().getId() == AppConstants.INVALID_ID 
				&& wcitiesId != null) {
			scrlVRootNoItemsFoundWithAction.setVisibility(View.VISIBLE);
			((TextView)scrlVRootNoItemsFoundWithAction.findViewById(R.id.txtNoItemsHeading)).setText(
					R.string.search_artists);
			((TextView)scrlVRootNoItemsFoundWithAction.findViewById(R.id.txtNoItemsMsg)).setText(
					R.string.no_events_in_your_area);
			((Button)scrlVRootNoItemsFoundWithAction.findViewById(R.id.btnAction)).setText(
					R.string.search_artists);
			((ImageView)scrlVRootNoItemsFoundWithAction.findViewById(R.id.imgNoItems)).setImageDrawable(
					res.getDrawable(R.drawable.no_my_events));
			*//**
			 * try-catch is used to handle case where even before we get call back to this function, user leaves 
			 * this screen.
			 *//*
			try {
				getListView().setVisibility(View.GONE);
				
			} catch (IllegalStateException e) {
				Log.e(TAG, "" + e.getMessage());
				e.printStackTrace();
			}
		}	
	}*/

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.btnAction:
			((DrawerListFragmentListener)FragmentUtil.getActivity(this)).onDrawerItemSelected(
					MainActivity.INDEX_NAV_ITEM_FOLLOWING, null);
			break;

		default:
			break;
		}
	}	
}
