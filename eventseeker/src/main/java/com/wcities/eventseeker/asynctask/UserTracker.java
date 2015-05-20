package com.wcities.eventseeker.asynctask;

import android.os.AsyncTask;

import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingItemType;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingType;
import com.wcities.eventseeker.app.EventSeekr;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

public class UserTracker extends AsyncTask<Void, Void, Void> {
	
	private static final String TAG = UserTracker.class.getName();
	
	private UserTrackingType trackingType;
	private EventSeekr eventSeekr;
	private UserTrackingItemType type;
	private long id;
	private List<Long> ids;
	private int attending;
	private String fb_postid, oauthToken;
	
	public UserTracker(String oauthToken, EventSeekr eventSeekr, UserTrackingItemType type, long id) {
		this.oauthToken = oauthToken;
		this.eventSeekr = eventSeekr;
		this.type = type;
		this.id = id;
		trackingType = UserTrackingType.Add;
	}

	public UserTracker(String oauthToken, EventSeekr eventSeekr, UserTrackingItemType type, List<Long> ids) {
		this.oauthToken = oauthToken;
		this.eventSeekr = eventSeekr;
		this.type = type;
		this.ids = ids;
		trackingType = UserTrackingType.AddMultiple;
	}
	
	public UserTracker(String oauthToken, EventSeekr eventSeekr, UserTrackingItemType type, long id, int attending, UserTrackingType trackingType) {
		this(oauthToken, eventSeekr, type, id);
		this.attending = attending;
		this.trackingType = trackingType;
	}
	
	public UserTracker(String oauthToken, EventSeekr eventSeekr, UserTrackingItemType type, long id, int attending, String fb_postid, 
			UserTrackingType trackingType) {
		this(oauthToken, eventSeekr, type, id);
		this.attending = attending;
		this.fb_postid = fb_postid;
		this.trackingType = trackingType;
	}

	@Override
	protected Void doInBackground(Void... params) {
		UserInfoApi userInfoApi = new UserInfoApi(oauthToken);
		userInfoApi.setUserId(eventSeekr.getWcitiesId());
		try {
			JSONObject jsonObject;
			if (trackingType == UserTrackingType.Add) {
				jsonObject = userInfoApi.addUserTracking(type, id, attending, fb_postid);
			
			} else if (trackingType == UserTrackingType.AddMultiple) {				
				jsonObject = userInfoApi.addMultipleUserTracking(type, ids);
			
			} else {
				jsonObject = userInfoApi.editUserTracking(type, id, attending);
			}
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
