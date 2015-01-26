package com.wcities.eventseeker;

import java.util.List;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout.DrawerListener;
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
import com.wcities.eventseeker.adapter.FeaturingArtistPagerAdapter;
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
import com.wcities.eventseeker.custom.view.ObservableScrollView;
import com.wcities.eventseeker.custom.view.ObservableScrollView.ObservableScrollViewListener;
import com.wcities.eventseeker.interfaces.CustomSharedElementTransitionDestination;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.VersionUtil;
import com.wcities.eventseeker.util.ViewUtil;
import com.wcities.eventseeker.viewdata.SharedElement;
import com.wcities.eventseeker.viewdata.SharedElementPosition;

public class ArtistDetailsFragment extends FragmentLoadableFromBackStack implements ObservableScrollViewListener, 
		DrawerListener, CustomSharedElementTransitionDestination, OnArtistUpdatedListener, OnClickListener {
	
	private static final String TAG = ArtistDetailsFragment.class.getName();
	
	private static final int UNSCROLLED = -1;
	private static final int TRANSITION_ANIM_DURATION = 400;
	private static final int MAX_LINES_ARTIST_DESC = 5;

	private View rootView;
	private ImageView imgArtist, imgDown;
	private TextView txtArtistTitle, txtArtistDesc;
	private RelativeLayout rltLytContent, rltLytPrgsBar, rltLytVideos;
	
	private int limitScrollAt, actionBarElevation, prevScrollY = UNSCROLLED;
	private float minTitleScale;
	private boolean isScrollLimitReached, isDrawerOpen;
	private String title = "";
	
	private Artist artist;
	private LoadArtistDetails loadArtistDetails;
	private boolean allDetailsLoaded;
	private boolean isArtistDescExpanded;
	
	private List<SharedElement> sharedElements;
	private boolean isOnCreateViewCalledFirstTime = true;
	private int screenW, imgArtistHt;
	private AnimatorSet animatorSet;
	
	private VideoPagerAdapter videoPagerAdapter;
	
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
		
		rltLytContent = (RelativeLayout) rootView.findViewById(R.id.rltLytContent);
		
		imgArtist = (ImageView) rootView.findViewById(R.id.imgArtist);
		updateArtistImg();
		
		txtArtistTitle = (TextView) rootView.findViewById(R.id.txtArtistTitle);
		txtArtistTitle.setText(artist.getName());
		// for marquee to work
		txtArtistTitle.setSelected(true);
		
		rltLytPrgsBar = (RelativeLayout) rootView.findViewById(R.id.rltLytPrgsBar);
		txtArtistDesc = (TextView) rootView.findViewById(R.id.txtDesc);
		imgDown = (ImageView) rootView.findViewById(R.id.imgDown);
		rltLytVideos = (RelativeLayout) rootView.findViewById(R.id.rltLytVideos);
		
		updateDetailsVisibility();
		
		ViewPager vPagerVideos = (ViewPager) rootView.findViewById(R.id.vPagerVideos);
		if (videoPagerAdapter == null) {
			videoPagerAdapter = new VideoPagerAdapter(getChildFragmentManager(), artist.getVideos(), artist.getId(),  
					vPagerVideos);
			
		} else {
			videoPagerAdapter.setViewPager(vPagerVideos);
		}
		vPagerVideos.setAdapter(videoPagerAdapter);
		vPagerVideos.setOnPageChangeListener(videoPagerAdapter);
		
		// Set current item to the middle page so we can fling to both directions left and right
		vPagerVideos.setCurrentItem(videoPagerAdapter.getCurrentPosition());
		
		// Necessary or the pager will only have one extra page to show make this at least however many pages you can see
		vPagerVideos.setOffscreenPageLimit(7);
		
		// Set margin for pages as a negative number, so a part of next and previous pages will be showed
		vPagerVideos.setPageMargin(FragmentUtil.getResources(this).getDimensionPixelSize(
				R.dimen.rlt_lyt_root_w_video) - screenW);
		
		final ObservableScrollView obsrScrlV = (ObservableScrollView) rootView.findViewById(R.id.obsrScrlV);
		obsrScrlV.setListener(this);
		
		obsrScrlV.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
            	//Log.d(TAG, "onGlobalLayout()");
				if (VersionUtil.isApiLevelAbove15()) {
					obsrScrlV.getViewTreeObserver().removeOnGlobalLayoutListener(this);

				} else {
					obsrScrlV.getViewTreeObserver().removeGlobalOnLayoutListener(this);
				}
				
                if (prevScrollY == UNSCROLLED) {
                	onScrollChanged(obsrScrlV.getScrollY(), true);
                	
                } else {
                	obsrScrlV.scrollTo(0, prevScrollY);
                	
                	if (isDrawerOpen) {
        				onDrawerOpened();
        			}
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
	public void onStart() {
		super.onStart();
		
		((MainActivity) FragmentUtil.getActivity(this)).setVStatusBarVisibility(View.GONE, AppConstants.INVALID_ID);
		if (prevScrollY != UNSCROLLED) {
			onScrollChanged(prevScrollY, true);
			
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
	
	private void updateDescVisibility() {
		if (artist.getDescription() != null) {
			makeDescVisible();
			
		} else {
			txtArtistDesc.setVisibility(View.GONE);
			imgDown.setVisibility(View.GONE);
		}
	}
	
	private void makeDescVisible() {
		txtArtistDesc.setVisibility(View.VISIBLE);
		txtArtistDesc.setText(Html.fromHtml(artist.getDescription()));
		imgDown.setVisibility(View.VISIBLE);
		imgDown.setOnClickListener(this);
		
		if (isArtistDescExpanded) {
			expandArtistDesc();
			
		} else {
			collapseArtistDesc();
		}
	}
	
	private void expandArtistDesc() {
		txtArtistDesc.setMaxLines(Integer.MAX_VALUE);
		txtArtistDesc.setEllipsize(null);
		imgDown.setImageDrawable(FragmentUtil.getResources(this).getDrawable(R.drawable.less));
		isArtistDescExpanded = true;
	}
	
	private void collapseArtistDesc() {
		txtArtistDesc.setMaxLines(MAX_LINES_ARTIST_DESC);
		txtArtistDesc.setEllipsize(TruncateAt.END);
		imgDown.setImageDrawable(FragmentUtil.getResources(this).getDrawable(R.drawable.down));
		isArtistDescExpanded = false;
	}
	
	private void updateVideosVisibility() {
		if (!artist.getVideos().isEmpty()) {
			rltLytVideos.setVisibility(View.VISIBLE);
			videoPagerAdapter.notifyDataSetChanged();
			
		} else {
			rltLytVideos.setVisibility(View.GONE);
		}
	}
	
	private void updateDetailsVisibility() {
		if (allDetailsLoaded) {
			//updateShareIntent();
			
			rltLytPrgsBar.setVisibility(View.GONE);
			updateDescVisibility();
			updateVideosVisibility();
			/*updateEventSchedule();
			updateAddressMapVisibility();
			updateFriendsVisibility();
			updateFabs();*/
			
		} else {
			rltLytPrgsBar.setVisibility(View.VISIBLE);
			txtArtistDesc.setVisibility(View.GONE);
			imgDown.setVisibility(View.GONE);
			rltLytVideos.setVisibility(View.GONE);
			/*rltLytVenue.setVisibility(View.GONE);
			rltLytFriends.setVisibility(View.GONE);
			fabTickets.setVisibility(View.GONE);
			fabSave.setVisibility(View.GONE);*/
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
	
	private void onScrollChanged(int scrollY, boolean forceUpdate) {
		//Log.d(TAG, "scrollY = " + scrollY);
		// Translate image
		ViewHelper.setTranslationY(imgArtist, scrollY / 2);
		
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		
		if (limitScrollAt == 0) {
			calculateScrollLimit();
		}
		
		if ((!isScrollLimitReached || forceUpdate) && scrollY >= limitScrollAt) {
			ma.animateToolbarElevation(0.0f, actionBarElevation);
			
			ma.setVStatusBarLayeredVisibility(View.VISIBLE, R.color.colorPrimaryDark);
			ma.setToolbarBg(ma.getResources().getColor(R.color.colorPrimary));
			
			title = artist.getName();
			ma.updateTitle(title);
			
			isScrollLimitReached = true;
			
		} else if ((isScrollLimitReached || forceUpdate) && scrollY < limitScrollAt) {
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
        
		prevScrollY = scrollY;
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
	public void onScrollChanged(int scrollY) {
		onScrollChanged(scrollY, false);
	}

	@Override
	public void onDownMotionEvent() {
		// TODO Auto-generated method stub
	}

	@Override
	public void onUpOrCancelMotionEvent() {
		// TODO Auto-generated method stub
	}

	@Override
	public void onDrawerClosed(View arg0) {
		isDrawerOpen = false;
		onScrollChanged(prevScrollY, true);
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
				rltLytContent.setVisibility(View.INVISIBLE);
				((MainActivity)FragmentUtil.getActivity(ArtistDetailsFragment.this)).onSharedElementAnimStart();
			}
			
			@Override
			public void onAnimationRepeat(Animator arg0) {}
			
			@Override
			public void onAnimationEnd(Animator arg0) {
				//Log.d(TAG, "onAnimationEnd()");
				if (!isCancelled) {
					//Log.d(TAG, "!isCancelled");
					rltLytContent.setVisibility(View.VISIBLE);
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
					rltLytContent.startAnimation(slideInFromBottom);
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
		rltLytContent.clearAnimation();
		
		Animation slideOutToBottom = AnimationUtils.loadAnimation(FragmentUtil.getApplication(ArtistDetailsFragment.this), R.anim.slide_out_to_bottom);
		slideOutToBottom.setAnimationListener(new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {}
			
			@Override
			public void onAnimationRepeat(Animation animation) {}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				rltLytContent.setVisibility(View.INVISIBLE);
				
				animatorSet = new AnimatorSet();
				
				SharedElement sharedElement = sharedElements.get(0);
		        
				final SharedElementPosition sharedElementPosition = sharedElement.getSharedElementPosition();
		        ObjectAnimator xAnim = ObjectAnimator.ofFloat(imgArtist, "x", 0, sharedElementPosition.getStartX());
		        xAnim.setDuration(TRANSITION_ANIM_DURATION);
		        
		        ObjectAnimator yAnim = ObjectAnimator.ofFloat(imgArtist, "y", 0, sharedElementPosition.getStartY() + prevScrollY);
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
		rltLytContent.startAnimation(slideOutToBottom);
    }

	@Override
	public void onArtistUpdated() {
		allDetailsLoaded = true;
		updateDetailsVisibility();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.imgDown:
			if (isArtistDescExpanded) {
				collapseArtistDesc();
				
			} else {
				expandArtistDesc();
			}
			break;
			
		default:
			break;
		}
	}
}
