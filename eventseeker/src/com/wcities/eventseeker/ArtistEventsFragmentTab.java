package com.wcities.eventseeker;

import java.util.List;

import android.R.color;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wcities.eventseeker.adapter.DateWiseMyEventListAdapter;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingItemType;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.asynctask.UserTracker;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Schedule;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener;
import com.wcities.eventseeker.interfaces.EventListener;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.DateWiseEventList;
import com.wcities.eventseeker.viewdata.DateWiseEventList.EventListItem;
import com.wcities.eventseeker.viewdata.DateWiseEventList.LIST_ITEM_TYPE;

public class ArtistEventsFragmentTab extends ArtistEventsParentFragment {

	private static final String TAG = ArtistEventsFragmentTab.class.getName();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return super.onCreateView(inflater, container, savedInstanceState);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	protected void updateFollowingFooter() {
		fragmentArtistDetailsFooter.setVisibility(View.GONE);
	}
	
	private static class ArtistEventsAdapter extends BaseAdapter implements DateWiseEventParentAdapterListener{

		private LayoutInflater mInflater;
		private BitmapCache bitmapCache;
		private DateWiseEventList dateWiseEventList;
		private boolean isTablet;
		private Resources res;
		private Context context;
		private static final String TAG_CONTENT = "content";
		private static final String TAG_HEADER = "header";
		
		public ArtistEventsAdapter(Context context, List<Event> list) {

			mInflater = LayoutInflater.from(context);
			bitmapCache = BitmapCache.getInstance();
			isTablet = ((MainActivity)context).isTablet();
			res = context.getResources();
			dateWiseEventList = new DateWiseEventList();
			this.context = context;
			setDataSet(list);
		}
		
		@Override
		public void setDataSet(List<Event> list) {
			dateWiseEventList.addEventListItems(list, null);
		}

		@Override
		public int getCount() {
			return dateWiseEventList.getCount();
		}

		@Override
		public Object getItem(int position) {
			return dateWiseEventList.getItem(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			EventListItem eventListItem = dateWiseEventList.getItem(position);
				
			if (dateWiseEventList.getItemViewType(position) == LIST_ITEM_TYPE.CONTENT) {

			if(convertView == null || !convertView.getTag().equals(TAG_CONTENT)) {
				
				if (isTablet) {
					convertView = mInflater.inflate(R.layout.fragment_my_events_list_item_tab, null);
				} else {
					convertView = mInflater.inflate(R.layout.fragment_discover_by_category_list_item_evt, null);
				}
				convertView.setTag(TAG_CONTENT);
			}	

			final Event event = eventListItem.getEvent();
			((TextView) convertView.findViewById(R.id.txtEvtTitle)).setText(event.getName());
			
			if (event.getSchedule() != null) {
				Schedule schedule = event.getSchedule();

				if (schedule.getDates().get(0).isStartTimeAvailable()) {
					String[] timeInArray = 
						ConversionUtil.getTimeInArray(schedule.getDates().get(0).getStartDate());

						((TextView) convertView.findViewById(R.id.txtEvtTime)).setText(timeInArray[0]);
						((TextView) convertView.findViewById(R.id.txtEvtTimeAMPM)).setText(" " + timeInArray[1]);
						convertView.findViewById(R.id.imgEvtTime).setVisibility(View.VISIBLE);

					} else {
						((TextView) convertView.findViewById(R.id.txtEvtTime)).setText("");
						((TextView) convertView.findViewById(R.id.txtEvtTimeAMPM)).setText("");
						convertView.findViewById(R.id.imgEvtTime).setVisibility(View.INVISIBLE);
					}
						((TextView) convertView.findViewById(R.id.txtEvtLocation)).setText(schedule.getVenue().getName());
				}

				ImageView imgVenue = (ImageView) convertView.findViewById(R.id.imgEvent);
				Venue venue = event.getSchedule().getVenue();
				String key = venue.getKey(ImgResolution.LOW);
				
				Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
				if (bitmap != null) {
					imgVenue.setImageBitmap(bitmap);
				} else {
					imgVenue.setImageBitmap(null);
					AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
					asyncLoadImg.loadImg(imgVenue, ImgResolution.LOW, venue);
				}

					
				LinearLayout lnrLayoutTickets = (LinearLayout) convertView.findViewById(R.id.lnrLayoutTickets);

				/**
				 * Using super class TextView instead of Button since some
				 * layouts have Button & others have TextView.
				 */

				TextView btnBuyTickets = (TextView) convertView.findViewById(R.id.btnBuyTickets);
				
				CheckBox chkBoxTickets = (CheckBox) convertView.findViewById(R.id.chkBoxTickets);

				final boolean doesBookingUrlExist = (event.getSchedule() != null 
						&& !event.getSchedule().getBookingInfos().isEmpty() 
						&& event.getSchedule().getBookingInfos().get(0).getBookingUrl() != null) ? true : false;

				lnrLayoutTickets.setEnabled(doesBookingUrlExist);

				if (doesBookingUrlExist) {
					btnBuyTickets.setTextColor(res.getColor(color.black));
					
					// Only some layouts use imgBuyTickets in place of
					// button drawable for btnBuyTickets.
					
					if (chkBoxTickets == null) {
						btnBuyTickets.setCompoundDrawablesWithIntrinsicBounds(
								res.getDrawable(R.drawable.tickets_grey), null, null, null);

					} else {
						chkBoxTickets.setButtonDrawable(R.drawable.tickets_grey);
					}

				} else {
					btnBuyTickets.setTextColor(res.getColor(R.color.btn_buy_tickets_disabled_txt_color));
					
					// Only some layouts use imgBuyTickets in place of button drawable for btnBuyTickets.
					
					if (chkBoxTickets == null) {
						btnBuyTickets.setCompoundDrawablesWithIntrinsicBounds(
								res.getDrawable(R.drawable.tickets_disabled), null, null, null);

					} else {
						chkBoxTickets.setButtonDrawable(R.drawable.tickets_disabled);
					}
				}

				convertView.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View arg0) {
						if (doesBookingUrlExist) {
							Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(
									event.getSchedule().getBookingInfos().get(0).getBookingUrl()));
							
							context.startActivity(browserIntent);
						}
					}
				});

				final CheckBox chkBoxGoing = (CheckBox) convertView.findViewById(R.id.chkBoxGoing);
				final CheckBox chkBoxWantToGo = (CheckBox) convertView.findViewById(R.id.chkBoxWantToGo);
				
				updateAttendingChkBoxes(event, chkBoxGoing, chkBoxWantToGo);

				OnClickListener goingClickListener = new OnClickListener() {

					@Override
					public void onClick(View v) {
						onChkBoxClick(event, chkBoxGoing, chkBoxWantToGo, true);
					}
				};

				OnClickListener wantToClickListener = new OnClickListener() {

					@Override
					public void onClick(View v) {
						onChkBoxClick(event, chkBoxGoing, chkBoxWantToGo, false);
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

				convertView.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
						((EventListener)context).onEventSelected(event);
					}
				});

			} else {
				
				if(convertView == null || !convertView.getTag().equals(TAG_HEADER)) {
					convertView = mInflater.inflate(R.layout.fragment_discover_by_category_list_item_header, null);
				}
				
				((TextView) convertView.findViewById(R.id.txtDate)).setText(eventListItem.getDate());
				
				int visibility = (position == 0) ? View.INVISIBLE : View.VISIBLE;
				convertView.findViewById(R.id.divider1).setVisibility(visibility);
				convertView.setTag(TAG_HEADER);
			}

			return convertView;
			
		}
		
		private void onChkBoxClick(Event event, CheckBox chkBoxGoing, CheckBox chkBoxWantToGo, boolean isGoingClicked) {
			Event.Attending attending;
			if (isGoingClicked) {
				attending = event.getAttending() == Event.Attending.GOING ? Event.Attending.NOT_GOING
						: Event.Attending.GOING;

			} else {
				attending = event.getAttending() == Event.Attending.WANTS_TO_GO ? Event.Attending.NOT_GOING
						: Event.Attending.WANTS_TO_GO;
			}

			event.setAttending(attending);
			updateAttendingChkBoxes(event, chkBoxGoing, chkBoxWantToGo);
			new UserTracker((EventSeekr) context.getApplicationContext(), UserTrackingItemType.event,
					event.getId(), event.getAttending().getValue(),
					UserTrackingType.Add).execute();
		}

		private void updateAttendingChkBoxes(Event event, CheckBox chkBoxGoing,
				CheckBox chkBoxWantToGo) {
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
		public int getEventsAlreadyRequested() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void setMoreDataAvailable(boolean isMoreDataAvailable) {
			// TODO Auto-generated method stub
		}

		@Override
		public void setEventsAlreadyRequested(int eventsAlreadyRequested) {
			// TODO Auto-generated method stub
		}

		@Override
		public void updateContext(Context context) {
			// TODO Auto-generated method stub
		}

		@Override
		public void setLoadDateWiseEvents(AsyncTask<Void, Void, List<Event>> loadDateWiseEvents) {
			// TODO Auto-generated method stub
		}

	}

	@Override
	protected DateWiseEventParentAdapterListener getAdapterInstance() {
		return new DateWiseMyEventListAdapter(FragmentUtil.getActivity(this), dateWiseEventList, null, this);
	}
	
}