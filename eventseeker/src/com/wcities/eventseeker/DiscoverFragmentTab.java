package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.AsyncTask.Status;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;

import com.wcities.eventseeker.adapter.CatTitlesAdapterTab;
import com.wcities.eventseeker.adapter.RVCatEventsAdapterTab;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.LoadEvents;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Category;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.VersionUtil;
import com.wcities.eventseeker.viewdata.ItemDecorationItemOffset;

public class DiscoverFragmentTab extends Fragment implements OnClickListener, LoadItemsInBackgroundListener, 
		AsyncTaskListener<Void> {
	
	private static final String TAG = DiscoverFragmentTab.class.getSimpleName();
	
	private static final int GRID_COLUMNS_PORTRAIT = 2;
	private static final int GRID_COLUMNS_LANDSCAPE = 3;

	private List<Category> evtCategories;
	private LoadEvents loadEvents;
	private List<Event> eventList;
	private double lat, lon;
	
	private int year, month, day, miles = DiscoverFragment.DEFAULT_SEARCH_RADIUS;
	private String startDate, endDate;
	
	private RecyclerView recyclerVCategories, recyclerVEvents;
	private LinearLayoutManager layoutManager;
	
	private CatTitlesAdapterTab catTitlesAdapterTab;
	private RVCatEventsAdapterTab rvCatEventsAdapterTab;
	private boolean isScrollStateIdle;
	
	private Handler handler;
	
	private enum ScrollDirection {
		UNDECIDED, LEFT, RIGHT;
	}
	
	private OnGlobalLayoutListener onGlobalLayoutListenerCatTitlesInit = new ViewTreeObserver.OnGlobalLayoutListener() {
		
        @Override
        public void onGlobalLayout() {
        	//Log.d(TAG, "onGlobalLayout()");
        	if (VersionUtil.isApiLevelAbove15()) {
				recyclerVCategories.getViewTreeObserver().removeOnGlobalLayoutListener(this);

			} else {
				recyclerVCategories.getViewTreeObserver().removeGlobalOnLayoutListener(this);
			}
        	
        	centerPosition(CatTitlesAdapterTab.FIRST_PAGE, true);
        }
    };
    
    private OnGlobalLayoutListener onGlobalLayoutListenerCatTitles = new ViewTreeObserver.OnGlobalLayoutListener() {
		
        @Override
        public void onGlobalLayout() {
        	//Log.d(TAG, "onGlobalLayout()");
        	if (VersionUtil.isApiLevelAbove15()) {
				recyclerVCategories.getViewTreeObserver().removeOnGlobalLayoutListener(this);

			} else {
				recyclerVCategories.getViewTreeObserver().removeGlobalOnLayoutListener(this);
			}
        	
        	centerPosition(catTitlesAdapterTab.getSelectedPos(), false);
        }
    };

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		
		handler = new Handler(Looper.getMainLooper());
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		//Log.d(TAG, "onCreateView()");
		if (year == 0) {
        	// initialize year, month, day values for setting action item
      		Calendar c = Calendar.getInstance();
      		year = c.get(Calendar.YEAR);
      		month = c.get(Calendar.MONTH);
      		day = c.get(Calendar.DAY_OF_MONTH);
      		updateStartEndDates();
        }
		
		View v = inflater.inflate(R.layout.fragment_discover, container, false);
		
		recyclerVCategories = (RecyclerView) v.findViewById(R.id.recyclerVCategories);
		// use a linear layout manager
		layoutManager = new LinearLayoutManager(FragmentUtil.getActivity(this));
		layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
		recyclerVCategories.setHasFixedSize(true);
		recyclerVCategories.setLayoutManager(layoutManager);
		
		recyclerVCategories.setOnScrollListener(new OnScrollListener() {
			
			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
				super.onScrolled(recyclerView, dx, dy);
				//Log.d(TAG, "onScrolled() - dx = " + dx + ", dy = " + dy);
				updateCenteredPosition();
			}
			
			@Override
			public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);
				//Log.d(TAG, "onScrollStateChanged() - newState = " + newState);
				isScrollStateIdle = (newState == RecyclerView.SCROLL_STATE_IDLE) ? true : false;
				if (isScrollStateIdle) {
					onCatChanged();
				}
			}
		});
		
		v.findViewById(R.id.imgPrev).setOnClickListener(this);
		v.findViewById(R.id.imgNext).setOnClickListener(this);
		
		recyclerVEvents = (RecyclerView) v.findViewById(R.id.recyclerVEvents);
		int spanCount = (FragmentUtil.getResources(this).getConfiguration().orientation == 
				Configuration.ORIENTATION_PORTRAIT) ? GRID_COLUMNS_PORTRAIT : GRID_COLUMNS_LANDSCAPE;
		GridLayoutManager gridLayoutManager = new GridLayoutManager(FragmentUtil.getActivity(this), spanCount);
		recyclerVEvents.setHasFixedSize(true);
		recyclerVEvents.setLayoutManager(gridLayoutManager);
		
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if (evtCategories == null) {
			buildEvtCategories();
			catTitlesAdapterTab = new CatTitlesAdapterTab(evtCategories, this);
			
			recyclerVCategories.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListenerCatTitlesInit);
			
			double[] latLon = DeviceUtil.getLatLon(FragmentUtil.getApplication(this));
			lat = latLon[0];
			lon = latLon[1];
			
			eventList = new ArrayList<Event>();
			eventList.add(null);
			
			rvCatEventsAdapterTab = new RVCatEventsAdapterTab(eventList, null, this);

		} else {
			recyclerVCategories.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListenerCatTitles);
		}
		
		recyclerVCategories.setAdapter(catTitlesAdapterTab);
		
		recyclerVEvents.addItemDecoration(new ItemDecorationItemOffset(FragmentUtil.getResources(this)
				.getDimensionPixelSize(R.dimen.rv_item_offset_discover_tab)));
		recyclerVEvents.setAdapter(rvCatEventsAdapterTab);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		
		/**
		 * Following call is required to prevent non-removal of onGlobalLayoutListener. If onGlobalLayout() 
		 * is not called yet & screen gets destroyed, then removal of onGlobalLayoutListener will not happen ever 
		 * since fragment won't be able to find its view tree observer. So, better to make sure
		 * that it gets removed at the end
		 */
		if (VersionUtil.isApiLevelAbove15()) {
			recyclerVCategories.getViewTreeObserver().removeOnGlobalLayoutListener(onGlobalLayoutListenerCatTitlesInit);
			recyclerVCategories.getViewTreeObserver().removeOnGlobalLayoutListener(onGlobalLayoutListenerCatTitles);
			
		} else {
			recyclerVCategories.getViewTreeObserver().removeGlobalOnLayoutListener(onGlobalLayoutListenerCatTitlesInit);
			recyclerVCategories.getViewTreeObserver().removeGlobalOnLayoutListener(onGlobalLayoutListenerCatTitles);
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (loadEvents != null && loadEvents.getStatus() != Status.FINISHED) {
			loadEvents.cancel(true);
		}
	}
	
	/**
	 * @param positionToBeCentered
	 * @param isCalledDueToCatChange This is false when called from onGlobalLayoutListenerCatTitles()
	 * For eg - on orientation change or while coming back to this screen from some other screen, when
	 * we don't want to reset eventList
	 */
	private void centerPosition(final int positionToBeCentered, final boolean isCalledDueToCatChange) {
		Runnable runnable = new Runnable() {
    		
    		private ScrollDirection scrollDirection = ScrollDirection.UNDECIDED;
    		private int prevSelectedCenteredCategory;
    		private int loopCount = 0;
    		
			@Override
			public void run() {
				int selectedCenteredCategoryPos = updateCenteredPosition();
				
				/**
				 * Sometimes it goes on looping continuously with same value for selectedCenteredCategory.
				 * To prevent this we just limit loopCount to 5 & then select right initial category
				 * w/o bothering about centering this selection.
				 */
				if (selectedCenteredCategoryPos == prevSelectedCenteredCategory) {
					loopCount++;
					if (loopCount >= 5) {
						recyclerVCategories.scrollToPosition(positionToBeCentered);
						//Log.d(TAG, "loopCount >= 5");
						catTitlesAdapterTab.setSelectedPos(positionToBeCentered);
						if (isCalledDueToCatChange) {
							onCatChanged();
						}
						return;
					}
					
				} else {
					loopCount = 0;
				}
				
				/**
				 * scrollDirection check is added because it's possible (although not encountered yet) 
				 * that selectedCenteredCategory never matches CatTitlesAdapterTab.FIRST_PAGE after 
				 * continuously scrolling along 1 direction few times. In such case we just need to stop 
				 * scrolling further & settle regardless of whether current category selected position is
				 * centered or not.
				 * eg - select dance category on samsung galaxy 10" tab in landscape & change orientation.
				 * It starts moving towards right but positionToBeCentered gets skipped depending on displayed 
				 * category lengths. In this case last else if block settles selection to correct category.
				 */
				if (scrollDirection != ScrollDirection.LEFT && selectedCenteredCategoryPos < positionToBeCentered) {
					scrollDirection = ScrollDirection.RIGHT;
					recyclerVCategories.scrollToPosition(layoutManager.findLastVisibleItemPosition() + 1);
					handler.post(this);
					
				} else if (scrollDirection != ScrollDirection.RIGHT && selectedCenteredCategoryPos > positionToBeCentered) {
					scrollDirection = ScrollDirection.LEFT;
					recyclerVCategories.scrollToPosition(layoutManager.findFirstVisibleItemPosition() - 1);
					handler.post(this);
					
				} else if (selectedCenteredCategoryPos != positionToBeCentered) {
					//Log.d(TAG, "selectedCenteredCategoryPos != positionToBeCentered");
					recyclerVCategories.scrollToPosition(positionToBeCentered);
					catTitlesAdapterTab.setSelectedPos(positionToBeCentered);
					if (isCalledDueToCatChange) {
						onCatChanged();
					}
					
				} else if (selectedCenteredCategoryPos == positionToBeCentered) {
					if (isCalledDueToCatChange) {
						onCatChanged();
					}
				}
				
				prevSelectedCenteredCategory = selectedCenteredCategoryPos;
			}
		};
		recyclerVCategories.scrollToPosition(positionToBeCentered);
		/**
		 * We post on handler otherwise directly calling selectCenteredCategory() from here doesn't return
		 * correct first & last visible positions in selectCenteredCategory() since scrollToPosition() takes 
		 * some time to update.
		 */
		handler.post(runnable);
	}
	
	private int updateCenteredPosition() {
		int selectedPos = layoutManager.findFirstVisibleItemPosition() + ((layoutManager.findLastVisibleItemPosition() 
						- layoutManager.findFirstVisibleItemPosition()) / 2);
		catTitlesAdapterTab.setSelectedPos(selectedPos);
		return selectedPos;
	}
	
	private void buildEvtCategories() {
		evtCategories = new ArrayList<Category>();
		int categoryIdStart = AppConstants.CATEGORY_ID_START;
		String[] categoryNames = getResources().getStringArray(R.array.evt_category_titles);
		for (int i = 0; i < AppConstants.TOTAL_CATEGORIES; i++) {
			evtCategories.add(new Category(categoryIdStart + i, categoryNames[i]));
		}
	}
	
	private void updateStartEndDates() {
		Calendar calendar = new GregorianCalendar(year, month, day);
	    startDate = ConversionUtil.getDay(calendar);
	    
	    calendar.add(Calendar.YEAR, 1);
	    endDate = ConversionUtil.getDay(calendar);
	}
	
	private void resetEventList() {
		if (rvCatEventsAdapterTab == null) {
			return;
		}
		//Log.d(TAG, "resetEventList()");
		isScrollStateIdle = true;
		rvCatEventsAdapterTab.reset();

		if (loadEvents != null) {
			loadEvents.cancel(true);
		}

		eventList.clear();
		eventList.add(null);
		
		rvCatEventsAdapterTab.notifyDataSetChanged();
		/**
		 * Although we expect rvCatEventsAdapterTab to call loadItemsInBackground() due to 1st null item in 
		 * eventList, it won't work always. For eg - If user changes categories fast one by one then loadDateWiseEvents
		 * in rvCatEventsAdapterTab won't be null & also it won't be in FINISHED state. Hence it won't call
		 * loadItemsInBackground() for latest category selection in this case, so better we call loadItemsInBackground()
		 * from here itself.
		 */
		loadItemsInBackground();
	}
	
	public void onCatTitleClicked(int pos) {
		centerPosition(pos, true);
	}
	
	private void onCatChanged() {
		//Log.d(TAG, "onCatChanged(), pos = " + pos + ", selectedCatId = " + selectedCatId);
		//currentItem = pos;
		resetEventList();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.imgPrev:
			centerPosition(catTitlesAdapterTab.getSelectedPos() - 1, true);
			break;
			
		case R.id.imgNext:
			centerPosition(catTitlesAdapterTab.getSelectedPos() + 1, true);
			break;

		default:
			break;
		}
	}
	
	@Override
	public void loadItemsInBackground() {
		//Log.d(TAG, "loadItemsInBackground()");
		/**
		 * load events only if scrolling of titles has finished.
		 * 1) Initially load only after onGlobalLayout() of onGlobalLayoutListenerCatTitlesInit has finished 
		 * & has set valid selectedCatId value in catTitlesAdapterTab; otherwise selectedCatId is 0 initially.
		 * 2) On scrolling titles, call only after recyclerView gets settled.
		 */
		if (isScrollStateIdle) {
			loadEvents = new LoadEvents(Api.OAUTH_TOKEN, eventList, rvCatEventsAdapterTab, lat, lon, startDate, endDate, 
					catTitlesAdapterTab.getSelectedCatId(), ((EventSeekr)FragmentUtil.getActivity(this)
							.getApplicationContext()).getWcitiesId(), miles, this);
			rvCatEventsAdapterTab.setLoadDateWiseEvents(loadEvents);
			AsyncTaskUtil.executeAsyncTask(loadEvents, true);
		}
	}

	@Override
	public void onTaskCompleted(Void... params) {
		// TODO Auto-generated method stub
	}
}
