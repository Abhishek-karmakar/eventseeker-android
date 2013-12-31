package com.wcities.eventseeker;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wcities.eventseeker.ArtistDetailsFragment.ArtistDetailsFragmentListener;
import com.wcities.eventseeker.ArtistDetailsFragment.FooterTxt;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingItemType;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.UserTracker;
import com.wcities.eventseeker.asynctask.LoadArtistNews.ArtistNewsListItem;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.ArtistNewsItem;
import com.wcities.eventseeker.core.Artist.Attending;
import com.wcities.eventseeker.util.FragmentUtil;

public class ArtistNewsFragment extends Fragment implements ArtistDetailsFragmentListener, OnClickListener {

	private static final String TAG = ArtistNewsFragment.class.getName();

	private static final String FRAGMENT_TAG_ARTIST_NEWS_LIST = "artistNewsList";
	
	private Artist artist;
	private RelativeLayout fragmentArtistDetailsFooter;
	private ImageView imgFollow;
	private TextView txtFollow;

	private View rltDummyLyt;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		artist = (Artist) getArguments().getSerializable(BundleKeys.ARTIST);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_artist_news, null);
		ArtistNewsListFragment fragment = (ArtistNewsListFragment) getChildFragmentManager().findFragmentByTag(
				FRAGMENT_TAG_ARTIST_NEWS_LIST);
        if (fragment == null) {
        	addArtistNewsListFragment();
        }
        
        imgFollow = (ImageView) v.findViewById(R.id.imgFollow);
		txtFollow = (TextView) v.findViewById(R.id.txtFollow);
		fragmentArtistDetailsFooter = (RelativeLayout) v.findViewById(R.id.fragmentArtistDetailsFooter);
		fragmentArtistDetailsFooter.setOnClickListener(this);
		
		updateFollowingFooter();
		
		rltDummyLyt = v.findViewById(R.id.rltDummyLyt);
		
		return v;
	}
	
	private void addArtistNewsListFragment() {
    	FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        
        ArtistNewsListFragment fragment = new ArtistNewsListFragment();
        fragment.setArguments(getArguments());
        fragmentTransaction.add(R.id.rltLayoutRoot, fragment, FRAGMENT_TAG_ARTIST_NEWS_LIST);
        fragmentTransaction.commit();
    } 
	
	private void updateFollowingFooter() {
		if (artist.getAttending() == null || ((EventSeekr)FragmentUtil.getActivity(this).getApplication()).getWcitiesId() == null) {
			fragmentArtistDetailsFooter.setVisibility(View.GONE);
		} else if(((MainActivity)FragmentUtil.getActivity(this)).isTablet()) {
			fragmentArtistDetailsFooter.setVisibility(View.GONE);		
		} else {
		
			fragmentArtistDetailsFooter.setVisibility(View.VISIBLE);
			
			switch (artist.getAttending()) {

			case Tracked:
				imgFollow.setImageDrawable(getResources().getDrawable(R.drawable.following));
				txtFollow.setText(FooterTxt.Following.name());
				break;

			case NotTracked:
				imgFollow.setImageDrawable(getResources().getDrawable(R.drawable.follow));
				txtFollow.setText(FooterTxt.Follow.name());
				break;

			default:
				break;
			}
		}
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.fragmentArtistDetailsFooter:
			if (txtFollow.getText().equals(FooterTxt.Follow.name())) {
				artist.setAttending(Attending.Tracked);
				updateFollowingFooter();
				new UserTracker((EventSeekr)FragmentUtil.getActivity(this).getApplication(), UserTrackingItemType.artist, artist.getId()).execute();
				
			} else {
				artist.setAttending(Attending.NotTracked);
				updateFollowingFooter();
				new UserTracker((EventSeekr)FragmentUtil.getActivity(this).getApplication(), UserTrackingItemType.artist, artist.getId(), 
						Attending.NotTracked.getValue(), UserTrackingType.Edit).execute();
			}
			((ArtistDetailsFragment) getParentFragment()).onArtistFollowingUpdated();
			break;
			
		default:
			break;
		}
	}

	@Override
	public void onArtistUpdatedByArtistDetailsFragment() {
		updateFollowingFooter();
	}

	@Override
	public void onArtistFollowingUpdated() {
		updateFollowingFooter();
	}
	
	
	public void changeRltDummyLytVisibility(int visibility) {
		rltDummyLyt.setVisibility(visibility);
	}
}
