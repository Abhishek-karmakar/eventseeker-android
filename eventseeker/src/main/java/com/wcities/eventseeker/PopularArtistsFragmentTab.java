package com.wcities.eventseeker;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RelativeLayout;

import com.wcities.eventseeker.adapter.RVPopularArtistsAdapter;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.asynctask.LoadPopularArtists;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.FeaturedListArtistCategory;
import com.wcities.eventseeker.core.PopularArtistCategory;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.interfaces.FullScrnProgressListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.interfaces.OnPopularArtistsCategoryClickListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;

import java.util.ArrayList;
import java.util.List;

public class PopularArtistsFragmentTab extends FragmentLoadableFromBackStack implements FullScrnProgressListener,
		LoadItemsInBackgroundListener, AsyncTaskListener<Void>, OnPopularArtistsCategoryClickListener, View.OnClickListener {

	private RecyclerView rvPopularArtists;
	private RelativeLayout rltLytProgressBar;

	private List<PopularArtistCategory> popularArtistCategories;
	private RVPopularArtistsAdapter popularArtistsAdapter;

	private double[] latlon;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		latlon = DeviceUtil.getLatLon(FragmentUtil.getApplication(this));
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_popular_artists_tab, null);

		rvPopularArtists = (RecyclerView) view.findViewById(R.id.rvPopularArtists);
		// use a linear layout manager
		RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(FragmentUtil.getActivity(this));
		rvPopularArtists.setLayoutManager(layoutManager);

		rltLytProgressBar = (RelativeLayout) view.findViewById(R.id.rltLytProgressBar);
		rltLytProgressBar.setBackgroundResource(R.drawable.bg_no_content_overlay);
		
		view.findViewById(R.id.btnSyncAccounts).setOnClickListener(this);
		view.findViewById(R.id.btnRecommended).setOnClickListener(this);
		view.findViewById(R.id.btnSearch).setOnClickListener(this);
		
		CheckBox btnPopularArtists = (CheckBox) view.findViewById(R.id.btnPopularArtists);
		btnPopularArtists.setOnClickListener(this);
		btnPopularArtists.setChecked(true);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (popularArtistCategories == null) {
			popularArtistCategories = new ArrayList<PopularArtistCategory>();
			popularArtistCategories.add(null);

			popularArtistsAdapter = new RVPopularArtistsAdapter(popularArtistCategories, this, this, this, this);
		}
		rvPopularArtists.setAdapter(popularArtistsAdapter);
	}

	@Override
	public void onResume() {
		super.onResume(AppConstants.INVALID_INDEX, FragmentUtil.getResources(this).getString(R.string.title_popular));
	}
	
	@Override
	public String getScreenName() {
		return ScreenNames.POPULAR_ARTISTS_SCREEN;
	}
	
	@Override
	public void onPopularArtistsCategoryClick(PopularArtistCategory popularArtistCategory) {

		Bundle args;
		switch (popularArtistCategory.getPopularArtistsType()) {

			case FeaturedListArtists:
				FeaturedListArtistCategory featuredListArtistCategory = ((FeaturedListArtistCategory) popularArtistCategory);
				args = new Bundle();
				args.putSerializable(BundleKeys.FEATURED_LIST_ARTISTS_ID, featuredListArtistCategory.getId());
				args.putString(BundleKeys.SCREEN_TITLE, featuredListArtistCategory.getName());

				SelectedFeaturedListArtistsFragmentTab selectedFeaturedListArtistsFragmentTab
					= new SelectedFeaturedListArtistsFragmentTab();
				selectedFeaturedListArtistsFragmentTab.setArguments(args);

				((PopularArtistsActivityTab) FragmentUtil.getActivity(this)).replaceFragment(R.id.content_frame,
					selectedFeaturedListArtistsFragmentTab, FragmentUtil.getTag(selectedFeaturedListArtistsFragmentTab), true);
				break;

			case FeaturedArtists:
				args = new Bundle();
				args.putSerializable(BundleKeys.GENRE, Artist.Genre.Featured);
				args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_featured_list);
				
				SelectedArtistCategoryFragmentTab selectedArtistCategoryFragmentTab = new SelectedArtistCategoryFragmentTab();
				selectedArtistCategoryFragmentTab.setArguments(args);
				
				((PopularArtistsActivityTab) FragmentUtil.getActivity(this)).replaceFragment(R.id.content_frame, 
						selectedArtistCategoryFragmentTab, FragmentUtil.getTag(selectedArtistCategoryFragmentTab), true);
				break;
				
			case MusicArtists:
				MusicArtistsFragmentTab musicArtistsFragmentTab = new MusicArtistsFragmentTab();
				
				((PopularArtistsActivityTab) FragmentUtil.getActivity(this)).replaceFragment(R.id.content_frame, 
						musicArtistsFragmentTab, FragmentUtil.getTag(musicArtistsFragmentTab), true);
				break;
				
			case ComedyArtists:
				args = new Bundle();
				args.putSerializable(BundleKeys.GENRE, Artist.Genre.Comedy);
				args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_comedy);
				
				selectedArtistCategoryFragmentTab = new SelectedArtistCategoryFragmentTab();
				selectedArtistCategoryFragmentTab.setArguments(args);
				
				((PopularArtistsActivityTab) FragmentUtil.getActivity(this)).replaceFragment(R.id.content_frame, 
						selectedArtistCategoryFragmentTab, FragmentUtil.getTag(selectedArtistCategoryFragmentTab), true);
				break;
				
			case TheaterArtists:
				args = new Bundle();
				args.putSerializable(BundleKeys.GENRE, Artist.Genre.Theater);
				args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_theater);

				selectedArtistCategoryFragmentTab = new SelectedArtistCategoryFragmentTab();
				selectedArtistCategoryFragmentTab.setArguments(args);
				
				((PopularArtistsActivityTab) FragmentUtil.getActivity(this)).replaceFragment(R.id.content_frame, 
						selectedArtistCategoryFragmentTab, FragmentUtil.getTag(selectedArtistCategoryFragmentTab), true);
				break;
				
			case SportsArtists:
				SportsArtistsFragmentTab sportsArtistsFragmentTab = new SportsArtistsFragmentTab();
				
				((PopularArtistsActivityTab) FragmentUtil.getActivity(this)).replaceFragment(R.id.content_frame, 
						sportsArtistsFragmentTab, FragmentUtil.getTag(sportsArtistsFragmentTab), true);
				break;

		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.btnSyncAccounts:
				((CheckBox) v).setChecked(false);
				Intent intent = new Intent(FragmentUtil.getApplication(this), ConnectAccountsActivityTab.class);
				startActivity(intent);
				FragmentUtil.getActivity(this).finish();
				break;

			case R.id.btnPopularArtists:
				((CheckBox) v).setChecked(true);
				break;
				
			case R.id.btnRecommended:
				((CheckBox) v).setChecked(false);
				intent = new Intent(FragmentUtil.getApplication(this), RecommendedArtistsActivityTab.class);
				startActivity(intent);
				FragmentUtil.getActivity(this).finish();
				break;

				
			case R.id.btnSearch:
				((CheckBox) v).setChecked(false);
				((BaseActivityTab) FragmentUtil.getActivity(this)).expandSearchView();
				break;
		}
	}

	@Override
	public void displayFullScrnProgress() {
		rltLytProgressBar.setVisibility(View.VISIBLE);
	}

	@Override
	public void loadItemsInBackground() {
		LoadPopularArtists loadPopularArtists = new LoadPopularArtists(Api.OAUTH_TOKEN,
				popularArtistCategories, latlon[0], latlon[1], popularArtistsAdapter, this);
		popularArtistsAdapter.setLoadArtists(loadPopularArtists);
		AsyncTaskUtil.executeAsyncTask(loadPopularArtists, true);
	}

	@Override
	public void onTaskCompleted(Void... params) {
		rltLytProgressBar.setVisibility(View.INVISIBLE);
	}
}
