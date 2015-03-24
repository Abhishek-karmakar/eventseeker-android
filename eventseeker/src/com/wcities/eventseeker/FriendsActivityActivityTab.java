package com.wcities.eventseeker;

import java.util.List;

import android.os.Bundle;

import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.interfaces.EventListener;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.SharedElement;

public class FriendsActivityActivityTab extends BaseActivityTab implements EventListener {
	
	private static final String TAG = FriendsActivityActivityTab.class.getSimpleName();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Log.d(TAG, "onCreate()");
		setContentView(R.layout.activity_base_tab);
		
		setCommonUI();
		
		if (isOnCreateCalledFirstTime) {
			FriendsActivityFragmentTab friendsActivityFragmentTab = new FriendsActivityFragmentTab();
			addFragment(R.id.content_frame, friendsActivityFragmentTab, FragmentUtil.getTag(friendsActivityFragmentTab), false);
		}
		
		if (savedInstanceState != null) {
			currentContentFragmentTag = savedInstanceState.getString(BundleKeys.CURRENT_CONTENT_FRAGMENT_TAG);
		}
	}

	@Override
	public String getScreenName() {
		return ScreenNames.FRIENDS_NEWS_SCREEN;
	}

	@Override
	protected String getScrnTitle() {
		return getResources().getString(R.string.title_friends_activity);
	}
	
	@Override
	protected int getDrawerItemPos() {
		return INDEX_NAV_ITEM_FRIENDS_ACTIVITY;
	}

	@Override
	public void onEventSelected(Event event) {
		EventDetailsFragment eventDetailsFragment = new EventDetailsFragment();
		Bundle args = new Bundle();
		args.putSerializable(BundleKeys.EVENT, event);
		eventDetailsFragment.setArguments(args);
	}

	@Override
	public void onEventSelected(Event event, List<SharedElement> sharedElements) {
		// TODO Auto-generated method stub
		
	}
}
