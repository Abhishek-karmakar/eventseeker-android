package com.wcities.eventseeker.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.wcities.eventseeker.GeneralDialogFragment;
import com.wcities.eventseeker.GeneralDialogFragment.DialogBtnClickListener;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.SelectedArtistCategoryFragmentTab;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Artist.Attending;
import com.wcities.eventseeker.interfaces.ArtistAdapterListener;
import com.wcities.eventseeker.interfaces.ArtistListenerTab;
import com.wcities.eventseeker.interfaces.ArtistTrackingListener;
import com.wcities.eventseeker.interfaces.FullScrnProgressListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.FragmentUtil;

import java.util.List;

/**
 * This Adapter is required for 'Recommended and SelectedArtistCategory' Screens. As, the 
 * 'SectionIndexer' is not required for these fragments. So, removed implementation for the 
 * Recommended and SelectedArtistCategory Screens from MyArtistListAdapter and created this adapter. 
 * The issue in the indexer was that, the fast scrolling thumb was getting disappeared in between 
 * of scrolling.
 * @author win2
 */
public class ArtistListAdapterWithoutIndexerTab extends BaseAdapter implements ArtistAdapterListener<Void> {

	private static final String TAG = ArtistListAdapterWithoutIndexerTab.class.getSimpleName();
	
	private BitmapCache bitmapCache;
	
	private int artistsAlreadyRequested;
	private boolean isMoreDataAvailable = true;
	
	private List<Artist> artistList;
	private AsyncTask<Void, Void, List<Artist>> loadArtists;
	
	private LoadItemsInBackgroundListener mListener;
	private DialogBtnClickListener dialogBtnClickListener;
	private ArtistTrackingListener artistTrackingListener;

	private Fragment fragment;
	
	public ArtistListAdapterWithoutIndexerTab(Fragment fragment, List<Artist> artistList, 
			AsyncTask<Void, Void, List<Artist>> loadArtists, 
			LoadItemsInBackgroundListener mListener, DialogBtnClickListener 
			dialogBtnClickListener, ArtistTrackingListener artistTrackingListener) {
		
		bitmapCache = BitmapCache.getInstance();
		
		this.fragment = fragment;
		
		this.artistList = artistList;
		this.loadArtists = loadArtists;
		
		this.mListener = mListener;
		this.dialogBtnClickListener = dialogBtnClickListener;
		this.artistTrackingListener = artistTrackingListener;
	}

	@Override
	public void updateContext(Context context) {}

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
	public void setLoadArtists(AsyncTask<Void, Void, List<Artist>> loadMyArtists) {
		this.loadArtists = loadMyArtists;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		final Artist artist = getItem(position);
		if (artist == null) {
			if (convertView == null || !convertView.getTag().equals(AppConstants.TAG_PROGRESS_INDICATOR)) {
				convertView = LayoutInflater.from(FragmentUtil.getActivity(fragment))
						.inflate(R.layout.fragment_following_artists_list_item_tab, null);
				convertView.setTag(AppConstants.TAG_PROGRESS_INDICATOR);
			}
			
			convertView.findViewById(R.id.rltLytRoot).setVisibility(View.INVISIBLE);
			convertView.findViewById(R.id.rltLytRootPrgs).setVisibility(View.VISIBLE);
			if (artistList.size() == 1) {
				// Instead of this limited height progress bar, we display full screen progress bar from fragment
				if (mListener instanceof FullScrnProgressListener) {
					((FullScrnProgressListener) mListener).displayFullScrnProgress();
				}
			}

			if ((loadArtists == null || loadArtists.getStatus() == Status.FINISHED) && isMoreDataAvailable) {
				mListener.loadItemsInBackground();
			}
			
		} else {
			String transitionName = fragment instanceof SelectedArtistCategoryFragmentTab ? "PopularArtists" : "RecommenededArtists";
			
			if (convertView == null || !convertView.getTag().equals(AppConstants.TAG_CONTENT)) {
				convertView = LayoutInflater.from(FragmentUtil.getActivity(fragment))
						.inflate(R.layout.fragment_following_artists_list_item_tab, null);
				convertView.setTag(AppConstants.TAG_CONTENT);
			}

			convertView.findViewById(R.id.rltLytRootPrgs).setVisibility(View.INVISIBLE);
			
			final TextView txtArtistName = (TextView) convertView.findViewById(R.id.txtArtistName);
			txtArtistName.setText(artist.getName());
			ViewCompat.setTransitionName(txtArtistName, "txtArtistName" + transitionName + position);
			
			CheckBox chkFollow = (CheckBox) convertView.findViewById(R.id.chkFollow);
			chkFollow.setSelected(artist.getAttending() == Attending.Tracked);
			chkFollow.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Resources res = FragmentUtil.getResources(fragment);
					if (artist.getAttending() == Attending.Tracked) {
						GeneralDialogFragment generalDialogFragment = GeneralDialogFragment.newInstance(
								dialogBtnClickListener,						
								res.getString(R.string.remove_artist),  
								res.getString(R.string.are_you_sure_you_want_to_remove_this_artist),  
								res.getString(R.string.btn_cancel),  
								res.getString(R.string.btn_Ok), false);
						/**
						 * Pass the position as tag. So, that in Positive button if response comes as
						 * true then we can remove that Artist.
						 */
						generalDialogFragment.show(((ActionBarActivity) FragmentUtil.getActivity(fragment))
								.getSupportFragmentManager(), "" + position);
						
					} else {
						/**
						 * This is the case, where user wants to Track an Artist. So, no dialog here.
						 */
						if (artistTrackingListener != null) {
							artistTrackingListener.onArtistTracking(getItem(position), position);
						}
					}
				}
			});
			
			final ImageView imgArtist = (ImageView) convertView.findViewById(R.id.imgItem);
			String key = artist.getKey(ImgResolution.LOW);
			Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
			if (bitmap != null) {
				//Log.d(TAG, "bitmap != null");
				imgArtist.setImageBitmap(bitmap);

			} else {
				//Log.d(TAG, "bitmap = null");
				imgArtist.setImageBitmap(null);

				AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
				asyncLoadImg.loadImg(imgArtist, ImgResolution.LOW, (AdapterView) parent, position, artist);
			}
			
			ViewCompat.setTransitionName(imgArtist, "imgArtist" + transitionName + position);
			
			convertView.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					((ArtistListenerTab) FragmentUtil.getActivity(fragment)).onArtistSelected(artist, imgArtist, txtArtistName);
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

	public void unTrackArtistAt(final int position) {
		if (artistTrackingListener != null) {
			artistTrackingListener.onArtistTracking(getItem(position), position);
		}
	}
}
