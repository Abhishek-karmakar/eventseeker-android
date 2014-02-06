package com.wcities.eventseeker.bosch;

import java.text.SimpleDateFormat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.facebook.Session;
import com.facebook.SessionState;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingItemType;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.asynctask.AsyncLoadImg.AsyncLoadImageListener;
import com.wcities.eventseeker.asynctask.LoadEventDetails;
import com.wcities.eventseeker.asynctask.LoadEventDetails.OnEventUpdatedListner;
import com.wcities.eventseeker.asynctask.UserTracker;
import com.wcities.eventseeker.bosch.BoschMainActivity.OnCarStationaryStatusChangedListener;
import com.wcities.eventseeker.bosch.BoschMainActivity.OnDisplayModeChangedListener;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Event.Attending;
import com.wcities.eventseeker.core.Schedule;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.custom.fragment.FbPublishEventLoadableFromBackStack;
import com.wcities.eventseeker.custom.view.ResizableImageView;
import com.wcities.eventseeker.interfaces.ReplaceFragmentListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FbUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class BoschEventDetailsFragment extends FbPublishEventLoadableFromBackStack implements OnClickListener, 
		AsyncLoadImageListener, OnEventUpdatedListner, OnCarStationaryStatusChangedListener, 
		OnDisplayModeChangedListener {

	private static final String TAG = BoschEventDetailsFragment.class.getName();
	
	private ResizableImageView imgItem;
	
	private Event event;
	
	private ProgressBar prgImg, prgDetails;
	
	private int fbCallCountForSameEvt;

	private TextView txtDate, txtVenueName, txtDistance;

	private Button btnFollow, btnArtists, btnArtists2, btnCall, btnInfo, btnMap;

	private boolean isEventLoading;

	private View lnrContent;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (event == null) {
			event = (Event) getArguments().getSerializable(BundleKeys.EVENT);
			isEventLoading = true;
			AsyncTaskUtil.executeAsyncTask(new LoadEventDetails(this, this, event), true);
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		View view = inflater.inflate(R.layout.fragment_bosch_event_details, null);

		lnrContent = view.findViewById(R.id.lnrContent);

		prgImg = (ProgressBar) view.findViewById(R.id.prgImg);
		prgDetails = (ProgressBar) view.findViewById(R.id.prgDetails);

		imgItem = (ResizableImageView) view.findViewById(R.id.imgItem);
		
		txtDate = (TextView) view.findViewById(R.id.txtDate);
		txtVenueName = (TextView) view.findViewById(R.id.txtVenueName);
		txtDistance = (TextView) view.findViewById(R.id.txtDistance);
		
		btnCall = (Button) view.findViewById(R.id.btnCall);
		btnInfo = (Button) view.findViewById(R.id.btnInfo);
		btnMap = (Button) view.findViewById(R.id.btnMap);
		btnFollow = (Button) view.findViewById(R.id.btnFollow);
		btnArtists = (Button) view.findViewById(R.id.btnArtists);
		btnArtists2 = (Button) view.findViewById(R.id.btnArtists2);
		
		btnCall.setOnClickListener(this);
		btnInfo.setOnClickListener(this);
		btnMap.setOnClickListener(this);
		btnFollow.setOnClickListener(this);
		btnArtists.setOnClickListener(this);
		btnArtists2.setOnClickListener(this);
		
		updateColors();

		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if (!isEventLoading) {
			updateScreen();
			
		} else {
			prgDetails.setVisibility(View.VISIBLE);
			lnrContent.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	public void onResume() {
		super.onResume(AppConstants.INVALID_INDEX, event.getName());
	}

	private void updateScreen() {
		updateEventImg();
		updateFollowBtn();
		updateArtistsAndInfoBtn();
		updateDistance();
		updateVenueNameAndDate();
	}

	private void updateVenueNameAndDate() {
		if (event.getSchedule() != null) {
			Schedule schedule = event.getSchedule();
			txtVenueName.setText(schedule.getVenue().getName() + ", " 
				+ schedule.getVenue().getAddress().getCity());

			if (schedule.getDates() != null && schedule.getDates().get(0) != null) {
				com.wcities.eventseeker.core.Date date = schedule.getDates().get(0);
				SimpleDateFormat sdf = date.isStartTimeAvailable() ? new SimpleDateFormat("EEEE MMMM d, h:mm a") :
					new SimpleDateFormat("EEEE MMMM d");
				txtDate.setText(sdf.format(date.getStartDate()).toUpperCase());
			} else {
				txtDate.setVisibility(View.GONE);
			}
		}
	}

	private void updateDistance() {
		String distance = event.getDistance();
		if (distance.equals(AppConstants.INVALID_DISTANCE)) {
			double latLon[] = DeviceUtil.getLatLon(FragmentUtil.getActivity(this));
			distance = event.getSchedule().getVenue().getDistanceFrom(latLon[0], latLon[1]) + "";
		}
		distance = String.format("%.3f", Double.parseDouble(distance));
		txtDistance.setText(distance + " mi");
	}
	
	private void updateArtistsAndInfoBtn() {
		if (AppConstants.IS_CAR_STATIONARY) {
			btnInfo.setVisibility(View.VISIBLE);
			
			if (event.hasArtists()) {
				btnArtists.setVisibility(View.VISIBLE);
			} else {
				btnArtists.setVisibility(View.INVISIBLE);
			}
			btnArtists2.setVisibility(View.INVISIBLE);
		} else {
			btnInfo.setVisibility(View.GONE);
			
			if (event.hasArtists()) {
				btnArtists2.setVisibility(View.VISIBLE);
			} else {
				btnArtists2.setVisibility(View.INVISIBLE);				
			}
			
			btnArtists.setVisibility(View.INVISIBLE);
		}		
	}

	private void updateFollowBtn() {
		btnFollow.setText((event.getAttending() == Attending.NOT_GOING) ? "Follow" : "Following");
	}

	private void updateEventImg() {
		if (event.doesValidImgUrlExist()) {
			String key = event.getKey(ImgResolution.LOW);
	        BitmapCache bitmapCache = BitmapCache.getInstance();
			Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);

			if (bitmap != null) {
				imgItem.setImageBitmap(bitmap);
				imgItem.setVisibility(View.VISIBLE);
				
		        prgImg.setVisibility(View.GONE);
			} else {
				imgItem.setImageBitmap(null);
				imgItem.setVisibility(View.INVISIBLE);
				
		    	AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
		        asyncLoadImg.loadImg(imgItem, ImgResolution.LOW, event, this);
		        
		        prgImg.setVisibility(View.VISIBLE);
			}
		}
	}
	
	private void updateColors() {
		int bgColor = AppConstants.IS_NIGHT_MODE_ENABLED ? R.color.bg_black_transparent_strip_night_mode : 
			R.color.bg_black_transparent_strip;
		txtDate.setBackgroundColor(getResources().getColor(bgColor));
		txtDistance.setBackgroundColor(getResources().getColor(bgColor));
		txtVenueName.setBackgroundColor(getResources().getColor(bgColor));
	}

	@Override
	public void onClick(View v) {
		Bundle args;
		
		switch (v.getId()) {

			case R.id.btnCall:
				Venue venue = event.getSchedule().getVenue();
				
				if (venue.getPhone() != null) {
					Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + venue.getPhone()));
					startActivity(Intent.createChooser(intent, "Call..."));
					
				} else {
					((BoschMainActivity)FragmentUtil.getActivity(this)).showBoschDialog(
							"Phone number is not available for this venue.");
				}
				
				break;
			
			case R.id.btnFollow :
				String wcitiesId = ((EventSeekr) FragmentUtil.getActivity(this).getApplication()).getWcitiesId();
				
				if (wcitiesId != null) {				
					Attending attending = event.getAttending() == Attending.GOING ? Attending.NOT_GOING 
						: Attending.GOING;
					if (attending == Attending.NOT_GOING) {
						event.setAttending(attending);
						new UserTracker((EventSeekr) FragmentUtil.getActivity(this).getApplication(), 
			               		UserTrackingItemType.event, event.getId(), event.getAttending().getValue(), 
			               		UserTrackingType.Add).execute();
						updateFollowBtn();
							
					} else {
						fbCallCountForSameEvt = 0;
						event.setNewAttending(attending);
						FbUtil.handlePublishEvent(this, this, AppConstants.PERMISSIONS_FB_PUBLISH_EVT, 
							AppConstants.REQ_CODE_FB_PUBLISH_EVT, event);
					}
						
				} else {
					((BoschMainActivity)FragmentUtil.getActivity(this)).showBoschDialog(getResources()
							.getString(R.string.pls_login_to_track_evt));
				}
				
				break;
			case R.id.btnInfo :
				args = new Bundle();
				args.putSerializable(BundleKeys.EVENT, event);
				((ReplaceFragmentListener)FragmentUtil.getActivity(this))
					.replaceByFragment(BoschInfoFragment.class.getSimpleName(), args);
				
				break;
			case R.id.btnMap :
				args = new Bundle();
				args.putSerializable(BundleKeys.VENUE, event.getSchedule().getVenue());
				((ReplaceFragmentListener)FragmentUtil.getActivity(this))
				.replaceByFragment(BoschNavigateFragment.class.getSimpleName(), args);				
				
				break;
			case R.id.btnArtists :
			case R.id.btnArtists2 :
				args = new Bundle();
				args.putSerializable(BundleKeys.EVENT, event);
				((ReplaceFragmentListener)FragmentUtil.getActivity(this))
					.replaceByFragment(BoschEventArtistsFragment.class.getSimpleName(), args);				
				
				break;
		}
	}

	@Override
	public void onImageLoaded() {
		updateEventImg();
	}

	@Override
	public void onImageCouldNotBeLoaded() {
		prgImg.setVisibility(View.GONE);
		((BoschMainActivity)FragmentUtil.getActivity(this)).showBoschDialog("No image found.");
	}

	@Override
	public void call(Session session, SessionState state, Exception exception) {
		
		fbCallCountForSameEvt++;
		/**
		 * To prevent infinite loop when network is off & we are calling requestPublishPermissions() of FbUtil.
		 */
		if (fbCallCountForSameEvt < AppConstants.MAX_FB_CALL_COUNT_FOR_SAME_EVT) {
			
			FbUtil.call(session, state, exception, this, this, AppConstants.PERMISSIONS_FB_PUBLISH_EVT, 
				AppConstants.REQ_CODE_FB_PUBLISH_EVT, event);
			
		} else {
			fbCallCountForSameEvt = 0;
			setPendingAnnounce(false);
		}
		
	}

	@Override
	public void onPublishPermissionGranted() {
		updateFollowBtn();
	}

	@Override
	public void onEventUpdated() {

		isEventLoading = false;

		try {
			/**
			 * added code in try catch as some times when device gets disconnected when execution is in 
			 * between of below 'if' block than app gets crash due to null pointer.
			 */
			if (!isDetached()) {
				
				prgDetails.setVisibility(View.GONE);
				lnrContent.setVisibility(View.VISIBLE);
				updateScreen();
				
			}
			
		} catch (Exception e) {
			Log.e(TAG, "Error : " + e.toString() + " in onEventUpdated()");
		}

	}

	@Override
	public void onCarStationaryStatusChanged(boolean isStationary) {
		updateArtistsAndInfoBtn();
	}

	@Override
	public void onDisplayModeChanged(boolean isNightModeEnabled) {
		updateColors();
	}
}