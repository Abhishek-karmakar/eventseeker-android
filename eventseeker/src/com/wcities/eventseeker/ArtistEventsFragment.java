package com.wcities.eventseeker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wcities.eventseeker.ArtistDetailsFragment.FooterTxt;
import com.wcities.eventseeker.adapter.DateWiseEventListAdapter;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener;
import com.wcities.eventseeker.util.FragmentUtil;

public class ArtistEventsFragment extends ArtistEventsParentFragment {

	private static final String TAG = ArtistEventsFragment.class.getName();

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
	protected void updateFollowingFooter() {

		if (artist.getAttending() == null || 
				((EventSeekr) FragmentUtil.getActivity(this).getApplication()).getWcitiesId() == null) {
			
			fragmentArtistDetailsFooter.setVisibility(View.GONE);
		
		} else {
		
			fragmentArtistDetailsFooter.setVisibility(View.VISIBLE);

			switch (artist.getAttending()) {

				case Tracked:
					imgFollow.setImageDrawable(getResources().getDrawable(R.drawable.following));
					txtFollow.setText(FooterTxt.Following.name());
					break;
	
				case NotTracked:
					imgFollow.setImageDrawable(getResources().getDrawable(R.drawable.follow));
					txtFollow.setText(FooterTxt.Follow.name());
					break;
	
				default:
					break;
			}
		}
	}

	@Override
	protected DateWiseEventParentAdapterListener getAdapterInstance() {
		return new DateWiseEventListAdapter(FragmentUtil.getActivity(this), dateWiseEventList, null, this);
	}

}
