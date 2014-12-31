package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;
import com.wcities.eventseeker.adapter.CatTitlesAdapter;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.asynctask.LoadEvents;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Category;
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

public class DiscoverFragment extends FragmentLoadableFromBackStack implements LoadItemsInBackgroundListener {
	
	private static final String TAG = DiscoverFragment.class.getSimpleName();
	
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
    private RecyclerView.LayoutManager layoutManager;
	private CatTitlesAdapter catTitlesAdapter;
	private RelativeLayout prgsBarLyt;
	//private View vDummy;
	
	private int toolbarSize;
	private int limitScrollAt;
	private float translationZPx;
	private boolean isScrollLimitReached, isDrawerOpen;
	private String title = "";
	private List<Category> evtCategories;
	private int totalScrolledDy = UNSCROLLED; // indicates layout not yet created
	private List<Event> eventList;
	private double lat, lon;
	private String startDate, endDate;
	private LoadEvents loadEvents;
	
	String[] values = new String[] { "Android", "iPhone", "WindowsMobile",
	        "Blackberry", "WebOS", "Ubuntu", "Windows7", "Max OS X",
	        "Linux", "OS/2", "Ubuntu", "Windows7", "Max OS X", "Linux",
	        "OS/2", "Ubuntu", "Windows7", "Max OS X", "Linux", "OS/2",
	        "Android", "iPhone", "WindowsMobile", 
	        "Android", "iPhone", "WindowsMobile",
	        "Blackberry", "WebOS", "Ubuntu", "Windows7", "Max OS X",
	        "Linux", "OS/2", "Ubuntu", "Windows7", "Max OS X", "Linux",
	        "OS/2", "Ubuntu", "Windows7", "Max OS X", "Linux", "OS/2",
	        "Android", "iPhone", "WindowsMobile"};
	
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
		
		setHasOptionsMenu(true);
		setRetainInstance(true);
		
		translationZPx = ConversionUtil.toPx(FragmentUtil.getResources(this), TRANSLATION_Z_DP);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		//Log.d(TAG, "onCreateView()");
		View v = inflater.inflate(R.layout.fragment_discover, container, false);
		
		imgCategory = (ImageView) v.findViewById(R.id.imgCategory);
		vPagerCatTitles = (ViewPager) v.findViewById(R.id.vPagerCatTitles);
		/*vDummy = v.findViewById(R.id.vDummy);
		vDummy.setOnTouchListener(vDummyOnTouchListener);*/
		
		if (evtCategories == null) {
			buildEvtCategories();
		}
		
		catTitlesAdapter = new CatTitlesAdapter(getChildFragmentManager(), vPagerCatTitles, evtCategories, 
				imgCategory);
		
		vPagerCatTitles.setAdapter(catTitlesAdapter);
		vPagerCatTitles.setOnPageChangeListener(catTitlesAdapter);
		
		// Set current item to the middle page so we can fling to both
		// directions left and right
		vPagerCatTitles.setCurrentItem(CatTitlesAdapter.FIRST_PAGE - 1);
		
		// Necessary or the pager will only have one extra page to show
		// make this at least however many pages you can see
		vPagerCatTitles.setOffscreenPageLimit(9);
		
		recyclerVEvents = (RecyclerViewInterceptingVerticalScroll) v.findViewById(R.id.recyclerVEvents);
		
		// use a linear layout manager
		layoutManager = new LinearLayoutManager(FragmentUtil.getActivity(this));
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
				onScrolled(0, true);
				if (isDrawerOpen) {
					// to maintain status bar & toolbar decorations after orientation change
					onDrawerOpened();
				}
            }
        });
	    
	    prgsBarLyt = (RelativeLayout) v.findViewById(R.id.prgsBarLyt);
		
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (eventList == null) {
			double[] latLon = DeviceUtil.getLatLon(FragmentUtil.getApplication(this));
			lat = latLon[0];
			lon = latLon[1];
			
			Calendar c = Calendar.getInstance();
			startDate = ConversionUtil.getDay(c);
			
			c.add(Calendar.YEAR, 1);
			endDate = ConversionUtil.getDay(c);

			eventList = new ArrayList<Event>();
			eventList.add(null);
			
			eventListAdapter = new EventListAdapter(FragmentUtil.getActivity(this), null, eventList, 
					this, this);
			
			loadItemsInBackground();
			
		} else {
			eventListAdapter.updateContext(FragmentUtil.getActivity(this));
		}
		
		recyclerVEvents.setAdapter(eventListAdapter);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		//Log.d(TAG, "onStart(), prevScrollY = " + prevScrollY);
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
	
	public void onTouchRecyclerViewDummyItem0(MotionEvent event) {
		vPagerCatTitles.onTouchEvent(event);
	}
	
	public void setCenterProgressBarVisibility(int visibility) {
		prgsBarLyt.setVisibility(visibility);
	}
	
	private static class EventListAdapter extends RecyclerView.Adapter<EventListAdapter.ViewHolder> implements 
			DateWiseEventParentAdapterListener {
		
		private static final int EXTRA_DUMMY_ITEM_COUNT = 2;
		
		private Context mContext;
		private AsyncTask<Void, Void, List<Event>> loadDateWiseEvents;
		private List<Event> eventList;
		private int eventsAlreadyRequested;
		private boolean isMoreDataAvailable = true;
		private BitmapCache bitmapCache;
		private LoadItemsInBackgroundListener mListener;
		private DiscoverFragment discoverFragment;
		private RecyclerView recyclerView;
		
		private static enum ViewType {
			POS_0, POS_1, PROGRESS, CONTENT
		};
		
		private static class ViewHolder extends RecyclerView.ViewHolder {
			
			private View root;
	        private TextView txtEvtTitle, txtEvtTime, txtEvtLocation;
	        private ImageView imgEvtTime, imgEvent;
	        
	        public ViewHolder(View root) {
	            super(root);
	            this.root = root;
	            txtEvtTitle = (TextView) root.findViewById(R.id.txtEvtTitle);
	            txtEvtTime = (TextView) root.findViewById(R.id.txtEvtTime);
	            imgEvtTime = (ImageView) root.findViewById(R.id.imgEvtTime);
	            txtEvtLocation = (TextView) root.findViewById(R.id.txtEvtLocation);
	            imgEvent = (ImageView) root.findViewById(R.id.imgEvent);
	        }
	    }
		
		public EventListAdapter(Context mContext, AsyncTask<Void, Void, List<Event>> loadDateWiseEvents,
				List<Event> eventList, LoadItemsInBackgroundListener mListener, 
				DiscoverFragment discoverFragment) {
			this.mContext = mContext;
			this.loadDateWiseEvents = loadDateWiseEvents;
			this.eventList = eventList;
			this.mListener = mListener;
			this.discoverFragment = discoverFragment;
			bitmapCache = BitmapCache.getInstance();
		}

		@Override
		public int getItemViewType(int position) {
			//Log.d(TAG, "getItemViewType() - pos = " + position);
			if (position == 0) {
				return ViewType.POS_0.ordinal();
				
			} else if (position == 1) {
				return ViewType.POS_1.ordinal();
				
			} else if (eventList.get(position - EXTRA_DUMMY_ITEM_COUNT) == null) {
				return ViewType.PROGRESS.ordinal();
				
			} else {
				return ViewType.CONTENT.ordinal();
			}
		}

		@Override
		public int getItemCount() {
			//Log.d(TAG, "getItemCount() = " + (eventList.size() + EXTRA_DUMMY_ITEM_COUNT));
			return eventList.size() + EXTRA_DUMMY_ITEM_COUNT;
		}

		@Override
		public void onBindViewHolder(ViewHolder holder, int position) {
			//Log.d(TAG, "onBindViewHolder(), pos = " + position);
			if (position != 0 && position != 1) {
				final Event event = eventList.get(position - EXTRA_DUMMY_ITEM_COUNT);
				
				if (event == null) {
					// progress indicator
					if (eventList.size() == 1) {
						// no events yet loaded
						holder.root.setVisibility(View.INVISIBLE);
						discoverFragment.setCenterProgressBarVisibility(View.VISIBLE);
						 
					} else {
						// at least 1 event is there
						holder.root.setVisibility(View.VISIBLE);
					}
					
				} else if (event.getId() == AppConstants.INVALID_ID) {
					// no events found
					
				} else {
					holder.txtEvtTitle.setText(event.getName());
					
					if (event.getSchedule() != null) {
						Schedule schedule = event.getSchedule();
						
						if (schedule.getDates().get(0).isStartTimeAvailable()) {
							String time = ConversionUtil.getTime(schedule.getDates().get(0).getStartDate());
							
							holder.txtEvtTime.setText(time);
							holder.imgEvtTime.setVisibility(View.VISIBLE);
							
						} else {
							holder.txtEvtTime.setText("");
							holder.imgEvtTime.setVisibility(View.INVISIBLE);
						}
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
					
					holder.root.setOnClickListener(new OnClickListener() {
						
						@Override
						public void onClick(View v) {
							((EventListener) mContext).onEventSelected(event);
						}
					});
				}
			}
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			//Log.d(TAG, "onCreateViewHolder(), viewType = " + viewType);
			View v = null;
			
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
				v = LayoutInflater.from(parent.getContext()).inflate(R.layout.progress_bar_eventseeker, parent, 
						false);
				break;
				
			case 3:
				v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_discover_by_category_list_item_evt, 
						parent, false);
				break;
				
			default:
				break;
			}
			
			ViewHolder vh = new ViewHolder(v);
	        return vh;
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
			mContext = context;
		}

		@Override
		public void setLoadDateWiseEvents(AsyncTask<Void, Void, List<Event>> loadDateWiseEvents) {
			this.loadDateWiseEvents = loadDateWiseEvents;
		}

		@Override
		public void onEventLoadingFinished() {
			discoverFragment.setCenterProgressBarVisibility(View.INVISIBLE);
		}
	}
}