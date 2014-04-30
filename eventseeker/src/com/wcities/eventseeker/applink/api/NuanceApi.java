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

	private static final String NUANCE_APPKEY = "9f31e5fc9ddeae9882299d407f336728815126977e295c53ca9fbeb53d5b80dc8d65d82a4386c2c5b060052f936d5714be7dac3ca6088f70a74d26e4a9a6c136";

	private static final String COMMON_URL = NUANCE_HOST + "/NMDPAsrCmdServlet/dictation?";

	private static final String APP_ID = "NMDPTRIAL_aazimparkar20130905101221";
	
	private static final long ID = 20130905101221L;
		
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
