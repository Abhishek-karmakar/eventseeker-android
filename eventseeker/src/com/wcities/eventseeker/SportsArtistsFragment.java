package com.wcities.eventseeker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Artist.Genre;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.util.FragmentUtil;

public class SportsArtistsFragment extends FragmentLoadableFromBackStack implements View.OnClickListener {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_sports_artists, null);
		
		view.findViewById(R.id.btnNFL).setOnClickListener(this);
		view.findViewById(R.id.btnNBA).setOnClickListener(this);
		view.findViewById(R.id.btnNHL).setOnClickListener(this);
		view.findViewById(R.id.btnMLB).setOnClickListener(this);
		view.findViewById(R.id.btnMLS).setOnClickListener(this);
		
		return view;
	}
	
	@Override
	public String getScreenName() {
		return "Popular Artists Screen - Sports";
	}

	@Override
	public void onClick(View v) {
		Bundle args = new Bundle();
		switch (v.getId()) {
			case R.id.btnNFL:
				args.putSerializable(BundleKeys.GENRE, Genre.NFL);
				args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_nfl);
				break;
				
			case R.id.btnNBA:
				args.putSerializable(BundleKeys.GENRE, Genre.NBA);
				args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_nba);
				break;
				
			case R.id.btnNHL:
				args.putSerializable(BundleKeys.GENRE, Genre.NHL);
				args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_nhl);
				break;
				
			case R.id.btnMLB:
				args.putSerializable(BundleKeys.GENRE, Genre.MLB);
				args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_mlb);
				break;
				
			case R.id.btnMLS:
				args.putSerializable(BundleKeys.GENRE, Genre.MLS);
				args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_mls);
				break;
		}
		((MainActivity) FragmentUtil.getActivity(this))
			.replaceByFragment(AppConstants.FRAGMENT_TAG_SELECTED_ARTIST_CATEGORY_FRAGMENT, args);
	}

}
