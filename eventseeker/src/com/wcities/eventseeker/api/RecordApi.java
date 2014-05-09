package com.wcities.eventseeker.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.wcities.eventseeker.api.EventApi.MoreInfo;
import com.wcities.eventseeker.constants.AppConstants;

public class RecordApi extends Api {
	
	private static final String TAG = RecordApi.class.getName();
	private static final String API = "record_api/";
	
	private long id;
	private double lat, lon;
	private int limit;
	private int alreadyRequested;
	private int miles;
	private String searchFor;

	public RecordApi(String oauthToken, double lat, double lon) {
		super(oauthToken);
		this.lat = lat;
		this.lon = lon;
	}
	
	public RecordApi(String oauthToken, long id) {
		super(oauthToken);
		this.id = id;
	}
	
	public double getLat() {
		return lat;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	public double getLon() {
		return lon;
	}

	public void setLon(double lon) {
		this.lon = lon;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public int getAlreadyRequested() {
		return alreadyRequested;
	}

	public void setAlreadyRequested(int alreadyRequested) {
		this.alreadyRequested = alreadyRequested;
	}

	public int getMiles() {
		return miles;
	}

	public void setMiles(int miles) {
		this.miles = miles;
	}

	public String getSearchFor() {
		return searchFor;
	}

	public void setSearchFor(String searchFor) {
		this.searchFor = searchFor;
	}
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public JSONObject getRecords() throws ClientProtocolException, IOException, JSONException {
		String METHOD = "getRecords.php?";
		StringBuilder uriBuilder = new StringBuilder(COMMON_URL).append(API).append(METHOD).append("oauth_token=").append(getOauthToken());
		
		if (id != NOT_INITIALIZED) {
			uriBuilder.append("&id=").append(id);
			
		} else if (lat != AppConstants.NOT_ALLOWED_LAT && lon != AppConstants.NOT_ALLOWED_LON) {
			uriBuilder.append("&lat=").append(lat).append("&lon=").append(lon);
		}
		
		if (miles != NOT_INITIALIZED) {
			uriBuilder.append("&miles=").append(miles);
		}
		
		if (limit != NOT_INITIALIZED) {
			uriBuilder.append("&limit=").append(alreadyRequested).append(",").append(limit);
		}
		
		if (searchFor != null) {
			uriBuilder.append("&searchFor=").append(searchFor);
		}
		
		uriBuilder.append("&moreInfo=fallbackimage,strictlang");
		
		uriBuilder.append("&strip_html=name,description");
		
		setUri(uriBuilder.toString());
		addLangParam = true;
		return execute(RequestMethod.GET, null, null);
	}
}
