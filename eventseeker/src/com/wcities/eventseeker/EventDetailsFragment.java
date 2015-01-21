package com.wcities.eventseeker;

import java.util.List;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
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
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.asynctask.LoadEventDetails;
import com.wcities.eventseeker.asynctask.LoadEventDetails.OnEventUpdatedListner;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Date;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Schedule;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.custom.view.ObservableScrollView;
import com.wcities.eventseeker.custom.view.ObservableScrollView.ObservableScrollViewListener;
import com.wcities.eventseeker.interfaces.CustomSharedElementTransitionDestination;
import com.wcities.eventseeker.interfaces.VenueListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.VersionUtil;
import com.wcities.eventseeker.util.ViewUtil;
import com.wcities.eventseeker.viewdata.SharedElement;
import com.wcities.eventseeker.viewdata.SharedElementPosition;

public class EventDetailsFragment extends FragmentLoadableFromBackStack implements ObservableScrollViewListener, 
		DrawerListener, CustomSharedElementTransitionDestination, OnClickListener, OnEventUpdatedListner {
	
	private static final String TAG = EventDetailsFragment.class.getSimpleName();
	
	private static final int UNSCROLLED = -1;
	private static final int TRANSITION_ANIM_DURATION = 400;
	
	private View rootView;
	private ImageView imgEvent, imgDown;
	private TextView txtEvtTitle, txtEvtDesc, txtEvtLoc, txtVenue, txtEvtTime;
	private RelativeLayout rltLytContent, rltLytFeaturing, prgsBar, rltLytVenue, rltLytFriends;
	
	private int limitScrollAt, actionBarElevation, actionBarTitleTextSize, txtEvtTitleTextSize, prevScrollY = UNSCROLLED;
	private boolean isScrollLimitReached, isDrawerOpen;
	private String title = "";
	private float minTitleScale;
	
	private Event event;
	private boolean isEvtDescExpanded;
	
	private LoadEventDetails loadEventDetails;
	private boolean allDetailsLoaded;
	
	private FeaturingArtistPagerAdapter featuringArtistPagerAdapter;
	
	private List<SharedElement> sharedElements;
	private boolean isOnCreateViewCalledFirstTime = true;
	private int screenW, imgEventHt;
	private AnimatorSet animatorSet;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setHasOptionsMenu(true);
		setRetainInstance(true);
		
		actionBarElevation = FragmentUtil.getResources(this).getDimensionPixelSize(R.dimen.action_bar_elevation);
		
		Bundle args = getArguments();
		if (args != null && args.containsKey(BundleKeys.SHARED_ELEMENTS)) {
			sharedElements = (List<SharedElement>) args.getSerializable(BundleKeys.SHARED_ELEMENTS);
		}
		
		if (event == null) {
			//Log.d(TAG, "event = null");
			event = (Event) args.getSerializable(BundleKeys.EVENT);
			
			event.getFriends().clear();

			loadEventDetails = new LoadEventDetails(Api.OAUTH_TOKEN, this, this, event);
			AsyncTaskUtil.executeAsyncTask(loadEventDetails, true);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		//Log.d(TAG, "onCreateView()");

		/**
		 * on orientation change we need to recalculate this due to different values of 
		 * action_bar_ht on both orientations
		 */
		calculateScrollLimit();
		calculateDimensions();
		
		rootView = inflater.inflate(R.layout.fragment_event_details, container, false);
		
		rltLytContent = (RelativeLayout) rootView.findViewById(R.id.rltLytContent);
		
		imgEvent = (ImageView) rootView.findViewById(R.id.imgEvent);
		txtEvtTitle = (TextView) rootView.findViewById(R.id.txtEvtTitle);
		txtEvtTitle.setText(event.getName());
		// for marquee to work
		txtEvtTitle.setSelected(true);
		
		txtEvtLoc = (TextView) rootView.findViewById(R.id.txtEvtLoc);
		txtEvtTime = (TextView) rootView.findViewById(R.id.txtEvtTime);
		txtVenue = (TextView) rootView.findViewById(R.id.txtVenue);
		updateEventSchedule();

		txtEvtDesc = (TextView) rootView.findViewById(R.id.txtDesc);
		imgDown = (ImageView) rootView.findViewById(R.id.imgDown);
		updateDescVisibility();
		
		updateEventImg();
		
		prgsBar = (RelativeLayout) rootView.findViewById(R.id.prgsBar);
		rltLytFeaturing = (RelativeLayout) rootView.findViewById(R.id.rltLytFeaturing);
		rltLytVenue = (RelativeLayout) rootView.findViewById(R.id.rltLytVenue);
		rltLytFriends = (RelativeLayout) rootView.findViewById(R.id.rltLytFriends);
		updateDetailsVisibility();
		
		// use a linear layout manager
		RecyclerView recyclerVFriends = (RecyclerView) rootView.findViewById(R.id.recyclerVFriends);
		RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(FragmentUtil.getActivity(this));
		recyclerVFriends.setLayoutManager(layoutManager);
		
		ViewPager vPagerFeaturing = (ViewPager) rootView.findViewById(R.id.vPagerFeaturing);
		if (featuringArtistPagerAdapter == null) {
			featuringArtistPagerAdapter = new FeaturingArtistPagerAdapter(getChildFragmentManager(), event.getArtists(), 
					vPagerFeaturing);
			
		} else {
			featuringArtistPagerAdapter.setViewPager(vPagerFeaturing);
		}
		vPagerFeaturing.setAdapter(featuringArtistPagerAdapter);
		vPagerFeaturing.setOnPageChangeListener(featuringArtistPagerAdapter);
		
		// Set current item to the middle page so we can fling to both directions left and right
		vPagerFeaturing.setCurrentItem(featuringArtistPagerAdapter.getCurrentPosition());
		
		// Necessary or the pager will only have one extra page to show make this at least however many pages you can see
		vPagerFeaturing.setOffscreenPageLimit(7);
		
		// Set margin for pages as a negative number, so a part of next and previous pages will be showed
		vPagerFeaturing.setPageMargin(FragmentUtil.getResources(this).getDimensionPixelSize(
				R.dimen.rlt_lyt_root_w_featuring_artist) - screenW);
		
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
		
		if (loadEventDetails != null && loadEventDetails.getStatus() != Status.FINISHED) {
			loadEventDetails.cancel(true);
		}
	}
	
	private void calculateDimensions() {
		DisplayMetrics dm = new DisplayMetrics();
		FragmentUtil.getActivity(this).getWindowManager().getDefaultDisplay().getMetrics(dm);
		screenW = dm.widthPixels;
		imgEventHt = FragmentUtil.getResources(this).getDimensionPixelSize(R.dimen.img_event_ht_event_details);
	}
	
	@Override
	public void animateSharedElements() {
		SharedElement sharedElement = sharedElements.get(0);
		
		ViewCompat.setTransitionName(imgEvent, sharedElement.getTransitionName());
		
		final SharedElementPosition sharedElementPosition = sharedElement.getSharedElementPosition();
		
        ObjectAnimator xAnim = ObjectAnimator.ofFloat(imgEvent, "x", sharedElementPosition.getStartX(), 0);
        xAnim.setDuration(TRANSITION_ANIM_DURATION);
        
        ObjectAnimator yAnim = ObjectAnimator.ofFloat(imgEvent, "y", sharedElementPosition.getStartY(), 0);
        yAnim.setDuration(TRANSITION_ANIM_DURATION);
        
        ValueAnimator va = ValueAnimator.ofInt(1, 100);
        va.setDuration(TRANSITION_ANIM_DURATION);
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
        	
            int color = FragmentUtil.getResources(EventDetailsFragment.this).getColor(android.R.color.white);
            
            public void onAnimationUpdate(ValueAnimator animation) {
                Integer progress = (Integer) animation.getAnimatedValue();

                RelativeLayout.LayoutParams lp = (LayoutParams) imgEvent.getLayoutParams();
                lp.width = (int) (sharedElementPosition.getWidth() + 
                		(((screenW - sharedElementPosition.getWidth()) * progress.intValue()) / 100));
                lp.height = (int) (sharedElementPosition.getHeight() + 
                		(((imgEventHt - sharedElementPosition.getHeight()) * progress.intValue()) / 100));
                imgEvent.setLayoutParams(lp);
                
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
			}
			
			@Override
			public void onAnimationRepeat(Animator arg0) {}
			
			@Override
			public void onAnimationEnd(Animator arg0) {
				//Log.d(TAG, "onAnimationEnd()");
				if (!isCancelled) {
					//Log.d(TAG, "!isCancelled");
					rltLytContent.setVisibility(View.VISIBLE);
					Animation slideInFromBottom = AnimationUtils.loadAnimation(FragmentUtil.getApplication(EventDetailsFragment.this), R.anim.slide_in_from_bottom);
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
		
		Animation slideOutToBottom = AnimationUtils.loadAnimation(FragmentUtil.getApplication(EventDetailsFragment.this), R.anim.slide_out_to_bottom);
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
		        ObjectAnimator xAnim = ObjectAnimator.ofFloat(imgEvent, "x", 0, sharedElementPosition.getStartX());
		        xAnim.setDuration(TRANSITION_ANIM_DURATION);
		        
		        ObjectAnimator yAnim = ObjectAnimator.ofFloat(imgEvent, "y", 0, sharedElementPosition.getStartY());
		        yAnim.setDuration(TRANSITION_ANIM_DURATION);
		        
		        ValueAnimator va = ValueAnimator.ofInt(100, 1);
		        va.setDuration(TRANSITION_ANIM_DURATION);
		        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
		        	
		        	int color = FragmentUtil.getResources(EventDetailsFragment.this).getColor(android.R.color.white);
		        	
		            public void onAnimationUpdate(ValueAnimator animation) {
		                Integer value = (Integer) animation.getAnimatedValue();
		                imgEvent.getLayoutParams().width = (int) (sharedElementPosition.getWidth() + 
		                		(((screenW - sharedElementPosition.getWidth()) * value.intValue()) / 100));
		                imgEvent.getLayoutParams().height = (int) (sharedElementPosition.getHeight() + 
		                		(((imgEventHt - sharedElementPosition.getHeight()) * value.intValue()) / 100));
		                imgEvent.requestLayout();
		                
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
						FragmentUtil.getActivity(EventDetailsFragment.this).onBackPressed();
					}
					
					@Override
					public void onAnimationCancel(Animator animation) {}
				});
		        animatorSet.start();
			}
		});
		rltLytContent.startAnimation(slideOutToBottom);
    }
	
	private void updateDescVisibility() {
		if (event.getDescription() != null) {
			makeDescVisible();
			
		} else {
			txtEvtDesc.setVisibility(View.GONE);
			imgDown.setVisibility(View.GONE);
		}
	}
	
	private void updateEventSchedule() {
		Schedule schedule = event.getSchedule();
		if (schedule != null) {
			if (schedule.getVenue() != null) {
				txtEvtLoc.setText(event.getSchedule().getVenue().getName());
				txtEvtLoc.setOnClickListener(this);
				txtVenue.setText(event.getSchedule().getVenue().getName());
				txtVenue.setOnClickListener(this);
			}
			
			if (schedule.getDates().size() > 0) {
				Date date = schedule.getDates().get(0);
				txtEvtTime.setText(ConversionUtil.getDateTime(date.getStartDate(), date.isStartTimeAvailable()));
			}
		}
	}
	
	private void makeDescVisible() {
		txtEvtDesc.setText(Html.fromHtml(event.getDescription()));
		imgDown.setVisibility(View.VISIBLE);
		imgDown.setOnClickListener(this);
		
		if (isEvtDescExpanded) {
			expandEvtDesc();
			
		} else {
			collapseEvtDesc();
		}
	}
	
	private void expandEvtDesc() {
		txtEvtDesc.setVisibility(View.VISIBLE);
		imgDown.setImageDrawable(FragmentUtil.getResources(this).getDrawable(R.drawable.less));

		isEvtDescExpanded = true;
	}
	
	private void collapseEvtDesc() {
		txtEvtDesc.setVisibility(View.GONE);
		imgDown.setImageDrawable(FragmentUtil.getResources(this).getDrawable(R.drawable.down));
		
		isEvtDescExpanded = false;
	}
	
	private void updateEventImg() {
		//Log.d(TAG, "updateEventImg(), url = " + event.getLowResImgUrl());
		if (event.doesValidImgUrlExist()) {
			String key = event.getKey(ImgResolution.LOW);
	        BitmapCache bitmapCache = BitmapCache.getInstance();
			Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
			if (bitmap != null) {
		        imgEvent.setImageBitmap(bitmap);
		        
		    } else {
		    	imgEvent.setImageBitmap(null);
		    	AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
		        asyncLoadImg.loadImg(imgEvent, ImgResolution.LOW, event);
		    }
		}
	}
	
	private void updateFeaturingVisibility() {
		if (event.hasArtists()) {
			rltLytFeaturing.setVisibility(View.VISIBLE);
			featuringArtistPagerAdapter.notifyDataSetChanged();
			
		} else {
			rltLytFeaturing.setVisibility(View.GONE);
		}
	}
	
	private void updateAddressMapVisibility() {
		if (event.getSchedule() == null || event.getSchedule().getVenue() == null) {
			return;
		}
		
		rltLytVenue.setVisibility(View.VISIBLE);
		AddressMapFragment fragment = (AddressMapFragment) getChildFragmentManager().findFragmentByTag(
				AppConstants.FRAGMENT_TAG_ADDRESS_MAP);
        if (fragment == null) {
        	addAddressMapFragment();
        }
	}
	
	private void updateFriendsVisibility() {
		if (!event.getFriends().isEmpty()) {
			rltLytFriends.setVisibility(View.VISIBLE);
		}
	}
	
	private void addAddressMapFragment() {
    	FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        
        AddressMapFragment fragment = new AddressMapFragment();
        fragment.setArguments(getArguments());
        fragmentTransaction.add(R.id.frmLayoutMapContainer, fragment, AppConstants.FRAGMENT_TAG_ADDRESS_MAP);
        try {
        	fragmentTransaction.commit();
        	
        } catch (IllegalStateException e) {
        	/**
        	 * This catch is to prevent possible "java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState"
        	 * when it's called from callback method updateDetailsVisibility() & if user has already left this screen.
        	 */
			Log.e(TAG, "IllegalStateException: " + e.getMessage());
			e.printStackTrace();
		}
    }
	
	private void updateDetailsVisibility() {
		if (allDetailsLoaded) {
			prgsBar.setVisibility(View.GONE);
			updateFeaturingVisibility();
			updateEventSchedule();
			updateAddressMapVisibility();
			updateFriendsVisibility();
			
		} else {
			prgsBar.setVisibility(View.VISIBLE);
			rltLytFeaturing.setVisibility(View.GONE);
			rltLytVenue.setVisibility(View.GONE);
			rltLytFriends.setVisibility(View.GONE);
		}
	}
	
	private void calculateScrollLimit() {
		Resources res = FragmentUtil.getResources(this);
		limitScrollAt = res.getDimensionPixelSize(R.dimen.img_event_ht_event_details) - res.getDimensionPixelSize(
				R.dimen.action_bar_ht);
		
		if (VersionUtil.isApiLevelAbove18()) {
			limitScrollAt -= ViewUtil.getStatusBarHeight(FragmentUtil.getResources(this));
		}
		
		actionBarTitleTextSize = res.getDimensionPixelSize(R.dimen.abc_text_size_title_material_toolbar);
		txtEvtTitleTextSize = res.getDimensionPixelSize(R.dimen.txt_evt_title_txt_size_event_details);
		minTitleScale = actionBarTitleTextSize / (float) txtEvtTitleTextSize;
	}
	
	private void onScrollChanged(int scrollY, boolean forceUpdate) {
		// Translate image
		ViewHelper.setTranslationY(imgEvent, scrollY / 2);
		
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		
		if (limitScrollAt == 0) {
			calculateScrollLimit();
		}
		
		if ((!isScrollLimitReached || forceUpdate) && scrollY >= limitScrollAt) {
			ma.animateToolbarElevation(0.0f, actionBarElevation);
			
			ma.setVStatusBarLayeredVisibility(View.VISIBLE, R.color.colorPrimaryDark);
			ma.setToolbarBg(ma.getResources().getColor(R.color.colorPrimary));
			
			title = event.getName();
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
			
			ViewHelper.setPivotX(txtEvtTitle, 0);
	        ViewHelper.setPivotY(txtEvtTitle, txtEvtTitle.getHeight() / 2);
	        ViewHelper.setScaleX(txtEvtTitle, scale);
	        ViewHelper.setScaleY(txtEvtTitle, scale);
		}
        
		prevScrollY = scrollY;
	}
	
	public String getCurrentTitle() {
		return title;
	}
	
	private void onDrawerOpened() {
		isDrawerOpen = true;
		
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		ma.setToolbarBg(ma.getResources().getColor(R.color.colorPrimary));
		ma.setToolbarElevation(ma.getResources().getDimensionPixelSize(R.dimen.action_bar_elevation));
		ma.setVStatusBarLayeredVisibility(View.VISIBLE, R.color.colorPrimaryDark);
		title = "Cut Copy";
		ma.updateTitle(title);
	}
	
	@Override
	public String getScreenName() {
		return "Event Detail Screen";
	}

	@Override
	public void onScrollChanged(int scrollY) {
		//Log.d(TAG, "onScrollChanged(), scrollY = " + scrollY);
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
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.txtEvtLoc:
			((VenueListener)FragmentUtil.getActivity(this)).onVenueSelected(event.getSchedule().getVenue());
			break;
			
		case R.id.imgDown:
			if (isEvtDescExpanded) {
				collapseEvtDesc();
				
			} else {
				expandEvtDesc();
			}
			break;
			
		default:
			break;
		}
	}

	@Override
	public void onEventUpdated() {
		allDetailsLoaded = true;
		updateDetailsVisibility();
	}
}
