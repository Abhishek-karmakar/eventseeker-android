package com.wcities.eventseeker.asynctask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.widget.BaseAdapter;

import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.ArtistApi;
import com.wcities.eventseeker.api.ArtistApi.Method;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener;
import com.wcities.eventseeker.jsonparser.ArtistApiJSONParser;
import com.wcities.eventseeker.viewdata.DateWiseEventList;

public class LoadArtistEvents extends AsyncTask<Void, Void, List<Event>> {

	private static final String TAG = LoadArtistEvents.class.getName();
	private static final int EVENTS_LIMIT = 10;

	private DateWiseEventList eventList;
	private DateWiseEventParentAdapterListener eventListAdapter;
	private int artistId;
	private String wcitiesId;

	public LoadArtistEvents(DateWiseEventList eventList, DateWiseEventParentAdapterListener eventListAdapter, int artistId, String wcitiesId) {
		this.eventList = eventList;
		this.eventListAdapter = eventListAdapter;
		this.artistId = artistId;
		this.wcitiesId = wcitiesId;
	}

	@Override
	protected List<Event> doInBackground(Void... params) {

		List<Event> events = new ArrayList<Event>();
		
		try {
			int eventsAlreadyRequested = eventListAdapter.getEventsAlreadyRequested();
			
			ArtistApi artistApi = new ArtistApi(Api.OAUTH_TOKEN);
			artistApi.setMethod(Method.artistEvent);
			artistApi.setVenueDetailEnabled(true);
			artistApi.setArtistId(artistId);
			artistApi.setUserId(wcitiesId);
			artistApi.setLimit(EVENTS_LIMIT);
			artistApi.setAlreadyRequested(eventsAlreadyRequested);

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
