package com.wcities.eventseeker.core.registration;

import android.util.Log;

import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.api.UserInfoApi.UserType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.gcm.GcmUtil;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser.SignupResponse;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class EmailSignup extends Registration {

	private static final String TAG = EmailSignup.class.getSimpleName();

	public EmailSignup(EventSeekr eventSeekr) {
		super(eventSeekr);
	}

	@Override
	public int perform() throws ClientProtocolException, IOException, JSONException {
		// Signup w/o deviceId
		UserInfoApi userInfoApi = new UserInfoApi(Api.OAUTH_TOKEN);
		JSONObject jsonObject = userInfoApi.signUp();
		//Log.d(TAG, jsonObject.toString());
		UserInfoApiJSONParser jsonParser = new UserInfoApiJSONParser();
		String userId = jsonParser.getUserId(jsonObject);
		Log.d(TAG, "userId = " + userId);
		
		// sign up with email & pwd
		jsonObject = userInfoApi.signup(eventSeekr.getEmailId(), eventSeekr.getPassword(), eventSeekr.getFirstName(), 
				eventSeekr.getLastName(), userId);
		Log.d(TAG, jsonObject.toString());
		SignupResponse signupResponse = jsonParser.parseSignup(jsonObject);
		
		if (signupResponse.getMsgCode() == UserInfoApiJSONParser.MSG_CODE_SUCCESS) {
			// sync last used account
			if (eventSeekr.getPreviousWcitiesId() != null) {
				jsonObject = userInfoApi.syncAccount(null, userId, eventSeekr.getEmailId(), 
						UserType.wcities, eventSeekr.getPreviousWcitiesId());
				Log.d(TAG, "jsonObject = " + jsonObject.toString());
				userId = jsonParser.getWcitiesId(jsonObject);
				Log.d(TAG, "userId = " + userId);
			}
			eventSeekr.updateWcitiesId(userId);
			
			// register device for notification
			new GcmUtil(eventSeekr).registerGCM();

			return UserInfoApiJSONParser.MSG_CODE_SUCCESS;
			
		} else if (signupResponse.getMsgCode() == UserInfoApiJSONParser.MSG_CODE_EMAIL_ALREADY_EXISTS) {
			return signupResponse.getMsgCode();
		}
		return UserInfoApiJSONParser.MSG_CODE_UNSUCCESS;
	}
}
