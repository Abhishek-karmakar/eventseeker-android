package com.wcities.eventseeker.asynctask;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;

import com.wcities.eventseeker.api.EventApi;
import com.wcities.eventseeker.api.EventApi.MoreInfo;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener;
import com.wcities.eventseeker.jsonparser.EventApiJSONParser;

public class LoadEvents extends AsyncTask<Void, Void, List<Event>> {
	
	private static final int EVENTS_LIMIT = 10;

	private static final String TAG = LoadDateWiseEvents.class.getName();
	
	private List<Event> eventList;
	
	private String query;
	
	private double lat, lon;
	private String startDate, endDate;
	private int categoryId;
	private int miles;
	private String wcitiesId, oauthToken;
	
	private DateWiseEventParentAdapterListener eventListAdapter;
	
	private LoadEvents(String oauthToken, List<Event> eventList, DateWiseEventParentAdapterListener eventListAdapter, 
			double lat, double lon, String wcitiesId) {
		this.oauthToken = oauthToken;
		this.eventList = eventList;
		this.eventListAdapter = eventListAdapter;
		this.lat = lat;
		this.lon = lon;
		this.wcitiesId = wcitiesId;
	}

	public LoadEvents(String oauthToken, List<Event> eventList, DateWiseEventParentAdapterListener eventListAdapter, double lat, 
			double lon, String startDate, String endDate, int categoryId, String wcitiesId, int miles) {
		this(oauthToken, eventList, eventListAdapter, lat, lon, wcitiesId);
		this.startDate = startDate;
		this.endDate = endDate;
		this.categoryId = categoryId;
		this.miles = miles;
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
			eventList.addAll(eventList.size() - 1, result);
			eventListAdapter.setEventsAlreadyRequested(eventListAdapter.getEventsAlreadyRequested() + result.size());
			
			if (result.size() < EVENTS_LIMIT) {
				eventListAdapter.setMoreDataAvailable(false);
				
				if (!isCancelled()) {
					//Log.d(TAG, "remove");
					eventList.remove(eventList.size() - 1);
				}
			}
			
		} else {
			eventListAdapter.setMoreDataAvailable(false);
			
			if (!isCancelled()) {
				//Log.d(TAG, "remove");
				eventList.remove(eventList.size() - 1);
			}
		}
		
		/*if (eventListAdapter instanceof BaseAdapter) {
			((BaseAdapter)eventListAdapter).notifyDataSetChanged();
			
		} else {*/
			((RecyclerView.Adapter)eventListAdapter).notifyDataSetChanged();
		//}
	}   
}
