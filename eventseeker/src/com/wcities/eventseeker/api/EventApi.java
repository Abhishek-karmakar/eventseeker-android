package com.wcities.eventseeker.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.wcities.eventseeker.constants.AppConstants;

public class EventApi extends Api {
	
	private static final String TAG = "EventApi";

	private static final String API = "event_api/";
	
	public static enum MoreInfo {
		fallbackimage
	};
	
	public static enum IdType {
		EVENT,
		VENUE;
	}
	
	public static enum Compact {
		venues,
		restrict;
	}
	
	private double lat = AppConstants.NOT_ALLOWED_LAT;
	private double lon = AppConstants.NOT_ALLOWED_LON;
	private long id;
	private long venueId;
	private int limit;
	private int alreadyRequested;
	private int miles;
	private int category;
	private int subcategory;
	private String start, end;
	private String searchFor;
	private boolean strictSearchEnabled;
	private boolean mediaEnabled;
	private boolean friendsEnabled;
	private List<String> moreInfo;
	private List<String> compactList;
	private String userId;
	
	private EventApi(String oauthToken) {
		super(oauthToken);
		moreInfo = new ArrayList<String>();
		compactList = new ArrayList<String>();
	}

	public EventApi(String oauthToken, double lat2, double lon2) {
		this(oauthToken);
		this.lat = lat2;
		this.lon = lon2;
	}
	
	public EventApi(String oauthToken, long id, IdType idType) {
		this(oauthToken);
		
		switch (idType) {
		
		case EVENT:
			this.id = id;
			break;

		case VENUE:
			venueId = id;
			break;
			
		default:
			break;
		}
	}
	
	public double getLat() {
		return lat;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	public double getLon() {
		return lon;
	}

	public void setLon(double lon) {
		this.lon = lon;
	}

	public long getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public long getVenueId() {
		return venueId;
	}

	public void setVenueId(int venueId) {
		this.venueId = venueId;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public int getAlreadyRequested() {
		return alreadyRequested;
	}

	public void setAlreadyRequested(int alreadyRequested) {
		this.alreadyRequested = alreadyRequested;
	}

	public int getMiles() {
		return miles;
	}

	public void setMiles(int miles) {
		this.miles = miles;
	}

	public int getCategory() {
		return category;
	}

	public void setCategory(int category) {
		this.category = category;
	}

	public int getSubcategory() {
		return subcategory;
	}

	public void setSubcategory(int subcategory) {
		this.subcategory = subcategory;
	}

	public String getStart() {
		return start;
	}

	public void setStart(String start) {
		this.start = start;
	}

	public String getEnd() {
		return end;
	}

	public void setEnd(String end) {
		this.end = end;
	}

	public String getSearchFor() {
		return searchFor;
	}

	public void setSearchFor(String searchFor) {
		this.searchFor = searchFor;
	}

	public boolean isStrictSearchEnabled() {
		return strictSearchEnabled;
	}

	public void setStrictSearchEnabled(boolean strictSearchEnabled) {
		this.strictSearchEnabled = strictSearchEnabled;
	}

	public boolean isMediaEnabled() {
		return mediaEnabled;
	}

	public void setMediaEnabled(boolean mediaEnabled) {
		this.mediaEnabled = mediaEnabled;
	}
	
	public boolean isFriendsEnabled() {
		return friendsEnabled;
	}

	public void setFriendsEnabled(boolean friendsEnabled) {
		this.friendsEnabled = friendsEnabled;
	}

	public void addCompact(Compact compact) {
		compactList.add(compact.name());
	}

	public void addMoreInfo(MoreInfo moreInfo) {
		this.moreInfo.add(moreInfo.name());
	}
	
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public JSONObject getFeaturedEvents() throws ClientProtocolException, IOException, JSONException {
		String METHOD = "getFeaturedEvents.php?";
		String uri = COMMON_URL + "featured_event/" + METHOD + "oauth_token=" + getOauthToken() + 
				"&type=featured";
		
		if (lat != AppConstants.NOT_ALLOWED_LAT && lon != AppConstants.NOT_ALLOWED_LON) {
			uri = uri.concat("&lat=" + lat + "&lon=" + lon);
		}
		
		if (miles != NOT_INITIALIZED) {
			uri = uri.concat("&miles=" + miles);
		}
		
		if (limit != NOT_INITIALIZED) {
			uri = uri.concat("&limit=" + limit);
		}
		
		setUri(uri);
		Log.i(TAG, "uri="+uri);
		return execute(RequestMethod.GET, null, null);
	}
	
	public JSONObject getEvents() throws ClientProtocolException, IOException, JSONException {
		String METHOD = "getEvents.php?";
		String uri = COMMON_URL + API + METHOD + "oauth_token=" + getOauthToken();
			
		if (id != NOT_INITIALIZED) {
			uri = uri + "&id=" + id;
		}
		
		if (venueId != NOT_INITIALIZED) {
			uri = uri + "&venueId=" + venueId;
		}
		
		if (lat != AppConstants.NOT_ALLOWED_LAT && lon != AppConstants.NOT_ALLOWED_LON) {
			uri = uri + "&lat=" + lat + "&lon=" + lon;
		}
		
		if (miles != NOT_INITIALIZED) {
			uri = uri + "&miles=" + miles;
		}
		
		if (limit != NOT_INITIALIZED) {
			uri = uri + "&limit=" + alreadyRequested + "," + limit;
		}
		
		if (category != NOT_INITIALIZED) {
			uri = uri + "&cat=" + category;
		}
		
		if (subcategory != NOT_INITIALIZED) {
			uri = uri + "&subcat=" + subcategory;
		}
		
		if (start != null) {
			uri = uri + "&start=" + start;
		}
		
		if (end != null) {
			uri = uri + "&end=" + end;
		}
		
		if (searchFor != null) {
			uri = uri + "&searchFor=" + searchFor;
		}
		
		if (strictSearchEnabled) {
			uri = uri + "&strictSearch=enable";
		}
		
		if (mediaEnabled) {
			uri = uri + "&media=enable";
		}
		
		if (!compactList.isEmpty()) {
			uri = uri + "&compact=";
			
			for (Iterator<String> iterator = compactList.iterator(); iterator.hasNext();) {
				if (!uri.endsWith("&compact=")) {
					uri = uri + ",";
				}
				uri = uri + iterator.next();				
			}
		}
		
		uri += "&moreInfo=artistdesc";
		if (!moreInfo.isEmpty()) {
			for (Iterator<String> iterator = moreInfo.iterator(); iterator.hasNext();) {
				uri = uri + "," + iterator.next();
			}
		}
		
		if (userId != null) {
			uri = uri + "&userId=" + userId + "&userType=wcities"; 
			
			if (friendsEnabled) {
				uri = uri + "&friends=enable";
			}
		}
		
		uri += "&link=enable&strip_html=name,description";
		setUri(uri.toString());
		addLangParam = true;
		return execute(RequestMethod.GET, null, null);
	}
}
