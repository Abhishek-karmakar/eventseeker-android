package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView.RecyclerListener;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wcities.eventseeker.GeneralDialogFragment.DialogBtnClickListener;
import com.wcities.eventseeker.adapter.MyArtistListAdapterTab;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingItemType;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.LoadMyArtists;
import com.wcities.eventseeker.asynctask.UserTracker;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Artist.Attending;
import com.wcities.eventseeker.core.FollowingList;
import com.wcities.eventseeker.interfaces.ArtistTrackingListener;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.interfaces.FullScrnProgressListener;
import com.wcities.eventseeker.interfaces.LoadArtistsListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class FollowingFragmentTab extends Fragment implements ArtistTrackingListener, LoadArtistsListener, 
		OnClickListener, LoadItemsInBackgroundListener, DialogBtnClickListener, FullScrnProgressListener, 
		AsyncTaskListener<Void> {

	private static final String TAG = FollowingFragmentTab.class.getName();

	private static final int NUM_COLUMNS_PORTRAIT = 2;
	private static final int NUM_COLUMNS_LANDSCAPE = 3;

	private String wcitiesId;
	private FollowingList cachedFollowingList;

	private LoadMyArtists loadMyArtists;
	protected MyArtistListAdapterTab myArtistListAdapter;

	protected List<Artist> artistList;
	private SortedSet<Integer> artistIds;
	
	private Map<Character, Integer> alphaNumIndexer;
	private List<Character> indices;

	private View rltRootNoContentFound;
	private RelativeLayout rltLytPrgsBar;
	
	private GridView grdvFollowing;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);

		if (wcitiesId == null) {
			wcitiesId = ((EventSeekr) FragmentUtil.getActivity(this).getApplication()).getWcitiesId();
			cachedFollowingList = ((EventSeekr) FragmentUtil.getActivity(this).getApplication()).getCachedFollowingList();
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_following_tab, null);
		grdvFollowing = (GridView) v.findViewById(R.id.grdvFollowing);
		grdvFollowing.setNumColumns(FragmentUtil.getResources(this).getConfiguration()
			.orientation == Configuration.ORIENTATION_PORTRAIT ? NUM_COLUMNS_PORTRAIT : NUM_COLUMNS_LANDSCAPE);
		
		rltRootNoContentFound = v.findViewById(R.id.rltRootNoContentFound);
		
		rltLytPrgsBar = (RelativeLayout) v.findViewById(R.id.rltLytPrgsBar);
		rltLytPrgsBar.setBackgroundResource(R.drawable.ic_no_content_background_overlay);
		
		v.findViewById(R.id.btnSyncAccounts).setOnClickListener(this);
		v.findViewById(R.id.btnPopularArtists).setOnClickListener(this);
		v.findViewById(R.id.btnRecommended).setOnClickListener(this);
		v.findViewById(R.id.btnSearch).setOnClickListener(this);
		
		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		// Log.d(TAG, "onActivityCreated()");
		if (artistList == null) {
			artistList = new ArrayList<Artist>();
			artistList.add(null);
			artistIds = new TreeSet<Integer>();
			
			alphaNumIndexer = new HashMap<Character, Integer>();
			indices = new ArrayList<Character>();

			myArtistListAdapter = new MyArtistListAdapterTab(this, artistList, null, alphaNumIndexer, 
					indices, this, this, this);

			loadItemsInBackground();

		} else {
			if (artistList.isEmpty()) {
				showNoArtistFound();				
			}
		}
		
		grdvFollowing.setRecyclerListener(new RecyclerListener() {
			
			@Override
			public void onMovedToScrapHeap(View view) {
				freeUpBitmapMemory(view);
			}
		});
		
		grdvFollowing.setAdapter(myArtistListAdapter);		
		grdvFollowing.setScrollingCacheEnabled(false);
		grdvFollowing.setFastScrollEnabled(true);
	}
	
	@Override
	public void loadItemsInBackground() {
		loadMyArtists = new LoadMyArtists(Api.OAUTH_TOKEN, wcitiesId, artistList, myArtistListAdapter, 
				cachedFollowingList, artistIds, indices, alphaNumIndexer, this);
		myArtistListAdapter.setLoadArtists(loadMyArtists);
		AsyncTaskUtil.executeAsyncTask(loadMyArtists, true);
	}
	
	protected void freeUpBitmapMemory(View view) {
		if (view.getTag().equals(AppConstants.TAG_CONTENT)) {
			((ImageView) view.findViewById(R.id.imgItem)).setImageBitmap(null);
		}
	}
	
	@Override
	public void onClick(View v) {
		((CheckBox) v).setChecked(false);
		
		switch (v.getId()) {
			case R.id.btnSyncAccounts:
				Intent intent = new Intent(FragmentUtil.getApplication(this), ConnectAccountsActivityTab.class);
				startActivity(intent);
				break;
				
			case R.id.btnPopularArtists:
				intent = new Intent(FragmentUtil.getApplication(this), PopularArtistsActivityTab.class);
				startActivity(intent);
				break;
				
			case R.id.btnRecommended:
				intent = new Intent(FragmentUtil.getApplication(this), RecommendedArtistsActivityTab.class);
				startActivity(intent);
				break;
				
			case R.id.btnSearch:
				((BaseActivityTab) FragmentUtil.getActivity(this)).expandSearchView();
				break;
		}
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		for (int i = grdvFollowing.getFirstVisiblePosition(), j = 0; 
				i <= grdvFollowing.getLastVisiblePosition(); 
				i++, j++) {
			freeUpBitmapMemory(grdvFollowing.getChildAt(j));
		}
	}
	
	@Override
	public void showNoArtistFound() {
		/**
		 * try-catch is used to handle case where even before we get call back to this function, user leaves 
		 * this screen.
		 */
		try {
			grdvFollowing.setVisibility(View.GONE);
			
		} catch (IllegalStateException e) {
			Log.e(TAG, "" + e.getMessage());
			e.printStackTrace();
		}
		
		TextView txtNoContentMsg = (TextView) rltRootNoContentFound.findViewById(R.id.txtNoItemsMsg);
		txtNoContentMsg.setText(R.string.following_no_content);
		txtNoContentMsg.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_list_follow, 0, 0);
		
		((ImageView) rltRootNoContentFound.findViewById(R.id.imgPhone))
			.setImageDrawable(FragmentUtil.getResources(this).getDrawable(R.drawable.ic_following_no_content));
		
		rltRootNoContentFound.setVisibility(View.VISIBLE);
	}

	@Override
	public void onArtistTracking(Artist artist, int position) {
		EventSeekr eventseekr = FragmentUtil.getApplication(this);
		artist.updateAttending(Attending.NotTracked, eventseekr );
		new UserTracker(Api.OAUTH_TOKEN, eventseekr, UserTrackingItemType.artist, artist.getId(), 
				Attending.NotTracked.getValue(), UserTrackingType.Edit).execute();
	}

	@Override
	public void doPositiveClick(String dialogTag) {
		/**
		 * Here in this case we would have passed the position of the Artist as its tag.
		 */
		myArtistListAdapter.unTrackArtistAt(Integer.parseInt(dialogTag));
	}

	@Override
	public void doNegativeClick(String dialogTag) {}
	
	@Override
	public void displayFullScrnProgress() {
		rltLytPrgsBar.setVisibility(View.VISIBLE);
	}
	
	@Override
	public void onTaskCompleted(Void... params) {
		rltLytPrgsBar.setVisibility(View.INVISIBLE);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		//Log.d(TAG, "onDestroy()");
		if (loadMyArtists != null && loadMyArtists.getStatus() != Status.FINISHED) {
			loadMyArtists.cancel(true);
		}
	}
}
