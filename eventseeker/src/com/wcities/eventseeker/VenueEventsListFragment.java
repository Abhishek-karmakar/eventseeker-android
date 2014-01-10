package com.wcities.eventseeker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.Session;
import com.facebook.SessionState;
import com.wcities.eventseeker.adapter.DateWiseEventListAdapter;
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener;
import com.wcities.eventseeker.util.FragmentUtil;

public class VenueEventsListFragment extends VenueEventsParentListFragment {
	
	private static final String TAG = VenueEventsListFragment.class.getName();

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
	protected DateWiseEventParentAdapterListener getAdapterInstance() {
		return new DateWiseEventListAdapter(FragmentUtil.getActivity(this), dateWiseEvtList, null, this);
	}

	@Override
	public void call(Session session, SessionState state, Exception exception) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPublishPermissionGranted() {
		// TODO Auto-generated method stub
		
	}
	
}
