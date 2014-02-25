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

import com.wcities.eventseeker.constants.AppConstants;

public abstract class Api {
	
	private static final String TAG = Api.class.getName();
	
	// provided by Samir after attaching italic tags with description
	public static final String OAUTH_TOKEN = "a82d32bd99044507417055f66c1a614c"; 
											//"5c63440e7db1ad33c3898cdac3405b1e";
	protected static final String COMMON_URL = "http://dev.wcities.com/V3/";
	
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
		
		try {
			InputStream in = new BufferedInputStream(conn.getInputStream());
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
