package com.android.gm.api;

import android.content.Context;
import android.text.TextUtils;

import com.android.gm.api.comm.GmHttpClient;
import com.android.gm.api.comm.SimpleForm;
import com.android.gm.api.exception.InvalidGooglePlayMusicAccountException;
import com.android.gm.api.lib.RequestParams;
import com.android.gm.api.model.AddPlaylistResponse;
import com.android.gm.api.model.Playlist;
import com.android.gm.api.model.Playlists;
import com.android.gm.api.model.QueryResults;
import com.android.gm.api.model.Song;
import com.loopj.android.http.PersistentCookieStore;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ByteArrayEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class GoogleMusicApi
{
	private static final String TAG = GoogleMusicApi.class.getName();
	
	private static PersistentCookieStore mCookieStore;
	private static GmHttpClient mHttpClient;
	private static GoogleMusicApi mInstance;

	public static void createInstance(Context context)
	{
		getInstance(context);
	}

	public static GoogleMusicApi getInstance(Context context)
	{
		if(mInstance == null)
			mInstance = new GoogleMusicApi(context);
		return mInstance;
	}

	private GoogleMusicApi(Context context)
	{
		mHttpClient = new GmHttpClient();

		mCookieStore = new PersistentCookieStore(context.getApplicationContext());
		mHttpClient.setCookieStore(mCookieStore);
		mHttpClient.setUserAgent("");
	}

	public static final HttpClient getRawHttpClient()
	{
		return mHttpClient.getHttpClient();
	}

	public static final void setAuthorizationHeader(String authToken)
	{
		mHttpClient.addHeader("Authorization", "GoogleLogin auth=" + authToken);
	}

	public static final void setUserAgent(String userAgent)
	{
		mHttpClient.setUserAgent(userAgent);
	}

	public static final boolean login(Context context, String authToken)
	{
		if(!TextUtils.isEmpty(authToken))
		{
			SimpleForm form = new SimpleForm().close();
			GoogleMusicApi.setAuthorizationHeader(authToken);
			String response = mHttpClient.post(context, "https://play.google.com/music/listen?hl=en&u=0", new ByteArrayEntity(form.toString().getBytes()), form.getContentType());
			return true;
		}
		else
			return false;
	}

	public static final boolean login(Context context, String email, String password)
	{

		SimpleForm form = new SimpleForm();
		form.addField("service", "sj");
		form.addField("Email", email);
		form.addField("Passwd", password);
		form.close();

		mHttpClient.getHttpClient().getParams().removeParameter("Authorization");

		String response = mHttpClient.post(context, "https://www.google.com/accounts/clientlogin", new ByteArrayEntity(form.toString().getBytes()), form.getContentType());

		if(mHttpClient.getResponseCode() == HttpStatus.SC_OK)
		{
			int startIndex = response.indexOf("Auth=") + "Auth=".length();
			int endIndex = response.indexOf("\n", startIndex);

			String authToken = response.substring(startIndex, endIndex).trim();
			return login(context, authToken);
		}

		return false;
	}

	public static final Playlists getAllPlaylist(Context context) throws JSONException
	{

		String response = getPlaylistHelper(context, null);

		JSONObject jsonObject = new JSONObject(response);

		return new Playlists().fromJsonObject(jsonObject);
	}

	public static final URI getSongStream(Song song) throws JSONException, URISyntaxException
	{

		RequestParams params = new RequestParams();
		params.put("u", "0");
		params.put("songid", song.getId());
		params.put("pt", "e");

		String response = mHttpClient.get("https://play.google.com/music/play", params);

		JSONObject jsonObject = new JSONObject(response);

		return new URI(jsonObject.optString("url", null));
	}

	public static final ArrayList<Song> getAllSongs(Context context) throws JSONException, 
			InvalidGooglePlayMusicAccountException
	{
		return getSongs(context, "");
	}

	public static final ArrayList<Song> getSongs(Context context, String continuationToken) throws 
			JSONException, InvalidGooglePlayMusicAccountException
	{

		SimpleForm form = new SimpleForm();
		form.addField("json", "{\"continuationToken\":\"" + continuationToken + "\"}");
		form.close();
		String response = mHttpClient.post(context, "https://play.google.com/music/services/loadalltracks?u=0&xt=" + getXtCookieValue(), new ByteArrayEntity(form.toString().getBytes()), form.getContentType());

		if (response == null) {
			/**
			 * Google play music service is available in selected territories only. When we use any google 
			 * account which is not registered for google play music on https://play.google.com/music/listen#/now,
			 * it returns null response.
			 */
			throw new InvalidGooglePlayMusicAccountException();
		}
		
		JSONObject jsonObject = new JSONObject(response);
		Playlist playlist = new Playlist().fromJsonObject(jsonObject);

		ArrayList<Song> chunkedSongList = new ArrayList<Song>();
		chunkedSongList.addAll(playlist.getPlaylist());

		if(!TextUtils.isEmpty(playlist.getContinuationToken()))
			chunkedSongList.addAll(getSongs(context, playlist.getContinuationToken()));

		return chunkedSongList;
	}
	
	public static final List<String> getAllArtistsNames(Context context) throws JSONException, InvalidGooglePlayMusicAccountException, 
			Exception {
		return getArtistNames(context, "");
	}
	
	private static final List<String> getArtistNames(Context context, String continuationToken) throws JSONException,
			InvalidGooglePlayMusicAccountException, Exception {
		SimpleForm form = new SimpleForm();
		form.addField("json", "{\"continuationToken\":\"" + continuationToken + "\"}");
		form.addField("format", "jsarray");
		form.close();
		//Log.d(TAG, "form = " + form.toString());
		
		/**
		 * Sample response: "<html><head></head><body><script>window.parent['slat_progress'](0.02);</script><script type='text/javascript'>window.parent['slat_process']([[[\"6ab2602d-b738-3fd4-891d-088573bb3206\",\"Mirrors\",\"//lh3.googleusercontent.com/6WWVst0780Lu5wG8kj1elfYwtpP4Oms5ptlgn3fzuD1FMGIPTAMwOIREE36KMBuZhFXGXtjh0Hs\",\"Justin Timberlake\",\"The 20/20 Experience\",\"Justin Timberlake\",],[\"e1e026ac-a7e9-3afb-a1af-87a8ad1c8786\",\"Tom Ford\",\"//lh5.googleusercontent.com/QlPIvhBUoCogdtePPgfmGZEdTH3c5PMlWby6ygS3K3BcKR7s2GYbUDZbIO9R0G6FRwIqxLJhvh0\",\"JAY Z\",\"Magna Carta... Holy Grail\",\"JAY Z\",],[\"c06058a6-951d-30bf-98d8-0c280aba0629\",\"Get Lucky\",\"//lh5.googleusercontent.com/BmUW_yAT4c7rAKftbu-R4SJMXFp8xtGAqVzV47S431IRyQBhIUBgYMdW50iWsEykoipb3iwK6A\",\"Daft Punk feat. Pharrell Williams and Nile Rodgers\",\"Random Access Memories\",\"Daft Punk\",]],1393501644111000]);window.parent['slat_progress'](1.0);</script><script type='text/javascript'>window.parent['slat_dispatch'](false);</script></body></html>";
		 */
		String response = mHttpClient.post(context, "https://play.google.com/music/services/streamingloadalltracks?u=0&xt="
						+ getXtCookieValue(), new ByteArrayEntity(form.toString().getBytes()), form.getContentType());
		//Log.d(TAG, "response length = " + response.length());
		//Log.d(TAG, "response = " + response.substring(response.length() - 1500));

		if (response == null) {
			/**
			 * Google play music service is available in selected territories
			 * only. When we use any google account which is not registered for
			 * google play music on https://play.google.com/music/listen#/now,
			 * it returns null response.
			 */
			throw new InvalidGooglePlayMusicAccountException();
		}
		
		List<String> artistNames = new ArrayList<String>();

		// these start & end tokens are encountered for each set of 1000 songs, hence we have while loop below.
		String startToken = "['slat_process']("; 
		String endToken = ");"; //"\\\nwindow.parent['slat_progress']";
		int startIndex = response.indexOf(startToken);
		while (startIndex >= 0) {
			//Log.d(TAG, "startIndex >= 0");
			// if response has any artists
			startIndex += startToken.length();
			
			int endIndex = response.indexOf(endToken, startIndex);
			//Log.d(TAG, "startindex = " + startIndex + ", endIndex = " + endIndex);
			//Log.d(TAG, "response substring length = " + response.substring(startIndex, endIndex).length());
			JSONArray jsonArray = new JSONArray(response.substring(startIndex, endIndex));
			parseArtistNames(jsonArray, artistNames);
			
			response = response.substring(endIndex);
			startIndex = response.indexOf(startToken);
		}
		
		return artistNames;
	}
	
	private static void parseArtistNames(JSONArray jsonArray, List<String> artistNames) {
		int count = 0;
		//Log.d(TAG, "parseArtistNames(), length = " + jsonArray.length());
		for (int i = 0; i < jsonArray.length(); i++) {
			try {
				Object ob = jsonArray.get(i);
				
				if (ob instanceof JSONArray) {
					parseArtistNames((JSONArray) ob, artistNames);
					
				} else if (ob instanceof String) {
					// Artist name comes at 6th index
					count++;

					if (count < 6) {
						continue;
						
					} else if (count == 6) {
						//Log.d(TAG, "artist name = " + ob);
						if (!artistNames.contains(ob)) {
							artistNames.add((String) ob);
						}
						break;
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static final String getPlaylistHelper(Context context, String playlistId) throws JSONException
	{

		JSONObject jsonParam = new JSONObject();
		// if playlistId is null, the value will not be put
		jsonParam.putOpt("id", playlistId);

		SimpleForm form = new SimpleForm();
		form.addField("json", jsonParam.toString());
		form.close();

		return mHttpClient.post(context, "https://play.google.com/music/services/loadplaylist?u=0&xt=" + getXtCookieValue(), new ByteArrayEntity(form.toString().getBytes()), form.getContentType());
	}

	private static final String getXtCookieValue()
	{

		for(Cookie cookie : mCookieStore.getCookies())
		{
			if(cookie.getName().equals("xt"))
				return cookie.getValue();
		}

		return null;
	}

	/*
	 * These methods are not used in the Android app. I built them out for completeness.
	 */

	public static final Playlist getPlaylist(Context context, String playlistId) throws JSONException
	{

		if(TextUtils.isEmpty(playlistId))
			throw new IllegalArgumentException("The playlist id parameter cannot be empty.");

		String response = getPlaylistHelper(context, playlistId);

		JSONObject jsonObject = new JSONObject(response);

		return new Playlist().fromJsonObject(jsonObject);
	}

	public final AddPlaylistResponse addPlaylist(Context context, String playlistName) throws JSONException, IllegalArgumentException
	{

		if(TextUtils.isEmpty(playlistName))
			throw new IllegalArgumentException("The playlist name parameter cannot be empty.");

		SimpleForm form = new SimpleForm();
		form.addField("json", "{\"title\":\"" + playlistName + "\"}");
		form.close();

		String response = mHttpClient.post(context, "https://play.google.com/music/services/addplaylist", new ByteArrayEntity(form.toString().getBytes()), form.getContentType());

		JSONObject jsonObject = new JSONObject(response);

		return new AddPlaylistResponse().fromJsonObject(jsonObject);
	}

	public static final String deletePlaylist(Context context, String id) throws JSONException, IllegalArgumentException
	{

		if(TextUtils.isEmpty(id))
			throw new IllegalArgumentException("The id parameter cannot be empty.");

		SimpleForm form = new SimpleForm();
		form.addField("json", "{\"id\":\"" + id + "\"}");
		form.close();

		String response = mHttpClient.post(context, "https://play.google.com/music/services/deletepaylist", new ByteArrayEntity(form.toString().getBytes()), form.getContentType());

		JSONObject jsonObject = new JSONObject(response);

		return jsonObject.optString("deleteId", null);
	}

	public static final QueryResults search(Context context, String query) throws JSONException, IllegalArgumentException
	{

		if(TextUtils.isEmpty(query))
			throw new IllegalArgumentException("The query parameter cannot be empty.");

		JSONObject jsonParam = new JSONObject();
		jsonParam.putOpt("q", query);

		SimpleForm form = new SimpleForm();
		form.addField("json", jsonParam.toString());
		form.close();

		String response = mHttpClient.post(context, "https://play.google.com/music/services/search", new ByteArrayEntity(form.toString().getBytes()), form.getContentType());

		JSONObject jsonObject = new JSONObject(response);

		return new QueryResults().fromJsonObject(jsonObject.optJSONObject("results"));
	}
}
