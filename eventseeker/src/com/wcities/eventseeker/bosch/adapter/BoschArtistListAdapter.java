package com.wcities.eventseeker.bosch.adapter;

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
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.interfaces.ArtistAdapterListener;
import com.wcities.eventseeker.interfaces.ArtistListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.ViewUtil;

public class BoschArtistListAdapter<T> extends BaseAdapter implements ArtistAdapterListener<T> {
	
	private static final String TAG = BoschArtistListAdapter.class.getSimpleName();

    private BitmapCache bitmapCache;
    private Context mContext;
    
    private List<Artist> artistList;
    private AsyncTask<T, Void, List<Artist>> loadArtists;
    private boolean isMoreDataAvailable = true;
    private LoadItemsInBackgroundListener mListener;
    private int artistsAlreadyRequested;

    public BoschArtistListAdapter(Context context, List<Artist> artistList, AsyncTask<T, Void, List<Artist>> 
    		loadArtists, LoadItemsInBackgroundListener listener) {
        bitmapCache = BitmapCache.getInstance();
        mContext = context;
        
        this.artistList = artistList;
        this.loadArtists = loadArtists;
        this.mListener = listener;
    }
    
    @Override
    public void updateContext(Context context) {
    	mContext = context;
	}
    
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (artistList.get(position) == null) {
			if (convertView == null || !convertView.getTag().equals(AppConstants.TAG_PROGRESS_INDICATOR)) {
				convertView = LayoutInflater.from(mContext).inflate(R.layout.list_progress_bar, null);
				convertView.setTag(AppConstants.TAG_PROGRESS_INDICATOR);
			}
			
			if ((loadArtists == null || loadArtists.getStatus() == Status.FINISHED) && isMoreDataAvailable) {
				mListener.loadItemsInBackground();
			}
			
		} else {
			
			final Artist artist = getItem(position);
			
			if (artist.getId() == AppConstants.INVALID_ID) {
				convertView = LayoutInflater.from(mContext).inflate(R.layout.list_no_items_found, null);
				((TextView)convertView).setText("No Artist Found.");
				convertView.setTag("");
		
				return convertView;
			
			} else if (convertView == null || !convertView.getTag().equals(AppConstants.TAG_CONTENT)) {
				convertView = LayoutInflater.from(mContext).inflate(R.layout.bosch_artists_list_item, null);
				convertView.setTag(AppConstants.TAG_CONTENT);
			}
			
			((TextView)convertView.findViewById(R.id.txtArtistName)).setText(artist.getName());
			
			String key = artist.getKey(ImgResolution.LOW);
			Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
			if (bitmap != null) {
		        ((ImageView)convertView.findViewById(R.id.imgArtist)).setImageBitmap(bitmap);
		        
		    } else {
		    	ImageView imgArtist = (ImageView)convertView.findViewById(R.id.imgArtist); 
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
		
		ViewUtil.updateFontColor(mContext.getResources(), convertView);
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
	
	@Override
	public int getArtistsAlreadyRequested() {
		return artistsAlreadyRequested;
	}

	@Override
	public void setArtistsAlreadyRequested(int artistsAlreadyRequested) {
		this.artistsAlreadyRequested = artistsAlreadyRequested;
	}

	@Override
	public void setMoreDataAvailable(boolean isMoreDataAvailable) {
		this.isMoreDataAvailable = isMoreDataAvailable;
	}

	@Override
	public void setLoadArtists(AsyncTask<T, Void, List<Artist>> loadArtists) {
		this.loadArtists = loadArtists;
	}
}
