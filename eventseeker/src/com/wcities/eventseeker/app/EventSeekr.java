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
import android.os.StrictMode;
import android.util.DisplayMetrics;
import android.util.Log;

import com.google.android.gms.auth.UserRecoverableAuthException;
import com.wcities.eventseeker.ConnectAccountsFragment.Service;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.api.UserInfoApi.LoginType;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.SharedPrefKeys;
import com.wcities.eventseeker.core.FollowingList;
import com.wcities.eventseeker.exception.DefaultUncaughtExceptionHandler;
import com.wcities.eventseeker.gcm.GcmUtil;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.interfaces.BoschAsyncTaskListener;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FbUtil;
import com.wcities.eventseeker.util.FileUtil;
import com.wcities.eventseeker.util.GPlusUtil;

public class EventSeekr extends Application {

	private static final String TAG = EventSeekr.class.getName();
	
	private boolean isTablet;
	private boolean is7InchTablet;
	private boolean isInLandscapeMode;

	private String fbUserId, gPlusUserId;
	private String fbUserName, gPlusUserName;
	private String wcitiesId;

	private boolean firstTimeLaunch;

	private String gcmRegistrationId;
	private int appVersionCode;
	private long gcmRegistrationExpirationTime;

	private static final int NOT_INITIALIZED = -1;
	public static final int UNSYNC_COUNT = -2;
	
	private static final int ALL_UNSYNCED_COUNT = UNSYNC_COUNT * 6;

	private int syncCountGooglePlayMusic = NOT_INITIALIZED;
	private int syncCountDeviceLib = NOT_INITIALIZED;
	private int syncCountTwitter = NOT_INITIALIZED;
	private int syncCountRdio = NOT_INITIALIZED;
	private int syncCountLastfm = NOT_INITIALIZED;
	private int syncCountPandora = NOT_INITIALIZED;

	private List<EventSeekrListener> listeners;

	private static BoschAsyncTaskListener boschAsyncTaskListener;
	
	private FollowingList followingList;

	public interface EventSeekrListener {
		public void onSyncCountUpdated(Service service);
	}
	
	public static BoschAsyncTaskListener getBoschAsyncTaskListener() {
		return boschAsyncTaskListener;
	}

	public static void setBoschAsyncTaskListener(BoschAsyncTaskListener boschAsyncTaskListener) {
		EventSeekr.boschAsyncTaskListener = boschAsyncTaskListener;
	}

	@Override
	public void onCreate() {
		// StrictMode testing
		if (AppConstants.STRICT_MODE_ENABLED) {
	         StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
	                 .detectAll()
	                 .penaltyLog()
	                 .build());
	         StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
	                 .detectAll()
	                 .penaltyLog()
	                 .build());
	    }
		
		super.onCreate();
		
		//Log.d(TAG, "onCreate()");
		listeners = new ArrayList<EventSeekr.EventSeekrListener>();

		initConfigParams();
		
		//ReportHandler.install(this, "ankur@wcities.com");
		if (AppConstants.CRASH_REPORTING_ENABLED) {
			Thread.setDefaultUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(this));
		}
		
		new GcmUtil(EventSeekr.this).registerGCMInBackground();
		DeviceUtil.getLatLon(this);

		isTablet = getResources().getBoolean(R.bool.is_tablet);
		is7InchTablet = getResources().getBoolean(R.bool.is_7_inch_tablet);
		//Log.d(TAG, "isTablet = " + isTablet);
		FileUtil.deleteShareImgCacheInBackground(this);
	}
	
	private void initConfigParams() {
		if (AppConstants.IS_RELEASE_MODE) {
			AppConstants.TWITTER_CONSUMER_KEY = "1l49nf8bD96xpgUbTrbzg";
			AppConstants.TWITTER_CONSUMER_SECRET = "9zzFZlJknYRIPrUrLSrTeVSTtkLHNP4d4fNwYHRP5Y";
			
			AppConstants.RDIO_KEY = "mbfj65dzv625pyvajtfqq6c2";
			AppConstants.RDIO_SECRET = "Wvvt3ysYdj";
			
			AppConstants.LASTFM_API_KEY = "dce45347e8ec4ce36c107d9d12549907";
			
			AppConstants.GCM_SENDER_ID = "972660105461";
			
		} else {
			AppConstants.TWITTER_CONSUMER_KEY = "Dt4IWLQhJmKVTdrfkvma7w";
			AppConstants.TWITTER_CONSUMER_SECRET = "MqQWwm7sEqHdTuU47grSTfV5fLct22RY4ilHXCjwA";
			
			AppConstants.RDIO_KEY = "x83dzkx2xdmxuqtguqdz2nj6";
			AppConstants.RDIO_SECRET = "rXNJ5ajSut";
			
			AppConstants.LASTFM_API_KEY = "5f7e82824ba8ba0fe1cbe2a6ea80472e";
			
			AppConstants.GCM_SENDER_ID = "802382771850";
		}
	}
	
	public void registerListener(EventSeekrListener eventSeekrListener) {
		if (eventSeekrListener != null) {
			listeners.add(eventSeekrListener);
		}
	}

	public boolean isTablet() {
		return isTablet;
	}

	public boolean isIs7InchTablet() {
		return is7InchTablet;
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
	
	public boolean isTabletAndInPortraitMode() {
		return (isTablet && !isInLandscapeMode);
	}

	public boolean is7InchTabletAndInPortraitMode() {
		return (is7InchTablet && !isInLandscapeMode);
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

	public void updateFbUserId(String fbUserId, AsyncTaskListener<Object> listener) {
		this.fbUserId = fbUserId;

		SharedPreferences pref = getSharedPreferences(
				AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.putString(SharedPrefKeys.FACEBOOK_USER_ID, fbUserId);
		editor.commit();
		
		GPlusUtil.callGPlusLogout(null, this);
		
		new GetWcitiesId(listener, LoginType.facebook, null).execute();
	}
	
	public void removeFbUserId() {
		this.fbUserId = null;

		SharedPreferences pref = getSharedPreferences(
				AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.remove(SharedPrefKeys.FACEBOOK_USER_ID);
		editor.commit();
	}
	
	public String getFbUserName() {
		if (fbUserName == null) {
			SharedPreferences pref = getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
			fbUserName = pref.getString(SharedPrefKeys.FACEBOOK_USER_NAME, null);
		}
		return fbUserName;
	}
	
	public void updateFbUserName(String fbUserName) {
		this.fbUserName = fbUserName;
		
		SharedPreferences pref = getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.putString(SharedPrefKeys.FACEBOOK_USER_NAME, fbUserName);
		editor.commit();
	}
	
	public void removeFbUserName() {
		this.fbUserName = null;

		SharedPreferences pref = getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.remove(SharedPrefKeys.FACEBOOK_USER_NAME);
		editor.commit();
	}
	
	public String getGPlusUserId() {
		if (gPlusUserId == null) {
			SharedPreferences pref = getSharedPreferences(
					AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
			gPlusUserId = pref.getString(SharedPrefKeys.GOOGLE_PLUS_USER_ID, null);
		}
		return gPlusUserId;
	}
	
	public void updateGPlusUserId(String gPlusUserId, String accountName, AsyncTaskListener<Object> listener) {
		this.gPlusUserId = gPlusUserId;

		SharedPreferences pref = getSharedPreferences(
				AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.putString(SharedPrefKeys.GOOGLE_PLUS_USER_ID, gPlusUserId);
		editor.commit();
		
		FbUtil.callFacebookLogout(this);
		
		updateGPlusAccountName(accountName);
		
		new GetWcitiesId(listener, LoginType.googlePlus, accountName).execute();
	}
	
	public void removeGPlusUserId() {
		this.gPlusUserId = null;

		SharedPreferences pref = getSharedPreferences(
				AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.remove(SharedPrefKeys.GOOGLE_PLUS_USER_ID);
		editor.commit();
	}
	
	public String getGPlusUserName() {
		if (gPlusUserName == null) {
			SharedPreferences pref = getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
			gPlusUserName = pref.getString(SharedPrefKeys.GOOGLE_PLUS_USER_NAME, null);
		}
		return gPlusUserName;
	}
	
	public void updateGPlusUserName(String gPlusUserName) {
		this.gPlusUserName = gPlusUserName;
		
		SharedPreferences pref = getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.putString(SharedPrefKeys.GOOGLE_PLUS_USER_NAME, gPlusUserName);
		editor.commit();
	}
	
	public void removeGPlusUserName() {
		this.gPlusUserName = null;

		SharedPreferences pref = getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.remove(SharedPrefKeys.GOOGLE_PLUS_USER_NAME);
		editor.commit();
	}
	
	public String getGPlusAccountName() {
		SharedPreferences pref = getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		return pref.getString(SharedPrefKeys.GOOGLE_PLUS_ACCOUNT_NAME, null);
	}
	
	public void updateGPlusAccountName(String gPlusAccountName) {
		SharedPreferences pref = getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.putString(SharedPrefKeys.GOOGLE_PLUS_ACCOUNT_NAME, gPlusAccountName);
		editor.commit();
	}
	
	public void removeGPlusAccountName() {
		SharedPreferences pref = getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.remove(SharedPrefKeys.GOOGLE_PLUS_ACCOUNT_NAME);
		editor.commit();
	}

	public String getWcitiesId() {
		if (wcitiesId == null) {
			SharedPreferences pref = getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
			wcitiesId = pref.getString(SharedPrefKeys.WCITIES_USER_ID, null);
		}

		// generate wcitiesId if not found in shared preference & if fbUserId or gPlusUserId is existing
		if (wcitiesId == null) {
			if (getFbUserId() != null) {
				new GetWcitiesId(null, LoginType.facebook, null).execute();
				
			} else if (getGPlusUserId() != null) {
				new GetWcitiesId(null, LoginType.googlePlus, getGPlusAccountName()).execute();
			}
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
	
	public boolean getFirstTimeLaunch() {
		SharedPreferences pref = getSharedPreferences(
				AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		firstTimeLaunch = pref.getBoolean(SharedPrefKeys.FIRST_TIME_LAUNCHED, true);
		
		return firstTimeLaunch;
	}
	
	public void updateFirstTimeLaunch(boolean firstTimeLaunch) {
		this.firstTimeLaunch = firstTimeLaunch;

		SharedPreferences pref = getSharedPreferences(
				AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.putBoolean(SharedPrefKeys.FIRST_TIME_LAUNCHED, firstTimeLaunch);
		editor.commit();
	}
	
	public FollowingList getCachedFollowingList() {
		if (followingList == null) {
			followingList = new FollowingList();
		}
		return followingList;
	}

	public int getSyncCount(Service service) {
		SharedPreferences pref = getSharedPreferences(
				AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);

		switch (service) {
		
		case GooglePlay:
			if (syncCountGooglePlayMusic == NOT_INITIALIZED) {
				syncCountGooglePlayMusic = pref.getInt(
						SharedPrefKeys.SYNC_COUNT_GOOGLE_PLAY_MUSIC, UNSYNC_COUNT);
			}
			return syncCountGooglePlayMusic;

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
		
		case GooglePlay:
			syncCountGooglePlayMusic = count;
			editor.putInt(SharedPrefKeys.SYNC_COUNT_GOOGLE_PLAY_MUSIC, syncCountGooglePlayMusic);
			break;

		case DeviceLibrary:
			syncCountDeviceLib = count;
			editor.putInt(SharedPrefKeys.SYNC_COUNT_DEVICE_LIB, syncCountDeviceLib);
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
	
	/**
	 * This function considers that all sync counts are already initialized.
	 * @return
	 */
	public boolean isAnyAccountSynced() {
		return ((syncCountGooglePlayMusic + syncCountDeviceLib + syncCountTwitter + syncCountRdio + 
				syncCountLastfm + syncCountPandora) == ALL_UNSYNCED_COUNT) ? false : true;
	}
	
	private class GetWcitiesId extends AsyncTask<Void, Void, String> {
		
		private AsyncTaskListener<Object> listener;
		private LoginType loginType;
		private String accountName;
		
		public GetWcitiesId(AsyncTaskListener<Object> listener, LoginType loginType, String accountName) {
			this.listener = listener;
			this.loginType = loginType;
			this.accountName = accountName;
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

				if (loginType == LoginType.facebook) {
					userInfoApi.setFbUserId(getFbUserId());
					
				} else if (loginType == LoginType.googlePlus) {
					userInfoApi.setgPlusUserId(getGPlusUserId());
				}
				userInfoApi.setUserId(userId);
				jsonObject = userInfoApi.syncAccount(null, loginType);
				wcitiesId = jsonParser.getWcitiesId(jsonObject);
				
				if (loginType == LoginType.googlePlus) {
					String accessToken = GPlusUtil.getAccessToken(EventSeekr.this, accountName);
					Log.d(TAG, "accessToken = " + accessToken);
					
					if (accessToken == null) {
						/**
						 * Return null from here so that it will try to update both wcitiesId & accesstoken 
						 * next time getWcitiesId() function is called up.
						 */
						return null;
						
					} else {
						jsonObject = userInfoApi.syncFriends(loginType, accessToken);
						return wcitiesId;
					}
					
				} else if (loginType == LoginType.facebook) {
					return wcitiesId;
				}
				
			} catch (ClientProtocolException e) {
				e.printStackTrace();

			} catch (IOException e) {
				e.printStackTrace();

			} catch (JSONException e) {
				e.printStackTrace();
			} 
			return null;
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
