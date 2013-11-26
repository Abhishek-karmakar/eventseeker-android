package com.wcities.eventseeker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

public class FollowingFragmentTab extends FollowingParentFragment {

	private static final String TAG = FollowingFragmentTab.class.getName();
	private GridView grdFollowing;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View v = inflater.inflate(R.layout.fragment_following_tab, null);

		grdFollowing = (GridView) v.findViewById(R.id.grdFollowing);

		return v;

	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		grdFollowing.setAdapter(artistListAdapter);
		grdFollowing.setScrollingCacheEnabled(false);
	}

}
