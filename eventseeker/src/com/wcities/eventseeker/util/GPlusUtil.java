package com.wcities.eventseeker.util;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.app.Fragment;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.plus.PlusClient;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.SharedPrefKeys;

public class GPlusUtil {

	public static boolean hasUserLoggedInBefore(Context context) {
		//Log.d(TAG, "hasUserLoggedInBefore()");
		SharedPreferences pref = context.getSharedPreferences(
                AppConstants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
		return pref.contains(SharedPrefKeys.GOOGLE_PLUS_USER_ID);
	}
	
	public static void callGPlusLogout(PlusClient plusClient, EventSeekr eventSeekr) {
		if (plusClient.isConnected()) {
			plusClient.clearDefaultAccount();
			plusClient.disconnect();
			plusClient.connect();
			eventSeekr.removeGPlusUserId();
	        eventSeekr.removeGPlusUserName();
		}
	}
	
	public static void showDialogForGPlayServiceUnavailability(int available, Fragment fragment) {
		if (GooglePlayServicesUtil.isUserRecoverableError(available)) {
			Dialog dialog = GooglePlayServicesUtil.getErrorDialog(available, FragmentUtil.getActivity(fragment), 
					AppConstants.REQ_CODE_GET_GOOGLE_PLAY_SERVICES);
			dialog.show();
			
		} else {
			new AlertDialog.Builder(FragmentUtil.getActivity(fragment))
            .setMessage(R.string.plus_generic_error)
            .setCancelable(true)
            .create().show();
		}
	}
}
