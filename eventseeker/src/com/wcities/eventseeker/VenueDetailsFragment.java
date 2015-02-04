package com.wcities.eventseeker;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.ShareActionProvider.OnShareTargetSelectedListener;
import android.text.Html;
import android.text.TextUtils.TruncateAt;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.Session;
import com.facebook.SessionState;
import com.melnykov.fab.FloatingActionButton;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.Animator.AnimatorListener;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.view.ViewHelper;
import com.wcities.eventseeker.analytics.GoogleAnalyticsTracker;
import com.wcities.eventseeker.analytics.GoogleAnalyticsTracker.Type;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingItemType;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.asynctask.LoadEvents;
import com.wcities.eventseeker.asynctask.LoadEvents.LoadEventsTaskListener;
import com.wcities.eventseeker.asynctask.LoadVenueDetails;
import com.wcities.eventseeker.asynctask.AsyncLoadImg.AsyncLoadImageListener;
import com.wcities.eventseeker.asynctask.LoadVenueDetails.OnVenueUpdatedListener;
import com.wcities.eventseeker.asynctask.UserTracker;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Date;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Event.Attending;
import com.wcities.eventseeker.core.Schedule;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.custom.fragment.PublishEventFragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.CustomSharedElementTransitionDestination;
import com.wcities.eventseeker.interfaces.CustomSharedElementTransitionSource;
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener;
import com.wcities.eventseeker.interfaces.EventListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.interfaces.ReplaceFragmentListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.FbUtil;
import com.wcities.eventseeker.util.FileUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.VersionUtil;
import com.wcities.eventseeker.util.ViewUtil;
import com.wcities.eventseeker.viewdata.SharedElement;
import com.wcities.eventseeker.viewdata.SharedElementPosition;

public class VenueDetailsFragment extends PublishEventFragmentLoadableFromBackStack implements DrawerListener, 
		CustomSharedElementTransitionDestination, OnVenueUpdatedListener, LoadItemsInBackgroundListener, 
		CustomSharedElementTransitionSource, AsyncLoadImageListener, LoadEventsTaskListener {

	private static final String TAG = VenueDetailsFragment.class.getSimpleName();
	
	private static final int UNSCROLLED = -1;
	private static final int TRANSITION_ANIM_DURATION = 400;
	private static final int TRANSLATION_Z_DP = 10;
			
	private Venue venue;
	private LoadVenueDetails loadVenueDetails;
	private LoadEvents loadEvents;
	private boolean allDetailsLoaded;
	private List<Event> eventList;
	
	private int totalScrolledDy = UNSCROLLED; // indicates layout not yet created
	private int limitScrollAt, actionBarElevation;
	private float minTitleScale;
	private boolean isScrollLimitReached, isDrawerOpen;
	private String title = "";
	private float translationZPx;
	
	private List<SharedElement> sharedElements;
	private boolean isOnCreateViewCalledFirstTime = true;
	private int screenW, imgVenueHt;
	private AnimatorSet animatorSet;
	
	private View rootView;
	private ImageView imgVenue;
	private TextView txtVenueTitle;
	private RecyclerView recyclerVVenues;
	private RelativeLayout rltLytTxtVenueTitle;
	
	private VenueRVAdapter venueRVAdapter;
	
	private int imgEventPadL, imgEventPadR, imgEventPadT, imgEventPadB;
	private List<View> hiddenViews;
	
	private Handler handler;
	
	private ShareActionProvider mShareActionProvider;
	
	private OnShareTargetSelectedListener onShareTargetSelectedListener = new OnShareTargetSelectedListener() {
		
		@Override
		public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
			String shareTarget = intent.getComponent().getPackageName();
			GoogleAnalyticsTracker.getInstance().sendShareEvent(FragmentUtil.getApplication(VenueDetailsFragment.this), 
					getScreenName(), shareTarget, Type.Venue, venue.getId());
			return false;
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setHasOptionsMenu(true);
		setRetainInstance(true);
		
		Bundle args = getArguments();
		if (args.containsKey(BundleKeys.SHARED_ELEMENTS)) {
			sharedElements = (List<SharedElement>) args.getSerializable(BundleKeys.SHARED_ELEMENTS);
		}
		
		if (venue == null) {
			//Log.d(TAG, "event = null");
			venue = (Venue) args.getSerializable(BundleKeys.VENUE);
			//updateShareIntent();
		}
		
		Resources res = FragmentUtil.getResources(this);
		translationZPx = ConversionUtil.toPx(res, TRANSLATION_Z_DP);
		handler = new Handler(Looper.getMainLooper());
		
		hiddenViews = new ArrayList<View>();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		/**
		 * on orientation change we need to recalculate this due to different values of 
		 * action_bar_ht on both orientations
		 */
		calculateScrollLimit();
		calculateDimensions();
		
		rootView = inflater.inflate(R.layout.fragment_venue_details, container, false);
		
		imgVenue = (ImageView) rootView.findViewById(R.id.imgVenue);
		updateVenueImg();
		
		rltLytTxtVenueTitle = (RelativeLayout) rootView.findViewById(R.id.rltLytTxtVenueTitle);
		
		txtVenueTitle = (TextView) rootView.findViewById(R.id.txtVenueTitle);
		txtVenueTitle.setText(venue.getName());
		// for marquee to work
		txtVenueTitle.setSelected(true);
		
		recyclerVVenues = (RecyclerView) rootView.findViewById(R.id.recyclerVVenues);
		// use a linear layout manager
		RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(FragmentUtil.getActivity(this));
		recyclerVVenues.setLayoutManager(layoutManager);
		
		recyclerVVenues.setOnScrollListener(new RecyclerView.OnScrollListener() {
	    	
	    	@Override
	    	public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
	    		super.onScrolled(recyclerView, dx, dy);
	    		//Log.d(TAG, "onScrolled - dx = " + dx + ", dy = " + dy);
	    		VenueDetailsFragment.this.onScrolled(dy, false);
	    	}
		});
		
		recyclerVVenues.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
            	//Log.d(TAG, "onGlobalLayout()");
				if (VersionUtil.isApiLevelAbove15()) {
					recyclerVVenues.getViewTreeObserver().removeOnGlobalLayoutListener(this);

				} else {
					recyclerVVenues.getViewTreeObserver().removeGlobalOnLayoutListener(this);
				}
				
				onScrolled(0, true);
				if (isDrawerOpen) {
					// to maintain status bar & toolbar decorations after orientation change
					onDrawerOpened();
				}
            }
        });
		
		if (isOnCreateViewCalledFirstTime) {
			isOnCreateViewCalledFirstTime = false;
			
			if (sharedElements != null) {
				animateSharedElements();
				
			} else {
				rootView.setBackgroundColor(Color.WHITE);
				loadVenueDetails = new LoadVenueDetails(Api.OAUTH_TOKEN, venue, this);
				AsyncTaskUtil.executeAsyncTask(loadVenueDetails, true);
			}
		}
		return rootView;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (venueRVAdapter == null) {
			eventList = new ArrayList<Event>();
			eventList.add(null);
			
			venueRVAdapter = new VenueRVAdapter(this, eventList, null, venue);
			
		} else {
			venueRVAdapter.onActivityCreated();
		}
		recyclerVVenues.setAdapter(venueRVAdapter);
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
		
		if (loadVenueDetails != null && loadVenueDetails.getStatus() != Status.FINISHED) {
			loadVenueDetails.cancel(true);
		}
		if (loadEvents != null && loadEvents.getStatus() != Status.FINISHED) {
			loadEvents.cancel(true);
		}
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_venue_details, menu);
		
		MenuItem item = (MenuItem) menu.findItem(R.id.action_share);
	    mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
	    updateShareIntent();
	    
    	super.onCreateOptionsMenu(menu, inflater);
	}
	
	protected void updateShareIntent() {
	    if (mShareActionProvider != null && venue != null) {
	    	Intent shareIntent = new Intent(Intent.ACTION_SEND);
		    shareIntent.setType("image/*");
		    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Venue Details");
		    shareIntent.putExtra(Intent.EXTRA_TEXT, venue.getName());
			
			String key = venue.getKey(ImgResolution.LOW);
	        BitmapCache bitmapCache = BitmapCache.getInstance();
			Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
			if (bitmap != null) {
				//Log.d(TAG, "bitmap != null");
				File tmpFile = FileUtil.createTempShareImgFile(FragmentUtil.getActivity(this).getApplication(), bitmap);
				if (tmpFile != null) {
					shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(tmpFile));
				}
			}
		    
	        mShareActionProvider.setShareIntent(shareIntent);
	        
	        mShareActionProvider.setOnShareTargetSelectedListener(onShareTargetSelectedListener);
	    }
	}
	
	private void onScrolled(int dy, boolean forceUpdate) {
		//Log.d(TAG, "dy = " + dy);
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		
		if (totalScrolledDy == UNSCROLLED) {
			totalScrolledDy = 0;
		}
		totalScrolledDy += dy;
		
		/**
		 * this is required to prevent changes in scrolled value due to automatic corrections in recyclerview size
		 * e.g.: 1) Due to event loading progressbar returning no events resulting in reduction of overall size
		 * & hence totalScrolledDy value must be changed but we don't have good way to calculate it & hence
		 * just update it to right value when position is 0 (when we are sure about exact totalScrolledDy value)
		 * It's actually needed for changing toolbar color which we do only when 1st visible position is 0.
		 * 2) When screen becomes scrollable after expanding description but not on collapsing, resulting in 
		 * automatic scroll to settle recyclerview.
		 */
		if (((LinearLayoutManager)recyclerVVenues.getLayoutManager()).findFirstVisibleItemPosition() == 0) {
			totalScrolledDy = -recyclerVVenues.getLayoutManager().findViewByPosition(0).getTop();
			//Log.d(TAG, "totalScrolledDy corrected = " + totalScrolledDy);
		}
		
		// Translate image
		ViewHelper.setTranslationY(imgVenue, (0 - totalScrolledDy) / 2);
		
		if (limitScrollAt == 0) {
			calculateScrollLimit();
			//Log.d(TAG, "vPagerCatTitles.getTop() = " + vPagerCatTitles.getTop() + ", toolbarSize = " + toolbarSize + ", ma.getStatusBarHeight() = " + ma.getStatusBarHeight());
		}
		
		int scrollY = (totalScrolledDy >= limitScrollAt) ? limitScrollAt : totalScrolledDy;
		//Log.d(TAG, "totalScrolledDy = " + totalScrolledDy + ", limitScrollAt = " + limitScrollAt + ", scrollY = " + scrollY);
		
		ViewHelper.setTranslationY(rltLytTxtVenueTitle, -totalScrolledDy);
		
		if ((!isScrollLimitReached || forceUpdate) && totalScrolledDy >= limitScrollAt) {
			ma.animateToolbarElevation(0.0f, actionBarElevation);
			
			ma.setVStatusBarLayeredVisibility(View.VISIBLE, R.color.colorPrimaryDark);
			ma.setToolbarBg(ma.getResources().getColor(R.color.colorPrimary));
			
			title = venue.getName();
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
			
			ViewHelper.setPivotX(txtVenueTitle, 0);
	        ViewHelper.setPivotY(txtVenueTitle, txtVenueTitle.getHeight() / 2);
	        ViewHelper.setScaleX(txtVenueTitle, scale);
	        ViewHelper.setScaleY(txtVenueTitle, scale);
		}
	}
	
	private void calculateScrollLimit() {
		Resources res = FragmentUtil.getResources(this);
		limitScrollAt = res.getDimensionPixelSize(R.dimen.img_venue_ht_venue_details) - res.getDimensionPixelSize(
				R.dimen.action_bar_ht);
		
		if (VersionUtil.isApiLevelAbove18()) {
			limitScrollAt -= ViewUtil.getStatusBarHeight(FragmentUtil.getResources(this));
		}
		
		int actionBarTitleTextSize = res.getDimensionPixelSize(R.dimen.abc_text_size_title_material_toolbar);
		int txtVenueTitleTextSize = res.getDimensionPixelSize(R.dimen.txt_venue_title_txt_size_venue_details);
		minTitleScale = actionBarTitleTextSize / (float) txtVenueTitleTextSize;
	}
	
	private void calculateDimensions() {
		DisplayMetrics dm = new DisplayMetrics();
		FragmentUtil.getActivity(this).getWindowManager().getDefaultDisplay().getMetrics(dm);
		screenW = dm.widthPixels;
		
		Resources res = FragmentUtil.getResources(this);
		imgVenueHt = res.getDimensionPixelSize(R.dimen.img_venue_ht_venue_details);
		
		imgEventPadL = res.getDimensionPixelSize(R.dimen.img_event_pad_l_list_item_discover);
		imgEventPadR = res.getDimensionPixelSize(R.dimen.img_event_pad_r_list_item_discover);
		imgEventPadT = res.getDimensionPixelSize(R.dimen.img_event_pad_t_list_item_discover);
		imgEventPadB = res.getDimensionPixelSize(R.dimen.img_event_pad_b_list_item_discover);
	}
	
	private void updateVenueImg() {
		//Log.d(TAG, "updateEventImg(), url = " + event.getLowResImgUrl());
		if (venue.doesValidImgUrlExist()) {
			String key = venue.getKey(ImgResolution.LOW);
	        BitmapCache bitmapCache = BitmapCache.getInstance();
			Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
			if (bitmap != null) {
		        imgVenue.setImageBitmap(bitmap);
		        
		    } else {
		    	imgVenue.setImageBitmap(null);
		    	AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
		        asyncLoadImg.loadImg(imgVenue, ImgResolution.LOW, venue, this);
		    }
		}
	}
	
	private void onDrawerOpened() {
		isDrawerOpen = true;
		
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		ma.setToolbarBg(ma.getResources().getColor(R.color.colorPrimary));
		ma.setToolbarElevation(ma.getResources().getDimensionPixelSize(R.dimen.action_bar_elevation));
		ma.setVStatusBarLayeredVisibility(View.VISIBLE, R.color.colorPrimaryDark);
		title = venue.getName();
		ma.updateTitle(title);
	}
	
	public String getCurrentTitle() {
		return title;
	}
	
	@Override
	public String getScreenName() {
		return "Venue Detail Screen";
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
		
        ObjectAnimator xAnim = ObjectAnimator.ofFloat(imgVenue, "x", sharedElementPosition.getStartX(), 0);
        xAnim.setDuration(TRANSITION_ANIM_DURATION);
        
        ObjectAnimator yAnim = ObjectAnimator.ofFloat(imgVenue, "y", sharedElementPosition.getStartY(), 0);
        yAnim.setDuration(TRANSITION_ANIM_DURATION);
        
        ValueAnimator va = ValueAnimator.ofInt(1, 100);
        va.setDuration(TRANSITION_ANIM_DURATION);
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
        	
            int color = FragmentUtil.getResources(VenueDetailsFragment.this).getColor(android.R.color.white);
            
            public void onAnimationUpdate(ValueAnimator animation) {
                Integer progress = (Integer) animation.getAnimatedValue();

                RelativeLayout.LayoutParams lp = (LayoutParams) imgVenue.getLayoutParams();
                lp.width = (int) (sharedElementPosition.getWidth() + 
                		(((screenW - sharedElementPosition.getWidth()) * progress.intValue()) / 100));
                lp.height = (int) (sharedElementPosition.getHeight() + 
                		(((imgVenueHt - sharedElementPosition.getHeight()) * progress.intValue()) / 100));
                imgVenue.setLayoutParams(lp);
                
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
				recyclerVVenues.setVisibility(View.INVISIBLE);
				((MainActivity)FragmentUtil.getActivity(VenueDetailsFragment.this)).onSharedElementAnimStart();
			}
			
			@Override
			public void onAnimationRepeat(Animator arg0) {}
			
			@Override
			public void onAnimationEnd(Animator arg0) {
				//Log.d(TAG, "onAnimationEnd()");
				if (!isCancelled) {
					//Log.d(TAG, "!isCancelled");
					recyclerVVenues.setVisibility(View.VISIBLE);
					Animation slideInFromBottom = AnimationUtils.loadAnimation(FragmentUtil.getApplication(
							VenueDetailsFragment.this), R.anim.slide_in_from_bottom);
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
							loadVenueDetails = new LoadVenueDetails(Api.OAUTH_TOKEN, venue, VenueDetailsFragment.this);
							AsyncTaskUtil.executeAsyncTask(loadVenueDetails, true);
						}
					});
					recyclerVVenues.startAnimation(slideInFromBottom);
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
		recyclerVVenues.clearAnimation();
		
		Animation slideOutToBottom = AnimationUtils.loadAnimation(FragmentUtil.getApplication(
				VenueDetailsFragment.this), R.anim.slide_out_to_bottom);
		slideOutToBottom.setAnimationListener(new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {}
			
			@Override
			public void onAnimationRepeat(Animation animation) {}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				recyclerVVenues.setVisibility(View.INVISIBLE);
				
				animatorSet = new AnimatorSet();
				
				SharedElement sharedElement = sharedElements.get(0);
		        
				final SharedElementPosition sharedElementPosition = sharedElement.getSharedElementPosition();
		        ObjectAnimator xAnim = ObjectAnimator.ofFloat(imgVenue, "x", 0, sharedElementPosition.getStartX());
		        xAnim.setDuration(TRANSITION_ANIM_DURATION);
		        
		        ObjectAnimator yAnim = ObjectAnimator.ofFloat(imgVenue, "y", 0, sharedElementPosition.getStartY());
		        yAnim.setDuration(TRANSITION_ANIM_DURATION);
		        
		        ValueAnimator va = ValueAnimator.ofInt(100, 1);
		        va.setDuration(TRANSITION_ANIM_DURATION);
		        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
		        	
		        	int color = FragmentUtil.getResources(VenueDetailsFragment.this).getColor(android.R.color.white);
		        	
		            public void onAnimationUpdate(ValueAnimator animation) {
		                Integer value = (Integer) animation.getAnimatedValue();
		                imgVenue.getLayoutParams().width = (int) (sharedElementPosition.getWidth() + 
		                		(((screenW - sharedElementPosition.getWidth()) * value.intValue()) / 100));
		                imgVenue.getLayoutParams().height = (int) (sharedElementPosition.getHeight() + 
		                		(((imgVenueHt - sharedElementPosition.getHeight()) * value.intValue()) / 100));
		                imgVenue.requestLayout();
		                
		                int newAlpha = (int) (value * 2.55);
		                rootView.setBackgroundColor(Color.argb(newAlpha, Color.red(color), Color.green(color), 
		                		Color.blue(color)));
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
						FragmentUtil.getActivity(VenueDetailsFragment.this).onBackPressed();
					}
					
					@Override
					public void onAnimationCancel(Animator animation) {}
				});
		        animatorSet.start();
			}
		});
		/**
		 * set visible to finish this screen even if user presses back button instantly even before animateSharedElements()
		 * has finished it work; otherwise if recyclerVVenues is invisible, then user has to press back once more in such case
		 * on instantly clicking back
		 */
		recyclerVVenues.setVisibility(View.VISIBLE);
		recyclerVVenues.startAnimation(slideOutToBottom);
    }

	@Override
	public void onVenueUpdated() {
		allDetailsLoaded = true;
		updateVenueImg();
		venueRVAdapter.notifyDataSetChanged();
		updateShareIntent();
	}
	
	private static class VenueRVAdapter extends RecyclerView.Adapter<VenueRVAdapter.ViewHolder> implements 
			DateWiseEventParentAdapterListener {
		
		private static final String FRAGMENT_TAG_SHARE_VIA_DIALOG = ShareViaDialogFragment.class.getSimpleName();
		
		private static final int EXTRA_TOP_DUMMY_ITEM_COUNT = 2;
		private static final int EXTRA_TOP_DUMMY_ITEM_COUNT_AFTER_DETAILS_LOADED = 4;
		private static final int INVALID = -1;
		private static final int MAX_LINES_VENUE_DESC = 5;
		
		private RecyclerView recyclerView;
		private VenueDetailsFragment venueDetailsFragment;
		
		private boolean isVenueDescExpanded;
		private List<Event> eventList;
		private Venue venue;
		private LoadEvents loadEvents;
		private boolean isMoreDataAvailable = true;
		private int eventsAlreadyRequested;
		
		private int fbCallCountForSameEvt = 0;
		private VenueRVAdapter.ViewHolder holderPendingPublish;
		private Event eventPendingPublish;
		
		private int openPos = INVALID;
		private int rltLytContentInitialMarginL, lnrSliderContentW, imgEventW, rltLytContentW = INVALID;
		
		private BitmapCache bitmapCache;
		
		private static enum ViewType {
			IMG, DESC, ADDRESS_MAP, UPCOMING_EVENTS_TITLE, PROGRESS, EVENT;
			
			private static ViewType getViewType(int type) {
				ViewType[] viewTypes = ViewType.values();
				for (int i = 0; i < viewTypes.length; i++) {
					if (viewTypes[i].ordinal() == type) {
						return viewTypes[i];
					}
				}
				return null;
			}
		};
		
		private static class ViewHolder extends RecyclerView.ViewHolder {
			
			private TextView txtDesc;
			private ImageView imgDown;
			private RelativeLayout rltLytPrgsBar;
			private View vHorLine;
			
			private TextView txtVenue;
			private FloatingActionButton fabPhone, fabNavigate;
			
			// event item
			private View vHandle;
			private TextView txtEvtTitle, txtEvtTime, txtEvtLocation;
			private ImageView imgEvent, imgTicket, imgSave, imgShare;
	        private LinearLayout lnrSliderContent;
	        private RelativeLayout rltLytRoot, rltLytContent;

			public ViewHolder(View itemView) {
				super(itemView);
				
				txtDesc = (TextView) itemView.findViewById(R.id.txtDesc);
				imgDown = (ImageView) itemView.findViewById(R.id.imgDown);
				rltLytPrgsBar = (RelativeLayout) itemView.findViewById(R.id.rltLytPrgsBar);
				vHorLine = itemView.findViewById(R.id.vHorLine);
				
				txtVenue = (TextView) itemView.findViewById(R.id.txtVenue);
				fabPhone = (FloatingActionButton) itemView.findViewById(R.id.fabPhone);
				fabNavigate = (FloatingActionButton) itemView.findViewById(R.id.fabNavigate);
				
				// event item
				txtEvtTitle = (TextView) itemView.findViewById(R.id.txtEvtTitle);
	            txtEvtTime = (TextView) itemView.findViewById(R.id.txtEvtTime);
	            txtEvtLocation = (TextView) itemView.findViewById(R.id.txtEvtLocation);
	            imgEvent = (ImageView) itemView.findViewById(R.id.imgEvent);
	            vHandle = itemView.findViewById(R.id.vHandle);
	            lnrSliderContent = (LinearLayout) itemView.findViewById(R.id.lnrSliderContent);
	            rltLytRoot = (RelativeLayout) itemView.findViewById(R.id.rltLytRoot);
	            rltLytContent = (RelativeLayout) itemView.findViewById(R.id.rltLytContent);
	            imgTicket = (ImageView) itemView.findViewById(R.id.imgTicket);
	            imgSave = (ImageView) itemView.findViewById(R.id.imgSave);
	            imgShare = (ImageView) itemView.findViewById(R.id.imgShare);
			}
			
			private boolean isSliderClose(int rltLytContentInitialMarginL) {
	        	RelativeLayout.LayoutParams rltLytContentLP = (RelativeLayout.LayoutParams) rltLytContent.getLayoutParams();
				return (rltLytContentLP.leftMargin == rltLytContentInitialMarginL);
	        }
		}
		
		public VenueRVAdapter(VenueDetailsFragment venueDetailsFragment, List<Event> eventList, 
				LoadEvents loadEvents, Venue venue) {
			this.venueDetailsFragment = venueDetailsFragment;
			this.eventList = eventList;
			this.loadEvents = loadEvents;
			this.venue = venue;
			
			bitmapCache = BitmapCache.getInstance();
			
			Resources res = FragmentUtil.getResources(venueDetailsFragment);
			rltLytContentInitialMarginL = res.getDimensionPixelSize(R.dimen.rlt_lyt_content_margin_l_list_item_discover);
			lnrSliderContentW = res.getDimensionPixelSize(R.dimen.lnr_slider_content_w_list_item_discover);
			imgEventW = res.getDimensionPixelSize(R.dimen.img_event_w_list_item_discover);
		}

		@Override
		public int getItemCount() {
			return venueDetailsFragment.allDetailsLoaded ? (EXTRA_TOP_DUMMY_ITEM_COUNT_AFTER_DETAILS_LOADED + 
					eventList.size()) : EXTRA_TOP_DUMMY_ITEM_COUNT;
		}
		
		@Override
		public int getItemViewType(int position) {
			if (position == ViewType.IMG.ordinal()) {
				return ViewType.IMG.ordinal();
				
			} else if (position == ViewType.DESC.ordinal()) {
				return ViewType.DESC.ordinal();
				
			} else if (position == ViewType.ADDRESS_MAP.ordinal()) {
				return ViewType.ADDRESS_MAP.ordinal();
				
			} else if (position == ViewType.UPCOMING_EVENTS_TITLE.ordinal()) {
				return ViewType.UPCOMING_EVENTS_TITLE.ordinal();
				
			} else if (eventList.get(position - EXTRA_TOP_DUMMY_ITEM_COUNT_AFTER_DETAILS_LOADED) == null) {
				return ViewType.PROGRESS.ordinal();
				
			} else {
				return ViewType.EVENT.ordinal();
			}
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			//Log.d(TAG, "onCreateViewHolder(), viewType = " + viewType);
			View v;
			
			recyclerView = (RecyclerView) parent;
			
			switch (ViewType.getViewType(viewType)) {
			
			case IMG:
				v = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_item_img_venue_venue_details, 
						parent, false);
				break;
				
			case DESC:
				v = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_item_desc, parent, false);
				break;
				
			case ADDRESS_MAP:
				v = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_item_address_map_venue_details, 
						parent, false);
				break;
				
			case UPCOMING_EVENTS_TITLE:
				v = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_item_upcoming_events_title, 
						parent, false);
				break;
				
			case PROGRESS:
				v = LayoutInflater.from(parent.getContext()).inflate(R.layout.progress_bar_eventseeker_fixed_ht, parent, 
						false);
				break;
				
			case EVENT:
				v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_discover, parent, false);
				break;
				
			default:
				v = null;
				break;
			}
			
			ViewHolder vh = new ViewHolder(v);
	        return vh;
		}
		
		@Override
		public void onBindViewHolder(final ViewHolder holder, final int position) {
			if (position == ViewType.IMG.ordinal()) {
				// nothing to do
				
			} else if (position == ViewType.DESC.ordinal()) {
				updateDescVisibility(holder);
				
			} else if (position == ViewType.ADDRESS_MAP.ordinal()) {
				updateAddressMap(holder);
				
			} else if (position == ViewType.UPCOMING_EVENTS_TITLE.ordinal()) {
				if (eventList.isEmpty()) {
					setViewGone(holder);
				}
				
			} else {
				final Event event = eventList.get(position - EXTRA_TOP_DUMMY_ITEM_COUNT_AFTER_DETAILS_LOADED);
				if (event == null) {
					// progress indicator
					
					if ((loadEvents == null || loadEvents.getStatus() == Status.FINISHED) && 
							isMoreDataAvailable) {
						//Log.d(TAG, "onBindViewHolder(), pos = " + position);
						((LoadItemsInBackgroundListener) venueDetailsFragment).loadItemsInBackground();
					}
					
				} else {

					/**
					 * If user clicks on save & changes orientation before call to onPublishPermissionGranted(), 
					 * then we need to update holderPendingPublish with right holder pointer in new orientation
					 */
					if (eventPendingPublish == event) {
						holderPendingPublish = holder;
					}
					
					holder.txtEvtTitle.setText(event.getName());
					
					if (event.getSchedule() != null) {
						Schedule schedule = event.getSchedule();
						Date date = schedule.getDates().get(0);
						holder.txtEvtTime.setText(ConversionUtil.getDateTime(date.getStartDate(), date.isStartTimeAvailable()));
						
						String venueName = (schedule.getVenue() != null) ? schedule.getVenue().getName() : "";
						holder.txtEvtLocation.setText(venueName);
					}
					
					BitmapCacheable bitmapCacheable = null;
					/**
					 * added this try catch as if event will not have valid url and schedule object then
					 * the below line may cause NullPointerException. So, added the try-catch and added the
					 * null check for bitmapCacheable on following statements.
					 */
					try {
						bitmapCacheable = event.doesValidImgUrlExist() ? event : event.getSchedule().getVenue();
						
					} catch (NullPointerException e) {
						e.printStackTrace();
					}
					
					if (bitmapCacheable != null) {
						String key = bitmapCacheable.getKey(ImgResolution.LOW);
						Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
						if (bitmap != null) {
					        holder.imgEvent.setImageBitmap(bitmap);
					        
					    } else {
					    	holder.imgEvent.setImageBitmap(null);
					    	AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
					        asyncLoadImg.loadImg(holder.imgEvent, ImgResolution.LOW, recyclerView, position, bitmapCacheable);
					    }
					}
					
					final Resources res = FragmentUtil.getResources(venueDetailsFragment);
					if (event.getSchedule() == null || event.getSchedule().getBookingInfos().isEmpty()) {
						holder.imgTicket.setImageDrawable(res.getDrawable(R.drawable.tickets_disabled));
						holder.imgTicket.setEnabled(false);
						
					} else {
						holder.imgTicket.setImageDrawable(res.getDrawable(R.drawable.tic_blue));
						holder.imgTicket.setEnabled(true);
					}
					
					updateImgSaveSrc(holder, event, res);
					
					if (rltLytContentW == INVALID) {
						/**
						 * Setting global layout listener on rltLytRoot instead of rltLytContent, because 
						 * on nexus 5, when orientation changes from portrait to landscape onGlobalLayout() 
						 * of rltLytContent returns wrong width (126px less than actual width)
						 */
						holder.rltLytRoot.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
				            @Override
				            public void onGlobalLayout() {
								RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) holder.rltLytContent.getLayoutParams();

								/**
								 * Following condition is to prevent above mentioned situation for nexus 5, 
								 * where orientation change from portrait to landscape returns less width (by 126px)
								 * for first time even when global layout listener is set on rltLytRoot.
								 */
								if (lp.width != RelativeLayout.LayoutParams.MATCH_PARENT) {
									if (VersionUtil.isApiLevelAbove15()) {
										holder.rltLytRoot.getViewTreeObserver().removeOnGlobalLayoutListener(this);
	
									} else {
										holder.rltLytRoot.getViewTreeObserver().removeGlobalOnLayoutListener(this);
									}
								}
								
								rltLytContentW = lp.width = (holder.rltLytRoot.getWidth() - imgEventW);
								holder.rltLytContent.setLayoutParams(lp);
				            	/*Log.d(TAG, "onGlobalLayout(), rltLytContentW = " + rltLytContentW + 
				            			", holder.rltLytRoot.getWidth() = " + holder.rltLytRoot.getWidth());*/

								if (openPos == position) {
									/**
									 * Now since we know fixed height of rltLytContent, we can update its 
									 * left margin in layoutParams by calling openSlider() which was delayed 
									 * until now [by condition if (rltLytContentW != INVALID) at end of 
									 * onBindViewHolder()].
									 */
									openSlider(holder, position, false);
								}
				            }
				        });
						
					} else {
						RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) holder.rltLytContent.getLayoutParams();
						lp.width = rltLytContentW;
						//Log.d(TAG, "else, rltLytContentW = " + rltLytContentW);
						holder.rltLytContent.setLayoutParams(lp);
					}
					
					holder.rltLytRoot.setOnTouchListener(new OnTouchListener() {
						
						int MIN_SWIPE_DISTANCE_X = ConversionUtil.toPx(res, 50);
						int MAX_CLICK_DISTANCE = ConversionUtil.toPx(res, 4);
						int pointerX = 0, initX = 0, pointerY = 0, initY = 0;
						boolean isSliderOpenInititally;
						int actionMoveCount = 0;
						
						@Override
						public boolean onTouch(View v, MotionEvent mEvent) {
							RelativeLayout.LayoutParams rltLytContentLP = (RelativeLayout.LayoutParams) holder.rltLytContent.getLayoutParams();
							RelativeLayout.LayoutParams lnrSliderContentLP = (RelativeLayout.LayoutParams) holder.lnrSliderContent.getLayoutParams();
							
							switch (mEvent.getAction()) {
							
							case MotionEvent.ACTION_DOWN:
								//Log.d(TAG, "down, x = " + mEvent.getRawX() + ", y = " + mEvent.getRawY());
								initX = pointerX = (int) mEvent.getRawX();
								initY = pointerY = (int) mEvent.getRawY();
								isSliderOpenInititally = !holder.isSliderClose(rltLytContentInitialMarginL);
								actionMoveCount = 0;
								return true;
							
							case MotionEvent.ACTION_MOVE:
								//Log.d(TAG, "move");
								holder.rltLytRoot.setPressed(true);
								
								actionMoveCount++;
								holder.lnrSliderContent.setVisibility(View.VISIBLE);
								
								int newX = (int) mEvent.getRawX();
								int dx = newX - pointerX;
								
								int scrollX = rltLytContentLP.leftMargin - rltLytContentInitialMarginL + dx;
								//Log.d(TAG, "move, rltLytContentLP.leftMargin = " + rltLytContentLP.leftMargin + ", lnrDrawerContentW = " + lnrDrawerContentW);
								if (scrollX >= (0 - lnrSliderContentW) && scrollX <= 0) {
									ViewCompat.setElevation(holder.imgEvent, venueDetailsFragment.translationZPx);
									
									rltLytContentLP.leftMargin = rltLytContentInitialMarginL + scrollX;
									//Log.d(TAG, "onTouch(), ACTION_MOVE");
									holder.rltLytContent.setLayoutParams(rltLytContentLP);
									
									lnrSliderContentLP.rightMargin = rltLytContentInitialMarginL 
											- rltLytContentLP.leftMargin - lnrSliderContentW;
									holder.lnrSliderContent.setLayoutParams(lnrSliderContentLP);
									
									pointerX = newX;
								}
								pointerY = (int) mEvent.getRawY();
								break;
								
							case MotionEvent.ACTION_UP:
							case MotionEvent.ACTION_CANCEL:
								//Log.d(TAG, "up, action = " + mEvent.getAction() + ", x = " + mEvent.getRawX() + ", y = " + mEvent.getRawY());
								holder.rltLytRoot.setPressed(false);
								boolean isMinSwipeDistanceXTravelled = Math.abs(initX - pointerX) > MIN_SWIPE_DISTANCE_X;
								boolean isSwipedToOpen = (initX > pointerX);
								
								if (isMinSwipeDistanceXTravelled) {
									//Log.d(TAG, "isMinSwipeDistanceXTravelled");
									if (isSwipedToOpen) {
										//Log.d(TAG, "isSwipedToOpen");
										openSlider(holder, position, true);
										
									} else {
										//Log.d(TAG, "!isSwipedToOpen");
										closeSlider(holder, position, true);
									}

								} else {
									//Log.d(TAG, "!isMinSwipeDistanceXTravelled");
									if (isSliderOpenInititally) {
										//Log.d(TAG, "isSliderOpenInititally");
										openSlider(holder, position, true);
										
									} else {
										//Log.d(TAG, "!isSliderOpenInititally");
										closeSlider(holder, position, true);
									}
									
									// consider click event
									if (actionMoveCount <= 2) {
										if (mEvent.getAction() == MotionEvent.ACTION_CANCEL) {
											break;
										}
										
										if (Math.abs(initX - pointerX) > MAX_CLICK_DISTANCE || 
												Math.abs(initY - pointerY) > MAX_CLICK_DISTANCE) {
											break;
										}
										
										/**
										 * Handle click event.
										 * We do it here instead of implementing onClick listener because then onClick listener
										 * of child element would block onTouch event on its parent (rltLytRoot) 
										 * if this onTouch starts from such a child view 
										 */
										if (ViewUtil.isPointInsideView(mEvent.getRawX(), mEvent.getRawY(), holder.vHandle)) {
											onHandleClick(holder, position);

										} else if (openPos == position) { 
											/**
											 * above condition is required, because otherwise these 3 conditions
											 * prevent event click on these positions even if slider is closed
											 */
											if (holder.imgTicket.isEnabled() && ViewUtil.isPointInsideView(
													mEvent.getRawX(), mEvent.getRawY(), holder.imgTicket)) {
												onImgTicketClick(holder, event);
													
											} else if (ViewUtil.isPointInsideView(mEvent.getRawX(), mEvent.getRawY(), holder.imgSave)) {
												onImgSaveClick(holder, event);
												
											} else if (ViewUtil.isPointInsideView(mEvent.getRawX(), mEvent.getRawY(), holder.imgShare)) {
												onImgShareClick(holder, event);
												
											} else if (ViewUtil.isPointInsideView(mEvent.getRawX(), mEvent.getRawY(), holder.rltLytRoot)) {
												/**
												 * This block is added to consider row click as event click even when
												 * slider is open (openPos == position); otherwise it won't do anything 
												 * on clicking outside the slider when it's open
												 */
												onEventClick(holder, event, position);
											}
											
										} else if (ViewUtil.isPointInsideView(mEvent.getRawX(), mEvent.getRawY(), holder.rltLytRoot)) {
											onEventClick(holder, event, position);
										}
									}
								}
								
								break;
							}
							return true;
						}
					});
					
					if (rltLytContentW != INVALID) {
						/**
						 * If at this point we don't know rltLytContentW, it means onGlobalLayout() for 
						 * rltLytContent is not yet called up where we actually calculate rltLytContentW
						 * & update rltLytContent layoutParams to update width from match_parent to fixed 
						 * value rltLytContentW.
						 * In such case w/o above condition, openSlider() function will change rltLytContent
						 * layoutParams resulting in extended width due to negative left margin & 
						 * width being match_parent. Hence instead, call it from onGlobalLayout().
						 * e.g. - opening slider in portrait & changing to landscape results in handle 
						 * getting overlapped by slider due to extended width of rltLytContent
						 */
						if (openPos == position) {
							//Log.d(TAG, "openPos == " + position);
							openSlider(holder, position, false);
							
						} else {
							closeSlider(holder, position, false);
						}
					}
				}
			}
		}
		
		private void updateAddressMap(ViewHolder holder) {
			holder.txtVenue.setText(venue.getFormatedAddress(false));
			AddressMapFragment fragment = (AddressMapFragment) venueDetailsFragment.getChildFragmentManager()
					.findFragmentByTag(AppConstants.FRAGMENT_TAG_ADDRESS_MAP);
	        if (fragment == null) {
	        	addAddressMapFragment();
	        }
	        
	        holder.fabPhone.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					if (venue.getPhone() != null) {
						Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + venue.getPhone()));
						venueDetailsFragment.startActivity(Intent.createChooser(intent, "Call..."));
						
					} else {
						Toast.makeText(FragmentUtil.getActivity(venueDetailsFragment), R.string.phone_number_not_available, 
								Toast.LENGTH_SHORT).show();
					}
				}
			});
	        
	        holder.fabNavigate.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					/*AddressMapFragment fragment = (AddressMapFragment) venueDetailsFragment.getChildFragmentManager()
							.findFragmentByTag(AppConstants.FRAGMENT_TAG_ADDRESS_MAP);
			        if (fragment != null) {
			        	fragment.displayDrivingDirection();
			        }*/
					Intent intent = null;
					double lat, lon;
					if (venue.getAddress() != null) {
						lat = venue.getAddress().getLat();
						lon = venue.getAddress().getLon();
						
						/*intent = new Intent(android.content.Intent.ACTION_VIEW, 
							    Uri.parse("navicon://setPOI?ver=1.4&ll=" + lat + "," + lon 
							    		+ "&appName=pb6Nlvh1&title=" + venue.getName() 
							    		+ "&radKM=15&tel=" + venue.getPhone() + "&callURL=com.wcities.eventseekrapp://"));
						Log.d(TAG, "uri = " + Uri.parse("navicon://setPOI?ver=1.4&ll=" + lat + "," + lon 
					    		+ "&appName=pb6Nlvh1&title=" + venue.getName() 
					    		+ "&radKM=15&tel=" + venue.getPhone() + "&callURL=com.wcities.eventseeker"));*/
						
						/*intent = new Intent(android.content.Intent.ACTION_VIEW, 
							    Uri.parse("http://apps.scout.me/v1/driveto?dt=" + venue.getAddress().getAddress1() + "@" + 
							    		lat + ", " + lon + "&title=" + venue.getName() 
							    		+ "&token=6T5HI14ZzJdKRk-PUhWzT7Zn-enFiGsUYskrN5EnXENaQnBUE3GDalgi8SN0x2J4aTxvvZuTwDfGx9WHtdwmJeJpzFprUq79p4gf54Yiq9jM6wFwHaZSBp1k1AYtzdcfhlWvjLcKWCpqe9juykeaHSTsRr-cJde4uYeWGDSFerI"));
						Log.d(TAG, "uri = " + Uri.parse("http://apps.scout.me/v1/driveto?dt=" + venue.getAddress().getAddress1() + "@" + 
					    		lat + ", " + lon + "&title=" + venue.getName() 
					    		+ "&token=6T5HI14ZzJdKRk-PUhWzT7Zn-enFiGsUYskrN5EnXENaQnBUE3GDalgi8SN0x2J4aTxvvZuTwDfGx9WHtdwmJeJpzFprUq79p4gf54Yiq9jM6wFwHaZSBp1k1AYtzdcfhlWvjLcKWCpqe9juykeaHSTsRr-cJde4uYeWGDSFerI"));*/
						
						if (lat == 0 && lon == 0) {
							if (venue.getAddress().getAddress1() != null) {
								//Log.d(TAG, "fabNavigate address - " + venue.getAddress().getAddress1());
								intent = new Intent(android.content.Intent.ACTION_VIEW, 
									    Uri.parse("google.navigation:q=" + venue.getAddress().getAddress1()));
								
							} else if (venue.getAddress().getCity() != null) {
								//Log.d(TAG, "fabNavigate city - " + venue.getAddress().getCity());
								intent = new Intent(android.content.Intent.ACTION_VIEW, 
									    Uri.parse("google.navigation:q=" + venue.getAddress().getCity()));
								
							} else {
								//Log.d(TAG, "fabNavigate name - " + venue.getName());
								intent = new Intent(android.content.Intent.ACTION_VIEW, 
									    Uri.parse("google.navigation:q=" + venue.getName()));
							}
							
						} else {
							//Log.d(TAG, "fabNavigate lat, lon - " + lat + "," + lon);
							intent = new Intent(android.content.Intent.ACTION_VIEW, 
								    Uri.parse("google.navigation:q=" + lat + "," + lon));
						}
						
					} else {
						//Log.d(TAG, "fabNavigate name - " + venue.getName());
						intent = new Intent(android.content.Intent.ACTION_VIEW, 
							    Uri.parse("google.navigation:q=" + venue.getName()));
					}
					
					//intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
					venueDetailsFragment.startActivity(intent);
				}
			});
		}
		
		private void addAddressMapFragment() {
	    	FragmentManager fragmentManager = venueDetailsFragment.getChildFragmentManager();
	        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
	        
	        AddressMapFragment fragment = new AddressMapFragment();
	        Bundle args = new Bundle();
	        args.putSerializable(BundleKeys.VENUE, venue);
	        fragment.setArguments(args);
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
		
		private void updateDescVisibility(ViewHolder holder) {
			if (venueDetailsFragment.allDetailsLoaded) {
				if (venueDetailsFragment.venue.getLongDesc() != null) {
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
			holder.txtDesc.setText(Html.fromHtml(venueDetailsFragment.venue.getLongDesc()));
			holder.imgDown.setVisibility(View.VISIBLE);
			holder.imgDown.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					//Log.d(TAG, "totalScrolled  = " + holder.itemView.getTop());
					if (isVenueDescExpanded) {
						collapseVenueDesc(holder);
						
						/**
						 * update scrolled distance after collapse, because sometimes it can happen that view becamse scrollable only
						 * due to expanded description after which if user collapses it, then based on recyclerview
						 * height it automatically resettles itself such that recyclerview again becomes unscrollable.
						 * Accordingly we need to reset scrolled amount, artist img & title
						 */
						venueDetailsFragment.handler.postDelayed(new Runnable() {
							
							@Override
							public void run() {
								venueDetailsFragment.onScrolled(0, true);
							}
						}, 100);
						
					} else {
						expandVenueDesc(holder);
					}
					//Log.d(TAG, "totalScrolled after  = " + holder.itemView.getTop());
				}
			});
			
			if (isVenueDescExpanded) {
				expandVenueDesc(holder);
				
			} else {
				collapseVenueDesc(holder);
			}
		}
		
		private void expandVenueDesc(ViewHolder holder) {
			holder.txtDesc.setMaxLines(Integer.MAX_VALUE);
			holder.txtDesc.setEllipsize(null);
			holder.imgDown.setImageDrawable(FragmentUtil.getResources(venueDetailsFragment).getDrawable(
					R.drawable.less));
			isVenueDescExpanded = true;
		}
		
		private void collapseVenueDesc(ViewHolder holder) {
			holder.txtDesc.setMaxLines(MAX_LINES_VENUE_DESC);
			holder.txtDesc.setEllipsize(TruncateAt.END);
			holder.imgDown.setImageDrawable(FragmentUtil.getResources(venueDetailsFragment).getDrawable(
					R.drawable.down));
			isVenueDescExpanded = false;
		}
		
		private void setViewGone(ViewHolder holder) {
			RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) holder.itemView.getLayoutParams();
			lp.height = 0;
			holder.itemView.setLayoutParams(lp);
		}
		
		private void updateImgSaveSrc(ViewHolder holder, Event event, Resources res) {
			int drawableId = (event.getAttending() == Attending.SAVED) ? R.drawable.checked_blue : R.drawable.calendar;
			holder.imgSave.setImageDrawable(res.getDrawable(drawableId));
		}
		
		private void openSlider(ViewHolder holder, int position, boolean isUserInitiated) {
			ViewCompat.setElevation(holder.imgEvent, venueDetailsFragment.translationZPx);
			holder.lnrSliderContent.setVisibility(View.VISIBLE);
			
			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) holder.lnrSliderContent.getLayoutParams();
			lp.rightMargin = 0;
			holder.lnrSliderContent.setLayoutParams(lp);
			
			lp = (RelativeLayout.LayoutParams) holder.rltLytContent.getLayoutParams();
			lp.leftMargin = rltLytContentInitialMarginL - lnrSliderContentW;
			holder.rltLytContent.setLayoutParams(lp);
			//Log.d(TAG, "openSlider()");
			
			if (isUserInitiated) {
				updateOpenPos(position, recyclerView);
			}
		}
		
		private void closeSlider(ViewHolder holder, int position, boolean isUserInitiated) {
			holder.lnrSliderContent.setVisibility(View.INVISIBLE);
			
			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) holder.lnrSliderContent.getLayoutParams();
			lp.rightMargin = 0 - lnrSliderContentW;;
			holder.lnrSliderContent.setLayoutParams(lp);
			
			lp = (RelativeLayout.LayoutParams) holder.rltLytContent.getLayoutParams();
			lp.leftMargin = rltLytContentInitialMarginL;
			holder.rltLytContent.setLayoutParams(lp);
			//Log.d(TAG, "closeSlider()");
			
			ViewCompat.setElevation(holder.imgEvent, 0);
			
			if (isUserInitiated) {
				if (openPos == position) {
					/**
					 * If slider closed is the one which was open & not already closed.
					 * W/o this condition if user tries to close already close slider than call to 
					 * updateOpenPos() will just overwrite openPos value with 'INVALID' (-1), even though
					 * some other row has its slider open.
					 */
					updateOpenPos(INVALID, null);
				}
			}
		}
		
		private void updateOpenPos(int openPos, ViewGroup parent) {
			//Log.d(TAG, "openPos = " + openPos);
			if (this.openPos == openPos) {
				/**
				 * onBindViewHolder can be called more than once, so no need to execute same code again 
				 * for same position
				 */
				return;
			}
			
			int oldOpenPos = this.openPos;
			this.openPos = openPos;
			
			if (parent != null && oldOpenPos != INVALID) {
				/**
				 * notify to close earlier open slider  
				 * 1) if we are opening another slider (not closing already open one). 
				 * While closing we pass null for parent, and
				 * 2) if some other slider was open before
				 */
				notifyItemChanged(oldOpenPos);
			}
		}
		
		private void onHandleClick(final ViewHolder holder, final int position) {
			//Log.d(TAG, "onHandleClick()");
			holder.vHandle.setPressed(true);
			
			if (holder.isSliderClose(rltLytContentInitialMarginL)) {
				// slider is close, so open it
				//Log.d(TAG, "open slider");
				ViewCompat.setElevation(holder.imgEvent, venueDetailsFragment.translationZPx);
				
				Animation slide = AnimationUtils.loadAnimation(FragmentUtil.getActivity(
						venueDetailsFragment), R.anim.slide_in_from_left);
				slide.setAnimationListener(new AnimationListener() {
					
					@Override
					public void onAnimationStart(Animation animation) {
						holder.lnrSliderContent.setVisibility(View.VISIBLE);
						
						RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) holder.lnrSliderContent.getLayoutParams();
						lp.rightMargin = 0;
						holder.lnrSliderContent.setLayoutParams(lp);
					}
					
					@Override
					public void onAnimationRepeat(Animation animation) {}
					
					@Override
					public void onAnimationEnd(Animation animation) {
						holder.vHandle.setPressed(false);
						
						RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) 
								holder.rltLytContent.getLayoutParams();
						lp.leftMargin -= holder.lnrSliderContent.getWidth();
						holder.rltLytContent.setLayoutParams(lp);
						//Log.d(TAG, "isSliderClose");
						
						updateOpenPos(position, recyclerView);
					}
				});
				holder.lnrSliderContent.startAnimation(slide);
				
			} else {
				// slider is open, so close it
				//Log.d(TAG, "close slider");
				Animation slide = AnimationUtils.loadAnimation(FragmentUtil.getActivity(
						venueDetailsFragment), android.R.anim.slide_out_right);
				slide.setAnimationListener(new AnimationListener() {
					
					@Override
					public void onAnimationStart(Animation animation) {}
					
					@Override
					public void onAnimationRepeat(Animation animation) {}
					
					@Override
					public void onAnimationEnd(Animation animation) {
						ViewCompat.setElevation(holder.imgEvent, 0);
						
						holder.vHandle.setPressed(false);
						
						holder.lnrSliderContent.setVisibility(View.INVISIBLE);
						RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) holder.lnrSliderContent.getLayoutParams();
						lp.rightMargin = 0 - lnrSliderContentW;;
						holder.lnrSliderContent.setLayoutParams(lp);
						
						lp = (RelativeLayout.LayoutParams) holder.rltLytContent.getLayoutParams();
						lp.leftMargin += holder.lnrSliderContent.getWidth();
						holder.rltLytContent.setLayoutParams(lp);
						//Log.d(TAG, "!isSliderClose");
						
						updateOpenPos(INVALID, null);
					}
				});
				holder.lnrSliderContent.startAnimation(slide);
			}
		}
		
		private void onEventClick(final ViewHolder holder, final Event event, final int position) {
			holder.rltLytRoot.setPressed(true);
			venueDetailsFragment.handler.postDelayed(new Runnable() {
				
				@Override
				public void run() {
					List<SharedElement> sharedElements = new ArrayList<SharedElement>();
					
					SharedElementPosition sharedElementPosition = new SharedElementPosition(venueDetailsFragment.imgEventPadL, 
							holder.itemView.getTop() + venueDetailsFragment.imgEventPadT, 
							holder.imgEvent.getWidth() - venueDetailsFragment.imgEventPadL - venueDetailsFragment.imgEventPadR, 
							holder.imgEvent.getHeight() - venueDetailsFragment.imgEventPadT - venueDetailsFragment.imgEventPadB);
					SharedElement sharedElement = new SharedElement(sharedElementPosition, holder.imgEvent);
					sharedElements.add(sharedElement);
					venueDetailsFragment.addViewsToBeHidden(holder.imgEvent);
					
					//Log.d(TAG, "AT issue event = " + event);
					((EventListener) FragmentUtil.getActivity(venueDetailsFragment)).onEventSelected(event, sharedElements);
					
					venueDetailsFragment.onPushedToBackStack();
					
					holder.rltLytRoot.setPressed(false);
				}
			}, 200);
		}
		
		private void onImgTicketClick(final ViewHolder holder, final Event event) {
			holder.imgTicket.setPressed(true);
			venueDetailsFragment.handler.postDelayed(new Runnable() {
				
				@Override
				public void run() {
					holder.imgTicket.setPressed(false);
					Bundle args = new Bundle();
					args.putString(BundleKeys.URL, event.getSchedule().getBookingInfos().get(0).getBookingUrl());
					((ReplaceFragmentListener)FragmentUtil.getActivity(venueDetailsFragment)).replaceByFragment(
							AppConstants.FRAGMENT_TAG_WEB_VIEW, args);
					/**
					 * added on 15-12-2014
					 */
					GoogleAnalyticsTracker.getInstance().sendEvent(FragmentUtil.getApplication(venueDetailsFragment), 
							venueDetailsFragment.getScreenName(), GoogleAnalyticsTracker.EVENT_LABEL_TICKETS_BUTTON, 
							GoogleAnalyticsTracker.Type.Event.name(), null, event.getId());
				}
			}, 200);
		}
		
		private void onImgSaveClick(final ViewHolder holder, final Event event) {
			holder.imgSave.setPressed(true);
			venueDetailsFragment.handler.postDelayed(new Runnable() {
				
				@Override
				public void run() {
					holder.imgSave.setPressed(false);
					
					EventSeekr eventSeekr = (EventSeekr) FragmentUtil.getActivity(venueDetailsFragment).getApplication();
					if (event.getAttending() == Attending.SAVED) {
						event.setAttending(Attending.NOT_GOING);
						new UserTracker(Api.OAUTH_TOKEN, eventSeekr, UserTrackingItemType.event, event.getId(), 
								event.getAttending().getValue(), UserTrackingType.Add).execute();
		    			updateImgSaveSrc(holder, event, FragmentUtil.getResources(venueDetailsFragment));
						
					} else {
						venueDetailsFragment.event = eventPendingPublish = event;
						holderPendingPublish = holder;
						
						if (eventSeekr.getGPlusUserId() != null) {
							event.setNewAttending(Attending.SAVED);
							venueDetailsFragment.handlePublishEvent();
							
						} else {
							fbCallCountForSameEvt = 0;
							event.setNewAttending(Attending.SAVED);
							//NOTE: THIS CAN BE TESTED WITH PODUCTION BUILD ONLY
							FbUtil.handlePublishEvent(venueDetailsFragment, venueDetailsFragment, AppConstants.PERMISSIONS_FB_PUBLISH_EVT_OR_ART, 
									AppConstants.REQ_CODE_FB_PUBLISH_EVT_OR_ART, event);
						}
					}
				}
			}, 200);
		}
		
		private void onImgShareClick(final ViewHolder holder, final Event event) {
			holder.imgShare.setPressed(true);
			venueDetailsFragment.handler.postDelayed(new Runnable() {
				
				@Override
				public void run() {
					holder.imgShare.setPressed(false);
					
					ShareViaDialogFragment shareViaDialogFragment = ShareViaDialogFragment.newInstance(event, 
							venueDetailsFragment.getScreenName());
					/**
					 * Passing activity fragment manager, since using this fragment's child fragment manager 
					 * doesn't retain dialog on orientation change
					 */
					shareViaDialogFragment.show(((FragmentActivity)FragmentUtil.getActivity(venueDetailsFragment))
							.getSupportFragmentManager(), FRAGMENT_TAG_SHARE_VIA_DIALOG);
				}
			}, 200);
		}
		
		// to update values which should change on orientation change
		private void onActivityCreated() {
			rltLytContentW = INVALID;
			Resources res = FragmentUtil.getResources(venueDetailsFragment);
			rltLytContentInitialMarginL = res.getDimensionPixelSize(R.dimen.rlt_lyt_content_margin_l_list_item_discover);
			lnrSliderContentW = res.getDimensionPixelSize(R.dimen.lnr_slider_content_w_list_item_discover);
		}
		
		private void call(Session session, SessionState state, Exception exception) {
			//Log.i(TAG, "call()");
			fbCallCountForSameEvt++;
			/**
			 * To prevent infinite loop when network is off & we are calling requestPublishPermissions() of FbUtil.
			 */
			if (fbCallCountForSameEvt < AppConstants.MAX_FB_CALL_COUNT_FOR_SAME_EVT_OR_ART) {
				FbUtil.call(session, state, exception, venueDetailsFragment, venueDetailsFragment, AppConstants.PERMISSIONS_FB_PUBLISH_EVT_OR_ART, 
						AppConstants.REQ_CODE_FB_PUBLISH_EVT_OR_ART, eventPendingPublish);
				
			} else {
				fbCallCountForSameEvt = 0;
				venueDetailsFragment.setPendingAnnounce(false);
			}
		}

		private void onPublishPermissionGranted() {
			//Log.d(TAG, "onPublishPermissionGranted()");
			updateImgSaveSrc(holderPendingPublish, eventPendingPublish, FragmentUtil.getResources(venueDetailsFragment));
		}

		@Override
		public int getEventsAlreadyRequested() {
			return eventsAlreadyRequested;
		}

		@Override
		public void setMoreDataAvailable(boolean isMoreDataAvailable) {
			this.isMoreDataAvailable = isMoreDataAvailable;
		}

		@Override
		public void setEventsAlreadyRequested(int eventsAlreadyRequested) {
			this.eventsAlreadyRequested = eventsAlreadyRequested;
		}

		@Override
		public void updateContext(Context context) {
			// TODO Auto-generated method stub
		}

		@Override
		public void setLoadDateWiseEvents(AsyncTask<Void, Void, List<Event>> loadDateWiseEvents) {
			this.loadEvents = (LoadEvents) loadDateWiseEvents;
		}
	}

	@Override
	public void loadItemsInBackground() {
		loadEvents = new LoadEvents(Api.OAUTH_TOKEN, eventList, venueRVAdapter, ((EventSeekr)FragmentUtil
				.getApplication(this)).getWcitiesId(), venue.getId(), this);
		venueRVAdapter.setLoadDateWiseEvents(loadEvents);
		AsyncTaskUtil.executeAsyncTask(loadEvents, true);
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
	public void onPushedToBackStack() {
		/**
		 * to remove facebook callback. Not calling onStop() to prevent toolbar color changes occurring in between
		 * the transition
		 */
		super.onStop();
		
		/**
		 * set null listener, otherwise even for artist/event details screen when selecting 
		 * share option it calls this listener's onShareTargetSelected() method.
		 */
		if (mShareActionProvider != null) {
			mShareActionProvider.setOnShareTargetSelectedListener(null);
		}
		setMenuVisibility(false);
	}

	@Override
	public void onPoppedFromBackStack() {
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

	@Override
	public void onPublishPermissionGranted() {
		venueRVAdapter.onPublishPermissionGranted();
	}

	@Override
	public void call(Session session, SessionState state, Exception exception) {
		venueRVAdapter.call(session, state, exception);
	}

	@Override
	public void onImageLoaded() {
		updateShareIntent();
	}

	@Override
	public void onImageCouldNotBeLoaded() {
		// TODO Auto-generated method stub
	}

	@Override
	public void onEventsLoaded() {
		handler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				onScrolled(0, true);
			}
		}, 100);
	}
}
