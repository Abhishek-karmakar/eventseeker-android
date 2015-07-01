package com.wcities.eventseeker;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

import com.wcities.eventseeker.analytics.GoogleAnalyticsTracker;
import com.wcities.eventseeker.analytics.IGoogleAnalyticsTracker;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.util.FragmentUtil;

public class LauncherActivityTab extends BaseActivityTab implements IGoogleAnalyticsTracker {
	
	private static final String TAG = LauncherActivityTab.class.getSimpleName();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Log.d(TAG, "onCreate()");
		setContentView(R.layout.activity_base_tab);

		setCommonUI();

		updateUIAsPerLauncherFragment();

		//Log.d(TAG, "isOnCreateCalledFirstTime = " + isOnCreateCalledFirstTime);
		if (isOnCreateCalledFirstTime) {
			LauncherFragmentTab launcherFragmentTab = new LauncherFragmentTab();
			addFragment(R.id.content_frame, launcherFragmentTab, FragmentUtil.getTag(launcherFragmentTab), false);
		}
	}

	private void updateUIAsPerLauncherFragment() {
		getSupportActionBar().hide();
		findViewById(R.id.content_frame).setPadding(0, 0, 0, 0);
	}

	@Override
	protected void onStart() {
		super.onStart();
		setDrawerLockMode(true);
	}

	@Override
	protected String getScrnTitle() {
		//This is kept as blank screen as we aren't suppose to show Toolbar on this screen. Thus we are hiding
		//the toolbar in 'onCreate()' and thus there isn't any need to show the title.
		return "";
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		((EventSeekr)getApplication()).onActivityDestroyed();
	}

	@Override
	public String getScreenName() {
		return ScreenNames.LAUNCHER;
	}
}
