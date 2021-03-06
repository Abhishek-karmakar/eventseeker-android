package com.wcities.eventseeker.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.ActionBarActivity;
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
import android.widget.SectionIndexer;
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
import com.wcities.eventseeker.interfaces.ArtistAdapterListener;
import com.wcities.eventseeker.interfaces.ArtistListener;
import com.wcities.eventseeker.interfaces.ArtistTrackingListener;
import com.wcities.eventseeker.interfaces.CustomSharedElementTransitionSource;
import com.wcities.eventseeker.interfaces.FullScrnProgressListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.ViewUtil;
import com.wcities.eventseeker.viewdata.SharedElement;
import com.wcities.eventseeker.viewdata.SharedElementPosition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This Adapter is only required for 'FollowingParentFragment'. As, the 'SectionIndexer' is required
 * only for FollowingParentFragment. So, removed implementation for the Recommended and 
 * SelectedArtistCategory Screens. The issue in the indexer was that, the fast scrolling thumb was 
 * getting disappeared in between of scrolling.
 * @author win2
 */
public class MyArtistListAdapter extends BaseAdapter implements SectionIndexer, ArtistAdapterListener<Void> {

	private static final String TAG = MyArtistListAdapter.class.getSimpleName();
	private static final long DELAY_REMOVE_ARTIST = 500;
	
	private Context mContext;
	private BitmapCache bitmapCache;
	
	private int artistsAlreadyRequested;
	private boolean isMoreDataAvailable = true;
	
	private List<Artist> artistList;
	private AsyncTask<Void, Void, List<Artist>> loadMyArtists;
	
	private Map<Character, Integer> alphaNumIndexer;
	private List<Character> indices;
	
	private boolean isTablet;
	
	private LoadItemsInBackgroundListener mListener;
	private DialogBtnClickListener dialogBtnClickListener;
	private ArtistTrackingListener artistTrackingListener;

	private Handler handler;
	
    private CustomSharedElementTransitionSource customSharedElementTransitionSource;
	
	public MyArtistListAdapter(Context context, List<Artist> artistList, AsyncTask<Void, Void, List<Artist>> loadMyArtists, 
			Map<Character, Integer> alphaNumIndexer, List<Character> indices, LoadItemsInBackgroundListener mListener, 
			DialogBtnClickListener dialogBtnClickListener, ArtistTrackingListener artistTrackingListener, 
			CustomSharedElementTransitionSource customSharedElementTransitionSource) {
		if (!(context instanceof ArtistListener)) {
			throw new ClassCastException(context.toString() + " must implement ArtistListener");
		}
		mContext = context;
		bitmapCache = BitmapCache.getInstance();
		isTablet = ((EventSeekr)mContext.getApplicationContext()).isTablet();
		
		this.artistList = artistList;
		this.loadMyArtists = loadMyArtists;
		
		this.alphaNumIndexer = alphaNumIndexer;
		this.indices = indices;
		
		this.mListener = mListener;
		this.dialogBtnClickListener = dialogBtnClickListener;
		this.artistTrackingListener = artistTrackingListener;
		
		this.customSharedElementTransitionSource = customSharedElementTransitionSource;
		
		handler = new Handler(Looper.getMainLooper());
	}

	@Override
	public void updateContext(Context context) {
        mContext = context;
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
	public void setLoadArtists(AsyncTask<Void, Void, List<Artist>> loadMyArtists) {
		this.loadMyArtists = loadMyArtists;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		//Log.d(TAG, "pos = " + position);
		final Artist artist = getItem(position);
		if (artist == null) {
			if (convertView == null || !convertView.getTag().equals(AppConstants.TAG_PROGRESS_INDICATOR)) {
				if (isTablet) {
					convertView = LayoutInflater.from(mContext).inflate(R.layout.grd_progress_bar, null);
					
				} else {
					convertView = LayoutInflater.from(mContext).inflate(R.layout.progress_bar_eventseeker_fixed_ht, null);
				}
				convertView.setTag(AppConstants.TAG_PROGRESS_INDICATOR);
			}
			
			if (artistList.size() == 1) {
				// Instead of this limited height progress bar, we display full screen progress bar from fragment
				convertView.setVisibility(View.INVISIBLE);
				if (mListener instanceof FullScrnProgressListener) {
					((FullScrnProgressListener) mListener).displayFullScrnProgress();
				}
				
			} else {
				convertView.setVisibility(View.VISIBLE);
			}

			if ((loadMyArtists == null || loadMyArtists.getStatus() == Status.FINISHED) && isMoreDataAvailable) {
				mListener.loadItemsInBackground();
			}

		} else {
			
			if (convertView == null || !convertView.getTag().equals(AppConstants.TAG_CONTENT)) {
				if (isTablet) {
					convertView = LayoutInflater.from(mContext).inflate(R.layout.fragment_following_artists_list_item_tab, null);
					
				} else {
					convertView = LayoutInflater.from(mContext).inflate(R.layout.fragment_search_artists_list_item, null);
				}
				convertView.setTag(AppConstants.TAG_CONTENT);
			}

			((TextView) convertView.findViewById(R.id.txtArtistName)).setText(artist.getName());

			if (artist.isOntour()) {
				convertView.findViewById(R.id.txtOnTour).setVisibility(View.VISIBLE);

			} else {
				convertView.findViewById(R.id.txtOnTour).setVisibility(View.INVISIBLE);
			}

			CheckBox chkFollow = (CheckBox) convertView.findViewById(R.id.chkFollow);
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
							artistTrackingListener.onArtistTracking(getItem(position), position);
						}
					}
					/**
					 * Pass the position as tag. So, that in Positive button if response comes as
					 * true then we can remove that Artist.
					 */
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

			convertView.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					List<SharedElement> sharedElements = new ArrayList<SharedElement>();

					int[] loc = ViewUtil.getLocationOnScreen(v, mContext.getResources());
					RelativeLayout.LayoutParams lp = (LayoutParams) imgArtist.getLayoutParams();
					
					SharedElementPosition sharedElementPosition = new SharedElementPosition(loc[0] + lp.leftMargin, 
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
		
		/**
		 * Delay is just for the list item removal effect.
		 */
		handler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				artistList.remove(position);
				notifyDataSetChanged();		
			}
		}, DELAY_REMOVE_ARTIST);
	}
	
	@Override
	public int getPositionForSection(int sectionIndex) {
		//Log.d(TAG, "index = " + alphaNumIndexer.get(indices.get(sectionIndex)));
		if (indices.size() > sectionIndex) {
			/**
			 * for some devices it has wrong sectionIndex (out of bounds). To prevent this we 
			 * have this if clause
			 * e.g. For Galaxy S3 4.3.1 (with custom OS) it throws ArrayIndexOutOfBoundsException, 
			 * because though indices size is 1, this function is called with sectionIndex=1.
			 */
			return alphaNumIndexer.get(indices.get(sectionIndex));
			
		} else {
			return -1;
		}
	}

	@Override
	public int getSectionForPosition(int position) {
		return 0;
	}

	@Override
	public Object[] getSections() {
		return indices.toArray();
	}
}
