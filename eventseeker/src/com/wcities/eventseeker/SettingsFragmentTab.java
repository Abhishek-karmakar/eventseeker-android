package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.res.Resources;
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

import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.constants.Enums.SettingsItem;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.VersionUtil;
import com.wcities.eventseeker.util.ViewUtil;

public class SettingsFragmentTab extends Fragment {
	
	private static final String TAG = SettingsFragmentTab.class.getSimpleName();
	
	private List<SettingsItem> settingsItems;
	private SettingsListAdapter settingsListAdapter;
	private RecyclerView rcyclrSettings;
	
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
    				rcyclrSettings.getViewTreeObserver().removeOnGlobalLayoutListener(this);

    			} else {
    				rcyclrSettings.getViewTreeObserver().removeGlobalOnLayoutListener(this);
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
		View view = inflater.inflate(R.layout.fragment_settings_tab, null);
		rcyclrSettings = (RecyclerView) view.findViewById(R.id.rcyclrSettings);
		// use a linear layout manager
		LinearLayoutManager layoutManager = new LinearLayoutManager(FragmentUtil.getActivity(this));
		layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
		rcyclrSettings.setLayoutManager(layoutManager);
		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (settingsItems == null) {
			settingsItems = new ArrayList<SettingsItem>(Arrays.asList(SettingsItem.values()));
			settingsListAdapter = new SettingsListAdapter(this, settingsItems);
		}
		rcyclrSettings.setAdapter(settingsListAdapter);
		
		rcyclrSettings.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
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
				rcyclrSettings.getViewTreeObserver().removeOnGlobalLayoutListener(onGlobalLayoutListener);
	
			} else {
				rcyclrSettings.getViewTreeObserver().removeGlobalOnLayoutListener(onGlobalLayoutListener);
			}
			
		} catch (NullPointerException ne) {
			// if listview is not yet created
			Log.e(TAG, ne.getMessage());
			
		} catch (IllegalStateException ie) {
			// if contentview is not yet created
			Log.e(TAG, ie.getMessage());
		}
	}
	
	private static class SettingsListAdapter extends RecyclerView.Adapter<VHSettings> {
		private List<SettingsItem> settingsItems;
		private int rowHt;
		private OnSettingsItemClickedListener onSettingsItemClickedListener;
		private Fragment fragment;
		private AdapterDataObserver adapterDataObserver;
		
		public SettingsListAdapter(Fragment fragment, List<SettingsItem> settingsMenuListItems) {
	        this.settingsItems = settingsMenuListItems;
	        this.fragment = fragment;
	        this.onSettingsItemClickedListener = (OnSettingsItemClickedListener) FragmentUtil.getActivity(fragment);
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
				try {
					unregisterAdapterDataObserver(adapterDataObserver);
					
				} catch (IllegalStateException e) {
					Log.e(TAG, "RecyclerViewDataObserver was not registered");
				}
			}
	        super.registerAdapterDataObserver(observer);
	        adapterDataObserver = observer;
	    }

		public void setHtForSettingsList(int htForSettingsList) {
			this.rowHt = htForSettingsList / settingsItems.size();
			notifyDataSetChanged();
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public int getItemCount() {
			return settingsItems.size();
		}

		@Override
		public VHSettings onCreateViewHolder(ViewGroup parent, int position) {
			View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_settings_tab, parent, false);
			VHSettings vhSettings = new VHSettings(v);
			return vhSettings;
		}

		@Override
		public void onBindViewHolder(VHSettings vhSettings, final int position) {
			SettingsItem settingsItem = (SettingsItem) settingsItems.get(position);

			AbsListView.LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, rowHt);
			vhSettings.convertView.setLayoutParams(lp);

			vhSettings.imgIcon.setImageDrawable(FragmentUtil.getResources(fragment)
				.getDrawable(settingsItem.getIcon()));

			vhSettings.txtTitle.setText(settingsItem.getTitle());
			
			/**
			 * This onClickListener is added as in Samsung Galaxy Tab, the If Settings screen is Started in
			 * Landscape mode then onListItemClick was not getting called. After that if change the Orientation 
			 * then it was working fine. So, resolved this issue by implementation.
			 */
			vhSettings.convertView.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					SettingsItem settingsItem = (SettingsItem) settingsItems.get(position);
					onSettingsItemClickedListener.onSettingsItemClicked(settingsItem, null);
				}
			});
		}
	}

	public static class VHSettings extends RecyclerView.ViewHolder {
		private ImageView imgIcon;
		private TextView txtTitle;
		private View convertView;
		
		public VHSettings(View convertView) {
			super(convertView);
			this.convertView = convertView;
			
			imgIcon = (ImageView) convertView.findViewById(R.id.imgIcon);
			txtTitle = (TextView) convertView.findViewById(R.id.txtTitle);
		}
	}
}
