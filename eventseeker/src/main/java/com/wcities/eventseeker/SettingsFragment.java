package com.wcities.eventseeker;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.constants.Enums.SettingsItem;
import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.custom.fragment.ListFragmentLoadableFromBackStack;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.ViewUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SettingsFragment extends ListFragmentLoadableFromBackStack {
	
	private static final String TAG = SettingsFragment.class.getSimpleName();
	
	private List<SettingsItem> settingsItems;
	private SettingsListAdapter settingsListAdapter;

	private int htForSettingsList;
	
	public interface OnSettingsItemClickedListener {
		/**
		 * Handles the Actions related to the options in Settings screen.
		 * @param settingsItem
		 */
		public void onSettingsItemClicked(SettingsItem settingsItem, Bundle args);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		/**
		 * If user logs in or signs up into the app from the SignUp screen he must be navigated to
		 * Sync Accounts screen. The below block implements this functionality.
		 */
		Bundle args = getArguments();
		if (args != null && args.containsKey(BundleKeys.SETTINGS_ITEM)) {
			// open passed settings item screen
			SettingsItem settingsItem = (SettingsItem) args.getSerializable(BundleKeys.SETTINGS_ITEM);
			//Log.d(TAG, "settings item = " + settingsItem.name());
			((OnSettingsItemClickedListener) FragmentUtil.getActivity(this)).onSettingsItemClicked(settingsItem, null);
		}
		
		Resources res = FragmentUtil.getResources(this);
		DisplayMetrics displaymetrics = new DisplayMetrics();
		FragmentUtil.getActivity(this).getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		htForSettingsList = displaymetrics.heightPixels - ViewUtil.getStatusBarHeight(FragmentUtil.getResources(this))
				- res.getDimensionPixelSize(R.dimen.action_bar_ht);
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
			settingsListAdapter = new SettingsListAdapter((Activity) FragmentUtil.getActivity(this), settingsItems, htForSettingsList);
			
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
		return ScreenNames.SETTINGS;
	}
	
	private static class SettingsListAdapter extends BaseAdapter {

		private List<SettingsItem> settingsItems;
		private WeakReference<Activity> mainActivity;
		private LayoutInflater inflater;
		private int rowHt;

		public SettingsListAdapter(Activity activity, List<SettingsItem> settingsMenuListItems, int htForSettingsList) {
			this.mainActivity = new WeakReference<Activity>(activity);
	        inflater = LayoutInflater.from(this.mainActivity.get());
	        this.settingsItems = settingsMenuListItems;
	        this.rowHt = htForSettingsList / settingsMenuListItems.size();
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
			final ListItemViewHolder listItemViewHolder;
			SettingsItem settingsItem = (SettingsItem) getItem(position);

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.list_item_settings, null);
				
				AbsListView.LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, rowHt);
				convertView.setLayoutParams(lp);

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
