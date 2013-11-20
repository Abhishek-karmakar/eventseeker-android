package com.wcities.eventseeker.api;

import java.io.IOException;
import java.net.URLEncoder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.wcities.eventseeker.constants.AppConstants;

public class InfoApi extends Api {
	
	private static final String TAG = InfoApi.class.getName();

	private static final String API = "info_api/";
	
	public static enum PageType {
		poi,
		event
	}
	
	private String lang;
	private String infoFor;
	private String userId;
	private UserType userType;
	private PageType pageType;
	private long pageId;
	private String comment;

	public InfoApi(String oauth_token) {
		super(oauth_token);
	}

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public String getInfoFor() {
		return infoFor;
	}

	public void setInfoFor(String infoFor) {
		this.infoFor = infoFor;
	}
	
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public UserType getUserType() {
		return userType;
	}

	public void setUserType(UserType userType) {
		this.userType = userType;
	}

	public PageType getPageType() {
		return pageType;
	}

	public void setPageType(PageType pageType) {
		this.pageType = pageType;
	}

	public long getPageId() {
		return pageId;
	}

	public void setPageId(long eventId) {
		this.pageId = eventId;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public JSONObject getCategories() throws ClientProtocolException, IOException, JSONException {
		String METHOD = "getCategories.php?";
		String uri = COMMON_URL + API + METHOD + "oauth_token=" + getOauthToken();
		
		if (infoFor != null) {
			uri.concat("&info_for=" + infoFor);
		}
		setUri(uri);
		Log.i(TAG, "uri="+uri);
		return execute(RequestMethod.GET, null, null);
	}
	
	public JSONObject addComment() throws ClientProtocolException, IOException, JSONException {
		String METHOD = "addComment.php?";
		StringBuilder uriBuilder = new StringBuilder(COMMON_URL).append(API)
				.append(METHOD).append("oauth_token=")
				.append(getOauthToken()).append("&data=")
				.append(URLEncoder.encode(getDataToAddComment(), AppConstants.CHARSET_NAME));
		/*StringBuilder uriBuilder = new StringBuilder(COMMON_URL).append(API)
				.append(METHOD).append("oauth_token=")
				.append(getOauthToken());*/
		setUri(uriBuilder.toString());
		Log.i(TAG, "uri="+uriBuilder.toString());
		return execute(RequestMethod.GET, null, null);
	}

	private String getDataToAddComment() throws JSONException {
		JSONObject jsonObject = new JSONObject();
		
		JSONObject jObjUserDetail = new JSONObject();
		jObjUserDetail.put("id", userId);
		jObjUserDetail.put("type", userType.name());
		jsonObject.put("userDetail", jObjUserDetail);
		
		JSONObject jObjComments = new JSONObject();
		jObjComments.put("comment", comment);
		jObjComments.put("page_id", pageId);
		jObjComments.put("page_type", pageType.name());
		jsonObject.put("comments", jObjComments);
		
		return jsonObject.toString();
	}
	
	/*protected JSONObject invokeHttpPost() throws ClientProtocolException, IOException, JSONException {
		JSONObject jsonObject = null;
		HttpClient httpClient = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost();
		httpPost.setHeader("Content-Type",
                "application/x-www-form-urlencoded;charset=UTF-8");
		httpPost.setEntity(new StringEntity(getUri(), AppConstants.CHARSET_NAME));
		
        HttpResponse response = httpClient.execute(httpPost);
		HttpEntity entity = response.getEntity();
		String text = EntityUtils.toString(entity, AppConstants.CHARSET_NAME);
		
		jsonObject = new JSONObject(text);
		httpClient.getConnectionManager().shutdown();
		return jsonObject;
		JSONObject jsonObject = null;
		HttpClient httpClient = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost(getUri());
		
		StringEntity se = new StringEntity(getDataToAddComment());  
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/x-www-form-urlencoded;charset=utf-8"));
        httpPost.setEntity(se);

        HttpResponse response = httpClient.execute(httpPost);
		HttpEntity entity = response.getEntity();
		String text = EntityUtils.toString(entity, AppConstants.CHARSET_NAME);
		
		jsonObject = new JSONObject("$"+text);
		httpClient.getConnectionManager().shutdown();
		return jsonObject;
	}*/
}
