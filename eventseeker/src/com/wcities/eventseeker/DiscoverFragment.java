package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.LayoutParams;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
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
import android.widget.TextView;

import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;
import com.wcities.eventseeker.DiscoverSettingDialogFragment.OnDateSelectedListener;
import com.wcities.eventseeker.adapter.CatTitlesAdapter;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.asynctask.LoadEvents;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Category;
import com.wcities.eventseeker.core.Date;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Schedule;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.custom.view.RecyclerViewInterceptingVerticalScroll;
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener;
import com.wcities.eventseeker.interfaces.EventListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.VersionUtil;
import com.wcities.eventseeker.util.ViewUtil;

public class DiscoverFragment extends FragmentLoadableFromBackStack implements LoadItemsInBackgroundListener, 
		OnDateSelectedListener {
	
	private static final String TAG = DiscoverFragment.class.getSimpleName();
	
	private static final String FRAGMENT_TAG_DISCOVER_SETTING_DIALOG = DiscoverSettingDialogFragment.class.getSimpleName();
	private static final int TRANSLATION_Z_DP = 10;
	private static final int UNSCROLLED = -1;
	
	private ImageView imgCategory;
	private ViewPager vPagerCatTitles;
	/**
	 * Used RecyclerViewInterceptingVerticalScroll in place of RecyclerView since we want to intercept only
	 * vertical scroll events; otherwise horizontal must be handled by its child as done for child at 
	 * position 0 (which is overlapping category image view)
	 */
	private RecyclerViewInterceptingVerticalScroll recyclerVEvents;
	private EventListAdapter eventListAdapter;
	private CatTitlesAdapter catTitlesAdapter;
	//private View vDummy;
	
	private int toolbarSize;
	private int limitScrollAt, screenHt, minRecyclerVHt, recyclerVDummyTopViewsHt, recyclerVPrgsBarHt, recyclerVContentRowHt;
	private float translationZPx;
	private boolean isScrollLimitReached, isDrawerOpen;
	private String title = "";
	private List<Category> evtCategories;
	private int totalScrolledDy = UNSCROLLED; // indicates layout not yet created
	private List<Event> eventList;
	private double lat, lon;
	private int year, month, day;
	private String startDate, endDate;
	private LoadEvents loadEvents;
	private int selectedCatId, currentItem = CatTitlesAdapter.FIRST_PAGE - 1;
	private Handler handler;
	private int firstItemHtDiff, firstItemHtPort, firstItemHtLand, prevOrientation = Configuration.ORIENTATION_UNDEFINED;
	
	private final HashMap<Integer, Integer> categoryImgs = new HashMap<Integer, Integer>() {
		{
			put(AppConstants.CATEGORY_ID_START, R.drawable.cat_900);
			put(AppConstants.CATEGORY_ID_START + 1, R.drawable.cat_901);
			put(AppConstants.CATEGORY_ID_START + 2, R.drawable.cat_902);
			put(AppConstants.CATEGORY_ID_START + 3, R.drawable.cat_903);
			put(AppConstants.CATEGORY_ID_START + 4, R.drawable.cat_904);
			put(AppConstants.CATEGORY_ID_START + 5, R.drawable.cat_905);
			put(AppConstants.CATEGORY_ID_START + 6, R.drawable.cat_906);
			put(AppConstants.CATEGORY_ID_START + 7, R.drawable.cat_907);
			put(AppConstants.CATEGORY_ID_START + 8, R.drawable.cat_908);
			put(AppConstants.CATEGORY_ID_START + 9, R.drawable.cat_909);
			put(AppConstants.CATEGORY_ID_START + 10, R.drawable.cat_910);
			put(AppConstants.CATEGORY_ID_START + 11, R.drawable.cat_911);
		}
	};
	
	/*View.OnTouchListener vDummyOnTouchListener = new OnTouchListener() {
		
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			Log.d(TAG, "onTouch() - event = " + event.getAction());
			vPagerCatTitles.onTouchEvent(event);
			recyclerVEvents.onTouchEvent(event);
	        return true;
		}
	};*/
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//Log.d(TAG, "onCreate()");
		setHasOptionsMenu(true);
		setRetainInstance(true);
		
		Resources res = FragmentUtil.getResources(this);
		translationZPx = ConversionUtil.toPx(res, TRANSLATION_Z_DP);
		handler = new Handler(Looper.getMainLooper());
		
		firstItemHtPort = res.getDimensionPixelSize(R.dimen.v_pager_cat_titles_margin_t_discover_port);
		firstItemHtLand = res.getDimensionPixelSize(R.dimen.v_pager_cat_titles_margin_t_discover_land);
		firstItemHtDiff = firstItemHtPort - firstItemHtLand;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// calculating here instead of onCreate() since it needs to be recalculated on orientation change
		calculateHeights();
		final int orientation = FragmentUtil.getResources(this).getConfiguration().orientation;
		
		if (year == 0) {
        	// initialize year, month, day values for setting action item
      		Calendar c = Calendar.getInstance();
      		year = c.get(Calendar.YEAR);
      		month = c.get(Calendar.MONTH);
      		day = c.get(Calendar.DAY_OF_MONTH);
      		updateStartEndDates();
        }
		
		View v = inflater.inflate(R.layout.fragment_discover, container, false);
		
		imgCategory = (ImageView) v.findViewById(R.id.imgCategory);
		vPagerCatTitles = (ViewPager) v.findViewById(R.id.vPagerCatTitles);
		/*vDummy = v.findViewById(R.id.vDummy);
		vDummy.setOnTouchListener(vDummyOnTouchListener);*/
		
		if (evtCategories == null) {
			buildEvtCategories();
		}
		
		/**
		 * creating this adapter everytime, since otherwise reusing same adapter instance on orientation change
		 * & updating just the viewpager instance on adapter doesn't inflate the titles at all
		 */
		catTitlesAdapter = new CatTitlesAdapter(getChildFragmentManager(), vPagerCatTitles, evtCategories, 
				this);
		
		vPagerCatTitles.setAdapter(catTitlesAdapter);
		vPagerCatTitles.setOnPageChangeListener(catTitlesAdapter);
		
		// Set current item to the middle page so we can fling to both
		// directions left and right
		vPagerCatTitles.setCurrentItem(currentItem);
		
		// Necessary or the pager will only have one extra page to show
		// make this at least however many pages you can see
		vPagerCatTitles.setOffscreenPageLimit(9);
		
		recyclerVEvents = (RecyclerViewInterceptingVerticalScroll) v.findViewById(R.id.recyclerVEvents);
		
		// use a linear layout manager
		RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(FragmentUtil.getActivity(this));
		recyclerVEvents.setLayoutManager(layoutManager);
		
	    recyclerVEvents.setOnScrollListener(new RecyclerView.OnScrollListener() {
	    	
	    	@Override
	    	public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
	    		super.onScrolled(recyclerView, dx, dy);
	    		//Log.d(TAG, "onScrolled - dx = " + dx + ", dy = " + dy);
	    		DiscoverFragment.this.onScrolled(dy, false);
	    	}
		});
	    
	    recyclerVEvents.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
            	//Log.d(TAG, "onGlobalLayout()");
				if (VersionUtil.isApiLevelAbove15()) {
					recyclerVEvents.getViewTreeObserver().removeOnGlobalLayoutListener(this);

				} else {
					recyclerVEvents.getViewTreeObserver().removeGlobalOnLayoutListener(this);
				}
				
				if (prevOrientation != Configuration.ORIENTATION_UNDEFINED) {
					// screen was already present before
					if (prevOrientation != orientation) {
						/**
						 * Since orientation is changed & user has scrolled past first dummy item 
						 * (list_child_cat_title_top), we need to update totalScrolledDy value depending 
						 * on current orientation, because first item height is different in both 
						 * orientations & recyclerview by default will just retain first visible position
						 * number & its offset
						 */
						if (prevOrientation == Configuration.ORIENTATION_PORTRAIT && 
								totalScrolledDy >= firstItemHtPort) {
							totalScrolledDy -= firstItemHtDiff;
							//Log.d(TAG, "update totalScrolledDy = " + totalScrolledDy);
							
						} else if (prevOrientation == Configuration.ORIENTATION_LANDSCAPE && 
								totalScrolledDy >= firstItemHtLand) {
							totalScrolledDy += firstItemHtDiff;
							//Log.d(TAG, "update totalScrolledDy = " + totalScrolledDy);
						}
					}
				}
				prevOrientation = orientation;

				onScrolled(0, true);
				if (isDrawerOpen) {
					// to maintain status bar & toolbar decorations after orientation change
					onDrawerOpened();
				}
            }
        });
	    
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (eventList == null) {
			double[] latLon = DeviceUtil.getLatLon(FragmentUtil.getApplication(this));
			lat = latLon[0];
			lon = latLon[1];
			
			eventList = new ArrayList<Event>();
			eventList.add(null);
			
			eventListAdapter = new EventListAdapter(null, eventList, this, this);
			
		} else {
			// to update values which should change on orientation change
			eventListAdapter.onActivityCreated();
		}
		
		recyclerVEvents.setAdapter(eventListAdapter);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		//Log.d(TAG, "onStart()");
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this); 
		ma.setVStatusBarVisibility(View.GONE);
		ma.setVDrawerStatusBarVisibility(View.VISIBLE);
		
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
		/**
		 * Revert toolbar & layered status bar updates here itself.
		 * We prefer reverting these changes here itself rather than applying updates for each screen 
		 * depending on specific requirement, since these are the changes applied to very small
		 * number of screens & hence no need to update these effects on every screen after reverting these here.
		 */
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		ma.setToolbarBg(ma.getResources().getColor(R.color.colorPrimary));
		ma.setToolbarElevation(ma.getResources().getDimensionPixelSize(R.dimen.action_bar_elevation));
		ma.setVStatusBarLayeredVisibility(View.GONE);
		ma.setVDrawerStatusBarVisibility(View.GONE);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_discover, menu);
    	super.onCreateOptionsMenu(menu, inflater);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.action_setting:
			DialogFragment newFragment = new DiscoverSettingDialogFragment();
			
			Bundle args = new Bundle();
			args.putInt(BundleKeys.YEAR, year);
			args.putInt(BundleKeys.MONTH, month);
			args.putInt(BundleKeys.DAY, day);
			newFragment.setArguments(args);
			
		    newFragment.show(getChildFragmentManager(), FRAGMENT_TAG_DISCOVER_SETTING_DIALOG);
			return true;

		default:
			break;
		}

		return super.onOptionsItemSelected(item);
	}
	
	private void calculateHeights() {
		DisplayMetrics dm = new DisplayMetrics();
		FragmentUtil.getActivity(this).getWindowManager().getDefaultDisplay().getMetrics(dm);
		screenHt = VersionUtil.isApiLevelAbove18() ? dm.heightPixels : dm.heightPixels - 
				((MainActivity) FragmentUtil.getActivity(this)).getStatusBarHeight();
		
		Resources res = FragmentUtil.getResources(this);
		recyclerVDummyTopViewsHt = res.getDimensionPixelSize(R.dimen.v_pager_cat_titles_margin_t_discover) + 
				res.getDimensionPixelSize(R.dimen.v_pager_cat_titles_ht_discover);
		recyclerVPrgsBarHt = res.getDimensionPixelSize(R.dimen.rlt_lyt_root_ht_progress_bar_eventseeker_fixed_ht);
		recyclerVContentRowHt = res.getDimensionPixelSize(R.dimen.rlt_layout_root_ht_list_item_discover);
		
		if (limitScrollAt != 0) {
			// this is not the first call to onCreateView(); otherwise limitScrollAt would be 0
			minRecyclerVHt = screenHt + limitScrollAt;
		}
	}
	
	private void buildEvtCategories() {
		evtCategories = new ArrayList<Category>();
		int categoryIdStart = AppConstants.CATEGORY_ID_START;
		String[] categoryNames = getResources().getStringArray(R.array.evt_category_titles);
		for (int i = 0; i < AppConstants.TOTAL_CATEGORIES; i++) {
			evtCategories.add(new Category(categoryIdStart + i, categoryNames[i]));
		}
	}
	
	private void calculateScrollLimit() {
		toolbarSize = FragmentUtil.getResources(this).getDimensionPixelSize(R.dimen.action_bar_ht);
		int initialPagerTop = FragmentUtil.getResources(this).getDimensionPixelSize(R.dimen.v_pager_cat_titles_margin_t_discover);
		limitScrollAt = initialPagerTop - toolbarSize;
		
		if (VersionUtil.isApiLevelAbove18()) {
			limitScrollAt -= ((MainActivity) FragmentUtil.getActivity(this)).getStatusBarHeight();
		}
		
		minRecyclerVHt = screenHt + limitScrollAt;
		// to decide dummy foorter view ht based on above value of minRecyclerVHt
		eventListAdapter.notifyDataSetChanged();
		//Log.d(TAG, "screenHt = " + screenHt + ", limitScrollAt = " + limitScrollAt);
	}
	
	private void onScrolled(int dy, boolean forceUpdate) {
		//Log.d(TAG, "totalScrolledDy = " + totalScrolledDy);
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		
		if (totalScrolledDy == UNSCROLLED) {
			totalScrolledDy = 0;
		}
		totalScrolledDy += dy;
		
		// Translate image
		ViewHelper.setTranslationY(imgCategory, (0 - totalScrolledDy) / 2);
		
		// Translate tabs
		if (limitScrollAt == 0) {
			calculateScrollLimit();
			//Log.d(TAG, "vPagerCatTitles.getTop() = " + vPagerCatTitles.getTop() + ", toolbarSize = " + toolbarSize + ", ma.getStatusBarHeight() = " + ma.getStatusBarHeight());
		}
		
		int scrollY = (totalScrolledDy >= limitScrollAt) ? limitScrollAt : totalScrolledDy;
		//Log.d(TAG, "totalScrolledDy = " + totalScrolledDy + ", limitScrollAt = " + limitScrollAt + ", scrollY = " + scrollY);
		/**
		 * Using layout parameters instead of setTranslationY(), since tabs scrolling doesn't work properly
		 * after applying setTranslationY()
		 */
		FrameLayout.LayoutParams frameLParams = (FrameLayout.LayoutParams) vPagerCatTitles.getLayoutParams();
		frameLParams.topMargin = FragmentUtil.getResources(this).getDimensionPixelSize(R.dimen.v_pager_cat_titles_margin_t_discover) 
				- scrollY;
		vPagerCatTitles.setLayoutParams(frameLParams);
		
		/*frameLParams = (FrameLayout.LayoutParams) vDummy.getLayoutParams();
		frameLParams.topMargin = 0 - scrollY;
		vDummy.setLayoutParams(frameLParams);*/
		
		if ((!isScrollLimitReached || forceUpdate) && totalScrolledDy >= limitScrollAt) {
			ObjectAnimator elevateAnim = ObjectAnimator.ofFloat(vPagerCatTitles, "translationZ", 0.0f, translationZPx);
			elevateAnim.setDuration(100);
			elevateAnim.start();
			
			ma.setVStatusBarLayeredVisibility(View.VISIBLE);
			ma.setVStatusBarLayeredColor(R.color.colorPrimaryDark);
			ma.setToolbarBg(ma.getResources().getColor(R.color.colorPrimary));
			vPagerCatTitles.setBackgroundColor(ma.getResources().getColor(R.color.colorPrimary));
			ma.setToolbarElevation(0);
			
			title = ma.getResources().getString(R.string.title_discover);
			ma.updateTitle(title);
			
			isScrollLimitReached = true;
			
		} else if ((isScrollLimitReached || forceUpdate) && totalScrolledDy < limitScrollAt) {
			//Log.d(TAG, "totalScrolledDy < limitScrollAt");
			ObjectAnimator elevateAnim = ObjectAnimator.ofFloat(vPagerCatTitles, "translationZ", translationZPx, 0.0f);
			elevateAnim.setDuration(100);
			elevateAnim.start();
			
			ma.setVStatusBarLayeredVisibility(View.GONE);
			ma.setToolbarBg(Color.TRANSPARENT);
			vPagerCatTitles.setBackgroundResource(R.drawable.bg_v_pager_cat_titles);
			ma.setToolbarElevation(ma.getResources().getDimensionPixelSize(R.dimen.action_bar_elevation));
			
			title = "";
			ma.updateTitle(title);
			
			isScrollLimitReached = false;
		}
	}
	
	private void resetEventList() {
		if (eventListAdapter == null) {
			return;
		}
		//Log.d(TAG, "resetEventList()");
		eventListAdapter.reset();

		if (loadEvents != null) {
			loadEvents.cancel(true);
		}

		eventList.clear();
		eventList.add(null);
		
		eventListAdapter.notifyDataSetChanged();
		
		// reset totalScrolledDy amount
		totalScrolledDy = (totalScrolledDy > limitScrollAt) ? limitScrollAt : totalScrolledDy;
		/**
		 * Only call to notifyDataSetChanged() is not enough to retain scrolled position, because it's possible that
		 * we had many events earlier, but now on resetting the list there are no events, hence 
		 * following manual scrolling to right position is required
		 */
		((LinearLayoutManager)recyclerVEvents.getLayoutManager()).scrollToPositionWithOffset(0, 0 - totalScrolledDy);

		loadItemsInBackground();
	}
	
	public String getCurrentTitle() {
		return title;
	}

	public void onDrawerOpened() {
		//Log.d(TAG, "onDrawerOpened()");
		isDrawerOpen = true;
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		ma.setToolbarBg(ma.getResources().getColor(R.color.colorPrimary));
		ma.setToolbarElevation(ma.getResources().getDimensionPixelSize(R.dimen.action_bar_elevation));
		ma.setVStatusBarLayeredVisibility(View.VISIBLE);
		ma.setVStatusBarLayeredColor(R.color.colorPrimaryDark);
		title = ma.getResources().getString(R.string.title_discover);
		ma.updateTitle(title);
	}
	
	public void onDrawerClosed(View view) {
		//Log.d(TAG, "onDrawerClosed()");
		isDrawerOpen = false;
		onScrolled(0, true);
	}
	
	public void onDrawerSlide(View drawerView, float slideOffset) {
		//Log.d(TAG, "onDrawerSlide(), slideOffset = " + slideOffset);
		if (!isScrollLimitReached) {
			((MainActivity)FragmentUtil.getActivity(this)).updateToolbarOnDrawerSlide(slideOffset);
		}
	}
	
	public void onCatTitleClicked(int item) {
		if (item - 1 >= 0) {
			vPagerCatTitles.setCurrentItem(item - 1, true);
		}
	}
	
	@Override
	public String getScreenName() {
		return "Discover Screen";
	}
	
	@Override
	public void loadItemsInBackground() {
		loadEvents = new LoadEvents(Api.OAUTH_TOKEN, eventList, eventListAdapter, lat, lon, startDate, endDate, 
				catTitlesAdapter.getSelectedCatId(), ((EventSeekr)FragmentUtil.getActivity(this).getApplicationContext()).getWcitiesId());
		eventListAdapter.setLoadDateWiseEvents(loadEvents);
		AsyncTaskUtil.executeAsyncTask(loadEvents, true);
	}
	
	public void onCatChanged(int selectedCatId) {
		//Log.d(TAG, "onCatChanged(), selectedCatId = " + selectedCatId);
		currentItem = vPagerCatTitles.getCurrentItem();
		if (this.selectedCatId != selectedCatId) {
			this.selectedCatId = selectedCatId;
			imgCategory.setImageResource(categoryImgs.get(selectedCatId));
			resetEventList();
		}
	}
	
	public void onTouchRecyclerViewDummyItem0(MotionEvent event) {
		vPagerCatTitles.onTouchEvent(event);
	}
	
	private int getRecyclerVHtExcludingFooter() {
		int recyclerVHt = recyclerVDummyTopViewsHt;
		if (eventList.size() > 0 && eventList.get(eventList.size() - 1) == null) {
			// progress bar is displayed
			recyclerVHt += (recyclerVContentRowHt * (eventList.size() - 1));
			recyclerVHt += recyclerVPrgsBarHt;
			
		} else {
			recyclerVHt += (recyclerVContentRowHt * eventList.size());
		}
		
		//Log.d(TAG, "recyclerVHt = " + recyclerVHt);
		return recyclerVHt;
	}
	
	private void updateStartEndDates() {
		Calendar calendar = new GregorianCalendar(year, month, day);
	    startDate = ConversionUtil.getDay(calendar);
	    
	    calendar.add(Calendar.YEAR, 1);
	    endDate = ConversionUtil.getDay(calendar);
	}
	
	@Override
	public void onDateSelected(int year, int month, int day) {
		this.year = year;
		this.month = month;
		this.day = day;

		String prevStartDate = startDate;
		updateStartEndDates();
		
	    if (!startDate.equals(prevStartDate)) {
			resetEventList();
		}
	}
	
	private static class EventListAdapter extends RecyclerView.Adapter<EventListAdapter.ViewHolder> implements 
			DateWiseEventParentAdapterListener {
		
		private static final int INVALID = -1;
		private static final int EXTRA_TOP_DUMMY_ITEM_COUNT = 2;
		private static final int EXTRA_BOTTOM_DUMMY_ITEM_COUNT = 1;
		
		private AsyncTask<Void, Void, List<Event>> loadDateWiseEvents;
		private List<Event> eventList;
		private int eventsAlreadyRequested;
		private boolean isMoreDataAvailable = true;
		private BitmapCache bitmapCache;
		private LoadItemsInBackgroundListener mListener;
		private DiscoverFragment discoverFragment;
		private RecyclerView recyclerView;
		private int openPos = INVALID;
		private int rltLytContentInitialMarginL, lnrSliderContentW, rltLytContentW = INVALID;
		
		private static enum ViewType {
			POS_0, POS_1, LAST_POS, PROGRESS, CONTENT
		};
		
		private static class ViewHolder extends RecyclerView.ViewHolder {
			
			private View root, vHandle;
	        private TextView txtEvtTitle, txtEvtTime, txtEvtLocation;
	        private ImageView imgEvent;
	        private LinearLayout lnrSliderContent;
	        private RelativeLayout rltLytRoot, rltLytContent;
	        
	        public ViewHolder(View root) {
	            super(root);
	            this.root = root;
	            txtEvtTitle = (TextView) root.findViewById(R.id.txtEvtTitle);
	            txtEvtTime = (TextView) root.findViewById(R.id.txtEvtTime);
	            txtEvtLocation = (TextView) root.findViewById(R.id.txtEvtLocation);
	            imgEvent = (ImageView) root.findViewById(R.id.imgEvent);
	            vHandle = root.findViewById(R.id.vHandle);
	            lnrSliderContent = (LinearLayout) root.findViewById(R.id.lnrSliderContent);
	            rltLytRoot = (RelativeLayout) root.findViewById(R.id.rltLytRoot);
	            rltLytContent = (RelativeLayout) root.findViewById(R.id.rltLytContent);
	        }
	        
	        private boolean isSliderClose(int rltLytContentInitialMarginL) {
	        	RelativeLayout.LayoutParams rltLytContentLP = (RelativeLayout.LayoutParams) rltLytContent.getLayoutParams();
				return (rltLytContentLP.leftMargin == rltLytContentInitialMarginL);
	        }
	    }
		
		public EventListAdapter(AsyncTask<Void, Void, List<Event>> loadDateWiseEvents, List<Event> eventList, 
				LoadItemsInBackgroundListener mListener, DiscoverFragment discoverFragment) {
			this.loadDateWiseEvents = loadDateWiseEvents;
			this.eventList = eventList;
			this.mListener = mListener;
			this.discoverFragment = discoverFragment;
			bitmapCache = BitmapCache.getInstance();
			Resources res = FragmentUtil.getResources(discoverFragment);
			rltLytContentInitialMarginL = res.getDimensionPixelSize(R.dimen.rlt_lyt_content_margin_l_list_item_discover);
			lnrSliderContentW = res.getDimensionPixelSize(R.dimen.lnr_slider_content_w_list_item_discover);
		}

		@Override
		public int getItemViewType(int position) {
			//Log.d(TAG, "getItemViewType() - pos = " + position);
			if (position == 0) {
				return ViewType.POS_0.ordinal();
				
			} else if (position == 1) {
				return ViewType.POS_1.ordinal();
				
			} else if (position == getItemCount() - 1) {
				return ViewType.LAST_POS.ordinal();
				
			} else if (eventList.get(position - EXTRA_TOP_DUMMY_ITEM_COUNT) == null) {
				return ViewType.PROGRESS.ordinal();
				
			} else {
				return ViewType.CONTENT.ordinal();
			}
		}

		@Override
		public int getItemCount() {
			//Log.d(TAG, "getItemCount() = " + (eventList.size() + EXTRA_DUMMY_ITEM_COUNT));
			return eventList.size() + EXTRA_TOP_DUMMY_ITEM_COUNT + EXTRA_BOTTOM_DUMMY_ITEM_COUNT;
		}

		@Override
		public void onBindViewHolder(final ViewHolder holder, final int position) {
			//Log.d(TAG, "onBindViewHolder(), pos = " + position);
			if (position == getItemCount() - 1) {
				RecyclerView.LayoutParams lp = (LayoutParams) holder.root.getLayoutParams();
				int recyclerVHtExcludingFooter = discoverFragment.getRecyclerVHtExcludingFooter();
				lp.height = (recyclerVHtExcludingFooter > discoverFragment.minRecyclerVHt) ?
						0 : discoverFragment.minRecyclerVHt - recyclerVHtExcludingFooter;
				//Log.d(TAG, "lp.ht = " + lp.height);
				holder.root.setLayoutParams(lp);
				
			} else if (position != 0 && position != 1) {
				final Event event = eventList.get(position - EXTRA_TOP_DUMMY_ITEM_COUNT);
				
				if (event == null) {
					// progress indicator
					holder.root.setVisibility(View.VISIBLE);
					
					if ((loadDateWiseEvents == null || loadDateWiseEvents.getStatus() == Status.FINISHED) && 
							isMoreDataAvailable) {
						//Log.d(TAG, "onBindViewHolder(), pos = " + position);
						mListener.loadItemsInBackground();
					}
					
				} else if (event.getId() == AppConstants.INVALID_ID) {
					// no events found
					
				} else {
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
					
					if (rltLytContentW == INVALID) {
						holder.rltLytContent.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
				            @Override
				            public void onGlobalLayout() {
								if (VersionUtil.isApiLevelAbove15()) {
									holder.rltLytContent.getViewTreeObserver().removeOnGlobalLayoutListener(this);

								} else {
									holder.rltLytContent.getViewTreeObserver().removeGlobalOnLayoutListener(this);
								}
								
								RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) holder.rltLytContent.getLayoutParams();
								rltLytContentW = lp.width = holder.rltLytContent.getWidth();
								holder.rltLytContent.setLayoutParams(lp);
								
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
						holder.rltLytContent.setLayoutParams(lp);
					}
					
					holder.rltLytRoot.setOnTouchListener(new OnTouchListener() {
						
						int MIN_SWIPE_DISTANCE_X = 50;
						int pointerX = 0, initX = 0;
						boolean isSliderOpenInititally, isScrolled;
						
						@Override
						public boolean onTouch(View v, MotionEvent mEvent) {
							RelativeLayout.LayoutParams rltLytContentLP = (RelativeLayout.LayoutParams) holder.rltLytContent.getLayoutParams();
							RelativeLayout.LayoutParams lnrSliderContentLP = (RelativeLayout.LayoutParams) holder.lnrSliderContent.getLayoutParams();
							
							switch (mEvent.getAction()) {
							
							case MotionEvent.ACTION_DOWN:
								//Log.d(TAG, "down");
								initX = pointerX = (int) mEvent.getRawX();
								isSliderOpenInititally = !holder.isSliderClose(rltLytContentInitialMarginL);
								isScrolled = false;
								return true;
							
							case MotionEvent.ACTION_MOVE:
								//Log.d(TAG, "move");
								holder.rltLytRoot.setPressed(true);
								isScrolled = true;
								holder.lnrSliderContent.setVisibility(View.VISIBLE);
								
								int newX = (int) mEvent.getRawX();
								int dx = newX - pointerX;
								
								int scrollX = rltLytContentLP.leftMargin - rltLytContentInitialMarginL + dx;
								//Log.d(TAG, "move, rltLytContentLP.leftMargin = " + rltLytContentLP.leftMargin + ", lnrDrawerContentW = " + lnrDrawerContentW);
								if (scrollX >= (0 - lnrSliderContentW) && scrollX <= 0) {
									rltLytContentLP.leftMargin = rltLytContentInitialMarginL + scrollX;
									holder.rltLytContent.setLayoutParams(rltLytContentLP);
									
									lnrSliderContentLP.rightMargin = rltLytContentInitialMarginL 
											- rltLytContentLP.leftMargin - lnrSliderContentW;
									holder.lnrSliderContent.setLayoutParams(lnrSliderContentLP);
									
									pointerX = newX;
								}
								
								break;
								
							case MotionEvent.ACTION_UP:
							case MotionEvent.ACTION_CANCEL:
								//Log.d(TAG, "up");
								if (!isScrolled) { 
									if (ViewUtil.isPointInsideView(mEvent.getRawX(), mEvent.getRawY(), holder.vHandle)) {
										onHandleClick(holder, position);

									} else if (ViewUtil.isPointInsideView(mEvent.getRawX(), mEvent.getRawY(), holder.rltLytRoot)) {
										onEventClick(holder, event);
									}
									break;
								}
								holder.rltLytRoot.setPressed(false);
								boolean isMinSwipeDistanceXTravelled = Math.abs(initX - pointerX) > MIN_SWIPE_DISTANCE_X;
								boolean isSwipedToOpen = (initX > pointerX);
								
								if (isMinSwipeDistanceXTravelled) {
									//Log.d(TAG, "isMinSwipeDistanceXTravelled");
									if (isSwipedToOpen) {
										Log.d(TAG, "isSwipedToOpen");
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
		
		private void openSlider(ViewHolder holder, int position, boolean isUserInitiated) {
			holder.lnrSliderContent.setVisibility(View.VISIBLE);
			
			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) holder.lnrSliderContent.getLayoutParams();
			lp.rightMargin = 0;
			holder.lnrSliderContent.setLayoutParams(lp);
			
			lp = (RelativeLayout.LayoutParams) holder.rltLytContent.getLayoutParams();
			lp.leftMargin = rltLytContentInitialMarginL - lnrSliderContentW;
			holder.rltLytContent.setLayoutParams(lp);
			
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
		
		private void reset() {
			openPos = INVALID;
			setEventsAlreadyRequested(0);
			setMoreDataAvailable(true);
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			//Log.d(TAG, "onCreateViewHolder(), viewType = " + viewType);
			View v;
			
			recyclerView = (RecyclerView) parent;
			
			switch (viewType) {
			
			case 0:
				v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_child_cat_title_top, 
						parent, false);
				v.setOnTouchListener(new OnTouchListener() {
					
					@Override
					public boolean onTouch(View v, MotionEvent event) {
						//Log.d(TAG, "child 0 onTouch()");
						// Scroll the category titles
						discoverFragment.onTouchRecyclerViewDummyItem0(event);
						return true;
					}
				});
				break;
				
			case 1:
				v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_child_cat_title, 
						parent, false);
				break;
				
			case 2:
				v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_child_dummy_footer, parent, 
						false);
				break;
				
			case 3:
				v = LayoutInflater.from(parent.getContext()).inflate(R.layout.progress_bar_eventseeker_fixed_ht, parent, 
						false);
				break;
				
			case 4:
				v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_discover, parent, false);
				break;
				
			default:
				v = null;
				break;
			}
			
			ViewHolder vh = new ViewHolder(v);
	        return vh;
		}
		
		private void onHandleClick(final ViewHolder holder, final int position) {
			//Log.d(TAG, "onHandleClick()");
			holder.vHandle.setPressed(true);
			
			if (holder.isSliderClose(rltLytContentInitialMarginL)) {
				// slider is close, so open it
				//Log.d(TAG, "open slider");
				holder.lnrSliderContent.setVisibility(View.VISIBLE);
				
				RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) holder.lnrSliderContent.getLayoutParams();
				lp.rightMargin = 0;
				holder.lnrSliderContent.setLayoutParams(lp);
				
				Animation slide = AnimationUtils.loadAnimation(FragmentUtil.getActivity(
						discoverFragment), R.anim.slide_in_from_left);
				slide.setAnimationListener(new AnimationListener() {
					
					@Override
					public void onAnimationStart(Animation animation) {
						/*ObjectAnimator elevateAnim = ObjectAnimator.ofFloat(holder.imgEvent, "translationZ", 
								0.0f, discoverFragment.translationZPx);
						elevateAnim.setDuration(android.R.integer.config_mediumAnimTime);
						elevateAnim.start();*/
						//ViewCompat.setElevation(holder.imgEvent, discoverFragment.translationZPx);
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
						
						updateOpenPos(position, recyclerView);
					}
				});
				holder.lnrSliderContent.startAnimation(slide);
				
			} else {
				// slider is open, so close it
				//Log.d(TAG, "close slider");
				Animation slide = AnimationUtils.loadAnimation(FragmentUtil.getActivity(
						discoverFragment), android.R.anim.slide_out_right);
				slide.setAnimationListener(new AnimationListener() {
					
					@Override
					public void onAnimationStart(Animation animation) {
						/*ObjectAnimator elevateAnim = ObjectAnimator.ofFloat(holder.imgEvent, "translationZ", 
								discoverFragment.translationZPx, 0.0f);
						elevateAnim.setDuration(android.R.integer.config_mediumAnimTime);
						elevateAnim.start();*/
					}
					
					@Override
					public void onAnimationRepeat(Animation animation) {}
					
					@Override
					public void onAnimationEnd(Animation animation) {
						//ViewCompat.setElevation(holder.imgEvent, 0);
						
						holder.vHandle.setPressed(false);
						
						holder.lnrSliderContent.setVisibility(View.INVISIBLE);
						RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) holder.lnrSliderContent.getLayoutParams();
						lp.rightMargin = 0 - lnrSliderContentW;;
						holder.lnrSliderContent.setLayoutParams(lp);
						
						lp = (RelativeLayout.LayoutParams) holder.rltLytContent.getLayoutParams();
						lp.leftMargin += holder.lnrSliderContent.getWidth();
						holder.rltLytContent.setLayoutParams(lp);
						
						updateOpenPos(INVALID, null);
					}
				});
				holder.lnrSliderContent.startAnimation(slide);
			}
		}
		
		private void onEventClick(final ViewHolder holder, final Event event) {
			holder.rltLytRoot.setPressed(true);
			discoverFragment.handler.postDelayed(new Runnable() {
				
				@Override
				public void run() {
					((EventListener) FragmentUtil.getActivity(discoverFragment)).onEventSelected(event);
					holder.rltLytRoot.setPressed(false);
				}
			}, 200);
		}
		
		// to update values which should change on orientation change
		private void onActivityCreated() {
			rltLytContentW = INVALID;
			Resources res = FragmentUtil.getResources(discoverFragment);
			rltLytContentInitialMarginL = res.getDimensionPixelSize(R.dimen.rlt_lyt_content_margin_l_list_item_discover);
			lnrSliderContentW = res.getDimensionPixelSize(R.dimen.lnr_slider_content_w_list_item_discover);
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
			this.loadDateWiseEvents = loadDateWiseEvents;
		}
	}
}