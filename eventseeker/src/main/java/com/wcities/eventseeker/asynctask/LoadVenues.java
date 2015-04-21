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
import android.support.v7.widget.RecyclerView.Adapter;
import android.widget.BaseAdapter;

import com.wcities.eventseeker.adapter.AbstractVenueListAdapter;
import com.wcities.eventseeker.adapter.RVSearchArtistsAdapterTab;
import com.wcities.eventseeker.adapter.RVSearchVenuesAdapterTab;
import com.wcities.eventseeker.api.RecordApi;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.interfaces.VenueAdapterListener;
import com.wcities.eventseeker.jsonparser.RecordApiJSONParser;

public class LoadVenues extends AsyncTask<String, Void, List<Venue>> {
	
	private static final int MILES = 50000;

	private static final int VENUES_LIMIT = 10;

	private static final String TAG = LoadVenues.class.getName();
	
	private VenueAdapterListener<String> venueAdapterListener;
	
	private List<Venue> venueList;
	private double[] latLon;
	private String oauthToken;
	
	private AsyncTaskListener<Void> asyncTaskListener;
	
	public LoadVenues(String oauthToken, VenueAdapterListener<String> venueAdapterListener, List<Venue> venueList, 
			double[] latLon) {
		this.oauthToken = oauthToken;
		this.venueAdapterListener = venueAdapterListener;
		this.venueList = venueList;
		this.latLon = latLon;
	}
	
	public LoadVenues(String oauthToken, VenueAdapterListener<String> venueAdapterListener, List<Venue> venueList, 
			double[] latLon, AsyncTaskListener<Void> asyncTaskListener) {
		this.oauthToken = oauthToken;
		this.venueAdapterListener = venueAdapterListener;
		this.venueList = venueList;
		this.latLon = latLon;
		this.asyncTaskListener = asyncTaskListener;
	}

	@Override
	protected List<Venue> doInBackground(String... params) {
		RecordApi recordApi = new RecordApi(oauthToken, latLon[0], latLon[1]);
		recordApi.setLimit(VENUES_LIMIT);
		recordApi.setAlreadyRequested(venueAdapterListener.getVenuesAlreadyRequested());
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
			venueAdapterListener.setVenuesAlreadyRequested(
				venueAdapterListener.getVenuesAlreadyRequested() + tmpVenues.size());
			
			if (tmpVenues.size() < VENUES_LIMIT) {
				venueAdapterListener.setMoreDataAvailable(false);
				venueList.remove(venueList.size() - 1);
			}
			
		} else {
			venueAdapterListener.setMoreDataAvailable(false);
			venueList.remove(venueList.size() - 1);
			if (venueList.isEmpty()) {
				venueList.add(new Venue(AppConstants.INVALID_ID));
			}
		}
		
		if (venueAdapterListener instanceof BaseAdapter) {
			((BaseAdapter) venueAdapterListener).notifyDataSetChanged();
			
		} else {
			((Adapter<RVSearchVenuesAdapterTab.ViewHolder>) venueAdapterListener).notifyDataSetChanged();
		}
		if (asyncTaskListener != null) {
			asyncTaskListener.onTaskCompleted();
		}
	}    	
}