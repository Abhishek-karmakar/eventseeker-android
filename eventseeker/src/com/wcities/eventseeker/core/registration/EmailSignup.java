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
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser.SignupResponse;

public class EmailSignup extends Registration {

	private static final String TAG = EmailSignup.class.getSimpleName();

	public EmailSignup(EventSeekr eventSeekr) {
		super(eventSeekr);
	}

	@Override
	public int register() throws ClientProtocolException, IOException, JSONException {
		// check if it's new user
		UserInfoApi userInfoApi = new UserInfoApi(Api.OAUTH_TOKEN);
		JSONObject jsonObject = userInfoApi.checkEmail(eventSeekr.getEmailId());
		Log.d(TAG, jsonObject.toString());
		UserInfoApiJSONParser jsonParser = new UserInfoApiJSONParser();
		SignupResponse signupResponse = jsonParser.parseSignup(jsonObject);
		if (signupResponse.getMsgCode() == UserInfoApiJSONParser.MSG_CODE_EMAIL_ALREADY_EXISTS) {
			return signupResponse.getMsgCode();
		}
		
		/**
		 * Signup w/o deviceId
		 * Don't set deviceId on userInfoApi, because otherwise if same device is used earlier
		 * to sign up using some other email, server will replace old email & pwd by new ones.
		 * Whereas we want to create new profile in such case so that all emails are saved at server side &
		 * news letters can be sent out to all these email ids.
		 */
		jsonObject = userInfoApi.signUp();
		Log.d(TAG, jsonObject.toString());
		String userId = jsonParser.getUserId(jsonObject);
		//Log.d(TAG, "userId = " + userId);
		
		// sign up with email & pwd
		jsonObject = userInfoApi.signup(eventSeekr.getEmailId(), eventSeekr.getPassword(), eventSeekr.getFirstName(), 
				eventSeekr.getLastName(), userId);
		Log.d(TAG, jsonObject.toString());
		signupResponse = jsonParser.parseSignup(jsonObject);
		
		if (signupResponse.getMsgCode() == UserInfoApiJSONParser.MSG_CODE_SUCCESS) {
			// sync last used fb/g+ account
			LoginType prevLoginType = eventSeekr.getPreviousLoginType();
			if (prevLoginType == LoginType.facebook || prevLoginType == LoginType.googlePlus) {
				jsonObject = userInfoApi.syncAccount(null, userId, eventSeekr.getEmailId(), 
						UserType.wcities, eventSeekr.getPreviousWcitiesId());
				Log.d(TAG, "jsonObject = " + jsonObject.toString());
				userId = jsonParser.getWcitiesId(jsonObject);
				//Log.d(TAG, "userId = " + userId);
			}
			eventSeekr.updateWcitiesId(userId);
			
			// register device for notification
			new GcmUtil(eventSeekr).registerGCM();

			return UserInfoApiJSONParser.MSG_CODE_SUCCESS;
		}
		return UserInfoApiJSONParser.MSG_CODE_UNSUCCESS;
	}
}
