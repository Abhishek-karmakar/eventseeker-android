package com.wcities.eventseeker;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.util.FragmentUtil;

public class VenueEventsFragment extends Fragment implements OnClickListener {

	private static final String TAG = VenueEventsFragment.class.getName();
	
	private static final String FRAGMENT_TAG_VENUE_EVENTS_LIST = "venueEventsListFragment";
	
	private Venue venue;

	private boolean isTablet;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		venue = (Venue) getArguments().getSerializable(BundleKeys.VENUE);
		isTablet = ((MainActivity)FragmentUtil.getActivity(this)).isTablet();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_venue_events, null);
        
		if(isTablet) {
			v.findViewById(R.id.lnrLayoutBtns).setVisibility(View.GONE);		
		} else {
			v.findViewById(R.id.btnPhone).setOnClickListener(this);
			v.findViewById(R.id.btnWeb).setOnClickListener(this);
			v.findViewById(R.id.btnDrive).setOnClickListener(this);			
		}
		
        VenueEventsParentListFragment fragment = (VenueEventsParentListFragment) getChildFragmentManager().findFragmentByTag(
				FRAGMENT_TAG_VENUE_EVENTS_LIST);
        if (fragment == null) {
        	addVenueEventsListFragment();
        }
		return v;
	}
	
	private void addVenueEventsListFragment() {
    	FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        
        VenueEventsParentListFragment fragment;
        
        if(((MainActivity)FragmentUtil.getActivity(this)).isTablet()) {
        	fragment = new VenueEventsListFragmentTab();
        } else {
        	fragment = new VenueEventsListFragment();
		}
        
        fragment.setArguments(getArguments());
        fragmentTransaction.add(R.id.rltLayoutRoot, fragment, FRAGMENT_TAG_VENUE_EVENTS_LIST);
        fragmentTransaction.commit();
    } 
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.btnPhone:
			if (venue.getPhone() != null) {
				Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + venue.getPhone()));
				startActivity(Intent.createChooser(intent, "Call..."));
			}
			break;
			
		case R.id.btnWeb:
			if (venue.getUrl() != null) {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(venue.getUrl()));
				startActivity(intent);
			}
			break;
			
		case R.id.btnDrive:
			((VenueDetailsFragment)getParentFragment()).onDriveClicked();
			break;
			
		default:
			break;
		}
	}
}
