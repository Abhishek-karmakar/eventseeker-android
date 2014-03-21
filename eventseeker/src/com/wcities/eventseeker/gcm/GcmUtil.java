package com.wcities.eventseeker.gcm;

import java.io.IOException;
import java.sql.Timestamp;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.AppConstants;

public class GcmUtil {

	private static final String TAG = GcmUtil.class.getName();
	
	// Default lifespan (7 days) of a reservation until it is considered expired.
	private static final long REGISTRATION_EXPIRY_TIME_MS = 1000 * 3600 * 24 * 7;
	
	private EventSeekr eventSeekr;
	private GoogleCloudMessaging gcm;

	public GcmUtil(EventSeekr eventSeekr) {
		this.eventSeekr = eventSeekr;
	}

	public void registerGCMInBackground(final boolean forceNewRegistration) {
    	Log.d(TAG, "registerGCMInBackground()");

		new AsyncTask<Void, Void, Void>() {
	    	
	        @Override
	        protected Void doInBackground(Void... params) {
	        	String wcitiesId = eventSeekr.getWcitiesId();
	        	Log.d(TAG, "wcitiesId=" + wcitiesId);
	        	
	        	if (wcitiesId != null) {
	        		String regId = getRegistrationId();
		    		//Log.d(TAG, "regId = " + regId);
		    		
		            if (regId.length() == 0 || forceNewRegistration) {
		                register();
		            }
	        	}
	    		
	            return null;
	        }
	    }.execute();
	}
	
	/**
	 * Gets the current registration id for application on GCM service.
	 * <p>
	 * If result is null, the registration has failed.
	 *
	 * @return registration id, or empty string if the registration is not
	 *         complete.
	 */
	private String getRegistrationId() {
	    String gcmRegistrationId = eventSeekr.getGcmRegistrationId();
	    if (gcmRegistrationId == null) {
	        Log.v(TAG, "Registration not found.");
	        return "";
	    }
	    
	    // check if app was updated; if so, it must clear registration id to
	    // avoid a race condition if GCM sends a message
	    int registeredVersion = eventSeekr.getAppVersionCode();
	    int currentVersion = getAppVersion();
	    if (registeredVersion != currentVersion) {
	    	Log.v(TAG, "App version changed or registration expired.");
	    	eventSeekr.updateAppVersionCode(currentVersion);
	        return "";
	    }
	    
	    if (isRegistrationExpired()) {
	        Log.v(TAG, "App version changed or registration expired.");
	        return "";
	    }
	    
	    return gcmRegistrationId;
	}
	
	/**
	 * @return Application's version code from the {@code PackageManager}.
	 */
	private int getAppVersion() {
	    try {
	        PackageInfo packageInfo = eventSeekr.getPackageManager().getPackageInfo(eventSeekr.getPackageName(), 0);
	        return packageInfo.versionCode;
	        
	    } catch (NameNotFoundException e) {
	        // should never happen
	        throw new RuntimeException("Could not get package name: " + e);
	    }
	}
	
	/**
	 * Checks if the registration has expired.
	 *
	 * <p>To avoid the scenario where the device sends the registration to the
	 * server but the server loses it, the app developer may choose to re-register
	 * after REGISTRATION_EXPIRY_TIME_MS.
	 *
	 * @return true if the registration has expired.
	 */
	private boolean isRegistrationExpired() {
	    // checks if the information is not stale
	    long expirationTime = eventSeekr.getGcmRegistrationExpirationTime();
	    return System.currentTimeMillis() > expirationTime;
	}
	
	private void register() {
		Log.d(TAG, "register()");
		
		try {
            if (gcm == null) {
                gcm = GoogleCloudMessaging.getInstance(eventSeekr);
            }
            
            Log.d(TAG, "register in bg()");
            String regId = gcm.register(AppConstants.GCM_SENDER_ID);
            Log.d(TAG, "GCM regId = " + regId);

            // You should send the registration ID to your server over HTTP,
            // so it can use GCM/HTTP or CCS to send messages to your app.
            // Save the regid - no need to register again.
            setGCMRegistrationId(regId);
            
        } catch (IOException ex) {
            Log.e(TAG, "Error :" + ex.getMessage());
        }
	}
	
	/**
	 * Stores the registration id, app versionCode, and expiration time in the
	 * {@code EventSeekrSettings}.
	 *
	 * @param context application's context.
	 * @param regId registration id
	 */
	private void setGCMRegistrationId(String gcmRegistrationId) {
		try {
			registerGcmIdForNotification(eventSeekr.getWcitiesId(), gcmRegistrationId);
			int appVersion = getAppVersion();
		    Log.d(TAG, "Saving regId on app version " + appVersion);
		    long expirationTime = System.currentTimeMillis() + REGISTRATION_EXPIRY_TIME_MS;
		    Log.d(TAG, "Setting registration expiry time to " + new Timestamp(expirationTime));
		    
		    eventSeekr.updateGcmInfo(gcmRegistrationId, appVersion, expirationTime);
		    
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	private void registerGcmIdForNotification(String wcitiesId, String gcmRegistrationId) throws ClientProtocolException, IOException, JSONException {
		UserInfoApi userInfoApi = new UserInfoApi(Api.OAUTH_TOKEN);
		userInfoApi.setUserId(wcitiesId);
		userInfoApi.setGcmRegistrationId(gcmRegistrationId);
		
		JSONObject jsonObject = userInfoApi.registerDevice();
		Log.d(TAG, "registerDevice response = " + jsonObject.toString());
	}
}
