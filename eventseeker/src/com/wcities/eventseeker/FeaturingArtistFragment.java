package com.wcities.eventseeker;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wcities.eventseeker.adapter.CatTitlesAdapter;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.custom.view.CategoryTitleLinearLayout;
import com.wcities.eventseeker.custom.view.FeaturingArtistRelativeLayout;

public class FeaturingArtistFragment extends Fragment {
	
	protected static final String TAG = FeaturingArtistFragment.class.getSimpleName();

	public static final FeaturingArtistFragment newInstance(Artist artist, float scale) {
		//Log.d(TAG, "newInstance()");
		FeaturingArtistFragment featuringArtistFragment = new FeaturingArtistFragment();
		Bundle b = new Bundle();
		b.putSerializable(BundleKeys.ARTIST, artist);
		b.putFloat(BundleKeys.SCALE, scale);
		featuringArtistFragment.setArguments(b);
		return featuringArtistFragment;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		//Log.d(TAG, "onCreateView()");
		Bundle args = getArguments();
		Artist artist = (Artist) args.getSerializable(BundleKeys.ARTIST);
		
		LinearLayout l = (LinearLayout) inflater.inflate(R.layout.featuring_artist, container, false);
		
		FeaturingArtistRelativeLayout featuringArtistRelativeLayout = (FeaturingArtistRelativeLayout) l.findViewById(R.id.rltLytRoot);
		float scale = getArguments().getFloat(BundleKeys.SCALE);
		featuringArtistRelativeLayout.setScaleBoth(scale);
		
		String key = artist.getKey(ImgResolution.LOW);
		BitmapCache bitmapCache = BitmapCache.getInstance();
		Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
		if (bitmap != null) {
	        ((ImageView) l.findViewById(R.id.imgArtist)).setImageBitmap(bitmap);
	        
	    } else {
	    	ImageView imgArtist = (ImageView) l.findViewById(R.id.imgArtist); 
	        imgArtist.setImageBitmap(null);

	        AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
	        asyncLoadImg.loadImg(imgArtist, ImgResolution.LOW, artist);
	    }
		
		TextView txtArtistName = (TextView) l.findViewById(R.id.txtArtistName);
		if (txtArtistName != null) {
			txtArtistName.setText(artist.getName());
		}
		
		return l;
	}
}
