package com.wcities.eventseeker.asynctask;

import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.widget.BaseAdapter;

import com.wcities.eventseeker.api.EventApi;
import com.wcities.eventseeker.api.EventApi.MoreInfo;
import com.wcities.eventseeker.bosch.interfaces.BoschAsyncTaskListener;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener;
import com.wcities.eventseeker.jsonparser.EventApiJSONParser;
import com.wcities.eventseeker.viewdata.DateWiseEventList;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class LoadDateWiseEvents extends AsyncTask<Void, Void, List<Event>> {
	
	private static final int EVENTS_LIMIT = 10;

	private static final String TAG = LoadDateWiseEvents.class.getName();
	
	private DateWiseEventList eventList;
	
	private String query;
	
	private double lat, lon;
	private String startDate, endDate;
	private int categoryId;
	private int miles;
	private String wcitiesId, oauthToken;
	
	private DateWiseEventParentAdapterListener eventListAdapter;
	private BoschAsyncTaskListener boschAsyncTaskListener;
	
	private LoadDateWiseEvents(String oauthToken, DateWiseEventList eventList, DateWiseEventParentAdapterListener eventListAdapter, 
			double lat, double lon, String wcitiesId) {
		this.oauthToken = oauthToken;
		this.eventList = eventList;
		this.eventListAdapter = eventListAdapter;
		this.lat = lat;
		this.lon = lon;
		this.wcitiesId = wcitiesId;
	}

	public LoadDateWiseEvents(String oauthToken, DateWiseEventList eventList, DateWiseEventParentAdapterListener eventListAdapter, String query, 
			double lat, double lon, int miles, String wcitiesId, String startDate, String endDate) {
		this(oauthToken, eventList, eventListAdapter, lat, lon, wcitiesId);
		this.query = query;
		this.miles = miles;
		this.startDate = startDate;
		this.endDate = endDate;
	}
	
	public LoadDateWiseEvents(String oauthToken, DateWiseEventList eventList, DateWiseEventParentAdapterListener eventListAdapter, double lat, 
			double lon, String startDate, String endDate, int categoryId, String wcitiesId) {
		this(oauthToken, eventList, eventListAdapter, lat, lon, wcitiesId);
		this.startDate = startDate;
		this.endDate = endDate;
		this.categoryId = categoryId;
	}

	public void setBoschAsyncTaskListener(BoschAsyncTaskListener boschAsyncTaskListener) {
		this.boschAsyncTaskListener = boschAsyncTaskListener;
	}

	@Override
	protected List<Event> doInBackground(Void... params) {
		List<Event> tmpEvents = new ArrayList<Event>();
		int eventsAlreadyRequested = eventListAdapter.getEventsAlreadyRequested();
		
		EventApi eventApi;
		eventApi = new EventApi(oauthToken, lat, lon);
		eventApi.setLimit(EVENTS_LIMIT);
		eventApi.setAlreadyRequested(eventsAlreadyRequested);
		eventApi.addMoreInfo(MoreInfo.fallbackimage);
		eventApi.setUserId(wcitiesId);//it can be null also
		eventApi.setStart(startDate);
		eventApi.setEnd(endDate);

		if (miles != 0) {
			eventApi.setMiles(miles);
		}

		try {
			if (query != null) {
				eventApi.setSearchFor(URLEncoder.encode(query, AppConstants.CHARSET_NAME));
				
			} else {
				// in case if this AsyncTask is called from DiscoverByCategoryFragment.
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
		
		if (eventListAdapter instanceof BaseAdapter) {
			((BaseAdapter)eventListAdapter).notifyDataSetChanged();
		// boschAsyncTaskListener.onTaskCompleted();
			
		} else {
			((RecyclerView.Adapter)eventListAdapter).notifyDataSetChanged();
		}
	}    	
}