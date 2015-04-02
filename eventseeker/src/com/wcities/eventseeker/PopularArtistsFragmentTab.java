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

public class PopularArtistsFragmentTab extends FragmentLoadableFromBackStack implements View.OnClickListener {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_popular_artists_tab, null);
		
		view.findViewById(R.id.btnFeatured).setOnClickListener(this);
		view.findViewById(R.id.btnMusic).setOnClickListener(this);
		view.findViewById(R.id.btnComedy).setOnClickListener(this);
		view.findViewById(R.id.btnTheater).setOnClickListener(this);
		view.findViewById(R.id.btnSports).setOnClickListener(this);
		
		view.findViewById(R.id.btnSyncAccounts).setOnClickListener(this);
		view.findViewById(R.id.btnRecommended).setOnClickListener(this);
		view.findViewById(R.id.btnSearch).setOnClickListener(this);
		
		Button btnPopularArtists = (Button) view.findViewById(R.id.btnPopularArtists);
		btnPopularArtists.setOnClickListener(this);
		btnPopularArtists.setSelected(true);
		btnPopularArtists.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_popular_artists_link_pressed, 0, 0);		
		return view;
	}

	@Override
	public void onResume() {
		super.onResume(AppConstants.INVALID_INDEX, 
				FragmentUtil.getResources(this).getString(R.string.title_popular));
	}
	
	@Override
	public String getScreenName() {
		return ScreenNames.POPULAR_ARTISTS_SCREEN;
	}
	
	@Override
	public void onClick(View v) {
		Bundle args;
		switch (v.getId()) {
		
			case R.id.btnFeatured:
				args = new Bundle();
				args.putSerializable(BundleKeys.GENRE, Genre.Featured);
				args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_featured_list);
				
				SelectedArtistCategoryFragmentTab selectedArtistCategoryFragmentTab = new SelectedArtistCategoryFragmentTab();
				selectedArtistCategoryFragmentTab.setArguments(args);
				
				((PopularArtistsActivityTab) FragmentUtil.getActivity(this)).replaceFragment(R.id.content_frame, 
						selectedArtistCategoryFragmentTab, FragmentUtil.getTag(selectedArtistCategoryFragmentTab), true);
				break;
				
			case R.id.btnMusic:
				MusicArtistsFragmentTab musicArtistsFragmentTab = new MusicArtistsFragmentTab();
				
				((PopularArtistsActivityTab) FragmentUtil.getActivity(this)).replaceFragment(R.id.content_frame, 
						musicArtistsFragmentTab, FragmentUtil.getTag(musicArtistsFragmentTab), true);
				break;
				
			case R.id.btnComedy:
				args = new Bundle();
				args.putSerializable(BundleKeys.GENRE, Genre.Comedy);
				args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_comedy);
				
				selectedArtistCategoryFragmentTab = new SelectedArtistCategoryFragmentTab();
				selectedArtistCategoryFragmentTab.setArguments(args);
				
				((PopularArtistsActivityTab) FragmentUtil.getActivity(this)).replaceFragment(R.id.content_frame, 
						selectedArtistCategoryFragmentTab, FragmentUtil.getTag(selectedArtistCategoryFragmentTab), true);
				break;
				
			case R.id.btnTheater:
				args = new Bundle();
				args.putSerializable(BundleKeys.GENRE, Genre.Theater);
				args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_theater);

				selectedArtistCategoryFragmentTab = new SelectedArtistCategoryFragmentTab();
				selectedArtistCategoryFragmentTab.setArguments(args);
				
				((PopularArtistsActivityTab) FragmentUtil.getActivity(this)).replaceFragment(R.id.content_frame, 
						selectedArtistCategoryFragmentTab, FragmentUtil.getTag(selectedArtistCategoryFragmentTab), true);
				break;
				
			case R.id.btnSports:
				SportsArtistsFragmentTab sportsArtistsFragmentTab = new SportsArtistsFragmentTab();
				
				((PopularArtistsActivityTab) FragmentUtil.getActivity(this)).replaceFragment(R.id.content_frame, 
						sportsArtistsFragmentTab, FragmentUtil.getTag(sportsArtistsFragmentTab), true);
				break;

				/**
				 * Tabs Buttons below:
				 */
				
			case R.id.btnSyncAccounts:
				Intent intent = new Intent(FragmentUtil.getApplication(this), ConnectAccountsActivityTab.class);
				startActivity(intent);
				FragmentUtil.getActivity(this).finish();
				break;

			case R.id.btnPopularArtists:
				intent = new Intent(FragmentUtil.getApplication(this), PopularArtistsActivityTab.class);
				startActivity(intent);
				FragmentUtil.getActivity(this).finish();
				break;
				
			case R.id.btnRecommended:
				intent = new Intent(FragmentUtil.getApplication(this), RecommendedArtistsActivityTab.class);
				startActivity(intent);
				FragmentUtil.getActivity(this).finish();
				break;

				
			case R.id.btnSearch:
				//((MainActivity) FragmentUtil.getActivity(this)).expandSearchView();
				break;
		}
	}

}
