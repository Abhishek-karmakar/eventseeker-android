package com.wcities.eventseeker.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.Session;
import com.facebook.SessionState;
import com.melnykov.fab.FloatingActionButton;
import com.wcities.eventseeker.MyEventsListFragment;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.analytics.GoogleAnalyticsTracker;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi.Type;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingItemType;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.asynctask.UserTracker;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Date;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Event.Attending;
import com.wcities.eventseeker.core.Schedule;
import com.wcities.eventseeker.custom.fragment.PublishEventListFragment;
import com.wcities.eventseeker.interfaces.CustomSharedElementTransitionSource;
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener;
import com.wcities.eventseeker.interfaces.EventListener;
import com.wcities.eventseeker.interfaces.FullScrnProgressListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.interfaces.PublishListener;
import com.wcities.eventseeker.interfaces.ReplaceFragmentListener;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.FbUtil;
import com.wcities.eventseeker.util.ViewUtil;
import com.wcities.eventseeker.viewdata.SharedElement;
import com.wcities.eventseeker.viewdata.SharedElementPosition;

import java.util.ArrayList;
import java.util.List;

public class MyEventListAdapter extends BaseAdapter implements DateWiseEventParentAdapterListener {

	private static final String TAG = MyEventListAdapter.class.getName();
	private static final int MAX_FB_CALL_COUNT_FOR_SAME_EVT = 20;
	
	private Context mContext;
	private BitmapCache bitmapCache;
	private List<Event> eventList;
	private AsyncTask<Void, Void, List<Event>> loadMyEvents;
	private int eventsAlreadyRequested;
	private boolean isMoreDataAvailable = true;
	private LoadItemsInBackgroundListener mListener;
	private String /*wcitiesId,*/ googleAnalyticsScreenName;
	
	// vars for fb event publish
	private PublishListener fbPublishListener;
	private int fbCallCountForSameEvt = 0;
	
	private Event eventPendingPublish;
	private FloatingActionButton eventPendingPublishFabSave;
	
	private CustomSharedElementTransitionSource customSharedElementTransitionSource;

	private OnNoEventsListener onNoEventsListener;
	
	public interface OnNoEventsListener {
		public void onNoEventsFound();
	}
	
	private static enum ViewType {
		PROGRESS, NO_EVENTS, CONTENT
	}
	
	public MyEventListAdapter(Context context, List<Event> eventList,
			AsyncTask<Void, Void, List<Event>> loadMyEvents, LoadItemsInBackgroundListener mListener, 
			PublishListener fbPublishListener, String googleAnalyticsScreenName, OnNoEventsListener onNoEventsListener, 
			CustomSharedElementTransitionSource customSharedElementTransitionSource) {
		
		mContext = context;
		
		bitmapCache = BitmapCache.getInstance();
		this.eventList = eventList;
		this.loadMyEvents = loadMyEvents;
		this.mListener = mListener;
		this.onNoEventsListener = onNoEventsListener;
		
		this.fbPublishListener = fbPublishListener;
		//wcitiesId = ((EventSeekr)mContext.getApplicationContext()).getWcitiesId();
		this.googleAnalyticsScreenName = googleAnalyticsScreenName;
		
		this.customSharedElementTransitionSource = customSharedElementTransitionSource;
	}
	
	@Override
	public void updateContext(Context context) {
		mContext = context;
	}

	@Override
	public void setLoadDateWiseEvents(AsyncTask<Void, Void, List<Event>> loadDateWiseMyEvents) {
		this.loadMyEvents = loadDateWiseMyEvents;
	}

	@Override
	public int getViewTypeCount() {
		return ViewType.values().length;
	}

	@Override
	public int getItemViewType(int position) {
		if (eventList.get(position) == null) {
			return ViewType.PROGRESS.ordinal();								
				
		} else if (eventList.get(position).getId() == AppConstants.INVALID_ID) {
			return ViewType.NO_EVENTS.ordinal();
		
		} else {
			return ViewType.CONTENT.ordinal();
		}
	}

	@Override
	public int getCount() {
		return eventList.size();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		//Log.d(TAG, "pos = " + position + ", item view type = " + getItemViewType(position));
		if (getItemViewType(position) == ViewType.PROGRESS.ordinal()) {

			if (convertView == null	|| convertView.getTag() != ViewType.PROGRESS) {
				convertView = LayoutInflater.from(mContext).inflate(R.layout.progress_bar_eventseeker_fixed_ht, null);
				convertView.setTag(ViewType.PROGRESS);
			}
			
			if (eventList.size() == 1) {
				// Instead of this limited height progress bar, we display full screen progress bar from fragment
				convertView.setVisibility(View.INVISIBLE);
				if (mListener instanceof FullScrnProgressListener) {
					((FullScrnProgressListener) mListener).displayFullScrnProgress();
				}
				
			} else {
				convertView.setVisibility(View.VISIBLE);
			}
			
			if ((loadMyEvents == null || loadMyEvents.getStatus() == Status.FINISHED) && isMoreDataAvailable) {
				mListener.loadItemsInBackground();
			}

		} else if (getItemViewType(position) == ViewType.NO_EVENTS.ordinal()) {
			final Event event = getItem(position);

			if (event.getId() == AppConstants.INVALID_ID) {
				convertView = LayoutInflater.from(mContext).inflate(R.layout.list_no_items_found, null);
				if (mListener instanceof MyEventsListFragment && 
						((EventSeekr)mContext.getApplicationContext()).getWcitiesId() == null) {
					Type loadType = ((MyEventsListFragment)mListener).getLoadType();
					if (loadType == Type.myevents) {
						((TextView)convertView).setText(mContext.getResources().getString(R.string.pls_login_to_see_all_events_you_are_following));
						
					} else if (loadType == Type.recommendedevent) {
						((TextView)convertView).setText(mContext.getResources().getString(R.string.pls_login_to_see_to_see_recommended_events));
						
					} else {
						// fallback condition
						((TextView)convertView).setText(mContext.getResources().getString(R.string.no_event_found));
					}
					
				} else {
					((TextView)convertView).setText(mContext.getResources().getString(R.string.no_event_found));
				}
				convertView.setTag("");
				
				if (onNoEventsListener != null) {
					onNoEventsListener.onNoEventsFound();
				}
			} 
			
		} else if (getItemViewType(position) == ViewType.CONTENT.ordinal()) {

			if (convertView == null	|| convertView.getTag() != ViewType.CONTENT) {
				convertView = LayoutInflater.from(mContext).inflate(R.layout.item_list_my_events, null);
				convertView.setTag(ViewType.CONTENT);
			}

			final Event event = getItem(position);
			((TextView) convertView.findViewById(R.id.txtEvtTitle)).setText(event.getName());

			Schedule schedule = event.getSchedule();
			if (schedule != null) {
				if (schedule.getVenue() != null) {
					TextView txtEvtLocation = (TextView) convertView.findViewById(R.id.txtEvtLocation);
					String venueName = (schedule.getVenue() != null) ? schedule.getVenue().getName() : "";
					txtEvtLocation.setText(venueName);
				}
				
				TextView txtEvtTime = ((TextView) convertView.findViewById(R.id.txtEvtTime));
				if (schedule.getDates().size() > 0) {
					Date date = schedule.getDates().get(0);
					txtEvtTime.setVisibility(View.VISIBLE);
					txtEvtTime.setText(ConversionUtil.getDateTime(mContext.getApplicationContext(),
                            date.getStartDate(), date.isStartTimeAvailable(), true, false, false));
				
				} else {
					txtEvtTime.setVisibility(View.GONE);
					txtEvtTime.setText("");
				}
			}

			final ImageView imgEvent = (ImageView) convertView.findViewById(R.id.imgEvent);

			BitmapCacheable bitmapCacheable = null;
			/**
			 * added this try catch as if event will not have valid url and schedule object then
			 * the below line may cause NullPointerException. So, added the try-catch and added the
			 * null check for bitmapCacheable on following statements.
			 */
			try {
				bitmapCacheable = event.doesValidImgUrlExist() ? event : event.getSchedule().getVenue(); 
				
			} catch (NullPointerException e) {
				e.printStackTrace();
			}
			
			if (bitmapCacheable != null) {
				String key = bitmapCacheable.getKey(ImgResolution.LOW);
				Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
				if (bitmap != null) {
					imgEvent.setImageBitmap(bitmap);
					
				} else {
					imgEvent.setImageBitmap(null);
					AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
					asyncLoadImg.loadImg(
							(ImageView) convertView.findViewById(R.id.imgEvent),
							ImgResolution.LOW, (AdapterView) parent, position, bitmapCacheable);
				}
			}

			final boolean doesBookingUrlExist = (event.getSchedule() != null && !event.getSchedule().getBookingInfos().isEmpty() 
					&& event.getSchedule().getBookingInfos().get(0).getBookingUrl() != null) ? true : false;
			
			FloatingActionButton fabTickets = (FloatingActionButton) convertView.findViewById(R.id.fabTickets);
			fabTickets.setEnabled(doesBookingUrlExist);
			fabTickets.setImageResource(doesBookingUrlExist ? R.drawable.ic_ticket_available_floating_mini 
					: R.drawable.ic_ticket_unavailable_floating_mini);
			fabTickets.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					if (doesBookingUrlExist) {
						Bundle args = new Bundle();
						args.putString(BundleKeys.URL, event.getSchedule().getBookingInfos().get(0).getBookingUrl()
                            + "&lang=" + ((EventSeekr) mContext.getApplicationContext()).getLocale().getLocaleCode());
						((ReplaceFragmentListener) mContext).replaceByFragment(AppConstants.FRAGMENT_TAG_WEB_VIEW, args);
						/**
						 * added on 15-12-2014
						 */
						GoogleAnalyticsTracker.getInstance().sendEvent((EventSeekr) mContext.getApplicationContext(), 
							googleAnalyticsScreenName, GoogleAnalyticsTracker.EVENT_LABEL_TICKETS_BUTTON, 
							com.wcities.eventseeker.analytics.GoogleAnalyticsTracker.Type.Event.name(), null, event.getId());
					}
				}
			});
			
			final FloatingActionButton fabSave = (FloatingActionButton) convertView.findViewById(R.id.fabSave);
			updateAttendingFabSaved(event, fabSave);
			fabSave.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					onSaveClick(event, fabSave);
				}
			});
			
			/**
			 * If user clicks on going or wantToGo or save & changes orientation instantly before call to 
			 * onPublishPermissionGranted(), then we need to update both checkboxes or FAB with right 
			 * checkbox or FAB pointers in new orientation
			 */
			if (eventPendingPublish == event) {
				eventPendingPublishFabSave = fabSave;
			}

			convertView.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					List<SharedElement> sharedElements = new ArrayList<SharedElement>();

					int[] loc = ViewUtil.getLocationOnScreen(v, mContext.getResources());
					SharedElementPosition sharedElementPosition = new SharedElementPosition(0, 
							loc[1], imgEvent.getWidth(), imgEvent.getHeight());
					
					SharedElement sharedElement = new SharedElement(sharedElementPosition, imgEvent);
					sharedElements.add(sharedElement);
					customSharedElementTransitionSource.addViewsToBeHidden(imgEvent);
					
					((EventListener) mContext).onEventSelected(event, sharedElements);

					customSharedElementTransitionSource.onPushedToBackStack();
				}
			});

		}

		return convertView;
	}
	
	private void onSaveClick(Event event, FloatingActionButton fabSave) {
		Attending attending = event.getAttending() == Attending.SAVED ? Attending.NOT_GOING : Attending.SAVED;
		EventSeekr eventSeekr = (EventSeekr) mContext.getApplicationContext();
		if (attending == Attending.NOT_GOING) {
			event.setAttending(attending);
			updateAttendingFabSaved(event, fabSave);
			new UserTracker(Api.OAUTH_TOKEN, eventSeekr, UserTrackingItemType.event, event.getId(), 
					event.getAttending().getValue(), UserTrackingType.Add).execute();
			
		} else {
			/**
			 * call to updateAttendingChkBoxes() to negate the click event for now on checkbox, 
			 * since it's handled after checking fb/google publish permission
			 */
			updateAttendingFabSaved(event, fabSave);
			
			if (eventSeekr.getGPlusUserId() != null) {
				event.setNewAttending(attending);
				eventPendingPublish = event;
				eventPendingPublishFabSave = fabSave;
				((PublishEventListFragment) mListener).setEvent(eventPendingPublish);
				((PublishEventListFragment) mListener).handlePublishEvent();
				
			} else {
				event.setNewAttending(attending);
				eventPendingPublish = event;
				eventPendingPublishFabSave = fabSave;
				fbCallCountForSameEvt = 0;
				FbUtil.handlePublishEvent(fbPublishListener, (Fragment) mListener, 
						AppConstants.PERMISSIONS_FB_PUBLISH_EVT_OR_ART, 
						AppConstants.REQ_CODE_FB_PUBLISH_EVT_OR_ART, event);
			}
		}
	}
	
	private void updateAttendingFabSaved(Event event, FloatingActionButton fabSave) {
		fabSave.setImageResource(event.getAttending() == Attending.SAVED ? 
				R.drawable.ic_saved_event_floating_mini : R.drawable.ic_unsaved_event_floating_mini);
	}

	@Override
	public Event getItem(int position) {
		return eventList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public int getEventsAlreadyRequested() {
		return eventsAlreadyRequested;
	}

	@Override
	public void setEventsAlreadyRequested(int eventsAlreadyRequested) {
		this.eventsAlreadyRequested = eventsAlreadyRequested;
	}

	@Override
	public void setMoreDataAvailable(boolean isMoreDataAvailable) {
		this.isMoreDataAvailable = isMoreDataAvailable;
	}
	
	public void call(Session session, SessionState state, Exception exception) {
		Log.d(TAG, "call()");
		fbCallCountForSameEvt++;
		/**
		 * To prevent infinite loop when network is off & we are calling requestPublishPermissions() of FbUtil.
		 */
		if (fbCallCountForSameEvt < MAX_FB_CALL_COUNT_FOR_SAME_EVT) {
			FbUtil.call(session, state, exception, fbPublishListener, (Fragment) mListener, AppConstants.PERMISSIONS_FB_PUBLISH_EVT_OR_ART, 
	    			AppConstants.REQ_CODE_FB_PUBLISH_EVT_OR_ART, eventPendingPublish);
			
		} else {
			fbCallCountForSameEvt = 0;
			fbPublishListener.setPendingAnnounce(false);
		}
	}
	
	public void onPublishPermissionGranted() {
		updateAttendingFabSaved(eventPendingPublish, eventPendingPublishFabSave);
	}
}
