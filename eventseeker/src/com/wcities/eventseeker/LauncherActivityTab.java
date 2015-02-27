package com.wcities.eventseeker;

import com.wcities.eventseeker.analytics.GoogleAnalyticsTracker;
import com.wcities.eventseeker.analytics.IGoogleAnalyticsTracker;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.util.FragmentUtil;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

public class LauncherActivityTab extends FragmentActivity implements IGoogleAnalyticsTracker {
	
	private static final String TAG = LauncherActivityTab.class.getSimpleName();
	
	private boolean isOnCreateCalledFirstTime = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_launcher_tab);
		
		if (savedInstanceState != null) {
			isOnCreateCalledFirstTime = savedInstanceState.getBoolean(BundleKeys.IS_ON_CREATE_CALLED_FIRST_TIME);
		}
		
		//Log.d(TAG, "isOnCreateCalledFirstTime = " + isOnCreateCalledFirstTime);
		if (isOnCreateCalledFirstTime) {
			GoogleAnalyticsTracker.getInstance().sendScreenView((EventSeekr) getApplication(), getScreenName());
			isOnCreateCalledFirstTime = false;
		}
		
		if (getFragmentManager().findFragmentByTag(FragmentUtil.getSupportTag(LauncherFragmentTab.class)) == null) {
			FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
			LauncherFragmentTab launcherFragmentTab = new LauncherFragmentTab();
			fragmentTransaction.add(R.id.lnrLytRoot, launcherFragmentTab, FragmentUtil.getTag(launcherFragmentTab));
			fragmentTransaction.commit();
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(BundleKeys.IS_ON_CREATE_CALLED_FIRST_TIME, isOnCreateCalledFirstTime);
	}

	@Override
	public String getScreenName() {
		return ScreenNames.LAUNCHER;
	}
}
