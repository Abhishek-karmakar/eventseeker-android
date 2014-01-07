package com.wcities.eventseeker.adapter;

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.interfaces.ArtistListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;

public class ArtistListAdapter<T> extends BaseAdapter {
	
	private static final String TAG = ArtistListAdapter.class.getName();

	private static final String TAG_PROGRESS_INDICATOR = "progressIndicator";
	private static final String TAG_CONTENT = "content";
	
    private BitmapCache bitmapCache;
    private Context mContext;
    
    private List<Artist> artistList;
    private AsyncTask<T, Void, List<Artist>> loadArtists;
    private boolean isMoreDataAvailable = true;
    private LoadItemsInBackgroundListener mListener;
    private int artistsAlreadyRequested;

    public ArtistListAdapter(Context context, List<Artist> artistList, AsyncTask<T, Void, List<Artist>> 
    		loadArtists, LoadItemsInBackgroundListener listener) {
        bitmapCache = BitmapCache.getInstance();
        mContext = context;
        
        this.artistList = artistList;
        this.loadArtists = loadArtists;
        this.mListener = listener;
    }
    
    public void updateContext(Context context) {
    	mContext = context;
	}
    
    public void setLoadArtists(AsyncTask<T, Void, List<Artist>> loadArtists) {
		this.loadArtists = loadArtists;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (artistList.get(position) == null) {
			if (convertView == null || !convertView.getTag().equals(TAG_PROGRESS_INDICATOR)) {
				convertView = LayoutInflater.from(mContext).inflate(R.layout.list_progress_bar, null);
				convertView.setTag(TAG_PROGRESS_INDICATOR);
			}
			
			if ((loadArtists == null || loadArtists.getStatus() == Status.FINISHED) && 
					isMoreDataAvailable) {
				mListener.loadItemsInBackground();
			}
			
		} else {
			
			final Artist artist = getItem(position);
			
			if (artist.getId() == AppConstants.INVALID_ID) {
				convertView = LayoutInflater.from(mContext).inflate(R.layout.list_no_items_found, null);
				((TextView)convertView).setText("No Artist Found.");
				convertView.setTag("");
		
				return convertView;
			
			} else if (convertView == null || !convertView.getTag().equals(TAG_CONTENT)) {
				convertView = LayoutInflater.from(mContext).inflate(R.layout.fragment_search_artists_list_item, null);
				convertView.setTag(TAG_CONTENT);
			}
			
			((TextView)convertView.findViewById(R.id.txtArtistName)).setText(artist.getName());
			
			if(!((EventSeekr)mContext.getApplicationContext()).isTablet()) {
				if (artist.isOntour()) {
					convertView.findViewById(R.id.txtOnTour).setVisibility(View.VISIBLE);
				} else {
					convertView.findViewById(R.id.txtOnTour).setVisibility(View.INVISIBLE);
				}
			}
			
			String key = artist.getKey(ImgResolution.LOW);
			Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
			if (bitmap != null) {
		        ((ImageView)convertView.findViewById(R.id.imgItem)).setImageBitmap(bitmap);
		        
		    } else {
		    	//Log.d(TAG, "bitmap is null, low res url = " + artist.getLowResImgUrl());
		    	ImageView imgArtist = (ImageView)convertView.findViewById(R.id.imgItem); 
		        imgArtist.setImageBitmap(null);

		        AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
		        asyncLoadImg.loadImg(imgArtist, ImgResolution.LOW, 
		        		(AdapterView) parent, position, artist);
		    }
			
			convertView.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					((ArtistListener)mContext).onArtistSelected(artist);
				}
			});
		}
		
		return convertView;
	}

	@Override
	public Artist getItem(int position) {
		return artistList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public int getCount() {
		return artistList.size();
	}
	
	public int getArtistsAlreadyRequested() {
		return artistsAlreadyRequested;
	}

	public void setArtistsAlreadyRequested(int artistsAlreadyRequested) {
		this.artistsAlreadyRequested = artistsAlreadyRequested;
	}

	public void setMoreDataAvailable(boolean isMoreDataAvailable) {
		this.isMoreDataAvailable = isMoreDataAvailable;
	}
}
