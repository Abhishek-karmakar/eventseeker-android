package com.wcities.eventseeker.bosch;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.util.FragmentUtil;

public class BoschDrawerListFragment extends ListFragment {

	private static final String TAG = BoschDrawerListFragment.class.getName();

	private BoschDrawerListFragmentListener mListener;
	private List<DrawerListItem> drawerListItems;
	private DrawerListAdapter drawerListAdapter;
	
	public interface BoschDrawerListFragmentListener {
		public void onDrawerItemSelected(int pos);
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (BoschDrawerListFragmentListener) activity;
			
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement BoschDrawerListFragmentListener");
        }
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		drawerListItems = new ArrayList<DrawerListItem>();
		loadDrawerListItems();
		
        drawerListAdapter = new DrawerListAdapter((Activity) FragmentUtil.getActivity(this), drawerListItems);
		
		setListAdapter(drawerListAdapter);
        getListView().setDivider(null);
        getListView().setVerticalScrollBarEnabled(false);
        getListView().setHorizontalScrollBarEnabled(false);
        
        // Set the list's click listener
        getListView().setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView parent, View view, int position, long id) {
				mListener.onDrawerItemSelected(position);
	        }
		});
	}
	
	private void loadDrawerListItems() {
		String[] drawerListItemTitles = getResources().getStringArray(R.array.bosch_navigation_drawer_item_titles);
		TypedArray drawerListItemIcons = getResources().obtainTypedArray(R.array.bosch_navigation_drawer_item_icons);
		
        for (int i = 0; i < drawerListItemTitles.length; i++) {
			DrawerListItem drawerListItem = new DrawerListItem(drawerListItemTitles[i],
				drawerListItemIcons.getDrawable(i));
			drawerListItems.add(drawerListItem);
		}
        drawerListItemIcons.recycle();
	}
	
	private static class DrawerListAdapter extends BaseAdapter {
		private WeakReference<Activity> mainActivity;
	    private LayoutInflater mInflater;
	    private List<DrawerListItem> drawerListItems;
	    
	    public DrawerListAdapter(Activity mainActivity, List<DrawerListItem> drawerListItems) {
	    	this.mainActivity = new WeakReference<Activity>(mainActivity);
	        mInflater = LayoutInflater.from(this.mainActivity.get());
	        this.drawerListItems = drawerListItems;
	    }
	    
		@Override
		public int getCount() {
			return drawerListItems.size();
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			ListItemViewHolder listItemViewHolder;
			DrawerListItem drawerListItem = getItem(position);
			
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.bosch_navigation_drawer_list_item, null);

				listItemViewHolder = new ListItemViewHolder();
				listItemViewHolder.imgIcon = (ImageView) convertView.findViewById(R.id.imgIcon);
				listItemViewHolder.txtTitle = (TextView) convertView.findViewById(R.id.txtTitle);
				convertView.setTag(listItemViewHolder);
				
			} else {
				listItemViewHolder = (ListItemViewHolder) convertView.getTag();
			}
		
			listItemViewHolder.imgIcon.setImageDrawable(drawerListItem.iconDrawable);
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
		}
	}
	
	private static class DrawerListItem {
		private String title;
		private Drawable iconDrawable;
		
		public DrawerListItem(String title, Drawable iconDrawable) {
			this.title = title;
			this.iconDrawable = iconDrawable;
		}
	}
}
