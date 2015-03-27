package com.wcities.eventseeker;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.core.Artist.Genre;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.util.FragmentUtil;

public class MusicArtistsFragmentTab extends FragmentLoadableFromBackStack implements View.OnClickListener {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_music_artists_tab, null);
		view.findViewById(R.id.btnAlternativeRock).setOnClickListener(this);
		view.findViewById(R.id.btnClassicRock).setOnClickListener(this);
		view.findViewById(R.id.btnIndieRock).setOnClickListener(this);
		view.findViewById(R.id.btnFolk).setOnClickListener(this);
		view.findViewById(R.id.btnCountry).setOnClickListener(this);
		view.findViewById(R.id.btnElectronic).setOnClickListener(this);
		view.findViewById(R.id.btnPop).setOnClickListener(this);
		view.findViewById(R.id.btnPunk).setOnClickListener(this);
		view.findViewById(R.id.btnHardRockMetal).setOnClickListener(this);
		view.findViewById(R.id.btnWorldMusic).setOnClickListener(this);
		view.findViewById(R.id.btnBluesJazz).setOnClickListener(this);
		view.findViewById(R.id.btnHipHop).setOnClickListener(this);
		view.findViewById(R.id.btnSoulRAndBFunck).setOnClickListener(this);
		view.findViewById(R.id.btnClassical).setOnClickListener(this);

		view.findViewById(R.id.btnSyncAccounts).setOnClickListener(this);
		view.findViewById(R.id.btnRecommended).setOnClickListener(this);
		view.findViewById(R.id.btnSearch).setOnClickListener(this);
		
		Button btnPopularArtists = (Button) view.findViewById(R.id.btnPopularArtists);
		btnPopularArtists.setOnClickListener(this);
		btnPopularArtists.setSelected(true);
		return view;
	}
	
	@Override
	public void onResume() {
		super.onResume(AppConstants.INVALID_INDEX, 
				FragmentUtil.getResources(this).getString(R.string.title_categories));
	}
	
	@Override
	public void onClick(View v) {
		Intent intent = null;
		Bundle args = new Bundle();
		switch (v.getId()) {
		case R.id.btnAlternativeRock:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_alternative_rock);
			args.putSerializable(BundleKeys.GENRE, Genre.AlternativeRock);
			break;
		case R.id.btnClassicRock:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_classic_rock);
			args.putSerializable(BundleKeys.GENRE, Genre.ClassicRock);
			break;
		case R.id.btnIndieRock:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_indie_rock);
			args.putSerializable(BundleKeys.GENRE, Genre.IndieRock);
			break;
		case R.id.btnFolk:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_folk);
			args.putSerializable(BundleKeys.GENRE, Genre.Folk);
			break;
		case R.id.btnCountry:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_country);
			args.putSerializable(BundleKeys.GENRE, Genre.CountryAndWestern);
			break;
		case R.id.btnElectronic:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_electronic);
			args.putSerializable(BundleKeys.GENRE, Genre.Electronic);
			break;
		case R.id.btnPop:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_pop);
			args.putSerializable(BundleKeys.GENRE, Genre.Pop);
			break;
		case R.id.btnPunk:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_punk);
			args.putSerializable(BundleKeys.GENRE, Genre.Punk);
			break;
		case R.id.btnHardRockMetal:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_hard_rock_metal);
			args.putSerializable(BundleKeys.GENRE, Genre.HardRock);
			break;
		case R.id.btnWorldMusic:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_world_music);
			args.putSerializable(BundleKeys.GENRE, Genre.International);
			break;
		case R.id.btnBluesJazz:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_blues_jazz);
			args.putSerializable(BundleKeys.GENRE, Genre.Blues);
			break;
		case R.id.btnHipHop:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_hip_hop);
			args.putSerializable(BundleKeys.GENRE, Genre.HipHopAndRap);
			break;
		case R.id.btnSoulRAndBFunck:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_soul_r_and_b_funk);
			args.putSerializable(BundleKeys.GENRE, Genre.RAndBFunkAndSoul);
			break;
		case R.id.btnClassical:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_classical);
			args.putSerializable(BundleKeys.GENRE, Genre.Classical);
			break;
		
		case R.id.btnSyncAccounts:
			intent = new Intent(FragmentUtil.getApplication(this), ConnectAccountsActivityTab.class);
			break;			

		case R.id.btnPopularArtists:
			intent = new Intent(FragmentUtil.getApplication(this), PopularArtistsActivityTab.class);
			break;			
			
		case R.id.btnRecommended:
			intent = new Intent(FragmentUtil.getApplication(this), RecommendedArtistsActivityTab.class);
			break;			
			
		case R.id.btnSearch:
			//((MainActivity) FragmentUtil.getActivity(this)).expandSearchView();
			break;
		}
		if (args.containsKey(BundleKeys.SCREEN_TITLE)) {
			SelectedArtistCategoryFragmentTab selectedArtistCategoryFragmentTab = new SelectedArtistCategoryFragmentTab();
			selectedArtistCategoryFragmentTab.setArguments(args);
			
			((PopularArtistsActivityTab) FragmentUtil.getActivity(this)).replaceFragment(R.id.content_frame, 
					selectedArtistCategoryFragmentTab, FragmentUtil.getTag(selectedArtistCategoryFragmentTab), true);
		
		} else if (intent != null) {
			startActivity(intent);
			FragmentUtil.getActivity(this).finish();
		}
	}

	@Override
	public String getScreenName() {
		return ScreenNames.POPULAR_MUSIC_ARTISTS;
	}		
}
