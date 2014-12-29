package com.wcities.eventseeker;

import com.wcities.eventseeker.util.FragmentUtil;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ListView;

public class FollowingFragment extends FollowingParentFragment {

	private static final String TAG = FollowingFragment.class.getName();
	private ListView listFollowing;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		((MainActivity) FragmentUtil.getActivity(this)).setVStatusBarColor(R.color.colorPrimaryDark);
		((MainActivity) FragmentUtil.getActivity(this)).setVStatusBarVisibility(View.VISIBLE);

		View v = super.onCreateView(inflater, container, savedInstanceState);
		listFollowing = (ListView) v.findViewById(android.R.id.list);
		btnFollowMoreArtists = (Button) v.findViewById(R.id.btnFollowMoreArtists);
		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		listFollowing.setDivider(null);
	}

	@Override
	protected AbsListView getScrollableView() {
		return listFollowing;
	}
	
}
