package com.wcities.eventseeker.asynctask;

import android.os.AsyncTask;
import android.widget.BaseAdapter;

import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.api.UserInfoApi.Type;
import com.wcities.eventseeker.constants.Enums.SortRecommendedArtist;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.ItemsList;
import com.wcities.eventseeker.interfaces.ArtistAdapterListener;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.interfaces.LoadArtistsListener;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LoadRecommendedArtists extends AsyncTask<Void, Void, List<Artist>> {

	private static final String TAG = LoadRecommendedArtists.class.getName();

	private static final int ARTISTS_LIMIT = 10;

	private String wcitiesId, oauthToken;
	
	private List<Artist> artistList;
	private ArtistAdapterListener<Void> artistAdapterListener;
	
	/*private FollowingList cachedFollowingList;
	private SortedSet<Integer> artistIds;*/
	
	/*private List<Character> indices;
	private Map<Character, Integer> alphaNumIndexer;*/
	
	private LoadArtistsListener loadArtistsListener;
	
	private SortRecommendedArtist sortBy;
	
	public LoadRecommendedArtists(String oauthToken, String wcitiesId, List<Artist> artistList, 
			ArtistAdapterListener<Void> artistAdapterListener, LoadArtistsListener loadArtistsListener, 
			SortRecommendedArtist sortBy) {
		this.oauthToken = oauthToken;
		this.wcitiesId = wcitiesId;
		this.artistList = artistList;
		this.artistAdapterListener = artistAdapterListener;
		this.loadArtistsListener = loadArtistsListener;
		this.sortBy = sortBy;
	}

	@Override
	protected List<Artist> doInBackground(Void... params) {
		List<Artist> tmpArtists = new ArrayList<Artist>();
		UserInfoApi userInfoApi = new UserInfoApi(oauthToken);
		userInfoApi.setLimit(ARTISTS_LIMIT);
		userInfoApi.setAlreadyRequested(artistAdapterListener.getArtistsAlreadyRequested());
		userInfoApi.setUserId(wcitiesId);
		userInfoApi.setSortRecommendedArtist(sortBy);

		try {
			JSONObject jsonObject = userInfoApi.getMyProfileInfoFor(Type.recommendedartist);
			UserInfoApiJSONParser jsonParser = new UserInfoApiJSONParser();
			ItemsList<Artist> recommendedArtistsList = jsonParser.getRecommendedArtistList(jsonObject);
			tmpArtists = recommendedArtistsList.getItems();

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
			artistAdapterListener.setArtistsAlreadyRequested(artistList.size() - 1);

			if (tmpArtists.size() < ARTISTS_LIMIT) {
				artistAdapterListener.setMoreDataAvailable(false);
				artistList.remove(artistList.size() - 1);
			}
			
		} else {
			/**
			 * 05-01-2015:
			 * NOTE: Commented below line as not required in modified implementation
			 * 
			 * handleLoadedArtists(tmpArtists);
			 **/
			
			artistAdapterListener.setMoreDataAvailable(false);
			artistList.remove(artistList.size() - 1);
			
			if (artistList.isEmpty() && loadArtistsListener != null) {
				loadArtistsListener.showNoArtistFound();
			}
		}
		
		((BaseAdapter) artistAdapterListener).notifyDataSetChanged();
		if (loadArtistsListener instanceof AsyncTaskListener) {
			((AsyncTaskListener<Void>) loadArtistsListener).onTaskCompleted();
		}
	}
	
}
