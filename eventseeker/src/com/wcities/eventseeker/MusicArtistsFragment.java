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

public class MusicArtistsFragment extends FragmentLoadableFromBackStack implements View.OnClickListener {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		((MainActivity) FragmentUtil.getActivity(this)).setVStatusBarColor(R.color.colorPrimaryDark);
		((MainActivity) FragmentUtil.getActivity(this)).setVStatusBarVisibility(View.VISIBLE);
		
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_music_artists, null);
		return view;
	}
	
	@Override
	public String getScreenName() {
		return "";
	}

	@Override
	public void onClick(View v) {
		Bundle args = new Bundle();
		switch (v.getId()) {
		case R.id.btnAlternativeRock:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_alternative_rock);
			args.putSerializable(BundleKeys.GENRE, Genre.AlternativeRock);
			((MainActivity) FragmentUtil.getActivity(this))
					.replaceByFragment(AppConstants.FRAGMENT_TAG_SELECTED_ARTIST_CATEGORY_FRAGMENT, args);
			break;
		case R.id.btnClassicRock:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_classic_rock);
			args.putSerializable(BundleKeys.GENRE, Genre.ClassicRock);
			((MainActivity) FragmentUtil.getActivity(this))
					.replaceByFragment(AppConstants.FRAGMENT_TAG_SELECTED_ARTIST_CATEGORY_FRAGMENT, args);
			break;
		case R.id.btnIndieRock:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_indie_rock);
			args.putSerializable(BundleKeys.GENRE, Genre.IndieRock);
			((MainActivity) FragmentUtil.getActivity(this))
					.replaceByFragment(AppConstants.FRAGMENT_TAG_SELECTED_ARTIST_CATEGORY_FRAGMENT, args);
			break;
		case R.id.btnFolk:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_folk);
			args.putSerializable(BundleKeys.GENRE, Genre.Folk);
			((MainActivity) FragmentUtil.getActivity(this))
					.replaceByFragment(AppConstants.FRAGMENT_TAG_SELECTED_ARTIST_CATEGORY_FRAGMENT, args);
			break;
		case R.id.btnCountry:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_country);
			args.putSerializable(BundleKeys.GENRE, Genre.CountryAndWestern);
			((MainActivity) FragmentUtil.getActivity(this))
					.replaceByFragment(AppConstants.FRAGMENT_TAG_SELECTED_ARTIST_CATEGORY_FRAGMENT, args);
			break;
		case R.id.btnElectronic:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_electronic);
			args.putSerializable(BundleKeys.GENRE, Genre.Electronic);
			((MainActivity) FragmentUtil.getActivity(this))
					.replaceByFragment(AppConstants.FRAGMENT_TAG_SELECTED_ARTIST_CATEGORY_FRAGMENT, args);
			break;
		case R.id.btnPop:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_pop);
			args.putSerializable(BundleKeys.GENRE, Genre.Pop);
			((MainActivity) FragmentUtil.getActivity(this))
					.replaceByFragment(AppConstants.FRAGMENT_TAG_SELECTED_ARTIST_CATEGORY_FRAGMENT, args);
			break;
		case R.id.btnPunk:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_punk);
			args.putSerializable(BundleKeys.GENRE, Genre.Punk);
			((MainActivity) FragmentUtil.getActivity(this))
					.replaceByFragment(AppConstants.FRAGMENT_TAG_SELECTED_ARTIST_CATEGORY_FRAGMENT, args);
			break;
		case R.id.btnHardRockMetal:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_hard_rock_metal);
			args.putSerializable(BundleKeys.GENRE, Genre.HardRock);
			((MainActivity) FragmentUtil.getActivity(this))
					.replaceByFragment(AppConstants.FRAGMENT_TAG_SELECTED_ARTIST_CATEGORY_FRAGMENT, args);
			break;
		case R.id.btnWorldMusic:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_world_music);
			args.putSerializable(BundleKeys.GENRE, Genre.International);
			((MainActivity) FragmentUtil.getActivity(this))
					.replaceByFragment(AppConstants.FRAGMENT_TAG_SELECTED_ARTIST_CATEGORY_FRAGMENT, args);
			break;
		/*case R.id.btnBluesJazz:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_blues_jazz);
			args.putSerializable(BundleKeys.GENRE, Genre.);
			((MainActivity) FragmentUtil.getActivity(this))
					.replaceByFragment(AppConstants.FRAGMENT_TAG_SELECTED_ARTIST_CATEGORY_FRAGMENT, args);
			break;
		case R.id.btn:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_);
			args.putSerializable(BundleKeys.GENRE, Genre.);
			((MainActivity) FragmentUtil.getActivity(this))
					.replaceByFragment(AppConstants.FRAGMENT_TAG_SELECTED_ARTIST_CATEGORY_FRAGMENT, args);
			break;
		case R.id.btn:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_);
			args.putSerializable(BundleKeys.GENRE, Genre.);
			((MainActivity) FragmentUtil.getActivity(this))
					.replaceByFragment(AppConstants.FRAGMENT_TAG_SELECTED_ARTIST_CATEGORY_FRAGMENT, args);
			break;
		case R.id.btn:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_);
			args.putSerializable(BundleKeys.GENRE, Genre.);
			((MainActivity) FragmentUtil.getActivity(this))
					.replaceByFragment(AppConstants.FRAGMENT_TAG_SELECTED_ARTIST_CATEGORY_FRAGMENT, args);
			break;*/
		}
	}	
}
