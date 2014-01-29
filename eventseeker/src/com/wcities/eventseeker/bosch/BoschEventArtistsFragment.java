package com.wcities.eventseeker.bosch;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.bosch.adapter.BoschArtistListAdapter;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.custom.fragment.ListFragmentLoadableFromBackStack;
import com.wcities.eventseeker.util.FragmentUtil;

public class BoschEventArtistsFragment extends ListFragmentLoadableFromBackStack {

	private Event event;
	private List<Artist> artistList;
	private BoschArtistListAdapter<Void> boschArtistListAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		event = (Event) getArguments().getSerializable(BundleKeys.EVENT);
	}
	

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if (artistList == null) {
			
			artistList = new ArrayList<Artist>();
			artistList.addAll(event.getArtists());
			
			if (artistList.isEmpty()) {
				// add dummy item to indicate loading progress
				artistList.add(null);
			}
			
			boschArtistListAdapter = new BoschArtistListAdapter<Void>(
				FragmentUtil.getActivity(this), artistList, null, null);
			boschArtistListAdapter.setMoreDataAvailable(false);
			
		} else {
			
			boschArtistListAdapter.updateContext(FragmentUtil.getActivity(this));
		
		}
		
		setListAdapter(boschArtistListAdapter);
        getListView().setDivider(null);
        
	}
	
	@Override
	public void onResume() {
		super.onResume();
		BOSCHMainActivity activity = (BOSCHMainActivity)FragmentUtil.getActivity(this);
		String title = activity.getResources().getString(R.string.title_artists);
		activity.onFragmentResumed(this, AppConstants.INVALID_INDEX, title);
	}
	
}

