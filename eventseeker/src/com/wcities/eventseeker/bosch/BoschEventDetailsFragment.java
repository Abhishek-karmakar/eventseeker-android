package com.wcities.eventseeker.bosch;

import java.text.SimpleDateFormat;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bosch.myspin.serversdk.MySpinServerSDK;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.api.Api;
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
import com.wcities.eventseeker.bosch.custom.fragment.BoschFragmentLoadableFromBackStack;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Event.Attending;
import com.wcities.eventseeker.core.Schedule;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.custom.view.ResizableImageView;
import com.wcities.eventseeker.interfaces.ReplaceFragmentListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class BoschEventDetailsFragment extends BoschFragmentLoadableFromBackStack implements OnClickListener, 
		AsyncLoadImageListener, OnEventUpdatedListner, OnCarStationaryStatusChangedListener, 
		OnDisplayModeChangedListener {

	private static final String TAG = BoschEventDetailsFragment.class.getName();
	
	private ResizableImageView imgItem;
	
	private Event event;
	
	private ProgressBar prgImg, prgDetails;
	
	private TextView txtDate, txtVenueName, txtDistance, txtActionBarTitle;

	private Button btnFollow, btnArtists, btnArtists2, btnCall, btnInfo, btnMap;

	private boolean isEventLoading;

	private View lnrContent;
	private Resources res;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (event == null) {
			event = (Event) getArguments().getSerializable(BundleKeys.EVENT);
			isEventLoading = true;
			AsyncTaskUtil.executeAsyncTask(new LoadEventDetails(Api.OAUTH_TOKEN_CAR_APPS, this, this, event), true);
		}
		
		res = FragmentUtil.getResources(this);
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
		
		txtActionBarTitle = (TextView) ((ActionBarActivity)FragmentUtil.getActivity(this)).getSupportActionBar()
				.getCustomView().findViewById(R.id.txtActionBarTitle);
		txtActionBarTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(
				R.dimen.b_txt_actionbar_title_txt_size_bosch_actionbar_titleview) - ConversionUtil.toPx(res, 
						AppConstants.REDUCE_TITLE_TXT_SIZE_BY_SP_FOR_BOSCH_DETAIL_SCREENS));
		
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
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		txtActionBarTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(
				R.dimen.b_txt_actionbar_title_txt_size_bosch_actionbar_titleview));
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
		EventSeekr eventSeekr = FragmentUtil.getApplication(this);
		
		double curLatLon[] = DeviceUtil.getCurrentLatLon(FragmentUtil.getApplication(this));
		double distance = event.getSchedule().getVenue().getDistanceFrom(curLatLon[0], curLatLon[1], eventSeekr);
		//distance = String.format("%.3f", Double.parseDouble(distance));
		String dstnc = ConversionUtil.formatFloatingNumber(3, distance);
		//txtDistance.setText(dstnc + " mi");
		txtDistance.setText(dstnc + " " + eventSeekr.getSavedProximityUnit().toString(eventSeekr));
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
				if (venue != null && venue.getPhone() != null && MySpinServerSDK.sharedInstance().hasPhoneCallCapability()) {
					MySpinServerSDK.sharedInstance().initiatePhoneCall(venue.getName(), venue.getPhone());
					
				} else {
					String msg = (venue.getPhone() == null) ? "Phone number is not available for this venue." : 
						"Calling is not supported on this IVI System.";
					((BoschMainActivity) FragmentUtil.getActivity(this)).showBoschDialog(msg);
				}
				
				break;
			
			case R.id.btnFollow :
				String wcitiesId = ((EventSeekr) FragmentUtil.getActivity(this).getApplication()).getWcitiesId();
				
				if (wcitiesId != null) {				
					Attending attending = event.getAttending() == Attending.GOING ? Attending.NOT_GOING 
						: Attending.GOING;
					if (attending == Attending.NOT_GOING) {
						event.setAttending(attending);
						new UserTracker(Api.OAUTH_TOKEN_CAR_APPS, (EventSeekr) FragmentUtil.getActivity(this).getApplication(), 
			               		UserTrackingItemType.event, event.getId(), event.getAttending().getValue(), 
			               		UserTrackingType.Add).execute();
						updateFollowBtn();
							
					} else {
						event.setAttending(attending);
						new UserTracker(Api.OAUTH_TOKEN_CAR_APPS, (EventSeekr) FragmentUtil.getActivity(this).getApplication(), 
			                	UserTrackingItemType.event, event.getId(), event.getAttending().getValue(), 
			                	UserTrackingType.Add).execute();
						updateFollowBtn();
					}
						
				} else {
					((BoschMainActivity)FragmentUtil.getActivity(this)).showBoschDialog(getResources()
							.getString(R.string.pls_login_through_mobile_app));
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
