package com.wcities.eventseeker.api;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.wcities.eventseeker.constants.AppConstants;

public class UserInfoApi extends Api {
	
	private static final String TAG = "UserInfoApi";
	private static final String API = "userInfo/";
	
	public static enum Type {
		artistsfeed,
		friendsfeed,
		myevents,
		myartists,
		signup,
		syncaccount,
		recommendedevent,
		registerDevice;
	};
	
	public static enum UserTrackingType {
		Add,
		Edit;
	}
	
	public static enum UserTrackingItemType {
		poi,
		event,
		artist;
	}
	
	public static enum Tracktype {
		artist,
		event
	}
	
	public static enum LoginType {
		facebook,
		googlePlus;
	}
	
	// userId - generated & returned by server on signup call
	private String fbUserId, gPlusUserId, deviceId, userId, gcmRegistrationId; 
	private Tracktype tracktype;
	
	private int limit;
	private int alreadyRequested;
	
	private double lat = AppConstants.NOT_ALLOWED_LAT, lon = AppConstants.NOT_ALLOWED_LON;
	
	private int artistId;

	public UserInfoApi(String oauthToken) {
		super(oauthToken);
	}

	public String getFbUserId() {
		return fbUserId;
	}

	public void setFbUserId(String fbUserId) {
		this.fbUserId = fbUserId;
	}

	public String getgPlusUserId() {
		return gPlusUserId;
	}

	public void setgPlusUserId(String gPlusUserId) {
		this.gPlusUserId = gPlusUserId;
	}

	public Tracktype getTracktype() {
		return tracktype;
	}

	public void setTracktype(Tracktype tracktype) {
		this.tracktype = tracktype;
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
	
	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getGcmRegistrationId() {
		return gcmRegistrationId;
	}

	public void setGcmRegistrationId(String gcmRegistrationId) {
		this.gcmRegistrationId = gcmRegistrationId;
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

	public int getArtistId() {
		return artistId;
	}

	public void setArtistId(int artistId) {
		this.artistId = artistId;
	}

	public JSONObject signUp() throws ClientProtocolException, IOException, JSONException {
		String METHOD = "myProfile.php?";
		StringBuilder uriBuilder = new StringBuilder(COMMON_URL).append(API).append(METHOD).append("oauth_token=")
				.append(getOauthToken()).append("&type=").append(Type.signup.name()).append("&deviceId=")
				.append(deviceId).append("&deviceType=android");
		
		setUri(uriBuilder.toString());
		Log.i(TAG, "uri=" + getUri());
		return execute(RequestMethod.GET, null, null); 
	}
	
	public JSONObject syncAccount(String repCode, LoginType loginType) throws ClientProtocolException, IOException, JSONException {
		String METHOD = "myProfile.php?";
		String loginId = null, userType = null;
		if (loginType == LoginType.facebook) {
			loginId = fbUserId;
			userType = "fb";
			
		} else if (loginType == LoginType.googlePlus) {
			loginId = gPlusUserId;
			userType = "google";
		}
		StringBuilder uriBuilder = new StringBuilder(COMMON_URL).append(API).append(METHOD).append("oauth_token=")
				.append(getOauthToken()).append("&type=").append(Type.syncaccount.name()).append("&userId=")
				.append(loginId).append("&userType=").append(userType).append("&wcitiesId=").append(userId);
		if (repCode != null) {
			uriBuilder.append("&repCode=" + repCode);
		}
		
		setUri(uriBuilder.toString());
		Log.i(TAG, "uri=" + getUri());
		return execute(RequestMethod.GET, null, null); 
	}
	
	public JSONObject registerDevice() throws ClientProtocolException, IOException, JSONException {
		String METHOD = "myProfile.php?";
		StringBuilder uriBuilder = new StringBuilder(COMMON_URL).append(API).append(METHOD).append("oauth_token=")
				.append(getOauthToken()).append("&type=").append(Type.registerDevice.name()).append("&userId=")
				.append(userId).append("&userType=wcities").append("&deviceId=").append(gcmRegistrationId)
				.append("&deviceType=android&notification=Yes");
		
		setUri(uriBuilder.toString());
		Log.i(TAG, "uri=" + getUri());
		
		return execute(RequestMethod.GET, null, null); 
	}
	
	public JSONObject addUserTracking(UserTrackingItemType type, long id, int attending, String fb_postid) throws ClientProtocolException, IOException, JSONException {
		String METHOD = "addUserTracking.php";
		StringBuilder uriBuilder = new StringBuilder(COMMON_URL).append(API).append(METHOD);
		setUri(uriBuilder.toString());
		
		JSONObject jObjUserDetail = new JSONObject();
		jObjUserDetail.put("id", "" + userId);
		jObjUserDetail.put("type", "wcities");

		JSONObject jObjTrackInfo = new JSONObject();
		jObjTrackInfo.put("type", type.name());
		jObjTrackInfo.put("type_id", "" + id);
		if (type == UserTrackingItemType.event) {
			jObjTrackInfo.put("attending", "" + attending);
			if (fb_postid != null) {
				jObjTrackInfo.put("fb_postid", fb_postid);
			}
		}
		
		JSONObject jObjUserTracking = new JSONObject();
		jObjUserTracking.put("trackInfo", jObjTrackInfo);
		
		JSONObject jObjData = new JSONObject();
		jObjData.put("userDetail", jObjUserDetail);
		jObjData.put("userTracking", jObjUserTracking);
		
		StringBuilder paramsBuilder = new StringBuilder();
		/*paramsBuilder = paramsBuilder.append("oauth_token=")
				.append(getOauthToken()).append("&data={\"userDetail\": {\"id\": \"").append(userId)
				.append("\",\"type\": \"wcities\"},\"userTracking\": {\"trackInfo\": [{\"type\": \"event\",\"type_id\": \"")
				.append(eventId).append("\", \"attending\": \"").append(attending.getValue()).append("\"}]}}");*/
		paramsBuilder = paramsBuilder.append("oauth_token=").append(getOauthToken()).append("&data=").append(jObjData.toString());
		Log.d(TAG, "params = " + paramsBuilder.toString());
		Log.i(TAG, "uri=" + getUri());
		return execute(RequestMethod.POST, ContentType.MIME_APPLICATION_X_WWW_FORM_URLENCODED, paramsBuilder.toString().getBytes());
	}
	
	public JSONObject editUserTracking(UserTrackingItemType type, long id, int attending) throws ClientProtocolException, IOException, JSONException {
		String METHOD = "editUserTracking.php?";
		StringBuilder uriBuilder = new StringBuilder(COMMON_URL).append(API).append(METHOD).append("oauth_token=")
				.append(getOauthToken()).append("&userId=").append(userId).append("&userType=wcities&type=").append(type.name())
				.append("&type_id=").append(id).append("&attending=").append(attending);
		setUri(uriBuilder.toString());
		
		Log.i(TAG, "uri=" + getUri());
		return execute(RequestMethod.GET, null, null);
	}

	public JSONObject getMyProfileInfoFor(Type type) throws ClientProtocolException, IOException, JSONException {
		String METHOD = "myProfile.php?";
		StringBuilder uriBuilder = new StringBuilder(COMMON_URL).append(API).append(METHOD).append("oauth_token=")
				.append(getOauthToken()).append("&type=").append(type.name()).append("&userId=").append(userId)
				.append("&userType=wcities");
		
		if (type == Type.friendsfeed && tracktype != null) {
			uriBuilder.append("&tracktype=").append(tracktype.name()).append("&removeTracking=enable");
		} 
		
		if (limit != NOT_INITIALIZED) {
			uriBuilder.append("&limit=").append(alreadyRequested).append(",").append(limit);
		}
		
		if (lat != AppConstants.NOT_ALLOWED_LAT && lon != AppConstants.NOT_ALLOWED_LON) {
			uriBuilder.append("&lat=").append(lat).append("&lon=").append(lon).append("&miles=50");
		}
		
		if (artistId != 0) {
			uriBuilder.append("&artistId=").append(artistId);
		}
		
		if (type == Type.myevents || type == Type.recommendedevent) {
			uriBuilder.append("&link=enable");
		}
		
		setUri(uriBuilder.toString());
		Log.i(TAG, "uri=" + getUri());
		return execute(RequestMethod.GET, null, null); 
	}
}
