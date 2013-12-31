package com.wcities.eventseeker.asynctask;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.BaseAdapter;

import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.EventApi;
import com.wcities.eventseeker.api.EventApi.MoreInfo;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener;
import com.wcities.eventseeker.jsonparser.EventApiJSONParser;
import com.wcities.eventseeker.viewdata.DateWiseEventList;

public class LoadDateWiseEvents extends AsyncTask<Void, Void, List<Event>> {
	
	private static final int EVENTS_LIMIT = 10;

	private static final String TAG = LoadDateWiseEvents.class.getName();
	
	private DateWiseEventList eventList;
	
	private String query;
	
	private double lat, lon;
	private String startDate;
	private int categoryId;
	private int miles;
	private String wcitiesId;
	
	private DateWiseEventParentAdapterListener eventListAdapter;
	
	private LoadDateWiseEvents(DateWiseEventList eventList, DateWiseEventParentAdapterListener eventListAdapter, 
			double lat, double lon, String wcitiesId) {
		this.eventList = eventList;
		this.eventListAdapter = eventListAdapter;
		this.lat = lat;
		this.lon = lon;
		this.wcitiesId = wcitiesId;
	}

	public LoadDateWiseEvents(DateWiseEventList eventList, DateWiseEventParentAdapterListener eventListAdapter, String query, 
			double lat, double lon, int miles, String wcitiesId) {
		this(eventList, eventListAdapter, lat, lon, wcitiesId);
		this.query = query;
		this.miles = miles;
	}
	
	public LoadDateWiseEvents(DateWiseEventList eventList, DateWiseEventParentAdapterListener eventListAdapter, double lat, 
			double lon, String startDate, int categoryId, String wcitiesId) {
		this(eventList, eventListAdapter, lat, lon, wcitiesId);
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
		eventApi.setUserId(wcitiesId);//it can be null also
		if (miles != 0) {
			eventApi.setMiles(miles);
		}

		try {
			if (query != null) {
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
		((BaseAdapter)eventListAdapter).notifyDataSetChanged();
	}    	
}