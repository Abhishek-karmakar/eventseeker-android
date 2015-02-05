package com.wcities.eventseeker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wcities.eventseeker.SettingsFragment.OnSettingsItemClickedListener;
import com.wcities.eventseeker.SettingsFragment.SettingsItem;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.ReplaceFragmentListener;
import com.wcities.eventseeker.util.FragmentUtil;

public class FollowMoreArtistsFragment extends FragmentLoadableFromBackStack implements View.OnClickListener {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_follows_more_artist, null);
		
		view.findViewById(R.id.btnSyncAccounts).setOnClickListener(this);
		view.findViewById(R.id.btnPopularArtists).setOnClickListener(this);
		view.findViewById(R.id.btnRecommended).setOnClickListener(this);
		view.findViewById(R.id.btnSearch).setOnClickListener(this);
		
		return view;
	}
	
	@Override
	public String getScreenName() {
		return "Follow More Artist";
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.btnSyncAccounts:
				((OnSettingsItemClickedListener) FragmentUtil.getActivity(this))
					.onSettingsItemClicked(SettingsItem.SYNC_ACCOUNTS, null);
				break;
				
			case R.id.btnPopularArtists:
				((ReplaceFragmentListener) FragmentUtil.getActivity(this))
					.replaceByFragment(AppConstants.FRAGMENT_TAG_POPULAR_ARTISTS, null);
				break;
				
			case R.id.btnRecommended:
				((ReplaceFragmentListener) FragmentUtil.getActivity(this))
					.replaceByFragment(AppConstants.FRAGMENT_TAG_RECOMMENDED_ARTISTS, null);
				break;
				
			case R.id.btnSearch:
				((MainActivity) FragmentUtil.getActivity(this)).expandSearchView();
				break;
		}
	}

}
