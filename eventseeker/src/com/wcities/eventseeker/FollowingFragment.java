package com.wcities.eventseeker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ListView;

import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.util.FragmentUtil;

public class FollowingFragment extends FollowingParentFragment {

	private static final String TAG = FollowingFragment.class.getSimpleName();
	private ListView listFollowing;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = super.onCreateView(inflater, container, savedInstanceState);
		listFollowing = (ListView) v.findViewById(android.R.id.list);
		(btnFollowMoreArtists = (Button) v.findViewById(R.id.btnFollowMoreArtists))
			.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				((MainActivity) FragmentUtil.getActivity(FollowingFragment.this))
					.replaceByFragment(AppConstants.FRAGMENT_TAG_FOLLOW_MORE_ARTISTS, null);
			}
		});
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
