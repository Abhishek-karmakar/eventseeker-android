package com.wcities.eventseeker.bosch;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.bosch.BoschMainActivity.OnDisplayModeChangedListener;
import com.wcities.eventseeker.bosch.adapter.BoschArtistListAdapter;
import com.wcities.eventseeker.bosch.custom.fragment.BoschListFragmentLoadableFromBackStack;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.util.FragmentUtil;

public class BoschEventArtistsFragment extends BoschListFragmentLoadableFromBackStack implements OnClickListener, 
		OnDisplayModeChangedListener {

	private Event event;
	private List<Artist> artistList;
	private BoschArtistListAdapter<Void> boschArtistListAdapter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		event = (Event) getArguments().getSerializable(BundleKeys.EVENT);
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
			artistList.addAll(event.getArtists());
			
			if (artistList.isEmpty()) {
				// for no artist found message
				artistList.add(new Artist(AppConstants.INVALID_ID, null)); 
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
		super.onResume(AppConstants.INVALID_INDEX, getResources().getString(R.string.title_artists));
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
	public void onDisplayModeChanged(boolean isNightModeEnabled) {
		if (boschArtistListAdapter != null) {
			boschArtistListAdapter.notifyDataSetChanged();
		}
	}
}

