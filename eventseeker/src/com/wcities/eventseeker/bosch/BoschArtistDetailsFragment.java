package com.wcities.eventseeker.bosch;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingItemType;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.asynctask.AsyncLoadImg.AsyncLoadImageListener;
import com.wcities.eventseeker.asynctask.LoadArtistDetails;
import com.wcities.eventseeker.asynctask.LoadArtistDetails.OnArtistUpdatedListener;
import com.wcities.eventseeker.asynctask.UserTracker;
import com.wcities.eventseeker.bosch.BoschMainActivity.OnCarStationaryStatusChangedListener;
import com.wcities.eventseeker.bosch.BoschMainActivity.OnDisplayModeChangedListener;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Artist.Attending;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.custom.view.ResizableImageView;
import com.wcities.eventseeker.interfaces.ReplaceFragmentListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FbUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class BoschArtistDetailsFragment extends FragmentLoadableFromBackStack implements OnClickListener, 
		AsyncLoadImageListener, OnArtistUpdatedListener, OnCarStationaryStatusChangedListener, 
		OnDisplayModeChangedListener {

	private static final String TAG = BoschArtistDetailsFragment.class.getName();

	private Button btnFollow, btnInfo, btnEvents;
	
	private ProgressBar prgImg, prgDetails;
	
	private ResizableImageView imgItem;
	
	private TextView txtName;
	
	private View lnrContent;

	private Artist artist;

	private boolean isLoadingArtistDetails;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (artist == null) {
			artist = (Artist) getArguments().getSerializable(BundleKeys.ARTIST);
			isLoadingArtistDetails = true;
			AsyncTaskUtil.executeAsyncTask(new LoadArtistDetails(artist, this, this), true);
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_bosch_artist_details, null);

		lnrContent = view.findViewById(R.id.lnrContent);

		prgDetails = (ProgressBar) view.findViewById(R.id.prgDetails);
		prgImg = (ProgressBar) view.findViewById(R.id.prgImg);

		imgItem = (ResizableImageView) view.findViewById(R.id.imgItem);
		
		txtName = (TextView) view.findViewById(R.id.txtName);

		btnInfo = (Button) view.findViewById(R.id.btnInfo);
		btnFollow = (Button) view.findViewById(R.id.btnFollow);
		btnEvents = (Button) view.findViewById(R.id.btnEvents);

		btnFollow.setOnClickListener(this);
		btnEvents.setOnClickListener(this);
		btnInfo.setOnClickListener(this);	
		
		updateColors();

		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if(isLoadingArtistDetails) {
			prgDetails.setVisibility(View.VISIBLE);
			lnrContent.setVisibility(View.INVISIBLE);
		} else {
			updateScreen();		
		}
	}
	
	private void updateScreen() {
		txtName.setText(artist.getName());
		
		updateArtistImg();
		updateInfoBtn();	
		updateFollowBtn();
		updateEventsBtn();
	}

	private void updateEventsBtn() {
		if(artist.getEvents().size() > 0) {
			btnEvents.setVisibility(View.VISIBLE);
		} else {			
			btnEvents.setVisibility(View.INVISIBLE);
		}
	}
	
	private void updateInfoBtn() {
		if (AppConstants.IS_CAR_STATIONARY) {
			btnInfo.setVisibility(View.VISIBLE);		
		} else {
			btnInfo.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	public void onResume() {
		super.onResume(AppConstants.INVALID_INDEX, artist.getName());
	}
	
	private void updateFollowBtn() {
		btnFollow.setText((artist.getAttending() == Attending.NotTracked) ? "Follow" : "Following");
	}
	
	private void updateColors() {
		int bgColor = AppConstants.IS_NIGHT_MODE_ENABLED ? R.color.bg_black_transparent_strip_night_mode : 
			R.color.bg_black_transparent_strip;
		txtName.setBackgroundColor(getResources().getColor(bgColor));
	}

	@Override
	public void onClick(View v) {
	
		Bundle args;
		
		switch (v.getId()) {

			case R.id.btnInfo:
				args = new Bundle();
				args.putSerializable(BundleKeys.ARTIST, artist);
				((ReplaceFragmentListener) FragmentUtil.getActivity(this))
					.replaceByFragment(BoschInfoFragment.class.getSimpleName(), args);
				break;

			case R.id.btnEvents:
				args = new Bundle();
				args.putSerializable(BundleKeys.ARTIST, artist);
				((ReplaceFragmentListener) FragmentUtil.getActivity(this))
					.replaceByFragment(BoschArtistEventsFragment.class.getSimpleName(), args);
				break;

			case R.id.btnFollow:
				//if (FbUtil.hasUserLoggedInBefore(FragmentUtil.getActivity(this))) {
				String wcitiesId = ((EventSeekr) FragmentUtil.getActivity(this).getApplication()).getWcitiesId();
				if (wcitiesId != null) {					
					Attending attending = artist.getAttending() == Attending.NotTracked ?
						Attending.Tracked : Attending.NotTracked;
					
					if (attending == Attending.Tracked) {
						artist.setAttending(Attending.Tracked);
							new UserTracker((EventSeekr) FragmentUtil.getActivity(this).getApplication(), 
								UserTrackingItemType.artist, artist.getId()).execute();

					} else {
						artist.setAttending(Attending.NotTracked);
							new UserTracker((EventSeekr) FragmentUtil.getActivity(this).getApplication(), 
									UserTrackingItemType.artist, artist.getId(), Attending.NotTracked.getValue(),
									UserTrackingType.Edit).execute();
					
					}
					updateFollowBtn();
					
				} else {
					((BoschMainActivity)FragmentUtil.getActivity(this)).showBoschDialog(
							"First Login through the Facebook Account.");
				}
				break;
	
		}
	}	

	private void updateArtistImg() {

		if (artist.doesValidImgUrlExist()) {
			
			String key = artist.getKey(ImgResolution.LOW);
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
		        asyncLoadImg.loadImg(imgItem, ImgResolution.LOW, artist, this);
		        
		        prgImg.setVisibility(View.VISIBLE);
		    
			}
		}
	}

	@Override
	public void onImageLoaded() {
		updateArtistImg();
	}

	@Override
	public void onImageCouldNotBeLoaded() {
		prgImg.setVisibility(View.GONE);
		((BoschMainActivity)FragmentUtil.getActivity(this)).showBoschDialog("No image found.");
	}

	@Override
	public void onArtistUpdated() {
		try {
			/**
			 * added code in try catch as some times when device gets disconnected when execution is in 
			 * between of below 'if' block than app gets crash due to null pointer.
			 */
			isLoadingArtistDetails = false;
			
				updateScreen();

				prgDetails.setVisibility(View.GONE);
				lnrContent.setVisibility(View.VISIBLE);
		} catch (Exception e) {
			Log.e(TAG, "Error : " + e.toString() + " in onArtistUpdated()");
		}
	}
	
	@Override
	public void onCarStationaryStatusChanged(boolean isStationary) {
		updateInfoBtn();
	}

	@Override
	public void onDisplayModeChanged(boolean isNightModeEnabled) {
		updateColors();
	}
}
