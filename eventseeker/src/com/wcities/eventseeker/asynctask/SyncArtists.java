package com.wcities.eventseeker.asynctask;

import java.io.IOException;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.support.v4.app.Fragment;

import com.wcities.eventseeker.ConnectAccountsFragment.Service;
import com.wcities.eventseeker.api.ArtistApi;
import com.wcities.eventseeker.api.ArtistApi.Method;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.interfaces.OnFragmentAliveListener;
import com.wcities.eventseeker.jsonparser.ArtistApiJSONParser;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.LogUtil;

public class SyncArtists extends AsyncTask<Void, Void, Void> {
	
	private static final String TAG = SyncArtists.class.getSimpleName();
	
	private static final int MAX_BATCH_SEARCH_LIMIT = 250;
	
	private List<String> artistNames;
	private EventSeekr eventSeekr;
	private Service service;
	private Fragment fragment;
	private String oauthToken;
	private String artistSource;
	
	public SyncArtists(String oauthToken, List<String> artistNames, EventSeekr eventSeekr, Service service, Fragment fragment, 
			String artistSource) {
		this.oauthToken = oauthToken;
		this.artistNames = artistNames;
		this.eventSeekr = eventSeekr;
		this.service = service;
		this.fragment = fragment;
		this.artistSource = artistSource;
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		//Log.d(TAG, "doInBackground()");
		int synchedArtistsCount = 0;
		int artistSize = artistNames.size();
		int calls = (artistSize / MAX_BATCH_SEARCH_LIMIT) + (artistSize % MAX_BATCH_SEARCH_LIMIT > 0 ? 1 : 0);
		for (int call = 0; call < calls; call++) {
			ArtistApi artistApi = new ArtistApi(oauthToken);
			artistApi.setExactSearchEnabled(true);
			artistApi.setMethod(Method.artistSearch);
			artistApi.setUserId(eventSeekr.getWcitiesId());
			artistApi.setArtistSource(artistSource);

			int startIndex = call * MAX_BATCH_SEARCH_LIMIT;
			int maxEndIndex = startIndex + MAX_BATCH_SEARCH_LIMIT;
			try {
				JSONObject jsonObject = artistApi.getArtists(artistNames, startIndex, maxEndIndex);
				//Log.d(TAG, jsonObject.toString());
				ArtistApiJSONParser jsonParser = new ArtistApiJSONParser();
				
				synchedArtistsCount += jsonParser.getBatchArtistCount(jsonObject);

			} catch (ClientProtocolException e) {
				e.printStackTrace();
				
			} catch (IOException e) {
				e.printStackTrace();
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		eventSeekr.setSyncCount(service, synchedArtistsCount);

		return null;
	}
	
	@Override
	protected void onPostExecute(Void result) {
		super.onPostExecute(result);
		if (fragment != null) {
			if (fragment instanceof OnFragmentAliveListener && ((OnFragmentAliveListener)fragment).isAlive()) {
				//Log.d(TAG, "isAlive = " + ((OnFragmentAliveListener)fragment).isAlive());
				FragmentUtil.getActivity(fragment).onBackPressed();
			}
		}
	}
}
