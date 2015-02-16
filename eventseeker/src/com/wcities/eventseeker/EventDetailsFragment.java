package com.wcities.eventseeker;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.ShareActionProvider.OnShareTargetSelectedListener;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.facebook.Session;
import com.facebook.SessionState;
import com.melnykov.fab.FloatingActionButton;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.Animator.AnimatorListener;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.view.ViewHelper;
import com.wcities.eventseeker.adapter.FeaturingArtistPagerAdapter;
import com.wcities.eventseeker.adapter.FriendsRVAdapter;
import com.wcities.eventseeker.analytics.GoogleAnalyticsTracker;
import com.wcities.eventseeker.analytics.GoogleAnalyticsTracker.Type;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingItemType;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.asynctask.LoadEventDetails;
import com.wcities.eventseeker.asynctask.LoadEventDetails.OnEventUpdatedListner;
import com.wcities.eventseeker.asynctask.UserTracker;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Date;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Event.Attending;
import com.wcities.eventseeker.core.Schedule;
import com.wcities.eventseeker.custom.fragment.PublishEventFragmentLoadableFromBackStack;
import com.wcities.eventseeker.custom.view.ObservableScrollView;
import com.wcities.eventseeker.custom.view.ObservableScrollView.ObservableScrollViewListener;
import com.wcities.eventseeker.interfaces.CustomSharedElementTransitionDestination;
import com.wcities.eventseeker.interfaces.CustomSharedElementTransitionSource;
import com.wcities.eventseeker.interfaces.ReplaceFragmentListener;
import com.wcities.eventseeker.interfaces.VenueListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.FbUtil;
import com.wcities.eventseeker.util.FileUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.VersionUtil;
import com.wcities.eventseeker.util.ViewUtil;
import com.wcities.eventseeker.viewdata.SharedElement;
import com.wcities.eventseeker.viewdata.SharedElementPosition;

public class EventDetailsFragment extends PublishEventFragmentLoadableFromBackStack implements ObservableScrollViewListener, 
		DrawerListener, CustomSharedElementTransitionDestination, OnClickListener, OnEventUpdatedListner, 
		CustomSharedElementTransitionSource {
	
	private static final String TAG = EventDetailsFragment.class.getSimpleName();
	
	private static final int UNSCROLLED = -1;
	private static final int TRANSITION_ANIM_DURATION = 400, FAB_SCROLL_THRESHOLD_IN_DP = 4;
	
	private View rootView;
	private ObservableScrollView obsrScrlV;
	private ImageView imgEvent, imgDown;
	private TextView txtEvtTitle, txtEvtDesc, txtEvtLoc, txtVenue, txtEvtTime;
	private RelativeLayout rltLytContent, rltLytFeaturing, rltLytPrgsBar, rltLytVenue, rltLytFriends;
	private RecyclerView recyclerVFriends;
	private ShareActionProvider mShareActionProvider;
	private FloatingActionButton fabTickets, fabSave;
	
	private int limitScrollAt, actionBarElevation, fabScrollThreshold, prevScrollY = UNSCROLLED;
	private boolean isScrollLimitReached, isOnPushedToBackStackCalled;
	private String title = "";
	private float minTitleScale;
	
	private boolean isEvtDescExpanded;
	
	private LoadEventDetails loadEventDetails;
	private boolean allDetailsLoaded;
	
	private FeaturingArtistPagerAdapter featuringArtistPagerAdapter;
	private FriendsRVAdapter friendsRVAdapter;
	
	private List<SharedElement> sharedElements;
	private boolean isOnCreateViewCalledFirstTime = true;
	private int screenW, imgEventHt;
	private AnimatorSet animatorSet;
	
	private int fbCallCountForSameEvt = 0;
	
	private List<View> hiddenViews;
	
	private OnShareTargetSelectedListener onShareTargetSelectedListener = new OnShareTargetSelectedListener() {
		
		@Override
		public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
			EventSeekr eventSeekr = FragmentUtil.getApplication(EventDetailsFragment.this);
			String shareTarget = intent.getComponent().getPackageName();
			//Log.d(TAG, "shareTarget = " + shareTarget);
			if (eventSeekr.getPackageName().equals(shareTarget)) {
				//Log.d(TAG, "shareTarget = " + shareTarget);
				// required to handle "add to calendar" action
				eventSeekr.setEventToAddToCalendar(event);
			}
			
			GoogleAnalyticsTracker.getInstance().sendShareEvent(eventSeekr, getScreenName(), 
					shareTarget, Type.Event, event.getId());
			return false;
		}
	};
	
	private OnGlobalLayoutListener onGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
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
            	
            	if (((MainActivity)FragmentUtil.getActivity(EventDetailsFragment.this)).isDrawerOpen()) {
    				onDrawerOpened();
    			}
            }
        }
    };
	
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
		
		if (event == null) {
			//Log.d(TAG, "event = null");
			event = (Event) args.getSerializable(BundleKeys.EVENT);
			
			event.getFriends().clear();

			updateShareIntent();
		}
		//Log.d(TAG, "AT issue event = " + event);
		fabScrollThreshold = ConversionUtil.toPx(FragmentUtil.getResources(this), FAB_SCROLL_THRESHOLD_IN_DP);
		
		hiddenViews = new ArrayList<View>();
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
		
		obsrScrlV = (ObservableScrollView) rootView.findViewById(R.id.obsrScrlV);
		obsrScrlV.setListener(this);
		
		rltLytPrgsBar = (RelativeLayout) rootView.findViewById(R.id.rltLytPrgsBar);
		rltLytFeaturing = (RelativeLayout) rootView.findViewById(R.id.rltLytFeaturing);
		rltLytVenue = (RelativeLayout) rootView.findViewById(R.id.rltLytVenue);
		rltLytFriends = (RelativeLayout) rootView.findViewById(R.id.rltLytFriends);
		
		fabTickets = (FloatingActionButton) rootView.findViewById(R.id.fabTickets);
		fabSave = (FloatingActionButton) rootView.findViewById(R.id.fabSave);
		fabTickets.setOnClickListener(this);
		fabSave.setOnClickListener(this);
        
		updateDetailsVisibility();
		
		recyclerVFriends = (RecyclerView) rootView.findViewById(R.id.recyclerVFriends);
		// use a linear layout manager
		RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(FragmentUtil.getActivity(this), 
				LinearLayoutManager.HORIZONTAL, false);
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
		
		obsrScrlV.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
		
		if (isOnCreateViewCalledFirstTime) { 
			isOnCreateViewCalledFirstTime = false;
			
			if (sharedElements != null) {
				animateSharedElements();
				
			} else {
				rootView.setBackgroundColor(Color.WHITE);
				
				loadEventDetails = new LoadEventDetails(Api.OAUTH_TOKEN, this, this, event);
				AsyncTaskUtil.executeAsyncTask(loadEventDetails, true);
			}
		}
		
		return rootView;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if (friendsRVAdapter == null) {
			friendsRVAdapter = new FriendsRVAdapter(event.getFriends());
		} 
		
		recyclerVFriends.setAdapter(friendsRVAdapter);
	}
	
	@Override
	public void onStart() {
		//Log.d(TAG, "onStart()");

		if (!isOnTop()) {
			callOnlySuperOnStart = true;
			super.onStart();
			return;
		}
		
		super.onStart();
		
		((MainActivity) FragmentUtil.getActivity(this)).setVStatusBarVisibility(View.GONE, AppConstants.INVALID_ID);
		if (prevScrollY != UNSCROLLED) {
			onScrollChanged(prevScrollY, true);
			
			if (((MainActivity)FragmentUtil.getActivity(EventDetailsFragment.this)).isDrawerOpen()) {
				onDrawerOpened();
			}
		}
	}
	
	@Override
	public void onStop() {
		//Log.d(TAG, "onStop()");
		super.onStop();
		
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		ma.setToolbarBg(ma.getResources().getColor(R.color.colorPrimary));
		ma.setVStatusBarVisibility(View.VISIBLE, R.color.colorPrimaryDark);
		ma.setVStatusBarLayeredVisibility(View.GONE, AppConstants.INVALID_ID);
	}
	
	@Override
	public void onDestroyView() {
		/**
		 * Following call is required to prevent non-removal of onGlobalLayoutListener. If onGlobalLayout() 
		 * is not called yet & screen gets destroyed, then removal of onGlobalLayoutListener will not happen ever 
		 * since fragment won't be able to find its view tree observer. So, better to make sure
		 * that it gets removed at the end from onDestroyView()
		 */
		if (VersionUtil.isApiLevelAbove15()) {
			obsrScrlV.getViewTreeObserver().removeOnGlobalLayoutListener(onGlobalLayoutListener);

		} else {
			obsrScrlV.getViewTreeObserver().removeGlobalOnLayoutListener(onGlobalLayoutListener);
		}
		super.onDestroyView();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (loadEventDetails != null && loadEventDetails.getStatus() != Status.FINISHED) {
			loadEventDetails.cancel(true);
		}
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_event_details, menu);
		
		MenuItem item = (MenuItem) menu.findItem(R.id.action_share);
	    mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
	    updateShareIntent();
	    
    	super.onCreateOptionsMenu(menu, inflater);
	}
	
	private void updateShareIntent() {
		//Log.d(TAG, "updateShareIntent(), event name = " + event.getName());
	    if (mShareActionProvider != null && event != null) {
	    	Intent shareIntent = new Intent(Intent.ACTION_SEND);
		    shareIntent.setType("image/*");
		    shareIntent.putExtra(Intent.EXTRA_SUBJECT, FragmentUtil.getResources(this).getString(
		    		R.string.title_event_details));
		    String message = "Checkout " + event.getName();
		    if (event.getSchedule() != null && event.getSchedule().getVenue() != null) {
		    	message += " @ " + event.getSchedule().getVenue().getName();
		    }
		    if (event.getEventUrl() != null) {
		    	message += ": " + event.getEventUrl();
		    }
		    shareIntent.putExtra(Intent.EXTRA_TEXT, message);
			
			String key = event.getKey(ImgResolution.LOW);
	        BitmapCache bitmapCache = BitmapCache.getInstance();
			Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
			if (bitmap != null) {
				File tmpFile = FileUtil.createTempShareImgFile(FragmentUtil.getActivity(this).getApplication(), bitmap);
				if (tmpFile != null) {
					shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(tmpFile));
				}
			}
		    
	        mShareActionProvider.setShareIntent(shareIntent);
	        
	        mShareActionProvider.setOnShareTargetSelectedListener(onShareTargetSelectedListener);
	    }
	}
	
	private void calculateDimensions() {
		DisplayMetrics dm = new DisplayMetrics();
		FragmentUtil.getActivity(this).getWindowManager().getDefaultDisplay().getMetrics(dm);
		screenW = dm.widthPixels;
		
		Resources res = FragmentUtil.getResources(this);
		imgEventHt = res.getDimensionPixelSize(R.dimen.img_event_ht_event_details);
	}
	
	@Override
	public void animateSharedElements() {
		//Log.d(TAG, "animateSharedElements()");
		SharedElement sharedElement = sharedElements.get(0);
		
		//ViewCompat.setTransitionName(imgEvent, sharedElement.getTransitionName());
		
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
                
                /*lp = (LayoutParams) rltLytTxtEvtTitle.getLayoutParams();
                lp.topMargin = rltLytTxtEvtTitleMarginT * progress.intValue() / 100;
                rltLytTxtEvtTitle.setLayoutParams(lp);*/
                
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
				((MainActivity)FragmentUtil.getActivity(EventDetailsFragment.this)).onSharedElementAnimStart();
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
							loadEventDetails = new LoadEventDetails(Api.OAUTH_TOKEN, EventDetailsFragment.this, 
									EventDetailsFragment.this, event);
							AsyncTaskUtil.executeAsyncTask(loadEventDetails, true);
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
		        
		        ObjectAnimator yAnim = ObjectAnimator.ofFloat(imgEvent, "y", 0, sharedElementPosition.getStartY() + prevScrollY);
		        yAnim.setDuration(TRANSITION_ANIM_DURATION);
		        
		        ValueAnimator va = ValueAnimator.ofInt(100, 1);
		        va.setDuration(TRANSITION_ANIM_DURATION);
		        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
		        	
		        	private int color = FragmentUtil.getResources(EventDetailsFragment.this).getColor(android.R.color.white);
		        	
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
		imgDown.setImageDrawable(FragmentUtil.getResources(this).getDrawable(R.drawable.ic_description_collapse));

		isEvtDescExpanded = true;
	}
	
	private void collapseEvtDesc() {
		txtEvtDesc.setVisibility(View.GONE);
		imgDown.setImageDrawable(FragmentUtil.getResources(this).getDrawable(R.drawable.ic_description_expand));
		
		isEvtDescExpanded = false;
	}
	
	private void updateEventImg() {
		//Log.d(TAG, "updateEventImg(), url = " + event.getLowResImgUrl());
		if (event.doesValidImgUrlExist()) {
			//Log.d(TAG, "updateEventImg(), ValidImgUrlExist");
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
			/**
			 * set again to gone since it won't be set from updateDetailsVisibility() in case user has moved to
			 * say tickets screen & comes back to this. 
			 */
			rltLytVenue.setVisibility(View.GONE);
			
		} else {
			rltLytVenue.setVisibility(View.VISIBLE);
			AddressMapFragment fragment = (AddressMapFragment) getChildFragmentManager().findFragmentByTag(
					AppConstants.FRAGMENT_TAG_ADDRESS_MAP);
	        if (fragment == null) {
	        	addAddressMapFragment();
	        }
		}
	}
	
	private void updateFriendsVisibility() {
		if (!event.getFriends().isEmpty()) {
			//Log.d(TAG, "event.getFriends() = " + event.getFriends());
			rltLytFriends.setVisibility(View.VISIBLE);
			friendsRVAdapter.notifyDataSetChanged();
			
		} else {
			rltLytFriends.setVisibility(View.GONE);
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
	
	private void updateFabs() {
		fabTickets.setVisibility(View.VISIBLE);
		fabSave.setVisibility(View.VISIBLE);
		
		final Resources res = FragmentUtil.getResources(this);
		if (event.getSchedule() == null || event.getSchedule().getBookingInfos().isEmpty()) {
			fabTickets.setImageDrawable(res.getDrawable(R.drawable.ic_ticket_unavailable_floating));
			fabTickets.setEnabled(false);
			
		} else {
			fabTickets.setImageDrawable(res.getDrawable(R.drawable.ic_ticket_available_floating));
			fabTickets.setEnabled(true);
		}
		
		updateFabSaveSrc(res);
	}
	
	private void updateFabSaveSrc(Resources res) {
		//Log.d(TAG, "AT issue event.getAttending() = " + event.getAttending().getValue());
		int drawableId = (event.getAttending() == Attending.SAVED) ? R.drawable.ic_saved_event_floating 
				: R.drawable.ic_unsaved_event_floating;
		fabSave.setImageDrawable(res.getDrawable(drawableId));
	}
	
	private void updateDetailsVisibility() {
		if (allDetailsLoaded) {
			updateShareIntent();
			
			rltLytPrgsBar.setVisibility(View.GONE);
			updateEventImg();
			updateFeaturingVisibility();
			updateEventSchedule();
			updateAddressMapVisibility();
			updateFriendsVisibility();
			updateFabs();
			
		} else {
			rltLytPrgsBar.setVisibility(View.VISIBLE);
			rltLytFeaturing.setVisibility(View.GONE);
			rltLytVenue.setVisibility(View.GONE);
			rltLytFriends.setVisibility(View.GONE);
			fabTickets.setVisibility(View.GONE);
			fabSave.setVisibility(View.GONE);
		}
	}
	
	private void calculateScrollLimit() {
		Resources res = FragmentUtil.getResources(this);
		limitScrollAt = res.getDimensionPixelSize(R.dimen.img_event_ht_event_details) - res.getDimensionPixelSize(
				R.dimen.action_bar_ht);
		
		if (VersionUtil.isApiLevelAbove18()) {
			limitScrollAt -= ViewUtil.getStatusBarHeight(FragmentUtil.getResources(this));
		}
		
		int actionBarTitleTextSize = res.getDimensionPixelSize(R.dimen.abc_text_size_title_material_toolbar);
		int txtEvtTitleTextSize = res.getDimensionPixelSize(R.dimen.txt_evt_title_txt_size_event_details);
		minTitleScale = actionBarTitleTextSize / (float) txtEvtTitleTextSize;
	}
	
	private void onScrollChanged(int scrollY, boolean forceUpdate) {
		//Log.d(TAG, "scrollY = " + scrollY);
		// Translate image
		ViewHelper.setTranslationY(imgEvent, scrollY / 2);
        
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		
		if (limitScrollAt == 0) {
			calculateScrollLimit();
		}
		
		if ((!isScrollLimitReached || forceUpdate) && scrollY >= limitScrollAt) {
			//Log.d(TAG, "if");
			ma.animateToolbarElevation(0.0f, actionBarElevation);
			
			ma.setVStatusBarLayeredVisibility(View.VISIBLE, R.color.colorPrimaryDark);
			ma.setToolbarBg(ma.getResources().getColor(R.color.colorPrimary));
			
			title = event.getName();
			ma.updateTitle(title);
			
			isScrollLimitReached = true;
			
		} else if ((isScrollLimitReached || forceUpdate) && scrollY < limitScrollAt) {
			//Log.d(TAG, "else if");
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
        
		boolean isSignificantDelta = Math.abs(scrollY - prevScrollY) > fabScrollThreshold;
        if (isSignificantDelta) {
            if (scrollY > prevScrollY) {
                fabTickets.hide(true);
                fabSave.hide(true);
                
            } else {
                fabTickets.show(true);
                fabSave.show(true);
            }
        }
        
		prevScrollY = scrollY;
	}
	
	public String getCurrentTitle() {
		return title;
	}
	
	private void onDrawerOpened() {
		//Log.d(TAG, "onDrawerOpened()");
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		ma.setToolbarBg(ma.getResources().getColor(R.color.colorPrimary));
		ma.setToolbarElevation(ma.getResources().getDimensionPixelSize(R.dimen.action_bar_elevation));
		ma.setVStatusBarLayeredVisibility(View.VISIBLE, R.color.colorPrimaryDark);
		title = event.getName();
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
		case R.id.txtVenue:
			((VenueListener)FragmentUtil.getActivity(this)).onVenueSelected(event.getSchedule().getVenue());
			break;
			
		case R.id.imgDown:
			if (isEvtDescExpanded) {
				collapseEvtDesc();
				
			} else {
				expandEvtDesc();
			}
			break;
			
		case R.id.fabTickets:
			Bundle args = new Bundle();
			args.putString(BundleKeys.URL, event.getSchedule().getBookingInfos().get(0).getBookingUrl());
			((ReplaceFragmentListener)FragmentUtil.getActivity(this)).replaceByFragment(
					AppConstants.FRAGMENT_TAG_WEB_VIEW, args);
			/**
			 * added on 15-12-2014
			 */
			GoogleAnalyticsTracker.getInstance().sendEvent(FragmentUtil.getApplication(this), 
					getScreenName(), GoogleAnalyticsTracker.EVENT_LABEL_TICKETS_BUTTON, 
					GoogleAnalyticsTracker.Type.Event.name(), null, event.getId());
			break;
			
		case R.id.fabSave:
			EventSeekr eventSeekr = (EventSeekr) FragmentUtil.getApplication(this);
			if (event.getAttending() == Attending.SAVED) {
				event.setAttending(Attending.NOT_GOING);
				new UserTracker(Api.OAUTH_TOKEN, eventSeekr, UserTrackingItemType.event, event.getId(), 
						event.getAttending().getValue(), UserTrackingType.Add).execute();
    			updateFabSaveSrc(FragmentUtil.getResources(this));
				
			} else {
				if (eventSeekr.getGPlusUserId() != null) {
					event.setNewAttending(Attending.SAVED);
					handlePublishEvent();
					
				} else {
					fbCallCountForSameEvt = 0;
					event.setNewAttending(Attending.SAVED);
					FbUtil.handlePublishEvent(this, this, AppConstants.PERMISSIONS_FB_PUBLISH_EVT_OR_ART, 
							AppConstants.REQ_CODE_FB_PUBLISH_EVT_OR_ART, event);
				}
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
	
	@Override
	public void onPoppedFromBackStack() {
		if (isOnPushedToBackStackCalled) {
			isOnPushedToBackStackCalled = false;
			
			// to update statusbar visibility
			onStart();
			// to call onFragmentResumed(Fragment) of MainActivity (to update title, current fragment tag, etc.)
			onResume();
			
			for (Iterator<View> iterator = hiddenViews.iterator(); iterator.hasNext();) {
				View view = iterator.next();
				view.setVisibility(View.VISIBLE);
			}
			hiddenViews.clear();
			
			if (mShareActionProvider != null) {
				mShareActionProvider.setOnShareTargetSelectedListener(onShareTargetSelectedListener);
			}
			setMenuVisibility(true);
		}
	}

	@Override
	public void onPushedToBackStack() {
		/**
		 * to remove facebook callback. Not calling onStop() to prevent toolbar color changes occurring in between
		 * the transition
		 */
		super.onStop();
		
		/**
		 * set null listener, otherwise even for artist/venue details screen when selecting 
		 * "add to calendar" option it calls this listener's onShareTargetSelected() method which in turn 
		 * sets eventToAddToCalendar on EventSeekr class. This results in sharing event wrongly from 
		 * artist/venue details screen.
		 */
		if (mShareActionProvider != null) {
			mShareActionProvider.setOnShareTargetSelectedListener(null);
		}
		//Log.d("MainActivity - event details", "onPushedToBackStack(), " + this);
		setMenuVisibility(false);
		isOnPushedToBackStackCalled = true;
	}

	@Override
	public void addViewsToBeHidden(View... views) {
		for (int i = 0; i < views.length; i++) {
			hiddenViews.add(views[i]);
		}
	}

	@Override
	public void hideSharedElements() {
		for (Iterator<View> iterator = hiddenViews.iterator(); iterator.hasNext();) {
			View view = iterator.next();
			view.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	public void onPublishPermissionGranted() {
		updateFabSaveSrc(FragmentUtil.getResources(this));
	}

	@Override
	public void call(Session session, SessionState state, Exception exception) {
		fbCallCountForSameEvt++;
		/**
		 * To prevent infinite loop when network is off & we are calling requestPublishPermissions() of FbUtil.
		 */
		if (fbCallCountForSameEvt < AppConstants.MAX_FB_CALL_COUNT_FOR_SAME_EVT_OR_ART) {
			FbUtil.call(session, state, exception, this, this, AppConstants.PERMISSIONS_FB_PUBLISH_EVT_OR_ART, 
					AppConstants.REQ_CODE_FB_PUBLISH_EVT_OR_ART, event);
			
		} else {
			fbCallCountForSameEvt = 0;
			setPendingAnnounce(false);
		}
	}

	@Override
	public boolean isOnTop() {
		return !isOnPushedToBackStackCalled;
	}
}
