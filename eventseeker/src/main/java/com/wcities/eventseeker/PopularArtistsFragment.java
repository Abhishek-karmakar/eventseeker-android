package com.wcities.eventseeker;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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


public class PopularArtistsFragment extends FragmentLoadableFromBackStack implements FullScrnProgressListener,
		LoadItemsInBackgroundListener, AsyncTaskListener<Void>, OnPopularArtistsCategoryClickListener {

	private static final String TAG = PopularArtistsFragment.class.getSimpleName();

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
		View view = inflater.inflate(R.layout.fragment_popular_artists, null);

		rvPopularArtists = (RecyclerView) view.findViewById(R.id.rvPopularArtists);
		// use a linear layout manager
		RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(FragmentUtil.getActivity(this));
		rvPopularArtists.setLayoutManager(layoutManager);

		rltLytProgressBar = (RelativeLayout) view.findViewById(R.id.rltLytProgressBar);
		rltLytProgressBar.setBackgroundResource(R.drawable.bg_no_content_overlay);

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
	public String getScreenName() {
		return ScreenNames.POPULAR_ARTISTS_SCREEN;
	}

	public void onPopularArtistsCategoryClick(PopularArtistCategory popularArtistCategory) {
		Bundle args;
		switch (popularArtistCategory.getPopularArtistsType()) {
			case FeaturedListArtists:
				FeaturedListArtistCategory featuredListArtistCategory = ((FeaturedListArtistCategory) popularArtistCategory);
				args = new Bundle();
				args.putSerializable(BundleKeys.FEATURED_LIST_ARTISTS_ID, featuredListArtistCategory.getId());
				args.putString(BundleKeys.SCREEN_TITLE, featuredListArtistCategory.getName());
				((MainActivity) FragmentUtil.getActivity(this))
					.replaceByFragment(AppConstants.FRAGMENT_TAG_SELECTED_FEATURED_LIST_ARTISTS_FRAGMENT, args);
				break;

			case FeaturedArtists:
				args = new Bundle();
				args.putSerializable(BundleKeys.GENRE, Artist.Genre.Featured);
				args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_featured_list);
				((MainActivity) FragmentUtil.getActivity(this))
						.replaceByFragment(AppConstants.FRAGMENT_TAG_SELECTED_ARTIST_CATEGORY_FRAGMENT, args);
				break;

			case MusicArtists:
				((MainActivity) FragmentUtil.getActivity(this))
						.replaceByFragment(AppConstants.FRAGMENT_TAG_MUSIC_ARTISTS, null);
				break;

			case ComedyArtists:
				args = new Bundle();
				args.putSerializable(BundleKeys.GENRE, Artist.Genre.Comedy);
				args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_comedy);
				((MainActivity) FragmentUtil.getActivity(this))
						.replaceByFragment(AppConstants.FRAGMENT_TAG_SELECTED_ARTIST_CATEGORY_FRAGMENT, args);
				break;

			case TheaterArtists:
				args = new Bundle();
				args.putSerializable(BundleKeys.GENRE, Artist.Genre.Theater);
				args.putInt(BundleKeys.SCREEN_TITLE, R.string.title_theater);
				((MainActivity) FragmentUtil.getActivity(this))
						.replaceByFragment(AppConstants.FRAGMENT_TAG_SELECTED_ARTIST_CATEGORY_FRAGMENT, args);
				break;

			case SportsArtists:
				((MainActivity) FragmentUtil.getActivity(this))
						.replaceByFragment(AppConstants.FRAGMENT_TAG_SPORTS_ARTISTS, null);
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
