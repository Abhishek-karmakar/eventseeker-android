package com.wcities.eventseeker.jsonparser;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.wcities.eventseeker.core.Address;
import com.wcities.eventseeker.core.Country;
import com.wcities.eventseeker.core.Venue;

public class RecordApiJSONParser {

	private static final String TAG = "RecordApiJSONParser";
	
	private static final String KEY_RECORDS = "records";
	private static final String KEY_RECORD = "record";
	private static final String KEY_DETAILS = "details";
	private static final String KEY_ID = "id";
	private static final String KEY_NAME = "name";
	private static final String KEY_IMAGEFILE = "imagefile";
	private static final String KEY_ADDRESS = "address";
	private static final String KEY_ADDRESS1 = "address1";
	private static final String KEY_ADDRESS2 = "address2";
	private static final String KEY_CITY = "city";
	private static final String KEY_COUNTRY = "country";
	private static final String KEY_LONG_DESC = "long_desc";
	private static final String KEY_LATITUDE = "latitude";
	private static final String KEY_LONGITUDE = "longitude";
	private static final String KEY_PHONE = "phone";
	private static final String KEY_URL = "url";

	private static final Object JSON_NULL = "null";
	
	public List<Venue> getVenueList(JSONObject jsonObject) {
		List<Venue> venues = new ArrayList<Venue>();
		
		try {
			JSONObject jObjRecords = jsonObject.getJSONObject(KEY_RECORDS);
			if (jObjRecords.has(KEY_RECORD)) {
				Object jsonRecord = jObjRecords.get(KEY_RECORD);

				if (jsonRecord instanceof JSONArray) {
					JSONArray jArrRecords = (JSONArray) jsonRecord;
					for (int i = 0; i < jArrRecords.length(); i++) {
						Venue venue = getVenue(jArrRecords.getJSONObject(i).getJSONObject(KEY_DETAILS), null);
						venues.add(venue);
					}
					
				} else {
					Venue venue = getVenue(((JSONObject) jsonRecord).getJSONObject(KEY_DETAILS), null);
					venues.add(venue);
				}
			}
			
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return venues;
	}
	
	public void fillVenueDetails(JSONObject jsonObject, Venue venue) throws JSONException {
		JSONObject jObjRecords = jsonObject.getJSONObject(KEY_RECORDS);
		if (jObjRecords.has(KEY_RECORD)) {
			Object jsonRecord = jObjRecords.get(KEY_RECORD);
			getVenue(((JSONObject) jsonRecord).getJSONObject(KEY_DETAILS), venue);
		}
	}
	
	private Venue getVenue(JSONObject jsonObject, Venue venue) throws JSONException {
		if (venue == null) {
			venue = new Venue(jsonObject.getInt(KEY_ID));
			venue.setName(jsonObject.getString(KEY_NAME));
		}
		if (jsonObject.has(KEY_LONG_DESC)) {
			venue.setLongDesc(jsonObject.getString(KEY_LONG_DESC));
		}
		
		String imagefile = jsonObject.getString(KEY_IMAGEFILE);
		if (!imagefile.startsWith("http:")) {
			venue.setImagefile(imagefile);
			
		} else {
			venue.setImageUrl(imagefile);
		}
		
		venue.setAddress(getAddress(jsonObject.getJSONObject(KEY_ADDRESS)));
		if (jsonObject.has(KEY_PHONE)) {
			venue.setPhone(jsonObject.getString(KEY_PHONE));
		}
		if (jsonObject.has(KEY_URL)) {
			String url = jsonObject.getString(KEY_URL);
			if (url != null && url.equals(JSON_NULL)) {
				url = null;
			}
			venue.setUrl(url);
		}
		
		return venue;
	}
	
	private Address getAddress(JSONObject jsonObject) throws JSONException {
		Address address = new Address();
		address.setAddress1(jsonObject.getString(KEY_ADDRESS1));
		if (jsonObject.has(KEY_ADDRESS2)) {
			address.setAddress2(jsonObject.getString(KEY_ADDRESS2));
		}
		address.setCity(jsonObject.getString(KEY_CITY));
		address.setCountry(getCountry(jsonObject.getJSONObject(KEY_COUNTRY)));
		if (jsonObject.has(KEY_LATITUDE)) {
			String strLat = jsonObject.getString(KEY_LATITUDE);
			String strLon = jsonObject.getString(KEY_LONGITUDE);
			if (!strLat.isEmpty()) {
				address.setLat(Double.parseDouble(strLat));
			}
			if (!strLon.isEmpty()) {
				address.setLon(Double.parseDouble(strLon));
			}
		}
		return address;
	}
	
	private Country getCountry(JSONObject jsonObject) throws JSONException {
		Country country = new Country();
		country.setName(jsonObject.getString(KEY_NAME));
		return country;
	}
}
