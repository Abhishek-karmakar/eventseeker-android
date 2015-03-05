package com.wcities.eventseeker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.bosch.myspin.serversdk.MySpinException;
import com.bosch.myspin.serversdk.MySpinServerSDK;
import com.wcities.eventseeker.analytics.GoogleAnalyticsTracker;
import com.wcities.eventseeker.analytics.IGoogleAnalyticsTracker;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.util.VersionUtil;

public class SplashActivity extends Activity implements IGoogleAnalyticsTracker {

	private static final String TAG = SplashActivity.class.getSimpleName();
	
	private Handler handler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		try {
			MySpinServerSDK.sharedInstance().registerApplication(getApplication());
			
		} catch (MySpinException e) {
			e.printStackTrace();
		}
		
		setContentView(R.layout.activity_splash);
		
		VersionUtil.updateCheckes((EventSeekr) getApplication());
		GoogleAnalyticsTracker.getInstance().sendScreenView((EventSeekr) getApplication(), getScreenName());
		
		handler = new Handler(Looper.getMainLooper());
		handler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				//Log.d(TAG, "startActivity");
				Intent intent;
				if (((EventSeekr)getApplication()).isTablet()) {
					if (((EventSeekr)getApplication()).getWcitiesId() == null) {
						intent = new Intent(getApplicationContext(), LauncherActivityTab.class);
						
					} else {
						intent = new Intent(getApplicationContext(), DiscoverActivityTab.class);
					}
					
				} else {
					intent = new Intent(getApplicationContext(), MainActivity.class);
				}
				startActivity(intent);

				finish();
			}
		}, 1000);
	}
	
	@Override
	protected void onDestroy() {
		//Log.d(TAG, "onDestroy()");
		/**
		 * Remove pending callbacks especially to prevent multiple startActivity() calls by handler in following case:
		 * user changes orientation before handler has finished its task due to which both handlers (portrait &
		 * landscape) will call startActivity() resulting in 2 activity instances. 
		 */
		handler.removeCallbacksAndMessages(null);
		super.onDestroy();
		((EventSeekr)getApplication()).onActivityDestroyed();
	}

	@Override
	public String getScreenName() {
		return ScreenNames.SPLASH;
	}
}
