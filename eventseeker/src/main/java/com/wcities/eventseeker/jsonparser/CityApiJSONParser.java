package com.wcities.eventseeker.jsonparser;

import android.content.Context;
import android.util.Log;

import com.wcities.eventseeker.core.CityPrefered;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CityApiJSONParser {

	private static final String TAG = CityApiJSONParser.class.getSimpleName();

	private static final String KEY_ERROR_MSG = "error_msg";
	private static final String KEY_ERROR_CODE = "error_code";
	private static final String KEY_NEAREST_CITY = "nearestCity";
	private static final String KEY_CITY = "city";
	private static final String KEY_LATITUDE = "latitude";
	private static final String KEY_LONGITUDE = "longitude";
	private static final String KEY_COUNTRY_NAME = "name";
	private static final String KEY_COUNTRY = "country";
	private static final String KEY_CITY_NAME = "cityName";

	public List<CityPrefered> parseCities(Context ctx, JSONObject jObjNearbyCity) {
		if(jObjNearbyCity == null) {
			return null;
		}
		try {
			List<CityPrefered> list = new ArrayList<CityPrefered>();
			JSONObject jObjNearestCity = jObjNearbyCity.getJSONObject(KEY_NEAREST_CITY);

			if (jObjNearestCity.has(KEY_ERROR_CODE)) {
				String msg = jObjNearestCity.getString(KEY_ERROR_MSG);
				Log.e(TAG, "Error : " + msg);
				return null;
			}
			try {
				JSONObject jObjCountry = jObjNearestCity.getJSONObject(KEY_COUNTRY);
				parseJSONCountry(jObjCountry, list);
			} catch (JSONException e) {
				JSONArray jsonCountries = jObjNearestCity.getJSONArray(KEY_COUNTRY);
				for (int i = 0; i < jsonCountries.length(); i++) {
					parseJSONCountry(jsonCountries.getJSONObject(i), list);
				}
			}
			Log.i(TAG, "Returning Results");
			return list;
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
	}

	private void parseJSONCountry(JSONObject jObjCountry, List<CityPrefered> list) throws JSONException {
		if(jObjCountry == null) {
			return;
		}
		String countryName = jObjCountry.getString(KEY_COUNTRY_NAME);
		try {
			JSONObject jObjCity = jObjCountry.getJSONObject(KEY_CITY);
			parseJSONCity(countryName, jObjCity, list);
		} catch (JSONException e) {
			JSONArray jArrayCity = jObjCountry.getJSONArray(KEY_CITY);
			for (int i = 0; i < jArrayCity.length(); i++) {
				parseJSONCity(countryName, jArrayCity.getJSONObject(i), list);
			}
		}
	}

	private void parseJSONCity(String countryName, JSONObject jObjCity, List<CityPrefered> list) throws JSONException {
		if(jObjCity == null) {
			return;
		}
		String cityName = jObjCity.getString(KEY_CITY_NAME);
		double lat = jObjCity.getDouble(KEY_LATITUDE);
		double lon = jObjCity.getDouble(KEY_LONGITUDE);
		list.add(new CityPrefered(countryName, cityName, lat, lon));
	}
}
