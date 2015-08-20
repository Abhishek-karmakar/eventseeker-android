package com.wcities.eventseeker.bosch;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.asynctask.BoschLoadArtists;
import com.wcities.eventseeker.bosch.adapter.BoschArtistListAdapter;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.interfaces.ArtistListener;
import com.wcities.eventseeker.interfaces.BoschOnChildFragmentDisplayModeChangedListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FragmentUtil;

import java.util.ArrayList;
import java.util.List;

public class BoschSearchArtistsFragment extends ListFragment implements LoadItemsInBackgroundListener,
	OnClickListener, BoschOnChildFragmentDisplayModeChangedListener {

	private static final String TAG = BoschSearchArtistsFragment.class.getName();

	private String query;
	private BoschLoadArtists boschLoadArtists;
	private BoschArtistListAdapter<String> boschArtistListAdapter;
	
	private List<Artist> artistList;
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (!(activity instanceof ArtistListener)) {
            throw new ClassCastException(activity.toString() + " must implement ArtistListener");
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
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
			
			boschArtistListAdapter = new BoschArtistListAdapter<String>(FragmentUtil.getActivity(this), 
				artistList, null, this);

			Bundle args = getArguments();
			
			if (args != null && args.containsKey(BundleKeys.QUERY)) {
				artistList.add(null);
				query = args.getString(BundleKeys.QUERY);
				loadItemsInBackground();
			}
			
		} else {
			boschArtistListAdapter.updateContext(FragmentUtil.getActivity(this));
		}

		setListAdapter(boschArtistListAdapter);
        getListView().setDivider(null);
	}
	
	@Override
	public void loadItemsInBackground() {
		boschLoadArtists = new BoschLoadArtists(Api.OAUTH_TOKEN_BOSCH_APP, artistList, boschArtistListAdapter);
		boschArtistListAdapter.setLoadArtists(boschLoadArtists);
		AsyncTaskUtil.executeAsyncTask(boschLoadArtists, true, query);
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
			case R.id.btnUp:
				getListView().smoothScrollByOffset(-1);
				break;
			
			case R.id.btnDown:
				getListView().smoothScrollByOffset(1);
				break;
				
		}
	}

	@Override
	public void onChildFragmentDisplayModeChanged() {
		if (boschArtistListAdapter != null) {
			boschArtistListAdapter.notifyDataSetChanged();
		}
	}
}
