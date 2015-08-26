package com.wcities.eventseeker.bosch;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView.RecyclerListener;
import android.widget.ImageView;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.LoadMyArtists;
import com.wcities.eventseeker.bosch.adapter.BoschArtistListAdapter;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.FollowingList;
import com.wcities.eventseeker.interfaces.BoschOnChildFragmentDisplayModeChangedListener;
import com.wcities.eventseeker.interfaces.LoadArtistsListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FragmentUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class BoschMyArtistsListFragment extends ListFragment implements OnClickListener, 
		LoadItemsInBackgroundListener, BoschOnChildFragmentDisplayModeChangedListener, LoadArtistsListener {

	private String wcitiesId;
	private FollowingList cachedFollowingList;
	
	private LoadMyArtists loadMyArtists;
	protected BoschArtistListAdapter<Void> boschArtistListAdapter;
	
	private List<Artist> artistList;
	private SortedSet<Integer> artistIds;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (wcitiesId == null) {
			wcitiesId = ((EventSeekr) FragmentUtil.getActivity(this).getApplication()).getWcitiesId();
			cachedFollowingList = ((EventSeekr) FragmentUtil.getActivity(this).getApplication()).getCachedFollowingList();
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.bosch_common_list_layout, null);
		
        view.findViewById(R.id.btnUp).setOnClickListener(this);
		view.findViewById(R.id.btnDown).setOnClickListener(this);
		
		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if (artistList == null) {
			artistList = new ArrayList<Artist>();
			artistList.add(null);
			artistIds = new TreeSet<Integer>();
			
			boschArtistListAdapter = new BoschArtistListAdapter<Void>(FragmentUtil.getActivity(this), 
					artistList, null, this);

			loadItemsInBackground();

		} else {
			boschArtistListAdapter.updateContext(FragmentUtil.getActivity(this));
		}
		
		getListView().setRecyclerListener(new RecyclerListener() {
			
			@Override
			public void onMovedToScrapHeap(View view) {
				freeUpBitmapMemory(view);
			}
		});
		
		getListView().setDivider(null);
		getListView().setAdapter(boschArtistListAdapter);
		getListView().setScrollingCacheEnabled(false);
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		for (int i = getListView().getFirstVisiblePosition(), j = 0; i <= getListView().getLastVisiblePosition(); 
				i++, j++) {
			freeUpBitmapMemory(getListView().getChildAt(j));
		}
		super.onDestroyView();
	}
	
	protected void freeUpBitmapMemory(View view) {
		if (view.getTag().equals(AppConstants.TAG_CONTENT)) {
			((ImageView) view.findViewById(R.id.imgArtist)).setImageBitmap(null);
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.btnUp:
			getListView().setSelection(getListView().getFirstVisiblePosition() - 1);
			break;
			
		case R.id.btnDown:
			getListView().setSelection(getListView().getFirstVisiblePosition() + 1);
			break;

		default:
			break;
		}
	}

	@Override
	public void loadItemsInBackground() {
		loadMyArtists = new LoadMyArtists(Api.OAUTH_TOKEN_BOSCH_APP, wcitiesId, artistList, boschArtistListAdapter, cachedFollowingList, 
				artistIds, null, null, this);
		boschArtistListAdapter.setLoadArtists(loadMyArtists);
		AsyncTaskUtil.executeAsyncTask(loadMyArtists, true);
	}

	@Override
	public void onChildFragmentDisplayModeChanged() {
		if (boschArtistListAdapter != null) {
			boschArtistListAdapter.notifyDataSetChanged();
		}
	}
	
	@Override
	public void showNoArtistFound() {
		artistList.add(new Artist(AppConstants.INVALID_ID, null));
	}
}
