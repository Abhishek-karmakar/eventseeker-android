package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.constants.Enums.SettingsItem;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.VersionUtil;
import com.wcities.eventseeker.util.ViewUtil;

public class SettingsFragmentTab extends ListFragment {
	
	private static final String TAG = SettingsFragmentTab.class.getSimpleName();
	
	private List<SettingsItem> settingsItems;
	private SettingsListAdapter settingsListAdapter;

	private int htForSettingsList;
	
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
        	
    		Resources res = FragmentUtil.getResources(SettingsFragmentTab.this);
    		DisplayMetrics displaymetrics = new DisplayMetrics();
    		FragmentUtil.getActivity(SettingsFragmentTab.this).getWindowManager().getDefaultDisplay()
    			.getMetrics(displaymetrics);
    		
    		int lstHt = displaymetrics.heightPixels;
    		htForSettingsList = lstHt - res.getDimensionPixelSize(R.dimen.action_bar_ht)
    				/**
    				 * subtracting the StatusBar height
    				 */
    				- ViewUtil.getStatusBarHeight(res);
    		
    		settingsListAdapter.setHtForSettingsList(htForSettingsList);
        }
    };
	
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
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_settings_tab, null);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (settingsItems == null) {
			settingsItems = new ArrayList<SettingsItem>(Arrays.asList(SettingsItem.values()));
			settingsListAdapter = new SettingsListAdapter(this, settingsItems);
		}
		setListAdapter(settingsListAdapter);
		
        getListView().setDivider(null);
        getListView().getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
	}
	
	private static class SettingsListAdapter extends BaseAdapter {

		private List<SettingsItem> settingsItems;
		private LayoutInflater inflater;
		private int rowHt;
		private OnSettingsItemClickedListener onSettingsItemClickedListener;
		private Fragment fragment;
		
		public SettingsListAdapter(Fragment fragment, List<SettingsItem> settingsMenuListItems) {
	        inflater = LayoutInflater.from(FragmentUtil.getActivity(fragment));
	        this.settingsItems = settingsMenuListItems;
	        this.fragment = fragment;
	        this.onSettingsItemClickedListener = (OnSettingsItemClickedListener) FragmentUtil.getActivity(fragment);
		}

		public void setHtForSettingsList(int htForSettingsList) {
			this.rowHt = htForSettingsList / settingsItems.size();
			notifyDataSetChanged();
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
		public View getView(final int position, View convertView, ViewGroup parent) {
			final ListItemViewHolder listItemViewHolder;
			SettingsItem settingsItem = (SettingsItem) getItem(position);

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.list_item_settings_tab, null);

				listItemViewHolder = new ListItemViewHolder();
				listItemViewHolder.imgIcon = (ImageView) convertView.findViewById(R.id.imgIcon);
				listItemViewHolder.txtTitle = (TextView) convertView.findViewById(R.id.txtTitle);
				convertView.setTag(listItemViewHolder);

			} else {
				listItemViewHolder = (ListItemViewHolder) convertView.getTag();
			}

			AbsListView.LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, rowHt);
			convertView.setLayoutParams(lp);

			listItemViewHolder.imgIcon.setImageDrawable(FragmentUtil.getResources(fragment)
				.getDrawable(settingsItem.getIcon()));

			listItemViewHolder.txtTitle.setText(settingsItem.getTitle());
			
			/**
			 * This onClickListener is added as in Samsung Galaxy Tab, the If Settings screen is Started in
			 * Landscape mode then onListItemClick was not getting called. After that if change the Orientation 
			 * then it was working fine. So, resolved this issue by implementation.
			 */
			convertView.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					SettingsItem settingsItem = (SettingsItem) getItem(position);
					onSettingsItemClickedListener.onSettingsItemClicked(settingsItem, null);
				}
			});
			
			return convertView;
		}
				
		private static class ListItemViewHolder {
			private ImageView imgIcon;
			private TextView txtTitle;
			private Object tag;
		}
	}

	/*@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Log.d(TAG, "onListItemClick");
		SettingsItem settingsItem = (SettingsItem) l.getAdapter().getItem(position);
		((OnSettingsItemClickedListener) FragmentUtil.getActivity(this)).onSettingsItemClicked(settingsItem, null);
	}*/
	
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
}
