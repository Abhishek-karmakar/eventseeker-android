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
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.GPlusUtil;

public class GooglePlusLogin extends Registration {

	private static final String TAG = GooglePlusLogin.class.getSimpleName();

	public GooglePlusLogin(EventSeekr eventSeekr) {
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
		
		// sync g+ with device
		jsonObject = userInfoApi.syncAccount(null, eventSeekr.getGPlusUserId(), eventSeekr.getGPlusEmailId(), 
				UserType.google, userId);
		Log.d(TAG, jsonObject.toString());
		userId = jsonParser.getWcitiesId(jsonObject);
		Log.d(TAG, "userId = " + userId);
		
		// sync friends
		String accessToken = GPlusUtil.getAccessToken(eventSeekr, eventSeekr.getGPlusEmailId());
		//Log.d(TAG, "accessToken = " + accessToken);
		if (accessToken == null) {
			return UserInfoApiJSONParser.MSG_CODE_NO_ACCESS_TOKEN;
			
		} else {
			userInfoApi.syncFriends(UserType.getUserType(LoginType.googlePlus), eventSeekr.getGPlusUserId(), accessToken);
		}
		
		// sync last used email account wcitiesId with this fb
		LoginType prevLoginType = eventSeekr.getPreviousLoginType();
		if (prevLoginType == LoginType.emailLogin && prevLoginType == LoginType.emailSignup) {
			jsonObject = userInfoApi.syncAccount(null, eventSeekr.getGPlusUserId(), eventSeekr.getGPlusEmailId(), 
					UserType.google, eventSeekr.getPreviousWcitiesId());
			Log.d(TAG, jsonObject.toString());
			userId = jsonParser.getWcitiesId(jsonObject);
			Log.d(TAG, "userId = " + userId);
		}
		eventSeekr.updateWcitiesId(userId);
		
		// register device for notification
		new GcmUtil(eventSeekr).registerGCM();

		return UserInfoApiJSONParser.MSG_CODE_SUCCESS;
	}
}