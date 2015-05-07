package com.wcities.eventseeker.api;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.wcities.eventseeker.constants.AppConstants;

public class ArtistApi extends Api {
	
	private static final String TAG = "ArtistApi";
	private static final String API = "artist_api/";
	
	public static enum Method {
		artistSearch,
		artistDetail,
		artistEvent,
		/**
		 * This is for Featured List of Artists Groups in Popular Artists Screen
		 */
		featuredList
	};
	
	private double lat = AppConstants.NOT_ALLOWED_LAT;
	private double lon = AppConstants.NOT_ALLOWED_LON;
	private String artist;
	private boolean strictSearchEnabled, exactSearchEnabled, playingArtistEnabled;
	private int limit;
	private int alreadyRequested;
	private Method method;
	private int artistId;
	private boolean venueDetail;
	private boolean friends;
	private String endDate;
	private int miles;
	private boolean	trackingEnabled;
	private String userId;
	/**
	 * 08-07/2014: added 'artistSource' param in batch artist call as per the samir's mail on July 02, 2014.
	 * added this param
	 */
	private String artistSource;

	public ArtistApi(String oauthToken) {
		super(oauthToken);
	}
	
	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}
	
	private String buildSearchArtistsJSON(List<String> artistNames, int startIndex, int maxEndIndex) throws JSONException {
		JSONObject artistObject = new JSONObject();
		JSONArray artistArray = new JSONArray();
		for (int i = startIndex; i < artistNames.size() && i < maxEndIndex; i++) {
			artistArray.put(artistNames.get(i));
		}
		artistObject.put("artist", artistArray);
		return artistObject.toString();
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

	public boolean isStrictSearchEnabled() {
		return strictSearchEnabled;
	}

	public void setStrictSearchEnabled(boolean strictSearchEnabled) {
		this.strictSearchEnabled = strictSearchEnabled;
	}

	public boolean isExactSearchEnabled() {
		return exactSearchEnabled;
	}

	public void setExactSearchEnabled(boolean exactSearchEnabled) {
		this.exactSearchEnabled = exactSearchEnabled;
	}

	public boolean isPlayingArtistEnabled() {
		return playingArtistEnabled;
	}

	public void setPlayingArtistEnabled(boolean playingArtistEnabled) {
		this.playingArtistEnabled = playingArtistEnabled;
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

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public int getArtistId() {
		return artistId;
	}

	public void setArtistId(int artistId) {
		this.artistId = artistId;
	}

	public boolean isVenueDetailEnabled() {
		return venueDetail;
	}

	public void setVenueDetailEnabled(boolean enable) {
		this.venueDetail = enable;
	}

	public boolean isFriendsEnabled() {
		return friends;
	}

	public void setFriendsEnabled(boolean enable) {
		this.friends = enable;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	public int getMiles() {
		return miles;
	}

	public void setMiles(int miles) {
		this.miles = miles;
	}
	
	public boolean isTrackingEnabled() {
		return trackingEnabled;
	}

	public void setTrackingEnabled(boolean trackingEnabled) {
		this.trackingEnabled = trackingEnabled;
	}

	public void setArtistSource(String artistSource) {
		this.artistSource = artistSource;
	}
	
	public JSONObject getArtists() throws ClientProtocolException, IOException, JSONException {
		String METHOD = "getArtist.php?";
		String uri = COMMON_URL + API + METHOD + "oauth_token=" + getOauthToken();

		if (artist != null) {
			uri = uri + "&artist=" + artist;
		}
		if (strictSearchEnabled) {
			uri = uri + "&strictSearch=enable";
		}
		if (trackingEnabled) {
			uri = uri + "&tracking=enable";
		}
		if (exactSearchEnabled) {
			uri = uri + "&exactSearch=enable";
		}
		if (playingArtistEnabled) {
			uri += "&playingArtist=enable";
		}
		if (lat != AppConstants.NOT_ALLOWED_LAT && lon != AppConstants.NOT_ALLOWED_LON) {
			uri = uri.concat("&lat=" + lat + "&lon=" + lon);
		}
		if (limit != NOT_INITIALIZED) {
			uri = uri + "&limit=" + alreadyRequested + "," + limit;
		}
		if (method != null) {
			uri = uri + "&method=" + method.name();
		}
		if (artistId != NOT_INITIALIZED) {
			uri = uri + "&id=" + artistId;
		}
		if (venueDetail) {
			uri = uri + "&venueDetail=enable";
		}
		if (endDate != null) {
			uri += "&endDate=" + endDate;
		}
		if (miles != NOT_INITIALIZED) {
			uri = uri.concat("&miles=" + miles);
		}
		if (userId != null) {
			uri = uri + "&userId=" + userId + "&userType=wcities"; 
			
			if (friends) {
				uri = uri + "&friends=enable";
			}
		}

		if (method != Method.featuredList) {
			uri += "&moreInfo=strictlang";

			setUri(uri);
			addLangParam = true;

		} else {
			setUri(uri);
		}
		
		return execute(RequestMethod.GET, null, null);
	}
	
	public JSONObject getArtists(List<String> artistNames, int startIndex, int maxEndIndex) throws ClientProtocolException, IOException, JSONException {
		String METHOD = "getArtist.php?";
		String uri = COMMON_URL + API + METHOD + "oauth_token=" + getOauthToken();
		if (exactSearchEnabled) {
			uri = uri + "&exactSearch=enable";
		}
		if (method != null) {
			uri = uri + "&method=" + method.name();
		}
		if (artistSource != null) {
			uri = uri + "&source=" + artistSource;
		}
		if (userId != null) {
			uri = uri + "&userId=" + userId + "&userType=wcities"; 
			
			if (friends) {
				uri = uri + "&friends=enable";
			}
		}
		if (limit != NOT_INITIALIZED) {
			uri = uri + "&limit=" + limit;
		}
		uri += "&response_type=json&strip_html=description";
		
		uri += "&moreInfo=strictlang";
		
		setUri(uri);
		addLangParam = true;
		
		String postParams = "artist=" + URLEncoder.encode(buildSearchArtistsJSON(artistNames, startIndex, maxEndIndex), AppConstants.CHARSET_NAME);
		//Log.d(TAG, "postParams="+buildSearchArtistsJSON(artistNames, startIndex, maxEndIndex));
		return execute(RequestMethod.POST, ContentType.MIME_APPLICATION_X_WWW_FORM_URLENCODED, postParams.getBytes());
	}
}
