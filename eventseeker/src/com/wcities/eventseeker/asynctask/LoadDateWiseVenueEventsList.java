package com.wcities.eventseeker.asynctask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;

import com.wcities.eventseeker.VenueEventsListFragment.DateWiseVenueEventsListAdapter;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.EventApi;
import com.wcities.eventseeker.api.EventApi.IdType;
import com.wcities.eventseeker.api.EventApi.MoreInfo;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.jsonparser.EventApiJSONParser;
import com.wcities.eventseeker.viewdata.DateWiseEventList;

public class LoadDateWiseVenueEventsList extends AsyncTask<Void, Void, List<Event>> {
	
	private static final int EVENTS_LIMIT = 10;
	
	private DateWiseEventList eventList;
	
	private long venueId;

	private DateWiseVenueEventsListAdapter eventListAdapter;
	
	private LoadDateWiseVenueEventsList(DateWiseEventList eventList, DateWiseVenueEventsListAdapter eventListAdapter) {
		this.eventList = eventList;
		this.eventListAdapter = eventListAdapter;
	}

	public LoadDateWiseVenueEventsList(DateWiseEventList eventList, DateWiseVenueEventsListAdapter eventListAdapter, long venueId) {
		this(eventList, eventListAdapter);
		this.venueId = venueId;
	}

	@Override
	protected List<Event> doInBackground(Void... params) {
		List<Event> tmpEvents = new ArrayList<Event>();
		int eventsAlreadyRequested = eventListAdapter.getEventsAlreadyRequested();
		
		EventApi eventApi;
		eventApi = new EventApi(Api.OAUTH_TOKEN, venueId, IdType.VENUE);
		eventApi.setLimit(EVENTS_LIMIT);
		eventApi.setAlreadyRequested(eventsAlreadyRequested);
		eventApi.addMoreInfo(MoreInfo.fallbackimage);

		try {
			JSONObject jsonObject = eventApi.getEvents();
			EventApiJSONParser jsonParser = new EventApiJSONParser();
			
			tmpEvents = jsonParser.getEventList(jsonObject);

		} catch (ClientProtocolException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
			
		} catch (JSONException e) {
			e.printStackTrace();
		}

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
		}
		eventListAdapter.notifyDataSetChanged();
	}    	
}
