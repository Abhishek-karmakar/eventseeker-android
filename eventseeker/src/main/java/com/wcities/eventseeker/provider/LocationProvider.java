package com.wcities.eventseeker.provider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.jsonparser.LocationJSONParser;

public class LocationProvider extends ContentProvider {

	private static final String TAG = LocationProvider.class.getSimpleName();
			
	private static final String AUTHORITY = "com.wcities.eventseeker.provider.LocationProvider";
	
	private static final int SUGGESTIONS = 1;
	
	// Defines a set of uris allowed with this content provider
	private static final UriMatcher mUriMatcher = buildUriMatcher();
	
	private static UriMatcher buildUriMatcher() {
		UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		
		// URI for suggestions in Search Dialog
        uriMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SUGGESTIONS);
        
		return uriMatcher;
	}
	
	@Override
	public boolean onCreate() {
		// TODO Auto-generated method stub
		//Log.d(TAG, "onCreate()");
		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		//Log.d(TAG, "query()");
		LocationJSONParser parser = new LocationJSONParser();
		
		String jsonString = "";
		
		List<HashMap<String, String>> list = null;
		
		MatrixCursor mCursor = null;
		
		switch (mUriMatcher.match(uri)) {
		
		case SUGGESTIONS :	
			// Defining a cursor object with column's id, SUGGEST_COLUMN_TEXT_1, SUGGEST_COLUMN_INTENT_EXTRA_DATA
			mCursor = new MatrixCursor(new String[] {"_id", SearchManager.SUGGEST_COLUMN_TEXT_1, SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA});
			// Creating a parser object to parse locs in JSON format
			parser = new LocationJSONParser();
			// Get Locs from Google Places API
			jsonString = getLocations(selectionArgs);		
			
			try {
				// Parse the locs ( JSON => List )
				list  = parser.parseLocs(new JSONObject(jsonString));
				
				// Creating cursor object with locs
				for (int i = 0; i < list.size(); i++) {
					HashMap<String, String> hMap = (HashMap<String, String>) list.get(i);
					// Adding loc details to cursor
					/**
					 * passing 3rd item as description itself instead of reference since we want to access the 
					 * description value from activity via EXTRA_DATA_KEY
					 */
					mCursor.addRow(new String[] {Integer.toString(i), hMap.get("description"), hMap.get("description")});
					//mCursor.addRow(new String[] {Integer.toString(i), hMap.get("description"), hMap.get("reference")});				
				}				
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			break;
		}		
		
		return mCursor;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	private String getLocations(String[] params) {
    	//Log.d(TAG, "getLocations()");
    	// For storing data from web service
        String data = "";
        String url = getLocationsUrl(params[0]);
        try {
            // Fetching the data from web service in background
            data = downloadUrl(url);
            
        } catch (Exception e) {
        	Log.d("Background Task", e.toString());
        }
        return data;    	
    }
	
	private String getLocationsUrl(String qry) {   	
    	//Log.d(TAG, "getLocationsUrl(), qry = " + qry);
        try {
            qry = "input=" + URLEncoder.encode(qry, AppConstants.CHARSET_NAME);
            
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }

        // Sensor enabled
        String sensor = "sensor=false";
        // place type to be searched
        String types = "types=(cities)";
        // Building the parameters to the web service
        String parameters = qry + "&" + types + "&" + sensor + "&key=" + AppConstants.BROWSER_KEY_FOR_PLACES_API;
        // Output format
        String output = "json";    
        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/place/autocomplete/" + output + "?" + parameters;        
        
        return url;
    }
	
	/** A method to download json data from url */
    private String downloadUrl(String strUrl) throws IOException {
    	//Log.d(TAG, "downloadUrl(), url = " + strUrl);
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        
        try {
            URL url = new URL(strUrl);
            // Creating an http connection to communicate with url 
            urlConnection = (HttpURLConnection) url.openConnection();
            // Connecting to url 
            urlConnection.connect();

            // Reading data from url 
            iStream = urlConnection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
            StringBuffer sb  = new StringBuffer();
            String line = "";
            while ((line = br.readLine()) != null) {
            	sb.append(line);
            }

            data = sb.toString();
            br.close();

        } catch (Exception e) {
            Log.d("Exception while downloading url", e.toString());
                
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }
}
