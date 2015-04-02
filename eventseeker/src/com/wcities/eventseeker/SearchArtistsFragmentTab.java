package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.List;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.AsyncTask.Status;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.wcities.eventseeker.adapter.RVSearchArtistsAdapterTab;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.asynctask.LoadArtists;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.interfaces.FullScrnProgressListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.ItemDecorationItemOffset;

public class SearchArtistsFragmentTab extends Fragment implements FullScrnProgressListener, LoadItemsInBackgroundListener, 
		AsyncTaskListener<Void> {
	
	private static final int GRID_COLS_PORTRAIT = 2;
	private static final int GRID_COLS_LANDSCAPE = 3;
	
	private RecyclerView recyclerVArtists;
	private RelativeLayout rltLytProgressBar;
	
	private List<Artist> artistList;
	private String query;
	
	private LoadArtists loadArtists;
	
	private RVSearchArtistsAdapterTab<String> rvSearchArtistsAdapterTab;
	
	private Handler handler;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_search_artists_tab, null);
		
		recyclerVArtists = (RecyclerView) v.findViewById(R.id.recyclerVArtists);
		int spanCount = (FragmentUtil.getResources(this).getConfiguration().orientation == 
				Configuration.ORIENTATION_PORTRAIT) ? GRID_COLS_PORTRAIT : GRID_COLS_LANDSCAPE;
		GridLayoutManager gridLayoutManager = new GridLayoutManager(FragmentUtil.getActivity(this), spanCount);
		recyclerVArtists.setHasFixedSize(true);
		recyclerVArtists.setLayoutManager(gridLayoutManager);
		
		rltLytProgressBar = (RelativeLayout) v.findViewById(R.id.rltLytProgressBar);
		// Applying background here since overriding background doesn't work from xml with <include> layout
		rltLytProgressBar.setBackgroundResource(R.drawable.bg_no_content_overlay_tab);
		
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if (artistList == null) {
			artistList = new ArrayList<Artist>();
			
			Bundle args = getArguments();
			if (args != null && args.containsKey(BundleKeys.QUERY)) {
				artistList.add(null);
				query = args.getString(BundleKeys.QUERY);
			}
			
			rvSearchArtistsAdapterTab = new RVSearchArtistsAdapterTab<String>(this);
		}
		
		Resources res = FragmentUtil.getResources(this);
		recyclerVArtists.addItemDecoration(new ItemDecorationItemOffset(res.getDimensionPixelSize(
				R.dimen.rv_item_l_r_offset_search_artists_tab), res.getDimensionPixelSize(R.dimen.rv_item_t_b_offset_search_artists_tab)));
		recyclerVArtists.setAdapter(rvSearchArtistsAdapterTab);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		//Log.d(TAG, "onDestroy()");
		if (loadArtists != null && loadArtists.getStatus() != Status.FINISHED) {
			loadArtists.cancel(true);
		}
	}

	public List<Artist> getArtistList() {
		return artistList;
	}

	@Override
	public void displayFullScrnProgress() {
		rltLytProgressBar.setVisibility(View.VISIBLE);
	}

	@Override
	public void loadItemsInBackground() {
		loadArtists = new LoadArtists(Api.OAUTH_TOKEN, artistList, rvSearchArtistsAdapterTab, 
				FragmentUtil.getApplication(this).getWcitiesId(), this);
		rvSearchArtistsAdapterTab.setLoadArtists(loadArtists);
		AsyncTaskUtil.executeAsyncTask(loadArtists, true, query);
	}

	@Override
	public void onTaskCompleted(Void... params) {
		handler.post(new Runnable() {
			
			@Override
			public void run() {
				//Log.d(TAG, "onEventsLoaded()");
				// remove full screen progressbar
				rltLytProgressBar.setVisibility(View.INVISIBLE);
			}
		});
	}
}
