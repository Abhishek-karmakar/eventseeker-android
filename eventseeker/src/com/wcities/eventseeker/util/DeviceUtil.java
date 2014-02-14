package com.wcities.eventseeker.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.IPToCityApi;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.SharedPrefKeys;
import com.wcities.eventseeker.jsonparser.IPToCityApiJSONParser;

public class DeviceUtil {

	private static final String TAG = DeviceUtil.class.getName();
	
	private static final double SAN_FRANCISCO_LAT = 37.7749295;
	private static final double SAN_FRANCISCO_LON = -122.4194155;
	
	private static boolean retryGenerating;

	private static LocationManager locationManager;
	
	private static final long MIN_TIME_BW_UPDATES = 1000 * 60; // 1 minutes
	private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // 1 meters

	/**
	 * Generates new values for first time if never generated until now after app start, otherwise 
	 * returns existing latitude, longitude values.
	 * @param context
	 * @return
	 */
	public static double[] getLatLon(Context context) {
		//Log.d(TAG, "getLatLon");
		double[] latLon = new double[] {0, 0};
		
    	if (AppConstants.lat == AppConstants.NOT_ALLOWED_LAT || AppConstants.lon == AppConstants.NOT_ALLOWED_LON 
    			|| retryGenerating) {
    		//Log.d(TAG, "generate");
    		LocationManager locationManager = getLocationManagerInstance(context);
	    	
    		//DeviceLocationListener.initialize(context);
    		
    		// getting GPS status
            boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            // getting network status
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

           //Log.d(TAG, "isGPSEnabled : " + isGPSEnabled); 
           //Log.d(TAG, "isNetworkEnabled : " + isNetworkEnabled); 

            if (!isGPSEnabled && !isNetworkEnabled) {
                // no gps or network provider is enabled then
				new FindLatLonFromApi().execute();
				
            } else {
            	Location lastKnownLocation = null;
            	
            	if (isGPSEnabled) {
                 	//First get the location from GPS Provider
 					locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES,
 							MIN_DISTANCE_CHANGE_FOR_UPDATES, DeviceLocationListener.getInstance());

 					if (locationManager != null) {
 						lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
 						//Log.d(TAG, "GPS_PROVIDER: " + lastKnownLocation); 

 						if (lastKnownLocation != null) {
 							//Log.d(TAG, "GPS_PROVIDER: " + lastKnownLocation.getLatitude() + ", " 
 							//		+ lastKnownLocation.getLongitude());
 		      	        	
                         	AppConstants.lat = latLon[0] = lastKnownLocation.getLatitude();
         		        	AppConstants.lon = latLon[1] = lastKnownLocation.getLongitude();
         		        	retryGenerating = false;
 						} 
 					}
                } 
            	
            	if (isNetworkEnabled && lastKnownLocation == null) {
                	//get location from Network Provider
					locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES, 
							MIN_DISTANCE_CHANGE_FOR_UPDATES, DeviceLocationListener.getInstance());
	
	                if (locationManager != null) {
	                	lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
	                	//Log.d(TAG, "NETWORK_PROVIDER: " + lastKnownLocation); 
	                        
	                	if (lastKnownLocation != null) {
	                		//Log.d(TAG, "NETWORK_PROVIDER: " + lastKnownLocation.getLatitude() + ", " 
	                		//		+ lastKnownLocation.getLongitude());
	            	        	
	                        AppConstants.lat = latLon[0] = lastKnownLocation.getLatitude();
	        		        AppConstants.lon = latLon[1] = lastKnownLocation.getLongitude();
	        		        retryGenerating = false;
	                    }
	                }
	                
				}
            	
            	if (lastKnownLocation == null) {
					new FindLatLonFromApi().execute();
				}
            }
			
    	} else {
    		latLon[0] = AppConstants.lat;
    		latLon[1] = AppConstants.lon;
    	}
    	
    	/*latLon[0] = AppConstants.lat = 19.1871777;
		latLon[1] = AppConstants.lon = 72.8339689;*/
    	if (latLon[0] == 0 && latLon[1] == 0) {
    		//Log.d(TAG, "latlon is 0");
	    	latLon[0] = SAN_FRANCISCO_LAT;
			latLon[1] = SAN_FRANCISCO_LON;
			retryGenerating = true;
    	}
    	return latLon;
    }
		
	private static LocationManager getLocationManagerInstance(Context context) {
		if (locationManager == null) {
			locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		}
		return locationManager;
	}
	
	/**
	 * This will remove the DeviceLocationListener instance from the DeviceUtil's Location Manager instance and 
	 * will disable all the location updates.
	 */
	public static void removeDeviceLocationListener() {
		Log.i(TAG, "DeviceLocationListener is has been removed");
		if (locationManager != null) {
			locationManager.removeUpdates(DeviceLocationListener.getInstance());
		}
	}

	private static class DeviceLocationListener implements LocationListener {

		private static DeviceLocationListener deviceLocationListener;
		//private static Context context;
		
		private DeviceLocationListener() {}
		
		/*private static void initialize(Context context) {
			DeviceLocationListener.context = context;
		}*/
		
		public static DeviceLocationListener getInstance() {
			if (deviceLocationListener == null) {
				deviceLocationListener = new DeviceLocationListener();
			}
			return deviceLocationListener;
		}
		
		public void onLocationChanged(Location location) {
        	AppConstants.lat = location.getLatitude();
        	AppConstants.lon = location.getLongitude();
        	retryGenerating = false;
        	
			Log.i(TAG, "Current Location Changed to " + location.getLatitude() + ", " + location.getLongitude());
			//Toast.makeText(context, "Current Location Changed to " + location.getLatitude() + ", " 
			//		+ location.getLongitude(), Toast.LENGTH_LONG).show();
		}

		public void onStatusChanged(String s, int i, Bundle b) {}

		public void onProviderDisabled(String s) {
			//Log.i(TAG, s + " Provider has been Disabled");
		}

		public void onProviderEnabled(String s) {
			//Log.i(TAG, s + " Provider has been Enabled");
		}
		
	}
	
	public static void updateLatLon(double lat, double lon) {
		AppConstants.lat = lat;
		AppConstants.lon = lon;
		retryGenerating = false;
	}
	
	/**
	 * Returns a unique UUID for the current android device. As with all UUIDs,
	 * this unique ID is "very highly likely" to be unique across all Android
	 * devices. Much more so than ANDROID_ID is.
	 * 
	 * The UUID is generated by using ANDROID_ID as the base key if appropriate,
	 * falling back on TelephonyManager.getDeviceID() if ANDROID_ID is known to
	 * be incorrect, and finally falling back on a random UUID that's persisted
	 * to SharedPreferences if getDeviceID() does not return a usable value.
	 * 
	 * In some rare circumstances, this ID may change. In particular, if the
	 * device is factory reset a new device ID may be generated. In addition, if
	 * a user upgrades their phone from certain buggy implementations of Android
	 * 2.2 to a newer, non-buggy version of Android, the device ID may change.
	 * Or, if a user uninstalls your app on a device that has neither a proper
	 * Android ID nor a Device ID, this ID may change on reinstallation.
	 * 
	 * Note that if the code falls back on using TelephonyManager.getDeviceId(),
	 * the resulting ID will NOT change after a factory reset. Something to be
	 * aware of.
	 * 
	 * Works around a bug in Android 2.2 for many devices when using ANDROID_ID
	 * directly.
	 * 
	 * @see http://code.google.com/p/android/issues/detail?id=10603
	 * 
	 * @return an id that may be used to uniquely identify your device for most
	 *         purposes.
	 */
	public static String getDeviceId(EventSeekr eventSeekr) {
		UUID uuid;
		SharedPreferences pref = eventSeekr.getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
		String id = pref.getString(SharedPrefKeys.DEVICE_ID, null);

		if (id != null) {
			// Use the id previously computed and stored in the prefs file
			uuid = UUID.fromString(id);

		} else {

			String androidId = Secure.getString(eventSeekr.getContentResolver(), Secure.ANDROID_ID);

			// Use the Android ID unless it's broken, in which case
			// fallback on deviceId,
			// unless it's not available, then fallback on a random
			// number which we store
			// to a prefs file
			try {
				if ((!"9774d56d682e549c".equals(androidId)) && (androidId != null)) {
					uuid = UUID.nameUUIDFromBytes(androidId.getBytes("utf8"));
					
				} else {
					String deviceId = ((TelephonyManager) eventSeekr.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
					uuid = deviceId != null ? UUID.nameUUIDFromBytes(deviceId.getBytes("utf8")) : UUID.randomUUID();
				}
				
			} catch (final UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}

			// Write the value out to the prefs file
			pref.edit().putString(SharedPrefKeys.DEVICE_ID, uuid.toString()).commit();

		}
		String deviceId = uuid.toString();
		return deviceId;
	}
	
	private static class FindLatLonFromApi extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			double[] latLon = new double[] {0, 0};
			
			IPToCityApi ipToCityApi = new IPToCityApi(Api.OAUTH_TOKEN);
			
			try {
				JSONObject jsonObject = ipToCityApi.findLatLon();
				IPToCityApiJSONParser jsonParser = new IPToCityApiJSONParser();
				latLon = jsonParser.getLatlon(jsonObject);
				if (latLon[0] != 0 || latLon[1] != 0) {
					AppConstants.lat = latLon[0];
		        	AppConstants.lon = latLon[1];
		        	retryGenerating = false;
				}
	        	
	        	//Log.d(TAG, "lat = " + AppConstants.lat + ", lon = " + AppConstants.lon);
				
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				
			} catch (IOException e) {
				e.printStackTrace();
				
			} catch (JSONException e) {
				e.printStackTrace();
			}

			return null;
		}
	}
}
