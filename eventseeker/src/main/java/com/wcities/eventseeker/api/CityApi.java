package com.wcities.eventseeker.api;

import android.util.Log;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class CityApi extends Api {
	
	private static final String TAG = CityApi.class.getSimpleName();
	private static final String API = "city_api/";

	public CityApi(String oauthToken) {
		super(oauthToken);
	}

	public JSONObject getCities(String city) throws ClientProtocolException, IOException, JSONException {
		String METHOD = "getNearCity.php?";
		StringBuilder uriBuilder = new StringBuilder(COMMON_URL).append(API).append(METHOD).append("oauth_token=")
			.append(getOauthToken()).append("&similarCity=").append(city);
		
		setUri(uriBuilder.toString());
		Log.d(TAG, "uri=" + getUri());
		return execute(RequestMethod.GET, null, null);
	}

	public JSONObject getNearbyCities(double lat, double lon) throws ClientProtocolException, IOException, JSONException {
		String METHOD = "getNearCity.php?";
		StringBuilder uriBuilder = new StringBuilder(COMMON_URL).append(API).append(METHOD).append("oauth_token=")
			.append(getOauthToken()).append("&lat=").append(lat).append("&lon=").append(lon)
			.append("&compact=country").append("&moreInfo=allnearby");
		
		setUri(uriBuilder.toString());
		Log.d(TAG, "uri=" + getUri());
		return execute(RequestMethod.GET, null, null);
	}
}
