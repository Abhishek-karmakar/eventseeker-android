package com.wcities.eventseeker.util;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.util.Log;

import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.AppConstants;

public class GeoUtil {
	
	private static final String TAG = GeoUtil.class.getSimpleName();
	
    private static final AndroidHttpClient ANDROID_HTTP_CLIENT = AndroidHttpClient.newInstance(GeoUtil.class.getName());
    
    private static final String KEY_RESULTS = "results";
    private static final String KEY_FORMATTED_ADDRESS = "formatted_address";
    private static final String KEY_GEOMETRY = "geometry";
    private static final String KEY_LOCATION = "location";
    private static final String KEY_LAT = "lat";
    private static final String KEY_LNG = "lng";
    private static final String KEY_ADDRESS_COMPONENTS = "address_components";
    private static final String KEY_TYPES = "types";
    private static final String KEY_LONG_NAME = "long_name";

	private static final String VALUE_LOCALITY = "locality";

	public interface GeoUtilListener {
		public void onAddressSearchCompleted(String strAddress);
		public void onCitySearchCompleted(String city);
		public void onLatlngSearchCompleted(Address address);
	}

	public static void getAddressFromLocation(double lat, double lon, GeoUtilListener listener) {
		new GetAddressFromLatlng(lat, lon, listener).execute();
	}
	
	public static void getFromAddress(String address, GeoUtilListener listener) {
		new GetLatlngFromAddress(address, listener).execute();
	}
	
	public static void getCityFromLocation(double lat, double lon, GeoUtilListener listener) {
		new GetCityFromLatlng(lat, lon, listener).execute();
	}
	
	/**
	 * will give the call back on Background thread. So, do the need full operation on UI thread if necesarry
	 * @param geoUtilListener
	 * @param eventSeekr
	 */
	public static void getCityName(final GeoUtilListener geoUtilListener, final Context activityContext) {
		
		new Thread(new Runnable() {

			@Override
			public void run() {
				String cityName = "";
				
				final double[] latLng = DeviceUtil.getLatLon((EventSeekr) activityContext.getApplicationContext());

				List<Address> addresses = null;
				
				Geocoder geocoder = new Geocoder(activityContext);
				try {
					addresses = geocoder.getFromLocation(latLng[0], latLng[1], 1);

					if (addresses != null && !addresses.isEmpty()) {
						Address address = addresses.get(0);
						cityName = address.getLocality();
						geoUtilListener.onCitySearchCompleted(cityName);
						EventSeekr.setCityName(cityName);
						
					} else {
						Log.d(TAG, "No relevant address found.");
					}

				} catch (IOException e) {
					e.printStackTrace();
				}

				// Alternative way to find lat-lon
				if (addresses == null || addresses.isEmpty() || cityName == null || cityName.equals("")) {
					GeoUtil.getCityFromLocation(latLng[0], latLng[1], geoUtilListener);
				}
			}
		}).start();
	}
	
	private static class GetCityFromLatlng extends AsyncTask<Void, Void, String> {
		
		private double lat, lon;
		private GeoUtilListener listener;
		
		public GetCityFromLatlng(double lat, double lon, GeoUtilListener listener) {
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
					
					if (result.has(KEY_ADDRESS_COMPONENTS)) {
						JSONArray jArrAddressComponents = result.getJSONArray(KEY_ADDRESS_COMPONENTS);
						
						for (int j = 0; j < jArrAddressComponents.length(); j++) {
							JSONObject jObjAddressComponent = jArrAddressComponents.getJSONObject(j);
							
							if (jObjAddressComponent.has(KEY_TYPES)) {
								JSONArray jArrTypes = jObjAddressComponent.getJSONArray(KEY_TYPES);
								
								if (jArrTypes.length() > 0 && jArrTypes.getString(0).equals(VALUE_LOCALITY)) {
									//Log.d(TAG, "got city");
									return jObjAddressComponent.getString(KEY_LONG_NAME);
								}
							}
						}
					}
				}
				
			} catch (Exception ignored) {
				ignored.printStackTrace();
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(String result) {
			listener.onCitySearchCompleted(result);
			EventSeekr.setCityName(result);
		}
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
			listener.onAddressSearchCompleted(result);
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
			listener.onLatlngSearchCompleted(address);
		}
	}
}
