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

public class PopularArtistsFragment extends FragmentLoadableFromBackStack implements View.OnClickListener {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_popular_artists, null);
		
		view.findViewById(R.id.btnFeatured).setOnClickListener(this);
		view.findViewById(R.id.btnMusic).setOnClickListener(this);
		view.findViewById(R.id.btnComedy).setOnClickListener(this);
		view.findViewById(R.id.btnTheater).setOnClickListener(this);
		view.findViewById(R.id.btnSports).setOnClickListener(this);
		
		return view;
	}
	
	@Override
	public String getScreenName() {
		// TODO Auto-generated method stub
		return "";
	}

	@Override
	public void onClick(View v) {
		Bundle args;
		switch (v.getId()) {
			case R.id.btnFeatured:
				args = new Bundle();
				args.putSerializable(BundleKeys.GENRE, Genre.Featured);
				args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_featured_list);
				((MainActivity) FragmentUtil.getActivity(this))
					.replaceByFragment(AppConstants.FRAGMENT_TAG_SELECTED_ARTIST_CATEGORY_FRAGMENT, args);				
				break;
				
			case R.id.btnMusic:
				((MainActivity) FragmentUtil.getActivity(this))
					.replaceByFragment(AppConstants.FRAGMENT_TAG_MUSIC_ARTISTS, null);				
				break;
				
			case R.id.btnComedy:
				args = new Bundle();
				args.putSerializable(BundleKeys.GENRE, Genre.Comedy);
				args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_comedy);
				((MainActivity) FragmentUtil.getActivity(this))
					.replaceByFragment(AppConstants.FRAGMENT_TAG_SELECTED_ARTIST_CATEGORY_FRAGMENT, args);
				break;
				
			case R.id.btnTheater:
				args = new Bundle();
				args.putSerializable(BundleKeys.GENRE, Genre.Theater);
				args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_theater);
				((MainActivity) FragmentUtil.getActivity(this))
					.replaceByFragment(AppConstants.FRAGMENT_TAG_SELECTED_ARTIST_CATEGORY_FRAGMENT, args);
				break;
				
			case R.id.btnSports:
				((MainActivity) FragmentUtil.getActivity(this))
					.replaceByFragment(AppConstants.FRAGMENT_TAG_SPORTS_ARTISTS, null);
				break;
		}
	}

}
