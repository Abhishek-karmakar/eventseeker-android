package com.wcities.eventseeker.asynctask;

import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;

import com.wcities.eventseeker.api.ArtistApi;
import com.wcities.eventseeker.api.ArtistApi.Method;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener;
import com.wcities.eventseeker.jsonparser.ArtistApiJSONParser;
import com.wcities.eventseeker.util.ConversionUtil;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class LoadArtistEvents extends AsyncTask<Void, Void, List<Event>> {

	private static final String TAG = LoadArtistEvents.class.getName();
	private static final int EVENTS_LIMIT = 10;
	private static final int TIME_LIMIT_IN_YEARS = 1;

	private DateWiseEventParentAdapterListener eventListAdapter;
	private int artistId;
	private String wcitiesId, oauthToken;
	private List<Event> events;
	
	private LoadArtistEventsListener loadArtistEventsListener;
	
	public interface LoadArtistEventsListener {
		public void onArtistEventsLoaded();
	}

	public LoadArtistEvents(String oauthToken, List<Event> eventList, DateWiseEventParentAdapterListener eventListAdapter, 
			int artistId, String wcitiesId, LoadArtistEventsListener loadArtistEventsListener) {
		this.oauthToken = oauthToken;
		this.events = eventList;
		this.eventListAdapter = eventListAdapter;
		this.artistId = artistId;
		this.wcitiesId = wcitiesId;
		this.loadArtistEventsListener = loadArtistEventsListener;
	}

	@Override
	protected List<Event> doInBackground(Void... params) {

		List<Event> events = new ArrayList<Event>();
		
		try {
			int eventsAlreadyRequested = eventListAdapter.getEventsAlreadyRequested();
			
			ArtistApi artistApi = new ArtistApi(oauthToken);
			artistApi.setMethod(Method.artistEvent);
			artistApi.setVenueDetailEnabled(true);
			artistApi.setArtistId(artistId);
			artistApi.setUserId(wcitiesId);
			artistApi.setLimit(EVENTS_LIMIT);
			artistApi.setTrackingEnabled(true);

			Calendar c = Calendar.getInstance();
			c.add(Calendar.YEAR, TIME_LIMIT_IN_YEARS);
			int year = c.get(Calendar.YEAR);
			int month = c.get(Calendar.MONTH);
			int day = c.get(Calendar.DAY_OF_MONTH);
			String endDate = ConversionUtil.getDay(year, month, day);
			artistApi.setEndDate(endDate);
			
			artistApi.setAlreadyRequested(eventsAlreadyRequested);
			artistApi.setAddTimestamp(true);

			JSONObject jsonObject = artistApi.getArtists();
			ArtistApiJSONParser jsonParser = new ArtistApiJSONParser();
			events = jsonParser.getArtistEvents(jsonObject);

		} catch (ClientProtocolException e) {
			e.printStackTrace();

		} catch (IOException e) {
			e.printStackTrace();

		} catch (JSONException e) {
			e.printStackTrace();
		}

		return events;
	}
	
	@Override
	protected void onPostExecute(List<Event> result) {
		if (result.size() > 0) {
			events.addAll(events.size() - 1, result);
			eventListAdapter.setEventsAlreadyRequested(eventListAdapter.getEventsAlreadyRequested() + result.size());
			
			if (result.size() < EVENTS_LIMIT) {
				eventListAdapter.setMoreDataAvailable(false);
				
				if (!isCancelled()) {
					events.remove(events.size() - 1);
				}
			}
			
		} else {
			eventListAdapter.setMoreDataAvailable(false);
			
			if (!isCancelled()) {
				events.remove(events.size() - 1);
			}
		}
		((RecyclerView.Adapter)eventListAdapter).notifyDataSetChanged();
		
		if (loadArtistEventsListener != null) {
			loadArtistEventsListener.onArtistEventsLoaded();
		}
	}    	

}
