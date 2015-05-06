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

public class SportsArtistsFragmentTab extends FragmentLoadableFromBackStack implements View.OnClickListener {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_sports_artists_tab, null);
		
		view.findViewById(R.id.btnNFL).setOnClickListener(this);
		view.findViewById(R.id.btnNBA).setOnClickListener(this);
		view.findViewById(R.id.btnNHL).setOnClickListener(this);
		view.findViewById(R.id.btnMLB).setOnClickListener(this);
		view.findViewById(R.id.btnMLS).setOnClickListener(this);

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
				FragmentUtil.getResources(this).getString(R.string.title_popular_sports));
	}
	
	@Override
	public String getScreenName() {
		return ScreenNames.POPULAR_SPORTS_ARTISTS;
	}

	@Override
	public void onClick(View v) {
		Bundle args = new Bundle();
		Intent intent = null;
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
    public void onDestroyView() {
        //to free the memory allocated for high-res cat images
        ((ImageView) getView().findViewById(R.id.imgBG)).setImageBitmap(null);
        ((ImageView) getView().findViewById(R.id.imgBG1)).setImageBitmap(null);
        ((ImageView) getView().findViewById(R.id.imgBG2)).setImageBitmap(null);
        ((ImageView) getView().findViewById(R.id.imgBG3)).setImageBitmap(null);
        ((ImageView) getView().findViewById(R.id.imgBG4)).setImageBitmap(null);
        super.onDestroyView();
    }
}
