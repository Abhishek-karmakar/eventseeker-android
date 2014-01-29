package com.wcities.eventseeker.bosch;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.bosch.adapter.BoschDateWiseEventListAdapter;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.custom.fragment.ListFragmentLoadableFromBackStack;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.DateWiseEventList;

public class BoschArtistEventsFragment extends ListFragmentLoadableFromBackStack implements OnClickListener {

	private Artist artist;
	private BoschDateWiseEventListAdapter adapter;
	private DateWiseEventList dateWiseEvtList;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (artist == null) {
			artist = (Artist) getArguments().getSerializable(BundleKeys.ARTIST);
		}
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

<<<<<<< HEAD
=======
		((BoschMainActivity)FragmentUtil.getActivity(this)).updateBoschActionBarTitle(artist.getName());
		
>>>>>>> e3b1c16eab66d9f93e532d1a1813b1ac3b884d41
		if (dateWiseEvtList == null) {

			dateWiseEvtList = new DateWiseEventList();
			dateWiseEvtList.addEventListItems(artist.getEvents(), null);

			adapter = new BoschDateWiseEventListAdapter(FragmentUtil.getActivity(this), dateWiseEvtList, null, null);

		} else {
			adapter.updateContext(FragmentUtil.getActivity(this));
		}

		setListAdapter(adapter);

		getListView().setDivider(null);
	}

	//Change title of the action bar and remove this temporary one
	@Override
	public void onResume() {
		super.onResume();
		BOSCHMainActivity activity = (BOSCHMainActivity)FragmentUtil.getActivity(this);
		activity.onFragmentResumed(this, AppConstants.INVALID_INDEX, getResources().getString(R.string.title_events));
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
		
}
