package com.wcities.eventseeker.core.registration;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser.SignupResponse;

public class ForgotPassword extends Registration {
	
	private static final String TAG = ForgotPassword.class.getSimpleName();
	
	private String email;

	public ForgotPassword(EventSeekr eventSeekr, String email) {
		super(eventSeekr);
		this.email = email;
	}

	@Override
	public int perform() throws ClientProtocolException, IOException, JSONException {
		UserInfoApi userInfoApi = new UserInfoApi(Api.OAUTH_TOKEN);
		JSONObject jsonObject = userInfoApi.forgotPassword(email);
		//Log.d(TAG, jsonObject.toString());
		UserInfoApiJSONParser jsonParser = new UserInfoApiJSONParser();
		SignupResponse signupResponse = jsonParser.parseSignup(jsonObject);
		if (signupResponse.getMsgCode() == UserInfoApiJSONParser.MSG_CODE_CHK_EMAIL_TO_RESET_PWD 
				|| signupResponse.getMsgCode() == UserInfoApiJSONParser.MSG_CODE_USER_EMAIL_DOESNT_EXIST) {
			return signupResponse.getMsgCode();
		}
		return UserInfoApiJSONParser.MSG_CODE_UNSUCCESS;
	}
}
