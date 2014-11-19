package com.wcities.eventseeker.core.registration;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.api.UserInfoApi.LoginType;
import com.wcities.eventseeker.api.UserInfoApi.UserType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.gcm.GcmUtil;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser.SignupResponse;

public class EmailSignup extends Registration {

	public EmailSignup(EventSeekr eventSeekr) {
		super(eventSeekr);
	}

	@Override
	public int register() throws ClientProtocolException, IOException, JSONException {
		// check if it's new user
		UserInfoApi userInfoApi = new UserInfoApi(Api.OAUTH_TOKEN);
		JSONObject jsonObject = userInfoApi.checkEmail(eventSeekr.getEmailId());
		UserInfoApiJSONParser jsonParser = new UserInfoApiJSONParser();
		SignupResponse signupResponse = jsonParser.parseSignup(jsonObject);
		if (signupResponse.getMsgCode() == UserInfoApiJSONParser.MSG_CODE_EMAIL_ALREADY_EXISTS) {
			return signupResponse.getMsgCode();
		}
		
		/**
		 * Don't set deviceId on userInfoApi, because otherwise if same device is used earlier
		 * to sign up using some other email, server will replace old email & pwd by new ones.
		 * Whereas we want to create new profile in such case.
		 */
		jsonObject = userInfoApi.signUp();
		String userId = jsonParser.getUserId(jsonObject);
		eventSeekr.updateWcitiesId(userId);
		
		// sign up with email & pwd
		jsonObject = userInfoApi.signup(eventSeekr.getEmailId(), eventSeekr.getPassword(), eventSeekr.getFirstName(), 
				eventSeekr.getLastName(), userId);
		signupResponse = jsonParser.parseSignup(jsonObject);
		
		if (signupResponse.getMsgCode() == UserInfoApiJSONParser.MSG_CODE_SUCCESS) {
			// register device for notification
			new GcmUtil(eventSeekr).registerGCM();
			
			// sync last used fb/g+ account
			LoginType prevLoginType = eventSeekr.getPreviousLoginType();
			if (prevLoginType == LoginType.facebook || prevLoginType == LoginType.googlePlus) {
				jsonObject = userInfoApi.syncAccount(null, eventSeekr.getPreviousUserId(), eventSeekr.getPreviousEmailId(), 
						UserType.getUserType(prevLoginType), eventSeekr.getWcitiesId());
				userId = jsonParser.getWcitiesId(jsonObject);
				eventSeekr.updateWcitiesId(userId);
			}
			return UserInfoApiJSONParser.MSG_CODE_SUCCESS;
		}
		return UserInfoApiJSONParser.MSG_CODE_UNSUCCESS;
	}
}
