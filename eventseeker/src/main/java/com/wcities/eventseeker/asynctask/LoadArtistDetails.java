package com.wcities.eventseeker.asynctask;

import android.os.AsyncTask;
import android.support.v4.app.Fragment;

import com.wcities.eventseeker.api.ArtistApi;
import com.wcities.eventseeker.api.ArtistApi.Method;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.jsonparser.ArtistApiJSONParser;
import com.wcities.eventseeker.util.FragmentUtil;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class LoadArtistDetails extends AsyncTask<Void, Void, Void> {
	
	private Artist artist;
	private Fragment fragment;
	private OnArtistUpdatedListener listener;
	private String oauthToken;
	private boolean addSrcFromNotification;
	
	public interface OnArtistUpdatedListener {
		public void onArtistUpdated();
	}

	public LoadArtistDetails(String oauthToken, Artist artist, OnArtistUpdatedListener listener, Fragment fragment) {
		this.oauthToken = oauthToken;
		this.artist = artist;
		this.listener = listener;
		this.fragment = fragment;
	}
	
	public void setAddSrcFromNotification(boolean addSrcFromNotification) {
		this.addSrcFromNotification = addSrcFromNotification;
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		ArtistApi artistApi = new ArtistApi(oauthToken);
		artistApi.setArtistId(artist.getId());
		artistApi.setMethod(Method.artistDetail);
		// null check is not required here, since if it's null, that's handled from eventApi
		artistApi.setUserId(((EventSeekr)FragmentUtil.getActivity(fragment).getApplication()).getWcitiesId());
		artistApi.setFriendsEnabled(true);
		artistApi.setSrcFromNotification(addSrcFromNotification);
		
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