package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.List;

import android.content.res.Configuration;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.Session;
import com.facebook.SessionState;
import com.wcities.eventseeker.adapter.MyEventGridAdapterTab1;
import com.wcities.eventseeker.adapter.MyEventGridAdapterTab1.SaveEventInstanceListener;
import com.wcities.eventseeker.adapter.MyEventGridAdapterTab1.OnNoEventsListener;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi.Type;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.LoadMyEvents;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.custom.fragment.PublishEventFragment;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.interfaces.FullScrnProgressListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class MyEventsGridFragmentTab1 extends PublishEventFragment implements LoadItemsInBackgroundListener, 
		FullScrnProgressListener, AsyncTaskListener<Void>, OnNoEventsListener, SaveEventInstanceListener {
	
	private static final String TAG = MyEventsGridFragmentTab1.class.getSimpleName();

	private static final int NUM_COLUMNS_PORTRAIT = 2;
	private static final int NUM_COLUMNS_LANDSCAPE = 3;
	
	private View rltRootNoContentFound;
	private RelativeLayout rltLytPrgsBar;
	private GridView grdvMyEvents;

	private String wcitiesId;
	private Type loadType;
	
	private LoadMyEvents loadEvents;
	private MyEventGridAdapterTab1 eventGridAdapter;
	private List<Event> eventList;
	
	
	private double[] latLon;
	
	/**
	 * Using its instance variable since otherwise calling getResources() directly from fragment from 
	 * callback methods is dangerous in a sense that it may throw java.lang.IllegalStateException: 
	 * Fragment MyEventsListFragment not attached to Activity, if user has already left this fragment & 
	 * then changed the orientation.
	 */
	private Handler handler;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (wcitiesId == null) {
			wcitiesId = ((EventSeekr)FragmentUtil.getActivity(this).getApplication()).getWcitiesId();
		}
		handler = new Handler(Looper.getMainLooper());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_my_events_tab1, null);
		grdvMyEvents = (GridView) v.findViewById(R.id.grdvMyEvents);
		grdvMyEvents.setNumColumns(FragmentUtil.getResources(this).getConfiguration()
			.orientation == Configuration.ORIENTATION_PORTRAIT ? NUM_COLUMNS_PORTRAIT : NUM_COLUMNS_LANDSCAPE);
				
		rltRootNoContentFound = v.findViewById(R.id.rltRootNoContentFound);
		rltLytPrgsBar = (RelativeLayout) v.findViewById(R.id.rltLytPrgsBar);
		rltLytPrgsBar.setBackgroundResource(R.drawable.bg_no_content_overlay);
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (eventList == null) {
			Bundle args = getArguments();
			loadType = (Type) args.getSerializable(BundleKeys.LOAD_TYPE);
			
			eventList = new ArrayList<Event>();
			eventList.add(null);
			
	        eventGridAdapter = new MyEventGridAdapterTab1(this, eventList, null, this, FragmentUtil.getScreenName(this), 
	        		this, this);
	        
			loadItemsInBackground();
			grdvMyEvents.setAdapter(eventGridAdapter);
		}

	}
	
	@Override
	public void loadItemsInBackground() {
		if (latLon == null) {
			latLon = DeviceUtil.getLatLon(FragmentUtil.getApplication(this));
		}
		loadEvents = new LoadMyEvents(Api.OAUTH_TOKEN, eventList, eventGridAdapter, wcitiesId, loadType, 
				latLon[0], latLon[1], this);
		eventGridAdapter.setLoadDateWiseEvents(loadEvents);
        AsyncTaskUtil.executeAsyncTask(loadEvents, true);
	}
	
	public Type getLoadType() {
		return loadType;
	}

	@Override
	public void onNoEventsFound() {
		if (eventList.size() == 1 && eventList.get(0) != null && eventList.get(0).getId() == AppConstants.INVALID_ID 
				&& wcitiesId != null) {
			/**
			 * The handler over here is used as this method will be called from the getView. So, there was an error
			 * occurring, even though the 'scrlVRootNoItemsFoundWithAction.setVisibility(View.VISIBLE);' called then 
			 * too that 'no events found' related layout wasn't appearing but using this post call on UI that started 
			 * working as expected.
			 */
			handler.post(new Runnable() {
				
				@Override
				public void run() {
					/**
					 * try-catch is used to handle case where even before we get call back to this function, user leaves 
					 * this screen.
					 */
					try {
						grdvMyEvents.setVisibility(View.GONE);
						
					} catch (IllegalStateException e) {
						Log.e(TAG, "" + e.getMessage());
						e.printStackTrace();
					}
					
					int txtres, imgNoItemsRes, imgPhoneRes;
					if (loadType == Type.mysavedevents) {
						txtres = R.string.saved_events_no_content; 
						imgNoItemsRes = R.drawable.ic_unsaved_event_slider; 
						imgPhoneRes = R.drawable.ic_saved_events_no_content;	
						
					} else {
						txtres = R.string.my_events_events_no_content; 
						imgNoItemsRes = R.drawable.ic_list_follow; 
						imgPhoneRes = (loadType == Type.myevents) ?
								R.drawable.ic_my_events_no_content : R.drawable.ic_recommended_events_no_content;							
					}
					
					/*((ImageView) rltRootNoContentFound.findViewById(R.id.imgNoItems))
						.setImageDrawable(res.getDrawable(imgNoItemsRes));*/

					TextView txtNoContentMsg = (TextView) rltRootNoContentFound.findViewById(R.id.txtNoItemsMsg);
					txtNoContentMsg.setText(txtres);
					txtNoContentMsg.setCompoundDrawablesWithIntrinsicBounds(0, imgNoItemsRes, 0, 0);
					
					((ImageView) rltRootNoContentFound.findViewById(R.id.imgPhone))
						.setImageDrawable(FragmentUtil.getResources(MyEventsGridFragmentTab1.this).getDrawable(imgPhoneRes));
					
					rltRootNoContentFound.setVisibility(View.VISIBLE);					
				}
			});
			
		}	
	}

	@Override
	public void onTaskCompleted(Void... params) {
		rltLytPrgsBar.setVisibility(View.INVISIBLE);
	}

	@Override
	public void displayFullScrnProgress() {
		rltLytPrgsBar.setVisibility(View.VISIBLE);
	}
	
	private void resetEventList() {
		if (loadEvents != null && loadEvents.getStatus() != Status.FINISHED) {
			loadEvents.cancel(true);
		}
		eventGridAdapter.setMoreDataAvailable(true);
		eventGridAdapter.setEventsAlreadyRequested(0);
		eventList.clear();
		eventList.add(null);
		eventGridAdapter.notifyDataSetChanged();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		//Log.d(TAG, "onDestroy()");
		if (loadEvents != null && loadEvents.getStatus() != Status.FINISHED) {
			loadEvents.cancel(true);
		}
	}
	
	public void onEventAttendingUpdated() {
		/**
		 * we will get this notification only while saving events. If user is saving event from following/recommended
		 * tab then saved events should refresh to display updated list
		 */
		if (loadType == Type.mysavedevents) {
			resetEventList();
		}
	}

	@Override
	public void onPublishPermissionGranted() {
		eventGridAdapter.onPublishPermissionGranted();
	}

	@Override
	public void call(Session session, SessionState state, Exception exception) {
		eventGridAdapter.call(session, state, exception);
	}

	@Override
	public void setEvent(Event event) {
		this.event = event;
	}
}
