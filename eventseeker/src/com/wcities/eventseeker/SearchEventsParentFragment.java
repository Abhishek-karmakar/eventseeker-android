package com.wcities.eventseeker;

import android.content.res.Configuration;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wcities.eventseeker.SearchFragment.SearchFragmentChildListener;
import com.wcities.eventseeker.adapter.DateWiseEventListAdapter;
import com.wcities.eventseeker.adapter.DateWiseEventListAdapter.DateWiseEventListAdapterListener;
import com.wcities.eventseeker.asynctask.LoadDateWiseEvents;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.DateWiseEventList;

public class SearchEventsParentFragment extends ListFragment {

	private static final String TAG = SearchEventsParentFragment.class
			.getName();

	protected static final int MILES_LIMIT = 10000;

	protected String query;
	protected DateWiseEventList eventList;
	protected LoadDateWiseEvents loadEvents;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = super.onCreateView(inflater, container, savedInstanceState);

		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			int pad = getResources().getDimensionPixelSize(
					R.dimen.tab_bar_margin_fragment_custom_tabs);
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
