package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;
import com.wcities.eventseeker.adapter.CatTitlesAdapter;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Category;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.custom.view.RecyclerViewInterceptingVerticalScroll;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.VersionUtil;

public class DiscoverFragment extends FragmentLoadableFromBackStack {
	
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
	
	private int toolbarSize;
	private int limitScrollAt;
	private float translationZPx;
	private boolean isScrollLimitReached, isDrawerOpen;
	private String title = "";
	protected List<Category> evtCategories;
	private int totalScrolledDy = UNSCROLLED; // indicates layout not yet created
	
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
		
		if (evtCategories == null) {
			buildEvtCategories();
		}
		
		catTitlesAdapter = new CatTitlesAdapter(getChildFragmentManager(), vPagerCatTitles, evtCategories, 
				imgCategory);
		imgCategory.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				vPagerCatTitles.onTouchEvent(event);
				return true;
			}
		});
		
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
		
	    if (eventListAdapter == null) {
	    	eventListAdapter = new EventListAdapter();
	    }
	    recyclerVEvents.setAdapter(eventListAdapter);
		
	    recyclerVEvents.setOnScrollListener(new RecyclerView.OnScrollListener() {
	    	
	    	@Override
	    	public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
	    		super.onScrolled(recyclerView, dx, dy);
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
		
		return v;
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
	
	private class EventListAdapter extends RecyclerView.Adapter<EventListAdapter.ViewHolder> {
		
		private class ViewHolder extends RecyclerView.ViewHolder {
			
			public View root;
	        public TextView mTextView;
	        
	        public ViewHolder(View root) {
	            super(root);
	            this.root = root;
	            mTextView = (TextView) root.findViewById(android.R.id.text1);
	        }
	    }
		
		@Override
		public int getItemViewType(int position) {
			//Log.d(TAG, "getItemViewType() - pos = " + position);
			if (position == 0) {
				return 0;
				
			} else if (position == 1) {
				return 1;
				
			} else {
				return 2;
			}
		}

		@Override
		public int getItemCount() {
			//Log.d(TAG, "getItemCount()");
			return values.length + 2;
		}

		@Override
		public void onBindViewHolder(ViewHolder holder, int position) {
			//Log.d(TAG, "onBindViewHolder(), pos = " + position);
			if (position != 0 && position != 1) {
				holder.mTextView.setText(values[position - 2]);
			}
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			//Log.d(TAG, "onCreateViewHolder(), viewType = " + viewType);
			View v = null;
			switch (viewType) {
			
			case 0:
				v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_child_cat_title_top, 
						parent, false);
				v.setOnTouchListener(new OnTouchListener() {
					
					@Override
					public boolean onTouch(View v, MotionEvent event) {
						//Log.d(TAG, "child 0 onTouch()");
						// Scroll the category titles
						vPagerCatTitles.onTouchEvent(event);
						return true;
					}
				});
				break;
				
			case 1:
				v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_child_cat_title, 
						parent, false);
				break;
				
			case 2:
				v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, 
						parent, false);
				v.setBackgroundColor(FragmentUtil.getResources(DiscoverFragment.this).getColor(android.R.color.black));
				break;

			default:
				break;
			}
			
			ViewHolder vh = new ViewHolder(v);
	        return vh;
		}
	}
}