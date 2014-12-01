package com.wcities.eventseeker.core.registration;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.api.UserInfoApi.LoginType;
import com.wcities.eventseeker.api.UserInfoApi.UserType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.gcm.GcmUtil;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser.SyncAccountResponse;
import com.wcities.eventseeker.util.DeviceUtil;

public class FacebookLogin extends Registration {
	
	private static final String TAG = FacebookLogin.class.getSimpleName();

	public FacebookLogin(EventSeekr eventSeekr) {
		super(eventSeekr);
	}

	@Override
	public int register() throws ClientProtocolException, IOException, JSONException {
		// signup with deviceId
		UserInfoApi userInfoApi = new UserInfoApi(Api.OAUTH_TOKEN);
		userInfoApi.setDeviceId(DeviceUtil.getDeviceId(eventSeekr));
		JSONObject jsonObject = userInfoApi.signUp();
		UserInfoApiJSONParser jsonParser = new UserInfoApiJSONParser();
		String userId = jsonParser.getUserId(jsonObject);
		Log.d(TAG, "userId = " + userId);
		
		// sync fb with device
		jsonObject = userInfoApi.syncAccount(null, eventSeekr.getFbUserId(), eventSeekr.getFbEmailId(), 
				UserType.fb, userId);
		Log.d(TAG, jsonObject.toString());
		SyncAccountResponse syncAccountResponse = jsonParser.parseSyncAccount(jsonObject);
		userId = syncAccountResponse.getWcitiesId();
		//Log.d(TAG, "userId = " + userId);
		
		// sync friends
		jsonObject = userInfoApi.syncFriends(UserType.fb, eventSeekr.getFbUserId(), null);
		
		// sync last used email account wcitiesId with this fb
		LoginType prevLoginType = eventSeekr.getPreviousLoginType();
		//Log.d(TAG, "prevLoginType = " + prevLoginType);
		/**
		 * 3rd condition below is to get new wcitiesId in following case:
		 * If user's one profile created from this device already has 1 fb account, then 1st syncaccount call will 
		 * return sync false with fb user id & wcitiesId associated with old profile, but after this sync friends
		 * call will create new account & hence wcitiesId returned in next syncaccount call executed below will be 
		 * of this new fb account.
		 */
		if (prevLoginType == LoginType.emailSignup || prevLoginType == LoginType.emailLogin || 
				(prevLoginType != null && !syncAccountResponse.isSync())) {
			jsonObject = userInfoApi.syncAccount(null, eventSeekr.getFbUserId(), eventSeekr.getFbEmailId(), 
					UserType.fb, eventSeekr.getPreviousWcitiesId());
			Log.d(TAG, jsonObject.toString());
			userId = jsonParser.getWcitiesId(jsonObject);
			//Log.d(TAG, "userId = " + userId);
		}
		eventSeekr.updateWcitiesId(userId);

		// register device for notification
		new GcmUtil(eventSeekr).registerGCM();
		
		return UserInfoApiJSONParser.MSG_CODE_SUCCESS;
	}
}
