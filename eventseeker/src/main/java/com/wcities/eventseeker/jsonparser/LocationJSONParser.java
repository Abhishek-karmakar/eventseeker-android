package com.wcities.eventseeker.jsonparser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LocationJSONParser {
	
	/** Receives a JSONObject and returns a list */
	public List<HashMap<String,String>> parseLocs(JSONObject jObject) {		
		JSONArray jLocs = null;
		try {			
			/** Retrieves all the elements in the 'locs' array */
			jLocs = jObject.getJSONArray("predictions");
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		/** Invoking getLocs with the array of json object
		 * where each json object represent a location
		 */
		return getLocs(jLocs);
	}
	
	
	private List<HashMap<String, String>> getLocs(JSONArray jLocs) {
		int locsCount = jLocs.length();
		List<HashMap<String, String>> locsList = new ArrayList<HashMap<String,String>>();
		HashMap<String, String> loc = null;	

		/** Taking each location, parses and adds to list object */
		for (int i = 0; i < locsCount; i++) {
			try {
				/** Call getLoc with location JSON object to parse the location */
				loc = getLoc((JSONObject)jLocs.get(i));
				locsList.add(loc);

			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		return locsList;
	}
	
	/** Parsing the Place JSON object */
	private HashMap<String, String> getLoc(JSONObject jLoc){
		HashMap<String, String> loc = new HashMap<String, String>();
		
		String id = "";
		String reference = "";
		String description = "";		
		
		try {
			description = jLoc.getString("description");			
			id = jLoc.getString("id");
			reference = jLoc.getString("reference");
			
			loc.put("description", description);
			loc.put("_id",id);
			loc.put("reference",reference);
			
		} catch (JSONException e) {			
			e.printStackTrace();
		}		
		return loc;
	}
}
