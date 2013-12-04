package com.wcities.eventseeker;

import java.util.Iterator;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wcities.eventseeker.EventDetailsFragment.EventDetailsFragmentChildListener;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingItemType;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.asynctask.UserTracker;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Event.Attending;
import com.wcities.eventseeker.core.Schedule;
import com.wcities.eventseeker.interfaces.ArtistListener;
import com.wcities.eventseeker.util.FragmentUtil;

public class EventFeaturingFragment extends Fragment implements OnClickListener, EventDetailsFragmentChildListener {
	
	private static final String TAG = EventFeaturingFragment.class.getName();
	
	private Event event;
	
	private LinearLayout lnrLayoutArtistList;
	private LinearLayout lnrLayoutTickets;
	private CheckBox chkBoxGoing, chkBoxWantToGo;
	private Button btnBuyTickets;

	private boolean isTablet;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		event = (Event) getArguments().getSerializable(BundleKeys.EVENT);
	
		isTablet = ((MainActivity)FragmentUtil.getActivity(this)).isTablet();
	
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_event_featuring, null);
		
		lnrLayoutTickets = (LinearLayout) v.findViewById(R.id.lnrLayoutTickets);
		btnBuyTickets = (Button) v.findViewById(R.id.btnBuyTickets);
		lnrLayoutTickets.setOnClickListener(this);
		
		updateEventScheduleVisibility();
		
		lnrLayoutArtistList =  (LinearLayout) v.findViewById(R.id.lnrLayoutArtistList);
		addFeaturingArtists();
		
		chkBoxGoing = (CheckBox) v.findViewById(R.id.chkBoxGoing);
		chkBoxWantToGo = (CheckBox) v.findViewById(R.id.chkBoxWantToGo);

		if (((EventSeekr)FragmentUtil.getActivity(this).getApplication()).getWcitiesId() != null) {
			chkBoxGoing.setOnClickListener(this);
			chkBoxWantToGo.setOnClickListener(this);
			
			updateAttendingChkBoxes();
			
		} else {
			chkBoxGoing.setEnabled(false);
			chkBoxWantToGo.setEnabled(false);
		}
		
		return v;
	}
	
	private void updateBtnBuyTicketsEnabled(boolean enabled) {
		lnrLayoutTickets.setEnabled(enabled);
		if (enabled) {
			if(isTablet) {
				btnBuyTickets.setTextColor(getResources().getColor(R.color.txt_color_include_fragment_event_details_footer_tab));
				btnBuyTickets.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.tic_blue), null, null, null);				
			} else {
				btnBuyTickets.setTextColor(getResources().getColor(android.R.color.white));
				btnBuyTickets.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.tickets), null, null, null);
			}
		} else {
			btnBuyTickets.setTextColor(getResources().getColor(R.color.btn_buy_tickets_disabled_txt_color));
			btnBuyTickets.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.tickets_disabled), null, null, 
					null);
		}
	}
	
	private void updateEventScheduleVisibility() {
		Schedule schedule = event.getSchedule();
		if (schedule == null || schedule.getBookingInfos().isEmpty()) {
			updateBtnBuyTicketsEnabled(false);
		}
	}
	
	private void updateAttendingChkBoxes() {
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

		default:
			break;
		}
	}
	
	private void addFeaturingArtists() {
		//Log.d(TAG, "addFeaturingArtists()");
		lnrLayoutArtistList.removeAllViews();
		
		BitmapCache bitmapCache = BitmapCache.getInstance();
		for (Iterator<Artist> iterator = event.getArtists().iterator(); iterator.hasNext();) {
			final Artist artist = (Artist) iterator.next();
			View artistView = LayoutInflater.from(FragmentUtil.getActivity(this)).inflate(R.layout.fragment_search_artists_list_item, null);

			lnrLayoutArtistList.addView(artistView);
			
			artistView.findViewById(R.id.txtOnTour).setVisibility(View.GONE);
			
			((TextView)artistView.findViewById(R.id.txtArtistName)).setText(artist.getName());
			
			String key = artist.getKey(ImgResolution.LOW);
			Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
			ImageView imageView = (ImageView)artistView.findViewById(R.id.imgItem);
			if (bitmap != null) {
				//Log.d(TAG, "addFeaturingArtists() bitmap != null");
		        imageView.setImageBitmap(bitmap);
		        
		    } else {
		    	//Log.d(TAG, "addFeaturingArtists() bitmap = null");
		        imageView.setImageBitmap(null);
		        AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
		        asyncLoadImg.loadImg(imageView, ImgResolution.LOW, artist);
		    }
			
			artistView.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					((ArtistListener)FragmentUtil.getActivity(EventFeaturingFragment.this)).onArtistSelected(artist);
				}
			});
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.lnrLayoutTickets:
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(event.getSchedule().getBookingInfos()
					.get(0).getBookingUrl()));
			startActivity(browserIntent);
			break;
			
		case R.id.chkBoxGoing:
			Attending attending = event.getAttending() == Attending.GOING ? Attending.NOT_GOING : Attending.GOING;
			event.setAttending(attending);
			updateAttendingChkBoxes();
			new UserTracker((EventSeekr) FragmentUtil.getActivity(this).getApplication(), UserTrackingItemType.event, event.getId(), 
					event.getAttending().getValue(), UserTrackingType.Add).execute();
			((EventDetailsFragment) getParentFragment()).onEventAttendingUpdated();
			break;
			
		case R.id.chkBoxWantToGo:
			attending = event.getAttending() == Attending.WANTS_TO_GO ? Attending.NOT_GOING : Attending.WANTS_TO_GO;
			event.setAttending(attending);
			updateAttendingChkBoxes();
			new UserTracker((EventSeekr) FragmentUtil.getActivity(this).getApplication(), UserTrackingItemType.event, event.getId(), 
					event.getAttending().getValue(), UserTrackingType.Add).execute();
			((EventDetailsFragment) getParentFragment()).onEventAttendingUpdated();
			break;

		default:
			break;
		}
	}

	@Override
	public void onEventUpdatedByEventDetailsFragment() {
		Log.d(TAG, "onEventUpdatedByEventDetailsFragment()");
		updateEventScheduleVisibility();

		addFeaturingArtists();
		
		if (!event.getSchedule().getBookingInfos().isEmpty()) {
			updateBtnBuyTicketsEnabled(true);
		}
		updateAttendingChkBoxes();		
	}

	@Override
	public void onEventAttendingUpdated() {
		updateAttendingChkBoxes();	
	}
}
