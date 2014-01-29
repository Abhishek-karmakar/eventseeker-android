package com.wcities.eventseeker.asynctask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;

import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.EventApi;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.jsonparser.EventApiJSONParser;

public class LoadFeaturedEvts extends AsyncTask<Void, Void, List<Event>> {
	
	private static final int FEATURED_EVTS_LIMIT = 5;

	private OnLoadFeaturedEventsListener listener;
	private double lat, lon;
	
	public interface OnLoadFeaturedEventsListener {
		public void onPreLoadingFeaturedEvents();
		public void onPostLoadingFeaturedEvents(List<Event> result);
	}
	
	public LoadFeaturedEvts(OnLoadFeaturedEventsListener listener, double lat, double lon) {
		this.listener = listener;
		this.lat = lat;
		this.lon = lon;
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		listener.onPreLoadingFeaturedEvents();
	}

	@Override
	protected List<Event> doInBackground(Void... params) {
		List<Event> events = new ArrayList<Event>();
		EventApi eventApi = new EventApi(Api.OAUTH_TOKEN, lat, lon);
		eventApi.setLimit(FEATURED_EVTS_LIMIT);
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

	@Override
	protected void onPostExecute(List<Event> result) {
		listener.onPostLoadingFeaturedEvents(result);
	}
}