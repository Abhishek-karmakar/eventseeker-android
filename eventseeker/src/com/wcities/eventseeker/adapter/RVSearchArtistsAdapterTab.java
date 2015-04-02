package com.wcities.eventseeker.adapter;

import java.util.List;

import android.content.Context;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.SearchArtistsFragmentTab;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.interfaces.ArtistAdapterListener;

public class RVSearchArtistsAdapterTab<T> extends Adapter<RVSearchArtistsAdapterTab.ViewHolder> implements 
		ArtistAdapterListener<T> {
	
	private SearchArtistsFragmentTab searchArtistsFragmentTab;
	private List<Artist> artistList;
	
	private RecyclerView recyclerView;
	
	private AsyncTask<T, Void, List<Artist>> loadArtists;
	private boolean isMoreDataAvailable = true;
	private int artistsAlreadyRequested;
	
	private static enum ViewType {
		ARTIST;
	};

	public static class ViewHolder extends RecyclerView.ViewHolder {
		
		private RelativeLayout rltLytRoot, rltLytRootPrgs;

		public ViewHolder(View itemView) {
			super(itemView);
			
			rltLytRootPrgs = (RelativeLayout) itemView.findViewById(R.id.rltLytRootPrgs);
			rltLytRoot = (RelativeLayout) itemView.findViewById(R.id.rltLytRoot);
		}
	}

	public RVSearchArtistsAdapterTab(SearchArtistsFragmentTab searchArtistsFragmentTab) {
		this.searchArtistsFragmentTab = searchArtistsFragmentTab;
		
		artistList = searchArtistsFragmentTab.getArtistList();
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
	public void onBindViewHolder(ViewHolder holder, int position) {
		final Artist artist = artistList.get(position);
		if (artist == null) {
			if (artistList.size() == 1) {
				// no events loaded yet
				searchArtistsFragmentTab.displayFullScrnProgress();
				
			} else {
				holder.rltLytRootPrgs.setVisibility(View.VISIBLE);
				holder.rltLytRoot.setVisibility(View.INVISIBLE);
			}
			
			if ((loadArtists == null || loadArtists.getStatus() == Status.FINISHED) && isMoreDataAvailable) {
				searchArtistsFragmentTab.loadItemsInBackground();
			}
			
		} else {
			
		}
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
