package com.wcities.eventseeker.asynctask;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;

import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingItemType;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingType;
import com.wcities.eventseeker.app.EventSeekr;

public class UserTracker extends AsyncTask<Void, Void, Void> {
	
	private static final String TAG = UserTracker.class.getName();
	
	private UserTrackingType trackingType;
	private EventSeekr eventSeekr;
	private UserTrackingItemType type;
	private long id;
	private int attending;
	private String fb_postid;
	
	public UserTracker(EventSeekr eventSeekr, UserTrackingItemType type, long id) {
		this.eventSeekr = eventSeekr;
		this.type = type;
		this.id = id;
		trackingType = UserTrackingType.Add;
	}
	
	public UserTracker(EventSeekr eventSeekr, UserTrackingItemType type, long id, int attending, UserTrackingType trackingType) {
		this(eventSeekr, type, id);
		this.attending = attending;
		this.trackingType = trackingType;
	}
	
	public UserTracker(EventSeekr eventSeekr, UserTrackingItemType type, long id, int attending, String fb_postid, 
			UserTrackingType trackingType) {
		this(eventSeekr, type, id);
		this.attending = attending;
		this.fb_postid = fb_postid;
		this.trackingType = trackingType;
	}

	@Override
	protected Void doInBackground(Void... params) {
		UserInfoApi userInfoApi = new UserInfoApi(Api.OAUTH_TOKEN);
		userInfoApi.setUserId(eventSeekr.getWcitiesId());
		try {
			JSONObject jsonObject = (trackingType == UserTrackingType.Add) ? 
					userInfoApi.addUserTracking(type, id, attending, fb_postid) : userInfoApi.editUserTracking(type, id, attending);
			//Log.d(TAG, "result = " + jsonObject.toString());
			
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
			
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return null;
	}
}
