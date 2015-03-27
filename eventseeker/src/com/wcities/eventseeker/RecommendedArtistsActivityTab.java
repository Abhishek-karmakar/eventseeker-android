package com.wcities.eventseeker;

import java.util.List;

import android.os.Bundle;

import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.interfaces.ArtistListener;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.SharedElement;

public class RecommendedArtistsActivityTab extends BaseActivityTab implements ArtistListener {
	
	private static final String TAG = RecommendedArtistsActivityTab.class.getSimpleName();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Log.d(TAG, "onCreate()");
		setContentView(R.layout.activity_base_tab);
		
		setCommonUI();
		
		if (isOnCreateCalledFirstTime) {
			RecommendedArtistsFragmentTab recommendedArtistsFragmentTab = new RecommendedArtistsFragmentTab();
			addFragment(R.id.content_frame, recommendedArtistsFragmentTab, FragmentUtil.getTag(recommendedArtistsFragmentTab), false);
		}
		
		if (savedInstanceState != null) {
			currentContentFragmentTag = savedInstanceState.getString(BundleKeys.CURRENT_CONTENT_FRAGMENT_TAG);
		}
	}

	@Override
	public String getScreenName() {
		return ScreenNames.RECOMMENDED_ARTISTS_SCREEN;
	}

	@Override
	protected String getScrnTitle() {
		return getResources().getString(R.string.title_recommended);
	}
	
	@Override
	protected int getDrawerItemPos() {
		return AppConstants.INVALID_INDEX;
	}

	@Override
	public void onArtistSelected(Artist artist) {
		/*ArtistDetailsFragment artistDetailsFragment = new ArtistDetailsFragment();
		Bundle args = new Bundle();
		args.putSerializable(BundleKeys.ARTIST, artist);
		artistDetailsFragment.setArguments(args);
		selectNonDrawerItem(artistDetailsFragment,
				AppConstants.FRAGMENT_TAG_ARTIST_DETAILS, "", true);*/
	}

	@Override
	public void onArtistSelected(Artist artist, List<SharedElement> sharedElements) {}
}
