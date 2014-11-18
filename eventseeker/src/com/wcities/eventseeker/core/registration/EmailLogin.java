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

public class EmailLogin extends Registration {

	public EmailLogin(EventSeekr eventSeekr) {
		super(eventSeekr);
	}

	@Override
	public int register() throws ClientProtocolException, IOException, JSONException {
		// login with email & pwd
		UserInfoApi userInfoApi = new UserInfoApi(Api.OAUTH_TOKEN);
		JSONObject jsonObject = userInfoApi.login(eventSeekr.getEmailId(), eventSeekr.getPassword());
		UserInfoApiJSONParser jsonParser = new UserInfoApiJSONParser();
		SignupResponse signupResponse = jsonParser.parseSignup(jsonObject);
		
		if (signupResponse.getMsgCode() == UserInfoApiJSONParser.MSG_CODE_EMAIL_OR_PWD_INCORRECT) {
			return signupResponse.getMsgCode();
			
		} else if (signupResponse.getMsgCode() == UserInfoApiJSONParser.MSG_CODE_SUCCESS) {
			eventSeekr.updateWcitiesId(signupResponse.getWcitiesId());
			
			// register device for notification
			new GcmUtil(eventSeekr).registerGCM();
			
			// sync last used fb/g+ account
			LoginType prevLoginType = eventSeekr.getPreviousLoginType();
			if (prevLoginType == LoginType.facebook || prevLoginType == LoginType.googlePlus) {
				jsonObject = userInfoApi.syncAccount(null, eventSeekr.getPreviousUserId(), eventSeekr.getPreviousEmailId(), 
						UserType.getUserType(prevLoginType), eventSeekr.getWcitiesId());
				String userId = jsonParser.getWcitiesId(jsonObject);
				eventSeekr.updateWcitiesId(userId);
			}
			return UserInfoApiJSONParser.MSG_CODE_SUCCESS;
		}
		return UserInfoApiJSONParser.MSG_CODE_UNSUCCESS;
	}
}
