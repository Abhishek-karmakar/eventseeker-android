package com.wcities.eventseeker.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.UUID;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.bosch.myspin.serversdk.MySpinServerSDK;
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
	private static long lastLatLngSetTime;

	private static LocationManager locationManager;
	
	private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 60; // 1 hour
	private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters

	/**
	 * Generates new values for first time if never generated until now after app start, otherwise 
	 * returns existing latitude, longitude values.
	 * @param context
	 * @return
	 */
	public static double[] getLatLon(EventSeekr eventSeekr) {
		//Log.d(TAG, "getLatLon()");
		if (MySpinServerSDK.sharedInstance().isConnected() && !AppConstants.IS_CAR_STATIONARY) {
			return getCurrentLatLonForBosch(eventSeekr);
		}
		
		double[] latLon = new double[] {0, 0};
		
		final LocationManager locationManager = getLocationManagerInstance(eventSeekr);

		// getting GPS status
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // getting network status
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        
    	if (AppConstants.lat == AppConstants.NOT_ALLOWED_LAT || AppConstants.lon == AppConstants.NOT_ALLOWED_LON 
    			|| retryGenerating) {
    		//Log.d(TAG, "isGPSEnabled : " + isGPSEnabled); 
    		//Log.d(TAG, "isNetworkEnabled : " + isNetworkEnabled); 

            if (!isGPSEnabled && !isNetworkEnabled) {
                // no gps or network provider is enabled then
				new FindLatLonFromApi(eventSeekr).execute();
				
            } else {
            	Location lastKnownLocation = null;
            	
            	if (isGPSEnabled) {
                 	//First get the location from GPS Provider
            		//requestLocationUpdatesOnUiThread(activityContext, locationManager, LocationManager.GPS_PROVIDER);

 					if (locationManager != null) {
 						lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
 						//Log.d(TAG, "GPS_PROVIDER: " + lastKnownLocation); 

 						if (lastKnownLocation != null) {
 							//Log.d(TAG, "GPS_PROVIDER: " + lastKnownLocation.getLatitude() + ", " + lastKnownLocation.getLongitude());
                         	latLon[0] = lastKnownLocation.getLatitude();
         		        	latLon[1] = lastKnownLocation.getLongitude();
         		        	updateLatLon(latLon[0], latLon[1]);
         		        	updateCurLatLon(eventSeekr, latLon[0], latLon[1]);
 						} 
 					}
                } 
            	
            	if (isNetworkEnabled && lastKnownLocation == null) {
                	//get location from Network Provider
            		//requestLocationUpdatesOnUiThread(activityContext, locationManager, LocationManager.NETWORK_PROVIDER);
	
	                if (locationManager != null) {
	                	lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
	                	//Log.d(TAG, "NETWORK_PROVIDER: " + lastKnownLocation); 
	                        
	                	if (lastKnownLocation != null) {
	                		//Log.d(TAG, "NETWORK_PROVIDER: " + lastKnownLocation.getLatitude() + ", " + lastKnownLocation.getLongitude());
	                        latLon[0] = lastKnownLocation.getLatitude();
	        		        latLon[1] = lastKnownLocation.getLongitude();
	        		        updateLatLon(latLon[0], latLon[1]);
	        		        updateCurLatLon(eventSeekr, latLon[0], latLon[1]);
	                    }
	                }
				}
            	
            	if (lastKnownLocation == null) {
					new FindLatLonFromApi(eventSeekr).execute();
				}
            }
			
    	} else {
    		latLon[0] = AppConstants.lat;
    		latLon[1] = AppConstants.lon;
    		
    		/**
    		 * If user has not set any city & time elapsed from last location update is more than MIN_TIME_BW_UPDATES, 
    		 * then again start listening for updates.
    		 * 
    		 * Without this code we won't get any updated location in following situation:
    		 * If user leaves app by pressing back button, we unregister location update listeners while finishing 
    		 * MainActivity. Now if we have already got some valid values for lat & lon in AppConstants.lat & 
    		 * AppConstants.lon, then unless app process restarts we won't be able to register location listener from 
    		 * above if condition.
    		 */
    		/*if (!isCitySet && lastLatLngSetTime + MIN_TIME_BW_UPDATES < new Date().getTime()) {
    			if (isGPSEnabled) {
            		requestLocationUpdatesOnUiThread(activityContext, locationManager, LocationManager.GPS_PROVIDER);
 					
    			} else if (isNetworkEnabled) {
            		requestLocationUpdatesOnUiThread(activityContext, locationManager, LocationManager.NETWORK_PROVIDER);
    			}
    		}*/
    	}

    	if (latLon[0] == 0 && latLon[1] == 0) {
    		//Log.d(TAG, "latlon is 0");
	    	latLon[0] = SAN_FRANCISCO_LAT;
			latLon[1] = SAN_FRANCISCO_LON;
			retryGenerating = true;
    	}
    	
    	return latLon;
    }
	
	public static double[] getCurrentLatLon(EventSeekr eventSeekr) {
		//Log.d(TAG, "getLatLon()");
		if (MySpinServerSDK.sharedInstance().isConnected() && !AppConstants.IS_CAR_STATIONARY) {
			return getCurrentLatLonForBosch(eventSeekr);
		}
		
		double[] latLon = new double[] {0, 0};
		
		LocationManager locationManager = getLocationManagerInstance(eventSeekr);

		// getting GPS status
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // getting network status
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        
    	if (eventSeekr.getCurLat() == AppConstants.NOT_ALLOWED_LAT || eventSeekr.getCurLon() == 
    			AppConstants.NOT_ALLOWED_LON) {

            if (!isGPSEnabled && !isNetworkEnabled) {
                // no gps or network provider is enabled then
				new FindLatLonFromApi(eventSeekr).execute();
				
            } else {
            	Location lastKnownLocation = null;
            	
            	if (isGPSEnabled) {
                 	//First get the location from GPS Provider

 					if (locationManager != null) {
 						lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

 						if (lastKnownLocation != null) {
                         	latLon[0] = lastKnownLocation.getLatitude();
         		        	latLon[1] = lastKnownLocation.getLongitude();
         		        	updateCurLatLon(eventSeekr, latLon[0], latLon[1]);
 						} 
 					}
                } 
            	
            	if (isNetworkEnabled && lastKnownLocation == null) {
                	//get location from Network Provider
	
	                if (locationManager != null) {
	                	lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
	                        
	                	if (lastKnownLocation != null) {
	                        latLon[0] = lastKnownLocation.getLatitude();
	        		        latLon[1] = lastKnownLocation.getLongitude();
	        		        updateCurLatLon(eventSeekr, latLon[0], latLon[1]);
	                    }
	                }
				}
            	
            	if (lastKnownLocation == null) {
					new FindLatLonFromApi(eventSeekr).execute();
				}
            }
			
    	} else {
    		latLon[0] = eventSeekr.getCurLat();
    		latLon[1] = eventSeekr.getCurLon();
    	}

    	if (latLon[0] == 0 && latLon[1] == 0) {
	    	latLon[0] = SAN_FRANCISCO_LAT;
			latLon[1] = SAN_FRANCISCO_LON;
    	}
    	
    	return latLon;
    }
	
	private static double[] getCurrentLatLonForBosch(EventSeekr eventSeekr) {
		//Log.d(TAG, "getLatLon()");
		double[] latLon = new double[] {0, 0};
		
		LocationManager locationManager = getLocationManagerInstance(eventSeekr);

		// getting GPS status
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // getting network status
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        
        if (isGPSEnabled || isNetworkEnabled) {
        	Location lastKnownLocation = null;

        	if (isGPSEnabled) {
        		lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

				if (lastKnownLocation != null) {
                 	latLon[0] = lastKnownLocation.getLatitude();
 		        	latLon[1] = lastKnownLocation.getLongitude();
 		        	updateCurLatLon(eventSeekr, latLon[0], latLon[1]);
 		        	/**
 		        	 * Update AppConstants.lat & lon values also, so that we get 
 		        	 * the same effective lat-lon values when bosch car stops.
 		        	 */
 		        	updateLatLon(latLon[0], latLon[1]);
				} 
            } 
        	
        	if (isNetworkEnabled && lastKnownLocation == null) {
            	//get location from Network Provider

                if (locationManager != null) {
                	lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        
                	if (lastKnownLocation != null) {
                        latLon[0] = lastKnownLocation.getLatitude();
        		        latLon[1] = lastKnownLocation.getLongitude();
        		        updateCurLatLon(eventSeekr, latLon[0], latLon[1]);
        		        /**
     		        	 * Update AppConstants.lat & lon values also, so that we get 
     		        	 * the same effective lat-lon values when bosch car stops.
     		        	 */
        		        updateLatLon(latLon[0], latLon[1]);
                    }
                }
			}
        } 

    	if (latLon[0] == 0 && latLon[1] == 0) {
    		if (eventSeekr.getCurLat() != AppConstants.NOT_ALLOWED_LAT 
        			&& eventSeekr.getCurLon() != AppConstants.NOT_ALLOWED_LON) {
        		latLon[0] = eventSeekr.getCurLat();
        		latLon[1] = eventSeekr.getCurLon();
        		
        	} else if (AppConstants.lat != AppConstants.NOT_ALLOWED_LAT 
        			&& AppConstants.lon != AppConstants.NOT_ALLOWED_LON) {
				latLon[0] = AppConstants.lat;
        		latLon[1] = AppConstants.lon;
				
			} else {
		    	latLon[0] = SAN_FRANCISCO_LAT;
				latLon[1] = SAN_FRANCISCO_LON;
        	}
    	}
    	
    	return latLon;
    }
	
	private static void requestLocationUpdatesOnUiThread(final Context activityContext, 
			final LocationManager locationManager, final String provider) {
		//Log.d(TAG, "requestLocationUpdatesOnUiThread(), provider = " + provider);
		if (Looper.myLooper() == Looper.getMainLooper()) {
			// if it's UI thread
			locationManager.requestLocationUpdates(provider, MIN_TIME_BW_UPDATES, 
					MIN_DISTANCE_CHANGE_FOR_UPDATES, DeviceLocationListener.getInstance(
							(EventSeekr) activityContext.getApplicationContext()));
			
		} else if (activityContext instanceof Activity) {
			((Activity)activityContext).runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					locationManager.requestLocationUpdates(provider, MIN_TIME_BW_UPDATES, 
							MIN_DISTANCE_CHANGE_FOR_UPDATES, DeviceLocationListener.getInstance(
									(EventSeekr) activityContext.getApplicationContext()));
				}
			});
		} 
	}
		
	private static LocationManager getLocationManagerInstance(EventSeekr eventSeekr) {
		if (locationManager == null) {
			locationManager = (LocationManager) eventSeekr.getSystemService(Context.LOCATION_SERVICE);
		}
		return locationManager;
	}
	
	/**
	 * This will remove the DeviceLocationListener instance from the DeviceUtil's Location Manager instance and 
	 * will disable all the location updates.
	 */
	public static void unregisterLocationListener(EventSeekr eventSeekr) {
		//Log.i(TAG, "DeviceLocationListener is has been removed");
		if (locationManager != null) {
			locationManager.removeUpdates(DeviceLocationListener.getInstance(eventSeekr));
		}
	}
	
	public static void registerLocationListener(Context context) {
		LocationManager locationManager = getLocationManagerInstance((EventSeekr) context.getApplicationContext());

		// getting GPS status
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // getting network status
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        
        if (isGPSEnabled) {
        	requestLocationUpdatesOnUiThread(context, locationManager, LocationManager.GPS_PROVIDER);
        }
        
        if (isNetworkEnabled) {
        	requestLocationUpdatesOnUiThread(context, locationManager, LocationManager.NETWORK_PROVIDER);
        }
	}

	private static class DeviceLocationListener implements LocationListener {

		private static DeviceLocationListener deviceLocationListener;
		private EventSeekr eventSeekr;
		
		public DeviceLocationListener(EventSeekr eventSeekr) {
			this.eventSeekr = eventSeekr;
		}

		public static DeviceLocationListener getInstance(EventSeekr eventSeekr) {
			if (deviceLocationListener == null) {
				deviceLocationListener = new DeviceLocationListener(eventSeekr);
			}
			return deviceLocationListener;
		}
		
		public void onLocationChanged(Location location) {
			if (retryGenerating) {
				updateLatLon(location.getLatitude(), location.getLongitude());
			}
        	updateCurLatLon(eventSeekr, location.getLatitude(), location.getLongitude());
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
		lastLatLngSetTime = new Date().getTime();
	}
	
	public static void updateCurLatLon(EventSeekr eventSeekr, double curLat, double curLon) {
		eventSeekr.setCurLat(curLat);
		eventSeekr.setCurLon(curLon);
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
		
		private EventSeekr eventSeekr;

		public FindLatLonFromApi(EventSeekr eventSeekr) {
			this.eventSeekr = eventSeekr;
		}

		@Override
		protected Void doInBackground(Void... params) {
			double[] latLon = new double[] {0, 0};
			
			IPToCityApi ipToCityApi = new IPToCityApi(Api.OAUTH_TOKEN);
			
			try {
				JSONObject jsonObject = ipToCityApi.findLatLon();
				IPToCityApiJSONParser jsonParser = new IPToCityApiJSONParser();
				latLon = jsonParser.getLatlon(jsonObject);
				if (latLon[0] != 0 || latLon[1] != 0) {
					if (retryGenerating) {
						updateLatLon(latLon[0], latLon[1]);
					}
					updateCurLatLon(eventSeekr, latLon[0], latLon[1]);
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
