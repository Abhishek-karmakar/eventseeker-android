package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.custom.view.RelativeLayoutCenterScale;
import com.wcities.eventseeker.interfaces.ArtistListener;
import com.wcities.eventseeker.interfaces.CustomSharedElementTransitionSource;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.ViewUtil;
import com.wcities.eventseeker.viewdata.SharedElement;
import com.wcities.eventseeker.viewdata.SharedElementPosition;

public class FeaturingArtistFragmentTab extends Fragment {
	
	protected static final String TAG = FeaturingArtistFragmentTab.class.getSimpleName();

	public static final FeaturingArtistFragmentTab newInstance(Artist artist, float scale) {
		//Log.d(TAG, "newInstance()");
		FeaturingArtistFragmentTab featuringArtistFragmentTab = new FeaturingArtistFragmentTab();
		Bundle b = new Bundle();
		b.putSerializable(BundleKeys.ARTIST, artist);
		b.putFloat(BundleKeys.SCALE, scale);
		featuringArtistFragmentTab.setArguments(b);
		return featuringArtistFragmentTab;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Bundle args = getArguments();
		final Artist artist = (Artist) args.getSerializable(BundleKeys.ARTIST);
		
		//Log.d(TAG, "onCreateView() - artist = " + artist.getName());
		final LinearLayout l = (LinearLayout) inflater.inflate(R.layout.featuring_artist, container, false);
		
		final RelativeLayoutCenterScale featuringArtistRelativeLayout = (RelativeLayoutCenterScale) l.findViewById(R.id.rltLytRoot);
		float scale = getArguments().getFloat(BundleKeys.SCALE);
		featuringArtistRelativeLayout.setScaleBoth(scale);
		
		ImageView imgArtist = (ImageView) l.findViewById(R.id.imgArtist);
		String key = artist.getKey(ImgResolution.LOW);
		BitmapCache bitmapCache = BitmapCache.getInstance();
		Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
		if (bitmap != null) {
	        imgArtist.setImageBitmap(bitmap);
	        
	    } else {
	        imgArtist.setImageBitmap(null);

	        AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
	        asyncLoadImg.loadImg(imgArtist, ImgResolution.LOW, artist);
	    }
		
		TextView txtArtistName = (TextView) l.findViewById(R.id.txtArtistName);
		if (txtArtistName != null) {
			txtArtistName.setText(artist.getName());
		}
		
		featuringArtistRelativeLayout.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				/*int[] loc = ViewUtil.getLocationOnScreen(featuringArtistRelativeLayout, FragmentUtil.getResources(FeaturingArtistFragmentTab.this));
				int scaledW = (int) (featuringArtistRelativeLayout.getWidth() * featuringArtistRelativeLayout.getScale());
				int scaledHt = (int) (featuringArtistRelativeLayout.getHeight() * featuringArtistRelativeLayout.getScale());
				List<SharedElement> sharedElements = new ArrayList<SharedElement>();
				
				SharedElementPosition sharedElementPosition = new SharedElementPosition(loc[0] + ((featuringArtistRelativeLayout.getWidth() - scaledW) / 2), 
						loc[1] + ((featuringArtistRelativeLayout.getHeight() - scaledHt) / 2), scaledW, scaledHt);
				SharedElement sharedElement = new SharedElement(sharedElementPosition, featuringArtistRelativeLayout);
				sharedElements.add(sharedElement);
				((CustomSharedElementTransitionSource)getParentFragment()).addViewsToBeHidden(featuringArtistRelativeLayout);
				
				//Log.d(TAG, "AT issue event = " + event);
				((ArtistListener)FragmentUtil.getActivity(FeaturingArtistFragment.this)).onArtistSelected(artist, sharedElements);
				
				((CustomSharedElementTransitionSource)getParentFragment()).onPushedToBackStack();*/
			}
		});
		
		return l;
	}
}

