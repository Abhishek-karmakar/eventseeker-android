package com.wcities.eventseeker.jsonparser;

import org.json.JSONException;
import org.json.JSONObject;

public class IPToCityApiJSONParser {

	private static final String TAG = IPToCityApiJSONParser.class.getName();

	private static final String KEY_NEAREST_CITY = "nearestCity";
	private static final String KEY_CITY = "city";
	private static final String KEY_LATITUDE = "latitude";
	private static final String KEY_LONGITUDE = "longitude";
	
	public double[] getLatlon(JSONObject jsonObject) throws JSONException {
		double[] latlon = new double[] {0, 0};
		if (jsonObject.has(KEY_NEAREST_CITY)) {
			JSONObject jObjNearestCity = jsonObject.getJSONObject(KEY_NEAREST_CITY);
			if (jObjNearestCity.has(KEY_CITY)) {
				JSONObject jObjCity = jObjNearestCity.getJSONObject(KEY_CITY);
				if (jObjCity.has(KEY_LATITUDE)) {
					latlon[0] = jObjCity.getDouble(KEY_LATITUDE);
					latlon[1] = jObjCity.getDouble(KEY_LONGITUDE);
				}
			}
		}
		return latlon;
	}
}
