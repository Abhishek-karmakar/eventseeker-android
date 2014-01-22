package com.wcities.eventseeker.asynctask;

import java.io.IOException;
import java.util.ArrayList;
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

public class LoadDateWiseMyEvents extends AsyncTask<Void, Void, List<Event>> {
	
	private static final int EVENTS_LIMIT = 10;
	
	private String wcitiesId;
	private Type loadType;
	private double lat, lon;
	
	private DateWiseEventList eventList;
	
	private DateWiseMyEventListAdapter eventListAdapter;
	private MyEventsLoadedListener myEventsLoadedListener;
	
	public interface MyEventsLoadedListener {
		public abstract void onEventsLoaded();
	}
	
	public LoadDateWiseMyEvents(DateWiseEventList eventList, DateWiseMyEventListAdapter eventListAdapter, String wcitiesId, 
			Type loadType, double lat, double lon, MyEventsLoadedListener myEventsLoadedListener) {
		this.eventList = eventList;
		this.eventListAdapter = eventListAdapter;
		this.wcitiesId = wcitiesId;
		this.loadType = loadType;
		this.lat = lat;
		this.lon = lon;
		this.myEventsLoadedListener = myEventsLoadedListener;
	}
	
	@Override
	protected List<Event> doInBackground(Void... params) {
		List<Event> tmpEvents = new ArrayList<Event>();
		int eventsAlreadyRequested = eventListAdapter.getEventsAlreadyRequested();
		
		UserInfoApi userInfoApi = new UserInfoApi(Api.OAUTH_TOKEN);
		userInfoApi.setLimit(EVENTS_LIMIT);
		userInfoApi.setAlreadyRequested(eventsAlreadyRequested);
		userInfoApi.setUserId(wcitiesId);
		userInfoApi.setLat(lat);
		userInfoApi.setLon(lon);
		
		try {
			JSONObject jsonObject = userInfoApi.getMyProfileInfoFor(loadType);
			UserInfoApiJSONParser jsonParser = new UserInfoApiJSONParser();
			
			if (loadType == Type.myevents) {
				MyItemsList<Event> myEventsList = jsonParser.getEventList(jsonObject);
				tmpEvents = myEventsList.getItems();
				
			} else {
				// loadType = Type.recommendedevent
				tmpEvents = jsonParser.getRecommendedEventList(jsonObject);
			}

		} catch (ClientProtocolException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
			
		} catch (JSONException e) {
			e.printStackTrace();
		}

		// TODO: Remove following line
		//tmpEvents.clear();
		return tmpEvents;
	}
	
	@Override
	protected void onPostExecute(List<Event> tmpEvents) {
		if (tmpEvents.size() > 0) {
			eventList.addEventListItems(tmpEvents, this);
			eventListAdapter.setEventsAlreadyRequested(eventListAdapter.getEventsAlreadyRequested() + tmpEvents.size());
			
			if (tmpEvents.size() < EVENTS_LIMIT) {
				eventListAdapter.setMoreDataAvailable(false);
				eventList.removeProgressBarIndicator(this);
			}
			
		} else {
			eventListAdapter.setMoreDataAvailable(false);
			eventList.removeProgressBarIndicator(this);
			// here 1 item is indicating no events message.
			if (eventList.getCount() == 1) {
				myEventsLoadedListener.onEventsLoaded();
			}
		}
		eventListAdapter.notifyDataSetChanged();
	}    	
}
