package com.wcities.eventseeker;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.wcities.eventseeker.custom.fragment.ListFragmentLoadableFromBackStack;
import com.wcities.eventseeker.util.FragmentUtil;

public class SettingsFragment extends ListFragmentLoadableFromBackStack {

	private List<SettingsMenuListItem> settingsMenuListItems;
	private SettingsMenuAdapter settingsMenuAdapter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}
	
	@Override
	public String getScreenName() {
		return "Menu Settings Screen";
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (settingsMenuListItems == null) {
			settingsMenuListItems = new ArrayList<SettingsMenuListItem>();
			loadSettingsMenuListItems();
			settingsMenuAdapter = new SettingsMenuAdapter((Activity) FragmentUtil.getActivity(this), settingsMenuListItems);
			
		} else {
			settingsMenuAdapter.setInflater((Activity) FragmentUtil.getActivity(this));
		}
		
		setListAdapter(settingsMenuAdapter);
        getListView().setDivider(null);
        getListView().setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        //getListView().setBackgroundResource(R.drawable.side_nav_bg);
        //getListView().setBackgroundResource(R.drawable.bg_drawer_list);
        getListView().setVerticalScrollBarEnabled(false);
        getListView().setHorizontalScrollBarEnabled(false);
        getListView().setCacheColorHint(android.R.color.transparent);
        getListView().setScrollingCacheEnabled(false);
        
        // Set the list's click listener
        /*getListView().setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView parent, View view, int position, long id) {
				//Log.d(TAG, "onItemClick(), pos = " + position);
				if (!sectionHeaderIndices.contains(position)) {
					mListener.onDrawerItemSelected(position, null);
				}
	        }
		});
        
        mListener.onDrawerListFragmentViewCreated();*/
	}
	
	private void loadSettingsMenuListItems() {
		String[] drawerListItemTitles = getResources().getStringArray(R.array.settings_menu_items);
		TypedArray drawerListItemIcons = getResources().obtainTypedArray(R.array.settings_menu_icons);
		
        for (int i = 0; i < drawerListItemTitles.length; i++) {
        	SettingsMenuListItem drawerListItem = new SettingsMenuListItem(drawerListItemTitles[i], 
        		drawerListItemIcons.getDrawable(i));
			settingsMenuListItems.add(drawerListItem);
		}
        drawerListItemIcons.recycle();
	}

	
	private static class SettingsMenuAdapter extends BaseAdapter {

		private List<SettingsMenuListItem> settingsMenuListItems;
		private WeakReference<Activity> mainActivity;
		private LayoutInflater inflater;

		public SettingsMenuAdapter(Activity activity, List<SettingsMenuListItem> settingsMenuListItems) {
			this.mainActivity = new WeakReference<Activity>(activity);
	        inflater = LayoutInflater.from(this.mainActivity.get());
	        this.settingsMenuListItems = settingsMenuListItems;
		}

		@Override
		public int getCount() {
			return settingsMenuListItems.size();
		}

		@Override
		public Object getItem(int position) {
			return settingsMenuListItems.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		public void setData(List<SettingsMenuListItem> settingsMenuListItems) {
			this.settingsMenuListItems = settingsMenuListItems;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ListItemViewHolder listItemViewHolder;
			SettingsMenuListItem settingsMenuListItem = (SettingsMenuListItem) getItem(position);

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.navigation_drawer_list_item, null);

				listItemViewHolder = new ListItemViewHolder();
				listItemViewHolder.imgIcon = (ImageView) convertView.findViewById(R.id.imgIcon);
				listItemViewHolder.txtTitle = (TextView) convertView.findViewById(R.id.txtTitle);
				convertView.setTag(listItemViewHolder);

			} else {
				listItemViewHolder = (ListItemViewHolder) convertView.getTag();
			}

			listItemViewHolder.imgIcon.setImageDrawable(settingsMenuListItem.iconDrawable);

			if (((ListView) parent).getCheckedItemPosition() == position) {
				convertView.setBackgroundColor(mainActivity.get().getResources().getColor(R.color.font_blue));
				listItemViewHolder.txtTitle.setTextColor(mainActivity.get().getResources().getColor(android.R.color.white));
				listItemViewHolder.imgIcon.setSelected(true);

			} else {
				listItemViewHolder.txtTitle.setTextColor(mainActivity.get().getResources().getColor(R.color.darker_gray));
				listItemViewHolder.imgIcon.setSelected(false);
			}

			listItemViewHolder.txtTitle.setText(settingsMenuListItem.title);

			return convertView;
		}
		
		public void setInflater(Activity activity) {
			this.mainActivity = new WeakReference<Activity>(activity);
			inflater = LayoutInflater.from(this.mainActivity.get());
		}
		
		private static class ListItemViewHolder {
			private ImageView imgIcon;
			private TextView txtTitle;
			private Object tag;
		}
		
	}

	private static class SettingsMenuListItem {
		private String title;
		private Drawable iconDrawable;
		
		public SettingsMenuListItem(String title, Drawable iconDrawable) {
			this.title = title;
			this.iconDrawable = iconDrawable;
		}
	}
	
}
