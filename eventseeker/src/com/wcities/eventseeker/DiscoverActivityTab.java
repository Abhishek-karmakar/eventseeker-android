package com.wcities.eventseeker;

import android.os.Bundle;
import android.transition.Transition;
import android.transition.TransitionInflater;

import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.util.FragmentUtil;

public class DiscoverActivityTab extends BaseActivityTab {
	
	private static final String TAG = DiscoverActivityTab.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getWindow().requestFeature(android.view.Window.FEATURE_CONTENT_TRANSITIONS);
        getWindow().requestFeature(android.view.Window.FEATURE_ACTIVITY_TRANSITIONS);
        Transition transition = TransitionInflater.from(this).inflateTransition(R.transition.change_image_transform);
		getWindow().setSharedElementEnterTransition(transition);
		getWindow().setSharedElementExitTransition(transition);
        
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
}
