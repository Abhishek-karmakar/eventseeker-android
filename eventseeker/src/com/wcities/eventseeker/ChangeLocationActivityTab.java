package com.wcities.eventseeker;

import android.os.Bundle;
import android.view.KeyEvent;

import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.util.FragmentUtil;

public class ChangeLocationActivityTab extends BaseActivityTab {
	
	private static final String TAG = ChangeLocationActivityTab.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Log.d(TAG, "onCreate()");
		setContentView(R.layout.activity_base_tab);
		
		setCommonUI();
		
		if (isOnCreateCalledFirstTime) {
			//Log.d(TAG, "add settings fragment tab");
			ChangeLocationFragmentTab changeLocationFragmentTab = new ChangeLocationFragmentTab();
			addFragment(R.id.content_frame, changeLocationFragmentTab, FragmentUtil.getTag(changeLocationFragmentTab), false);
		}
	}

	@Override
	public String getScreenName() {
		return ScreenNames.CHANGE_LOCATION;
	}

	@Override
	protected String getScrnTitle() {
		return getResources().getString(R.string.title_change_location);
	}

	@Override
	public void onBackPressed() {
		onDrawerItemSelected(INDEX_NAV_ITEM_DISCOVER, null);
		finish();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			onBackPressed();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
}
