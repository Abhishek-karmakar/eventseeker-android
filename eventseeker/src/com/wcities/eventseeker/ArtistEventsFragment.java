package com.wcities.eventseeker;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
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
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		artist = (Artist) getArguments().getSerializable(BundleKeys.ARTIST);
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
				view = LayoutInflater.from(FragmentUtil.getActivity(this)).inflate(R.layout.fragment_discover_by_category_list_item_evt, null);

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
				String key = venue.getKey(ImgResolution.MOBILE);
				Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
				if (bitmap != null) {
			        imgVenue.setImageBitmap(bitmap);
			        
			    } else {
			    	imgVenue.setImageBitmap(null);
			    	AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
			        asyncLoadImg.loadImg(imgVenue, ImgResolution.MOBILE, venue);
			    }
				
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
		if (artist.getAttending() == null || ((EventSeekr)FragmentUtil.getActivity(this).getApplication()).getWcitiesId() == null) {
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
}
