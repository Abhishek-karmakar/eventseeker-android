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
import com.wcities.eventseeker.util.DeviceUtil;

public class FacebookLogin extends Registration {

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
		
		// sync fb with device
		jsonObject = userInfoApi.syncAccount(null, eventSeekr.getFbUserId(), eventSeekr.getFbEmailId(), 
				UserType.fb, userId);
		userId = jsonParser.getWcitiesId(jsonObject);
		
		// sync friends
		jsonObject = userInfoApi.syncFriends(LoginType.facebook, null);
		
		// sync last used email account wcitiesId with this fb
		LoginType prevLoginType = eventSeekr.getPreviousLoginType();
		if (prevLoginType != null && prevLoginType != LoginType.facebook) {
			jsonObject = userInfoApi.syncAccount(null, eventSeekr.getFbUserId(), eventSeekr.getFbEmailId(), 
					UserType.fb, eventSeekr.getPreviousWcitiesId());
			userId = jsonParser.getWcitiesId(jsonObject);
		}
		eventSeekr.updateWcitiesId(userId);

		// register device for notification
		new GcmUtil(eventSeekr).registerGCM();
		
		return UserInfoApiJSONParser.MSG_CODE_SUCCESS;
	}
}
