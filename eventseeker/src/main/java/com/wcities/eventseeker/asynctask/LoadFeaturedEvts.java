package com.wcities.eventseeker.asynctask;

import android.os.AsyncTask;

import com.wcities.eventseeker.api.EventApi;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.jsonparser.EventApiJSONParser;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LoadFeaturedEvts extends AsyncTask<Void, Void, List<Event>> {
	
	private static final int FEATURED_EVTS_LIMIT = 5;

	private double lat, lon;
	private String oauthToken, wcitiesId;
	
	public LoadFeaturedEvts(String oauthToken, double lat, double lon, String wcitiesId) {
		this.oauthToken = oauthToken;
		this.lat = lat;
		this.lon = lon;
		this.wcitiesId = wcitiesId;
	}
	
	@Override
	protected List<Event> doInBackground(Void... params) {
		List<Event> events = new ArrayList<Event>();
		EventApi eventApi = new EventApi(oauthToken, lat, lon);
		eventApi.setLimit(FEATURED_EVTS_LIMIT);
		eventApi.setUserId(wcitiesId);
		try {
			JSONObject jsonObject = eventApi.getFeaturedEvents();
			EventApiJSONParser jsonParser = new EventApiJSONParser();
			events = jsonParser.getFeaturedEventList(jsonObject);

		} catch (ClientProtocolException e) {
			e.printStackTrace();

		} catch (IOException e) {
			e.printStackTrace();

		} catch (JSONException e) {
			e.printStackTrace();
		}

		return events;
	}
}