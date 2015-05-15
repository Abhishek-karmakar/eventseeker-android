package com.wcities.eventseeker;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.wcities.eventseeker.GeneralDialogFragment.DialogBtnClickListener;
import com.wcities.eventseeker.ShareOnFBDialogFragment.OnFacebookShareClickedListener;
import com.wcities.eventseeker.adapter.RVSearchArtistsAdapterTab;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingItemType;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.LoadArtists;
import com.wcities.eventseeker.asynctask.UserTracker;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Artist.Attending;
import com.wcities.eventseeker.custom.fragment.PublishArtistFragment;
import com.wcities.eventseeker.interfaces.ArtistTrackingListener;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.interfaces.FullScrnProgressListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.interfaces.SearchFragmentChildListener;
import com.wcities.eventseeker.interfaces.SwipeTabVisibilityListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FbUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.ItemDecorationItemOffset;

import java.util.ArrayList;
import java.util.List;

public class SearchArtistsFragmentTab extends PublishArtistFragment implements FullScrnProgressListener, LoadItemsInBackgroundListener, 
		AsyncTaskListener<Void>, DialogBtnClickListener, ArtistTrackingListener, OnFacebookShareClickedListener, SearchFragmentChildListener,
		SwipeTabVisibilityListener {
	
	private static final String TAG = SearchArtistsFragmentTab.class.getSimpleName();

	private static final int GRID_COLS_PORTRAIT = 2;
	private static final int GRID_COLS_LANDSCAPE = 3;
	
	private RecyclerView recyclerVArtists;
	private RelativeLayout rltLytProgressBar;
	private TextView txtNoItemsFound;
	private ImageView imgPrgOverlay;
	
	private List<Artist> artistList;
	private String query;
	
	private LoadArtists loadArtists;
	
	private RVSearchArtistsAdapterTab<String> rvSearchArtistsAdapterTab;
	
	private Handler handler;
	
	private Artist artistToBeSaved;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		handler = new Handler(Looper.getMainLooper());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_search_items_tab, null);
		recyclerVArtists = (RecyclerView) v.findViewById(R.id.recyclerVItems);
		int spanCount = (FragmentUtil.getResources(this).getConfiguration().orientation == 
				Configuration.ORIENTATION_PORTRAIT) ? GRID_COLS_PORTRAIT : GRID_COLS_LANDSCAPE;
		GridLayoutManager gridLayoutManager = new GridLayoutManager(FragmentUtil.getActivity(this), spanCount);
		recyclerVArtists.setHasFixedSize(true);
		recyclerVArtists.setLayoutManager(gridLayoutManager);
		
		rltLytProgressBar = (RelativeLayout) v.findViewById(R.id.rltLytProgressBar);
		// Applying background here since overriding background doesn't work from xml with <include> layout
		imgPrgOverlay = (ImageView) rltLytProgressBar.findViewById(R.id.imgPrgOverlay);
		
		txtNoItemsFound = (TextView) v.findViewById(R.id.txtNoItemsFound);
		
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
				R.dimen.rv_item_l_r_offset_search_items_tab), res.getDimensionPixelSize(R.dimen.rv_item_t_b_offset_search_items_tab)));
		recyclerVArtists.setAdapter(rvSearchArtistsAdapterTab);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (loadArtists != null && loadArtists.getStatus() != Status.FINISHED) {
			loadArtists.cancel(true);
		}
	}

	public List<Artist> getArtistList() {
		return artistList;
	}
	
	public void displayNoItemsFound() {
		txtNoItemsFound.setText(R.string.no_artist_found);
		txtNoItemsFound.setVisibility(View.VISIBLE);
	}
	
	private void refresh(String newQuery) {
		//Log.d(TAG, "refresh()");
		// if user selection has changed then only reset the list
		if (query == null || !query.equals(newQuery)) {
			//Log.d(TAG, "query == null || !query.equals(newQuery)");

			query = newQuery;
			rvSearchArtistsAdapterTab.setArtistsAlreadyRequested(0);
			rvSearchArtistsAdapterTab.setMoreDataAvailable(true);
			
			if (loadArtists != null && loadArtists.getStatus() != Status.FINISHED) {
				loadArtists.cancel(true);
			}
			
			txtNoItemsFound.setVisibility(View.INVISIBLE);
			
			artistList.clear();
			artistList.add(null);
			rvSearchArtistsAdapterTab.notifyDataSetChanged();
			
			loadItemsInBackground();
		}
	}

	@Override
	public void displayFullScrnProgress() {
		/**
		 * Since we are using the transparent Progressbar layout.So, we need to set background white, else
		 * in portrait mode in 10" devices the background will get visible.
		 */
		handler.post(new Runnable() {

			@Override
			public void run() {
				rltLytProgressBar.setBackgroundColor(Color.WHITE);
				rltLytProgressBar.setVisibility(View.VISIBLE);
				imgPrgOverlay.setVisibility(View.VISIBLE);
			}
		});
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
				// free up memory
				imgPrgOverlay.setBackgroundResource(0);
				imgPrgOverlay.setVisibility(View.GONE);
				rltLytProgressBar.setVisibility(View.GONE);
			}
		});
	}
	
	@Override
	public void doPositiveClick(String dialogTag) {
		//This is for Remove Artist Dialog
		rvSearchArtistsAdapterTab.unTrackArtistAt(Integer.parseInt(dialogTag));
	}

	@Override
	public void doNegativeClick(String dialogTag) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onArtistTracking(Artist artist, int position) {
		EventSeekr eventseekr = FragmentUtil.getApplication(this);
		if (artist.getAttending() == Attending.NotTracked) {
			artist.updateAttending(Attending.Tracked, eventseekr);
			new UserTracker(Api.OAUTH_TOKEN, eventseekr, UserTrackingItemType.artist, artist.getId()).execute();
			//The below notifyDataSetChange will change the status of following CheckBox for current Artist
			rvSearchArtistsAdapterTab.notifyItemChanged(position);

			ShareOnFBDialogFragment dialogFragment = ShareOnFBDialogFragment.newInstance(this);
			dialogFragment.show(((ActionBarActivity) FragmentUtil.getActivity(this)).getSupportFragmentManager(), 
						FragmentUtil.getTag(dialogFragment) + ":" + artist.getId());
			
		} else {			
			artist.updateAttending(Attending.NotTracked, eventseekr);
			new UserTracker(Api.OAUTH_TOKEN, eventseekr, UserTrackingItemType.artist, artist.getId(), 
					Attending.NotTracked.getValue(), UserTrackingType.Edit).execute();
		}
	}

	@Override
	public void onFacebookShareClicked(String dialogTag) {
		if (dialogTag.contains(FragmentUtil.getTag(ShareOnFBDialogFragment.class))) {
			String strId = dialogTag.substring(dialogTag.indexOf(":") + 1);
			//Log.d(TAG, "strId : " + strId);
			for (Artist artist : artistList) {
				if (artist != null && artist.getId() == Integer.parseInt(strId)) {					
					artistToBeSaved = artist;
					FbUtil.handlePublishArtist(this, AppConstants.PERMISSIONS_FB_PUBLISH_EVT_OR_ART,
							artist);
					break;
				}
			}
		}
	}

	@Override
	public void onSuccess(LoginResult loginResult) {
		Log.d(TAG, "onSuccess()");
		FbUtil.handlePublishArtist(this, AppConstants.PERMISSIONS_FB_PUBLISH_EVT_OR_ART,
				artistToBeSaved);
	}

	@Override
	public void onCancel() {
		Log.d(TAG, "onCancel()");
	}

	@Override
	public void onError(FacebookException e) {
		Log.d(TAG, "onError()");
	}
	
	@Override
	public void onQueryTextSubmit(String query) {
		//Log.d(TAG, "onQueryTextSubmit(), query = " + query);
		refresh(query);
	}

	@Override
	public void onInvisible() {
		if (rvSearchArtistsAdapterTab != null) {
			rvSearchArtistsAdapterTab.setVisible(false);
			rvSearchArtistsAdapterTab.notifyDataSetChanged();
		}
	}

	@Override
	public void onVisible() {
		if (rvSearchArtistsAdapterTab != null) {
			rvSearchArtistsAdapterTab.setVisible(true);
			/**
			 * need to call this because it doesn't call onBindViewHolder() automatically if 
			 * next or previous tab is selected. Calls it only for tab selection changing from position 1 to 3 or 
			 * 3 to 1
			 */
			rvSearchArtistsAdapterTab.notifyDataSetChanged();
		}
	}
}
