package com.wcities.eventseeker.bosch;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.asynctask.AsyncLoadImg.AsyncLoadImageListener;
import com.wcities.eventseeker.bosch.BoschMainActivity.OnCarStationaryStatusChangedListener;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.ReplaceFragmentListener;
import com.wcities.eventseeker.util.FragmentUtil;

public class BoschVenueDetailsFragment extends FragmentLoadableFromBackStack implements OnClickListener, 
	AsyncLoadImageListener, OnCarStationaryStatusChangedListener {

	public static final String TAG = BoschVenueDetailsFragment.class.getName();
	
	private View prgDetails, prgImg;
	// private View lnrContent;

	private Button btnInfo;
	
	private TextView txtVenueName, txtAddress;
	// private TextView txtDistance;
	
	private ImageView imgVenue;

	private Venue venue;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		venue = (Venue) getArguments().getSerializable(BundleKeys.VENUE);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		
		View view = inflater.inflate(R.layout.fragment_bosch_venue_details, null);
		
		// prgDetails = view.findViewById(R.id.prgDetails);
		prgImg = view.findViewById(R.id.prgImg);

		// lnrContent = view.findViewById(R.id.lnrContent);

		imgVenue = (ImageView) view.findViewById(R.id.imgItem);

		txtVenueName = (TextView) view.findViewById(R.id.txtVenueName);
		txtAddress = (TextView) view.findViewById(R.id.txtAddress);
		//txtDistance = (TextView) view.findViewById(R.id.txtDistance);

		(btnInfo = (Button) view.findViewById(R.id.btnInfo)).setOnClickListener(this);		
		updateInfoBtn();
		
		view.findViewById(R.id.btnEvents).setOnClickListener(this);
		view.findViewById(R.id.btnCall).setOnClickListener(this);
		
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
		updateScreen();
	}

	private void updateScreen() {
		updateVenueImg();
		
		txtVenueName.setText(venue.getName());
		txtAddress.setText(venue.getFormatedAddress());
		// txtDistance.setText(text);
	}

	@Override
	public void onResume() {
		super.onResume();
		((BoschMainActivity) FragmentUtil.getActivity(this))
			.onFragmentResumed(this, AppConstants.INVALID_INDEX, venue.getName());
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
				//lnrContent.setVisibility(View.VISIBLE);
		    } else {
		    	imgVenue.setImageBitmap(null);
		    	imgVenue.setVisibility(View.INVISIBLE);

		    	prgImg.setVisibility(View.VISIBLE);
		    	
		    	AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
		        asyncLoadImg.loadImg(imgVenue, ImgResolution.LOW, venue, this);
		    }
		}
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
				Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + venue.getPhone()));
				startActivity(Intent.createChooser(intent, "Call..."));
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
		// lnrContent.setVisibility(View.VISIBLE);
	}

	@Override
	public void onCarStationaryStatusChanged(boolean isStationary) {
		updateInfoBtn();
	}
	
}
