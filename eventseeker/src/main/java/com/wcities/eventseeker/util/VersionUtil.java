package com.wcities.eventseeker.util;

import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;

import com.wcities.eventseeker.app.EventSeekr;

public class VersionUtil {

	private static final String TAG = VersionUtil.class.getSimpleName();

	public static void updateCheckes(EventSeekr eventSeekr) {
		try {
			int currentAppVersion = eventSeekr.getPackageManager().getPackageInfo(eventSeekr.getPackageName(), 
					0).versionCode;
			//Log.d(TAG, "updateCheckes() - currentAppVersion = " + currentAppVersion);

			if (currentAppVersion > eventSeekr.getAppVersionCodeForUpgrades()) {
				//Log.d(TAG, "currentAppVersion > eventSeekr.getAppVersionCodeForUpgrades()");
				if (currentAppVersion == 4) {
					//Log.d(TAG, "currentAppVersion == 4");
					updatesForV4(eventSeekr);
				}
				eventSeekr.updateAppVersionCodeForUpgrades(currentAppVersion);
			}
			
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	private static void updatesForV4(EventSeekr eventSeekr) {
		FbUtil.callFacebookLogout(eventSeekr);
		GPlusUtil.callGPlusLogout(null, eventSeekr);
		eventSeekr.removeWcitiesId();
	}
	
	public static boolean isApiLevelAbove10() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
	}
	
	public static boolean isApiLevelAbove13() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
	}
	
	public static boolean isApiLevelAbove15() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
	}
	
	public static boolean isApiLevelAbove18() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
	}
	
	public static boolean isApiLevelAbove20() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
	}
}
