package com.wcities.eventseeker;

import java.util.List;

import android.os.Bundle;
import android.widget.ImageView;

import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.interfaces.EventListener;
import com.wcities.eventseeker.interfaces.EventListenerTab;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.SharedElement;

public class DiscoverActivityTab extends BaseActivityTab implements EventListenerTab {
	
	private static final String TAG = DiscoverActivityTab.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getWindow().requestFeature(android.view.Window.FEATURE_CONTENT_TRANSITIONS);
        getWindow().requestFeature(android.view.Window.FEATURE_ACTIVITY_TRANSITIONS);
        
		super.onCreate(savedInstanceState);
		//Log.d(TAG, "onCreate()");
		setContentView(R.layout.activity_base_tab);
		
		setCommonUI();
		removeToolbarElevation();
		
		if (isOnCreateCalledFirstTime) {
			//Log.d(TAG, "add login fragment tab");
			DiscoverFragmentTab discoverFragmentTab = new DiscoverFragmentTab();
			addFragment(R.id.content_frame, discoverFragmentTab, FragmentUtil.getTag(discoverFragmentTab), false);
		}
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
	public void onEventSelected(Event event, ImageView imageView) {
		super.onEventSelected(event, imageView);
	}
}
