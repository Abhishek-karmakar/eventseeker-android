package com.wcities.eventseeker;

import android.R.color;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wcities.eventseeker.ArtistDetailsFragment.ArtistDetailsFragmentListener;
import com.wcities.eventseeker.ArtistDetailsFragment.FooterTxt;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingItemType;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.asynctask.UserTracker;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Artist.Attending;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Schedule;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.interfaces.EventListener;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.DateWiseEventList;
import com.wcities.eventseeker.viewdata.DateWiseEventList.EventListItem;
import com.wcities.eventseeker.viewdata.DateWiseEventList.LIST_ITEM_TYPE;

public class ArtistEventsFragment extends Fragment implements ArtistDetailsFragmentListener, OnClickListener {

	private static final String TAG = ArtistEventsFragment.class.getName();
	
	private Artist artist;
	
	private LinearLayout lnrLayoutEventList;
	private RelativeLayout fragmentArtistDetailsFooter;
	private ImageView imgFollow;
	private TextView txtFollow;

	private boolean isTablet;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		artist = (Artist) getArguments().getSerializable(BundleKeys.ARTIST);
		isTablet = ((MainActivity)FragmentUtil.getActivity(this)).isTablet();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_artist_events, null);
		
		lnrLayoutEventList =  (LinearLayout) v.findViewById(R.id.lnrLayoutEventList);
		addEvents();
		
		imgFollow = (ImageView) v.findViewById(R.id.imgFollow);
		txtFollow = (TextView) v.findViewById(R.id.txtFollow);
		fragmentArtistDetailsFooter = (RelativeLayout) v.findViewById(R.id.fragmentArtistDetailsFooter);
		fragmentArtistDetailsFooter.setOnClickListener(this);
		
		updateFollowingFooter();
		
		return v;
	}
	
	private void addEvents() {
		lnrLayoutEventList.removeAllViews();
		
		BitmapCache bitmapCache = BitmapCache.getInstance();
		DateWiseEventList dateWiseEventList = new DateWiseEventList();
		dateWiseEventList.addEventListItems(artist.getEvents(), null);
		
		for (int i = 0; i < dateWiseEventList.getCount(); i++) {
			EventListItem eventListItem = dateWiseEventList.getItem(i);
			View view;
			
			if (dateWiseEventList.getItemViewType(i) == LIST_ITEM_TYPE.CONTENT) {
				
				if(isTablet) {					
					view = LayoutInflater.from(FragmentUtil.getActivity(this)).inflate(R.layout.fragment_my_events_list_item_tab, null);
				} else {				
					view = LayoutInflater.from(FragmentUtil.getActivity(this)).inflate(R.layout.fragment_discover_by_category_list_item_evt, null);
				}
				
				final Event event = eventListItem.getEvent();
				((TextView)view.findViewById(R.id.txtEvtTitle)).setText(event.getName());
				
				if (event.getSchedule() != null) {
					Schedule schedule = event.getSchedule();
					
					if (schedule.getDates().get(0).isStartTimeAvailable()) {
						String[] timeInArray = ConversionUtil.getTimeInArray(schedule.getDates().get(0).getStartDate());
						
						((TextView)view.findViewById(R.id.txtEvtTime)).setText(timeInArray[0]);
						((TextView)view.findViewById(R.id.txtEvtTimeAMPM)).setText(" " + timeInArray[1]);
						view.findViewById(R.id.imgEvtTime).setVisibility(View.VISIBLE);
						
					} else {
						((TextView)view.findViewById(R.id.txtEvtTime)).setText("");
						((TextView)view.findViewById(R.id.txtEvtTimeAMPM)).setText("");
						view.findViewById(R.id.imgEvtTime).setVisibility(View.INVISIBLE);
					}
					((TextView)view.findViewById(R.id.txtEvtLocation)).setText(schedule.getVenue().getName());
				}
				
				ImageView imgVenue = (ImageView) view.findViewById(R.id.imgEvent);
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
				
				/**
				 * Added code to handle the going to, want to and tickets functionality
				 */
				
				if(isTablet) {
					LinearLayout lnrLayoutTickets = (LinearLayout) view.findViewById(R.id.lnrLayoutTickets);
					/**
					 * Using super class TextView instead of Button since some layouts have Button & 
					 * others have TextView.
					 */
					TextView btnBuyTickets = (TextView) view.findViewById(R.id.btnBuyTickets);
					CheckBox chkBoxTickets = (CheckBox) view.findViewById(R.id.chkBoxTickets);
					
					final boolean doesBookingUrlExist = (event.getSchedule() != null && !event.getSchedule().getBookingInfos().isEmpty() 
							&& event.getSchedule().getBookingInfos().get(0).getBookingUrl() != null) ? true : false;
					lnrLayoutTickets.setEnabled(doesBookingUrlExist);
					
					if (doesBookingUrlExist) {
						btnBuyTickets.setTextColor(getResources().getColor(color.black));
						// Only some layouts use imgBuyTickets in place of button drawable for btnBuyTickets.
						if (chkBoxTickets == null) {
							btnBuyTickets.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(
									R.drawable.tickets_grey), null, null, null);
							
						} else {
							chkBoxTickets.setButtonDrawable(R.drawable.tickets_grey);
						}
	
					} else {
						btnBuyTickets.setTextColor(getResources().getColor(R.color.btn_buy_tickets_disabled_txt_color));
						// Only some layouts use imgBuyTickets in place of button drawable for btnBuyTickets.
						if (chkBoxTickets == null) {
							btnBuyTickets.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(
									R.drawable.tickets_disabled), null, null, null);
							
						} else {
							chkBoxTickets.setButtonDrawable(R.drawable.tickets_disabled);
						}
					}
					
					lnrLayoutTickets.setOnClickListener(new OnClickListener() {
	
						@Override
						public void onClick(View arg0) {
							if (doesBookingUrlExist) {
								Intent browserIntent = new Intent(Intent.ACTION_VIEW,
										Uri.parse(event.getSchedule().getBookingInfos()
												.get(0).getBookingUrl()));
								FragmentUtil.getActivity(ArtistEventsFragment.this).startActivity(browserIntent);
							}
						}
					});
	
					final CheckBox chkBoxGoing = (CheckBox) view.findViewById(R.id.chkBoxGoing);
					final CheckBox chkBoxWantToGo = (CheckBox) view.findViewById(R.id.chkBoxWantToGo);
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
					
					TextView txtGoing = (TextView) view.findViewById(R.id.txtGoing);
					TextView txtWantTo = (TextView) view.findViewById(R.id.txtWantTo);
					if (txtGoing != null) {
						txtGoing.setOnClickListener(goingClickListener);
						txtWantTo.setOnClickListener(wantToClickListener);
					}
				}
				
				/**
				 * till here
				 */
				
				view.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						((EventListener)FragmentUtil.getActivity(ArtistEventsFragment.this)).onEventSelected(event);
					}
				});
				
			} else {
				view = LayoutInflater.from(FragmentUtil.getActivity(this)).inflate(R.layout.fragment_discover_by_category_list_item_header, null);
				((TextView)view.findViewById(R.id.txtDate)).setText(eventListItem.getDate());
				int visibility = (i == 0) ? View.INVISIBLE : View.VISIBLE;
				view.findViewById(R.id.divider1).setVisibility(visibility);
			}
			
			lnrLayoutEventList.addView(view);
		}
	}
	
	private void updateFollowingFooter() {
		if (artist.getAttending() == null || ((EventSeekr)FragmentUtil.getActivity(this).getApplication()).getWcitiesId() == null || isTablet) {
			fragmentArtistDetailsFooter.setVisibility(View.GONE);
			
		} else {
			fragmentArtistDetailsFooter.setVisibility(View.VISIBLE);
			
			switch (artist.getAttending()) {

			case Tracked:
				imgFollow.setImageDrawable(getResources().getDrawable(R.drawable.following));
				txtFollow.setText(FooterTxt.Following.name());
				break;

			case NotTracked:
				imgFollow.setImageDrawable(getResources().getDrawable(R.drawable.follow));
				txtFollow.setText(FooterTxt.Follow.name());
				break;

			default:
				break;
			}
		}
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.fragmentArtistDetailsFooter:
			if (txtFollow.getText().equals(FooterTxt.Follow.name())) {
				artist.setAttending(Attending.Tracked);
				updateFollowingFooter();
				new UserTracker((EventSeekr)FragmentUtil.getActivity(this).getApplication(), UserTrackingItemType.artist, artist.getId()).execute();
				
			} else {
				artist.setAttending(Attending.NotTracked);
				updateFollowingFooter();
				new UserTracker((EventSeekr)FragmentUtil.getActivity(this).getApplication(), UserTrackingItemType.artist, artist.getId(), 
						Attending.NotTracked.getValue(), UserTrackingType.Edit).execute();
			}
			((ArtistDetailsFragment) getParentFragment()).onArtistFollowingUpdated();
			break;
			
		default:
			break;
		}
	}

	@Override
	public void onArtistUpdatedByArtistDetailsFragment() {
		updateFollowingFooter();
		addEvents();
	}

	@Override
	public void onArtistFollowingUpdated() {
		updateFollowingFooter();
	}
	

	private void onChkBoxClick(Event event, CheckBox chkBoxGoing, CheckBox chkBoxWantToGo, boolean isGoingClicked) {
		Event.Attending attending;
		if (isGoingClicked) {
			attending = event.getAttending() == Event.Attending.GOING ? Event.Attending.NOT_GOING : Event.Attending.GOING;
			
		} else {
			attending = event.getAttending() == Event.Attending.WANTS_TO_GO ? Event.Attending.NOT_GOING : Event.Attending.WANTS_TO_GO;
		}
		
		event.setAttending(attending);
		updateAttendingChkBoxes(event, chkBoxGoing, chkBoxWantToGo);
		new UserTracker((EventSeekr) FragmentUtil.getActivity(this).getApplicationContext(), UserTrackingItemType.event, 
				event.getId(), event.getAttending().getValue(), UserTrackingType.Add).execute();
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

}
