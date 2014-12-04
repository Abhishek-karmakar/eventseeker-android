package com.wcities.eventseeker;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.wcities.eventseeker.util.FragmentUtil;

public class DrawerListFragment extends ListFragment {

	private static final String TAG = DrawerListFragment.class.getName();

	private DrawerListFragmentListener mListener;
	private List<DrawerListItem> drawerListItems;
	private DrawerListAdapter drawerListAdapter;
	
	public static final int SECT_1_HEADER_POS = 0;
	public static final int SECT_2_HEADER_POS = 6;
	// TODO: make it 9 for disabling language
	public static final int SECT_3_HEADER_POS = 10;
	
    private List<Integer> sectionHeaderIndices = new ArrayList<Integer>(Arrays.asList(SECT_1_HEADER_POS, SECT_2_HEADER_POS, SECT_3_HEADER_POS));
	
	public interface DrawerListFragmentListener {
		public void onDrawerListFragmentViewCreated();
		public void onDrawerItemSelected(int pos, Bundle args);
	}
	
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
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (drawerListItems == null) {
			drawerListItems = new ArrayList<DrawerListItem>();
			loadDrawerListItems();
			
	        drawerListAdapter = new DrawerListAdapter((Activity) FragmentUtil.getActivity(this), drawerListItems);
			
		} else {
			drawerListAdapter.setmInflater((Activity) FragmentUtil.getActivity(this));
		}
		
		setListAdapter(drawerListAdapter);
        getListView().setDivider(null);
        getListView().setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        //getListView().setBackgroundResource(R.drawable.side_nav_bg);
        getListView().setBackgroundResource(R.drawable.bg_drawer_list);
        getListView().setVerticalScrollBarEnabled(false);
        getListView().setHorizontalScrollBarEnabled(false);
        getListView().setCacheColorHint(android.R.color.transparent);
        getListView().setScrollingCacheEnabled(false);
        
        // Set the list's click listener
        getListView().setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView parent, View view, int position, long id) {
				//Log.d(TAG, "onItemClick(), pos = " + position);
				/**
				 * This notification generation is just for testing purpose
				 */
				/*if (!AppConstants.IS_RELEASE_MODE) {
					if (position == drawerListItems.size() - 1) {
						//Change notification type in below line
						NotificationType notificationType = NotificationType.SYNC_ACCOUNTS;
						GcmBroadcastReceiver.createDummyNotificationForTesting(getActivity(), 
							notificationType.ordinal() + "", "", "Testing Notification " + notificationType.name());
						getActivity().finish();
						return;
					}
				}*/
				
				if (!sectionHeaderIndices.contains(position)) {
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
	    
	    private static final ArrayList<Integer> sectionEndsOnIndices = new ArrayList<Integer>(Arrays.asList(SECT_2_HEADER_POS - 1, SECT_3_HEADER_POS - 1));

	    public DrawerListAdapter(Activity mainActivity, List<DrawerListItem> drawerListItems) {
	    	this.mainActivity = new WeakReference<Activity>(mainActivity);
	        mInflater = LayoutInflater.from(this.mainActivity.get());
	        this.drawerListItems = drawerListItems;
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
					listItemViewHolder.txtTitle = (TextView) convertView.findViewById(R.id.txtTitle);
					listItemViewHolder.vSectionDivider = convertView.findViewById(R.id.dividerSection);
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
					listItemViewHolder.vDivider = convertView.findViewById(R.id.divider);
					listItemViewHolder.vSectionDivider = convertView.findViewById(R.id.dividerSection);
					listItemViewHolder.tag = LIST_ITEM_TYPE.ITEM;
					convertView.setTag(listItemViewHolder);
					
				} else {
					listItemViewHolder = (ListItemViewHolder) convertView.getTag();
				}
			
				if (sectionEndsOnIndices.contains(position)) {
					listItemViewHolder.vDivider.setVisibility(View.GONE);
					listItemViewHolder.vSectionDivider.setVisibility(View.VISIBLE);
					
				} else {
					listItemViewHolder.vDivider.setVisibility(View.VISIBLE);
					listItemViewHolder.vSectionDivider.setVisibility(View.GONE);
				}
				
				listItemViewHolder.imgIcon.setImageDrawable(drawerListItem.iconDrawable);

				if (((ListView)parent).getCheckedItemPosition() == position) {
					convertView.setBackgroundColor(mainActivity.get().getResources().getColor(R.color.font_blue));
					listItemViewHolder.txtTitle.setTextColor(mainActivity.get().getResources().getColor(android.R.color.white));
					listItemViewHolder.imgIcon.setSelected(true);
					
				} else {
					convertView.setBackgroundResource(0);
					listItemViewHolder.txtTitle.setTextColor(mainActivity.get().getResources().getColor(R.color.darker_gray));
					listItemViewHolder.imgIcon.setSelected(false);
				}
			}
			listItemViewHolder.txtTitle.setText(drawerListItem.title);
			
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
			private View vDivider, vSectionDivider;
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
