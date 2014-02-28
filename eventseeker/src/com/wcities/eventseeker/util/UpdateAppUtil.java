package com.wcities.eventseeker.util;

import android.content.pm.PackageManager.NameNotFoundException;

import com.wcities.eventseeker.app.EventSeekr;

public class UpdateAppUtil {

	private static final String TAG = UpdateAppUtil.class.getSimpleName();

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
}
