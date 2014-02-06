package com.wcities.eventseeker.asynctask;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.support.v4.app.Fragment;

import com.wcities.eventseeker.adapter.AbstractVenueListAdapter;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.RecordApi;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.jsonparser.RecordApiJSONParser;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class LoadVenues extends AsyncTask<String, Void, List<Venue>> {
	
	private static final int MILES = 50000;

	private static final int VENUES_LIMIT = 10;

	private static final String TAG = LoadVenues.class.getName();
	
	private Fragment fragment;
	
	private AbstractVenueListAdapter venueListAdapter;
	
	private List<Venue> venueList;
	
	public LoadVenues(Fragment fragment, AbstractVenueListAdapter venueListAdapter, List<Venue> venueList) {
		this.fragment = fragment;
		this.venueListAdapter = venueListAdapter;
		this.venueList = venueList;
	}
	
	@Override
	protected List<Venue> doInBackground(String... params) {
		double[] latLon = DeviceUtil.getLatLon(FragmentUtil.getActivity(fragment));
		RecordApi recordApi = new RecordApi(Api.OAUTH_TOKEN, latLon[0], latLon[1]);
		recordApi.setLimit(VENUES_LIMIT);
		recordApi.setAlreadyRequested(venueListAdapter.getVenuesAlreadyRequested());
		recordApi.setMiles(MILES);

		List<Venue> tmpVenues = new ArrayList<Venue>();
		try {
			recordApi.setSearchFor(URLEncoder.encode(params[0], AppConstants.CHARSET_NAME));
			
			JSONObject jsonObject = recordApi.getRecords();
			RecordApiJSONParser jsonParser = new RecordApiJSONParser();
			tmpVenues = jsonParser.getVenueList(jsonObject);
			
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
			
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return tmpVenues;
	}
	
	@Override
	protected void onPostExecute(List<Venue> tmpVenues) {
		if (tmpVenues.size() > 0) {
			venueList.addAll(venueList.size() - 1, tmpVenues);
			venueListAdapter.setVenuesAlreadyRequested(
				venueListAdapter.getVenuesAlreadyRequested() + tmpVenues.size());
			
			if (tmpVenues.size() < VENUES_LIMIT) {
				venueListAdapter.setMoreDataAvailable(false);
				venueList.remove(venueList.size() - 1);
			}
			
		} else {
			venueListAdapter.setMoreDataAvailable(false);
			venueList.remove(venueList.size() - 1);
			if(venueList.isEmpty()) {
				venueList.add(new Venue(AppConstants.INVALID_ID));
			}
		}
		
		venueListAdapter.notifyDataSetChanged();
	}    	
}