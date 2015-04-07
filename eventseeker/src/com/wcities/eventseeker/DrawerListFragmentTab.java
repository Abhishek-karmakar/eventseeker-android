package com.wcities.eventseeker;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.AdapterDataObserver;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AbsListView;
import android.widget.AbsListView.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import com.wcities.eventseeker.DrawerListFragmentTab.DrawerListAdapter.ListItemViewHolder;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.interfaces.DrawerListFragmentListener;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.VersionUtil;
import com.wcities.eventseeker.util.ViewUtil;

public class DrawerListFragmentTab extends Fragment {

	private static final String TAG = DrawerListFragmentTab.class.getSimpleName();
	
	public static final int DIVIDER_POS = 5;

	private DrawerListFragmentListener mListener;
	private List<DrawerListItem> drawerListItems;
	private DrawerListAdapter drawerListAdapter;
	
	private RecyclerView rcyclrDrawer;
	
	private int htForDrawerList;
	
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
    				rcyclrDrawer.getViewTreeObserver().removeOnGlobalLayoutListener(this);

    			} else {
    				rcyclrDrawer.getViewTreeObserver().removeGlobalOnLayoutListener(this);
    			}
        	}
        	
			/**
			 * this is required because some tablets don't have statusbar (eg - samsung galaxy 10") where our calculation
			 * for htForDrawerList in onCreate() won't return right value since we are subtracting statusbar height
			 * as well.
			 */
			htForDrawerList = rcyclrDrawer.getHeight()
					// subtracting divider height
					- FragmentUtil.getResources(DrawerListFragmentTab.this).getDimensionPixelSize(R.dimen.divider_section_ht_navigation_drawer_list_item);
			drawerListAdapter.onHtForDrawerListUpdated(htForDrawerList);
        }
    };
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (DrawerListFragmentListener) activity;
			
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
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_drawer_list_tab, null);
		rcyclrDrawer = (RecyclerView) view.findViewById(R.id.rcyclrDrawer);
		// use a linear layout manager
		LinearLayoutManager layoutManager = new LinearLayoutManager(FragmentUtil.getActivity(this));
		layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
		rcyclrDrawer.setLayoutManager(layoutManager);
		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (drawerListItems == null) {
			drawerListItems = new ArrayList<DrawerListItem>();
			loadDrawerListItems();
	        drawerListAdapter = new DrawerListAdapter((Activity) FragmentUtil.getActivity(this), drawerListItems, 
	        		htForDrawerList, mListener);
			
		} else {
			drawerListAdapter.setmInflater((Activity) FragmentUtil.getActivity(this));
		}
		
		rcyclrDrawer.setAdapter(drawerListAdapter);
        //TODO:NEED TO CHECK USE OF IT IN RECYCLERVIEW
		//getListView().setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
		rcyclrDrawer.setVerticalScrollBarEnabled(false);
		rcyclrDrawer.setHorizontalScrollBarEnabled(false);
        
        EventSeekr eventSeekr = FragmentUtil.getApplication(this);
        if (!eventSeekr.is10InchTabletAndInPortraitMode()) {
        	// for 10" tablet portrait orientation we are using fix height, hence this is not needed
        	rcyclrDrawer.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
        }
        mListener.onDrawerListFragmentViewCreated();
	}
	
	public void updateCheckedDrawerItem(int position) {
		drawerListAdapter.setChecked(position);
		drawerListAdapter.notifyDataSetChanged();
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
				rcyclrDrawer.getViewTreeObserver().removeOnGlobalLayoutListener(onGlobalLayoutListener);
	
			} else {
				rcyclrDrawer.getViewTreeObserver().removeGlobalOnLayoutListener(onGlobalLayoutListener);
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
	
	public static class DrawerListAdapter extends RecyclerView.Adapter<ListItemViewHolder> {
		
		public static enum LIST_ITEM_TYPE {
			HEADER, ITEM;
		};

		private WeakReference<Activity> baseActivityTab;
	    private LayoutInflater mInflater;
	    private List<DrawerListItem> drawerListItems;
	    private DrawerListFragmentListener mListener;
	    private int rowHt;
	    private AdapterDataObserver adapterDataObserver;
	    
	    public DrawerListAdapter(Activity baseActivityTab, List<DrawerListItem> drawerListItems, int htForDrawerList, DrawerListFragmentListener mListener) {
	    	this.baseActivityTab = new WeakReference<Activity>(baseActivityTab);
	        this.drawerListItems = drawerListItems;
	        this.mListener = mListener;

	        mInflater = LayoutInflater.from(this.baseActivityTab.get());
	        
	        // 1 subtracted since that item is just the section divider
	        rowHt = htForDrawerList / (drawerListItems.size() - 1);
	    }
	    
	    /**
		 * Need to unregister manually because otherwise using same adapter on orientation change results in
		 * multiple time registrations w/o unregistration, due to which we need to manually 
		 * call unregisterAdapterDataObserver if it tries to register with new observer when already some older
		 * observer is registered. W/o having this results in multiple observers holding cardview & imgEvt memory.
		 */
		@Override
		public void registerAdapterDataObserver(AdapterDataObserver observer) {
			if (adapterDataObserver != null) {
				unregisterAdapterDataObserver(adapterDataObserver);
			}
	        super.registerAdapterDataObserver(observer);
	        adapterDataObserver = observer;
	    }
	    
	    private void onHtForDrawerListUpdated(int htForDrawerList) {
	    	rowHt = htForDrawerList / (drawerListItems.size() - 1);
	    	notifyDataSetChanged();
	    }
	    
	    public void setmInflater(Activity baseActivityTab) {
	    	this.baseActivityTab = new WeakReference<Activity>(baseActivityTab);
	        mInflater = LayoutInflater.from(this.baseActivityTab.get());
		}

		public void setData(List<DrawerListItem> drawerListItems) {
			this.drawerListItems = drawerListItems;
		}

		@Override
		public int getItemViewType(int position) {
			return 	drawerListItems.get(position).iconDrawable == null ? 
					DrawerListAdapter.LIST_ITEM_TYPE.HEADER.ordinal() : DrawerListAdapter.LIST_ITEM_TYPE.ITEM.ordinal();
		}
		
		@Override
		public ListItemViewHolder onCreateViewHolder(ViewGroup parent, int itemType) {
			View convertView;
			LIST_ITEM_TYPE type;
			if (itemType == LIST_ITEM_TYPE.ITEM.ordinal()) {
				convertView = mInflater.inflate(R.layout.navigation_drawer_list_item, null);
				type = LIST_ITEM_TYPE.ITEM;
				
			} else {
				convertView = mInflater.inflate(R.layout.navigation_drawer_list_section_header, null);
				type = LIST_ITEM_TYPE.HEADER;
			}
			
			return new ListItemViewHolder(convertView, type);
		}

		@Override
		public void onBindViewHolder(ListItemViewHolder listItemViewHolder, final int position) {
			final DrawerListItem drawerListItem = drawerListItems.get(position);
			if (listItemViewHolder.type == LIST_ITEM_TYPE.ITEM) {
				/**
				 * Can't set this only if convertView is null, because we are updating height afterwards as well
				 * from onHtForDrawerListUpdated() due to which we need to update layout params even if convertview 
				 * is not null. 
				 */
				// set custom height to fit entire list exactly within the available screen height  
				AbsListView.LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, rowHt);
				listItemViewHolder.vRoot.setLayoutParams(lp);
				
				listItemViewHolder.imgIcon.setImageDrawable(drawerListItem.iconDrawable);
				listItemViewHolder.txtTitle.setText(drawerListItem.title);
				
				if (drawerListItem.isChecked) {
					listItemViewHolder.vSelection.setVisibility(View.VISIBLE);
					
				} else {
					listItemViewHolder.vSelection.setVisibility(View.INVISIBLE);
				}
				
				listItemViewHolder.vRoot.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View view) {
						mListener.onDrawerItemSelected(position, null);
			        }
				});
			}
		}
		
		@Override
		public int getItemCount() {
			return drawerListItems.size();
		}
		
		public void setChecked(int position) {
			drawerListItems.get(position).isChecked = true;
		}

		public static class ListItemViewHolder extends RecyclerView.ViewHolder {
			private View vRoot, vSelection;
			private ImageView imgIcon;
			private TextView txtTitle;
			
			private LIST_ITEM_TYPE type;
			
			public ListItemViewHolder(View itemView, LIST_ITEM_TYPE type) {
				super(itemView);
				vRoot = itemView;
				imgIcon = (ImageView) itemView.findViewById(R.id.imgIcon);
				txtTitle = (TextView) itemView.findViewById(R.id.txtTitle);
				vSelection = itemView.findViewById(R.id.vSelection);
				
				this.type = type; 
			}
		}
	}
	
	private static class DrawerListItem {
		private String title;
		private Drawable iconDrawable;
		private DrawerListAdapter.LIST_ITEM_TYPE type;
		private boolean isChecked = false;
		
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
