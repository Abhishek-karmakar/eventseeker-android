package com.wcities.eventseeker.util;

import java.net.URLEncoder;
import java.util.Locale;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import com.wcities.eventseeker.constants.AppConstants;

import android.location.Address;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.util.Log;

public class GeoUtil {
	
    private static final AndroidHttpClient ANDROID_HTTP_CLIENT = AndroidHttpClient.newInstance(GeoUtil.class.getName());
    
    private static final String KEY_RESULTS = "results";
    private static final String KEY_FORMATTED_ADDRESS = "formatted_address";
    private static final String KEY_GEOMETRY = "geometry";
    private static final String KEY_LOCATION = "location";
    private static final String KEY_LAT = "lat";
    private static final String KEY_LNG = "lng";
    
	public interface GeoUtilListener {
		public void onLatlngSearchCompleted(String strAddress);
		public void onAddressSearchCompleted(Address address);
	}

	public static void getFromLocation(double lat, double lon, GeoUtilListener listener) {
		new GetAddressFromLatlng(lat, lon, listener).execute();
	}
	
	public static void getFromAddress(String address, GeoUtilListener listener) {
		new GetLatlngFromAddress(address, listener).execute();
	}
	
	private static class GetAddressFromLatlng extends AsyncTask<Void, Void, String> {
		
		private double lat, lon;
		private GeoUtilListener listener;
		
		public GetAddressFromLatlng(double lat, double lon, GeoUtilListener listener) {
			this.lat = lat;
			this.lon = lon;
			this.listener = listener;
		}
		
		@Override
		protected String doInBackground(Void... params) {
			String googleMapUrl = "http://maps.googleapis.com/maps/api/geocode/json?latlng=" + lat + ","
                    + lon + "&sensor=true";

            try {
                JSONObject googleMapResponse = new JSONObject(ANDROID_HTTP_CLIENT.execute(new HttpGet(googleMapUrl),
                        new BasicResponseHandler()));

                // many nested loops.. not great -> use expression instead
                // loop among all results
				JSONArray results = (JSONArray) googleMapResponse.get(KEY_RESULTS);
				for (int i = 0; i < results.length(); i++) {
					
					// loop among all addresses within this result
					JSONObject result = results.getJSONObject(i);
					if (result.has(KEY_FORMATTED_ADDRESS)) {
						return result.getString(KEY_FORMATTED_ADDRESS);
					}
				}
				
			} catch (Exception ignored) {
				ignored.printStackTrace();
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(String result) {
			listener.onLatlngSearchCompleted(result);
		}
	}
	
	private static class GetLatlngFromAddress extends AsyncTask<Void, Void, Address> {
		
		private String strAddress;
		private GeoUtilListener listener;
		
		public GetLatlngFromAddress(String strAddress, GeoUtilListener listener) {
			this.strAddress = strAddress;
			this.listener = listener;
		}

		@Override
		protected Address doInBackground(Void... params) {
            try {
            	String googleMapUrl = "http://maps.googleapis.com/maps/api/geocode/json?address=" 
    					+ URLEncoder.encode(strAddress, AppConstants.CHARSET_NAME) 
    					+ "&sensor=true";
            	
                JSONObject googleMapResponse = new JSONObject(ANDROID_HTTP_CLIENT.execute(new HttpGet(googleMapUrl),
                        new BasicResponseHandler()));

                // many nested loops.. not great -> use expression instead
                // loop among all results
				JSONArray results = (JSONArray) googleMapResponse.get(KEY_RESULTS);
				for (int i = 0; i < results.length(); i++) {
					
					// loop among all addresses within this result
					JSONObject result = results.getJSONObject(i);
					if (result.has(KEY_GEOMETRY)) {
						Address address = new Address(Locale.getDefault());
						JSONObject jObjLocation = result.getJSONObject(KEY_GEOMETRY).getJSONObject(KEY_LOCATION);
						address.setLatitude(jObjLocation.getDouble(KEY_LAT));
						address.setLongitude(jObjLocation.getDouble(KEY_LNG));
						if (result.has(KEY_FORMATTED_ADDRESS)) {
							strAddress = result.getString(KEY_FORMATTED_ADDRESS);
						} 
						address.setAddressLine(0, strAddress);
						return address;
					}
				}
				
			} catch (Exception ignored) {
				ignored.printStackTrace();
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Address address) {
			listener.onAddressSearchCompleted(address);
		}
	}
}
