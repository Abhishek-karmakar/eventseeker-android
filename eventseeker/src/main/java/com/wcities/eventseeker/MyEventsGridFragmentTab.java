package com.wcities.eventseeker;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.wcities.eventseeker.adapter.RVMyEventsAdapterTab;
import com.wcities.eventseeker.adapter.RVMyEventsAdapterTab.RVMyEventsAdapterTabListener;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi.Type;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.LoadMyEvents;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.custom.fragment.PublishEventFragment;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.interfaces.SwipeTabVisibilityListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.ItemDecorationItemOffset;

import java.util.ArrayList;
import java.util.List;

public class MyEventsGridFragmentTab extends PublishEventFragment implements LoadItemsInBackgroundListener, 
		AsyncTaskListener<Void>, RVMyEventsAdapterTabListener, SwipeTabVisibilityListener, GeneralDialogFragment.DialogBtnClickListener {
	
	private static final String TAG = MyEventsGridFragmentTab.class.getSimpleName();
	
	private static final int GRID_COLUMNS_PORTRAIT = 2;
	private static final int GRID_COLUMNS_LANDSCAPE = 3;

	private LoadMyEvents loadEvents;
	private List<Event> eventList;
	private double lat, lon;
	
	private RecyclerView recyclerVEvents;
	private LinearLayoutManager layoutManager;
	private RelativeLayout rltLytProgressBar, rltLytNoEvts;
	private ImageView imgPrgOverlay;
	
	private RVMyEventsAdapterTab rvMyEventsAdapterTab;
	
	private Handler handler;

	private Type loadType;

	private String wcitiesId;

	private boolean isNoEventFound;

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
		//Log.d(TAG, "onCreateView()");		
		View v = inflater.inflate(R.layout.fragment_my_events_tab, container, false);
		
		// use a linear layout manager
		layoutManager = new LinearLayoutManager(FragmentUtil.getActivity(this));
		layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
		
		recyclerVEvents = (RecyclerView) v.findViewById(R.id.recyclerVEvents);
		int spanCount = (FragmentUtil.getResources(this).getConfiguration().orientation == 
				Configuration.ORIENTATION_PORTRAIT) ? GRID_COLUMNS_PORTRAIT : GRID_COLUMNS_LANDSCAPE;
		GridLayoutManager gridLayoutManager = new GridLayoutManager(FragmentUtil.getActivity(this), spanCount);
		recyclerVEvents.setHasFixedSize(true);
		recyclerVEvents.setLayoutManager(gridLayoutManager);
		
		rltLytProgressBar = (RelativeLayout) v.findViewById(R.id.rltLytProgressBar);
		// Applying background here since overriding background doesn't work from xml with <include> layout
		imgPrgOverlay = (ImageView) rltLytProgressBar.findViewById(R.id.imgPrgOverlay);
		
		rltLytNoEvts = (RelativeLayout) v.findViewById(R.id.rltLytNoEvts);
		if (isNoEventFound) {
			// retain no events layout visibility on orientation change
			refreshNoContentLyt(View.VISIBLE);
		}
		
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if (eventList == null) {
			Bundle args = getArguments();
			loadType = (Type) args.getSerializable(BundleKeys.LOAD_TYPE);
			
			double[] latLon = DeviceUtil.getLatLon(FragmentUtil.getApplication(this));
			lat = latLon[0];
			lon = latLon[1];
			
			eventList = new ArrayList<Event>();
			eventList.add(null);
			
			rvMyEventsAdapterTab = new RVMyEventsAdapterTab(eventList, null, this, this, this);
			
		} else {
			// to update values which should change on orientation change
			rvMyEventsAdapterTab.onActivityCreated();
		}
		
		Resources res = FragmentUtil.getResources(this);
		recyclerVEvents.addItemDecoration(new ItemDecorationItemOffset(res.getDimensionPixelSize(
				R.dimen.rv_item_l_r_offset_discover_tab), res.getDimensionPixelSize(R.dimen.rv_item_t_b_offset_discover_tab)));
		recyclerVEvents.setAdapter(rvMyEventsAdapterTab);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (loadEvents != null && loadEvents.getStatus() != Status.FINISHED) {
			loadEvents.cancel(true);
		}
	}
	
	private void resetEventList() {
		if (rvMyEventsAdapterTab == null) {
			return;
		}
		//Log.d(TAG, "resetEventList()");
		rvMyEventsAdapterTab.reset();

		if (loadEvents != null) {
			loadEvents.cancel(true);
		}

		eventList.clear();
		eventList.add(null);
		
		rvMyEventsAdapterTab.notifyDataSetChanged();
		/**
		 * Although we expect rvMyEventsAdapterTab to call loadItemsInBackground() due to 1st null item in
		 * eventList, it won't work always. For eg - If user changes categories fast one by one then loadDateWiseEvents
		 * in rvMyEventsAdapterTab won't be null & also it won't be in FINISHED state. Hence it won't call
		 * loadItemsInBackground() for latest category selection in this case, so better we call loadItemsInBackground()
		 * from here itself.
		 */
		loadItemsInBackground();
	}
		
	@Override
	public void setEvent(Event event) {
		this.event = event;
	}
	
	public void setCenterProgressBarVisibility(final int visibility) {
		handler.post(new Runnable() {

			@Override
			public void run() {
				rltLytProgressBar.setVisibility(visibility);
				imgPrgOverlay.setVisibility(visibility);

				if (visibility == View.VISIBLE) {
					refreshNoContentLyt(View.GONE);

				} else {
					// free up memory
					imgPrgOverlay.setBackgroundResource(0);
				}
			}
		});
	}
	
	@Override
	public void loadItemsInBackground() {
		loadEvents = new LoadMyEvents(Api.OAUTH_TOKEN, eventList, rvMyEventsAdapterTab, wcitiesId, loadType,
				lat, lon, this);
		if (FragmentUtil.getActivity(this).getIntent().hasExtra(BundleKeys.IS_FROM_NOTIFICATION)
				&& loadType == Type.recommendedevent) {
			loadEvents.setAddSrcFromNotification(true);
			FragmentUtil.getActivity(this).getIntent().removeExtra(BundleKeys.IS_FROM_NOTIFICATION);
		}
		rvMyEventsAdapterTab.setLoadDateWiseEvents(loadEvents);
        AsyncTaskUtil.executeAsyncTask(loadEvents, true);
	}

	@Override
	public void onTaskCompleted(Void... params) {
		isNoEventFound = eventList.size() == 1 && eventList.get(0) != null && eventList.get(0).getId() == AppConstants.INVALID_ID;
		handler.post(new Runnable() {
			
			@Override
			public void run() {
				//Log.d(TAG, "onEventsLoaded()");
				// to remove full screen progressbar
				setCenterProgressBarVisibility(View.GONE);
				if (isNoEventFound) {
					refreshNoContentLyt(View.VISIBLE);
				}
			}
		});
	}

	@Override
	public void onPublishPermissionGranted() {
		if (FragmentUtil.getActivity(this) != null) {
			 // if user has not left the screen (activity)
			//Log.d(TAG, "activity != null");
			rvMyEventsAdapterTab.onPublishPermissionGranted();
			((MyEventsFragmentTab) getParentFragment()).onEventAttendingUpdated();
			showAddToCalendarDialog(this);
		}
	}

	@Override
	public void onSuccess(LoginResult loginResult) {
		Log.d(TAG, "onSuccess()");
		rvMyEventsAdapterTab.onSuccess(loginResult);
	}

	@Override
	public void onCancel() {
		Log.d(TAG, "onCancel()");
	}

	@Override
	public void onError(FacebookException e) {
		Log.d(TAG, "onError()");
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
	
	private void refreshNoContentLyt(int visibility) {
		rltLytNoEvts.setVisibility(visibility);
		if (visibility == View.VISIBLE) {
			recyclerVEvents.setVisibility(View.GONE);
			
			int txtres, imgNoItemsRes, imgPhoneRes;
			if (loadType == Type.mysavedevents) {
				txtres = R.string.saved_events_no_content; 
				imgNoItemsRes = R.drawable.ic_unsaved_event_slider; 
				imgPhoneRes = R.drawable.ic_saved_events_no_content;	
				
			} else {
				txtres = R.string.my_events_events_no_content; 
				imgNoItemsRes = R.drawable.ic_recommended_link_pressed; 
				imgPhoneRes = (loadType == Type.myevents) ?
						R.drawable.ic_my_events_no_content : R.drawable.ic_recommended_events_no_content;							
			}
			
			TextView txtNoContentMsg = (TextView) rltLytNoEvts.findViewById(R.id.txtNoItemsMsg);
			txtNoContentMsg.setText(txtres);
			txtNoContentMsg.setCompoundDrawablesWithIntrinsicBounds(0, imgNoItemsRes, 0, 0);
			
			((ImageView) rltLytNoEvts.findViewById(R.id.imgPhone))
				.setImageDrawable(FragmentUtil.getResources(this).getDrawable(imgPhoneRes));

		} else {
			recyclerVEvents.setVisibility(View.VISIBLE);
			((ImageView) rltLytNoEvts.findViewById(R.id.imgPhone)).setImageDrawable(null);
		}
	}

	@Override
	public void onVisible() {
		if (rvMyEventsAdapterTab != null) {
			rvMyEventsAdapterTab.setVisible(true);
			/**
			 * need to call this because it doesn't call onBindViewHolder() automatically if 
			 * next or previous tab is selected. Calls it only for tab selection changing from position 1 to 3 or 
			 * 3 to 1
			 */
			rvMyEventsAdapterTab.notifyDataSetChanged();
		}
	}

	@Override
	public void onInvisible() {
		if (rvMyEventsAdapterTab != null) {
			rvMyEventsAdapterTab.setVisible(false);
			rvMyEventsAdapterTab.notifyDataSetChanged();
		}
	}

	@Override
	public void doPositiveClick(String dialogTag) {
		if (AppConstants.DIALOG_FRAGMENT_TAG_EVENT_SAVED.equals(dialogTag)) {
			addEventToCalendar();
		}
	}

	@Override
	public void doNegativeClick(String dialogTag) {

	}
}
