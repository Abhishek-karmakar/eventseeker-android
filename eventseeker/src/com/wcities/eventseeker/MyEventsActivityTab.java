package com.wcities.eventseeker;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.wcities.eventseeker.interfaces.FragmentLoadedFromBackstackListener;
import com.wcities.eventseeker.util.FragmentUtil;

public class MyEventsActivityTab extends BaseActivityTab implements FragmentLoadedFromBackstackListener {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Log.d(TAG, "onCreate()");
		setContentView(R.layout.activity_base_tab_with_tabs);
		
		setCommonUI();
		
		/**
		 * if user moves away quickly to any other screen resulting in fragment
		 * replacement & if we are adding this fragment into backstack, then
		 * orientation change made now will result in getActivity() returning
		 * null for all fragments existing in back stack. To resolve this we
		 * don't use getActivity() or getParentFragment().getActivity() call
		 * directly from fragment; rather we keep activity as instance variable
		 * of all such fragments & we keep this reference updated in all the
		 * back stack fragments by below call.
		 */
		FragmentUtil.updateActivityReferenceInAllFragments(getSupportFragmentManager(), this);
		
		if (isOnCreateCalledFirstTime) {
			//Log.d(TAG, "add settings fragment tab");
			MyEventsFragmentTab myEventsFragmentTab = new MyEventsFragmentTab();
			addFragment(R.id.content_frame, myEventsFragmentTab, FragmentUtil.getTag(myEventsFragmentTab), false);
		}
	}

	@Override
	public String getScreenName() {
		/**
		 * The ScreenName for the Google Analytics Tracker are handled in the child 
		 * Fragments of this Activity.
		 */
		return null;
	}

	@Override
	protected String getScrnTitle() {
		return getResources().getString(R.string.title_my_events);
	}
	
	@Override
	protected int getDrawerItemPos() {
		return INDEX_NAV_ITEM_MY_EVENTS;
	}
	
	@Override
	public void onFragmentResumed(Fragment fragment, int drawerPosition, String actionBarTitle) {
		updateTitleForFragment(actionBarTitle, currentContentFragmentTag);
		// Log.d(TAG, "got the current tag as : " + fragmentTag);
	}
	
	public void updateTitleForFragment(String newTitle, String fragmentTag) {
		if (newTitle != null) {
			getSupportActionBar().setTitle(newTitle);
		}
	}
	
	@Override
	public void onFragmentResumed(Fragment fragment) {}
}
