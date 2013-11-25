package com.wcities.eventseeker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class FollowingFragment extends FollowingParentFragment {

	private static final String TAG = FollowingFragment.class.getName();
	private ListView listFollowing;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		/*
		 * View v = super.onCreateView(inflater, container, savedInstanceState);
		 * v.setBackgroundColor(getResources().getColor(R.color.dark_gray)); int
		 * pad = getResources().getDimensionPixelSize(
		 * R.dimen.tab_bar_margin_fragment_custom_tabs); v.setPadding(pad, pad,
		 * pad, 0);
		 */
		View v = inflater.inflate(R.layout.fragment_list, null);

		listFollowing = (ListView) v.findViewById(android.R.id.list);

		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		listFollowing.setAdapter(artistListAdapter);
		/*
		 * getListView().setDivider(null);
		 * getListView().setBackgroundResource(R.drawable.story_space);
		 */
	}

}
