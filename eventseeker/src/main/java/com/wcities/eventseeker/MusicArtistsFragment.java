package com.wcities.eventseeker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.core.Artist.Genre;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.util.FragmentUtil;

public class MusicArtistsFragment extends FragmentLoadableFromBackStack implements View.OnClickListener {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_music_artists, null);

		view.findViewById(R.id.imgRock).setOnClickListener(this);
		view.findViewById(R.id.imgHipHop).setOnClickListener(this);
		view.findViewById(R.id.imgPop).setOnClickListener(this);
		view.findViewById(R.id.imgFolk).setOnClickListener(this);
		view.findViewById(R.id.imgCountry).setOnClickListener(this);
		view.findViewById(R.id.imgElectronic).setOnClickListener(this);
		view.findViewById(R.id.imgSoulRAndB).setOnClickListener(this);
		view.findViewById(R.id.imgJazz).setOnClickListener(this);
		view.findViewById(R.id.imgClassical).setOnClickListener(this);
		view.findViewById(R.id.imgBlues).setOnClickListener(this);
		view.findViewById(R.id.imgMetal).setOnClickListener(this);
		view.findViewById(R.id.imgWorld).setOnClickListener(this);

		return view;
	}
	
	@Override
	public String getScreenName() {
		return ScreenNames.POPULAR_MUSIC_ARTISTS;
	}

	@Override
	public void onClick(View v) {
		Bundle args = new Bundle();
		switch (v.getId()) {
		case R.id.imgRock:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_rock);
			args.putSerializable(BundleKeys.GENRE, Genre.Rock);
			((MainActivity) FragmentUtil.getActivity(this))
					.replaceByFragment(AppConstants.FRAGMENT_TAG_SELECTED_ARTIST_CATEGORY_FRAGMENT, args);
			break;
		case R.id.imgFolk:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_folk);
			args.putSerializable(BundleKeys.GENRE, Genre.Folk);
			((MainActivity) FragmentUtil.getActivity(this))
					.replaceByFragment(AppConstants.FRAGMENT_TAG_SELECTED_ARTIST_CATEGORY_FRAGMENT, args);
			break;
		case R.id.imgCountry:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_country);
			args.putSerializable(BundleKeys.GENRE, Genre.Country);
			((MainActivity) FragmentUtil.getActivity(this))
					.replaceByFragment(AppConstants.FRAGMENT_TAG_SELECTED_ARTIST_CATEGORY_FRAGMENT, args);
			break;
		case R.id.imgElectronic:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_electronic);
			args.putSerializable(BundleKeys.GENRE, Genre.Electronic);
			((MainActivity) FragmentUtil.getActivity(this))
					.replaceByFragment(AppConstants.FRAGMENT_TAG_SELECTED_ARTIST_CATEGORY_FRAGMENT, args);
			break;
		case R.id.imgPop:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_pop);
			args.putSerializable(BundleKeys.GENRE, Genre.Pop);
			((MainActivity) FragmentUtil.getActivity(this))
					.replaceByFragment(AppConstants.FRAGMENT_TAG_SELECTED_ARTIST_CATEGORY_FRAGMENT, args);
			break;
		case R.id.imgJazz:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_jazz);
			args.putSerializable(BundleKeys.GENRE, Genre.Jazz);
			((MainActivity) FragmentUtil.getActivity(this))
					.replaceByFragment(AppConstants.FRAGMENT_TAG_SELECTED_ARTIST_CATEGORY_FRAGMENT, args);
			break;
		case R.id.imgMetal:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_metal);
			args.putSerializable(BundleKeys.GENRE, Genre.Metal);
			((MainActivity) FragmentUtil.getActivity(this))
					.replaceByFragment(AppConstants.FRAGMENT_TAG_SELECTED_ARTIST_CATEGORY_FRAGMENT, args);
			break;
		case R.id.imgWorld:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_world);
			args.putSerializable(BundleKeys.GENRE, Genre.World);
			((MainActivity) FragmentUtil.getActivity(this))
					.replaceByFragment(AppConstants.FRAGMENT_TAG_SELECTED_ARTIST_CATEGORY_FRAGMENT, args);
			break;
		case R.id.imgBlues:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_blues);
			args.putSerializable(BundleKeys.GENRE, Genre.Blues);
			((MainActivity) FragmentUtil.getActivity(this))
					.replaceByFragment(AppConstants.FRAGMENT_TAG_SELECTED_ARTIST_CATEGORY_FRAGMENT, args);
			break;
		case R.id.imgHipHop:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_hip_hop);
			args.putSerializable(BundleKeys.GENRE, Genre.HipHop);
			((MainActivity) FragmentUtil.getActivity(this))
					.replaceByFragment(AppConstants.FRAGMENT_TAG_SELECTED_ARTIST_CATEGORY_FRAGMENT, args);
			break;
		case R.id.imgSoulRAndB:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_soul_r_and_b);
			args.putSerializable(BundleKeys.GENRE, Genre.SoulRAndB);
			((MainActivity) FragmentUtil.getActivity(this))
					.replaceByFragment(AppConstants.FRAGMENT_TAG_SELECTED_ARTIST_CATEGORY_FRAGMENT, args);
			break;
		case R.id.imgClassical:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_classical);
			args.putSerializable(BundleKeys.GENRE, Genre.Classical);
			((MainActivity) FragmentUtil.getActivity(this))
					.replaceByFragment(AppConstants.FRAGMENT_TAG_SELECTED_ARTIST_CATEGORY_FRAGMENT, args);
			break;
		}
	}	
}
