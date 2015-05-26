package com.wcities.eventseeker.asynctask;

import android.os.AsyncTask;

import com.wcities.eventseeker.adapter.RVPopularArtistsAdapter;
import com.wcities.eventseeker.api.ArtistApi;
import com.wcities.eventseeker.api.ArtistApi.Method;
import com.wcities.eventseeker.core.PopularArtistCategory;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.jsonparser.ArtistApiJSONParser;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LoadPopularArtists extends AsyncTask<Void, Void, List<PopularArtistCategory>> {
	
	private List<PopularArtistCategory> popularArtistCategories;
	private String oauthToken;
	private double lat;
	private double lon;
	private final int ARTISTS_LIMIT = 10;
	
	private RVPopularArtistsAdapter popularArtistsAdapter;
	private AsyncTaskListener<Void> asyncTaskListener;

	public LoadPopularArtists(String oauthToken, List<PopularArtistCategory> popularArtistCategories, 
			double lat, double lon, RVPopularArtistsAdapter popularArtistsAdapter,  
			AsyncTaskListener<Void> asyncTaskListener) {
		this.oauthToken = oauthToken;
		this.popularArtistCategories = popularArtistCategories;
		this.lat = lat;
		this.lon = lon;
		this.popularArtistsAdapter = popularArtistsAdapter;
		this.asyncTaskListener = asyncTaskListener;
	}

	@Override
	protected List<PopularArtistCategory> doInBackground(Void... params) {
		List<PopularArtistCategory> tmpFeaturedListArtistCategories = new ArrayList<PopularArtistCategory>();
		
		ArtistApi artistApi = new ArtistApi(oauthToken);
		artistApi.setMethod(Method.featuredList);
		artistApi.setLat(lat);
		artistApi.setLon(lon);
		artistApi.setAlreadyRequested(popularArtistsAdapter.getArtistsAlreadyRequested());
		artistApi.setLimit(ARTISTS_LIMIT);

		try {
			JSONObject jsonObject = artistApi.getArtists();
			ArtistApiJSONParser jsonParser = new ArtistApiJSONParser();
			tmpFeaturedListArtistCategories = jsonParser.getFeaturedListArtistCategories(jsonObject);

		} catch (ClientProtocolException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
			
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return tmpFeaturedListArtistCategories;
	}
	
	@Override
	protected void onPostExecute(List<PopularArtistCategory> tmpFeaturedListArtistCategories) {
		if (!tmpFeaturedListArtistCategories.isEmpty()) {
			popularArtistCategories.addAll(popularArtistCategories.size() - 1, tmpFeaturedListArtistCategories);
			popularArtistsAdapter.setArtistsAlreadyRequested(popularArtistsAdapter.getArtistsAlreadyRequested() 
				+ tmpFeaturedListArtistCategories.size());
			
			if (tmpFeaturedListArtistCategories.size() < ARTISTS_LIMIT) {
				popularArtistsAdapter.setMoreDataAvailable(false);
				popularArtistCategories.remove(popularArtistCategories.size() - 1);
				//adding Popular Artists Categories
				popularArtistCategories.addAll(PopularArtistCategory.getPopularArtistCategories());
			}
			
		} else {
			popularArtistsAdapter.setMoreDataAvailable(false);
			popularArtistCategories.remove(popularArtistCategories.size() - 1);
			//adding Popular Artists Categories
			popularArtistCategories.addAll(PopularArtistCategory.getPopularArtistCategories());
		}
		popularArtistsAdapter.notifyDataSetChanged();
		
		if (asyncTaskListener != null) {
			asyncTaskListener.onTaskCompleted();
		}
	}    	
}