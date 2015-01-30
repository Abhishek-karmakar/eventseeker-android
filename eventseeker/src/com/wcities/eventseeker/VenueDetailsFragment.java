package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils.TruncateAt;
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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.melnykov.fab.FloatingActionButton;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.Animator.AnimatorListener;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.view.ViewHelper;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.asynctask.LoadVenueDetails;
import com.wcities.eventseeker.asynctask.LoadVenueDetails.OnVenueUpdatedListener;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.CustomSharedElementTransitionDestination;
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.VersionUtil;
import com.wcities.eventseeker.util.ViewUtil;
import com.wcities.eventseeker.viewdata.SharedElement;
import com.wcities.eventseeker.viewdata.SharedElementPosition;

public class VenueDetailsFragment extends FragmentLoadableFromBackStack implements DrawerListener, 
		CustomSharedElementTransitionDestination, OnVenueUpdatedListener {

	private static final String TAG = VenueDetailsFragment.class.getSimpleName();
	
	private static final int UNSCROLLED = -1;
	private static final int TRANSITION_ANIM_DURATION = 400;
			
	private Venue venue;
	private LoadVenueDetails loadVenueDetails;
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
			
			venueRVAdapter = new VenueRVAdapter(this, eventList, venue);
			
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
	
	private void onScrolled(int dy, boolean forceUpdate) {
		//Log.d(TAG, "dy = " + dy);
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		
		if (totalScrolledDy == UNSCROLLED) {
			totalScrolledDy = 0;
		}
		totalScrolledDy += dy;
		
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
		        asyncLoadImg.loadImg(imgVenue, ImgResolution.LOW, venue);
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
		recyclerVVenues.startAnimation(slideOutToBottom);
    }

	@Override
	public void onVenueUpdated() {
		allDetailsLoaded = true;
		updateVenueImg();
		venueRVAdapter.notifyDataSetChanged();
		//updateShareIntent();
	}
	
	private static class VenueRVAdapter extends RecyclerView.Adapter<VenueRVAdapter.ViewHolder> implements 
			DateWiseEventParentAdapterListener {
		
		private static final int EXTRA_TOP_DUMMY_ITEM_COUNT = 2;
		private static final int EXTRA_TOP_DUMMY_ITEM_COUNT_AFTER_DETAILS_LOADED = 3;
		private static final int INVALID = -1;
		private static final int MAX_LINES_VENUE_DESC = 5;
		
		private RecyclerView recyclerView;
		private VenueDetailsFragment venueDetailsFragment;
		
		private boolean isVenueDescExpanded;
		private List<Event> eventList;
		private Venue venue;
		
		private int openPos = INVALID;
		private int rltLytContentInitialMarginL, lnrSliderContentW, imgEventW, rltLytContentW = INVALID;
		
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
		
		public VenueRVAdapter(VenueDetailsFragment venueDetailsFragment, List<Event> eventList, Venue venue) {
			this.venueDetailsFragment = venueDetailsFragment;
			this.eventList = eventList;
			this.venue = venue;
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
				
			case PROGRESS:
				v = LayoutInflater.from(parent.getContext()).inflate(R.layout.progress_bar_eventseeker_fixed_ht, parent, 
						false);
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
								venueDetailsFragment.totalScrolledDy = venueDetailsFragment.imgVenueHt - holder.itemView.getTop();
								venueDetailsFragment.onScrolled(0, true);
				            }
				        });
						
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
		
		// to update values which should change on orientation change
		private void onActivityCreated() {
			rltLytContentW = INVALID;
			Resources res = FragmentUtil.getResources(venueDetailsFragment);
			rltLytContentInitialMarginL = res.getDimensionPixelSize(R.dimen.rlt_lyt_content_margin_l_list_item_discover);
			lnrSliderContentW = res.getDimensionPixelSize(R.dimen.lnr_slider_content_w_list_item_discover);
		}

		@Override
		public int getEventsAlreadyRequested() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void setMoreDataAvailable(boolean isMoreDataAvailable) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setEventsAlreadyRequested(int eventsAlreadyRequested) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void updateContext(Context context) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setLoadDateWiseEvents(
				AsyncTask<Void, Void, List<Event>> loadDateWiseEvents) {
			// TODO Auto-generated method stub
			
		}
	}
}
