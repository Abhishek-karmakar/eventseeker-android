package com.wcities.eventseeker.api;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class IPToCityApi extends Api {
	
	private static final String TAG = IPToCityApi.class.getName();
	private static final String API = "iptocity_api/";

	public IPToCityApi(String oauthToken) {
		super(oauthToken);
	}

	public JSONObject findLatLon() throws ClientProtocolException, IOException, JSONException {
		String METHOD = "iptocity.php?";
		StringBuilder uriBuilder = new StringBuilder(COMMON_URL).append(API).append(METHOD).append("oauth_token=").append(getOauthToken());
		
		setUri(uriBuilder.toString());
		addLangParam = true;
		return execute(RequestMethod.GET, null, null);
	}
}
