package com.wcities.eventseeker;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;

import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.core.Artist.Genre;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.util.FragmentUtil;

public class MusicArtistsFragmentTab extends FragmentLoadableFromBackStack implements View.OnClickListener {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_music_artists_tab, null);

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

		view.findViewById(R.id.btnSyncAccounts).setOnClickListener(this);
		view.findViewById(R.id.btnRecommended).setOnClickListener(this);
		view.findViewById(R.id.btnSearch).setOnClickListener(this);
		
		CheckBox btnPopularArtists = (CheckBox) view.findViewById(R.id.btnPopularArtists);
		btnPopularArtists.setOnClickListener(this);
		btnPopularArtists.setChecked(true);
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
		case R.id.imgRock:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_rock);
			args.putSerializable(BundleKeys.GENRE, Genre.Rock);
			break;
		case R.id.imgHipHop:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_hip_hop);
			args.putSerializable(BundleKeys.GENRE, Genre.HipHop);
			break;
		case R.id.imgPop:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_pop);
			args.putSerializable(BundleKeys.GENRE, Genre.Pop);
			break;
		case R.id.imgFolk:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_folk);
			args.putSerializable(BundleKeys.GENRE, Genre.Folk);
			break;
		case R.id.imgCountry:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_country);
			args.putSerializable(BundleKeys.GENRE, Genre.Country);
			break;
		case R.id.imgElectronic:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_electronic);
			args.putSerializable(BundleKeys.GENRE, Genre.Electronic);
			break;
		case R.id.imgSoulRAndB:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_soul_r_and_b);
			args.putSerializable(BundleKeys.GENRE, Genre.SoulRAndB);
			break;
		case R.id.imgJazz:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_jazz);
			args.putSerializable(BundleKeys.GENRE, Genre.Jazz);
			break;
		case R.id.imgClassical:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_classical);
			args.putSerializable(BundleKeys.GENRE, Genre.Classical);
			break;
		case R.id.imgBlues:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_blues);
			args.putSerializable(BundleKeys.GENRE, Genre.Blues);
			break;
		case R.id.imgMetal:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_metal);
			args.putSerializable(BundleKeys.GENRE, Genre.Metal);
			break;
		case R.id.imgWorld:
			args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_world);
			args.putSerializable(BundleKeys.GENRE, Genre.World);
			break;

		case R.id.btnSyncAccounts:
			((CheckBox) v).setChecked(false);
			intent = new Intent(FragmentUtil.getApplication(this), ConnectAccountsActivityTab.class);
			break;			

		case R.id.btnPopularArtists:
			((CheckBox) v).setChecked(true);
			intent = new Intent(FragmentUtil.getApplication(this), PopularArtistsActivityTab.class);
			break;			
			
		case R.id.btnRecommended:
			((CheckBox) v).setChecked(false);
			intent = new Intent(FragmentUtil.getApplication(this), RecommendedArtistsActivityTab.class);
			break;			
			
		case R.id.btnSearch:
			((CheckBox) v).setChecked(false);
			((BaseActivityTab) FragmentUtil.getActivity(this)).expandSearchView();
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

    @Override
    public void onDestroyView() {
        ((ImageView) getView().findViewById(R.id.imgRock)).setImageBitmap(null);
        ((ImageView) getView().findViewById(R.id.imgFolk)).setImageBitmap(null);
        ((ImageView) getView().findViewById(R.id.imgCountry)).setImageBitmap(null);
        ((ImageView) getView().findViewById(R.id.imgElectronic)).setImageBitmap(null);
        ((ImageView) getView().findViewById(R.id.imgPop)).setImageBitmap(null);
        ((ImageView) getView().findViewById(R.id.imgJazz)).setImageBitmap(null);
        ((ImageView) getView().findViewById(R.id.imgMetal)).setImageBitmap(null);
        ((ImageView) getView().findViewById(R.id.imgWorld)).setImageBitmap(null);
        ((ImageView) getView().findViewById(R.id.imgBlues)).setImageBitmap(null);
        ((ImageView) getView().findViewById(R.id.imgHipHop)).setImageBitmap(null);
        ((ImageView) getView().findViewById(R.id.imgSoulRAndB)).setImageBitmap(null);
        ((ImageView) getView().findViewById(R.id.imgClassical)).setImageBitmap(null);
        super.onDestroyView();
    }
}
