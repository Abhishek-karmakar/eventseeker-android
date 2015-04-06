package com.wcities.eventseeker.adapter;

import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wcities.eventseeker.GeneralDialogFragment;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.SearchArtistsFragmentTab;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Artist.Attending;
import com.wcities.eventseeker.interfaces.ArtistAdapterListener;
import com.wcities.eventseeker.interfaces.ArtistListenerTab;
import com.wcities.eventseeker.interfaces.FullScrnProgressListener;
import com.wcities.eventseeker.util.FragmentUtil;

public class RVSearchArtistsAdapterTab<T> extends Adapter<RVSearchArtistsAdapterTab.ViewHolder> implements 
		ArtistAdapterListener<T> {
	
	private static final String TAG = RVSearchArtistsAdapterTab.class.getSimpleName();
	
	private SearchArtistsFragmentTab searchArtistsFragmentTab;
	private List<Artist> artistList;
	
	private RecyclerView recyclerView;
	
	private AsyncTask<T, Void, List<Artist>> loadArtists;
	private boolean isMoreDataAvailable = true, isVisible = true;
	private int artistsAlreadyRequested;
	
	private BitmapCache bitmapCache;
	
	private static enum ViewType {
		ARTIST;
	};

	public static class ViewHolder extends RecyclerView.ViewHolder {
		
		private RelativeLayout rltLytRoot, rltLytRootPrgs;
		private TextView txtArtistName;
		private CheckBox chkFollow;
		private ImageView imgItem;
		
		public ViewHolder(View itemView) {
			super(itemView);
			
			rltLytRootPrgs = (RelativeLayout) itemView.findViewById(R.id.rltLytRootPrgs);
			rltLytRoot = (RelativeLayout) itemView.findViewById(R.id.rltLytRoot);
			txtArtistName = (TextView) itemView.findViewById(R.id.txtArtistName);
			chkFollow = (CheckBox) itemView.findViewById(R.id.chkFollow);
			imgItem = (ImageView) itemView.findViewById(R.id.imgItem);
		}
	}

	public RVSearchArtistsAdapterTab(SearchArtistsFragmentTab searchArtistsFragmentTab) {
		this.searchArtistsFragmentTab = searchArtistsFragmentTab;
		
		artistList = searchArtistsFragmentTab.getArtistList();
		bitmapCache = BitmapCache.getInstance();
	}

	@Override
	public int getItemCount() {
		return artistList.size();
	}
	
	@Override
	public int getItemViewType(int position) {
		return ViewType.ARTIST.ordinal();
	}
	
	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		recyclerView = (RecyclerView) parent;
		
		View v;
		
		ViewType vType = ViewType.values()[viewType];
		switch (vType) {
		
		case ARTIST:
			v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_following_artists_list_item_tab, parent, false);
			break;
			
		default:
			v = null;
			break;
		}
		
        return new ViewHolder(v);
	}

	@Override
	public void onBindViewHolder(final ViewHolder holder, final int position) {
		//Log.d(TAG, "onBindViewHolder(), pos = " + position);
		final Artist artist = artistList.get(position);
		if (artist == null) {
			holder.itemView.setVisibility(View.VISIBLE);
			if (artistList.size() == 1) {
				// no events loaded yet
				((FullScrnProgressListener) searchArtistsFragmentTab).displayFullScrnProgress();
				
			} else {
				holder.rltLytRootPrgs.setVisibility(View.VISIBLE);
				holder.rltLytRoot.setVisibility(View.INVISIBLE);
			}
			
			if ((loadArtists == null || loadArtists.getStatus() == Status.FINISHED) && isMoreDataAvailable) {
				searchArtistsFragmentTab.loadItemsInBackground();
			}
			
		} else {
			if (artist.getId() == AppConstants.INVALID_ID) {
				searchArtistsFragmentTab.displayNoItemsFound();
				holder.itemView.setVisibility(View.INVISIBLE);
				
			} else {
				holder.itemView.setVisibility(View.VISIBLE);
				holder.rltLytRootPrgs.setVisibility(View.INVISIBLE);
				holder.rltLytRoot.setVisibility(View.VISIBLE);
				
				holder.txtArtistName.setText(artist.getName());
				ViewCompat.setTransitionName(holder.txtArtistName, "txtArtistNameSearch" + position);
				
				holder.chkFollow.setSelected(artist.getAttending() == Attending.Tracked);
				holder.chkFollow.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						Resources res = FragmentUtil.getResources(searchArtistsFragmentTab);
						if (artist.getAttending() == Attending.Tracked) {
							GeneralDialogFragment generalDialogFragment = GeneralDialogFragment.newInstance(
									searchArtistsFragmentTab,						
									res.getString(R.string.remove_artist),  
									res.getString(R.string.are_you_sure_you_want_to_remove_this_artist),  
									res.getString(R.string.btn_cancel),  
									res.getString(R.string.btn_Ok), false);
							/**
							 * Pass the position as tag. So, that in Positive button if response comes as
							 * true then we can remove that Artist.
							 */
							generalDialogFragment.show(((ActionBarActivity) FragmentUtil.getActivity(searchArtistsFragmentTab))
									.getSupportFragmentManager(), "" + position);
							
						} else {
							// This is the case, where user wants to Track an Artist. So, no dialog here.
							searchArtistsFragmentTab.onArtistTracking(artist, position);
						}
					}
				});
				
				if (!isVisible) {
					// free memory
					holder.imgItem.setImageBitmap(null);
					
				} else {
					String key = artist.getKey(ImgResolution.LOW);
					// set tag to compare it in AsyncLoadImg before setting bitmap to imageview
			    	holder.imgItem.setTag(key);
			    	
					Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
					if (bitmap != null) {
						//Log.d(TAG, "bitmap != null");
						holder.imgItem.setImageBitmap(bitmap);
	
					} else {
						//Log.d(TAG, "bitmap = null");
						holder.imgItem.setImageBitmap(null);
	
						AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
						asyncLoadImg.loadImg(holder.imgItem, ImgResolution.LOW, recyclerView, position, artist);
					}
				}
				ViewCompat.setTransitionName(holder.imgItem, "imgArtistSearch" + position);

				holder.itemView.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						((ArtistListenerTab)FragmentUtil.getActivity(searchArtistsFragmentTab))
							.onArtistSelected(artist, holder.imgItem, holder.txtArtistName);
					}
				});
			}
		}
	}
	
	public void unTrackArtistAt(int position) {
		searchArtistsFragmentTab.onArtistTracking(artistList.get(position), position);
		notifyItemChanged(position);
	}

	public void setVisible(boolean isVisible) {
		this.isVisible = isVisible;
	}

	@Override
	public void setMoreDataAvailable(boolean isMoreDataAvailable) {
		this.isMoreDataAvailable = isMoreDataAvailable;
	}

	@Override
	public void setArtistsAlreadyRequested(int artistsAlreadyRequested) {
		this.artistsAlreadyRequested = artistsAlreadyRequested;
	}

	@Override
	public int getArtistsAlreadyRequested() {
		return artistsAlreadyRequested;
	}

	@Override
	public void updateContext(Context context) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setLoadArtists(AsyncTask<T, Void, List<Artist>> loadArtists) {
		this.loadArtists = loadArtists;
	}
}
