package com.wcities.eventseeker.asynctask;

import java.io.IOException;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;

import com.wcities.eventseeker.adapter.DateWiseMyEventListAdapter;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.api.UserInfoApi.Type;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser.MyItemsList;
import com.wcities.eventseeker.viewdata.DateWiseEventList;

public class LoadDateWiseMyEvents extends AsyncTask<Void, Void, Void> {
	
	private static final int EVENTS_LIMIT = 10;
	
	private String wcitiesId;
	private Type loadType;
	
	private DateWiseEventList eventList;
	
	private DateWiseMyEventListAdapter eventListAdapter;
	
	public LoadDateWiseMyEvents(DateWiseEventList eventList, DateWiseMyEventListAdapter eventListAdapter, String wcitiesId, Type loadType) {
		this.eventList = eventList;
		this.eventListAdapter = eventListAdapter;
		this.wcitiesId = wcitiesId;
		this.loadType = loadType;
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		int eventsAlreadyRequested = eventListAdapter.getEventsAlreadyRequested();
		
		UserInfoApi userInfoApi = new UserInfoApi(Api.OAUTH_TOKEN);
		userInfoApi.setLimit(EVENTS_LIMIT);
		userInfoApi.setAlreadyRequested(eventsAlreadyRequested);
		userInfoApi.setUserId(wcitiesId);
		
		try {
			JSONObject jsonObject = userInfoApi.getMyProfileInfoFor(loadType);
			UserInfoApiJSONParser jsonParser = new UserInfoApiJSONParser();
			
			List<Event> tmpEvents;
			if (loadType == Type.myevents) {
				MyItemsList<Event> myEventsList = jsonParser.getEventList(jsonObject);
				tmpEvents = myEventsList.getItems();
				
			} else {
				// loadType = Type.recommendedevent
				tmpEvents = jsonParser.getRecommendedEventList(jsonObject);
			}

			if (tmpEvents.size() > 0) {
				eventList.addEventListItems(tmpEvents, this);
				eventsAlreadyRequested += tmpEvents.size();
				eventListAdapter.setEventsAlreadyRequested(eventsAlreadyRequested);
				
				if (tmpEvents.size() < EVENTS_LIMIT) {
					eventListAdapter.setMoreDataAvailable(false);
					eventList.removeProgressBarIndicator(this);
				}
				
			} else {
				eventListAdapter.setMoreDataAvailable(false);
				eventList.removeProgressBarIndicator(this);
			}
			
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
		eventListAdapter.notifyDataSetChanged();
	}    	
}
