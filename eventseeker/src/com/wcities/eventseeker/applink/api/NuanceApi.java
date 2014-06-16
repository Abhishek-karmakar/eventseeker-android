package com.wcities.eventseeker.applink.api;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import android.util.Log;

public class NuanceApi {
	
	private static final String TAG = NuanceApi.class.getName();

	private static final String NUANCE_HOST = "https://dictation.nuancemobility.net";

	private static final String NUANCE_APPKEY = "1a3a8794d0431767224ac1b0ee67fd34d3992dddbc55688a49d7ef27ae7221c8aca95c7cec74e30fbea5bfac0cbce0e0550dbd1162259cabfc8bd3abfdf473a9";

	private static final String COMMON_URL = NUANCE_HOST + "/NMDPAsrCmdServlet/dictation?";

	private static final String APP_ID = "HTTP_NMDPPRODUCTION_wcities_eventseeker_20140519131346";
	
	private static final long ID = 20140519131346L;
		
	//private static String ACCEPT_TYPE = "text/plain";
	
	//private static int XDictation_NBestListSize = 1;
	
	private static enum ContentType {
		AUDIO_CONTENT_TYPE;
		
		public String toString() {
			return "audio/x-wav;codec=pcm;bit=16;rate=8000";
		};
	};
	
	public static final String CHARSET_NAME = "UTF-8";
	public static final String RequestMethod = "POST";
	public String uri;
	
	public String execute(ByteArrayOutputStream audioDataBAO) throws IOException{
		
		uri = COMMON_URL + "appId=" + APP_ID + "&appKey=" + NUANCE_APPKEY + "&id=" + ID;
		
		Log.d(TAG, "uri : " + uri);
		
		URL url = new URL(uri);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(20000);
		conn.setReadTimeout(20000);
		conn.setRequestMethod(RequestMethod);
		conn.setRequestProperty("Charset", CHARSET_NAME);
		conn.setRequestProperty("Content-Type", ContentType.AUDIO_CONTENT_TYPE.toString()
				/*"audio/x-wav;codec=pcm;bit=16;rate=8000"*/);
		conn.setRequestProperty("Content-Length", Integer.toString(audioDataBAO.size()));
		conn.setRequestProperty("X-Dictation-NBestListSize", "1");
		conn.setUseCaches(false);
		conn.setDoOutput(true);
		
		final DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
		if (audioDataBAO != null) {
			//wr.write(audioDataBAO.toByteArray());
			audioDataBAO.writeTo(wr);
			audioDataBAO.close();
		}
		wr.flush();
		wr.close();

		String result = null;
		try {
			InputStream in = new BufferedInputStream(conn.getInputStream());
			result = readStream(in);

		} catch (Exception e) {
			e.printStackTrace();
			result = null;
		} finally {
			conn.disconnect();
		}
		return result;
	}
	
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
