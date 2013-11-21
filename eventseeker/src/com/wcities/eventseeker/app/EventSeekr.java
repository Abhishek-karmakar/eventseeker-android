package com.wcities.eventseeker.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.util.DisplayMetrics;

import com.wcities.eventseeker.ConnectAccountsFragment.Service;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.SharedPrefKeys;
import com.wcities.eventseeker.gcm.GcmUtil;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser;
import com.wcities.eventseeker.util.DeviceUtil;

public class EventSeekr extends Application {

	private static final String TAG = EventSeekr.class.getName();

	private boolean isTablet;
	private boolean isInLandscapeMode;


	private String fbUserId;
	private String wcitiesId;

	private String gcmRegistrationId;
	private int appVersionCode;
	private long gcmRegistrationExpirationTime;

	private static final int NOT_INITIALIZED = -1;
	public static final int UNSYNC_COUNT = -2;

	private int syncCountDeviceLib = NOT_INITIALIZED;
	private int syncCountTwitter = NOT_INITIALIZED;
	private int syncCountRdio = NOT_INITIALIZED;
	private int syncCountLastfm = NOT_INITIALIZED;
	private int syncCountPandora = NOT_INITIALIZED;

	private List<EventSeekrListener> listeners;

	public interface EventSeekrListener {
		public void onSyncCountUpdated(Service service);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		// Log.d(TAG, "onCreate()");
		listeners = new ArrayList<EventSeekr.EventSeekrListener>();

		new GcmUtil(EventSeekr.this).registerGCMInBackground();
		DeviceUtil.getLatLon(this);

		isTablet = getResources().getBoolean(R.bool.is_tablet);
		
	}
	
	public void registerListener(EventSeekrListener eventSeekrListener) {
		if (eventSeekrListener != null) {
			listeners.add(eventSeekrListener);
		}
	}

	public boolean isTablet() {
		return isTablet;
	}

	public boolean isInLandscapeMode() {
		return isInLandscapeMode;
	}

	public void checkAndSetIfInLandscapeMode() {
		DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
		int width = displayMetrics.widthPixels;
		int height = displayMetrics.heightPixels;
		isInLandscapeMode = (width > height);
	}
	
	public boolean isTabletAndInLandscapeMode() {
		return (isTablet && isInLandscapeMode);
	}

	public void unregisterListener(EventSeekrListener eventSeekrListener) {
		listeners.remove(eventSeekrListener);
	}

	public void updateGcmInfo(String gcmRegistrationId, int appVersionCode,
			long gcmRegistrationExpirationTime) {
		this.gcmRegistrationId = gcmRegistrationId;
		this.appVersionCode = appVersionCode;
		this.gcmRegistrationExpirationTime = gcmRegistrationExpirationTime;

		SharedPreferences pref = getSharedPreferences(
				AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.putString(SharedPrefKeys.GCM_REGISTRATION_ID, gcmRegistrationId);
		editor.putInt(SharedPrefKeys.APP_VERSION_CODE, appVersionCode);
		editor.putLong(SharedPrefKeys.GCM_REGISTRATION_EXPIRATION_TIME,
				gcmRegistrationExpirationTime);

		editor.commit();
	}

	public String getGcmRegistrationId() {
		if (gcmRegistrationId == null) {
			SharedPreferences pref = getSharedPreferences(
					AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
			gcmRegistrationId = pref.getString(
					SharedPrefKeys.GCM_REGISTRATION_ID, null);
		}
		return gcmRegistrationId;
	}

	public void updateGcmRegistrationId(String gcmRegistrationId) {
		this.gcmRegistrationId = gcmRegistrationId;

		SharedPreferences pref = getSharedPreferences(
				AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.putString(SharedPrefKeys.GCM_REGISTRATION_ID, gcmRegistrationId);
		editor.commit();
	}

	public int getAppVersionCode() {
		if (appVersionCode == 0) {
			SharedPreferences pref = getSharedPreferences(
					AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
			appVersionCode = pref.getInt(SharedPrefKeys.APP_VERSION_CODE, 0);
		}
		return appVersionCode;
	}

	public void updateAppVersionCode(int appVersionCode) {
		this.appVersionCode = appVersionCode;

		SharedPreferences pref = getSharedPreferences(
				AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.putInt(SharedPrefKeys.APP_VERSION_CODE, appVersionCode);
		editor.commit();
	}

	public long getGcmRegistrationExpirationTime() {
		if (gcmRegistrationExpirationTime == 0) {
			SharedPreferences pref = getSharedPreferences(
					AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
			gcmRegistrationExpirationTime = pref.getLong(
					SharedPrefKeys.GCM_REGISTRATION_EXPIRATION_TIME, 0);
		}
		return gcmRegistrationExpirationTime;
	}

	public void updateGcmRegistrationExpirationTime(
			long gcmRegistrationExpirationTime) {
		this.gcmRegistrationExpirationTime = gcmRegistrationExpirationTime;

		SharedPreferences pref = getSharedPreferences(
				AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.putLong(SharedPrefKeys.GCM_REGISTRATION_EXPIRATION_TIME,
				gcmRegistrationExpirationTime);
		editor.commit();
	}

	public String getFbUserId() {
		if (fbUserId == null) {
			SharedPreferences pref = getSharedPreferences(
					AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
			fbUserId = pref.getString(SharedPrefKeys.FACEBOOK_USER_ID, null);
		}
		return fbUserId;
	}

	public void updateFbUserId(String fbUserId, AsyncTaskListener listener) {
		this.fbUserId = fbUserId;

		SharedPreferences pref = getSharedPreferences(
				AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.putString(SharedPrefKeys.FACEBOOK_USER_ID, fbUserId);
		editor.commit();
		
		new GetWcitiesId(listener).execute();

	}

	public void removeFbUserId() {
		this.fbUserId = null;

		SharedPreferences pref = getSharedPreferences(
				AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.remove(SharedPrefKeys.FACEBOOK_USER_ID);
		editor.commit();
	}

	public String getWcitiesId() {
		if (wcitiesId == null) {
			SharedPreferences pref = getSharedPreferences(
					AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
			wcitiesId = pref.getString(SharedPrefKeys.WCITIES_USER_ID, null);
		}

		// generate wcitiesId if not found in shared preference & if fbUserId is
		// existing
		if (wcitiesId == null && getFbUserId() != null) {
			new GetWcitiesId(null).execute();
		}
		return wcitiesId;
	}

	private void updateWcitiesId(String wcitiesId) {
		this.wcitiesId = wcitiesId;

		SharedPreferences pref = getSharedPreferences(
				AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.putString(SharedPrefKeys.WCITIES_USER_ID, wcitiesId);
		editor.commit();

		new GcmUtil(this).registerGCMInBackground();
	}

	public int getSyncCount(Service service) {
		SharedPreferences pref = getSharedPreferences(
				AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);

		switch (service) {

		case DeviceLibrary:
			if (syncCountDeviceLib == NOT_INITIALIZED) {
				syncCountDeviceLib = pref.getInt(
						SharedPrefKeys.SYNC_COUNT_DEVICE_LIB, UNSYNC_COUNT);
			}
			return syncCountDeviceLib;

		case Twitter:
			if (syncCountTwitter == NOT_INITIALIZED) {
				syncCountTwitter = pref.getInt(
						SharedPrefKeys.SYNC_COUNT_TWITTER, UNSYNC_COUNT);
			}
			return syncCountTwitter;

		case Rdio:
			if (syncCountRdio == NOT_INITIALIZED) {
				syncCountRdio = pref.getInt(SharedPrefKeys.SYNC_COUNT_RDIO,
						UNSYNC_COUNT);
			}
			return syncCountRdio;

		case Lastfm:
			if (syncCountLastfm == NOT_INITIALIZED) {
				syncCountLastfm = pref.getInt(SharedPrefKeys.SYNC_COUNT_LASTFM,
						UNSYNC_COUNT);
			}
			return syncCountLastfm;

		case Pandora:
			if (syncCountPandora == NOT_INITIALIZED) {
				syncCountPandora = pref.getInt(
						SharedPrefKeys.SYNC_COUNT_PANDORA, UNSYNC_COUNT);
			}
			return syncCountPandora;

		default:
			break;
		}

		return UNSYNC_COUNT;
	}

	public void setSyncCount(Service service, int count) {
		SharedPreferences pref = getSharedPreferences(
				AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		Editor editor = pref.edit();

		switch (service) {

		case DeviceLibrary:
			syncCountDeviceLib = count;
			editor.putInt(SharedPrefKeys.SYNC_COUNT_DEVICE_LIB,
					syncCountDeviceLib);
			break;

		case Twitter:
			syncCountTwitter = count;
			editor.putInt(SharedPrefKeys.SYNC_COUNT_TWITTER, syncCountTwitter);
			break;

		case Rdio:
			syncCountRdio = count;
			editor.putInt(SharedPrefKeys.SYNC_COUNT_RDIO, syncCountRdio);
			break;

		case Lastfm:
			syncCountLastfm = count;
			editor.putInt(SharedPrefKeys.SYNC_COUNT_LASTFM, syncCountLastfm);
			break;

		case Pandora:
			syncCountPandora = count;
			editor.putInt(SharedPrefKeys.SYNC_COUNT_PANDORA, syncCountPandora);
			break;

		default:
			break;
		}

		editor.commit();

		for (Iterator<EventSeekrListener> iterator = listeners.iterator(); iterator
				.hasNext();) {
			EventSeekrListener listener = iterator.next();
			listener.onSyncCountUpdated(service);
		}
	}
	
	private class GetWcitiesId extends AsyncTask<Void, Void, String> {
		
		private AsyncTaskListener listener;
		
		public GetWcitiesId(AsyncTaskListener listener) {
			this.listener = listener;
		}

		@Override
		protected String doInBackground(Void... params) {
			String wcitiesId = null;
			UserInfoApi userInfoApi = new UserInfoApi(Api.OAUTH_TOKEN);
			userInfoApi.setDeviceId(DeviceUtil.getDeviceId(EventSeekr.this));
			try {
				JSONObject jsonObject = userInfoApi.signUp();
				UserInfoApiJSONParser jsonParser = new UserInfoApiJSONParser();
				String userId = jsonParser.getUserId(jsonObject);

				userInfoApi.setFbUserId(getFbUserId());
				userInfoApi.setUserId(userId);
				jsonObject = userInfoApi.syncAccount();
				wcitiesId = jsonParser.getWcitiesId(jsonObject);
				updateWcitiesId(wcitiesId);

			} catch (ClientProtocolException e) {
				e.printStackTrace();

			} catch (IOException e) {
				e.printStackTrace();

			} catch (JSONException e) {
				e.printStackTrace();
			}
			return wcitiesId;
		}
		
		@Override
		protected void onPostExecute(String wcitiesId) {
			if (wcitiesId != null) {
				updateWcitiesId(wcitiesId);
			}
			
			if (listener != null) {
				listener.onTaskCompleted();
			}
		}
	}
}
