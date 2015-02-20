package com.wcities.eventseeker.adapter;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.wcities.eventseeker.GeneralDialogFragment;
import com.wcities.eventseeker.GeneralDialogFragment.DialogBtnClickListener;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Artist.Attending;
import com.wcities.eventseeker.interfaces.ArtistListener;
import com.wcities.eventseeker.interfaces.ArtistTrackingListener;
import com.wcities.eventseeker.interfaces.CustomSharedElementTransitionSource;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.ViewUtil;
import com.wcities.eventseeker.viewdata.SharedElement;
import com.wcities.eventseeker.viewdata.SharedElementPosition;

public class ArtistListAdapter<T> extends BaseAdapter {
	
	private static final String TAG = ArtistListAdapter.class.getName();

	private boolean addPadding;
	
    private BitmapCache bitmapCache;
    private Context mContext;
    
    private List<Artist> artistList;
    private AsyncTask<T, Void, List<Artist>> loadArtists;
    private boolean isMoreDataAvailable = true;
    private LoadItemsInBackgroundListener mListener;
    private int artistsAlreadyRequested;
    private ArtistTrackingListener artistTrackingListener;
    private DialogBtnClickListener dialogBtnClickListener;
    private CustomSharedElementTransitionSource customSharedElementTransitionSource;
    
    public ArtistListAdapter(Context context, List<Artist> artistList, AsyncTask<T, Void, List<Artist>> 
    		loadArtists, LoadItemsInBackgroundListener listener, ArtistTrackingListener artistTrackingListener,
    		DialogBtnClickListener dialogBtnClickListener, CustomSharedElementTransitionSource customSharedElementTransitionSource) {
    	this(context, artistList, loadArtists, listener);
    	this.artistTrackingListener = artistTrackingListener;
    	this.dialogBtnClickListener = dialogBtnClickListener;
    	this.customSharedElementTransitionSource = customSharedElementTransitionSource;
    }

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
	public View getView(final int position, View convertView, ViewGroup parent) {
		//Log.d(TAG, "pos = " + position);
		if (artistList.get(position) == null) {
			if (convertView == null || !convertView.getTag().equals(AppConstants.TAG_PROGRESS_INDICATOR)) {
				convertView = LayoutInflater.from(mContext).inflate(R.layout.progress_bar_eventseeker_fixed_ht, null);
				convertView.setTag(AppConstants.TAG_PROGRESS_INDICATOR);
			}
			
			if ((loadArtists == null || loadArtists.getStatus() == Status.FINISHED) && 
					isMoreDataAvailable) {
				mListener.loadItemsInBackground();
			}
			
		} else {
			
			final Artist artist = getItem(position);
			
			if (artist.getId() == AppConstants.INVALID_ID) {
				convertView = LayoutInflater.from(mContext).inflate(R.layout.list_no_items_found, null);
				((TextView)convertView).setText(R.string.no_artist_found);
				convertView.setTag("");
		
				return convertView;
			
			} else if (convertView == null || !convertView.getTag().equals(AppConstants.TAG_CONTENT)) {
				convertView = LayoutInflater.from(mContext).inflate(R.layout.fragment_search_artists_list_item, null);
				convertView.setTag(AppConstants.TAG_CONTENT);
			}
			
			/**
			 * For Search Artist screen we are adding padding for right side here
			 */
			if (addPadding) {
				int pad = mContext.getResources().getDimensionPixelSize(R.dimen.tab_bar_margin_fragment_custom_tabs);
				convertView.setPadding(0, 0, pad, 0);
			}
			
			((TextView)convertView.findViewById(R.id.txtArtistName)).setText(artist.getName());
			
			if(!((EventSeekr)mContext.getApplicationContext()).isTablet()) {
				if (artist.isOntour()) {
					convertView.findViewById(R.id.txtOnTour).setVisibility(View.VISIBLE);
				} else {
					convertView.findViewById(R.id.txtOnTour).setVisibility(View.INVISIBLE);
				}
			}
			
			CheckBox chkFollow = (CheckBox) convertView.findViewById(R.id.chkFollow);
			//Log.d(TAG, "pos = " + position + ", attending = " + artist.getAttending());
			chkFollow.setSelected(artist.getAttending() == Attending.Tracked);
			chkFollow.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Resources res = mContext.getResources();
					if (artist.getAttending() == Attending.Tracked) {
						GeneralDialogFragment generalDialogFragment = GeneralDialogFragment.newInstance(
								dialogBtnClickListener,						
								res.getString(R.string.remove_artist),  
								res.getString(R.string.are_you_sure_you_want_to_remove_this_artist),  
								res.getString(R.string.btn_cancel),  
								res.getString(R.string.btn_Ok), false);
						generalDialogFragment.show(((ActionBarActivity) mContext).getSupportFragmentManager(), "" + position);
						
					} else {
						/**
						 * This is the case, where user wants to Track an Artist. So, no dialog here.
						 */
						if (artistTrackingListener != null) {
							artistTrackingListener.onArtistTracking(mContext, getItem(position));
						}
					}
					/**
					 * Pass the position as tag. So, that in Positive button if response comes as
					 * true then we can remove that Artist.
					 */
				}
			});
			
	    	final ImageView imgArtist = (ImageView)convertView.findViewById(R.id.imgItem); 
			String key = artist.getKey(ImgResolution.LOW);
			Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
			if (bitmap != null) {
		        imgArtist.setImageBitmap(bitmap);
		        
		    } else {
		    	//Log.d(TAG, "bitmap is null, low res url = " + artist.getLowResImgUrl());
		        imgArtist.setImageBitmap(null);

		        AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
		        asyncLoadImg.loadImg(imgArtist, ImgResolution.LOW, 
		        		(AdapterView) parent, position, artist);
		    }
			
			convertView.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					List<SharedElement> sharedElements = new ArrayList<SharedElement>();

					int[] loc = ViewUtil.getLocationOnScreen(v, mContext.getResources());
					RelativeLayout.LayoutParams lp = (LayoutParams) imgArtist.getLayoutParams();
					
					SharedElementPosition sharedElementPosition = new SharedElementPosition(lp.leftMargin, 
							loc[1] + lp.topMargin, lp.width, lp.height);
					SharedElement sharedElement = new SharedElement(sharedElementPosition, imgArtist);
					sharedElements.add(sharedElement);
					customSharedElementTransitionSource.addViewsToBeHidden(imgArtist);
					
					((ArtistListener)mContext).onArtistSelected(artist, sharedElements);

					customSharedElementTransitionSource.onPushedToBackStack();
				}
			});
		}
		
		return convertView;
	}
	
	public void unTrackArtistAt(final int position) {
		if (artistTrackingListener != null) {
			artistTrackingListener.onArtistTracking(mContext, getItem(position));
			notifyDataSetChanged();
		}
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

	public void setAddPadding(boolean addPadding) {
		this.addPadding = addPadding;
	}
	
}
