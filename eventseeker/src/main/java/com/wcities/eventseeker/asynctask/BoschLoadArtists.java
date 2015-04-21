package com.wcities.eventseeker.asynctask;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;

import com.wcities.eventseeker.api.ArtistApi;
import com.wcities.eventseeker.api.ArtistApi.Method;
import com.wcities.eventseeker.bosch.adapter.BoschArtistListAdapter;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.jsonparser.ArtistApiJSONParser;

public class BoschLoadArtists extends AsyncTask<String, Void, List<Artist>> {
	
	private List<Artist> artistList;
	private BoschArtistListAdapter<String> artistListAdapter;
	private String oauthToken;
	private final int ARTISTS_LIMIT = 10;
	
	public BoschLoadArtists(String oauthToken, List<Artist> artistList, BoschArtistListAdapter<String> artistListAdapter) {
		this.oauthToken = oauthToken;
		this.artistList = artistList;
		this.artistListAdapter = artistListAdapter;
	}

	@Override
	protected List<Artist> doInBackground(String... params) {
		List<Artist> tmpArtists = new ArrayList<Artist>();
		ArtistApi artistApi = new ArtistApi(oauthToken);
		artistApi.setLimit(ARTISTS_LIMIT);
		artistApi.setAlreadyRequested(artistListAdapter.getArtistsAlreadyRequested());
		artistApi.setMethod(Method.artistSearch);

		try {
			artistApi.setArtist(URLEncoder.encode(params[0], AppConstants.CHARSET_NAME));

			JSONObject jsonObject = artistApi.getArtists();
			ArtistApiJSONParser jsonParser = new ArtistApiJSONParser();
			
			tmpArtists = jsonParser.getArtistList(jsonObject);

		} catch (ClientProtocolException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
			
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return tmpArtists;
	}
	
	@Override
	protected void onPostExecute(List<Artist> tmpArtists) {
		if (!tmpArtists.isEmpty()) {
			artistList.addAll(artistList.size() - 1, tmpArtists);
			artistListAdapter.setArtistsAlreadyRequested(artistListAdapter.getArtistsAlreadyRequested() 
					+ tmpArtists.size());
			
			if (tmpArtists.size() < ARTISTS_LIMIT) {
				artistListAdapter.setMoreDataAvailable(false);
				artistList.remove(artistList.size() - 1);
			}
			
		} else {
			artistListAdapter.setMoreDataAvailable(false);
			artistList.remove(artistList.size() - 1);
			if(artistList.isEmpty()) {
				artistList.add(new Artist(AppConstants.INVALID_ID, null));
			}
		}
		artistListAdapter.notifyDataSetChanged();
	}    	
}