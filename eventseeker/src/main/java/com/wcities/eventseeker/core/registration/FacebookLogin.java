package com.wcities.eventseeker.core.registration;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi;
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
	public int perform() throws ClientProtocolException, IOException, JSONException {
		JSONObject jsonObject;
		UserInfoApi userInfoApi = new UserInfoApi(Api.OAUTH_TOKEN);
		UserInfoApiJSONParser jsonParser = new UserInfoApiJSONParser();
		
		String userId = eventSeekr.getPreviousWcitiesId();
		
		if (userId == null) {
			// signup with deviceId
			userInfoApi.setDeviceId(DeviceUtil.getDeviceId(eventSeekr));
			jsonObject = userInfoApi.signUp();
			userId = jsonParser.getUserId(jsonObject);
		}
		Log.d(TAG, "userId = " + userId);
		
		// sync fb with previous userId or userId generated above based on deviceId
		jsonObject = userInfoApi.syncAccount(null, eventSeekr.getFbUserId(), eventSeekr.getFbEmailId(), 
				UserType.fb, userId);
		Log.d(TAG, jsonObject.toString());
		SyncAccountResponse syncAccountResponse = jsonParser.parseSyncAccount(jsonObject);
		userId = syncAccountResponse.getWcitiesId();
		Log.d(TAG, "userId = " + userId);
		
		// sync friends
		jsonObject = userInfoApi.syncFriends(UserType.fb, eventSeekr.getFbUserId(), null);
		
		/**
		 * It is possible that first syncaccount call has returned false & some other fb id used
		 * earlier from same device. In that case syncFriends call creates new account. We can get 
		 * new wcitiesId corresponding to this new account by again calling syncaccount
		 */
		if (syncAccountResponse.getFbGoogleId() != null && !syncAccountResponse.getFbGoogleId().equals(eventSeekr.getFbUserId())) {
			// sync fb again with userId
			jsonObject = userInfoApi.syncAccount(null, eventSeekr.getFbUserId(), eventSeekr.getFbEmailId(), 
					UserType.fb, userId);
			Log.d(TAG, jsonObject.toString());
			syncAccountResponse = jsonParser.parseSyncAccount(jsonObject);
			userId = syncAccountResponse.getWcitiesId();
		}
		
		eventSeekr.updateWcitiesId(userId);

		// register device for notification
		new GcmUtil(eventSeekr).registerGCM();
		
		return UserInfoApiJSONParser.MSG_CODE_SUCCESS;
	}
}
