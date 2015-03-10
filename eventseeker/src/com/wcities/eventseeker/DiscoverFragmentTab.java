package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
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
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Category;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.VersionUtil;

public class DiscoverFragmentTab extends Fragment implements OnClickListener {
	
	private static final String TAG = DiscoverFragmentTab.class.getSimpleName();

	private List<Category> evtCategories;
	
	private RecyclerView recyclerVCategories;
	private LinearLayoutManager layoutManager;
	
	private CatTitlesAdapterTab catTitlesAdapterTab;
	
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
        	
        	centerPosition(CatTitlesAdapterTab.FIRST_PAGE);
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
        	
        	centerPosition(catTitlesAdapterTab.getSelectedPos());
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
		View v = inflater.inflate(R.layout.fragment_discover, container, false);
		
		recyclerVCategories = (RecyclerView) v.findViewById(R.id.recyclerVCategories);
		// use a linear layout manager
		layoutManager = new LinearLayoutManager(FragmentUtil.getActivity(this));
		layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
		recyclerVCategories.setLayoutManager(layoutManager);
		
		recyclerVCategories.setOnScrollListener(new OnScrollListener() {
			
			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
				super.onScrolled(recyclerView, dx, dy);
				updateCenteredPosition();
			}
		});
		
		v.findViewById(R.id.imgPrev).setOnClickListener(this);
		v.findViewById(R.id.imgNext).setOnClickListener(this);
		
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if (evtCategories == null) {
			buildEvtCategories();
			catTitlesAdapterTab = new CatTitlesAdapterTab(evtCategories, this);
			
			recyclerVCategories.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListenerCatTitlesInit);
			recyclerVCategories.setAdapter(catTitlesAdapterTab);

		} else {
			recyclerVCategories.setAdapter(catTitlesAdapterTab);
			recyclerVCategories.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListenerCatTitles);
		}
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
	
	private void centerPosition(final int positionToBeCentered) {
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
	
	public void onCatTitleClicked(int pos) {
		centerPosition(pos);
	}
	
	public void onCatChanged(int pos, int selectedCatId) {
		//Log.d(TAG, "onCatChanged(), pos = " + pos + ", selectedCatId = " + selectedCatId);
		//currentItem = pos;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.imgPrev:
			centerPosition(catTitlesAdapterTab.getSelectedPos() - 1);
			break;
			
		case R.id.imgNext:
			centerPosition(catTitlesAdapterTab.getSelectedPos() + 1);
			break;

		default:
			break;
		}
	}
}
