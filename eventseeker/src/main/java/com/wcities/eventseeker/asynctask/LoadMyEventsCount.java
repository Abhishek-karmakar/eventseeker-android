package com.wcities.eventseeker.asynctask;

import android.os.AsyncTask;

import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.api.UserInfoApi.Type;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class LoadMyEventsCount extends AsyncTask<Void, Void, Integer> {
	
	private static final int EVENTS_LIMIT = 1;
	
	private String wcitiesId, oauthToken;
	private double lat, lon;
	
	private AsyncTaskListener<Integer> listener;
	
	public LoadMyEventsCount(String oauthToken, String wcitiesId, double lat, double lon, AsyncTaskListener<Integer> listener) {
		this.oauthToken = oauthToken;
		this.wcitiesId = wcitiesId;
		this.lat = lat;
		this.lon = lon;
		this.listener = listener;
	}
	
	@Override
	protected Integer doInBackground(Void... params) {
		int count = 0;
		
		UserInfoApi userInfoApi = new UserInfoApi(oauthToken);
		userInfoApi.setLimit(EVENTS_LIMIT);
		userInfoApi.setUserId(wcitiesId);
		userInfoApi.setLat(lat);
		userInfoApi.setLon(lon);
		
		try {
			JSONObject jsonObject = userInfoApi.getMyProfileInfoFor(Type.myevents);
			UserInfoApiJSONParser jsonParser = new UserInfoApiJSONParser();
			
			count = jsonParser.getMyEventsCount(jsonObject);

		} catch (ClientProtocolException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return count;
	}
	
	@Override
	protected void onPostExecute(Integer evtCount) {
		listener.onTaskCompleted(evtCount);
	}    	
}
