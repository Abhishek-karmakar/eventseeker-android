package com.wcities.eventseeker.adapter;

import java.util.List;

import android.R.color;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.Session;
import com.facebook.SessionState;
import com.wcities.eventseeker.GeneralDialogFragment.DialogBtnClickListener;
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
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Event.Attending;
import com.wcities.eventseeker.core.Schedule;
import com.wcities.eventseeker.custom.fragment.PublishEventListFragment;
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener;
import com.wcities.eventseeker.interfaces.EventListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.interfaces.PublishListener;
import com.wcities.eventseeker.interfaces.ReplaceFragmentListener;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.FbUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.DateWiseEventList;
import com.wcities.eventseeker.viewdata.DateWiseEventList.EventListItem;
import com.wcities.eventseeker.viewdata.DateWiseEventList.LIST_ITEM_TYPE;

public class DateWiseMyEventListAdapter extends BaseAdapter implements DateWiseEventParentAdapterListener{

	private static final String TAG = DateWiseMyEventListAdapter.class.getName();
	private static final int MAX_FB_CALL_COUNT_FOR_SAME_EVT = 20;
	
	private Context mContext;
	private BitmapCache bitmapCache;
	private DateWiseEventList dateWiseEvtList;
	private AsyncTask<Void, Void, List<Event>> loadDateWiseMyEvents;
	private int eventsAlreadyRequested;
	private boolean isMoreDataAvailable = true;
	private LoadItemsInBackgroundListener mListener;
	private int orientation;
	private LayoutParams lpImgEvtPort;
	private boolean isTablet;
	private String wcitiesId, googleAnalyticsScreenName;
	
	// vars for fb event publish
	private PublishListener fbPublishListener;
	private int fbCallCountForSameEvt = 0;
	
	private Event eventPendingPublish;
	private CheckBox eventPendingPublishChkBoxGoing, eventPendingPublishChkBoxWantToGo;
	private DialogBtnClickListener listener;
	
	public DateWiseMyEventListAdapter(Context context, DateWiseEventList dateWiseEvtList,
			AsyncTask<Void, Void, List<Event>> loadDateWiseEvents, LoadItemsInBackgroundListener mListener, 
			PublishListener fbPublishListener, String googleAnalyticsScreenName, DialogBtnClickListener listener) {
		
		mContext = context;
		this.listener = listener;
		
		bitmapCache = BitmapCache.getInstance();
		this.dateWiseEvtList = dateWiseEvtList;
		this.loadDateWiseMyEvents = loadDateWiseEvents;
		this.mListener = mListener;
		orientation = mContext.getResources().getConfiguration().orientation;

		DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
		int width = dm.widthPixels < dm.heightPixels ? dm.widthPixels : dm.heightPixels;
		int widthPort = width 
				- (mContext.getResources().getDimensionPixelSize(
						R.dimen.tab_bar_margin_fragment_custom_tabs) * 2)
				- (mContext.getResources().getDimensionPixelSize(
						R.dimen.img_event_margin_fragment_my_events_list_item) * 2);
		lpImgEvtPort = new LayoutParams(widthPort, widthPort * 3 / 4);
		lpImgEvtPort.topMargin = lpImgEvtPort.leftMargin = lpImgEvtPort.rightMargin = mContext
				.getResources().getDimensionPixelSize(
						R.dimen.img_event_margin_fragment_my_events_list_item);
		isTablet = ((EventSeekr) mContext.getApplicationContext()).isTablet();
		
		this.fbPublishListener = fbPublishListener;
		wcitiesId = ((EventSeekr)mContext.getApplicationContext()).getWcitiesId();
		this.googleAnalyticsScreenName = googleAnalyticsScreenName;
	}

	@Override
	public void updateContext(Context context) {
		mContext = context;
		orientation = mContext.getResources().getConfiguration().orientation;
	}

	@Override
	public void setLoadDateWiseEvents(AsyncTask<Void, Void, List<Event>> loadDateWiseMyEvents) {
		this.loadDateWiseMyEvents = loadDateWiseMyEvents;
	}

	@Override
	public int getViewTypeCount() {
		return LIST_ITEM_TYPE.values().length;
	}

	@Override
	public int getItemViewType(int position) {
		return dateWiseEvtList.getItemViewType(position).ordinal();
	}

	@Override
	public int getCount() {
		return dateWiseEvtList.getCount();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		//Log.d(TAG, "pos = " + position + ", item view type = " + getItemViewType(position));
		if (getItemViewType(position) == LIST_ITEM_TYPE.PROGRESS.ordinal()) {

			if (convertView == null	|| convertView.getTag() != LIST_ITEM_TYPE.PROGRESS) {
				convertView = LayoutInflater.from(mContext).inflate(R.layout.list_progress_bar, null);
				convertView.setTag(LIST_ITEM_TYPE.PROGRESS);
			}
			
			/*if (isMoreDataAvailable) {
				if (loadDateWiseMyEvents == null || loadDateWiseMyEvents.getStatus() == Status.FINISHED) {
					mListener.loadEventsInBackground();
				}
				
			} else {
				TextView txtNoData = (TextView)convertView.findViewById(R.id.txtNoData);
				txtNoData.setText("No events found.");
				txtNoData.setVisibility(View.VISIBLE);
				convertView.findViewById(R.id.progressBar).setVisibility(View.GONE);
			}*/
			
			if ((loadDateWiseMyEvents == null || loadDateWiseMyEvents.getStatus() == Status.FINISHED) 
					&& isMoreDataAvailable) {
				mListener.loadItemsInBackground();
			}

		} else if (getItemViewType(position) == LIST_ITEM_TYPE.NO_EVENTS.ordinal()) {

			final Event event = getItem(position).getEvent();

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
			} 
			
		} else if (getItemViewType(position) == LIST_ITEM_TYPE.CONTENT.ordinal()) {

			if (convertView == null	|| convertView.getTag() != LIST_ITEM_TYPE.CONTENT) {
				convertView = LayoutInflater.from(mContext).inflate(R.layout.fragment_my_events_list_item, null);
				convertView.setTag(LIST_ITEM_TYPE.CONTENT);
			}

			final Event event = getItem(position).getEvent();
			((TextView) convertView.findViewById(R.id.txtEvtTitle)).setText(event.getName());

			if (event.getSchedule() != null) {
				Schedule schedule = event.getSchedule();

				if (orientation == Configuration.ORIENTATION_LANDSCAPE || isTablet) {
					if (schedule.getDates().get(0).isStartTimeAvailable()) {
						String time = ConversionUtil.getTime(schedule.getDates().get(0).getStartDate());

						((TextView) convertView.findViewById(R.id.txtEvtTime)).setText(time);
						convertView.findViewById(R.id.imgEvtTime).setVisibility(View.VISIBLE);

					} else {
						((TextView) convertView.findViewById(R.id.txtEvtTime)).setText("");
						convertView.findViewById(R.id.imgEvtTime).setVisibility(View.INVISIBLE);
					}
				}
				String cityName = ""; 
				if (event.getCityName() != null) {
					cityName = ", " + event.getCityName();
				}
				TextView txtEvtLocation = (TextView) convertView.findViewById(R.id.txtEvtLocation);
				String venueName = (schedule.getVenue() != null) ? schedule.getVenue().getName() : "";
				txtEvtLocation.setText(venueName + cityName);
			}

			ImageView imgEvent = (ImageView) convertView.findViewById(R.id.imgEvent);
			if(!isTablet) {
				if (orientation == Configuration.ORIENTATION_PORTRAIT) {
					imgEvent.setLayoutParams(lpImgEvtPort);
				}
			}

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

			LinearLayout lnrLayoutTickets = (LinearLayout) convertView.findViewById(R.id.lnrLayoutTickets);
			/**
			 * Using super class TextView instead of Button since some layouts have Button & 
			 * others have TextView.
			 */
			TextView btnBuyTickets = (TextView) convertView.findViewById(R.id.btnBuyTickets);
			CheckBox chkBoxTickets = (CheckBox) convertView.findViewById(R.id.chkBoxTickets);
			
			final boolean doesBookingUrlExist = (event.getSchedule() != null && !event.getSchedule().getBookingInfos().isEmpty() 
					&& event.getSchedule().getBookingInfos().get(0).getBookingUrl() != null) ? true : false;
			lnrLayoutTickets.setEnabled(doesBookingUrlExist);
			
			if (doesBookingUrlExist) {
				btnBuyTickets.setTextColor(mContext.getResources().getColor(color.black));
				// Only some layouts use imgBuyTickets in place of button drawable for btnBuyTickets.
				if (chkBoxTickets == null) {
					btnBuyTickets.setCompoundDrawablesWithIntrinsicBounds(mContext.getResources().getDrawable(
							R.drawable.tickets_grey), null, null, null);
					
				} else {
					chkBoxTickets.setButtonDrawable(R.drawable.tickets_grey);
				}

			} else {
				btnBuyTickets.setTextColor(mContext.getResources().getColor(R.color.btn_buy_tickets_disabled_txt_color));
				// Only some layouts use imgBuyTickets in place of button drawable for btnBuyTickets.
				if (chkBoxTickets == null) {
					btnBuyTickets.setCompoundDrawablesWithIntrinsicBounds(mContext.getResources().getDrawable(
							R.drawable.tickets_disabled), null, null, null);
					
				} else {
					chkBoxTickets.setButtonDrawable(R.drawable.tickets_disabled);
				}
			}
			
			lnrLayoutTickets.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					if (doesBookingUrlExist) {
						Bundle args = new Bundle();
						args.putString(BundleKeys.URL, event.getSchedule().getBookingInfos().get(0).getBookingUrl());
						((ReplaceFragmentListener)mContext).replaceByFragment(
								AppConstants.FRAGMENT_TAG_WEB_VIEW, args);
						/**
						 * added on 15-12-2014
						 */
						GoogleAnalyticsTracker.getInstance().sendEvent((EventSeekr)mContext.getApplicationContext(), 
								googleAnalyticsScreenName, GoogleAnalyticsTracker.EVENT_LABEL_TICKETS_BUTTON, 
								com.wcities.eventseeker.analytics.GoogleAnalyticsTracker.Type.Event.name(), 
								null, event.getId());
					}
				}
			});

			final CheckBox chkBoxGoing = (CheckBox) convertView.findViewById(R.id.chkBoxGoing);
			final CheckBox chkBoxWantToGo = (CheckBox) convertView.findViewById(R.id.chkBoxWantToGo);
			updateAttendingChkBoxes(event, chkBoxGoing, chkBoxWantToGo);

			OnClickListener goingClickListener = new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					if (wcitiesId != null) {
						onChkBoxClick(event, chkBoxGoing, chkBoxWantToGo, true);
						
					} else {
						chkBoxGoing.setChecked(false);
						Toast.makeText(mContext, mContext.getString(R.string.pls_login_to_track_evt), 
								Toast.LENGTH_LONG).show();
					}
				}
			};
			OnClickListener wantToClickListener = new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					if (wcitiesId != null) {
						onChkBoxClick(event, chkBoxGoing, chkBoxWantToGo, false);		
						
					} else {
						chkBoxWantToGo.setChecked(false);
						Toast.makeText(mContext, mContext.getString(R.string.pls_login_to_track_evt), 
								Toast.LENGTH_LONG).show();
					}
				}
			};
			
			chkBoxGoing.setOnClickListener(goingClickListener);
			chkBoxWantToGo.setOnClickListener(wantToClickListener);
			
			TextView txtGoing = (TextView) convertView.findViewById(R.id.txtGoing);
			TextView txtWantTo = (TextView) convertView.findViewById(R.id.txtWantTo);
			if (txtGoing != null) {
				txtGoing.setOnClickListener(goingClickListener);
				txtWantTo.setOnClickListener(wantToClickListener);
			}
			
			/**
			 * If user clicks on going or wantToGo & changes orientation instantly before call to 
			 * onPublishPermissionGranted(), then we need to update both checkboxes with right 
			 * checkbox pointers in new orientation
			 */
			if (eventPendingPublish == event) {
				eventPendingPublishChkBoxGoing = chkBoxGoing;
				eventPendingPublishChkBoxWantToGo = chkBoxWantToGo;
			}

			convertView.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					((EventListener) mContext).onEventSelected(event);
				}
			});

		} else if (getItemViewType(position) == LIST_ITEM_TYPE.HEADER.ordinal()) {
			if (convertView == null || convertView.getTag() != LIST_ITEM_TYPE.HEADER) {
				convertView = LayoutInflater.from(mContext).inflate(
						R.layout.fragment_discover_by_category_list_item_header, null);
				convertView.setTag(LIST_ITEM_TYPE.HEADER);
			}
			((TextView) convertView.findViewById(R.id.txtDate)).setText(getItem(position).getDate());
			int visibility = (position == 0) ? View.INVISIBLE : View.VISIBLE;
			View view = convertView.findViewById(R.id.divider1);
			view.setVisibility(visibility);
			if (((EventSeekr) mContext.getApplicationContext()).isTablet()) {
				view.setBackgroundResource(R.color.dark_gray);
			}
		}

		return convertView;
	}
	
	private void onChkBoxClick(Event event, CheckBox chkBoxGoing, CheckBox chkBoxWantToGo, boolean isGoingClicked) {
		Attending attending;
		if (isGoingClicked) {
			attending = event.getAttending() == Attending.GOING ? Attending.NOT_GOING : Attending.GOING;
			
		} else {
			attending = event.getAttending() == Attending.WANTS_TO_GO ? Attending.NOT_GOING : Attending.WANTS_TO_GO;
		}
		
		if (attending == Attending.NOT_GOING) {
			event.setAttending(attending);
			updateAttendingChkBoxes(event, chkBoxGoing, chkBoxWantToGo);
			new UserTracker(Api.OAUTH_TOKEN, (EventSeekr) mContext.getApplicationContext(), UserTrackingItemType.event, 
					event.getId(), event.getAttending().getValue(), UserTrackingType.Add).execute();
			
		} else {
			EventSeekr eventSeekr = (EventSeekr) mContext.getApplicationContext();

			/**
			 * call to updateAttendingChkBoxes() to negate the click event for now on checkbox, 
			 * since it's handled after checking fb/google publish permission
			 */
			updateAttendingChkBoxes(event, chkBoxGoing, chkBoxWantToGo);
			
			if (eventSeekr.getFbUserId() != null) {
				event.setNewAttending(attending);
				eventPendingPublish = event;
				eventPendingPublishChkBoxGoing = chkBoxGoing;
				eventPendingPublishChkBoxWantToGo = chkBoxWantToGo;
				fbCallCountForSameEvt = 0;
				FbUtil.handlePublishEvent(fbPublishListener, (Fragment) mListener, AppConstants.PERMISSIONS_FB_PUBLISH_EVT_OR_ART, 
						AppConstants.REQ_CODE_FB_PUBLISH_EVT_OR_ART, event);
				
			} else if (eventSeekr.getGPlusUserId() != null) {
				event.setNewAttending(attending);
				eventPendingPublish = event;
				eventPendingPublishChkBoxGoing = chkBoxGoing;
				eventPendingPublishChkBoxWantToGo = chkBoxWantToGo;
				((PublishEventListFragment)mListener).setEvent(eventPendingPublish);
				((PublishEventListFragment)mListener).handlePublishEvent();
				
			} else {
				FragmentUtil.showLoginNeededForTrackingEventDialog(mContext, listener);
			}
		}
	}
	
	private void updateAttendingChkBoxes(Event event, CheckBox chkBoxGoing, CheckBox chkBoxWantToGo) {
		switch (event.getAttending()) {

		case GOING:
			chkBoxGoing.setChecked(true);
			chkBoxWantToGo.setChecked(false);
			break;

		case WANTS_TO_GO:
			chkBoxGoing.setChecked(false);
			chkBoxWantToGo.setChecked(true);
			break;

		case NOT_GOING:
			chkBoxGoing.setChecked(false);
			chkBoxWantToGo.setChecked(false);
			break;

		default:
			break;
		}
	}

	@Override
	public EventListItem getItem(int position) {
		return dateWiseEvtList.getItem(position);
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
		updateAttendingChkBoxes(eventPendingPublish, eventPendingPublishChkBoxGoing, eventPendingPublishChkBoxWantToGo);
	}
}
