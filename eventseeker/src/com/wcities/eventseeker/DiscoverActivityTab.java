package com.wcities.eventseeker;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.wcities.eventseeker.DiscoverSettingDialogFragment.DiscoverSettingChangedListenerTab;
import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.interfaces.EventListenerTab;
import com.wcities.eventseeker.util.FragmentUtil;

public class DiscoverActivityTab extends BaseActivityTab implements EventListenerTab, DiscoverSettingChangedListenerTab {
	
	private static final String TAG = DiscoverActivityTab.class.getSimpleName();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(android.view.Window.FEATURE_ACTIVITY_TRANSITIONS);
        
		super.onCreate(savedInstanceState);
		//Log.d(TAG, "onCreate()");
		setContentView(R.layout.activity_base_tab);
		
		setCommonUI();
		removeToolbarElevation();
		
		if (isOnCreateCalledFirstTime) {
			//Log.d(TAG, "add login fragment tab");
			DiscoverFragmentTab discoverFragmentTab = new DiscoverFragmentTab();
			/**
			 * Args are used on coming back to this screen from change location due to no events found
			 * earlier on this screen showing change location button.
			 */
			discoverFragmentTab.setArguments(getIntent().getExtras());
			addFragment(R.id.content_frame, discoverFragmentTab, FragmentUtil.getTag(discoverFragmentTab), false);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_discover_tab, menu);
		setSearchView(menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.action_setting:
			DiscoverFragmentTab discoverFragmentTab = (DiscoverFragmentTab) getSupportFragmentManager()
					.findFragmentByTag(FragmentUtil.getTag(DiscoverFragmentTab.class));
			if (discoverFragmentTab != null) {
				discoverFragmentTab.onActionItemSettingsSelected();
			}
			return true;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public String getScreenName() {
		return ScreenNames.DISCOVER;
	}

	@Override
	protected String getScrnTitle() {
		return getResources().getString(R.string.title_discover);
	}
	
	@Override
	protected int getDrawerItemPos() {
		return INDEX_NAV_ITEM_DISCOVER;
	}

	@Override
	public void onEventSelected(Event event, ImageView imageView, TextView textView) {
		super.onEventSelected(event, imageView, textView);
	}

	@Override
	public void onSettingChanged(int year, int month, int day, int miles) {
		DiscoverFragmentTab discoverFragmentTab = (DiscoverFragmentTab) getSupportFragmentManager()
				.findFragmentByTag(FragmentUtil.getTag(DiscoverFragmentTab.class));
		if (discoverFragmentTab != null) {
			discoverFragmentTab.onSettingChanged(year, month, day, miles);
		}
	}
}
