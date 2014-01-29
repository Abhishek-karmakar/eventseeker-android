package com.wcities.eventseeker.asynctask;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.support.v4.app.Fragment;

import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.ArtistApi;
import com.wcities.eventseeker.api.ArtistApi.Method;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.jsonparser.ArtistApiJSONParser;
import com.wcities.eventseeker.util.FragmentUtil;

public class LoadArtistDetails extends AsyncTask<Void, Void, Void> {
	
	private Artist artist;
	private Fragment fragment;
	private OnArtistUpdatedListener listener;

	public LoadArtistDetails(Artist artist, OnArtistUpdatedListener listener, Fragment fragment) {
		this.artist = artist;
		this.listener = listener;
		this.fragment = fragment;
	}
	
	public interface OnArtistUpdatedListener {
		public void onArtistUpdated();
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		ArtistApi artistApi = new ArtistApi(Api.OAUTH_TOKEN);
		artistApi.setArtistId(artist.getId());
		artistApi.setMethod(Method.artistDetail);
		// null check is not required here, since if it's null, that's handled from eventApi
		artistApi.setUserId(((EventSeekr)FragmentUtil.getActivity(fragment).getApplication()).getWcitiesId());
		artistApi.setFriendsEnabled(true);
		
		try {
			JSONObject jsonObject = artistApi.getArtists();
			ArtistApiJSONParser jsonParser = new ArtistApiJSONParser();
			jsonParser.fillArtistDetails(artist, jsonObject);
			
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
	protected void onPostExecute(Void result) {
		//Log.d(TAG, "LoadEventDetails onPostExecute()");
		listener.onArtistUpdated();
	}    	
}