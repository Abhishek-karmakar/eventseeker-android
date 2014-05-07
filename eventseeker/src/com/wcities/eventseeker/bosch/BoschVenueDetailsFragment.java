package com.wcities.eventseeker.bosch;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bosch.myspin.serversdk.MySpinServerSDK;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.asynctask.AsyncLoadImg.AsyncLoadImageListener;
import com.wcities.eventseeker.asynctask.LoadDateWiseVenueEventsList;
import com.wcities.eventseeker.asynctask.LoadDateWiseVenueEventsList.EventExistListener;
import com.wcities.eventseeker.bosch.BoschMainActivity.OnCarStationaryStatusChangedListener;
import com.wcities.eventseeker.bosch.BoschMainActivity.OnDisplayModeChangedListener;
import com.wcities.eventseeker.bosch.custom.fragment.BoschFragmentLoadableFromBackStack;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.interfaces.ReplaceFragmentListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class BoschVenueDetailsFragment extends BoschFragmentLoadableFromBackStack implements OnClickListener, 
		AsyncLoadImageListener, OnCarStationaryStatusChangedListener, OnDisplayModeChangedListener,
		EventExistListener {

	public static final String TAG = BoschVenueDetailsFragment.class.getName();
	
	private View prgDetails, prgImg;
	private View lnrContent;

	private Button btnInfo, btnEvents;
	
	private TextView txtVenueName, txtAddress, txtActionBarTitle;
	// private TextView txtDistance;
	
	private ImageView imgVenue;

	private Venue venue;
	
	boolean isLoadingEvents;

	private Resources res;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		venue = (Venue) getArguments().getSerializable(BundleKeys.VENUE);
		
		res = FragmentUtil.getResources(this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		
		View view = inflater.inflate(R.layout.fragment_bosch_venue_details, null);
		
		prgDetails = view.findViewById(R.id.prgDetails);
		prgImg = view.findViewById(R.id.prgImg);

		lnrContent = view.findViewById(R.id.lnrContent);

		imgVenue = (ImageView) view.findViewById(R.id.imgItem);

		txtVenueName = (TextView) view.findViewById(R.id.txtVenueName);
		txtAddress = (TextView) view.findViewById(R.id.txtAddress);
		//txtDistance = (TextView) view.findViewById(R.id.txtDistance);

		(btnInfo = (Button) view.findViewById(R.id.btnInfo)).setOnClickListener(this);		
		updateInfoBtn();
		
		(btnEvents = (Button) view.findViewById(R.id.btnEvents)).setOnClickListener(this);
		
		view.findViewById(R.id.btnCall).setOnClickListener(this);
		
		updateColors();
		
		txtActionBarTitle = (TextView) ((ActionBarActivity)FragmentUtil.getActivity(this)).getSupportActionBar()
				.getCustomView().findViewById(R.id.txtActionBarTitle);
		txtActionBarTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(
				R.dimen.b_txt_actionbar_title_txt_size_bosch_actionbar_titleview) - ConversionUtil.toPx(res, 
						AppConstants.REDUCE_TITLE_TXT_SIZE_BY_SP_FOR_BOSCH_DETAIL_SCREENS));
		
		return view;
	}
	
	private void updateInfoBtn() {
		if (AppConstants.IS_CAR_STATIONARY) {
			btnInfo.setVisibility(View.VISIBLE);
		} else {
			btnInfo.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		isLoadingEvents = true;
		/**
		 *  The below async task call is made just to check the number of events, if the events are more than 0, 
		 *  then show the events button.
		 */
		LoadDateWiseVenueEventsList loadEvents = new LoadDateWiseVenueEventsList(Api.OAUTH_TOKEN_CAR_APPS, null, null, 
			((EventSeekr)FragmentUtil.getActivity(this).getApplicationContext()).getWcitiesId(), 
			venue.getId(), this);
		AsyncTaskUtil.executeAsyncTask(loadEvents, true);
		
		updateScreen();
	}

	private void updateScreen() {
		updateLoadingUI();
		updateVenueImg();
		
		txtVenueName.setText(venue.getName());
		txtAddress.setText(venue.getFormatedAddress());
		// txtDistance.setText(text);
	}

	private void updateLoadingUI() {
		if (isLoadingEvents) {
			prgDetails.setVisibility(View.VISIBLE);
			lnrContent.setVisibility(View.INVISIBLE);
		} else {
			prgDetails.setVisibility(View.GONE);
			lnrContent.setVisibility(View.VISIBLE);			
		}		
	}

	@Override
	public void onResume() {
		super.onResume();
		((BoschMainActivity) FragmentUtil.getActivity(this))
			.onFragmentResumed(this, AppConstants.INVALID_INDEX, venue.getName());
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		txtActionBarTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(
				R.dimen.b_txt_actionbar_title_txt_size_bosch_actionbar_titleview));
	}
	
	private void updateVenueImg() {
		if (venue.doesValidImgUrlExist()) {
			String key = venue.getKey(ImgResolution.LOW);
	        BitmapCache bitmapCache = BitmapCache.getInstance();
			Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
			
			if (bitmap != null) {
		        imgVenue.setImageBitmap(bitmap);
		        imgVenue.setVisibility(View.VISIBLE);
				
		        prgImg.setVisibility(View.GONE);

			} else {
		    	imgVenue.setImageBitmap(null);
		    	imgVenue.setVisibility(View.INVISIBLE);

		    	prgImg.setVisibility(View.VISIBLE);
		    	
		    	AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
		        asyncLoadImg.loadImg(imgVenue, ImgResolution.LOW, venue, this);
		    }
		}
	}
	
	private void updateColors() {
		int bgColor = AppConstants.IS_NIGHT_MODE_ENABLED ? R.color.bg_black_transparent_strip_night_mode : 
				R.color.bg_black_transparent_strip;
		txtAddress.setBackgroundColor(getResources().getColor(bgColor));
		txtVenueName.setBackgroundColor(getResources().getColor(bgColor));
	}

	@Override
	public void onClick(View v) {
		Bundle args;
		
		switch (v.getId()) {
			case R.id.btnEvents:
				args = new Bundle();
				args.putSerializable(BundleKeys.VENUE, venue);
				((ReplaceFragmentListener)FragmentUtil.getActivity(this))
					.replaceByFragment(BoschVenueEventsFragment.class.getSimpleName(), args);
				break;
			
			case R.id.btnInfo:
				args = new Bundle();
				args.putSerializable(BundleKeys.VENUE, venue);
				((ReplaceFragmentListener)FragmentUtil.getActivity(this))
					.replaceByFragment(BoschInfoFragment.class.getSimpleName(), args);
				break;
			
			case R.id.btnCall:
				if (venue.getPhone() != null && MySpinServerSDK.sharedInstance().hasPhoneCallCapability()) {
					MySpinServerSDK.sharedInstance().initiatePhoneCall(venue.getName(), venue.getPhone());
					
				} else {
					String msg = (venue.getPhone() == null) ? "Phone number is not available for this venue." : 
						"Calling is not supported on this IVI System.";
					((BoschMainActivity) FragmentUtil.getActivity(this)).showBoschDialog(msg);
				}
				break;
		}
		
	}

	@Override
	public void onImageLoaded() {
		updateVenueImg();
	}

	@Override
	public void onImageCouldNotBeLoaded() {
		prgImg.setVisibility(View.GONE);
	}

	@Override
	public void onCarStationaryStatusChanged(boolean isStationary) {
		updateInfoBtn();
	}
	
	@Override
	public void onDisplayModeChanged(boolean isNightModeEnabled) {
		updateColors();
	}

	@Override
	public void hasEvents(boolean hasEvents) {
		btnEvents.setVisibility(hasEvents ? View.VISIBLE : View.INVISIBLE);		

		isLoadingEvents = false;
		updateLoadingUI();
	}
}
