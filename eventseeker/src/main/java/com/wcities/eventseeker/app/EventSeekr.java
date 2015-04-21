package com.wcities.eventseeker.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.util.DisplayMetrics;
import android.util.Log;

import com.bosch.myspin.serversdk.MySpinServerSDK;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Logger.LogLevel;
import com.google.android.gms.common.api.GoogleApiClient;
import com.wcities.eventseeker.BaseActivity;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi.LoginType;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.Enums.Locales;
import com.wcities.eventseeker.constants.Enums.Service;
import com.wcities.eventseeker.constants.SharedPrefKeys;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.FollowingList;
import com.wcities.eventseeker.core.registration.Registration;
import com.wcities.eventseeker.exception.DefaultUncaughtExceptionHandler;
import com.wcities.eventseeker.gcm.GcmUtil;
import com.wcities.eventseeker.interfaces.ActivityDestroyedListener;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.interfaces.ConnectionFailureListener;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FileUtil;

public class EventSeekr extends Application {

	private static final String TAG = EventSeekr.class.getSimpleName();
	
	/**
	 * This will hold the value of Device specific Locale. Reason to create :- after setting the default locale 
	 * in 'setDefaultLocale()' when we try to get Country code in 'getCurrentProximityUnit()' it was getting 
	 * Country code as "". So, whatever be the Locale changed by the user outside of the app, the Proximity unit
	 * was not getting changed accordingly but it was constantly MI. So, now this field holds the value and gets
	 * notified for being updated in 'onConfigurationChanged()'
	 */
	private static Locale ACTUAL_SYSTEM_LOCALE;
	
	private static EventSeekr eventSeekr;
	
	private boolean isTablet;
	private boolean is7InchTablet;
	private boolean isInLandscapeMode;

	private String fbUserId, gPlusUserId;
	private String fbUserName, gPlusUserName;
	private String fbEmailId, gPlusEmailId;
	private String emailId, password, firstName, lastName;
	private String wcitiesId;
	
	private String previousWcitiesId;

	private boolean firstTimeLaunch;
	/**
	 * This variable is to determine whether the current Event is First Event.
	 * After app is (re)initialized.
	 */
	private boolean isFirstEventTitleForFordEventAL = true;
	private boolean isFirstEventDetailsForFordEventAL = true;
	private boolean isFirstArtistTitleForFord = true;

	private String gcmRegistrationId;
	private int appVersionCode;

	private static final int NOT_INITIALIZED = -1;
	public static final int UNSYNC_COUNT = -2;
	
	private static final int ALL_UNSYNCED_COUNT = UNSYNC_COUNT * Service.getServiceCount();

	private int syncCountGooglePlayMusic = NOT_INITIALIZED;
	private int syncCountDeviceLib = NOT_INITIALIZED;
	private int syncCountTwitter = NOT_INITIALIZED;
	private int syncCountSpotify = NOT_INITIALIZED;
	private int syncCountRdio = NOT_INITIALIZED;
	private int syncCountLastfm = NOT_INITIALIZED;
	private int syncCountPandora = NOT_INITIALIZED;

	private List<EventSeekrListener> listeners;

	private static ConnectionFailureListener connectionFailureListener;
	
	private FollowingList followingList;

	private Locales defaultLocale, fordDefaultLocale;

	private static String cityName;
	
	private Event eventToAddToCalendar;
	
	private int uniqueGcmNotificationId = AppConstants.UNIQUE_GCM_NOTIFICATION_ID_START;
	
	private double curLat = AppConstants.NOT_ALLOWED_LAT, curLon = AppConstants.NOT_ALLOWED_LON;

	private ProximityUnit savedProximityUnit;
	
	private Handler handler;
	private ActivityDestroyedListener activityDestroyedListener;
	
	public static GoogleApiClient mGoogleApiClient;
	// for ford
	private static BaseActivity currentBaseActivity;
	
	static {
		ACTUAL_SYSTEM_LOCALE = Locale.getDefault();
	}
	
	public static enum ProximityUnit {
		MI(R.string.unit_mi, R.string.unit_miles),
		KM(R.string.unit_km, R.string.unit_kilometers);
		
		// 1km = 0.621371mi
		public static final double CONVERSION_FACTOR = 0.621371;
		private int unitStrResId, fullFormResId;

		private ProximityUnit(int strFormResId, int fullFormResId) {
			this.unitStrResId = strFormResId;
			this.fullFormResId = fullFormResId;
		}

		public String toString(Context context) {
			return context.getResources().getString(unitStrResId);
		}
		
		public String getFullForm(Context context) {
			return context.getResources().getString(fullFormResId);
		}

		public static int convertMiToKm(double mi) {
			return ConversionUtil.doubleToIntRoundOff(mi / CONVERSION_FACTOR);
		}
		
		public static int convertKmToMi(double km) {
			return ConversionUtil.doubleToIntRoundOff(km * CONVERSION_FACTOR);
		}

		public static ProximityUnit getProximityUnitByOrdinal(int proximityUnitOrdinal, Context context) {
			ProximityUnit[] pu = ProximityUnit.values();
			for (ProximityUnit proximityUnit : pu) {
				if (proximityUnitOrdinal == proximityUnit.ordinal()) {
					return proximityUnit;
				}
			}
			return null;
		}
	}	
	
	public interface EventSeekrListener {
		public void onSyncCountUpdated(Service service);
	}
	
	public static ConnectionFailureListener getConnectionFailureListener() {
		return connectionFailureListener;
	}

	public static void setConnectionFailureListener(ConnectionFailureListener connectionFailureListener) {
		EventSeekr.connectionFailureListener = connectionFailureListener;
	}

	public static void resetConnectionFailureListener(ConnectionFailureListener connectionFailureListener) {
		if (EventSeekr.connectionFailureListener == connectionFailureListener) {
			EventSeekr.connectionFailureListener = null;
		}
	}

	/**
	 * @return null, if app is connected to bosch & car is in moving mode, so that based on current lat-lng
	 * cityname has to be derived by caller; otherwise it returns last cityname set.
	 */
	public static String getCityName() {
		if (MySpinServerSDK.sharedInstance().isConnected() && !AppConstants.IS_CAR_STATIONARY) {
			return null;
			
		} else {
			return cityName;
		}
	}

	public static void setCityName(String cityName) {
		EventSeekr.cityName = cityName;
	}

	public boolean isFirstEventTitleForFordEventAL() {
		return isFirstEventTitleForFordEventAL;
	}

	public void setFirstEventTitleForFordEventAL(boolean isFirstEventTitleForFordEventAL) {
		this.isFirstEventTitleForFordEventAL = isFirstEventTitleForFordEventAL;
	}
	
	public boolean isFirstArtistTitleForFord() {
		return isFirstArtistTitleForFord;
	}
	
	public void setFirstArtistTitleForFord(boolean isFirstArtistTitleForFord) {
		this.isFirstArtistTitleForFord = isFirstArtistTitleForFord;
	}

	public boolean isFirstEventDetailsForFordEventAL() {
		return isFirstEventDetailsForFordEventAL;
	}

	public void setFirstEventDetailsForFordEventAL(boolean isFirstEventDetailsForFordEventAL) {
		this.isFirstEventDetailsForFordEventAL = isFirstEventDetailsForFordEventAL;
	}
	
	public static boolean isConnectedWithBosch() {
		return MySpinServerSDK.sharedInstance().isConnected();
	}
	
	public static BaseActivity getCurrentBaseActivity() {
		return currentBaseActivity;
	}

	public static void setCurrentBaseActivity(BaseActivity currentBaseActivity) {
		EventSeekr.currentBaseActivity = currentBaseActivity;
	}
	
	public static void resetCurrentBaseActivityFor(BaseActivity currentBaseActivity) {
		/**
		 * reset to null only if it's called by same activity which had called setCurrentBaseActivity() last
		 * time, because otherwise it's possible that activity A starts activity B where onStart() of B gets 
		 * called up first followed by onStop() of A, resulting in first currentBaseActivity set to B & then
		 * to null by A, which we don't want, since B is right value in this case; whereas if last activity's 
		 * onStop() is getting called up then in that case this method will rightly set currentBaseActivity
		 * to null.
		 */
		if (currentBaseActivity == EventSeekr.currentBaseActivity) {
			EventSeekr.currentBaseActivity = null;
		}
	}

	@Override
	public void onCreate() {
		// StrictMode testing
		if (AppConstants.STRICT_MODE_ENABLED) {
	         StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog()
	                 .build());
	         StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());
	    }
		
		super.onCreate();
		
		eventSeekr = this;
		//Log.d(TAG, "onCreate()");
		listeners = new ArrayList<EventSeekr.EventSeekrListener>();

		initConfigParams();
		
		//ReportHandler.install(this, "ankur@wcities.com");
		if (AppConstants.CRASH_REPORTING_ENABLED) {
			Thread.setDefaultUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(this));
		}
		
		new GcmUtil(EventSeekr.this).registerGCMInBackground(false);
		DeviceUtil.getLatLon(this);

		isTablet = getResources().getBoolean(R.bool.is_tablet);
		is7InchTablet = getResources().getBoolean(R.bool.is_7_inch_tablet);
		//Log.d(TAG, "isTablet = " + isTablet);
		FileUtil.deleteShareImgCacheInBackground(this);
		
		handler = new Handler(Looper.getMainLooper());
		
		if (!AppConstants.SEND_GOOGLE_ANALYTICS) {
			// When dry run is set, hits will not be dispatched, but will still be logged as
			// though they were dispatched.
			GoogleAnalytics.getInstance(this).setDryRun(true);
			// Set the log level to verbose.
			GoogleAnalytics.getInstance(this).getLogger().setLogLevel(LogLevel.VERBOSE);
		}
	}
	
	private void initConfigParams() {
		if (AppConstants.IS_RELEASE_MODE) {
			AppConstants.TWITTER_CONSUMER_KEY = "1l49nf8bD96xpgUbTrbzg";
			AppConstants.TWITTER_CONSUMER_SECRET = "9zzFZlJknYRIPrUrLSrTeVSTtkLHNP4d4fNwYHRP5Y";
			
			AppConstants.RDIO_KEY = "mbfj65dzv625pyvajtfqq6c2";
			AppConstants.RDIO_SECRET = "Wvvt3ysYdj";
			
			AppConstants.LASTFM_API_KEY = "dce45347e8ec4ce36c107d9d12549907";
			
			AppConstants.SPOTIFY_CLIENT_ID = "2aeb9eb4ddf04129b8a1e4b00420756d";
			AppConstants.SPOTIFY_CLIENT_SECRET = "5775d920e70a4eaa98955486e790e7f7";
			
			AppConstants.GCM_SENDER_ID = "972660105461";
			
			AppConstants.BROWSER_KEY_FOR_PLACES_API = "AIzaSyBOTuIbME9QmZsfoBFFLa38zjTGJ-jWT-k";
			
		} else {
			AppConstants.TWITTER_CONSUMER_KEY = "Dt4IWLQhJmKVTdrfkvma7w";
			AppConstants.TWITTER_CONSUMER_SECRET = "MqQWwm7sEqHdTuU47grSTfV5fLct22RY4ilHXCjwA";
			
			AppConstants.RDIO_KEY = "x83dzkx2xdmxuqtguqdz2nj6";
			AppConstants.RDIO_SECRET = "rXNJ5ajSut";
			
			AppConstants.LASTFM_API_KEY = "5f7e82824ba8ba0fe1cbe2a6ea80472e";
			
			AppConstants.SPOTIFY_CLIENT_ID = "f875bc46fc284a31af776cedf8076bc8";
			AppConstants.SPOTIFY_CLIENT_SECRET = "0c1e396da5a54fa18fa849bd4e8a14b9";
			
			AppConstants.GCM_SENDER_ID = "802382771850";
			
			AppConstants.BROWSER_KEY_FOR_PLACES_API = "AIzaSyBHlh_Iyq33PY1zx9uSAT0isCGgL1wxwb4";
		}
	}
	
	public static EventSeekr getEventSeekr() {
		return eventSeekr;
	}

	public void registerListener(EventSeekrListener eventSeekrListener) {
		if (eventSeekrListener != null) {
			listeners.add(eventSeekrListener);
		}
	}

	public boolean isTablet() {
		return isTablet;
	}

	public boolean is7InchTablet() {
		return is7InchTablet;
	}

	public void checkAndSetIfInLandscapeMode() {
		//Log.d(TAG, "checkAndSetIfInLandscapeMode()");
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
	
	public boolean is10InchTabletAndInPortraitMode() {
		return (isTablet && !is7InchTablet && !isInLandscapeMode);
	}

	public void unregisterListener(EventSeekrListener eventSeekrListener) {
		listeners.remove(eventSeekrListener);
	}

	public void updateGcmInfo(String gcmRegistrationId, int appVersionCode) {
		this.gcmRegistrationId = gcmRegistrationId;
		this.appVersionCode = appVersionCode;

		SharedPreferences pref = getSharedPreferences(
				AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.putString(SharedPrefKeys.GCM_REGISTRATION_ID, gcmRegistrationId);
		editor.putInt(SharedPrefKeys.APP_VERSION_CODE, appVersionCode);

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
	
	public int getAppVersionCodeForUpgrades() {
		SharedPreferences pref = getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		return pref.getInt(SharedPrefKeys.APP_VERSION_CODE_FOR_UPGRADES, 0);
	}

	public void updateAppVersionCodeForUpgrades(int appVersionCodeForUpgrades) {
		SharedPreferences pref = getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.putInt(SharedPrefKeys.APP_VERSION_CODE_FOR_UPGRADES, appVersionCodeForUpgrades);
		editor.commit();
	}

	public String getFbUserId() {
		if (fbUserId == null) {
			SharedPreferences pref = getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
			fbUserId = pref.getString(SharedPrefKeys.FACEBOOK_USER_ID, null);
		}
		return fbUserId;
	}
	
	public String getFbUserName() {
		if (fbUserName == null) {
			SharedPreferences pref = getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
			fbUserName = pref.getString(SharedPrefKeys.FACEBOOK_USER_NAME, null);
		}
		return fbUserName;
	}
	
	public String getFbEmailId() {
		if (fbEmailId == null) {
			SharedPreferences pref = getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
			fbEmailId = pref.getString(SharedPrefKeys.FACEBOOK_EMAIL_ID, null);
		}
		return fbEmailId;
	}

	public boolean updateFbUserInfo(String fbUserId, String fbUserName, String fbEmailId, 
			AsyncTaskListener<Object> listener) {
		this.fbUserId = fbUserId;
		this.fbUserName = fbUserName;
		this.fbEmailId = fbEmailId;
		
		SharedPreferences pref = getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.putString(SharedPrefKeys.FACEBOOK_USER_ID, fbUserId);
		editor.putString(SharedPrefKeys.FACEBOOK_USER_NAME, fbUserName);
		editor.putString(SharedPrefKeys.FACEBOOK_EMAIL_ID, fbEmailId);
		editor.commit();
		
		return AsyncTaskUtil.executeAsyncTask(new GetWcitiesId(listener, LoginType.facebook), true);
	}
	
	public void removeFbUserInfo() {
		updatePreviousWcitiesId(wcitiesId);
		
		this.fbUserId = null;
		this.fbUserName = null;
		this.fbEmailId = null;
		this.wcitiesId = null;
		
		SharedPreferences pref = getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.remove(SharedPrefKeys.FACEBOOK_USER_ID);
		editor.remove(SharedPrefKeys.FACEBOOK_USER_NAME);
		editor.remove(SharedPrefKeys.FACEBOOK_EMAIL_ID);
		editor.remove(SharedPrefKeys.WCITIES_USER_ID);
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
	
	public String getGPlusUserName() {
		if (gPlusUserName == null) {
			SharedPreferences pref = getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
			gPlusUserName = pref.getString(SharedPrefKeys.GOOGLE_PLUS_USER_NAME, null);
		}
		return gPlusUserName;
	}
	
	public String getGPlusEmailId() {
		if (gPlusEmailId == null) {
			SharedPreferences pref = getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
			gPlusEmailId = pref.getString(SharedPrefKeys.GOOGLE_PLUS_EMAIL_ID, null);
		}
		return gPlusEmailId;
	}
	
	public boolean updateGPlusUserInfo(String gPlusUserId, String gPlusUserName, String gPlusEmailId, 
			AsyncTaskListener<Object> listener) {
		this.gPlusUserId = gPlusUserId;
		this.gPlusUserName = gPlusUserName;
		this.gPlusEmailId = gPlusEmailId;

		SharedPreferences pref = getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.putString(SharedPrefKeys.GOOGLE_PLUS_USER_ID, gPlusUserId);
		editor.putString(SharedPrefKeys.GOOGLE_PLUS_USER_NAME, gPlusUserName);
		editor.putString(SharedPrefKeys.GOOGLE_PLUS_EMAIL_ID, gPlusEmailId);
		editor.commit();
		
		return AsyncTaskUtil.executeAsyncTask(new GetWcitiesId(listener, LoginType.googlePlus), true);
	}
	
	public void removeGPlusUserInfo() {
		updatePreviousWcitiesId(wcitiesId);
		
		this.gPlusUserId = null;
		this.gPlusUserName = null;
		this.gPlusEmailId = null;
		this.wcitiesId = null;

		SharedPreferences pref = getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.remove(SharedPrefKeys.GOOGLE_PLUS_USER_ID);
		editor.remove(SharedPrefKeys.GOOGLE_PLUS_USER_NAME);
		editor.remove(SharedPrefKeys.GOOGLE_PLUS_EMAIL_ID);
		editor.remove(SharedPrefKeys.WCITIES_USER_ID);
		editor.commit();
	}
	
	public String getEmailId() {
		if (emailId == null) {
			SharedPreferences pref = getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
			emailId = pref.getString(SharedPrefKeys.EMAIL_ID, null);
		}
		return emailId;
	}

	public String getFirstName() {
		if (firstName == null) {
			SharedPreferences pref = getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
			firstName = pref.getString(SharedPrefKeys.FIRST_NAME, null);
		}
		return firstName;
	}

	public String getLastName() {
		if (lastName == null) {
			SharedPreferences pref = getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
			lastName = pref.getString(SharedPrefKeys.LAST_NAME, null);
		}
		return lastName;
	}

	public String getPassword() {
		return password;
	}

	public boolean updateEmailSignupInfo(String emailId, String firstName, String lastName, String password, 
			AsyncTaskListener<Object> listener) {
		this.emailId = emailId;
		this.firstName = firstName;
		this.lastName = lastName;
		this.password = password;
		
		SharedPreferences pref = getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.putString(SharedPrefKeys.EMAIL_ID, emailId);
		editor.putString(SharedPrefKeys.FIRST_NAME, firstName);
		editor.putString(SharedPrefKeys.LAST_NAME, lastName);
		editor.commit();
		
		return AsyncTaskUtil.executeAsyncTask(new GetWcitiesId(listener, LoginType.emailSignup), true);
	}
	
	public void removeEmailSignupInfo() {
		updatePreviousWcitiesId(wcitiesId);
		
		this.emailId = null;
		this.firstName = null;
		this.lastName = null;
		this.wcitiesId = null;

		SharedPreferences pref = getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.remove(SharedPrefKeys.EMAIL_ID);
		editor.remove(SharedPrefKeys.FIRST_NAME);
		editor.remove(SharedPrefKeys.LAST_NAME);
		editor.remove(SharedPrefKeys.WCITIES_USER_ID);
		editor.commit();
	}
	
	public boolean updateEmailLoginInfo(String emailId, String password, AsyncTaskListener<Object> listener) {
		this.emailId = emailId;
		this.password = password;
		
		SharedPreferences pref = getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.putString(SharedPrefKeys.EMAIL_ID, emailId);
		editor.commit();
		
		return AsyncTaskUtil.executeAsyncTask(new GetWcitiesId(listener, LoginType.emailLogin), true);
	}
	
	public void removeEmailLoginInfo() {
		updatePreviousWcitiesId(wcitiesId);
		
		this.emailId = null;
		this.wcitiesId = null;

		SharedPreferences pref = getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.remove(SharedPrefKeys.EMAIL_ID);
		editor.remove(SharedPrefKeys.WCITIES_USER_ID);
		editor.commit();
	}

	public String getWcitiesId() {
		if (wcitiesId == null) {
			SharedPreferences pref = getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
			wcitiesId = pref.getString(SharedPrefKeys.WCITIES_USER_ID, null);
		}
		return wcitiesId;
	}
	
	public void updateWcitiesId(String wcitiesId) {
		this.wcitiesId = wcitiesId;

		SharedPreferences pref = getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.putString(SharedPrefKeys.WCITIES_USER_ID, wcitiesId);
		editor.commit();

		//new GcmUtil(this).registerGCMInBackground(true);
	}
	
	public void removeWcitiesId() {
		this.wcitiesId = null;

		SharedPreferences pref = getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.remove(SharedPrefKeys.WCITIES_USER_ID);
		editor.commit();
	}
	
	public String getPreviousWcitiesId() {
		if (previousWcitiesId == null) {
			SharedPreferences pref = getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
			previousWcitiesId = pref.getString(SharedPrefKeys.PREVIOUS_WCITIES_ID, null);
		}
		return previousWcitiesId;
	}

	private void updatePreviousWcitiesId(String previousWcitiesId) {
		this.previousWcitiesId = previousWcitiesId;
		
		SharedPreferences pref = getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.putString(SharedPrefKeys.PREVIOUS_WCITIES_ID, previousWcitiesId);
		editor.commit();
	}
	
	/**
	 * This will decide the Proximity Unit for the current Locale
	 * @return
	 */
	public ProximityUnit getCurrentProximityUnit() {
		/**String countryCode = Locale.getDefault().getCountry() - This will return Default Locale NOT CURRENT ONE*/;
		String countryCode = ACTUAL_SYSTEM_LOCALE.getCountry();//getResources().getConfiguration().locale.getCountry();
		//will get current system Locale
		Log.i(TAG, "CURRENT COUNTRY CODE : " + countryCode);
		if (countryCode.equals("US") || countryCode.equals("") || countryCode.equals("LR") || countryCode.equals("MM")) {
			/**
			 * If countryCode is "", we assume default as 'US'
			 */
			return ProximityUnit.MI;
		} 
		return ProximityUnit.KM;
	}
	
	/**
	 * Will get the saved Proximity unit. If the instance is null then will get it from the SharedPref.
	 * @return
	 */
	public ProximityUnit getSavedProximityUnit() {
		if (savedProximityUnit == null) {
			SharedPreferences pref = getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
			int proximityUnitOrdinal = pref.getInt(SharedPrefKeys.PROXIMITY_UNIT, getCurrentProximityUnit().ordinal());
			savedProximityUnit = ProximityUnit.getProximityUnitByOrdinal(proximityUnitOrdinal, this);
		}
		return savedProximityUnit;
	}

	/**
	 * will update the given Proximity Unit in local instance and in SharedPref.
	 * @param savedProximityUnit
	 */
	public void updateSavedProximityUnit(ProximityUnit savedProximityUnit) {
		this.savedProximityUnit = savedProximityUnit;
		SharedPreferences pref = getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.putInt(SharedPrefKeys.PROXIMITY_UNIT, savedProximityUnit.ordinal());
		editor.commit();
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
	
	/**
	 * This needs to be done as if user has set certain 'Language' other than English in Mobile app, and if he
	 * connects the app in Bosch system then the strings used in Bosch app which are earlier defined for Mobile 
	 * app will be changed as per the Language settings in the Mobile app.
	 */
	public void updateLocaleForBosch() {
		this.defaultLocale = Locales.ENGLISH;
		//Log.d(TAG, "updateLocale()");
		setDefaultLocale();
		//Log.d(TAG, "updateLocale()");
	}
	
	public void updateLocale(Locales locale) {
		this.defaultLocale = locale;

		SharedPreferences pref = getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.putString(SharedPrefKeys.DEFAULT_LOCALE_CODE, locale.getLocaleCode());
		editor.commit();
		
		//Log.d(TAG, "updateLocale(locale)");
		setDefaultLocale();
		//Log.d(TAG, "updateLocale(locale)");
	}
	
	public void setDefaultLocale() {
		//Log.d(TAG, "getLocale().getLocaleCode() : " + getLocale().getLocaleCode());
		Locale locale = new Locale(getLocale().getLocaleCode());
		Locale.setDefault(locale);

		Configuration appConfig = new Configuration();
		appConfig.locale = locale;

		getResources().updateConfiguration(appConfig, getResources().getDisplayMetrics());
		
		/**
		 * update Locale in Api class for api-calls
		 */
		Api.updateLocaleCode(getLocale().getLocaleCode());
	}		

	public Locales getLocale() {
		if (defaultLocale == null) {
			SharedPreferences pref = getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
			String localeCode = pref.getString(SharedPrefKeys.DEFAULT_LOCALE_CODE, Locales.ENGLISH.getLocaleCode());
			defaultLocale = Locales.getLocaleByLocaleCode(localeCode);
		}
		return defaultLocale;
	}
	
	public void updateFordLocale(Locales locale) {
		this.fordDefaultLocale = locale;
		setFordDefaultLocale();
	}
	
	public void setFordDefaultLocale() {
		Locale locale = new Locale(getFordLocale().getLocaleCode(), getFordLocale().getCountryCode());
		Locale.setDefault(locale);

		Configuration appConfig = new Configuration();
		appConfig.locale = locale;

		getResources().updateConfiguration(appConfig, getResources().getDisplayMetrics());
		
		/**
		 * update Locale in Api class for api-calls
		 */
		Api.updateFordLocaleCode(getFordLocale().getLocaleCode());
	}		

	public Locales getFordLocale() {
		if (fordDefaultLocale == null) {
			fordDefaultLocale = Locales.ENGLISH_UNITED_STATES;
		}
		return fordDefaultLocale;
	}

	public int getSyncCount(Service service) {
		SharedPreferences pref = getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);

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
			
		case Spotify:
			if (syncCountSpotify == NOT_INITIALIZED) {
				syncCountSpotify = pref.getInt(
						SharedPrefKeys.SYNC_COUNT_SPOTIFY, UNSYNC_COUNT);
			}
			return syncCountSpotify;

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
			
		case Spotify:
			syncCountSpotify = count;
			editor.putInt(SharedPrefKeys.SYNC_COUNT_SPOTIFY, syncCountSpotify);
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

		for (Iterator<EventSeekrListener> iterator = listeners.iterator(); iterator.hasNext();) {
			EventSeekrListener listener = iterator.next();
			listener.onSyncCountUpdated(service);
		}
	}
	
	/**
	 * This function considers that all sync counts are already initialized.
	 * @return
	 */
	public boolean isAnyAccountSynced() {
		return ((syncCountGooglePlayMusic + syncCountDeviceLib + syncCountTwitter + syncCountSpotify 
				+ syncCountRdio + syncCountLastfm + syncCountPandora) == ALL_UNSYNCED_COUNT) ? false : true;
	}
	
	public int getTotalSyncCount() {
		int totalSyncCnt = 0;
		Service[] services = Service.values();
		for (int i = 0; i < services.length; i++) {
			if (services[i].isService()) {
				int cnt = getSyncCount(services[i]);
				if (cnt != UNSYNC_COUNT) {
					totalSyncCnt += cnt;
				}
			}
		}
		return totalSyncCnt;
	}
	
	public Event getEventToAddToCalendar() {
		return eventToAddToCalendar;
	}

	public void setEventToAddToCalendar(Event eventToAddToCalendar) {
		this.eventToAddToCalendar = eventToAddToCalendar;
	}
	
	public int getUniqueGcmNotificationId() {
		return ++uniqueGcmNotificationId;
	}
	
	public double getCurLat() {
		return curLat;
	}

	public void setCurLat(double curLat) {
		this.curLat = curLat;
	}

	public double getCurLon() {
		return curLon;
	}

	public void setCurLon(double curLon) {
		this.curLon = curLon;
	}

	/**
	 * THIS IS THE STARTING POINT FOR THE PROXIMITY RELATED UPDATES.
	 * Whenever user will change the Locale(language from Device's Settings screen), this app will be notified
	 * by 'onConfigurationChanged()' call back. So, now app must change its Proximity unit on the basis of the
	 * new Locale selected.
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		ACTUAL_SYSTEM_LOCALE = newConfig.locale;
	}
	
	/**
	 * This is called from BoschMainActivity's onStop(). Reason :- From BoscgMainActivity's onResume() app
	 * makes a call for 'updateLocaleForBosch()' which will change the app's Locale to English for Bosch 
	 * System. So, this method will revert this change by making the defaultLocale as null. Now, when 
	 * BoschMainActivity will resume the MainActivity, then from MainActivity's onResume() a call to 
	 * 'setDefaultLocale()' is made which will inturn make a call to 'getLocale()' and this method now 
	 * will refresh the defaultLocale from the SharedPref value.
	 */
	public void resetDefaultLocale() {
		defaultLocale = null;
	}

	public void onActivityDestroyed() {
		if (activityDestroyedListener != null) {
			/**
			 * We call onOtherActivityDestroyed() on activityDestroyedListener because without this sometimes
			 * back arrow is set instead of drawer indicator due to more activities found in back stack. This 
			 * happens because onStart() of new activity executes first where we set drawer indicator/back arrow
			 * based on isTaskRoot() call and onDestroy() of previous activity gets called up in the end after 
			 * which it's possible that isTaskRoot() call for new activity might return true if that is the only 
			 * activity in stack. In this case we should update drawer indicator icon from back arrow to hamburger.
			 * 
			 * We use handler here to post in the message queue so that onDestroy() which has called this 
			 * function completes first & then only we check for isTaskRoot() from onOtherActivityDestroyed().
			 */
			handler.post(new Runnable() {
				
				@Override
				public void run() {
					activityDestroyedListener.onOtherActivityDestroyed();
				}
			});
		}
	}

	public void setActivityDestroyedListener(ActivityDestroyedListener activityDestroyedListener) {
		this.activityDestroyedListener = activityDestroyedListener;
	}

	private class GetWcitiesId extends AsyncTask<Void, Void, Integer> {
		
		private AsyncTaskListener<Object> listener;
		private LoginType loginType;
		
		public GetWcitiesId(AsyncTaskListener<Object> listener, LoginType loginType) {
			this.listener = listener;
			this.loginType = loginType;
		}

		@Override
		protected Integer doInBackground(Void... params) {
			try {
				Registration registration = loginType.getRegistrationInstance(EventSeekr.this);
				return registration.perform();
				
			} catch (ClientProtocolException e) {
				e.printStackTrace();

			} catch (IOException e) {
				e.printStackTrace();

			} catch (JSONException e) {
				e.printStackTrace();
			} 
			return UserInfoApiJSONParser.MSG_CODE_UNSUCCESS;
		}
		
		@Override
		protected void onPostExecute(Integer msgCode) {
			if (msgCode.intValue() != UserInfoApiJSONParser.MSG_CODE_SUCCESS) {
				removeWcitiesId();
			}

			if (listener != null) {
				listener.onTaskCompleted(msgCode);
			}
			/*if (response != null) {
				if (response instanceof String) {
					
					updateWcitiesId((String) response);
					
					if (listener != null) {
						listener.onTaskCompleted();
					}
					
				} else if (response instanceof Integer) {
					
				}
			}*/
		}
	}
}
