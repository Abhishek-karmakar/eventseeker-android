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

public class EmailLogin extends Registration {

	private static final String TAG = EmailLogin.class.getSimpleName();

	public EmailLogin(EventSeekr eventSeekr) {
		super(eventSeekr);
	}

	@Override
	public int register() throws ClientProtocolException, IOException, JSONException {
		// login with email & pwd
		UserInfoApi userInfoApi = new UserInfoApi(Api.OAUTH_TOKEN);
		JSONObject jsonObject = userInfoApi.login(eventSeekr.getEmailId(), eventSeekr.getPassword());
		Log.d(TAG, "login response = " + jsonObject.toString());
		UserInfoApiJSONParser jsonParser = new UserInfoApiJSONParser();
		SignupResponse signupResponse = jsonParser.parseSignup(jsonObject);
		
		if (signupResponse.getMsgCode() == UserInfoApiJSONParser.MSG_CODE_EMAIL_OR_PWD_INCORRECT) {
			return signupResponse.getMsgCode();
			
		} else if (signupResponse.getMsgCode() == UserInfoApiJSONParser.MSG_CODE_SUCCESS) {
			String userId = signupResponse.getWcitiesId();
			
			// sync last used account
			if (eventSeekr.getPreviousWcitiesId() != null) {
				jsonObject = userInfoApi.syncAccount(null, userId, eventSeekr.getEmailId(), 
						UserType.wcities, eventSeekr.getPreviousWcitiesId());
				Log.d(TAG, "syncAccount response = " + jsonObject.toString());
				userId = jsonParser.getWcitiesId(jsonObject);
			}
			eventSeekr.updateWcitiesId(userId);
			
			// register device for notification
			new GcmUtil(eventSeekr).registerGCM();

			return UserInfoApiJSONParser.MSG_CODE_SUCCESS;
		}
		return UserInfoApiJSONParser.MSG_CODE_UNSUCCESS;
	}
}
