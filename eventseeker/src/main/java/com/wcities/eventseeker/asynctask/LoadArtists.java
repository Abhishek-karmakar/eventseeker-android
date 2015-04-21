package com.wcities.eventseeker.asynctask;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView.Adapter;
import android.widget.BaseAdapter;

import com.wcities.eventseeker.adapter.ArtistListAdapter;
import com.wcities.eventseeker.adapter.RVSearchArtistsAdapterTab;
import com.wcities.eventseeker.api.ArtistApi;
import com.wcities.eventseeker.api.ArtistApi.Method;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.interfaces.ArtistAdapterListener;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.jsonparser.ArtistApiJSONParser;

public class LoadArtists extends AsyncTask<String, Void, List<Artist>> {
	
	private List<Artist> artistList;
	private ArtistAdapterListener<String> artistAdapterListener;
	private String oauthToken;
	private final int ARTISTS_LIMIT = 10;
	private String userId;
	
	private AsyncTaskListener<Void> asyncTaskListener;
	
	public LoadArtists(String oauthToken, List<Artist> artistList, ArtistAdapterListener<String> artistAdapterListener, 
			String userId, AsyncTaskListener<Void> asyncTaskListener) {
		this.oauthToken = oauthToken;
		this.artistList = artistList;
		this.artistAdapterListener = artistAdapterListener;
		this.userId = userId;
		this.asyncTaskListener = asyncTaskListener;
	}

	@Override
	protected List<Artist> doInBackground(String... params) {
		List<Artist> tmpArtists = new ArrayList<Artist>();
		ArtistApi artistApi = new ArtistApi(oauthToken);
		artistApi.setLimit(ARTISTS_LIMIT);
		artistApi.setAlreadyRequested(artistAdapterListener.getArtistsAlreadyRequested());
		artistApi.setMethod(Method.artistSearch);
		artistApi.setTrackingEnabled(true);
		artistApi.setUserId(userId);

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
			artistAdapterListener.setArtistsAlreadyRequested(artistAdapterListener.getArtistsAlreadyRequested() 
					+ tmpArtists.size());
			
			if (tmpArtists.size() < ARTISTS_LIMIT) {
				artistAdapterListener.setMoreDataAvailable(false);
				artistList.remove(artistList.size() - 1);
			}
			
		} else {
			artistAdapterListener.setMoreDataAvailable(false);
			artistList.remove(artistList.size() - 1);
			if (artistList.isEmpty()) {
				artistList.add(new Artist(AppConstants.INVALID_ID, null));
			}
		}
		
		if (artistAdapterListener instanceof BaseAdapter) {
			((BaseAdapter) artistAdapterListener).notifyDataSetChanged();
			
		} else {
			((Adapter<RVSearchArtistsAdapterTab.ViewHolder>) artistAdapterListener).notifyDataSetChanged();
		}
		
		if (asyncTaskListener != null) {
			asyncTaskListener.onTaskCompleted();
		}
	}    	
}