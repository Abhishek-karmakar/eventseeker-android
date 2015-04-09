package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.List;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/*import com.scvngr.levelup.views.gallery.AdapterView;
import com.scvngr.levelup.views.gallery.AdapterView.OnItemClickListener;
import com.scvngr.levelup.views.gallery.Gallery;*/
import com.viewpagerindicator.CirclePageIndicator;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingItemType;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.asynctask.AsyncLoadImg.AsyncLoadImageListener;
import com.wcities.eventseeker.asynctask.UserTracker;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Artist.Attending;
import com.wcities.eventseeker.core.ArtistLink.LinkType;
import com.wcities.eventseeker.core.Friend;
import com.wcities.eventseeker.core.Video;
import com.wcities.eventseeker.interfaces.ReplaceFragmentListener;
import com.wcities.eventseeker.util.FragmentUtil;

public class ArtistInfoFragment extends Fragment implements OnClickListener,
		AsyncLoadImageListener {

	private static final String TAG = ArtistInfoFragment.class.getName();

	private static int MAX_FRIENDS_GRID = 3;
	private static final int MAX_LINES_ARTIST_DESC_PORTRAIT = 3;
	private static final int MAX_LINES_ARTIST_DESC_LANDSCAPE = 5;

	private Artist artist;
	private boolean isArtistDescExpanded, isFriendsGridExpanded;
	private int orientation;
	private boolean allDetailsLoaded, isImgLoaded;

	private Resources res;
	private ProgressBar progressBar, progressBar2;
	private RelativeLayout rltLayoutArtistDesc, rltLayoutLoadedContent;
	private View lnrVideos;
	private View rltLayoutVideos;
	private TextView txtArtistDesc;
	private ImageView imgDown, imgArtist, imgRight;
	private ViewPager viewPager;
	private CirclePageIndicator indicator;
	private RelativeLayout rltLayoutFriends;
	private TextView txtViewAll;
	private RelativeLayout fragmentArtistDetailsFooter;
	private ImageView imgFollow;
	private TextView txtFollow;

	private boolean isTablet;
	private boolean isTabletInPortraitMode;
	
	private List<Video> videos;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate()");
		artist = (Artist) getArguments().getSerializable(BundleKeys.ARTIST);

		res = getResources();

		isTablet = ((EventSeekr) FragmentUtil.getActivity(this).getApplicationContext()).isTablet();
		
		videos = new ArrayList<Video>();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Log.d(TAG, "onCreateView() - " + (System.currentTimeMillis() / 1000));
		orientation = res.getConfiguration().orientation;
		isTabletInPortraitMode = ((EventSeekr) FragmentUtil.getActivity(this).getApplicationContext())
				.isTabletAndInPortraitMode();
		// Log.d(TAG, "child manager = " + getChildFragmentManager());
		if(isTablet) {
			MAX_FRIENDS_GRID = ((EventSeekr) FragmentUtil.getActivity(this).getApplicationContext())
					.isTabletAndInLandscapeMode() ? 6 : 5;
		}
		
		View v = inflater.inflate(R.layout.fragment_artist_info, null);

		progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
		rltLayoutLoadedContent = (RelativeLayout) v.findViewById(R.id.rltLayoutLoadedContent);

		if (orientation == Configuration.ORIENTATION_PORTRAIT) {
			progressBar2 = (ProgressBar) v.findViewById(R.id.progressBar2);
		}

		imgArtist = (ImageView) v.findViewById(R.id.imgItem);
		updateArtistImg();

		((TextView) v.findViewById(R.id.txtItemTitle)).setText(artist.getName());

		lnrVideos = v.findViewById(R.id.lnrVideos);
		rltLayoutArtistDesc = (RelativeLayout) v.findViewById(R.id.rltLayoutDesc);
		txtArtistDesc = (TextView) v.findViewById(R.id.txtDesc);
		imgDown = (ImageView) v.findViewById(R.id.imgDown);

		updateDescVisibility();

		rltLayoutVideos = v.findViewById(R.id.rltLayoutVideos);

		if (isTabletInPortraitMode) {

			//glryVideo = (Gallery) v.findViewById(R.id.glryVideo);
			//glryVideo.setAdapter(videoGalleryAdapter);
			/*glryVideo.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					Video video = (Video) parent.getItemAtPosition(position);
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(video.getVideoUrl()));
					startActivity(Intent.createChooser(intent, ""));
					*//**
					 * 15-12-2014: added Google Analytics tracker code.
					 *//*
					GoogleAnalyticsTracker.getInstance().sendEvent(FragmentUtil.getApplication(ArtistInfoFragment.this), 
						FragmentUtil.getScreenName(ArtistInfoFragment.this), GoogleAnalyticsTracker.ARTIST_VIDEO_CLICK,
						GoogleAnalyticsTracker.Type.Artist.name(), video.getVideoUrl(), 
						artist.getId());
				}
			});*/

		} else {

			viewPager = (ViewPager) v.findViewById(R.id.viewPager);

			indicator = (CirclePageIndicator) v.findViewById(R.id.pageIndicator);
			indicator.setViewPager(viewPager);

		}

		updateVideosVisibility();

		//grdVFriends = (ExpandableGridView) v.findViewById(R.id.grdVFriends);
		/*if(isTablet) {
			grdVFriends.setNumColumns(MAX_FRIENDS_GRID);
		}
		grdVFriends.setAdapter(friendsGridAdapter);*/

		rltLayoutFriends = (RelativeLayout) v.findViewById(R.id.rltLayoutFriends);
		txtViewAll = (TextView) v.findViewById(R.id.txtViewAll);
		imgRight = (ImageView) v.findViewById(R.id.imgRight);
		updateFriendsVisibility();

		//imgFollow = (ImageView) v.findViewById(R.id.imgFollow);
		//txtFollow = (TextView) v.findViewById(R.id.txtFollow);
		fragmentArtistDetailsFooter = (RelativeLayout) v.findViewById(R.id.fragmentArtistDetailsFooter);
		fragmentArtistDetailsFooter.setOnClickListener(this);

		updateFollowingFooter();

		if (isTablet) {

			v.findViewById(R.id.imgFacebook).setOnClickListener(this);
			v.findViewById(R.id.imgTwitter).setOnClickListener(this);
			v.findViewById(R.id.imgWeb).setOnClickListener(this);

		}

		// Log.d(TAG, "onCreateView() done - " + (System.currentTimeMillis() /
		// 1000));
		Log.d(TAG, "oncreateview() completed...");
		return v;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		//Log.d(TAG, "onDestroy()");
	}

	private void updateArtistImg() {
		// Log.d(TAG, "updateEventImg(), url = " + event.getLowResImgUrl());
		if (artist.doesValidImgUrlExist() || allDetailsLoaded) {
			String key = artist.getKey(ImgResolution.LOW);
			BitmapCache bitmapCache = BitmapCache.getInstance();
			Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
			if (bitmap != null) {
				imgArtist.setImageBitmap(bitmap);
				isImgLoaded = true;

			} else {
				imgArtist.setImageBitmap(null);
				AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
				asyncLoadImg.loadImg(imgArtist, ImgResolution.LOW, artist, this);
				isImgLoaded = false;
			}
		}
		updateProgressBarVisibility();
	}

	private void updateFollowingFooter() {
		Log.d(TAG, "updateFollowingFooter()");
		if (artist.getAttending() == null || ((EventSeekr) FragmentUtil.getActivity(this)
						.getApplication()).getWcitiesId() == null) {
			fragmentArtistDetailsFooter.setVisibility(View.GONE);

		} else {
			fragmentArtistDetailsFooter.setVisibility(View.VISIBLE);

			switch (artist.getAttending()) {

			case Tracked:
				//imgFollow.setImageDrawable(res.getDrawable(R.drawable.following));
				txtFollow.setText("");
				break;

			case NotTracked:
				//imgFollow.setImageDrawable(res.getDrawable(R.drawable.follow));
				txtFollow.setText("");
				break;

			default:
				break;
			}
		}
	}

	private void updateFriendsVisibility() {
		//Log.d(TAG, "updateFriendsVisibility()");
		if (((EventSeekr) FragmentUtil.getActivity(this).getApplication()).getWcitiesId() == null) {
			rltLayoutFriends.setVisibility(View.GONE);

		} else if (isTablet) {

			/*int visibility = (artist.getFriends().isEmpty()) ? View.GONE : View.VISIBLE;
			rltLayoutFriends.setVisibility(visibility);*/
			
			if (artist.getFriends().isEmpty()) {
				rltLayoutFriends.setVisibility(View.GONE);

			} else {
				rltLayoutFriends.setVisibility(View.VISIBLE);

				if (artist.getFriends().size() > MAX_FRIENDS_GRID) {
					RelativeLayout rltLayoutViewAll = (RelativeLayout) rltLayoutFriends
							.findViewById(R.id.rltLayoutViewAll);
					rltLayoutViewAll.setVisibility(View.VISIBLE);
					rltLayoutViewAll.setOnClickListener(this);

					expandOrCollapseFriendsGrid();
				}
			}

		} else if (orientation == Configuration.ORIENTATION_PORTRAIT) {

			if (!isImgLoaded || !allDetailsLoaded || artist.getFriends().isEmpty()) {
				rltLayoutFriends.setVisibility(View.GONE);

			} else {
				rltLayoutFriends.setVisibility(View.VISIBLE);

				if (artist.getFriends().size() > MAX_FRIENDS_GRID) {
					RelativeLayout rltLayoutViewAll = (RelativeLayout) rltLayoutFriends
							.findViewById(R.id.rltLayoutViewAll);
					rltLayoutViewAll.setVisibility(View.VISIBLE);
					rltLayoutViewAll.setOnClickListener(this);

					expandOrCollapseFriendsGrid();
				}
			}

		} else {
			if (artist.getFriends().isEmpty()) {

				// Remove entire bottom part since neither description nor
				// friends are available
				if (artist.getDescription() == null) {
					rltLayoutFriends.setVisibility(View.GONE);
					rltLayoutArtistDesc.setVisibility(View.GONE);

				} else {
					/**
					 * Use INVISIBLE instead of GONE to restrain description
					 * section space to half the screen width; otherwise it will
					 * expand horizontally to fill screen width
					 */
					rltLayoutFriends.setVisibility(View.INVISIBLE);
					rltLayoutArtistDesc.setVisibility(View.VISIBLE);
				}

			} else {
				rltLayoutFriends.setVisibility(View.VISIBLE);
			}
		}
	}

	private void expandOrCollapseFriendsGrid() {
		//Log.d(TAG, "expandOrCollapseFriendsGrid()");
		if (isFriendsGridExpanded) {
			txtViewAll.setText(res.getString(R.string.txt_view_less));
			imgRight.setImageResource(R.drawable.less);
			//grdVFriends.setExpanded(isFriendsGridExpanded);

		} else {
			txtViewAll.setText(res.getString(R.string.txt_view_all));
			imgRight.setImageResource(R.drawable.down);
			//grdVFriends.setExpanded(isFriendsGridExpanded);
		}
	}

	private void updateVideosVisibility() {
		//Log.d(TAG, "updateVideosVisibility()");
		if (isTablet) {

			int visibility;
			if(artist.getVideos().isEmpty()) {
				visibility = (orientation == Configuration.ORIENTATION_PORTRAIT) ? View.GONE : View.INVISIBLE;
			} else {
				visibility = View.VISIBLE;
			}
			rltLayoutVideos.setVisibility(visibility);
			if(visibility == View.VISIBLE) {
				lnrVideos.setVisibility(View.VISIBLE);
			}
		} else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {

			int visibility = artist.getVideos().isEmpty() ? View.GONE : View.VISIBLE;
			viewPager.setVisibility(visibility);
			indicator.setVisibility(visibility);

		} else {
			if (!isImgLoaded || !allDetailsLoaded || artist.getVideos().isEmpty()) {
				rltLayoutVideos.setVisibility(View.GONE);

			} else {
				rltLayoutVideos.setVisibility(View.VISIBLE);
			}
		}
	}

	private void makeDescVisible() {
		//Log.d(TAG, "updateVideosVisibility()");

		if(isTablet) {
			lnrVideos.setVisibility(View.VISIBLE);
		}
		rltLayoutArtistDesc.setVisibility(View.VISIBLE);
		imgDown.setOnClickListener(this);

		txtArtistDesc.setText(Html.fromHtml(artist.getDescription()));

		if (isArtistDescExpanded) {
			expandEvtDesc();

		} else {
			collapseEvtDesc();
		}
	}

	private void makeDescVisibleInExpandedMode() {
		//Log.d(TAG, "makeDescVisibleInExpandedMode()");

		imgDown.setVisibility(View.GONE);
		if(isTablet) {
			lnrVideos.setVisibility(View.VISIBLE);
		}
		rltLayoutArtistDesc.setVisibility(View.VISIBLE);
		txtArtistDesc.setText(Html.fromHtml(artist.getDescription()));
		expandEvtDesc();
	}

	private void updateDescVisibility() {
		//Log.d(TAG, "updateDescVisibility");
		/**
		 * Mithil:added new condition for the Tablet UI. Here if device is
		 * tablet then if artist description is null then make the
		 * rltLayoutArtistDesc invisible else expand the Description and make
		 * down arrow's visibility as gone.
		 */
		if (isTablet) {

			if (artist.getDescription() == null) {
				int visibility = (orientation == Configuration.ORIENTATION_PORTRAIT) ? View.GONE : View.INVISIBLE;
				rltLayoutArtistDesc.setVisibility(visibility);

			} else {
				makeDescVisibleInExpandedMode();
			}
		} else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
			if (!isImgLoaded || !allDetailsLoaded || artist.getDescription() == null) {
				rltLayoutArtistDesc.setVisibility(View.GONE);

			} else {
				makeDescVisible();
			}

		} else {
			if (artist.getDescription() == null) {
				rltLayoutArtistDesc.setVisibility(View.INVISIBLE);

			} else {
				makeDescVisible();
			}
		}
	}

	private void expandEvtDesc() {
		//Log.d(TAG, "expandEvtDesc");
		txtArtistDesc.setMaxLines(Integer.MAX_VALUE);
		txtArtistDesc.setEllipsize(null);

		imgDown.setImageDrawable(res.getDrawable(R.drawable.less));

		isArtistDescExpanded = true;
	}

	private void collapseEvtDesc() {
		//Log.d(TAG, "collapseEvtDesc");
		int maxLines = orientation == Configuration.ORIENTATION_PORTRAIT ? MAX_LINES_ARTIST_DESC_PORTRAIT
				: MAX_LINES_ARTIST_DESC_LANDSCAPE;
		txtArtistDesc.setMaxLines(maxLines);
		txtArtistDesc.setEllipsize(TruncateAt.END);

		imgDown.setImageDrawable(res.getDrawable(R.drawable.down));

		isArtistDescExpanded = false;
	}

	private void updateProgressBarVisibility() {
		//Log.d(TAG, "updateProgressBarVisibility()");
		if (isImgLoaded) {
			//Log.d(TAG, "updateProgressBarVisibility() isImgLoaded");
			rltLayoutLoadedContent.setVisibility(View.VISIBLE);
			progressBar.setVisibility(View.GONE);
			if (progressBar2 != null) {
				if (allDetailsLoaded) {
					//Log.d(TAG, "updateProgressBarVisibility() allDetailsLoaded");
					progressBar2.setVisibility(View.GONE);

				} else {
					//Log.d(TAG, "updateProgressBarVisibility() !allDetailsLoaded");
					progressBar2.setVisibility(View.VISIBLE);
				}
			}

		} else {
			//Log.d(TAG, "updateProgressBarVisibility() !isImgLoaded");
			rltLayoutLoadedContent.setVisibility(View.GONE);
			progressBar.setVisibility(View.VISIBLE);
		}
	}


	@Override
	public void onClick(View v) {
		switch (v.getId()) {

		case R.id.imgDown:
			if (isArtistDescExpanded) {
				collapseEvtDesc();

			} else {
				expandEvtDesc();
			}
			break;

		case R.id.fragmentArtistDetailsFooter:
			EventSeekr eventSeekr = (EventSeekr) FragmentUtil.getActivity(this).getApplication();
			if (/*txtFollow.getText().equals(FooterTxt.Follow.getStringForm(this))*/
					artist.getAttending() == Attending.NotTracked) {
				artist.updateAttending(Attending.Tracked, eventSeekr);
				updateFollowingFooter();
				new UserTracker(Api.OAUTH_TOKEN, eventSeekr, UserTrackingItemType.artist, artist.getId()).execute();

			} else {
				artist.updateAttending(Attending.NotTracked, eventSeekr);
				updateFollowingFooter();
				new UserTracker(Api.OAUTH_TOKEN, eventSeekr, UserTrackingItemType.artist, artist.getId(), 
						Attending.NotTracked.getValue(), UserTrackingType.Edit).execute();
			}
			//((ArtistDetailsFragment1) getParentFragment()).onArtistFollowingUpdated();
			break;

		case R.id.rltLayoutViewAll:
			isFriendsGridExpanded = !isFriendsGridExpanded;
			expandOrCollapseFriendsGrid();
			break;

		case R.id.imgFacebook:
			openURL(artist.getArtistLinkByType(LinkType.FACEBOOK), R.string.facebook_link_unavailable);
			break;

		case R.id.imgTwitter:
			openURL(artist.getArtistLinkByType(LinkType.TWITTER), R.string.twitter_link_unavailable);
			break;

		case R.id.imgWeb:
			openURL(artist.getArtistLinkByType(LinkType.WEBSITE), R.string.web_link_unavailable);
			break;

		default:
			break;
		}
	}

	private void updateScreen() {
		Log.d(TAG, "updateScreen()");
		// if fragment is destroyed (user has pressed back)
		if (FragmentUtil.getActivity(this) == null) {
			return;
		}

		updateArtistImg();

		updateDescVisibility();

		updateVideosVisibility();

		updateFriendsVisibility();
		updateFollowingFooter();
	}

	@Override
	public void onImageLoaded() {
		Log.d(TAG, "onImageLoaded()");
		isImgLoaded = true;
		updateScreen();
	}

	@Override
	public void onImageCouldNotBeLoaded() {
		isImgLoaded = true;
		updateScreen();
	}
	
	private void openURL(String url, int errorStr) {
		if(url != null) {
			Bundle args = new Bundle();
			args.putString(BundleKeys.URL, url);
			((ReplaceFragmentListener) FragmentUtil.getActivity(this))
					.replaceByFragment(AppConstants.FRAGMENT_TAG_WEB_VIEW, args);
			
		} else {
			Toast.makeText(FragmentUtil.getActivity(this), errorStr, Toast.LENGTH_SHORT).show();
		}
	}

		
}
