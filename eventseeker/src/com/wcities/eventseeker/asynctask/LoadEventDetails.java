package com.wcities.eventseeker.asynctask;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.support.v4.app.Fragment;

import com.wcities.eventseeker.api.EventApi;
import com.wcities.eventseeker.api.EventApi.IdType;
import com.wcities.eventseeker.api.EventApi.MoreInfo;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.jsonparser.EventApiJSONParser;
import com.wcities.eventseeker.util.FragmentUtil;

public class LoadEventDetails extends AsyncTask<Void, Void, Void> {
	
	private OnEventUpdatedListner listner;
	private Fragment fragment;
	private Event event;
	private String oauthToken;
	
	public interface OnEventUpdatedListner{
		public void onEventUpdated();
	};
	
	public LoadEventDetails(String oauthToken, OnEventUpdatedListner listner, Fragment fragment, Event event) {
		this.oauthToken = oauthToken;
		this.fragment = fragment;
		this.event = event;
		this.listner = listner;
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		//Log.d(TAG, "LoadEventDetails doInBackground()");
		EventApi eventApi = new EventApi(oauthToken, event.getId(), IdType.EVENT);
		
		// null check is not required here, since if it's null, that's handled from eventApi
		eventApi.setUserId(((EventSeekr)FragmentUtil.getActivity(fragment).getApplication()).getWcitiesId());
		eventApi.setFriendsEnabled(true);
		eventApi.addMoreInfo(MoreInfo.fallbackimage);
		
		try {
			JSONObject jsonObject = eventApi.getEvents();
			EventApiJSONParser jsonParser = new EventApiJSONParser();
			jsonParser.fillEventDetails(event, jsonObject);
			
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
			
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return null;
	}
	
	@Override
	protected void onPostExecute(Void result) {
		listner.onEventUpdated();
	}
}