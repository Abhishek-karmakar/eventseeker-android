package com.wcities.eventseeker.util;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.facebook.Request;
import com.facebook.Request.GraphUserCallback;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.Session.StatusCallback;
import com.facebook.model.GraphUser;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.SharedPrefKeys;

public class FbUtil {

	private static final String TAG = FbUtil.class.getName();

	public static boolean hasUserLoggedInBefore(Context context) {
		Log.d(TAG, "hasUserLoggedInBefore()");
		SharedPreferences pref = context.getSharedPreferences(
                AppConstants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
		return pref.contains(SharedPrefKeys.FACEBOOK_USER_ID);
	}
	
	public static void callFacebookLogout(EventSeekr eventSeekr) {
	    Session session = Session.getActiveSession();
	    if (session != null) {
	        //if (!session.isClosed()) {
	            session.closeAndClearTokenInformation();
	            //clear your preferences if saved
	            eventSeekr.removeFbUserId();
	        //}
	        
	    } else {
	        session = new Session(eventSeekr);
	        Session.setActiveSession(session);

	        session.closeAndClearTokenInformation();
	        
	        //clear your preferences if saved
	        eventSeekr.removeFbUserId();
	    }
	}
	
	public static void onClickLogin(Fragment fragment, StatusCallback statusCallback) {
        Session session = Session.getActiveSession();
        if (!session.isOpened() && !session.isClosed()) {
            session.openForRead(new Session.OpenRequest(fragment).setCallback(statusCallback));
            
        } else {
            Session.openActiveSession(FragmentUtil.getActivity(fragment), fragment, true, statusCallback);
        }
    }
	
	public static void makeMeRequest(final Session session, GraphUserCallback graphUserCallback) {
	    // Make an API call to get user data and define a 
	    // new callback to handle the response.
	    Request request = Request.newMeRequest(session, graphUserCallback);
	    request.executeAsync();
	} 
}
