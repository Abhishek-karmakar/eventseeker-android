package com.wcities.eventseeker;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ListFragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AbsListView;
import android.widget.AbsListView.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.VersionUtil;
import com.wcities.eventseeker.util.ViewUtil;

public class DrawerListFragmentTab extends ListFragment {

	private static final String TAG = DrawerListFragmentTab.class.getSimpleName();
	
	public static final int DIVIDER_POS = 5;

	private DrawerListFragmentTabListener mListener;
	private List<DrawerListItem> drawerListItems;
	private DrawerListAdapter drawerListAdapter;
	
	private int htForDrawerList;
	
	public interface DrawerListFragmentTabListener {
		public void onDrawerListFragmentViewCreated();
		public void onDrawerItemSelected(int pos, Bundle args);
	}
	
	private OnGlobalLayoutListener onGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
		
		private int count = 0;
		
        @Override
        public void onGlobalLayout() {
        	//Log.d(TAG, "onGlobalLayout()");
        	if (count == 0) {
        		count++;
        		
        	} else {
        		count = 0;
        		if (VersionUtil.isApiLevelAbove15()) {
    				getListView().getViewTreeObserver().removeOnGlobalLayoutListener(this);

    			} else {
    				getListView().getViewTreeObserver().removeGlobalOnLayoutListener(this);
    			}
        	}
        	
			/**
			 * this is required because some tablets don't have statusbar (eg - samsung galaxy 10") where our calculation
			 * for htForDrawerList in onCreate() won't return right value since we are subtracting statusbar height
			 * as well.
			 */
			htForDrawerList = getListView().getHeight()
					// subtracting divider height
					- FragmentUtil.getResources(DrawerListFragmentTab.this).getDimensionPixelSize(R.dimen.divider_section_ht_navigation_drawer_list_item);
			drawerListAdapter.onHtForDrawerListUpdated(htForDrawerList);
        }
    };
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (DrawerListFragmentTabListener) activity;
			
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement DrawerListFragmentTabListener");
        }
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//Log.d(TAG, "onCreate()");
		EventSeekr eventSeekr = FragmentUtil.getApplication(this);
		/**
		 * In addition to calling checkAndSetIfInLandscapeMode() from BaseActivityTab, we need to call it from 
		 * here as well, because otherwise on orientation change this fragment's onCreate() is called even 
		 * before its activity's onCreate() & we are using is10InchTabletAndInPortraitMode() below for 10" tablet
		 * which requires updated isInLandscape value before onCreate() of activity in above case.
		 */
		eventSeekr.checkAndSetIfInLandscapeMode();
		Resources res = FragmentUtil.getResources(this);

		if (eventSeekr.is10InchTabletAndInPortraitMode()) {
			htForDrawerList = res.getDimensionPixelSize(R.dimen.ht_navigation_drawer_list);
			
		} else {
			DisplayMetrics displaymetrics = new DisplayMetrics();
			FragmentUtil.getActivity(this).getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
			htForDrawerList = displaymetrics.heightPixels - ViewUtil.getStatusBarHeight(FragmentUtil.getResources(this))
					- res.getDimensionPixelSize(R.dimen.action_bar_ht) 
					// subtracting divider height
					- res.getDimensionPixelSize(R.dimen.divider_section_ht_navigation_drawer_list_item);
		}
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (drawerListItems == null) {
			drawerListItems = new ArrayList<DrawerListItem>();
			loadDrawerListItems();
			
	        drawerListAdapter = new DrawerListAdapter((Activity) FragmentUtil.getActivity(this), drawerListItems, 
	        		htForDrawerList);
			
		} else {
			drawerListAdapter.setmInflater((Activity) FragmentUtil.getActivity(this));
		}
		
		setListAdapter(drawerListAdapter);
        getListView().setDivider(null);
        getListView().setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        //getListView().setBackgroundResource(R.drawable.side_nav_bg);
        //getListView().setBackgroundColor(FragmentUtil.getResources(this).getColor(R.color.bg_screen_dark_blue));
        getListView().setVerticalScrollBarEnabled(false);
        getListView().setHorizontalScrollBarEnabled(false);
        getListView().setCacheColorHint(android.R.color.transparent);
        getListView().setScrollingCacheEnabled(false);
        
        // Set the list's click listener
        getListView().setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView parent, View view, int position, long id) {
				//Log.d(TAG, "onItemClick(), pos = " + position);
				if (position != DIVIDER_POS) {
					mListener.onDrawerItemSelected(position, null);
				}
	        }
		});
        
        EventSeekr eventSeekr = FragmentUtil.getApplication(this);
        if (!eventSeekr.is10InchTabletAndInPortraitMode()) {
        	// for 10" tablet portrait orientation we are using fix height, hence this is not needed
        	getListView().getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
        }
        mListener.onDrawerListFragmentViewCreated();
	}
	
	@Override
	public void onStop() {
		super.onStop();
		//Log.d(TAG, "onStop()");
		
		/**
		 * Following call is required to prevent non-removal of onGlobalLayoutListener. If onGlobalLayout() 
		 * is not called yet & screen gets destroyed, then removal of onGlobalLayoutListener will not happen ever 
		 * since fragment won't be able to find its view tree observer. So, better to make sure
		 * that it gets removed at the end
		 */
		try {
			if (VersionUtil.isApiLevelAbove15()) {
				getListView().getViewTreeObserver().removeOnGlobalLayoutListener(onGlobalLayoutListener);
	
			} else {
				getListView().getViewTreeObserver().removeGlobalOnLayoutListener(onGlobalLayoutListener);
			}
			
		} catch (NullPointerException ne) {
			// if listview is not yet created
			Log.e(TAG, ne.getMessage());
			
		} catch (IllegalStateException ie) {
			// if contentview is not yet created
			Log.e(TAG, ie.getMessage());
		}
	}
	
	private void loadDrawerListItems() {
		String[] drawerListItemTitles = getResources().getStringArray(R.array.navigation_drawer_item_titles);
		TypedArray drawerListItemIcons = getResources().obtainTypedArray(R.array.navigation_drawer_item_icons);
		
        for (int i = 0; i < drawerListItemTitles.length; i++) {
			DrawerListItem drawerListItem = new DrawerListItem(drawerListItemTitles[i], drawerListItemIcons.getDrawable(i));
			drawerListItems.add(drawerListItem);
		}
        drawerListItemIcons.recycle();
	}
	
	public void refreshDrawerList() {
		drawerListItems = new ArrayList<DrawerListItem>();
		loadDrawerListItems();
		drawerListAdapter.setData(drawerListItems);
		drawerListAdapter.notifyDataSetChanged();
	}
	
	private static class DrawerListAdapter extends BaseAdapter {
		
		public static enum LIST_ITEM_TYPE {
			HEADER, ITEM;
		};

		private WeakReference<Activity> baseActivityTab;
	    private LayoutInflater mInflater;
	    private List<DrawerListItem> drawerListItems;
	    private int rowHt;
	    
	    public DrawerListAdapter(Activity baseActivityTab, List<DrawerListItem> drawerListItems, int htForDrawerList) {
	    	this.baseActivityTab = new WeakReference<Activity>(baseActivityTab);
	        mInflater = LayoutInflater.from(this.baseActivityTab.get());
	        this.drawerListItems = drawerListItems;
	        // 1 subtracted since that item is just the section divider
	        rowHt = htForDrawerList / (drawerListItems.size() - 1);
	    }
	    
	    private void onHtForDrawerListUpdated(int htForDrawerList) {
	    	rowHt = htForDrawerList / (drawerListItems.size() - 1);
	    	notifyDataSetChanged();
	    }
	    
	    public void setmInflater(Activity baseActivityTab) {
	    	this.baseActivityTab = new WeakReference<Activity>(baseActivityTab);
	        mInflater = LayoutInflater.from(this.baseActivityTab.get());
		}

	    @Override
	    public int getViewTypeCount() {
	    	return LIST_ITEM_TYPE.values().length;
	    }
	    
		@Override
		public int getCount() {
			return drawerListItems.size();
		}
		
		public void setData(List<DrawerListItem> drawerListItems) {
			this.drawerListItems = drawerListItems;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ListItemViewHolder listItemViewHolder;
			DrawerListItem drawerListItem = getItem(position);
			
			if (drawerListItem.getItemViewType() == LIST_ITEM_TYPE.HEADER) {
				if (convertView == null || !((ListItemViewHolder)convertView.getTag()).tag.equals(LIST_ITEM_TYPE.HEADER)) {
					convertView = mInflater.inflate(R.layout.navigation_drawer_list_section_header, null);
					
					listItemViewHolder = new ListItemViewHolder();
					/*listItemViewHolder.txtTitle = (TextView) convertView.findViewById(R.id.txtTitle);
					listItemViewHolder.vSectionDivider = convertView.findViewById(R.id.dividerSection);*/
					listItemViewHolder.tag = LIST_ITEM_TYPE.HEADER;
					convertView.setTag(listItemViewHolder);
					
				} else {
					listItemViewHolder = (ListItemViewHolder) convertView.getTag();
				}
				
			} else {
				if (convertView == null || !((ListItemViewHolder)convertView.getTag()).tag.equals(LIST_ITEM_TYPE.ITEM)) {
					convertView = mInflater.inflate(R.layout.navigation_drawer_list_item, null);

					listItemViewHolder = new ListItemViewHolder();
					listItemViewHolder.imgIcon = (ImageView) convertView.findViewById(R.id.imgIcon);
					listItemViewHolder.txtTitle = (TextView) convertView.findViewById(R.id.txtTitle);
					listItemViewHolder.vSelection = convertView.findViewById(R.id.vSelection);
					/*listItemViewHolder.vDivider = convertView.findViewById(R.id.divider);
					listItemViewHolder.vSectionDivider = convertView.findViewById(R.id.dividerSection);*/
					listItemViewHolder.tag = LIST_ITEM_TYPE.ITEM;
					convertView.setTag(listItemViewHolder);
					
				} else {
					listItemViewHolder = (ListItemViewHolder) convertView.getTag();
				}
			
				/**
				 * Can't set this only if convertView is null, because we are updating height afterwards as well
				 * from onHtForDrawerListUpdated()
				 */
				// set custom height to fit entire list exactly within the available screen height  
				AbsListView.LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, rowHt);
				convertView.setLayoutParams(lp);
				
				/*if (sectionEndsOnIndices.contains(position)) {
					listItemViewHolder.vDivider.setVisibility(View.GONE);
					listItemViewHolder.vSectionDivider.setVisibility(View.VISIBLE);
					
				} else {
					listItemViewHolder.vDivider.setVisibility(View.VISIBLE);
					listItemViewHolder.vSectionDivider.setVisibility(View.GONE);
				}*/
				
				listItemViewHolder.imgIcon.setImageDrawable(drawerListItem.iconDrawable);

				if (((ListView)parent).getCheckedItemPosition() == position) {
					listItemViewHolder.vSelection.setVisibility(View.VISIBLE);
					//listItemViewHolder.vSelection.setBackgroundColor(mainActivity.get().getResources().getColor(android.R.color.white));
					//convertView.setBackgroundColor(mainActivity.get().getResources().getColor(android.R.color.white));
					//listItemViewHolder.txtTitle.setTextColor(mainActivity.get().getResources().getColor(R.color.bg_screen_dark_blue));
					//listItemViewHolder.imgIcon.setSelected(true);
					
				} else {
					listItemViewHolder.vSelection.setVisibility(View.INVISIBLE);
					//listItemViewHolder.vSelection.setBackgroundResource(0);
					//convertView.setBackgroundResource(0);
					//listItemViewHolder.txtTitle.setTextColor(mainActivity.get().getResources().getColor(android.R.color.white));
					//listItemViewHolder.imgIcon.setSelected(false);
				}
			}
			if (listItemViewHolder.tag != LIST_ITEM_TYPE.HEADER) {
				listItemViewHolder.txtTitle.setText(drawerListItem.title);
			}
			
			return convertView;
		}

		@Override
		public DrawerListItem getItem(int position) {
			return drawerListItems.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
		
		private static class ListItemViewHolder {
			private ImageView imgIcon;
			private TextView txtTitle;
			private View vSelection;
			//private View vDivider, vSectionDivider;
			private Object tag;
		}
	}
	
	private static class DrawerListItem {
		
		private String title;
		private Drawable iconDrawable;
		private DrawerListAdapter.LIST_ITEM_TYPE type;
		
		public DrawerListItem(String title, Drawable iconDrawable) {
			this.title = title;
			this.iconDrawable = iconDrawable;
			type = iconDrawable == null ? DrawerListAdapter.LIST_ITEM_TYPE.HEADER : DrawerListAdapter.LIST_ITEM_TYPE.ITEM;
		}

		public DrawerListAdapter.LIST_ITEM_TYPE getItemViewType() {
			return type;
		}
	}
}
