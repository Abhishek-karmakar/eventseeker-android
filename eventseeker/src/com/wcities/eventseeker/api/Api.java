package com.wcities.eventseeker.api;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.wcities.eventseeker.LanguageFragment.Locales;
import com.wcities.eventseeker.analytics.GoogleAnalyticsTracker;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.AppConstants;

public abstract class Api {
	
	private static final String TAG = Api.class.getName();
	
	// provided by Samir after attaching italic tags with description
	public static final String OAUTH_TOKEN = "a82d32bd99044507417055f66c1a614c"; 
											//"5c63440e7db1ad33c3898cdac3405b1e";
	public static final String OAUTH_TOKEN_BOSCH_APP = "5455fb63e3e6804ed2b45a48abbdcd50";
	public static final String OAUTH_TOKEN_FORD_APP = "f435ec60b8f8cd9dbaf98c83c46caf4f";
	
	public static final String COMMON_URL = "http://dev.wcities.com/V3/";
	
	protected static final float NOT_INITIALIZED = 0;
	public static final int ERROR_CODE_NO_RECORDS_FOUND = 2;
	
	public static enum UserType {
		fb,
		twitter
	}
	
	public static enum RequestMethod {
		GET, POST, PUT, DELETE;
	};
	
	public static enum ContentType {
		MIME_APPLICATION_X_WWW_FORM_URLENCODED;
		
		public String toString() {
			return "application/x-www-form-urlencoded;charset=UTF-8";
		};
	};

	private static String localeCode = Locales.ENGLISH.getLocaleCode();
	private static String fordLocaleCode = Locales.ENGLISH_UNITED_STATES.getLocaleCode();
	
	/**
	 * needs to be set true in the current api-call request, if it supports lang parameter.
	 */
	protected boolean addLangParam;
	private boolean addFordLangParam;
	
	private String uri;
	private String oauthToken;

	public Api(String oauthToken) {
		this.oauthToken = oauthToken;
	}

	public String getOauthToken() {
		return oauthToken;
	}

	public void setOauthToken(String oauthToken) {
		this.oauthToken = oauthToken;
	}

	protected String getUri() {
		return uri;
	}

	protected void setUri(String uri) {
		this.uri = uri;
	}
	
	public void setAddFordLangParam(boolean addFordLangParam) {
		this.addFordLangParam = addFordLangParam;
	}

	private void addLangParam() {
		if (uri != null) {
			/**
			 * first check for addFordLangParam, because it's possible that both flags are true most of 
			 * the times
			 */
			if (addFordLangParam) {
				uri = uri + "&lang=" + fordLocaleCode;
				
			} else if (addLangParam) {
				uri = uri + "&lang=" + localeCode;
			}
			Log.d(TAG, "uri="+uri);
		}
	}
	
	public static void updateLocaleCode(String newLocaleCode) {
		localeCode = newLocaleCode;
	}
	
	public static void updateFordLocaleCode(String newFordLocaleCode) {
		fordLocaleCode = newFordLocaleCode;
	}
	
	protected JSONObject execute(RequestMethod requestMethod, ContentType contentType, byte[] data) throws ClientProtocolException, IOException, JSONException {
		/*JSONObject jsonObject = null;
		HttpClient httpClient = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost(uri);

        HttpResponse response = httpClient.execute(httpPost);
		HttpEntity entity = response.getEntity();
		String text = EntityUtils.toString(entity, AppConstants.CHARSET_NAME);
		
		jsonObject = new JSONObject(text);
		httpClient.getConnectionManager().shutdown();
		return jsonObject;*/
		/**
		 * For multi-Language support in api calls we are adding 'iso2' parameter
		 * using setLocale method.
		 */
		addLangParam();
		GoogleAnalyticsTracker.getInstance().sendApiCall(EventSeekr.getEventSeekr(), uri, data);

		JSONObject jsonObject;
		URL url = new URL(uri);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(100000);
		conn.setReadTimeout(100000);
		conn.setRequestMethod(requestMethod.name());
		conn.setRequestProperty("Charset", AppConstants.CHARSET_NAME);
		
		if (requestMethod == RequestMethod.POST) {
			conn.setRequestProperty("Content-Type", contentType.toString());
			conn.setRequestProperty("Content-Length", "" + Integer.toString(data != null ? data.length : 0));
			conn.setUseCaches(false);
			conn.setDoOutput(true);
			
			final DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
			if (data != null) {
				wr.write(data);
			}
			wr.flush();
			wr.close();
		}
		/*if (uri.contains(Type.myevents.name())) {
			Log.d(TAG, "b4 load time = " + (System.currentTimeMillis() / 1000));
		}*/
		try {
			InputStream in = new BufferedInputStream(conn.getInputStream());
			/*if (uri.contains(Type.myevents.name())) {
				Log.d(TAG, "after load time = " + (System.currentTimeMillis() / 1000));
			}*/
			String result = readStream(in);
			jsonObject = new JSONObject(result);
			return jsonObject;

		} finally {
			conn.disconnect();
		}
	}
	
	/*protected JSONObject execute() throws IOException, JSONException {
		JSONObject jsonObject;
		URL url = new URL(uri);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(100000);
		conn.setReadTimeout(100000);
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Charset", AppConstants.CHARSET_NAME);
		try {
			InputStream in = new BufferedInputStream(conn.getInputStream());
			String result = readStream(in);
			jsonObject = new JSONObject(result);
			return jsonObject;

		} finally {
			conn.disconnect();
		}
	}*/
	
	private String readStream(InputStream in) {
		StringBuilder builder = new StringBuilder();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(in));
			String line = "";
			while ((line = reader.readLine()) != null) {
				builder.append(line + "\n");
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return builder.toString();
	}
}
