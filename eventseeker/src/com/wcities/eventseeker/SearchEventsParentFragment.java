package com.wcities.eventseeker;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wcities.eventseeker.asynctask.LoadDateWiseEvents;
import com.wcities.eventseeker.custom.fragment.PublishEventListFragment;
import com.wcities.eventseeker.viewdata.DateWiseEventList;

public abstract class SearchEventsParentFragment extends PublishEventListFragment {

	private static final String TAG = SearchEventsParentFragment.class.getName();

	protected static final int MILES_LIMIT = 10000;

	protected String query;
	protected DateWiseEventList eventList;
	protected LoadDateWiseEvents loadEvents;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		View v = super.onCreateView(inflater, container, savedInstanceState);

		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			int pad = getResources().getDimensionPixelSize(R.dimen.tab_bar_margin_fragment_custom_tabs);
			v.setPadding(pad, 0, pad, 0);
		}
		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		getListView().setDivider(null);
		getListView().setBackgroundResource(R.drawable.story_space);
	}

}