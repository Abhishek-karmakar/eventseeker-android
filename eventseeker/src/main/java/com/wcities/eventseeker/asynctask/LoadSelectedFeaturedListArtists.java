package com.wcities.eventseeker.asynctask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.widget.BaseAdapter;

import com.wcities.eventseeker.adapter.ArtistListAdapterWithoutIndexer;
import com.wcities.eventseeker.api.ArtistApi;
import com.wcities.eventseeker.api.ArtistApi.Method;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.interfaces.ArtistAdapterListener;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.jsonparser.ArtistApiJSONParser;

public class LoadSelectedFeaturedListArtists extends AsyncTask<Void, Void, List<Artist>> {
	
	private List<Artist> artistList;
	private String oauthToken;
	private int id;
	private double lat;
	private double lon;
	private String wcitiesId;
	
	private ArtistAdapterListener artistListAdapter;
	private AsyncTaskListener<Void> asyncTaskListener;
	
	/**
	 * 'ARTISTS_LIMIT' is not needed in this call as in this call we will get all the available Artists
	 * in single api call. So, no need to do lazy loading.
	 */
	//private ARTISTS_LIMIT = 10;
	
	public LoadSelectedFeaturedListArtists(String oauthToken, List<Artist> artistList, 
			 int id, double lat, double lon, ArtistAdapterListener artistListAdapter,
			AsyncTaskListener<Void> asyncTaskListener, String wcitiesId) {
		this.oauthToken = oauthToken;
		this.artistList = artistList;
		this.id = id;
		this.lat = lat;
		this.lon = lon;
		this.artistListAdapter = artistListAdapter;
		this.asyncTaskListener = asyncTaskListener;
		this.wcitiesId = wcitiesId;
	}

	@Override
	protected List<Artist> doInBackground(Void... params) {
		List<Artist> tmpArtists = new ArrayList<Artist>();
		
		ArtistApi artistApi = new ArtistApi(oauthToken);
		artistApi.setMethod(Method.featuredList);
		artistApi.setFeaturedListId(id);
		artistApi.setUserId(wcitiesId);
		artistApi.setLat(lat);
		artistApi.setLon(lon);

		try {
			JSONObject jsonObject = artistApi.getArtists();
			ArtistApiJSONParser jsonParser = new ArtistApiJSONParser();
			tmpArtists = jsonParser.getFeaturedListArtistsDetailsList(jsonObject);

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
		/**
		 * 'ARTISTS_LIMIT' is not needed in this call as in this call we will get all the available Artists
		 * in single api call. So, no need to do lazy loading.
		 */
		if (!tmpArtists.isEmpty()) {
			artistListAdapter.setArtistsAlreadyRequested(artistListAdapter.getArtistsAlreadyRequested() + tmpArtists.size());
			artistList.addAll(artistList.size() - 1, tmpArtists);
		}
		artistList.remove(artistList.size() - 1);
		artistListAdapter.setMoreDataAvailable(false);
		((BaseAdapter) artistListAdapter).notifyDataSetChanged();
		
		if (asyncTaskListener != null) {
			asyncTaskListener.onTaskCompleted();
		}
	}    	
}