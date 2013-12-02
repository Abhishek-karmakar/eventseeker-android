package com.wcities.eventseeker.api;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.wcities.eventseeker.constants.AppConstants;

public class ArtistApi extends Api {
	
	private static final String TAG = "ArtistApi";
	private static final String API = "artist_api/";
	
	public static enum Method {
		artistSearch,
		artistDetail,
		artistEvent
	};
	
	private String artist;
	private boolean strictSearchEnabled;
	private boolean exactSearchEnabled;
	private int limit;
	private int alreadyRequested;
	private Method method;
	private int artistId;
	private boolean venueDetail;
	private boolean friends;
	private String endDate;
	
	private String userId;

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

	public JSONObject getArtists() throws ClientProtocolException, IOException, JSONException {
		String METHOD = "getArtist.php?";
		String uri = COMMON_URL + API + METHOD + "oauth_token=" + getOauthToken();

		if (artist != null) {
			uri = uri + "&artist=" + artist;
		}
		if (strictSearchEnabled) {
			uri = uri + "&strictSearch=enable";
		}
		if (exactSearchEnabled) {
			uri = uri + "&exactSearch=enable";
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
		if (userId != null) {
			uri = uri + "&userId=" + userId + "&userType=wcities"; 
			
			if (friends) {
				uri = uri + "&friends=enable";
			}
		}
		setUri(uri);
		Log.d(TAG, "uri="+uri);
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
		if (userId != null) {
			uri = uri + "&userId=" + userId + "&userType=wcities"; 
			
			if (friends) {
				uri = uri + "&friends=enable";
			}
		}
		uri += "&response_type=json&lang=eng&strip_html=description&limit=42";
		setUri(uri);

		Log.d(TAG, "uri="+uri);
		String postParams = "artist=" + URLEncoder.encode(buildSearchArtistsJSON(artistNames, startIndex, maxEndIndex), AppConstants.CHARSET_NAME);
		//Log.d(TAG, "postParams="+URLEncoder.encode(buildSearchArtistsJSON(artistNames, startIndex, endIndex), AppConstants.CHARSET_NAME));
		return execute(RequestMethod.POST, ContentType.MIME_APPLICATION_X_WWW_FORM_URLENCODED, postParams.getBytes());
	}
}
