package com.wcities.eventseeker;

import com.wcities.eventseeker.adapter.VideoPagerAdapter;
import com.wcities.eventseeker.analytics.GoogleAnalyticsTracker;
import com.wcities.eventseeker.analytics.IGoogleAnalyticsTracker;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Video;
import com.wcities.eventseeker.custom.view.RelativeLayoutCenterScale;
import com.wcities.eventseeker.util.FragmentUtil;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageView;

public class VideoFragment extends Fragment {
	
	private static final String TAG = VideoFragment.class.getSimpleName();
	
	private BitmapCache bitmapCache;

	public static final VideoFragment newInstance(Video video, int artistId, float scale, int position) {
		//Log.d(TAG, "newInstance()");
		VideoFragment videoFragment = new VideoFragment();
		Bundle bundle = new Bundle();
		bundle.putSerializable(BundleKeys.VIDEO, video);
		bundle.putInt(BundleKeys.ARTIST_ID, artistId);
		bundle.putFloat(BundleKeys.SCALE, scale);
		bundle.putInt(BundleKeys.POS, position);
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
    	//Log.d(TAG, "video url = " + video.getVideoUrl());
    	View v = inflater.inflate(R.layout.fragment_video, null);
    	
    	RelativeLayoutCenterScale relativeLayoutCenterScale = (RelativeLayoutCenterScale) v.findViewById(R.id.rltLytRoot);
		float scale = getArguments().getFloat(BundleKeys.SCALE);
		relativeLayoutCenterScale.setScaleBoth(scale);
		if (scale == VideoPagerAdapter.BIG_SCALE) {
			//Log.d(TAG, "scale = " + scale);
			/**
			 * We get BIG_SCALE only after orientation change for current centered position.
			 * Reset it to small scale again otherwise changing orientation for some other centered position
			 * will result into multiple fragments having scale value = BIG_SCALE which we don't want
			 * as there can be only single centered page.
			 */
			getArguments().putFloat(BundleKeys.SCALE, RelativeLayoutCenterScale.SMALL_SCALE);
		}
    	
    	ImageView imgVideo = (ImageView) v.findViewById(R.id.imgVideo);
    	//Log.d(TAG, "imgVideo = " + imgVideo);
    	
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
    			
    			/**
				 * 15-12-2014: added Google Analytics tracker code.
				 */
    			String screenName; 
    			if (FragmentUtil.getActivity(VideoFragment.this) instanceof IGoogleAnalyticsTracker) {
    				screenName = ((IGoogleAnalyticsTracker) FragmentUtil.getActivity(VideoFragment.this)).getScreenName();
    				
    			} else {
    				screenName = FragmentUtil.getScreenName(VideoFragment.this);
    			}
    			GoogleAnalyticsTracker.getInstance().sendEvent(FragmentUtil.getApplication(VideoFragment.this), 
						screenName, GoogleAnalyticsTracker.ARTIST_VIDEO_CLICK, GoogleAnalyticsTracker.Type.Artist.name(), 
						video.getVideoUrl(), getArguments().getInt(BundleKeys.ARTIST_ID));
    		}
    	});
    	
    	return v;
    }
}
