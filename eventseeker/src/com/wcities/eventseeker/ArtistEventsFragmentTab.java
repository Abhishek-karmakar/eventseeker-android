package com.wcities.eventseeker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wcities.eventseeker.adapter.DateWiseMyEventListAdapter;
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener;
import com.wcities.eventseeker.util.FragmentUtil;

public class ArtistEventsFragmentTab extends ArtistEventsParentFragment {

	private static final String TAG = ArtistEventsFragmentTab.class.getName();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return super.onCreateView(inflater, container, savedInstanceState);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	protected void updateFollowingFooter() {
		fragmentArtistDetailsFooter.setVisibility(View.GONE);
	}

	@Override
	protected DateWiseEventParentAdapterListener getAdapterInstance() {
		return new DateWiseMyEventListAdapter(FragmentUtil.getActivity(this), getChildFragmentManager(), 
				dateWiseEventList, null, this);
	}
	
}
