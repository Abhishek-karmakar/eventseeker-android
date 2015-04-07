package com.wcities.eventseeker;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.wcities.eventseeker.interfaces.DrawerListFragmentListener;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.ViewUtil;

public class DrawerListFragment extends ListFragment {

	private static final String TAG = DrawerListFragment.class.getSimpleName();

	public static final int DIVIDER_POS = 5;

	private DrawerListFragmentListener mListener;
	private List<DrawerListItem> drawerListItems;
	private DrawerListAdapter drawerListAdapter;
	
    private int htForDrawerList;
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (DrawerListFragmentListener) activity;
			
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement DrawerListFragmentListener");
        }
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Resources res = FragmentUtil.getResources(this);
		DisplayMetrics displaymetrics = new DisplayMetrics();
		FragmentUtil.getActivity(this).getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		htForDrawerList = displaymetrics.heightPixels - ViewUtil.getStatusBarHeight(FragmentUtil.getResources(this))
				- res.getDimensionPixelSize(R.dimen.action_bar_ht) 
				// subtracting divider height
				- res.getDimensionPixelSize(R.dimen.divider_section_ht_navigation_drawer_list_item);
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
        
        mListener.onDrawerListFragmentViewCreated();
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
	
	/*public void setMyEventsCount(int count) {
		String myEvents = getResources().getString(R.string.navigation_drawer_item_my_events);
		
		for (Iterator<DrawerListItem> iterator = drawerListItems.iterator(); iterator.hasNext();) {
			DrawerListItem drawerListItem = iterator.next();
			
			if (drawerListItem.title.equals(myEvents)) {
				
				FragmentUtil.getActivity(this).runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						drawerListAdapter.notifyDataSetChanged();
					}
				});
				break;
			}
		}
	}
	
	public void setMyArtistsCount(int count) {
		String myArtists = getResources().getString(R.string.navigation_drawer_item_following);
		
		for (Iterator<DrawerListItem> iterator = drawerListItems.iterator(); iterator.hasNext();) {
			DrawerListItem drawerListItem = iterator.next();
			
			if (drawerListItem.title.equals(myArtists)) {
				
				FragmentUtil.getActivity(this).runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						drawerListAdapter.notifyDataSetChanged();
					}
				});
				break;
			}
		}
	}*/
	
	private static class DrawerListAdapter extends BaseAdapter {
		
		public static enum LIST_ITEM_TYPE {
			HEADER, ITEM;
		};

		private WeakReference<Activity> mainActivity;
	    private LayoutInflater mInflater;
	    private List<DrawerListItem> drawerListItems;
	    private int rowHt;
	    
	    public DrawerListAdapter(Activity mainActivity, List<DrawerListItem> drawerListItems, int htForDrawerList) {
	    	this.mainActivity = new WeakReference<Activity>(mainActivity);
	        mInflater = LayoutInflater.from(this.mainActivity.get());
	        this.drawerListItems = drawerListItems;
	        // 1 subtracted since that item is just the section divider
	        rowHt = htForDrawerList / (drawerListItems.size() - 1);
	    }
	    
	    public void setmInflater(Activity mainActivity) {
	    	this.mainActivity = new WeakReference<Activity>(mainActivity);
	        mInflater = LayoutInflater.from(this.mainActivity.get());
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
		public View getView(final int position, View convertView, ViewGroup parent) {
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
					
					// set custom height to fit entire list exactly within the available screen height  
					AbsListView.LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, rowHt);
					convertView.setLayoutParams(lp);

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
