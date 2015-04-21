package com.wcities.eventseeker.asynctask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.widget.BaseAdapter;

import com.wcities.eventseeker.api.EventApi;
import com.wcities.eventseeker.api.EventApi.IdType;
import com.wcities.eventseeker.api.EventApi.MoreInfo;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener;
import com.wcities.eventseeker.jsonparser.EventApiJSONParser;
import com.wcities.eventseeker.viewdata.DateWiseEventList;

public class LoadDateWiseVenueEventsList extends AsyncTask<Void, Void, List<Event>> {
	
	private static final int EVENTS_LIMIT = 10;

	private static final String TAG = LoadDateWiseVenueEventsList.class.getName();
	
	private DateWiseEventList eventList;
	
	private long venueId;

	private DateWiseEventParentAdapterListener eventListAdapter;

	private String wcitiesId, oauthToken;
	
	private EventExistListener listener;
	
	public interface EventExistListener {
		public void hasEvents(boolean hasEvents);
	}
	
	private LoadDateWiseVenueEventsList(String oauthToken, DateWiseEventList eventList, DateWiseEventParentAdapterListener eventListAdapter, String wcitiesId) {
		this.oauthToken = oauthToken;
		this.eventList = eventList;
		this.eventListAdapter = eventListAdapter;
		this.wcitiesId = wcitiesId;
	}

	public LoadDateWiseVenueEventsList(String oauthToken, DateWiseEventList eventList, DateWiseEventParentAdapterListener eventListAdapter, String wcitiesId, long venueId) {
		this(oauthToken, eventList, eventListAdapter, wcitiesId);
		this.venueId = venueId;
	}
	
	public LoadDateWiseVenueEventsList(String oauthToken, DateWiseEventList eventList, 
			DateWiseEventParentAdapterListener eventListAdapter, String wcitiesId, long venueId, 
			EventExistListener listener) {
		this(oauthToken, eventList, eventListAdapter, wcitiesId);
		this.venueId = venueId;
		this.listener = listener;
	}

	@Override
	protected List<Event> doInBackground(Void... params) {
		List<Event> tmpEvents = new ArrayList<Event>();
		
		int eventsAlreadyRequested = 0;
		if(eventListAdapter != null) {
			eventsAlreadyRequested = eventListAdapter.getEventsAlreadyRequested();
		}
		
		EventApi eventApi;
		eventApi = new EventApi(oauthToken, venueId, IdType.VENUE);
		eventApi.setLimit(EVENTS_LIMIT);
		eventApi.setAlreadyRequested(eventsAlreadyRequested);
		eventApi.addMoreInfo(MoreInfo.fallbackimage);
		eventApi.setUserId(wcitiesId);

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
		/**
		 *  This 'eventListAdapter != null' check is kept for the BoschVenueDetails screen. For this screen, 
		 *  we just need to calculate the number of events. That's why the adapter will be sent null. 
		 *  So, in that case just add all the temporary list items to the given list.
		 */
		if(eventListAdapter != null) {
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
			((BaseAdapter)eventListAdapter).notifyDataSetChanged();
		
		} else if (listener != null) {
			listener.hasEvents(tmpEvents.size() > 0);
		}
	}    	
}
