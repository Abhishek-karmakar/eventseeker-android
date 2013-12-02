package com.wcities.eventseeker.receiver;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;

import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.ArtistApi;
import com.wcities.eventseeker.api.ArtistApi.Method;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.jsonparser.ArtistApiJSONParser;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.NotificationUtil;

public class MusicNotificationReceiver extends BroadcastReceiver {

	private static final String TAG = MusicNotificationReceiver.class.getName();
	private static final int NOTIFICATION_ID = 1;
	
	private static String strArtist;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		Log.d(TAG, action);
		String artist = intent.getStringExtra(MediaStore.Audio.ArtistColumns.ARTIST);
		if (artist == null) {
			return;
		}
		//String album = intent.getStringExtra(MediaStore.Audio.AlbumColumns.ALBUM);
		//String track = intent.getStringExtra(MediaStore.Audio.AudioColumns.TRACK);
		Log.d(TAG, "Artist: " + artist);
		
		if (strArtist == null || !strArtist.equals(artist)) {
			strArtist = artist;
			new LoadArtistEvent(context).execute();
		}
	}
	
	private static class LoadArtistEvent extends AsyncTask<Void, Void, Artist> {
		
		private static final int TIME_LIMIT_IN_DAYS = 7;
		private static final int EVENT_LIMIT = 1;
		
		public Context context;
		
		public LoadArtistEvent(Context context) {
			this.context = context;
		}

		@Override
		protected Artist doInBackground(Void... params) {
			Log.d(TAG, "create notification");
			List<Artist> tmpArtists = new ArrayList<Artist>();
			ArtistApi artistApi = new ArtistApi(Api.OAUTH_TOKEN);
			artistApi.setExactSearchEnabled(true);
			artistApi.setMethod(Method.artistSearch);

			try {
				artistApi.setArtist(URLEncoder.encode(strArtist, AppConstants.CHARSET_NAME));

				JSONObject jsonObject = artistApi.getArtists();
				ArtistApiJSONParser jsonParser = new ArtistApiJSONParser();
				
				tmpArtists = jsonParser.getArtistList(jsonObject);
				
				artistApi = new ArtistApi(Api.OAUTH_TOKEN);
				artistApi.setMethod(Method.artistEvent);
				artistApi.setLimit(EVENT_LIMIT);

				Calendar c = Calendar.getInstance();
				c.add(Calendar.DATE, TIME_LIMIT_IN_DAYS);
	      		int year = c.get(Calendar.YEAR);
	      		int month = c.get(Calendar.MONTH);
	      		int day = c.get(Calendar.DAY_OF_MONTH);
	      		String endDate = ConversionUtil.getDay(year, month, day);
				artistApi.setEndDate(endDate);

				for (Iterator<Artist> iterator = tmpArtists.iterator(); iterator.hasNext();) {
					Artist artist = (Artist) iterator.next();
					artistApi.setArtistId(artist.getId());
					
					jsonObject = artistApi.getArtists();
					Event event = jsonParser.getArtistUpcomingEvent(jsonObject);
					
					if (event != null) {
						List<Event> events = new ArrayList<Event>();
						events.add(event);
						artist.setEvents(events);
						return artist;
					}
				}

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
			if (artist != null) {
				String message = artist.getName() + " is performing for an event " + artist.getEvents().get(0).getName();
				NotificationUtil.addNotification(context, artist.getEvents().get(0), message, NOTIFICATION_ID);
			}
		}    	
    }
}
