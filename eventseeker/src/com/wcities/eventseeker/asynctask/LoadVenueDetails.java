package com.wcities.eventseeker.asynctask;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;

import com.wcities.eventseeker.api.RecordApi;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.jsonparser.RecordApiJSONParser;

public class LoadVenueDetails extends AsyncTask<Void, Void, Void> {
	
	private Venue venue;
	private String oauthToken;
	private OnVenueUpdatedListener listener;
	
	public interface OnVenueUpdatedListener {
		public void onVenueUpdated();
	}

	public LoadVenueDetails(String oauthToken, Venue venue, OnVenueUpdatedListener listener) {
		this.oauthToken = oauthToken;
		this.venue = venue;
		this.listener = listener;
	}

	@Override
	protected Void doInBackground(Void... params) {
		RecordApi recordApi = new RecordApi(oauthToken, venue.getId());

		try {
			JSONObject jsonObject = recordApi.getRecords();
			RecordApiJSONParser jsonParser = new RecordApiJSONParser();
			jsonParser.fillVenueDetails(jsonObject, venue);
			
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
			
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return null;
	}
	
	@Override
	protected void onPostExecute(Void result) {
		listener.onVenueUpdated();
	} 
}
