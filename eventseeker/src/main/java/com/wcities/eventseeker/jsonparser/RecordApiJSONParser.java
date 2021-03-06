package com.wcities.eventseeker.jsonparser;

import com.wcities.eventseeker.core.Address;
import com.wcities.eventseeker.core.Country;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.util.ConversionUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RecordApiJSONParser {

	private static final String TAG = RecordApiJSONParser.class.getName();
	
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
	private static final String KEY_STATE = "state";
	private static final String KEY_COUNTRY = "country";
	private static final String KEY_LONG_DESC = "long_desc";
	private static final String KEY_LATITUDE = "latitude";
	private static final String KEY_LONGITUDE = "longitude";
	private static final String KEY_PHONE = "phone";
	private static final String KEY_URL = "url";
    private static final String KEY_LINKS = "links";
    private static final String KEY_SOCIAL_URL = "social_url";

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
						Venue venue = getVenue(jArrRecords.getJSONObject(i), null);
						venues.add(venue);
					}
					
				} else {
					Venue venue = getVenue((JSONObject) jsonRecord, null);
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
			getVenue(jObjRecords.getJSONObject(KEY_RECORD), venue);
		}
	}
	
	private Venue getVenue(JSONObject jObjRecord, Venue venue) throws JSONException {
        JSONObject jObjDetails = jObjRecord.getJSONObject(KEY_DETAILS);
		if (venue == null) {
			venue = new Venue(jObjDetails.getInt(KEY_ID));
			venue.setName(ConversionUtil.decodeHtmlEntities(jObjDetails, KEY_NAME));
		}
		if (jObjDetails.has(KEY_LONG_DESC)) {
			venue.setLongDesc(ConversionUtil.removeBuggyTextsFromDesc(ConversionUtil.decodeHtmlEntities(
					jObjDetails, KEY_LONG_DESC)));
		}
		
		String imagefile = jObjDetails.getString(KEY_IMAGEFILE);
		if (!imagefile.startsWith("http:")) {
			venue.setImagefile(imagefile);
			
		} else {
			venue.setImageUrl(imagefile);
		}
		
		venue.setAddress(getAddress(jObjDetails.getJSONObject(KEY_ADDRESS)));
		if (jObjDetails.has(KEY_PHONE)) {
			Object jPhone = jObjDetails.get(KEY_PHONE);
			if (jPhone instanceof JSONArray) {
				//Log.d(TAG, "array");
				String phone = ((JSONArray) jPhone).getString(0);
				phone = ConversionUtil.parseForPhone(phone);
				venue.setPhone(phone);
				
			} else {
				//Log.d(TAG, "not array");
				String phone = (String) jPhone;
				phone = ConversionUtil.parseForPhone(phone);
				venue.setPhone(phone);
			}
		}
		if (jObjDetails.has(KEY_URL)) {
			String url = jObjDetails.getString(KEY_URL);
			if (url != null && url.equals(JSON_NULL)) {
				url = null;
			}
			venue.setUrl(url);
		}

        if (jObjRecord.has(KEY_LINKS)) {
            JSONObject jObjLinks = jObjRecord.getJSONObject(KEY_LINKS);

            if (jObjLinks.has(KEY_SOCIAL_URL)) {
                Object objSocialUrl = jObjLinks.get(KEY_SOCIAL_URL);

                if (objSocialUrl instanceof JSONArray) {
                    JSONArray jArrSocialUrl = (JSONArray) objSocialUrl;

                    for (int i = 0; i < jArrSocialUrl.length(); i++) {
                        String link = jArrSocialUrl.getString(i);
                        if (link.contains("//www.facebook.com/")) {
                            venue.setFbLink(link);
                            break;
                        }
                    }

                } else if (((String) objSocialUrl).contains("//www.facebook.com/")) {
                    venue.setFbLink((String) objSocialUrl);
                }
            }
        }
		
		return venue;
	}
	
	private Address getAddress(JSONObject jsonObject) throws JSONException {
		Address address = new Address();
		address.setAddress1(ConversionUtil.decodeHtmlEntities(jsonObject, KEY_ADDRESS1));
		if (jsonObject.has(KEY_ADDRESS2)) {
			address.setAddress2(ConversionUtil.decodeHtmlEntities(jsonObject, KEY_ADDRESS2));
		}
		address.setCity(ConversionUtil.decodeHtmlEntities(jsonObject, KEY_CITY));
		if (jsonObject.has(KEY_STATE)) {
			address.setState(ConversionUtil.decodeHtmlEntities(jsonObject, KEY_STATE));
		}
		address.setCountry(getCountry(jsonObject.getJSONObject(KEY_COUNTRY)));
		if (jsonObject.has(KEY_LATITUDE)) {
			String strLat = jsonObject.getString(KEY_LATITUDE);
			String strLon = jsonObject.getString(KEY_LONGITUDE);
			if (strLat.length() != 0) {
				address.setLat(Double.parseDouble(strLat));
			}
			if (strLon.length() != 0) {
				address.setLon(Double.parseDouble(strLon));
			}
		}
		return address;
	}
	
	private Country getCountry(JSONObject jsonObject) throws JSONException {
		Country country = new Country();
		country.setName(ConversionUtil.decodeHtmlEntities(jsonObject, KEY_NAME));
		return country;
	}
}
