package com.wcities.eventseeker.asynctask;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;

import com.wcities.eventseeker.adapter.DateWiseEventListAdapter;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.EventApi;
import com.wcities.eventseeker.api.EventApi.IdType;
import com.wcities.eventseeker.api.EventApi.MoreInfo;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.jsonparser.EventApiJSONParser;
import com.wcities.eventseeker.viewdata.DateWiseEventList;

public class LoadDateWiseEvents extends AsyncTask<Void, Void, List<Event>> {
	
	private static final int EVENTS_LIMIT = 10;
	
	private DateWiseEventList eventList;
	
	private String query;
	
	private double lat, lon;
	private String startDate;
	private int categoryId;
	private int miles;
	
	private DateWiseEventListAdapter eventListAdapter;
	
	private LoadDateWiseEvents(DateWiseEventList eventList, DateWiseEventListAdapter eventListAdapter, 
			double lat, double lon) {
		this.eventList = eventList;
		this.eventListAdapter = eventListAdapter;
		this.lat = lat;
		this.lon = lon;
	}

	public LoadDateWiseEvents(DateWiseEventList eventList, DateWiseEventListAdapter eventListAdapter, String query, 
			double lat, double lon, int miles) {
		this(eventList, eventListAdapter, lat, lon);
		this.query = query;
		this.miles = miles;
	}
	
	public LoadDateWiseEvents(DateWiseEventList eventList, DateWiseEventListAdapter eventListAdapter, double lat, 
			double lon, String startDate, int categoryId) {
		this(eventList, eventListAdapter, lat, lon);
		this.startDate = startDate;
		this.categoryId = categoryId;
	}

	@Override
	protected List<Event> doInBackground(Void... params) {
		List<Event> tmpEvents = new ArrayList<Event>();
		int eventsAlreadyRequested = eventListAdapter.getEventsAlreadyRequested();
		
		EventApi eventApi;
		eventApi = new EventApi(Api.OAUTH_TOKEN, lat, lon);
		eventApi.setLimit(EVENTS_LIMIT);
		eventApi.setAlreadyRequested(eventsAlreadyRequested);
		eventApi.addMoreInfo(MoreInfo.fallbackimage);
		if (miles != 0) {
			eventApi.setMiles(miles);
		}

		try {
			if (query != null) {
				eventApi.setStrictSearchEnabled(true);
				eventApi.setSearchFor(URLEncoder.encode(query, AppConstants.CHARSET_NAME));
				
			} else {
				// in case if this AsyncTask is called from DiscoverByCategoryFragment.
				eventApi.setStart(startDate);
				eventApi.setCategory(categoryId);
			}
			
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
	protected void onPostExecute(List<Event> result) {
		if (result.size() > 0) {
			eventList.addEventListItems(result, this);
			eventListAdapter.setEventsAlreadyRequested(eventListAdapter.getEventsAlreadyRequested() + result.size());
			
			if (result.size() < EVENTS_LIMIT) {
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