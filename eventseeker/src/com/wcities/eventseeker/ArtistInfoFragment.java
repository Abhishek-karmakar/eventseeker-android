package com.wcities.eventseeker;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
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

import com.viewpagerindicator.CirclePageIndicator;
import com.wcities.eventseeker.ArtistDetailsFragment.ArtistDetailsFragmentListener;
import com.wcities.eventseeker.ArtistDetailsFragment.FooterTxt;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingItemType;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.asynctask.AsyncLoadImg.AsyncLoadImageListener;
import com.wcities.eventseeker.asynctask.UserTracker;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Artist.Attending;
import com.wcities.eventseeker.core.Friend;
import com.wcities.eventseeker.custom.view.ExpandableGridView;
import com.wcities.eventseeker.util.FragmentUtil;

public class ArtistInfoFragment extends Fragment implements OnClickListener, ArtistDetailsFragmentListener, 
		AsyncLoadImageListener {

	private static final String TAG = ArtistInfoFragment.class.getName();
	
	private static final int MAX_FRIENDS_GRID = 3;
	private static final int MAX_LINES_ARTIST_DESC_PORTRAIT = 3;
	private static final int MAX_LINES_ARTIST_DESC_LANDSCAPE = 5;

	private Artist artist;
	private boolean isArtistDescExpanded, isFriendsGridExpanded;
	private int orientation;
	private boolean allDetailsLoaded, isImgLoaded;
	
	private Resources res;
	private ProgressBar progressBar, progressBar2;
	private RelativeLayout rltLayoutArtistDesc, rltLayoutLoadedContent;
	private View rltLayoutVideos;
	private TextView txtArtistDesc;
	private ImageView imgDown, imgArtist, imgRight;
	private ViewPager viewPager;
	private CirclePageIndicator indicator;
	private RelativeLayout rltLayoutFriends;
	private ExpandableGridView grdVFriends;
	private TextView txtViewAll;
	private RelativeLayout fragmentArtistDetailsFooter;
	private ImageView imgFollow;
	private TextView txtFollow;
	
	private VideoFragmentPagerAdapter videoFragmentPagerAdapter;
	private FriendsGridAdapter friendsGridAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		artist = (Artist) getArguments().getSerializable(BundleKeys.ARTIST);
		
		res = getResources();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		//Log.d(TAG, "onCreateView() - " + (System.currentTimeMillis() / 1000));
		orientation = res.getConfiguration().orientation;
		//Log.d(TAG, "child manager = " + getChildFragmentManager());
		videoFragmentPagerAdapter = new VideoFragmentPagerAdapter(artist, getChildFragmentManager());  
		
		View v = inflater.inflate(R.layout.fragment_artist_info, null);
		
		progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
		rltLayoutLoadedContent = (RelativeLayout) v.findViewById(R.id.rltLayoutLoadedContent);
		
		if (orientation == Configuration.ORIENTATION_PORTRAIT) {
			progressBar2 = (ProgressBar) v.findViewById(R.id.progressBar2);
		}
		
		imgArtist = (ImageView) v.findViewById(R.id.imgItem);
		updateArtistImg();
		
		((TextView)v.findViewById(R.id.txtItemTitle)).setText(artist.getName());
		
		rltLayoutArtistDesc = (RelativeLayout) v.findViewById(R.id.rltLayoutDesc);
		txtArtistDesc = (TextView) v.findViewById(R.id.txtDesc);
		imgDown = (ImageView) v.findViewById(R.id.imgDown);
		
		updateDescVisibility();
		
		rltLayoutVideos = v.findViewById(R.id.rltLayoutVideos);
		viewPager = (ViewPager) v.findViewById(R.id.viewPager);
		viewPager.setAdapter(videoFragmentPagerAdapter);
		
		indicator = (CirclePageIndicator)v.findViewById(R.id.pageIndicator);
    	indicator.setViewPager(viewPager);
    	
    	updateVideosVisibility();
		
		grdVFriends = (ExpandableGridView) v.findViewById(R.id.grdVFriends);
		if (friendsGridAdapter == null) {
			friendsGridAdapter = new FriendsGridAdapter();
		}
		grdVFriends.setAdapter(friendsGridAdapter);
		
		rltLayoutFriends = (RelativeLayout) v.findViewById(R.id.rltLayoutFriends);
		txtViewAll = (TextView) v.findViewById(R.id.txtViewAll);
		imgRight = (ImageView) v.findViewById(R.id.imgRight);
		updateFriendsVisibility();
		
		imgFollow = (ImageView) v.findViewById(R.id.imgFollow);
		txtFollow = (TextView) v.findViewById(R.id.txtFollow);
		fragmentArtistDetailsFooter = (RelativeLayout) v.findViewById(R.id.fragmentArtistDetailsFooter);
		fragmentArtistDetailsFooter.setOnClickListener(this);
		
		updateFollowingFooter();
		
		//Log.d(TAG, "onCreateView() done - " + (System.currentTimeMillis() / 1000));
		return v;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy()");
	}
	
	private void updateArtistImg() {
		//Log.d(TAG, "updateEventImg(), url = " + event.getLowResImgUrl());
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
		if (artist.getAttending() == null || ((EventSeekr)FragmentUtil.getActivity(this).getApplication()).getWcitiesId() == null) {
			fragmentArtistDetailsFooter.setVisibility(View.GONE);
			
		} else {
			fragmentArtistDetailsFooter.setVisibility(View.VISIBLE);
			
			switch (artist.getAttending()) {

			case Tracked:
				imgFollow.setImageDrawable(res.getDrawable(R.drawable.following));
				txtFollow.setText(FooterTxt.Following.name());
				break;

			case NotTracked:
				imgFollow.setImageDrawable(res.getDrawable(R.drawable.follow));
				txtFollow.setText(FooterTxt.Follow.name());
				break;

			default:
				break;
			}
		}
	}
	
	private void updateFriendsVisibility() {
		if (((EventSeekr)FragmentUtil.getActivity(this).getApplication()).getWcitiesId() == null) {
			rltLayoutFriends.setVisibility(View.GONE);
			
		} else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
			
			if (!isImgLoaded || !allDetailsLoaded || artist.getFriends().isEmpty()) {
				rltLayoutFriends.setVisibility(View.GONE);
				
			} else {
				rltLayoutFriends.setVisibility(View.VISIBLE);
				
				if (artist.getFriends().size() > MAX_FRIENDS_GRID) {
					RelativeLayout rltLayoutViewAll = (RelativeLayout) rltLayoutFriends.findViewById(R.id.rltLayoutViewAll);
					rltLayoutViewAll.setVisibility(View.VISIBLE);
					rltLayoutViewAll.setOnClickListener(this);
					
					expandOrCollapseFriendsGrid();
				}
			}
			
		} else {
			if (artist.getFriends().isEmpty()) {
				
				// Remove entire bottom part since neither description nor friends are available
				if (artist.getDescription() == null) {
					rltLayoutFriends.setVisibility(View.GONE);
					rltLayoutArtistDesc.setVisibility(View.GONE);
					
				} else {
					/**
					 * Use INVISIBLE instead of GONE to restrain description section space to half the 
					 * screen width; otherwise it will expand horizontally to fill screen width
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
		if (isFriendsGridExpanded) {
			txtViewAll.setText(res.getString(R.string.txt_view_less));
			imgRight.setImageResource(R.drawable.less);
			grdVFriends.setExpanded(isFriendsGridExpanded);
			
		} else {
			txtViewAll.setText(res.getString(R.string.txt_view_all));
			imgRight.setImageResource(R.drawable.down);
			grdVFriends.setExpanded(isFriendsGridExpanded);
		}
		
		friendsGridAdapter.notifyDataSetChanged();
	}
	
	private void updateVideosVisibility() {
		if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
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
		rltLayoutArtistDesc.setVisibility(View.VISIBLE);
		imgDown.setOnClickListener(this);

		txtArtistDesc.setText(artist.getDescription());

		if (isArtistDescExpanded) {
			expandEvtDesc();
			
		} else {
			collapseEvtDesc();
		}
	}
	
	private void updateDescVisibility() {
		if (orientation == Configuration.ORIENTATION_PORTRAIT) {
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
		txtArtistDesc.setMaxLines(Integer.MAX_VALUE);
		txtArtistDesc.setEllipsize(null);
		
		imgDown.setImageDrawable(res.getDrawable(R.drawable.less));

		isArtistDescExpanded = true;
	}
	
	private void collapseEvtDesc() {
		int maxLines = orientation == Configuration.ORIENTATION_PORTRAIT ? MAX_LINES_ARTIST_DESC_PORTRAIT : 
			MAX_LINES_ARTIST_DESC_LANDSCAPE;
		txtArtistDesc.setMaxLines(maxLines);
		txtArtistDesc.setEllipsize(TruncateAt.END);
		
		imgDown.setImageDrawable(res.getDrawable(R.drawable.down));
		
		isArtistDescExpanded = false;
	}
	
	private void updateProgressBarVisibility() {
		Log.d(TAG, "updateProgressBarVisibility()");
		if (isImgLoaded) {
			Log.d(TAG, "updateProgressBarVisibility() isImgLoaded");
			rltLayoutLoadedContent.setVisibility(View.VISIBLE);
			progressBar.setVisibility(View.GONE);
			if (progressBar2 != null) {
				if (allDetailsLoaded) {
					Log.d(TAG, "updateProgressBarVisibility() allDetailsLoaded");
					progressBar2.setVisibility(View.GONE);
					
				} else {
					Log.d(TAG, "updateProgressBarVisibility() !allDetailsLoaded");
					progressBar2.setVisibility(View.VISIBLE);
				}
			}
			
		} else {
			Log.d(TAG, "updateProgressBarVisibility() !isImgLoaded");
			rltLayoutLoadedContent.setVisibility(View.GONE);
			progressBar.setVisibility(View.VISIBLE);
		}
	}
	
	private static class VideoFragmentPagerAdapter extends FragmentStatePagerAdapter {
    	
		private static final int MAX_LIMIT = 5;
		
		private Artist artist;
    	   
        public VideoFragmentPagerAdapter(Artist artist, FragmentManager fm) {  
            super(fm);
            //Log.d(TAG, "VideoFragmentPagerAdapter()");
            this.artist = artist;
        }  
        
        @Override  
        public Fragment getItem(int index) { 
        	//Log.d(TAG, "getItem() for index = " + index);
        	VideoFragment videoFragment = VideoFragment.newInstance(artist.getVideos().get(index));
            return videoFragment;
        }  

        @Override  
        public int getCount() {  
        	//Log.d(TAG, "getCount()");
            return artist.getVideos().size() > MAX_LIMIT ? MAX_LIMIT : artist.getVideos().size();  
        }  
        
        @Override
        public int getItemPosition(Object object) {
        	//Log.d(TAG, "getItemPosition()");
        	return POSITION_NONE;
        }
    }
	
	private class FriendsGridAdapter extends BaseAdapter {
		
		private BitmapCache bitmapCache;
		
		public FriendsGridAdapter() {
			this.bitmapCache = BitmapCache.getInstance();
		}

		@Override
		public int getCount() {
			if (orientation == Configuration.ORIENTATION_PORTRAIT) {
				return artist.getFriends().size() > MAX_FRIENDS_GRID ? 
						(isFriendsGridExpanded ? artist.getFriends().size() : MAX_FRIENDS_GRID) : artist.getFriends().size();
				
			} else {
				return artist.getFriends().size() > MAX_FRIENDS_GRID ? MAX_FRIENDS_GRID : artist.getFriends().size();
			}
		}

		@Override
		public Friend getItem(int position) {
			return artist.getFriends().get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			Friend friend = getItem(position);
			//Log.d(TAG, "friend = " + friend);
			
			if (friend == null) {
				convertView = LayoutInflater.from(FragmentUtil.getActivity(ArtistInfoFragment.this)).inflate(R.layout.list_progress_bar, null);
				
			} else {
				GridFriendHolder holder;

				if (convertView == null || convertView.getTag() == null) {
					convertView = LayoutInflater.from(FragmentUtil.getActivity(ArtistInfoFragment.this)).inflate(R.layout.grid_friend, null);
					
					holder = new GridFriendHolder();
					holder.imgFriend = (ImageView) convertView.findViewById(R.id.imgFriend);
					holder.txtFriendName = (TextView) convertView.findViewById(R.id.txtFriendName);
					
					convertView.setTag(holder);
					
				} else {
					holder = (GridFriendHolder) convertView.getTag();
				}
				
				holder.txtFriendName.setText(friend.getName());
				
				String key = friend.getKey(ImgResolution.LOW);
				Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
				if (bitmap != null) {
					holder.imgFriend.setImageBitmap(bitmap);
			        
			    } else {
			    	holder.imgFriend.setImageBitmap(null);
			        AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
			        asyncLoadImg.loadImg(holder.imgFriend, ImgResolution.LOW, friend);
			    }
			}
			return convertView;
		}
		
		private class GridFriendHolder {
			private TextView txtFriendName;
			private ImageView imgFriend;
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
			
		case R.id.rltLayoutViewAll:
			isFriendsGridExpanded = !isFriendsGridExpanded;
			expandOrCollapseFriendsGrid();
			break;
			
		default:
			break;
		}
	}
	
	private void updateScreen() {
		// if fragment is destroyed (user has pressed back)
		if (FragmentUtil.getActivity(this) == null) {
			return;
		}
		
		updateArtistImg();
		
		updateDescVisibility();
		
		updateVideosVisibility();
		if (videoFragmentPagerAdapter != null) {
			videoFragmentPagerAdapter.notifyDataSetChanged();
		}
		
		updateFriendsVisibility();
		friendsGridAdapter.notifyDataSetChanged();
		
		updateFollowingFooter();
	}

	@Override
	public void onArtistUpdatedByArtistDetailsFragment() {
		Log.d(TAG, "onArtistUpdatedByArtistDetailsFragment()");
		allDetailsLoaded = true;
		
		updateScreen();
		Log.d(TAG, "onArtistUpdatedByArtistDetailsFragment() done");
	}

	@Override
	public void onArtistFollowingUpdated() {
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
}
