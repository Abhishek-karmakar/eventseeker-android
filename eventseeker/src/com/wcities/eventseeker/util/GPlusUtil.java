package com.wcities.eventseeker.util;

import java.io.IOException;
import java.text.SimpleDateFormat;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.PlusShare;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.SharedPrefKeys;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Event.Attending;
import com.wcities.eventseeker.core.FriendNewsItem;

public class GPlusUtil {

	private static final String TAG = GPlusUtil.class.getSimpleName();
	
	/**
	 * This flag is required to direct onActivityResult() calls from MainActivity & handle it at right 
	 * place, because google plus share intent doesn't return right request code in onActivityResult() 
	 * method.
	 */
	public static boolean isGPlusPublishPending;

	public static boolean hasUserLoggedInBefore(Context context) {
		//Log.d(TAG, "hasUserLoggedInBefore()");
		SharedPreferences pref = context.getSharedPreferences(
                AppConstants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
		return pref.contains(SharedPrefKeys.GOOGLE_PLUS_USER_ID);
	}
	
	public static GoogleApiClient createPlusClientInstance(Fragment fragment, ConnectionCallbacks 
			connectionCallbacks, OnConnectionFailedListener onConnectionFailedListener) {
		/*return new PlusClient.Builder(FragmentUtil.getActivity(fragment), connectionCallbacks, onConnectionFailedListener)
	    	.setActions(AppConstants.GOOGLE_PLUS_ACTION)
	    	.setScopes(AppConstants.GOOGLE_PLUS_SCOPES)  // PLUS_LOGIN is recommended login scope for social features
	    	// .setScopes("profile")       // alternative basic login scope
	    	.build();*/
		return new GoogleApiClient.Builder(FragmentUtil.getActivity(fragment), 
				connectionCallbacks, onConnectionFailedListener)
			.addApi(Plus.API)
			.addScope(new Scope(Scopes.PLUS_LOGIN))  // PLUS_LOGIN is recommended login scope for social features)
			.addScope(new Scope(Scopes.PLUS_ME))
			.addScope(new Scope(AppConstants.SCOPE_URI_USERINFO_EMAIL))
			.addScope(new Scope(AppConstants.SCOPE_URI_USERINFO_PROFILE))
			.addScope(new Scope(AppConstants.SCOPE_URI_PLUS_PROFILE_EMAILS_READ))
			.build();
	}
	
	public static void callGPlusLogout(GoogleApiClient googleApiClient, EventSeekr eventSeekr) {
		if (googleApiClient != null && googleApiClient.isConnected()) {
			Plus.AccountApi.clearDefaultAccount(googleApiClient);
			googleApiClient.disconnect();
			//plusClient.connect();
		}
		eventSeekr.removeGPlusUserInfo();
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
	
	public static void publishEvent(Event event, Fragment fragment) {
		//Log.d(TAG, "publishEvent()");
		isGPlusPublishPending = true;
		String userName = ((EventSeekr) FragmentUtil.getActivity(fragment).getApplication()).getGPlusUserName();
        if (userName == null) {
        	return;
        }
		String text = userName + " is going to ";
		/**
		 * Using getNewAttending() instead of getAttending() since we update right value on event only 
		 * after successfully sharing on google+
		 */
        if (event.getNewAttending() == Attending.WANTS_TO_GO) {
        	text = userName + " wants to go to ";
        }
        text += event.getName();
        if (event.getSchedule() != null) {
        	if (event.getSchedule().getVenue().getAddress() != null) {
        		text += " at " + event.getSchedule().getVenue().getAddress().getCity();
        	}
        	if (event.getSchedule().getDates().size() > 0) {
        		text += " on " + new SimpleDateFormat("EEEE, MMM d").format(event.getSchedule().getDates().get(0).getStartDate());
        	}
        }
        text += ".";
        
        String link = event.getEventUrl();
        if (link == null) {
        	link = "http://eventseeker.com/event/" + event.getId();
	    }
        
		// Launch the Google+ share dialog with attribution to your app.
		Intent shareIntent = new PlusShare.Builder(FragmentUtil.getActivity(fragment))
          	.setType("text/plain")
          	.setText(text)
          	.setContentUrl(Uri.parse(link))
          	.getIntent();
        
        /**
         * 16-12-2014:
         * Above code is commented and used below one because it was crashing after GooglePlayLib & support libs update.
           Exception:
           12-16 11:28:27.632: E/AndroidRuntime(10358): FATAL EXCEPTION: main
		   12-16 11:28:27.632: E/AndroidRuntime(10358): Process: com.google.android.gms.ui, PID: 10358
	       12-16 11:28:27.632: E/AndroidRuntime(10358): java.lang.IllegalArgumentException
		   12-16 11:28:27.632: E/AndroidRuntime(10358): 	at com.google.k.a.aj.a(SourceFile:72)
		   12-16 11:28:27.632: E/AndroidRuntime(10358): 	at com.google.android.gms.plus.audience.a.e.<init>(SourceFile:63)
		   12-16 11:28:27.632: E/AndroidRuntime(10358): 	at com.google.android.gms.plus.audience.a.e.<init>(SourceFile:53)
		   12-16 11:28:27.632: E/AndroidRuntime(10358): 	at com.google.android.gms.plus.audience.a.d.<init>(SourceFile:28)
		   12-16 11:28:27.632: E/AndroidRuntime(10358): 	at com.google.android.gms.plus.sharebox.al.a(SourceFile:213)
         */
        
        /*String key = event.getKey(ImgResolution.LOW);
        BitmapCache bitmapCache = BitmapCache.getInstance();
		Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
		Uri uri = null;
		if (bitmap != null) {
			File tmpFile = FileUtil.createTempShareImgFile(FragmentUtil.getActivity(fragment).getApplication(), bitmap);
			if (tmpFile != null) {
				uri = Uri.fromFile(tmpFile);
			}
		}

		IntentBuilder shareIntentbuBuilder = ShareCompat.IntentBuilder.from(FragmentUtil.getActivity(fragment));
		if (uri != null) {
			shareIntentbuBuilder.setStream(uri).setType("image/jpg");

		} else {
			shareIntentbuBuilder.setType("text/plain");	
		}
        Intent shareIntent = shareIntentbuBuilder
                .setText(text + " " + link)
                .getIntent()
                .setPackage("com.google.android.apps.plus");*/

		fragment.startActivityForResult(shareIntent, AppConstants.REQ_CODE_GOOGLE_PLUS_PUBLISH_EVT);
	}
	
	public static void publishFriendNewsItem(FriendNewsItem item, Fragment fragment) {
		//Log.d(TAG, "publishFriendNewsItem()");
		isGPlusPublishPending = true;
		String userName = ((EventSeekr) FragmentUtil.getActivity(fragment).getApplication()).getGPlusUserName();
        if (userName == null) {
        	return;
        }
		String text = userName + " is going to ";
		/**
		 * Using getNewAttending() instead of getAttending() since we update right value on event only 
		 * after successfully sharing on google+
		 */
        if (item.getNewUserAttending() == Attending.WANTS_TO_GO) {
        	text = userName + " wants to go to ";
        }
        text += item.getTrackName();
        
        if (item.getVenueName() != null) {
        	text += " at " + item.getVenueName();
    	}
    	if (item.getStartTime() != null) {
    		text += " on " + new SimpleDateFormat("EEEE, MMM d").format(item.getStartTime().getStartDate());
    	}
        text += ".";
        
        String link = "http://eventseeker.com/event/" + item.getTrackId();
        
		// Launch the Google+ share dialog with attribution to your app.
		Intent shareIntent = new PlusShare.Builder(FragmentUtil.getActivity(fragment))
          	.setType("text/plain")
          	.setText(text)
          	.setContentUrl(Uri.parse(link))
          	.getIntent();

        /**
         * 16-12-2014:
         * Above code is commented and used below one because it was crashing after GooglePlayLib & support libs update.
           Exception:
           12-16 11:28:27.632: E/AndroidRuntime(10358): FATAL EXCEPTION: main
		   12-16 11:28:27.632: E/AndroidRuntime(10358): Process: com.google.android.gms.ui, PID: 10358
	       12-16 11:28:27.632: E/AndroidRuntime(10358): java.lang.IllegalArgumentException
		   12-16 11:28:27.632: E/AndroidRuntime(10358): 	at com.google.k.a.aj.a(SourceFile:72)
		   12-16 11:28:27.632: E/AndroidRuntime(10358): 	at com.google.android.gms.plus.audience.a.e.<init>(SourceFile:63)
		   12-16 11:28:27.632: E/AndroidRuntime(10358): 	at com.google.android.gms.plus.audience.a.e.<init>(SourceFile:53)
		   12-16 11:28:27.632: E/AndroidRuntime(10358): 	at com.google.android.gms.plus.audience.a.d.<init>(SourceFile:28)
		   12-16 11:28:27.632: E/AndroidRuntime(10358): 	at com.google.android.gms.plus.sharebox.al.a(SourceFile:213)
         */
        
        /*String key = event.getKey(ImgResolution.LOW);
        BitmapCache bitmapCache = BitmapCache.getInstance();
		Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
		Uri uri = null;
		if (bitmap != null) {
			File tmpFile = FileUtil.createTempShareImgFile(FragmentUtil.getActivity(fragment).getApplication(), bitmap);
			if (tmpFile != null) {
				uri = Uri.fromFile(tmpFile);
			}
		}

		IntentBuilder shareIntentbuBuilder = ShareCompat.IntentBuilder.from(FragmentUtil.getActivity(fragment));
		if (uri != null) {
			shareIntentbuBuilder.setStream(uri).setType("image/jpg");

		} else {
			shareIntentbuBuilder.setType("text/plain");	
		}
        Intent shareIntent = shareIntentbuBuilder
                .setText(text + " " + link)
                .getIntent()
                .setPackage("com.google.android.apps.plus");*/
        
		fragment.startActivityForResult(shareIntent, AppConstants.REQ_CODE_GOOGLE_PLUS_PUBLISH_EVT);
	}
	
	public static String getAccessToken(EventSeekr eventSeekr, String accountName) {
		String scopes = "oauth2:" + AppConstants.GOOGLE_PLUS_SCOPES_FOR_SERVER_ACCESS;
		String code = null;
		try {
			code = GoogleAuthUtil.getToken(eventSeekr, accountName, scopes, null);

		} catch (IOException transientEx) {
			Log.e(TAG, "IOException");
			// network or server error, the call is expected to succeed if you try again later.
			// Don't attempt to call again immediately - the request is likely to
			// fail, you'll hit quotas or back-off.
			return null;
		  
		} catch (UserRecoverableAuthException e) {
			Log.e(TAG, "UserRecoverableAuthException");
			// Requesting an authorization code will always throw
			// UserRecoverableAuthException on the first call to GoogleAuthUtil.getToken
			// because the user must consent to offline access to their data.  After
			// consent is granted control is returned to your activity in onActivityResult
			// and the second call to GoogleAuthUtil.getToken will succeed.
			
			/**
			 * This should not occur because we ask for this permission first time itself while 
			 * logging in with google.
			 */
			//fragment.startActivityForResult(e.getIntent(), AppConstants.REQ_CODE_GOOGLE_AUTH_CODE_FOR_SERVER_ACCESS);
			return null;
		  
		} catch (GoogleAuthException authEx) {
			Log.e(TAG, "GoogleAuthException");
			// Failure. The call is not expected to ever succeed so it should not be
			// retried.
			return null;
		  
		} catch (Exception e) {
			Log.d(TAG, "Exception");
			throw new RuntimeException(e);
		}
		return code;
	}
}
