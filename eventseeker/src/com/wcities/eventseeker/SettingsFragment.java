package com.wcities.eventseeker;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.custom.fragment.ListFragmentLoadableFromBackStack;
import com.wcities.eventseeker.util.FragmentUtil;

public class SettingsFragment extends ListFragmentLoadableFromBackStack {
	
	private static final String TAG = SettingsFragment.class.getSimpleName();
	
	private List<SettingsItem> settingsItems;
	private SettingsListAdapter settingsListAdapter;
	
	public enum SettingsItem {
		SYNC_ACCOUNTS(R.drawable.selector_sync, R.string.navigation_drawer_item_sync_accounts),	  	
		CHANGE_LOCATION(R.drawable.selector_changelocation, R.string.navigation_drawer_item_change_location),
		LANGUAGE(R.drawable.selector_language, R.string.navigation_drawer_item_language),
		INVITE_FRIENDS(R.drawable.selector_invitefriends, R.string.navigation_drawer_item_invite_friends),
		RATE_APP(R.drawable.selector_store, R.string.navigation_drawer_item_rate_app),
		ABOUT(R.drawable.selector_info, R.string.navigation_drawer_item_about),
		EULA(R.drawable.selector_eula, R.string.navigation_drawer_item_eula),
		REPCODE(R.drawable.selector_repcode, R.string.navigation_drawer_item_enter_rep_code);
		
		private int icon, title;
		private SettingsItem(int icon, int title) {
			this.icon = icon; 
			this.title = title;			
		}
		
		public int getIcon() {
			return icon;
		}
		
		public int getTitle() {
			return title;
		}
		
		public static SettingsItem getSettingsItemByOrdinal(int ordinal) {
			for (SettingsItem settingsItem : SettingsItem.values()) {
				if (settingsItem.ordinal() == ordinal) {
					return settingsItem;
				}
			}
			return null;
		}
	}
	
	public interface OnSettingsItemClickedListener {
		/**
		 * Handles the Actions related to the options in Settings screen.
		 * @param settingsItem
		 * @param args TODO
		 */
		public void onSettingsItemClicked(SettingsItem settingsItem, Bundle args);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		
		Bundle args = getArguments();
		if (args != null && args.containsKey(BundleKeys.SETTINGS_ITEM)) {
			// open passed settings item screen
			SettingsItem settingsItem = (SettingsItem) args.getSerializable(BundleKeys.SETTINGS_ITEM);
			//Log.d(TAG, "settings item = " + settingsItem.name());
			Bundle passArgs = new Bundle();
			passArgs.putBoolean(BundleKeys.DISABLE_DRAWER_INDICATOR_FROM_ONRESUME, true);
			((OnSettingsItemClickedListener) FragmentUtil.getActivity(this)).onSettingsItemClicked(settingsItem, passArgs);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_settings, null);
		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (settingsItems == null) {
			settingsItems = new ArrayList<SettingsItem>(Arrays.asList(SettingsItem.values()));
			settingsListAdapter = new SettingsListAdapter((Activity) FragmentUtil.getActivity(this), settingsItems);
			
		} else {
			settingsListAdapter.setInflater((Activity) FragmentUtil.getActivity(this));
		}
		
		setListAdapter(settingsListAdapter);
        getListView().setDivider(null);
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
	
	@Override
	public String getScreenName() {
		return "Menu Settings Screen";
	}
	
	private static class SettingsListAdapter extends BaseAdapter {

		private List<SettingsItem> settingsItems;
		private WeakReference<Activity> mainActivity;
		private LayoutInflater inflater;

		public SettingsListAdapter(Activity activity, List<SettingsItem> settingsMenuListItems) {
			this.mainActivity = new WeakReference<Activity>(activity);
	        inflater = LayoutInflater.from(this.mainActivity.get());
	        this.settingsItems = settingsMenuListItems;
		}

		@Override
		public int getCount() {
			return settingsItems.size();
		}

		@Override
		public Object getItem(int position) {
			return settingsItems.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ListItemViewHolder listItemViewHolder;
			SettingsItem settingsItem = (SettingsItem) getItem(position);

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.list_item_settings, null);

				listItemViewHolder = new ListItemViewHolder();
				listItemViewHolder.imgIcon = (ImageView) convertView.findViewById(R.id.imgIcon);
				listItemViewHolder.txtTitle = (TextView) convertView.findViewById(R.id.txtTitle);
				convertView.setTag(listItemViewHolder);

			} else {
				listItemViewHolder = (ListItemViewHolder) convertView.getTag();
			}

			listItemViewHolder.imgIcon.setImageDrawable(mainActivity.get().getResources()
				.getDrawable(settingsItem.getIcon()));

			listItemViewHolder.txtTitle.setText(settingsItem.getTitle());

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
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		SettingsItem settingsItem = (SettingsItem) l.getAdapter().getItem(position);
		((OnSettingsItemClickedListener) FragmentUtil.getActivity(this)).onSettingsItemClicked(settingsItem, null);
	}
}
