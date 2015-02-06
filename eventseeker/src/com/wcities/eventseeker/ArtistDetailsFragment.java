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
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.ShareActionProvider.OnShareTargetSelectedListener;
import android.text.Html;
import android.text.TextUtils.TruncateAt;
import android.util.DisplayMetrics;
import android.view.Gravity;
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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.wcities.eventseeker.GeneralDialogFragment.DialogBtnClickListener;
import com.wcities.eventseeker.ShareOnFBDialogFragment.OnFacebookShareClickedListener;
import com.wcities.eventseeker.adapter.FriendsRVAdapter;
import com.wcities.eventseeker.adapter.VideoPagerAdapter;
import com.wcities.eventseeker.analytics.GoogleAnalyticsTracker;
import com.wcities.eventseeker.analytics.GoogleAnalyticsTracker.Type;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingItemType;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.asynctask.LoadArtistDetails;
import com.wcities.eventseeker.asynctask.LoadArtistDetails.OnArtistUpdatedListener;
import com.wcities.eventseeker.asynctask.LoadArtistEvents;
import com.wcities.eventseeker.asynctask.LoadArtistEvents.LoadArtistEventsListener;
import com.wcities.eventseeker.asynctask.UserTracker;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Date;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Event.Attending;
import com.wcities.eventseeker.core.Schedule;
import com.wcities.eventseeker.custom.fragment.PublishEventFragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.ArtistTrackingListener;
import com.wcities.eventseeker.interfaces.CustomSharedElementTransitionDestination;
import com.wcities.eventseeker.interfaces.CustomSharedElementTransitionSource;
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener;
import com.wcities.eventseeker.interfaces.EventListener;
import com.wcities.eventseeker.interfaces.FragmentHavingFragmentInRecyclerView;
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

public class ArtistDetailsFragment extends PublishEventFragmentLoadableFromBackStack implements DrawerListener, 
		CustomSharedElementTransitionDestination, OnArtistUpdatedListener, OnClickListener, 
		LoadItemsInBackgroundListener, CustomSharedElementTransitionSource, ArtistTrackingListener, 
		DialogBtnClickListener, LoadArtistEventsListener, FragmentHavingFragmentInRecyclerView, 
		OnFacebookShareClickedListener {
	
	private static final String TAG = ArtistDetailsFragment.class.getName();
	
	private static final String FRAGMENT_TAG_SHARE_VIA_DIALOG = ShareViaDialogFragment.class.getSimpleName();
	private static final String FRAGMENT_TAG_REMOVE_ARTIST_DIALOG = "RemoveArtist";
	private static final String FRAGMENT_TAG_ARTIST_SAVED_DIALOG = "ArtistSaved";
	
	private static final int UNSCROLLED = -1;
	private static final int TRANSITION_ANIM_DURATION = 400, FAB_SCROLL_THRESHOLD_IN_DP = 12;
	private static final int TRANSLATION_Z_DP = 10;

	private View rootView;
	private ImageView imgArtist;
	private TextView txtArtistTitle;
	private RecyclerView recyclerVArtists;
	private RelativeLayout rltLytTxtArtistTitle;
	private FloatingActionButton fabSave;
	
	private ArtistRVAdapter artistRVAdapter;
	
	private int totalScrolledDy = UNSCROLLED; // indicates layout not yet created
	private int limitScrollAt, actionBarElevation, fabScrollThreshold;
	private float minTitleScale;
	private boolean isScrollLimitReached;
	private String title = "";
	private float translationZPx;
	
	private Artist artist;
	private List<Event> eventList;
	
	private List<SharedElement> sharedElements;
	private boolean isOnCreateViewCalledFirstTime = true;
	private int screenW, imgArtistHt;
	private AnimatorSet animatorSet;
	private boolean isOnPushedToBackStackCalled;
	
	private LoadArtistDetails loadArtistDetails;
	private LoadArtistEvents loadArtistEvents;
	private boolean allDetailsLoaded;
	
	private Handler handler;
	
	private int imgEventPadL, imgEventPadR, imgEventPadT, imgEventPadB, fabSaveMarginT;
	private List<View> hiddenViews;
	
	private ShareActionProvider mShareActionProvider;
	
	private int fbCallCountForSameArtist = 0;
	
	private boolean isArtistSaveClicked;
	
	private OnShareTargetSelectedListener onShareTargetSelectedListener = new OnShareTargetSelectedListener() {
		
		@Override
		public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
			String shareTarget = intent.getComponent().getPackageName();
			GoogleAnalyticsTracker.getInstance().sendShareEvent(FragmentUtil.getApplication(ArtistDetailsFragment.this), 
					getScreenName(), shareTarget, Type.Artist, artist.getId());
			return false;
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setHasOptionsMenu(true);
		setRetainInstance(true);
		
		actionBarElevation = FragmentUtil.getResources(this).getDimensionPixelSize(R.dimen.action_bar_elevation);
		fabScrollThreshold = ConversionUtil.toPx(FragmentUtil.getResources(this), FAB_SCROLL_THRESHOLD_IN_DP);
		
		Bundle args = getArguments();
		if (args.containsKey(BundleKeys.SHARED_ELEMENTS)) {
			sharedElements = (List<SharedElement>) args.getSerializable(BundleKeys.SHARED_ELEMENTS);
		}
		if (artist == null) {
			//Log.d(TAG, "event = null");
			artist = (Artist) args.getSerializable(BundleKeys.ARTIST);
			artist.getVideos().clear();
			artist.getFriends().clear();
			
			updateShareIntent();
		}
		
		Resources res = FragmentUtil.getResources(this);
		translationZPx = ConversionUtil.toPx(res, TRANSLATION_Z_DP);
		handler = new Handler(Looper.getMainLooper());
		
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
		
		rootView = inflater.inflate(R.layout.fragment_artist_details, container, false);
		
		imgArtist = (ImageView) rootView.findViewById(R.id.imgArtist);
		updateArtistImg();
		
		rltLytTxtArtistTitle = (RelativeLayout) rootView.findViewById(R.id.rltLytTxtArtistTitle);
		
		txtArtistTitle = (TextView) rootView.findViewById(R.id.txtArtistTitle);
		txtArtistTitle.setText(artist.getName());
		// for marquee to work
		txtArtistTitle.setSelected(true);
		
		fabSave = (FloatingActionButton) rootView.findViewById(R.id.fabSave);
		fabSave.setSelected(artist.getAttending() == Artist.Attending.Tracked);
		fabSave.setOnClickListener(this);
		
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
				if (((MainActivity)FragmentUtil.getActivity(ArtistDetailsFragment.this)).isDrawerOpen()) {
					// to maintain status bar & toolbar decorations after orientation change
					onDrawerOpened();
				}
            }
        });
		
		if (isOnCreateViewCalledFirstTime) {
			fabSave.setVisibility(View.INVISIBLE);
			
			isOnCreateViewCalledFirstTime = false;
			
			if (sharedElements != null) {
				animateSharedElements();
				
			} else {
				rootView.setBackgroundColor(Color.WHITE);
				
				loadArtistDetails = new LoadArtistDetails(Api.OAUTH_TOKEN, artist, this, this);
				AsyncTaskUtil.executeAsyncTask(loadArtistDetails, true);
			}
		}
		
		return rootView;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (artistRVAdapter == null) {
			eventList = new ArrayList<Event>();
			eventList.add(null);
			
			artistRVAdapter = new ArtistRVAdapter(this, eventList, null, this);
			
		} else {
			artistRVAdapter.onActivityCreated();
		}
		recyclerVArtists.setAdapter(artistRVAdapter);
	}
	
	@Override
	public void onStart() {
		//Log.d(TAG, "onStart()");
		super.onStart();
		
		((MainActivity) FragmentUtil.getActivity(this)).setVStatusBarVisibility(View.GONE, AppConstants.INVALID_ID);
		if (totalScrolledDy != UNSCROLLED) {
			onScrolled(0, true);
			
			if (((MainActivity)FragmentUtil.getActivity(ArtistDetailsFragment.this)).isDrawerOpen()) {
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
		if (loadArtistEvents != null && loadArtistEvents.getStatus() != Status.FINISHED) {
			loadArtistEvents.cancel(true);
		}
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_artist_details, menu);
		
		MenuItem item = (MenuItem) menu.findItem(R.id.action_share);
	    mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
	    updateShareIntent();
	    
    	super.onCreateOptionsMenu(menu, inflater);
	}
	
	private void updateShareIntent() {
	    if (mShareActionProvider != null && artist != null) {
			//Log.d(TAG, "updateShareIntent()");
	    	Intent shareIntent = new Intent(Intent.ACTION_SEND);
		    shareIntent.setType("image/*");
		    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Artist Details");
		    String message = "Checkout " + artist.getName() + " on eventseeker";
		    if (artist.getArtistUrl() != null) {
		    	message += ": " + artist.getArtistUrl();
		    }
		    shareIntent.putExtra(Intent.EXTRA_TEXT, message);
			
			String key = artist.getKey(ImgResolution.LOW);
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
	
	private void onScrolled(int dy, boolean forceUpdate) {
		//Log.d(TAG, "first visible pos = " + ((LinearLayoutManager)recyclerVArtists.getLayoutManager()).findFirstVisibleItemPosition());
		//Log.d(TAG, "dy = " + dy);
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		if (totalScrolledDy == UNSCROLLED) {
			totalScrolledDy = 0;
		}
		totalScrolledDy += dy;
		//Log.d(TAG, "totalScrolledDy = " + totalScrolledDy + ", top = " + recyclerVArtists.getLayoutManager().findViewByPosition(((LinearLayoutManager)recyclerVArtists.getLayoutManager()).findFirstVisibleItemPosition()).getTop());
		/**
		 * this is required to prevent changes in scrolled value due to automatic corrections in recyclerview size
		 * e.g.: 1) Due to event loading progressbar returning no events resulting in reduction of overall size
		 * & hence totalScrolledDy value must be changed but we don't have good way to calculate it & hence
		 * just update it to right value when position is 0 (when we are sure about exact totalScrolledDy value)
		 * It's actually needed for changing toolbar color which we do only when 1st visible position is 0.
		 * 2) When screen becomes scrollable after expanding description but not on collapsing, resulting in 
		 * automatic scroll to settle recyclerview.
		 */
		if (((LinearLayoutManager)recyclerVArtists.getLayoutManager()).findFirstVisibleItemPosition() == 0) {
			totalScrolledDy = -recyclerVArtists.getLayoutManager().findViewByPosition(0).getTop();
			//Log.d(TAG, "totalScrolledDy corrected = " + totalScrolledDy);
		}
		
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
			
			FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) fabSave.getLayoutParams();
			lp.gravity = Gravity.BOTTOM | Gravity.RIGHT;
			fabSave.setLayoutParams(lp);
			
			isScrollLimitReached = true;
			
		} else if ((isScrollLimitReached || forceUpdate) && totalScrolledDy < limitScrollAt) {
			//Log.d(TAG, "totalScrolledDy < limitScrollAt");
			ma.animateToolbarElevation(actionBarElevation, 0.0f);
			
			ma.setVStatusBarLayeredVisibility(View.GONE, AppConstants.INVALID_ID);
			ma.setToolbarBg(Color.TRANSPARENT);
			
			title = "";
			ma.updateTitle(title);
			
			/**
			 * to negate the translationY applied by hide() function of fabSave if show() is not called 
			 * anytime after call to hide().
			 */
			fabSave.show();
			FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) fabSave.getLayoutParams();
			lp.gravity = Gravity.TOP | Gravity.RIGHT;
			//Log.d(TAG, "fabSaveMarginT top margin = " + lp.topMargin);
			if (fabSaveMarginT == 0) {
				fabSaveMarginT = lp.topMargin;
				//Log.d(TAG, "fabSaveMarginT = " + fabSaveMarginT);
			}
			lp.topMargin = fabSaveMarginT - totalScrolledDy;
			fabSave.setLayoutParams(lp);
			
			isScrollLimitReached = false;
			
		} else if (totalScrolledDy < limitScrollAt) {
			FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) fabSave.getLayoutParams();
			lp.topMargin = fabSaveMarginT - totalScrolledDy;
			fabSave.setLayoutParams(lp);
		}
		
		if (scrollY < limitScrollAt) {
			float scale = 1 - (((1 - minTitleScale) / limitScrollAt) * scrollY);
			//Log.d(TAG, "scale = " + scale);
			
			ViewHelper.setPivotX(txtArtistTitle, 0);
	        ViewHelper.setPivotY(txtArtistTitle, txtArtistTitle.getHeight() / 2);
	        ViewHelper.setScaleX(txtArtistTitle, scale);
	        ViewHelper.setScaleY(txtArtistTitle, scale);
	        
		} else {
			boolean isSignificantDelta = Math.abs(dy) > fabScrollThreshold;
	        if (isSignificantDelta) {
	            if (dy > 0) {
	                fabSave.hide(true);
	                
	            } else {
	                fabSave.show(true);
	            }
	        }
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
		
		Resources res = FragmentUtil.getResources(this);
		imgArtistHt = res.getDimensionPixelSize(R.dimen.img_artist_ht_artist_details);
		
		imgEventPadL = res.getDimensionPixelSize(R.dimen.img_event_pad_l_list_item_discover);
		imgEventPadR = res.getDimensionPixelSize(R.dimen.img_event_pad_r_list_item_discover);
		imgEventPadT = res.getDimensionPixelSize(R.dimen.img_event_pad_t_list_item_discover);
		imgEventPadB = res.getDimensionPixelSize(R.dimen.img_event_pad_b_list_item_discover);
		
		/**
		 * Don't get it from dimen. Not sure why, but even though actual value set from xml is same as this,
		 * returned value from LayoutParams of fabSave is less (tested for galaxy S, instead of ~290px, it returns 272px)  
		 */
		//fabSaveMarginT = res.getDimensionPixelSize(R.dimen.fab_margin_t_artist_details);
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
		/**
		 * set visible to finish this screen even if user presses back button instantly even before animateSharedElements()
		 * has finished it work; otherwise if recyclerVVenues is invisible, then user has to press back once more in such case
		 * on instantly clicking back
		 */
		recyclerVArtists.setVisibility(View.VISIBLE);
		recyclerVArtists.startAnimation(slideOutToBottom);
    }

	@Override
	public void onArtistUpdated() {
		allDetailsLoaded = true;
		fabSave.setSelected(artist.getAttending() == Artist.Attending.Tracked);
		fabSave.setVisibility(View.VISIBLE);
		artistRVAdapter.notifyDataSetChanged();
		updateShareIntent();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.fabSave:
			if (artist.getAttending() == Artist.Attending.Tracked) {
				Resources res = FragmentUtil.getResources(this);
				GeneralDialogFragment generalDialogFragment = GeneralDialogFragment.newInstance(
						this,						
						res.getString(R.string.remove_artist),  
						res.getString(R.string.are_you_sure_you_want_to_remove_this_artist),  
						res.getString(R.string.btn_cancel),  
						res.getString(R.string.btn_Ok), false);
				generalDialogFragment.show(((ActionBarActivity) FragmentUtil.getActivity(this)).getSupportFragmentManager(), 
						FRAGMENT_TAG_REMOVE_ARTIST_DIALOG);
				
			} else {
				/**
				 * This is the case, where user wants to Track an Artist. So, no dialog here.
				 */
				onArtistTracking(FragmentUtil.getApplication(this), artist);
			}
			break;
			
		default:
			break;
		}
	}
	
	private static class ArtistRVAdapter extends RecyclerView.Adapter<ArtistRVAdapter.ViewHolder> implements 
			DateWiseEventParentAdapterListener {
		
		private static final int EXTRA_TOP_DUMMY_ITEM_COUNT = 2;
		private static final int EXTRA_TOP_DUMMY_ITEM_COUNT_AFTER_DETAILS_LOADED = 5;
		private static final int MAX_LINES_ARTIST_DESC = 5;
		private static final int INVALID = -1;
		
		private RecyclerView recyclerView;
		private ArtistDetailsFragment artistDetailsFragment;
		
		private boolean isArtistDescExpanded;
		private List<Event> eventList;
		private LoadArtistEvents loadArtistEvents;
		private boolean isMoreDataAvailable = true;
		private int eventsAlreadyRequested;
		private LoadItemsInBackgroundListener mListener;

		private VideoPagerAdapter videoPagerAdapter;
		private FriendsRVAdapter friendsRVAdapter;
		
		private int fbCallCountForSameEvt = 0;
		private ArtistRVAdapter.ViewHolder holderPendingPublish;
		private Event eventPendingPublish;
		
		private BitmapCache bitmapCache;
		
		private int openPos = INVALID;
		private int rltLytContentInitialMarginL, lnrSliderContentW, imgEventW, rltLytContentW = INVALID;
		
		private static enum ViewType {
			IMG, DESC, VIDEOS, FRIENDS, UPCOMING_EVENTS_TITLE, PROGRESS, EVENT;
			
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
			private ViewPager vPagerVideos;
			private RecyclerView recyclerVFriends;
			
			// event item
			private View vHandle;
			private TextView txtEvtTitle, txtEvtTime, txtEvtLocation;
			private ImageView imgEvent, imgTicket, imgSave, imgShare;
	        private LinearLayout lnrSliderContent;
	        private RelativeLayout rltLytRoot, rltLytContent, rltTicket, rltSave, rltShare;

			public ViewHolder(View itemView) {
				super(itemView);
				txtDesc = (TextView) itemView.findViewById(R.id.txtDesc);
				imgDown = (ImageView) itemView.findViewById(R.id.imgDown);
				rltLytPrgsBar = (RelativeLayout) itemView.findViewById(R.id.rltLytPrgsBar);
				vHorLine = itemView.findViewById(R.id.vHorLine);
				vPagerVideos = (ViewPager) itemView.findViewById(R.id.vPagerVideos);
				recyclerVFriends = (RecyclerView) itemView.findViewById(R.id.recyclerVFriends);
				
				// event item
				txtEvtTitle = (TextView) itemView.findViewById(R.id.txtEvtTitle);
	            txtEvtTime = (TextView) itemView.findViewById(R.id.txtEvtTime);
	            txtEvtLocation = (TextView) itemView.findViewById(R.id.txtEvtLocation);
	            imgEvent = (ImageView) itemView.findViewById(R.id.imgEvent);
	            vHandle = itemView.findViewById(R.id.vHandle);
	            lnrSliderContent = (LinearLayout) itemView.findViewById(R.id.lnrSliderContent);
	            rltLytRoot = (RelativeLayout) itemView.findViewById(R.id.rltLytRoot);
	            rltLytContent = (RelativeLayout) itemView.findViewById(R.id.rltLytContent);
	            rltTicket = (RelativeLayout) itemView.findViewById(R.id.rltTicket);
	            rltSave = (RelativeLayout) itemView.findViewById(R.id.rltSave);
	            rltShare = (RelativeLayout) itemView.findViewById(R.id.rltShare);
	            imgTicket = (ImageView) itemView.findViewById(R.id.imgTicket);
	            imgSave = (ImageView) itemView.findViewById(R.id.imgSave);
	            imgShare = (ImageView) itemView.findViewById(R.id.imgShare);
			}
			
			private boolean isSliderClose(int rltLytContentInitialMarginL) {
	        	RelativeLayout.LayoutParams rltLytContentLP = (RelativeLayout.LayoutParams) rltLytContent.getLayoutParams();
				return (rltLytContentLP.leftMargin == rltLytContentInitialMarginL);
	        }
		}
		
		public ArtistRVAdapter(ArtistDetailsFragment artistDetailsFragment, List<Event> eventList, 
				LoadArtistEvents loadArtistEvents, LoadItemsInBackgroundListener mListener) {
			this.artistDetailsFragment = artistDetailsFragment;
			this.eventList = eventList;
			this.loadArtistEvents = loadArtistEvents;
			this.mListener = mListener;
			
			bitmapCache = BitmapCache.getInstance();
			
			Resources res = FragmentUtil.getResources(artistDetailsFragment);
			rltLytContentInitialMarginL = res.getDimensionPixelSize(R.dimen.rlt_lyt_content_margin_l_list_item_discover);
			lnrSliderContentW = res.getDimensionPixelSize(R.dimen.lnr_slider_content_w_list_item_discover);
			imgEventW = res.getDimensionPixelSize(R.dimen.img_event_w_list_item_discover);
		}

		@Override
		public int getItemViewType(int position) {
			if (position == ViewType.IMG.ordinal()) {
				return ViewType.IMG.ordinal();
				
			} else if (position == ViewType.DESC.ordinal()) {
				return ViewType.DESC.ordinal();
				
			} else if (position == ViewType.VIDEOS.ordinal()) {
				return ViewType.VIDEOS.ordinal();
				
			} else if (position == ViewType.FRIENDS.ordinal()) {
				return ViewType.FRIENDS.ordinal();
				
			} else if (position == ViewType.UPCOMING_EVENTS_TITLE.ordinal()) {
				return ViewType.UPCOMING_EVENTS_TITLE.ordinal();
				
			} else if (eventList.get(position - EXTRA_TOP_DUMMY_ITEM_COUNT_AFTER_DETAILS_LOADED) == null) {
				return ViewType.PROGRESS.ordinal();
				
			} else {
				return ViewType.EVENT.ordinal();
			}
		}

		@Override
		public int getItemCount() {
			return artistDetailsFragment.allDetailsLoaded ? (EXTRA_TOP_DUMMY_ITEM_COUNT_AFTER_DETAILS_LOADED + 
					eventList.size()) : EXTRA_TOP_DUMMY_ITEM_COUNT;
		}

		@Override
		public void onBindViewHolder(final ViewHolder holder, final int position) {
			if (position == ViewType.IMG.ordinal()) {
				// nothing to do
				
			} else if (position == ViewType.DESC.ordinal()) {
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
				} 
				
				// use a linear layout manager
				RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(FragmentUtil.getActivity(artistDetailsFragment), 
						LinearLayoutManager.HORIZONTAL, false);
				holder.recyclerVFriends.setLayoutManager(layoutManager);
				
				holder.recyclerVFriends.setAdapter(friendsRVAdapter);
				
				updateFriendsVisibility(holder);
				
			} else if (position == ViewType.UPCOMING_EVENTS_TITLE.ordinal()) {
				if (eventList.isEmpty()) {
					setViewGone(holder);
				}
				
			} else {
				final Event event = eventList.get(position - EXTRA_TOP_DUMMY_ITEM_COUNT_AFTER_DETAILS_LOADED);
				if (event == null) {
					// progress indicator
					
					if ((loadArtistEvents == null || loadArtistEvents.getStatus() == Status.FINISHED) && 
							isMoreDataAvailable) {
						//Log.d(TAG, "onBindViewHolder(), pos = " + position);
						mListener.loadItemsInBackground();
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
					
					final Resources res = FragmentUtil.getResources(artistDetailsFragment);
					if (event.getSchedule() == null || event.getSchedule().getBookingInfos().isEmpty()) {
						holder.imgTicket.setImageDrawable(res.getDrawable(R.drawable.ic_tickets_unavailable_slider));
						holder.imgTicket.setEnabled(false);
						
					} else {
						holder.imgTicket.setImageDrawable(res.getDrawable(R.drawable.ic_tickets_available_slider));
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
									ViewCompat.setElevation(holder.imgEvent, artistDetailsFragment.translationZPx);
									
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
													mEvent.getRawX(), mEvent.getRawY(), holder.rltTicket)) {
												onImgTicketClick(holder, event);
													
											} else if (ViewUtil.isPointInsideView(mEvent.getRawX(), mEvent.getRawY(), holder.rltSave)) {
												onImgSaveClick(holder, event);
												
											} else if (ViewUtil.isPointInsideView(mEvent.getRawX(), mEvent.getRawY(), holder.rltShare)) {
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

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			//Log.d(TAG, "onCreateViewHolder(), viewType = " + viewType);
			View v;
			
			recyclerView = (RecyclerView) parent;
			
			switch (ViewType.getViewType(viewType)) {
			
			case IMG:
				v = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_item_img_artist_artist_details, 
						parent, false);
				break;
				
			case DESC:
				v = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_item_desc, 
						parent, false);
				break;
				
			case VIDEOS:
				v = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_item_videos_artist_details, 
						parent, false);
				break;
				
			case FRIENDS:
				v = LayoutInflater.from(parent.getContext()).inflate(R.layout.include_friends, 
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
		
		// to update values which should change on orientation change
		private void onActivityCreated() {
			rltLytContentW = INVALID;
			Resources res = FragmentUtil.getResources(artistDetailsFragment);
			rltLytContentInitialMarginL = res.getDimensionPixelSize(R.dimen.rlt_lyt_content_margin_l_list_item_discover);
			lnrSliderContentW = res.getDimensionPixelSize(R.dimen.lnr_slider_content_w_list_item_discover);
		}
		
		private void updateImgSaveSrc(ViewHolder holder, Event event, Resources res) {
			int drawableId = (event.getAttending() == Attending.SAVED) ? R.drawable.ic_saved_event_slider
					: R.drawable.ic_unsaved_event_slider;
			holder.imgSave.setImageDrawable(res.getDrawable(drawableId));
		}
		
		private void openSlider(ViewHolder holder, int position, boolean isUserInitiated) {
			ViewCompat.setElevation(holder.imgEvent, artistDetailsFragment.translationZPx);
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
				ViewCompat.setElevation(holder.imgEvent, artistDetailsFragment.translationZPx);
				
				Animation slide = AnimationUtils.loadAnimation(FragmentUtil.getActivity(
						artistDetailsFragment), R.anim.slide_in_from_left);
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
						artistDetailsFragment), android.R.anim.slide_out_right);
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
			artistDetailsFragment.handler.postDelayed(new Runnable() {
				
				@Override
				public void run() {
					List<SharedElement> sharedElements = new ArrayList<SharedElement>();
					
					SharedElementPosition sharedElementPosition = new SharedElementPosition(artistDetailsFragment.imgEventPadL, 
							holder.itemView.getTop() + artistDetailsFragment.imgEventPadT, 
							holder.imgEvent.getWidth() - artistDetailsFragment.imgEventPadL - artistDetailsFragment.imgEventPadR, 
							holder.imgEvent.getHeight() - artistDetailsFragment.imgEventPadT - artistDetailsFragment.imgEventPadB);
					SharedElement sharedElement = new SharedElement(sharedElementPosition, holder.imgEvent);
					sharedElements.add(sharedElement);
					artistDetailsFragment.addViewsToBeHidden(holder.imgEvent);
					
					//Log.d(TAG, "AT issue event = " + event);
					((EventListener) FragmentUtil.getActivity(artistDetailsFragment)).onEventSelected(event, sharedElements);
					
					artistDetailsFragment.onPushedToBackStack();
					
					holder.rltLytRoot.setPressed(false);
				}
			}, 200);
		}
		
		private void onImgTicketClick(final ViewHolder holder, final Event event) {
			holder.rltTicket.setPressed(true);
			artistDetailsFragment.handler.postDelayed(new Runnable() {
				
				@Override
				public void run() {
					holder.rltTicket.setPressed(false);
					Bundle args = new Bundle();
					args.putString(BundleKeys.URL, event.getSchedule().getBookingInfos().get(0).getBookingUrl());
					((ReplaceFragmentListener)FragmentUtil.getActivity(artistDetailsFragment)).replaceByFragment(
							AppConstants.FRAGMENT_TAG_WEB_VIEW, args);
					
					/**
					 * need to call onPushedToBackStack() since we are adding any fragment instead of replacing on artist details screen.
					 * Why adding? Answer: If we replace or remove-add anything on artist details fragment, it crashes with 
					 * "IllegalArgumentException: no view found for id R.id.vPagerVideos for 
					 * VideoFragment" on coming back to artist details screen. Couldn't find its solution. 
					 * Probably it's happening with any Fragment within RecyclerView.
					 */ 
					((FragmentHavingFragmentInRecyclerView)artistDetailsFragment).onPushedToBackStackFHFIR();
					
					/**
					 * added on 15-12-2014
					 */
					GoogleAnalyticsTracker.getInstance().sendEvent(FragmentUtil.getApplication(artistDetailsFragment), 
							artistDetailsFragment.getScreenName(), GoogleAnalyticsTracker.EVENT_LABEL_TICKETS_BUTTON, 
							GoogleAnalyticsTracker.Type.Event.name(), null, event.getId());
				}
			}, 200);
		}
		
		private void onImgSaveClick(final ViewHolder holder, final Event event) {
			holder.rltSave.setPressed(true);
			artistDetailsFragment.handler.postDelayed(new Runnable() {
				
				@Override
				public void run() {
					holder.rltSave.setPressed(false);
					
					EventSeekr eventSeekr = (EventSeekr) FragmentUtil.getActivity(artistDetailsFragment).getApplication();
					if (event.getAttending() == Attending.SAVED) {
						event.setAttending(Attending.NOT_GOING);
						new UserTracker(Api.OAUTH_TOKEN, eventSeekr, UserTrackingItemType.event, event.getId(), 
								event.getAttending().getValue(), UserTrackingType.Add).execute();
		    			updateImgSaveSrc(holder, event, FragmentUtil.getResources(artistDetailsFragment));
						
					} else {
						artistDetailsFragment.event = eventPendingPublish = event;
						holderPendingPublish = holder;
						
						if (eventSeekr.getGPlusUserId() != null) {
							event.setNewAttending(Attending.SAVED);
							artistDetailsFragment.handlePublishEvent();
							
						} else {
							artistDetailsFragment.isArtistSaveClicked = false;
							fbCallCountForSameEvt = 0;
							event.setNewAttending(Attending.SAVED);
							//NOTE: THIS CAN BE TESTED WITH PODUCTION BUILD ONLY
							FbUtil.handlePublishEvent(artistDetailsFragment, artistDetailsFragment, AppConstants.PERMISSIONS_FB_PUBLISH_EVT_OR_ART, 
									AppConstants.REQ_CODE_FB_PUBLISH_EVT_OR_ART, event);
						}
					}
				}
			}, 200);
		}
		
		private void onImgShareClick(final ViewHolder holder, final Event event) {
			holder.rltShare.setPressed(true);
			artistDetailsFragment.handler.postDelayed(new Runnable() {
				
				@Override
				public void run() {
					holder.rltShare.setPressed(false);
					
					ShareViaDialogFragment shareViaDialogFragment = ShareViaDialogFragment.newInstance(event, 
							artistDetailsFragment.getScreenName());
					/**
					 * Passing activity fragment manager, since using this fragment's child fragment manager 
					 * doesn't retain dialog on orientation change
					 */
					shareViaDialogFragment.show(((FragmentActivity)FragmentUtil.getActivity(artistDetailsFragment))
							.getSupportFragmentManager(), FRAGMENT_TAG_SHARE_VIA_DIALOG);
				}
			}, 200);
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
						artistDetailsFragment.handler.post(new Runnable() {
							
							@Override
							public void run() {
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
			holder.imgDown.setImageDrawable(FragmentUtil.getResources(artistDetailsFragment).getDrawable(R.drawable.ic_description_collapse));
			isArtistDescExpanded = true;
		}
		
		private void collapseArtistDesc(ViewHolder holder) {
			holder.txtDesc.setMaxLines(MAX_LINES_ARTIST_DESC);
			holder.txtDesc.setEllipsize(TruncateAt.END);
			holder.imgDown.setImageDrawable(FragmentUtil.getResources(artistDetailsFragment).getDrawable(R.drawable.ic_description_expand));
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
		
		private void call(Session session, SessionState state, Exception exception) {
			//Log.i(TAG, "call()");
			fbCallCountForSameEvt++;
			/**
			 * To prevent infinite loop when network is off & we are calling requestPublishPermissions() of FbUtil.
			 */
			if (fbCallCountForSameEvt < AppConstants.MAX_FB_CALL_COUNT_FOR_SAME_EVT_OR_ART) {
				FbUtil.call(session, state, exception, artistDetailsFragment, artistDetailsFragment, AppConstants.PERMISSIONS_FB_PUBLISH_EVT_OR_ART, 
						AppConstants.REQ_CODE_FB_PUBLISH_EVT_OR_ART, eventPendingPublish);
				
			} else {
				fbCallCountForSameEvt = 0;
				artistDetailsFragment.setPendingAnnounce(false);
			}
		}

		private void onPublishPermissionGranted() {
			//Log.d(TAG, "onPublishPermissionGranted()");
			updateImgSaveSrc(holderPendingPublish, eventPendingPublish, FragmentUtil.getResources(artistDetailsFragment));
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
			
		}

		@Override
		public void setLoadDateWiseEvents(AsyncTask<Void, Void, List<Event>> loadDateWiseEvents) {
			this.loadArtistEvents = (LoadArtistEvents) loadDateWiseEvents;
		}
	}

	@Override
	public void loadItemsInBackground() {
		loadArtistEvents = new LoadArtistEvents(Api.OAUTH_TOKEN, eventList, artistRVAdapter, artist.getId(),
				((EventSeekr)FragmentUtil.getActivity(this).getApplicationContext()).getWcitiesId(), this);

		artistRVAdapter.setLoadDateWiseEvents(loadArtistEvents);
		AsyncTaskUtil.executeAsyncTask(loadArtistEvents, true);
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
	
	private void onPushedToBackStack(boolean revertToolbarStatusBarChanges) {
		if (revertToolbarStatusBarChanges) {
			onStop();
			
		} else {
			/**
			 * to remove facebook callback. Not calling onStop() to prevent toolbar color changes occurring in between
			 * the transition
			 */
			super.onStop();
		}
		
		/**
		 * set null listener, otherwise even for event/venue details screen when selecting 
		 * share option it calls this listener's onShareTargetSelected() method.
		 */
		if (mShareActionProvider != null) {
			mShareActionProvider.setOnShareTargetSelectedListener(null);
		}
		setMenuVisibility(false);
		isOnPushedToBackStackCalled = true;
	}

	@Override
	public void onPushedToBackStack() {
		onPushedToBackStack(false);
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
	public void onPublishPermissionGranted() {
		if (!isArtistSaveClicked) {
			artistRVAdapter.onPublishPermissionGranted();
		}
	}

	@Override
	public void call(Session session, SessionState state, Exception exception) {
		if (isArtistSaveClicked) {
			fbCallCountForSameArtist++;
			/**
			 * To prevent infinite loop when network is off & we are calling requestPublishPermissions() of FbUtil.
			 */
			if (fbCallCountForSameArtist < AppConstants.MAX_FB_CALL_COUNT_FOR_SAME_EVT_OR_ART) {
				FbUtil.call(session, state, exception, this, this, AppConstants.PERMISSIONS_FB_PUBLISH_EVT_OR_ART, 
						AppConstants.REQ_CODE_FB_PUBLISH_EVT_OR_ART, artist);
				
			} else {
				fbCallCountForSameArtist = 0;
				setPendingAnnounce(false);
			}
			
		} else {
			artistRVAdapter.call(session, state, exception);
		}
	}

	@Override
	public void onArtistTracking(Context context, Artist artist) {
		EventSeekr eventseekr = FragmentUtil.getApplication(this);
		if (artist.getAttending() == Artist.Attending.NotTracked) {
			artist.updateAttending(Artist.Attending.Tracked, eventseekr);
			new UserTracker(Api.OAUTH_TOKEN, eventseekr, UserTrackingItemType.artist, artist.getId()).execute();
			fabSave.setSelected(artist.getAttending() == Artist.Attending.Tracked);
			
			Resources res = FragmentUtil.getResources(this);
			/*GeneralDialogFragment generalDialogFragment = GeneralDialogFragment.newInstance(this, 
					res.getString(R.string.follow_artist), res.getString(R.string.artist_saved));
			generalDialogFragment.show(((ActionBarActivity) FragmentUtil.getActivity(this)).getSupportFragmentManager(), 
					FRAGMENT_TAG_ARTIST_SAVED_DIALOG);*/			
			ShareOnFBDialogFragment shareOnFbDialog = ShareOnFBDialogFragment.newInstance(this);
			shareOnFbDialog.show(((ActionBarActivity) FragmentUtil.getActivity(this)).getSupportFragmentManager(), 
						FRAGMENT_TAG_ARTIST_SAVED_DIALOG);
			
		} else {			
			artist.updateAttending(Artist.Attending.NotTracked, eventseekr);
			new UserTracker(Api.OAUTH_TOKEN, eventseekr, UserTrackingItemType.artist, artist.getId(), 
					Artist.Attending.NotTracked.getValue(), UserTrackingType.Edit).execute();
		}
	}

	@Override
	public void doPositiveClick(String dialogTag) {
		//This is for Remove Artist Dialog
		if (dialogTag.equals(FRAGMENT_TAG_REMOVE_ARTIST_DIALOG)) {
			onArtistTracking(FragmentUtil.getApplication(this), artist);
			fabSave.setSelected(artist.getAttending() == Artist.Attending.Tracked);
		}
	}

	@Override
	public void doNegativeClick(String dialogTag) {
		/*if (dialogTag.equals(FRAGMENT_TAG_ARTIST_SAVED_DIALOG)) {
			isArtistSaveClicked = true;
			fbCallCountForSameArtist = 0;
			FbUtil.handlePublishArtist(this, this, AppConstants.PERMISSIONS_FB_PUBLISH_EVT_OR_ART, 
					AppConstants.REQ_CODE_FB_PUBLISH_EVT_OR_ART, artist);
		}*/
	}

	@Override
	public void onArtistEventsLoaded() {
		/*Log.d(TAG, "top = " + recyclerVArtists.getLayoutManager().findViewByPosition(1).getTop() 
				+ ", totalScrolledDy = " + totalScrolledDy + ", diff = " + (imgArtistHt - recyclerVArtists.getLayoutManager().findViewByPosition(1).getTop()));*/
		//totalScrolledDy = imgArtistHt - recyclerVArtists.getLayoutManager().getChildAt(0).getTop();
		handler.post(new Runnable() {
			
			@Override
			public void run() {
				onScrolled(0, true);
			}
		});
	}

	@Override
	public void onPushedToBackStackFHFIR() {
		onPushedToBackStack(true);
	}

	@Override
	public void onPoppedFromBackStackFHFIR() {
		onPoppedFromBackStack();
	}
	
	public void onFacebookShareClicked(String dialogTag) {
		if (dialogTag.equals(FRAGMENT_TAG_ARTIST_SAVED_DIALOG)) {
			isArtistSaveClicked = true;
			fbCallCountForSameArtist = 0;
			FbUtil.handlePublishArtist(this, this, AppConstants.PERMISSIONS_FB_PUBLISH_EVT_OR_ART, 
					AppConstants.REQ_CODE_FB_PUBLISH_EVT_OR_ART, artist);
		}
	}
}
