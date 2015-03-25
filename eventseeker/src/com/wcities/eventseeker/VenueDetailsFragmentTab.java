package com.wcities.eventseeker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.Session;
import com.facebook.SessionState;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.asynctask.LoadEventDetails;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.custom.fragment.PublishEventFragmentReatiningChildFragmentManager;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class VenueDetailsFragmentTab extends PublishEventFragmentReatiningChildFragmentManager {

	private String title = "";
	
	private int actionBarElevation;
	
	private Venue venue;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setHasOptionsMenu(true);
		
		actionBarElevation = FragmentUtil.getResources(this).getDimensionPixelSize(R.dimen.action_bar_elevation);
		
		Bundle args = getArguments();
		if (venue == null) {
			//Log.d(TAG, "event = null");
			venue = (Venue) args.getSerializable(BundleKeys.VENUE);
			
			/*loadEventDetails = new LoadEventDetails(Api.OAUTH_TOKEN, this, this, event);
			AsyncTaskUtil.executeAsyncTask(loadEventDetails, true);*/
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_venue_details_tab, container, false);
		return rootView;
	}
	
	public String getTitle() {
		return title;
	}
	
	@Override
	public void onPublishPermissionGranted() {
		// TODO Auto-generated method stub

	}

	@Override
	public void call(Session session, SessionState state, Exception exception) {
		// TODO Auto-generated method stub

	}

}
