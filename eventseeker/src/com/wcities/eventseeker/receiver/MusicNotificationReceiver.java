package com.wcities.eventseeker.receiver;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Calendar;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;

import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.ArtistApi;
import com.wcities.eventseeker.api.ArtistApi.Method;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.gcm.GcmBroadcastReceiver.NotificationType;
import com.wcities.eventseeker.jsonparser.ArtistApiJSONParser;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.NotificationUtil;

public class MusicNotificationReceiver extends BroadcastReceiver {

	private static final String TAG = MusicNotificationReceiver.class.getName();
	private static final String XTRA_PLAYING = "playing";
	
	private static String strArtist;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		//String action = intent.getAction();
		//Log.d(TAG, action + ", playing = " + intent.getBooleanExtra("playing", false));
		String artist = intent.getStringExtra(MediaStore.Audio.ArtistColumns.ARTIST);
		if (artist == null) {
			return;
		}
		//String album = intent.getStringExtra(MediaStore.Audio.AlbumColumns.ALBUM);
		//String track = intent.getStringExtra(MediaStore.Audio.AudioColumns.TRACK);
		//Log.d(TAG, "Artist: " + artist);
		
		//if (strArtist == null || !strArtist.equals(artist)) {
		if (intent.hasExtra(XTRA_PLAYING) && intent.getBooleanExtra(XTRA_PLAYING, false)) {
			strArtist = artist;
			new LoadArtistEvent(context, Api.OAUTH_TOKEN).execute();
		}
	}
	
	private static class LoadArtistEvent extends AsyncTask<Void, Void, Artist> {
		
		private static final int TIME_LIMIT_IN_DAYS = 7;
		private static final int EVENT_LIMIT = 1;
		private static final int MILES_LIMIT = 50;
		
		public Context context;
		private String oauthToken;
		
		public LoadArtistEvent(Context context, String oauthToken) {
			this.context = context;
			this.oauthToken = oauthToken;
		}

		@Override
		protected Artist doInBackground(Void... params) {
			//Log.d(TAG, "LoadArtistEvent");

			try {
				ArtistApi artistApi = new ArtistApi(oauthToken);
				artistApi.setMethod(Method.artistEvent);
				artistApi.setArtist(URLEncoder.encode(strArtist, AppConstants.CHARSET_NAME));
				artistApi.setPlayingArtistEnabled(true);
				artistApi.setVenueDetailEnabled(true);

				Calendar c = Calendar.getInstance();
				c.add(Calendar.DATE, TIME_LIMIT_IN_DAYS);
				int year = c.get(Calendar.YEAR);
				int month = c.get(Calendar.MONTH);
				int day = c.get(Calendar.DAY_OF_MONTH);
				String endDate = ConversionUtil.getDay(year, month, day);
				artistApi.setEndDate(endDate);
				
				double[] latLng = DeviceUtil.getLatLon((EventSeekr) context.getApplicationContext());
				artistApi.setLat(latLng[0]);
				artistApi.setLon(latLng[1]);
				
				artistApi.setMiles(MILES_LIMIT);
				artistApi.setLimit(EVENT_LIMIT);
				
				JSONObject jsonObject = artistApi.getArtists();
				ArtistApiJSONParser jsonParser = new ArtistApiJSONParser();
				Artist artist = jsonParser.getArtistUpcomingEvent(jsonObject);
				
				return artist;

			} catch (ClientProtocolException e) {
				e.printStackTrace();
				
			} catch (IOException e) {
				e.printStackTrace();
				
			} catch (JSONException e) {
				e.printStackTrace();
			}

			return null;
		}
		
		@Override
		protected void onPostExecute(Artist artist) {
			if (artist != null && !artist.getEvents().isEmpty()) {
				String message = artist.getName() + " is performing for an event " + artist.getEvents().get(0).getName();
				NotificationUtil.addNotification(context, message, AppConstants.MUSIC_NOTIFICATION_ID, 
						NotificationType.EVENT_DETAILS, artist.getEvents().get(0));
			}
		}    	
    }
}
