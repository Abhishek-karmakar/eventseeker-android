package com.wcities.eventseeker.api;

import android.content.res.Resources;
import android.util.Log;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.Enums.SortArtistNewsBy;
import com.wcities.eventseeker.constants.Enums.SortRecommendedArtist;
import com.wcities.eventseeker.core.Artist.Genre;
import com.wcities.eventseeker.core.registration.EmailLogin;
import com.wcities.eventseeker.core.registration.EmailSignup;
import com.wcities.eventseeker.core.registration.FacebookLogin;
import com.wcities.eventseeker.core.registration.GooglePlusLogin;
import com.wcities.eventseeker.core.registration.Registration;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

public class UserInfoApi extends Api {
	
	private static final String TAG = "UserInfoApi";
	private static final String API = "userInfo/";
	
	public static enum Type {
		artistsfeed,
		friendsfeed,
		myevents,
		myartists,
		recommendedartist,
		popularartist,
		signup,
		syncaccount,
		syncfriends,
		recommendedevent,
		registerDevice,
		checkemail,
		login,
		forgot,
		mysavedevents,
		updaterepcode;
	};
	
	public static enum UserTrackingType {
		Add,
		AddMultiple,
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
		googlePlus,
		emailSignup,
		emailLogin;
		
		public Registration getRegistrationInstance(EventSeekr eventSeekr) {
			switch (this) {
			
			case emailSignup:
				return new EmailSignup(eventSeekr);
				
			case emailLogin:
				return new EmailLogin(eventSeekr);
				
			case facebook:
				return new FacebookLogin(eventSeekr);
				
			case googlePlus:
				return new GooglePlusLogin(eventSeekr);
			}
			return null;
		}
	}
	
	public static enum UserType {
		fb,
		google,
		wcities;
	}
	
	public static enum RepCodeResponse {
		OTHER_ACCOUNT_ALREADY_SYNCED(0, R.string.rep_code_response_other_account_already_synced),
		SUCCESSFULLY_SUBMITTED(1, R.string.rep_code_response_successfully_submitted),
		DATABASE_INSERTION_ERROR(-1, R.string.rep_code_response_database_insertion_error),
		INVALID_REP_CODE(2, R.string.rep_code_response_invalid_rep_code),
		UNKNOWN_ERROR(-2, R.string.rep_code_response_unknown_error);
		
		private int repCode;
		private int msgId;
		
		private RepCodeResponse(int repCode, int msgId) {
			this.repCode = repCode;
			this.msgId = msgId;
		}

		public int getRepCode() {
			return repCode;
		}

		public String getMsg(Resources res) {
			return res.getString(msgId);
		}
		
		public static RepCodeResponse getRepCodeResponse(int repCode) {
			RepCodeResponse[] repCodeResponses = RepCodeResponse.values();
			for (int i = 0; i < repCodeResponses.length; i++) {
				if (repCode == repCodeResponses[i].getRepCode()) {
					return repCodeResponses[i];
				}
			}
			return UNKNOWN_ERROR;
		}
	}
	
	// userId - generated & returned by server on signup call
	private String fbUserId, gPlusUserId, deviceId, userId, gcmRegistrationId; 
	private String fbEmailId, gPlusEmailId;
	private Tracktype tracktype;
	
	private int limit;
	private int alreadyRequested;
	
	private double lat = AppConstants.NOT_ALLOWED_LAT, lon = AppConstants.NOT_ALLOWED_LON;
	
	private int artistId;
	
	private SortArtistNewsBy sortBy;
	private SortRecommendedArtist sortRecommendedArtist;

	public UserInfoApi(String oauthToken) {
		super(oauthToken);
	}

	public String getFbUserId() {
		return fbUserId;
	}

	public void setFbUserId(String fbUserId) {
		this.fbUserId = fbUserId;
	}

	public String getFbEmailId() {
		return fbEmailId;
	}

	public void setFbEmailId(String fbEmailId) {
		this.fbEmailId = fbEmailId;
	}

	public String getGPlusUserId() {
		return gPlusUserId;
	}

	public void setGPlusUserId(String gPlusUserId) {
		this.gPlusUserId = gPlusUserId;
	}

	public String getGPlusEmailId() {
		return gPlusEmailId;
	}

	public void setGPlusEmailId(String gPlusEmailId) {
		this.gPlusEmailId = gPlusEmailId;
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

	public SortArtistNewsBy getSortBy() {
		return sortBy;
	}

	public void setSortBy(SortArtistNewsBy sortBy) {
		this.sortBy = sortBy;
	}

	public SortRecommendedArtist getSortRecommendedArtist() {
		return sortRecommendedArtist;
	}

	public void setSortRecommendedArtist(SortRecommendedArtist sortRecommendedArtist) {
		this.sortRecommendedArtist = sortRecommendedArtist;
	}

	public JSONObject signUp() throws ClientProtocolException, IOException, JSONException {
		String METHOD = "myProfile.php?";
		StringBuilder uriBuilder = new StringBuilder(COMMON_URL).append(API).append(METHOD).append("oauth_token=")
				.append(getOauthToken()).append("&type=").append(Type.signup.name()).append("&deviceType=android");
		if (deviceId != null) {
			uriBuilder.append("&deviceId=").append(deviceId);
		}
		
		setUri(uriBuilder.toString());
		Log.i(TAG, "uri=" + getUri());
		return execute(RequestMethod.GET, null, null); 
	}
	
	public JSONObject signup(String email, String password, String fname, String lname, String wcitiesId) 
			throws ClientProtocolException, IOException, JSONException {
		String METHOD = "wLogin.php?";
		
		StringBuilder uriBuilder = new StringBuilder(COMMON_URL).append(API).append(METHOD).append("oauth_token=")
				.append(getOauthToken());
		setUri(uriBuilder.toString());
		
		StringBuilder paramsBuilder = new StringBuilder();
		paramsBuilder = paramsBuilder.append("type=").append(Type.signup.name()).append("&email=").append(email)
				.append("&password=").append(password).append("&fname=").append(fname).append("&lname=")
				.append(lname).append("&deviceType=android").append("&wcitiesId=").append(wcitiesId);
		Log.d(TAG, "uri=" + getUri());
		//Log.d(TAG, "params=" + paramsBuilder.toString());
		return execute(RequestMethod.POST, ContentType.MIME_APPLICATION_X_WWW_FORM_URLENCODED, paramsBuilder.toString().getBytes());
	}
	
	public JSONObject login(String email, String password) throws ClientProtocolException, IOException, JSONException {
		String METHOD = "wLogin.php?";
		StringBuilder uriBuilder = new StringBuilder(COMMON_URL).append(API).append(METHOD).append("oauth_token=")
				.append(getOauthToken());
		setUri(uriBuilder.toString());
		
		StringBuilder paramsBuilder = new StringBuilder();
		paramsBuilder = paramsBuilder.append("type=").append(Type.login.name()).append("&email=").append(email)
				.append("&password=").append(password);
		Log.d(TAG, "uri=" + getUri());
		//Log.d(TAG, "params=" + paramsBuilder.toString());
		return execute(RequestMethod.POST, ContentType.MIME_APPLICATION_X_WWW_FORM_URLENCODED, paramsBuilder.toString().getBytes());
	}
	
	public JSONObject syncAccount(String repCode, String loginId, String email, UserType userType, 
			String wcitiesId) throws ClientProtocolException, IOException, JSONException {
		String METHOD = "myProfile.php?";
		StringBuilder uriBuilder = new StringBuilder(COMMON_URL).append(API).append(METHOD).append("oauth_token=")
				.append(getOauthToken()).append("&type=").append(Type.syncaccount.name()).append("&userId=")
				.append(loginId).append("&userType=").append(userType.name())
				.append("&wcitiesId=").append(wcitiesId);
		if (email != null) {
			uriBuilder.append("&email=").append(email);
		}
		if (repCode != null) {
			uriBuilder.append("&repCode=" + repCode);
		}
		
		setUri(uriBuilder.toString());
		Log.i(TAG, "uri=" + getUri());
		return execute(RequestMethod.GET, null, null); 
	}
	
	public JSONObject forgotPassword(String email) throws ClientProtocolException, IOException, JSONException {
		String METHOD = "wLogin.php?";
		StringBuilder uriBuilder = new StringBuilder(COMMON_URL).append(API).append(METHOD).append("oauth_token=")
				.append(getOauthToken()).append("&type=").append(Type.forgot.name()).append("&email=")
				.append(email);
		
		setUri(uriBuilder.toString());
		Log.d(TAG, "uri=" + getUri());
		return execute(RequestMethod.GET, null, null);
	}
	
	/**
	 * Nov 19, 2014:
	 * update Repcode with the wcities Id. Use this call only for Wcities Email Login as for Fb & Google Login the
	 * Sync Account call handles the Recode updates.
	 * @param repCode
	 * @param loginId
	 * @param email
	 * @param userType
	 * @param wcitiesId
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws JSONException
	 * http://dev.wcities.com/V3/userInfo/myProfile.php?oauth_token=5c63440e7db1ad33c3898cdac3405b1e&type=updaterepcode&wcitiesId=1111740&repCode=AX3315&response_type=xml
	 */
	public JSONObject updateRepcodeWithWcitiesId(String repCode, String wcitiesId)
			throws ClientProtocolException, IOException, JSONException {
		String METHOD = "myProfile.php?";
		StringBuilder uriBuilder = new StringBuilder(COMMON_URL).append(API).append(METHOD).append("oauth_token=")
				.append(getOauthToken()).append("&type=").append(Type.updaterepcode.name()).append("&wcitiesId=")
				.append(wcitiesId);
		if (repCode != null) {
			uriBuilder.append("&repCode=" + repCode);
		}
		
		setUri(uriBuilder.toString());
		Log.i(TAG, "uri=" + getUri());
		return execute(RequestMethod.GET, null, null); 
	}
	
	/*public JSONObject syncAccount(String repCode, LoginType loginType) throws ClientProtocolException, IOException, JSONException {
		String METHOD = "myProfile.php?";
		String loginId = null, userType = null, email = null;
		if (loginType == LoginType.facebook) {
			loginId = fbUserId;
			email = fbEmailId;
			userType = UserType.fb.name();
			
		} else if (loginType == LoginType.googlePlus) {
			loginId = gPlusUserId;
			email = gPlusEmailId;
			userType = UserType.google.name();
		}
		StringBuilder uriBuilder = new StringBuilder(COMMON_URL).append(API).append(METHOD).append("oauth_token=")
				.append(getOauthToken()).append("&type=").append(Type.syncaccount.name()).append("&userId=")
				.append(loginId).append("&email=").append(email).append("&userType=").append(userType)
				.append("&wcitiesId=").append(userId);
		if (repCode != null) {
			uriBuilder.append("&repCode=" + repCode);
		}
		
		setUri(uriBuilder.toString());
		Log.i(TAG, "uri=" + getUri());
		return execute(RequestMethod.GET, null, null); 
	}*/
	
	public JSONObject syncFriends(UserType userType, String loginId, String accessToken) throws ClientProtocolException, IOException, JSONException {
		String METHOD = "myProfile.php?";
		
		/*StringBuilder uriBuilder = new StringBuilder("http://108.166.108.227/SocialApi/user/myProfile-gp.php?").append("oauth_token=")
				.append(getOauthToken()).append("&type=").append(Type.syncfriends.name()).append("&userId=")
				.append(loginId).append("&userType=").append(userType);*/
		StringBuilder uriBuilder = new StringBuilder(COMMON_URL).append(API).append(METHOD).append("oauth_token=")
				.append(getOauthToken()).append("&type=").append(Type.syncfriends.name()).append("&userId=")
				.append(loginId).append("&userType=").append(userType.name());
		if (accessToken != null) {
			uriBuilder = uriBuilder.append("&access_token=").append(accessToken);
		}
		
		setUri(uriBuilder.toString());
		Log.d(TAG, "uri=" + getUri());
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
	
	public JSONObject checkEmail(String email) throws ClientProtocolException, IOException, JSONException {
		String METHOD = "wLogin.php?";
		StringBuilder uriBuilder = new StringBuilder(COMMON_URL).append(API).append(METHOD).append("oauth_token=")
				.append(getOauthToken()).append("&type=").append(Type.checkemail.name()).append("&email=")
				.append(email);
		setUri(uriBuilder.toString());
		Log.d(TAG, "uri=" + getUri());
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
	
	/**
	 * 06-01-2014:
	 * Currently, created considering the scenario for multiple Artist Tracking. So, check if using for the events or pois
	 * then if any modifications are needed.
	 * mailed this call to Anupam on 09-01-2015
	 * @param type
	 * @param id
	 * @param attending
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws JSONException
	 */
	public JSONObject addMultipleUserTracking(UserTrackingItemType type, List<Long> ids) 
			throws ClientProtocolException, IOException, JSONException {
		String METHOD = "addUserTracking.php";
		StringBuilder uriBuilder = new StringBuilder(COMMON_URL).append(API).append(METHOD);
		setUri(uriBuilder.toString());
		
		JSONObject jObjUserDetail = new JSONObject();
		jObjUserDetail.put("id", "" + userId);
		jObjUserDetail.put("type", "wcities");

		JSONArray jArrTrackInfo = new JSONArray();
		for (long id : ids) {
			JSONObject jObj = new JSONObject();
			jObj.put("type", type.name());
			jObj.put("type_id", "" + id);
			
			jArrTrackInfo.put(jObj);
		}
		
		JSONObject jObjUserTracking = new JSONObject();
		jObjUserTracking.put("trackInfo", jArrTrackInfo);
		
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
		StringBuilder uriBuilder;
		/*if (type == Type.friendsfeed) {
			uriBuilder = new StringBuilder("http://108.166.108.227/SocialApi/user/myProfile-gp.php?").append("oauth_token=")
					.append(getOauthToken()).append("&type=").append(type.name()).append("&userId=").append(userId)
					.append("&userType=wcities");
		} else {*/
			uriBuilder = new StringBuilder(COMMON_URL).append(API).append(METHOD).append("oauth_token=")
					.append(getOauthToken()).append("&type=").append(type.name()).append("&userId=").append(userId)
					.append("&userType=wcities");
		//}
		
		if (type == Type.friendsfeed) { 
			uriBuilder.append("&multiple_account=enable&moreInfo=daterange");
			
			if (tracktype != null) {
				uriBuilder.append("&tracktype=").append(tracktype.name()).append("&removeTracking=enable");
			}
			
		} else if (type == Type.artistsfeed && sortBy == SortArtistNewsBy.trending) {
			uriBuilder.append("&sort=" + sortBy.trending.name());			
		
		} else if (type == Type.recommendedartist) {
			uriBuilder.append("&moreInfo=score");
			if (sortRecommendedArtist == SortRecommendedArtist.name) {
				uriBuilder.append("&sort=" + sortRecommendedArtist.name.name());				
			}
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
		
		if (type == Type.myevents || type == Type.recommendedevent || type == Type.mysavedevents) {
			uriBuilder.append("&link=enable&moreInfo=daterange");
		}
		
		/*if (type == Type.mysavedevents) {
			uriBuilder.append("&link=enable");
			uriBuilder.append("&moreInfo=fallbackimage");
			uriBuilder.append("&strip_html=name,description");
		}*/
		
		setUri(uriBuilder.toString());
		addLangParam = true;
		
		Log.i(TAG, "uri=" + getUri());
		return execute(RequestMethod.GET, null, null); 
	}
	
	public JSONObject getMyProfileInfoForPopularArtist(Genre genre) throws ClientProtocolException, IOException, JSONException {
		String METHOD = "myProfile.php?";
		StringBuilder uriBuilder = new StringBuilder(COMMON_URL).append(API).append(METHOD).append("oauth_token=")
				.append(getOauthToken()).append("&type=").append(Type.popularartist.name())
				.append("&userId=").append(userId).append("&userType=wcities");
		
		//For Featured Artist List Genre's URL form will be 'null'.
		if (genre.getUrlForm() != null) {
			uriBuilder.append("&genre=").append(URLEncoder.encode(genre.getUrlForm(), AppConstants.CHARSET_NAME));
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
		
		setUri(uriBuilder.toString());
		addLangParam = true;
		
		Log.i(TAG, "uri=" + getUri());
		return execute(RequestMethod.GET, null, null); 
	}
	
	public JSONObject getAvailableSyncServices() throws ClientProtocolException, IOException, JSONException {
		StringBuilder uriBuilder = new StringBuilder(COMMON_URL).append(API).append("syncService.php?deviceType=android");
		setUri(uriBuilder.toString());
		Log.i(TAG, "uri=" + getUri());
		return execute(RequestMethod.GET, null, null); 
	}
}
