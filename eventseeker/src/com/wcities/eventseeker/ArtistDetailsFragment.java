package com.wcities.eventseeker;

import java.util.List;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils.TruncateAt;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.Animator.AnimatorListener;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.view.ViewHelper;
import com.wcities.eventseeker.adapter.FriendsRVAdapter;
import com.wcities.eventseeker.adapter.VideoPagerAdapter;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.asynctask.LoadArtistDetails;
import com.wcities.eventseeker.asynctask.LoadArtistDetails.OnArtistUpdatedListener;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.CustomSharedElementTransitionDestination;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.VersionUtil;
import com.wcities.eventseeker.util.ViewUtil;
import com.wcities.eventseeker.viewdata.SharedElement;
import com.wcities.eventseeker.viewdata.SharedElementPosition;

public class ArtistDetailsFragment extends FragmentLoadableFromBackStack implements  
		DrawerListener, CustomSharedElementTransitionDestination, OnArtistUpdatedListener, OnClickListener {
	
	private static final String TAG = ArtistDetailsFragment.class.getName();
	
	private static final int UNSCROLLED = -1;
	private static final int TRANSITION_ANIM_DURATION = 400;

	private View rootView;
	private ImageView imgArtist;
	private TextView txtArtistTitle;
	private RecyclerView recyclerVArtists;
	private RelativeLayout rltLytTxtArtistTitle;
	
	private ArtistRVAdapter artistRVAdapter;
	
	private int totalScrolledDy = UNSCROLLED; // indicates layout not yet created
	private int limitScrollAt, actionBarElevation;
	private float minTitleScale;
	private boolean isScrollLimitReached, isDrawerOpen;
	private String title = "";
	
	private Artist artist;
	
	private List<SharedElement> sharedElements;
	private boolean isOnCreateViewCalledFirstTime = true;
	private int screenW, imgArtistHt;
	private AnimatorSet animatorSet;
	
	private LoadArtistDetails loadArtistDetails;
	private boolean allDetailsLoaded;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setHasOptionsMenu(true);
		setRetainInstance(true);
		
		actionBarElevation = FragmentUtil.getResources(this).getDimensionPixelSize(R.dimen.action_bar_elevation);
		
		Bundle args = getArguments();
		if (args.containsKey(BundleKeys.SHARED_ELEMENTS)) {
			sharedElements = (List<SharedElement>) args.getSerializable(BundleKeys.SHARED_ELEMENTS);
		}
		if (artist == null) {
			//Log.d(TAG, "event = null");
			artist = (Artist) args.getSerializable(BundleKeys.ARTIST);
			artist.getVideos().clear();
			artist.getFriends().clear();
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		/**
		 * on orientation change we need to recalculate this due to different values of 
		 * action_bar_ht on both orientations
		 */
		calculateScrollLimit();
		calculateDimensions();
		
		rootView = inflater.inflate(R.layout.fragment_artist_details, container, false);
		
		imgArtist = (ImageView) rootView.findViewById(R.id.imgArtist);
		updateArtistImg();
		
		rltLytTxtArtistTitle = (RelativeLayout) rootView.findViewById(R.id.rltLytTxtArtistTitle);
		
		txtArtistTitle = (TextView) rootView.findViewById(R.id.txtArtistTitle);
		txtArtistTitle.setText(artist.getName());
		// for marquee to work
		txtArtistTitle.setSelected(true);
		
		recyclerVArtists = (RecyclerView) rootView.findViewById(R.id.recyclerVArtists);
		// use a linear layout manager
		RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(FragmentUtil.getActivity(this));
		recyclerVArtists.setLayoutManager(layoutManager);
		
		recyclerVArtists.setOnScrollListener(new RecyclerView.OnScrollListener() {
	    	
	    	@Override
	    	public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
	    		super.onScrolled(recyclerView, dx, dy);
	    		//Log.d(TAG, "onScrolled - dx = " + dx + ", dy = " + dy);
	    		ArtistDetailsFragment.this.onScrolled(dy, false);
	    	}
		});
		
		recyclerVArtists.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
            	//Log.d(TAG, "onGlobalLayout()");
				if (VersionUtil.isApiLevelAbove15()) {
					recyclerVArtists.getViewTreeObserver().removeOnGlobalLayoutListener(this);

				} else {
					recyclerVArtists.getViewTreeObserver().removeGlobalOnLayoutListener(this);
				}
				
				onScrolled(0, true);
				if (isDrawerOpen) {
					// to maintain status bar & toolbar decorations after orientation change
					onDrawerOpened();
				}
            }
        });
		
		if (isOnCreateViewCalledFirstTime && sharedElements != null) {
			isOnCreateViewCalledFirstTime = false;
			animateSharedElements();
		}
		
		return rootView;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (artistRVAdapter == null) {
			artistRVAdapter = new ArtistRVAdapter(this);
		}
		recyclerVArtists.setAdapter(artistRVAdapter);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		((MainActivity) FragmentUtil.getActivity(this)).setVStatusBarVisibility(View.GONE, AppConstants.INVALID_ID);
		if (totalScrolledDy != UNSCROLLED) {
			onScrolled(0, true);
			
			if (isDrawerOpen) {
				onDrawerOpened();
			}
		}
	}
	
	@Override
	public void onStop() {
		super.onStop();
		
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		ma.setToolbarBg(ma.getResources().getColor(R.color.colorPrimary));
		ma.setVStatusBarVisibility(View.VISIBLE, R.color.colorPrimaryDark);
		ma.setVStatusBarLayeredVisibility(View.GONE, AppConstants.INVALID_ID);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (loadArtistDetails != null && loadArtistDetails.getStatus() != Status.FINISHED) {
			loadArtistDetails.cancel(true);
		}
	}
	
	private void onScrolled(int dy, boolean forceUpdate) {
		//Log.d(TAG, "totalScrolledDy = " + totalScrolledDy);
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		
		if (totalScrolledDy == UNSCROLLED) {
			totalScrolledDy = 0;
		}
		totalScrolledDy += dy;
		
		// Translate image
		ViewHelper.setTranslationY(imgArtist, (0 - totalScrolledDy) / 2);
		
		if (limitScrollAt == 0) {
			calculateScrollLimit();
			//Log.d(TAG, "vPagerCatTitles.getTop() = " + vPagerCatTitles.getTop() + ", toolbarSize = " + toolbarSize + ", ma.getStatusBarHeight() = " + ma.getStatusBarHeight());
		}
		
		int scrollY = (totalScrolledDy >= limitScrollAt) ? limitScrollAt : totalScrolledDy;
		//Log.d(TAG, "totalScrolledDy = " + totalScrolledDy + ", limitScrollAt = " + limitScrollAt + ", scrollY = " + scrollY);
		
		ViewHelper.setTranslationY(rltLytTxtArtistTitle, -totalScrolledDy);
		
		if ((!isScrollLimitReached || forceUpdate) && totalScrolledDy >= limitScrollAt) {
			ma.animateToolbarElevation(0.0f, actionBarElevation);
			
			ma.setVStatusBarLayeredVisibility(View.VISIBLE, R.color.colorPrimaryDark);
			ma.setToolbarBg(ma.getResources().getColor(R.color.colorPrimary));
			
			title = artist.getName();
			ma.updateTitle(title);
			
			isScrollLimitReached = true;
			
		} else if ((isScrollLimitReached || forceUpdate) && totalScrolledDy < limitScrollAt) {
			//Log.d(TAG, "totalScrolledDy < limitScrollAt");
			ma.animateToolbarElevation(actionBarElevation, 0.0f);
			
			ma.setVStatusBarLayeredVisibility(View.GONE, AppConstants.INVALID_ID);
			ma.setToolbarBg(Color.TRANSPARENT);
			
			title = "";
			ma.updateTitle(title);
			
			isScrollLimitReached = false;
		}
		
		if (scrollY < limitScrollAt) {
			float scale = 1 - (((1 - minTitleScale) / limitScrollAt) * scrollY);
			//Log.d(TAG, "scale = " + scale);
			
			ViewHelper.setPivotX(txtArtistTitle, 0);
	        ViewHelper.setPivotY(txtArtistTitle, txtArtistTitle.getHeight() / 2);
	        ViewHelper.setScaleX(txtArtistTitle, scale);
	        ViewHelper.setScaleY(txtArtistTitle, scale);
		}
	}
	
	private void updateArtistImg() {
		//Log.d(TAG, "updateEventImg(), url = " + event.getLowResImgUrl());
		if (artist.doesValidImgUrlExist()) {
			String key = artist.getKey(ImgResolution.LOW);
	        BitmapCache bitmapCache = BitmapCache.getInstance();
			Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
			if (bitmap != null) {
		        imgArtist.setImageBitmap(bitmap);
		        
		    } else {
		    	imgArtist.setImageBitmap(null);
		    	AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
		        asyncLoadImg.loadImg(imgArtist, ImgResolution.LOW, artist);
		    }
		}
	}
	
	private void calculateDimensions() {
		DisplayMetrics dm = new DisplayMetrics();
		FragmentUtil.getActivity(this).getWindowManager().getDefaultDisplay().getMetrics(dm);
		screenW = dm.widthPixels;
		imgArtistHt = FragmentUtil.getResources(this).getDimensionPixelSize(R.dimen.img_artist_ht_artist_details);
	}
	
	private void calculateScrollLimit() {
		Resources res = FragmentUtil.getResources(this);
		limitScrollAt = res.getDimensionPixelSize(R.dimen.img_artist_ht_artist_details) - res.getDimensionPixelSize(
				R.dimen.action_bar_ht);
		
		if (VersionUtil.isApiLevelAbove18()) {
			limitScrollAt -= ViewUtil.getStatusBarHeight(FragmentUtil.getResources(this));
		}
		
		int actionBarTitleTextSize = res.getDimensionPixelSize(R.dimen.abc_text_size_title_material_toolbar);
		int txtArtistTitleTextSize = res.getDimensionPixelSize(R.dimen.txt_artist_title_txt_size_artist_details);
		minTitleScale = actionBarTitleTextSize / (float) txtArtistTitleTextSize;
	}
	
	private void onDrawerOpened() {
		isDrawerOpen = true;
		
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		ma.setToolbarBg(ma.getResources().getColor(R.color.colorPrimary));
		ma.setToolbarElevation(ma.getResources().getDimensionPixelSize(R.dimen.action_bar_elevation));
		ma.setVStatusBarLayeredVisibility(View.VISIBLE, R.color.colorPrimaryDark);
		title = artist.getName();
		ma.updateTitle(title);
	}
	
	public String getCurrentTitle() {
		return title;
	}
	
	@Override
	public String getScreenName() {
		return "Artist Detail Screen";
	}

	@Override
	public void onDrawerClosed(View arg0) {
		isDrawerOpen = false;
		onScrolled(0, true);
	}

	@Override
	public void onDrawerOpened(View arg0) {
		onDrawerOpened();
	}

	@Override
	public void onDrawerSlide(View drawerView, float slideOffset) {
		if (!isScrollLimitReached) {
			((MainActivity)FragmentUtil.getActivity(this)).updateToolbarOnDrawerSlide(slideOffset);
		}
	}

	@Override
	public void onDrawerStateChanged(int arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void animateSharedElements() {
		SharedElement sharedElement = sharedElements.get(0);
		
		final SharedElementPosition sharedElementPosition = sharedElement.getSharedElementPosition();
		
        ObjectAnimator xAnim = ObjectAnimator.ofFloat(imgArtist, "x", sharedElementPosition.getStartX(), 0);
        xAnim.setDuration(TRANSITION_ANIM_DURATION);
        
        ObjectAnimator yAnim = ObjectAnimator.ofFloat(imgArtist, "y", sharedElementPosition.getStartY(), 0);
        yAnim.setDuration(TRANSITION_ANIM_DURATION);
        
        ValueAnimator va = ValueAnimator.ofInt(1, 100);
        va.setDuration(TRANSITION_ANIM_DURATION);
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
        	
            int color = FragmentUtil.getResources(ArtistDetailsFragment.this).getColor(android.R.color.white);
            
            public void onAnimationUpdate(ValueAnimator animation) {
                Integer progress = (Integer) animation.getAnimatedValue();

                RelativeLayout.LayoutParams lp = (LayoutParams) imgArtist.getLayoutParams();
                lp.width = (int) (sharedElementPosition.getWidth() + 
                		(((screenW - sharedElementPosition.getWidth()) * progress.intValue()) / 100));
                lp.height = (int) (sharedElementPosition.getHeight() + 
                		(((imgArtistHt - sharedElementPosition.getHeight()) * progress.intValue()) / 100));
                imgArtist.setLayoutParams(lp);
                
                int newAlpha = (int) (progress * 2.55);
                rootView.setBackgroundColor(Color.argb(newAlpha, Color.red(color), Color.green(color), Color.blue(color)));
            }
        });
        
		animatorSet = new AnimatorSet();
        animatorSet.playTogether(xAnim, yAnim, va);
        animatorSet.addListener(new AnimatorListener() {
        	
        	private boolean isCancelled;
			
			@Override
			public void onAnimationStart(Animator arg0) {
				recyclerVArtists.setVisibility(View.INVISIBLE);
				((MainActivity)FragmentUtil.getActivity(ArtistDetailsFragment.this)).onSharedElementAnimStart();
			}
			
			@Override
			public void onAnimationRepeat(Animator arg0) {}
			
			@Override
			public void onAnimationEnd(Animator arg0) {
				//Log.d(TAG, "onAnimationEnd()");
				if (!isCancelled) {
					//Log.d(TAG, "!isCancelled");
					recyclerVArtists.setVisibility(View.VISIBLE);
					Animation slideInFromBottom = AnimationUtils.loadAnimation(FragmentUtil.getApplication(ArtistDetailsFragment.this), R.anim.slide_in_from_bottom);
					slideInFromBottom.setAnimationListener(new AnimationListener() {
						
						@Override
						public void onAnimationStart(Animation animation) {}
						
						@Override
						public void onAnimationRepeat(Animation animation) {}
						
						@Override
						public void onAnimationEnd(Animation animation) {
							/**
							 * Load here instead of onCreate(), because otherwise animation slows down on some 
							 * devices
							 */
							loadArtistDetails = new LoadArtistDetails(Api.OAUTH_TOKEN, artist, ArtistDetailsFragment.this, ArtistDetailsFragment.this);
							AsyncTaskUtil.executeAsyncTask(loadArtistDetails, true);
						}
					});
					recyclerVArtists.startAnimation(slideInFromBottom);
				}
			}
			
			@Override
			public void onAnimationCancel(Animator arg0) {
				//Log.d(TAG, "onAnimationCancel()");
				isCancelled = true;
			}
		});
        
        animatorSet.start();
	}

	@Override
	public void exitAnimation() {
		animatorSet.cancel();
		recyclerVArtists.clearAnimation();
		
		Animation slideOutToBottom = AnimationUtils.loadAnimation(FragmentUtil.getApplication(ArtistDetailsFragment.this), R.anim.slide_out_to_bottom);
		slideOutToBottom.setAnimationListener(new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {}
			
			@Override
			public void onAnimationRepeat(Animation animation) {}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				recyclerVArtists.setVisibility(View.INVISIBLE);
				
				animatorSet = new AnimatorSet();
				
				SharedElement sharedElement = sharedElements.get(0);
		        
				final SharedElementPosition sharedElementPosition = sharedElement.getSharedElementPosition();
		        ObjectAnimator xAnim = ObjectAnimator.ofFloat(imgArtist, "x", 0, sharedElementPosition.getStartX());
		        xAnim.setDuration(TRANSITION_ANIM_DURATION);
		        
		        ObjectAnimator yAnim = ObjectAnimator.ofFloat(imgArtist, "y", 0, sharedElementPosition.getStartY());
		        yAnim.setDuration(TRANSITION_ANIM_DURATION);
		        
		        ValueAnimator va = ValueAnimator.ofInt(100, 1);
		        va.setDuration(TRANSITION_ANIM_DURATION);
		        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
		        	
		        	int color = FragmentUtil.getResources(ArtistDetailsFragment.this).getColor(android.R.color.white);
		        	
		            public void onAnimationUpdate(ValueAnimator animation) {
		                Integer value = (Integer) animation.getAnimatedValue();
		                imgArtist.getLayoutParams().width = (int) (sharedElementPosition.getWidth() + 
		                		(((screenW - sharedElementPosition.getWidth()) * value.intValue()) / 100));
		                imgArtist.getLayoutParams().height = (int) (sharedElementPosition.getHeight() + 
		                		(((imgArtistHt - sharedElementPosition.getHeight()) * value.intValue()) / 100));
		                imgArtist.requestLayout();
		                
		                int newAlpha = (int) (value * 2.55);
		                rootView.setBackgroundColor(Color.argb(newAlpha, Color.red(color), Color.green(color), Color.blue(color)));
		            }
		        });
		        
		        animatorSet.playTogether(xAnim, yAnim, va);
		        animatorSet.addListener(new AnimatorListener() {
					
					@Override
					public void onAnimationStart(Animator animation) {}
					
					@Override
					public void onAnimationRepeat(Animator animation) {}
					
					@Override
					public void onAnimationEnd(Animator animation) {
						FragmentUtil.getActivity(ArtistDetailsFragment.this).onBackPressed();
					}
					
					@Override
					public void onAnimationCancel(Animator animation) {}
				});
		        animatorSet.start();
			}
		});
		recyclerVArtists.startAnimation(slideOutToBottom);
    }

	@Override
	public void onArtistUpdated() {
		allDetailsLoaded = true;
		artistRVAdapter.notifyDataSetChanged();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.imgDown:
			break;
			
		default:
			break;
		}
	}
	
	private static class ArtistRVAdapter extends RecyclerView.Adapter<ArtistRVAdapter.ViewHolder>  {
		
		private static final int EXTRA_TOP_DUMMY_ITEM_COUNT = 2;
		private static final int EXTRA_TOP_DUMMY_ITEM_COUNT_AFTER_DETAILS_LOADED = 4;
		private static final int MAX_LINES_ARTIST_DESC = 5;
		
		private ArtistDetailsFragment artistDetailsFragment;
		
		private boolean isArtistDescExpanded;
		
		private VideoPagerAdapter videoPagerAdapter;
		private FriendsRVAdapter friendsRVAdapter;
		
		private static enum ViewType {
			POS_0, DESC, VIDEOS, FRIENDS
		};
		
		private static class ViewHolder extends RecyclerView.ViewHolder {
			
			private TextView txtDesc;
			private ImageView imgDown;
			private RelativeLayout rltLytPrgsBar;
			private View vHorLine;
			private ViewPager vPagerVideos;
			private RecyclerView recyclerVFriends;

			public ViewHolder(View itemView) {
				super(itemView);
				txtDesc = (TextView) itemView.findViewById(R.id.txtDesc);
				imgDown = (ImageView) itemView.findViewById(R.id.imgDown);
				rltLytPrgsBar = (RelativeLayout) itemView.findViewById(R.id.rltLytPrgsBar);
				vHorLine = itemView.findViewById(R.id.vHorLine);
				vPagerVideos = (ViewPager) itemView.findViewById(R.id.vPagerVideos);
				recyclerVFriends = (RecyclerView) itemView.findViewById(R.id.recyclerVFriends);
			}
		}
		
		public ArtistRVAdapter(ArtistDetailsFragment artistDetailsFragment) {
			this.artistDetailsFragment = artistDetailsFragment;
		}

		@Override
		public int getItemViewType(int position) {
			if (position == ViewType.POS_0.ordinal()) {
				return ViewType.POS_0.ordinal();
				
			} else if (position == ViewType.DESC.ordinal()) {
				return ViewType.DESC.ordinal();
				
			} else if (position == ViewType.VIDEOS.ordinal()) {
				return ViewType.VIDEOS.ordinal();
				
			} else {
				return ViewType.FRIENDS.ordinal();
			}
		}

		@Override
		public int getItemCount() {
			return artistDetailsFragment.allDetailsLoaded ? EXTRA_TOP_DUMMY_ITEM_COUNT_AFTER_DETAILS_LOADED :
				EXTRA_TOP_DUMMY_ITEM_COUNT;
		}

		@Override
		public void onBindViewHolder(ViewHolder holder, int position) {
			if (position == ViewType.DESC.ordinal()) {
				updateDescVisibility(holder);
				
			} else if (position == ViewType.VIDEOS.ordinal()) {
				if (videoPagerAdapter == null) {
					videoPagerAdapter = new VideoPagerAdapter(artistDetailsFragment.getChildFragmentManager(), 
							artistDetailsFragment.artist.getVideos(), artistDetailsFragment.artist.getId());
				}
				
				holder.vPagerVideos.setAdapter(videoPagerAdapter);
				holder.vPagerVideos.setOnPageChangeListener(videoPagerAdapter);
				
				// Necessary or the pager will only have one extra page to show make this at least however many pages you can see
				holder.vPagerVideos.setOffscreenPageLimit(7);
				
				// Set margin for pages as a negative number, so a part of next and previous pages will be showed
				holder.vPagerVideos.setPageMargin(FragmentUtil.getResources(artistDetailsFragment).getDimensionPixelSize(
						R.dimen.rlt_lyt_root_w_video) - artistDetailsFragment.screenW);
				
				// Set current item to the middle page so we can fling to both directions left and right
				holder.vPagerVideos.setCurrentItem(videoPagerAdapter.getCurrentPosition());
				
				updateVideosVisibility(holder);
				
			} else if (position == ViewType.FRIENDS.ordinal()) {
				if (friendsRVAdapter == null) {
					friendsRVAdapter = new FriendsRVAdapter(artistDetailsFragment.artist.getFriends());
					
					// use a linear layout manager
					RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(FragmentUtil.getActivity(artistDetailsFragment), 
							LinearLayoutManager.HORIZONTAL, false);
					holder.recyclerVFriends.setLayoutManager(layoutManager);
				} 
				
				holder.recyclerVFriends.setAdapter(friendsRVAdapter);
				
				updateFriendsVisibility(holder);
			}
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			//Log.d(TAG, "onCreateViewHolder(), viewType = " + viewType);
			View v;
			
			switch (viewType) {
			
			case 0:
				v = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_item_img_artist_artist_details, 
						parent, false);
				break;
				
			case 1:
				v = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_item_desc_artist_details, 
						parent, false);
				break;
				
			case 2:
				v = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_item_videos_artist_details, 
						parent, false);
				break;
				
			case 3:
				v = LayoutInflater.from(parent.getContext()).inflate(R.layout.include_friends, 
						parent, false);
				break;
				
			default:
				v = null;
				break;
			}
			
			ViewHolder vh = new ViewHolder(v);
	        return vh;
		}
		
		private void updateDescVisibility(ViewHolder holder) {
			if (artistDetailsFragment.allDetailsLoaded) {
				if (artistDetailsFragment.artist.getDescription() != null) {
					holder.rltLytPrgsBar.setVisibility(View.GONE);
					holder.txtDesc.setVisibility(View.VISIBLE);
					holder.imgDown.setVisibility(View.VISIBLE);
					holder.vHorLine.setVisibility(View.VISIBLE);
					
					makeDescVisible(holder);
					
				} else {
					setViewGone(holder);
				}
				
			} else {
				holder.rltLytPrgsBar.setVisibility(View.VISIBLE);
				holder.txtDesc.setVisibility(View.GONE);
				holder.imgDown.setVisibility(View.GONE);
				holder.vHorLine.setVisibility(View.GONE);
			}
		}
		
		private void makeDescVisible(final ViewHolder holder) {
			holder.txtDesc.setText(Html.fromHtml(artistDetailsFragment.artist.getDescription()));
			holder.imgDown.setVisibility(View.VISIBLE);
			holder.imgDown.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					//Log.d(TAG, "totalScrolled  = " + holder.itemView.getTop());
					if (isArtistDescExpanded) {
						collapseArtistDesc(holder);
						
						/**
						 * update scrolled distance after collapse, because sometimes it can happen that view becamse scrollable only
						 * due to expanded description after which if user collapses it, then based on recyclerview
						 * height it automatically resettles itself such that recyclerview again becomes unscrollable.
						 * Accordingly we need to reset scrolled amount, artist img & title
						 */
						holder.itemView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
				            @Override
				            public void onGlobalLayout() {
				            	//Log.d(TAG, "onGlobalLayout()");
								if (VersionUtil.isApiLevelAbove15()) {
									holder.itemView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

								} else {
									holder.itemView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
								}
								
								//Log.d(TAG, "totalScrolled on global layout  = " + holder.itemView.getTop());
								artistDetailsFragment.totalScrolledDy = artistDetailsFragment.imgArtistHt - holder.itemView.getTop();
								artistDetailsFragment.onScrolled(0, true);
				            }
				        });
						
					} else {
						expandArtistDesc(holder);
					}
					//Log.d(TAG, "totalScrolled after  = " + holder.itemView.getTop());
				}
			});
			
			if (isArtistDescExpanded) {
				expandArtistDesc(holder);
				
			} else {
				collapseArtistDesc(holder);
			}
		}
		
		private void expandArtistDesc(ViewHolder holder) {
			holder.txtDesc.setMaxLines(Integer.MAX_VALUE);
			holder.txtDesc.setEllipsize(null);
			holder.imgDown.setImageDrawable(FragmentUtil.getResources(artistDetailsFragment).getDrawable(R.drawable.less));
			isArtistDescExpanded = true;
		}
		
		private void collapseArtistDesc(ViewHolder holder) {
			holder.txtDesc.setMaxLines(MAX_LINES_ARTIST_DESC);
			holder.txtDesc.setEllipsize(TruncateAt.END);
			holder.imgDown.setImageDrawable(FragmentUtil.getResources(artistDetailsFragment).getDrawable(R.drawable.down));
			isArtistDescExpanded = false;
		}
		
		private void updateVideosVisibility(ViewHolder holder) {
			if (!artistDetailsFragment.artist.getVideos().isEmpty()) {
				videoPagerAdapter.notifyDataSetChanged();
				
			} else {
				setViewGone(holder);
			}
		}
		
		private void updateFriendsVisibility(ViewHolder holder) {
			if (!artistDetailsFragment.artist.getFriends().isEmpty()) {
				friendsRVAdapter.notifyDataSetChanged();
				
			} else {
				setViewGone(holder);
			}
		}
		
		private void setViewGone(ViewHolder holder) {
			RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) holder.itemView.getLayoutParams();
			lp.height = 0;
			holder.itemView.setLayoutParams(lp);
		}
	}
}
