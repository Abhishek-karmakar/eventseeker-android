package com.wcities.eventseeker;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.wcities.eventseeker.analytics.GoogleAnalyticsTracker;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Video;
import com.wcities.eventseeker.util.FragmentUtil;

public class VideoFragment extends Fragment {
	
    private BitmapCache bitmapCache;

    public static final VideoFragment newInstance(Video video, int artistId) {
    	VideoFragment videoFragment = new VideoFragment();
		Bundle bundle = new Bundle();
		bundle.putSerializable(BundleKeys.VIDEO, video);
		bundle.putInt(BundleKeys.ARTIST_ID, artistId);
		videoFragment.setArguments(bundle);
		return videoFragment;
	}
    
    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        bitmapCache = BitmapCache.getInstance();
	}
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
    		Bundle savedInstanceState) {
    	//Log.d(TAG, "onCreateView()");
    	final Video video = (Video) getArguments().getSerializable(BundleKeys.VIDEO);
    	View v = inflater.inflate(R.layout.fragment_video, null);
    	
    	if(((MainActivity)FragmentUtil.getActivity(this)).isTablet()) {
    		
    		ImageView imgVideo = (ImageView) v.findViewById(R.id.imgVideo);
    		
    		String key = video.getKey(ImgResolution.LOW);
    		Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
    		if (bitmap != null) {
    			imgVideo.setImageBitmap(bitmap);
    			
    		} else {
    			imgVideo.setImageResource(R.drawable.placeholder);
    			AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
    			asyncLoadImg.loadImg(imgVideo, ImgResolution.LOW, video);
    		}

    	} else {
			
	    	ImageView imgVideo = (ImageView) v.findViewById(R.id.imgVideo);
	    	
	    	String key = video.getKey(ImgResolution.LOW);
	    	Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
	    	if (bitmap != null) {
	    		imgVideo.setImageBitmap(bitmap);
	    		
	    	} else {
	    		imgVideo.setImageBitmap(null);
	    		AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
	    		asyncLoadImg.loadImg(imgVideo, ImgResolution.LOW, video);
	    	}
    	}    	
    	v.setOnClickListener(new OnClickListener() {
    		
    		@Override
    		public void onClick(View v) {
    			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(video.getVideoUrl()));
    			startActivity(Intent.createChooser(intent, ""));
    			
    			/**
				 * 15-12-2014: added Google Analytics tracker code.
				 */
				GoogleAnalyticsTracker.getInstance().sendEvent(FragmentUtil.getApplication(VideoFragment.this), 
					FragmentUtil.getScreenName(VideoFragment.this), GoogleAnalyticsTracker.ARTIST_VIDEO_CLICK,
					GoogleAnalyticsTracker.Type.Artist.name(), video.getVideoUrl(), 
					getArguments().getInt(BundleKeys.ARTIST_ID));
    		}
    	});
    	
    	return v;
    }
    
    /*@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		//Log.d(TAG, "onCreateView()");
		final Video video = (Video) getArguments().getSerializable(BundleKeys.VIDEO);
		View v = inflater.inflate(R.layout.fragment_video, null);
		
		ImageView imgVideo = (ImageView) v.findViewById(R.id.imgVideo);
		
		String key = video.getKey(ImgResolution.LOW);
		Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
		if (bitmap != null) {
			imgVideo.setImageBitmap(bitmap);
	        
	    } else {
	    	imgVideo.setImageBitmap(null);
	    	AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
	        asyncLoadImg.loadImg(imgVideo, ImgResolution.LOW, video);
	    }
		
		v.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(video.getVideoUrl()));
				startActivity(Intent.createChooser(intent, ""));
			}
		});
		
		return v;
	}*/
}
