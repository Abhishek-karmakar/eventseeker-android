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
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
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
AsyncLoadImageListener, OnArtistUpdatedListener {

	private static final String TAG = BoschArtistDetailsFragment.class.getName();
	
	private Artist artist;
	private Button btnFollow, btnInfo, btnEvents;
	private ProgressBar prgImg, prgDetails;
	private ResizableImageView imgItem;
	private boolean isLoadingArtistDetails;
	private TextView txtName;
	private View lnrContent;
	
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
		
		txtName= (TextView) view.findViewById(R.id.txtName);

		btnEvents = (Button) view.findViewById(R.id.btnEvents);
		btnInfo = (Button) view.findViewById(R.id.btnInfo);
		btnFollow = (Button) view.findViewById(R.id.btnFollow);
			
		btnFollow.setOnClickListener(this);
		btnInfo.setOnClickListener(this);
		btnEvents.setOnClickListener(this);

		return view;
		
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if(!isLoadingArtistDetails) {
			updateScreen();		
		} else {
			prgDetails.setVisibility(View.VISIBLE);
			lnrContent.setVisibility(View.INVISIBLE);
		}
		
	}
	
	private void updateScreen() {

		txtName.setText(artist.getName());
		
		updateArtistImg();
		updateFollowBtn();
				
	}

	@Override
	public void onResume() {
		super.onResume();
		//set the above title on the action bar as screen title
		((BoschMainActivity)FragmentUtil.getActivity(this)).updateBoschActionBarTitle(artist.getName());
	}
	
	private void updateFollowBtn() {
		btnFollow.setText((artist.getAttending() == Attending.NotTracked) ? "Follow" : "Following");
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
				
				if (FbUtil.hasUserLoggedInBefore(FragmentUtil.getActivity(this))) {
					
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
					//TODO: Show FB Login Dialog
					Toast.makeText(FragmentUtil.getActivity(this), "First Login through the Facebook Account",
						Toast.LENGTH_SHORT).show();
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
		// TODO:TO show Error
		prgImg.setVisibility(View.GONE);
		Toast.makeText(FragmentUtil.getActivity(this), "No image found.", Toast.LENGTH_LONG).show();
	}

	@Override
	public void onArtistUpdated() {
		
		try {
			/**
			 * added code in try catch as some times when device gets disconnected when execution is in 
			 * between of below 'if' block than app gets crash due to null pointer.
			 */
			isLoadingArtistDetails = false;
			
			if (isAdded()) {
			
				prgDetails.setVisibility(View.GONE);
				lnrContent.setVisibility(View.VISIBLE);
				updateScreen();
	
			}
			
		} catch (Exception e) {
			Log.e(TAG, "Error : " + e.toString() + " in onArtistUpdated()");
		}
		
	}

}
