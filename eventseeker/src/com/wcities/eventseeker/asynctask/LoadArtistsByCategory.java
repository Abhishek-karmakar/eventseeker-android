package com.wcities.eventseeker.asynctask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.widget.BaseAdapter;

import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Artist.Genre;
import com.wcities.eventseeker.core.ItemsList;
import com.wcities.eventseeker.interfaces.ArtistAdapterListener;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.interfaces.LoadArtistsListener;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser;

public class LoadArtistsByCategory extends AsyncTask<Void, Void, List<Artist>> {

	private static final String TAG = LoadArtistsByCategory.class.getName();

	private static final int ARTISTS_LIMIT = 10;

	private String wcitiesId, oauthToken;
	
	private List<Artist> artistList;
	private ArtistAdapterListener<Void> artistAdapterListener;
	private LoadArtistsListener loadArtistsListener;
	private Genre genre;

	private SortedSet<Integer> artistIds;
	
	private List<Character> indices;
	private Map<Character, Integer> alphaNumIndexer;
	
	public LoadArtistsByCategory(String oauthToken, String wcitiesId, List<Artist> artistList, ArtistAdapterListener<Void> artistAdapterListener,
			SortedSet<Integer> artistIds, List<Character> indices, 
			Map<Character, Integer> alphaNumIndexer, LoadArtistsListener loadArtistsListener, Genre genre) {
		this.oauthToken = oauthToken;
		this.wcitiesId = wcitiesId;
		this.artistList = artistList;
		this.artistAdapterListener = artistAdapterListener;
		this.artistIds = artistIds;
		this.indices = indices;
		this.alphaNumIndexer = alphaNumIndexer;
		this.loadArtistsListener = loadArtistsListener;
		this.genre = genre;
	}
	
	@Override
	protected List<Artist> doInBackground(Void... params) {
		List<Artist> tmpArtists = new ArrayList<Artist>();
		UserInfoApi userInfoApi = new UserInfoApi(oauthToken);
		userInfoApi.setLimit(ARTISTS_LIMIT);
		userInfoApi.setAlreadyRequested(artistAdapterListener.getArtistsAlreadyRequested());
		userInfoApi.setUserId(wcitiesId);

		try {
			JSONObject jsonObject = userInfoApi.getMyProfileInfoForPopularArtist(genre);
			UserInfoApiJSONParser jsonParser = new UserInfoApiJSONParser();
			ItemsList<Artist> popularArtistsList = jsonParser.getPopularArtistList(jsonObject);
			tmpArtists = popularArtistsList.getItems();

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
			handleLoadedArtists(tmpArtists);

			if (tmpArtists.size() < ARTISTS_LIMIT) {
				artistAdapterListener.setMoreDataAvailable(false);
				artistList.remove(artistList.size() - 1);
			}
			
		} else {
			handleLoadedArtists(tmpArtists);
			
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
	
	private void handleLoadedArtists(List<Artist> tmpArtists) {
		//Log.d(TAG, "handleLoadedArtists()");
		int prevArtistListSize = artistList.size();
		
		artistList.addAll(artistList.size() - 1, tmpArtists);
		artistAdapterListener.setArtistsAlreadyRequested(artistAdapterListener.getArtistsAlreadyRequested() 
				+ tmpArtists.size());
		
		int i = 0;
		for (Iterator<Artist> iterator = tmpArtists.iterator(); iterator.hasNext();) {
			Artist artist = iterator.next();
			if (!artistIds.contains(artist.getId())) {
				//Log.d(TAG, "handleLoadedArtists() add - " + artist.getId());
				artistIds.add(artist.getId());
				
			} else {
				//Log.d(TAG, "handleLoadedArtists() remove - " + artist.getId());
				artistList.remove(artist);
				continue;
			}
			
			if (indices != null) {
				char key = artist.getName().charAt(0);
				if (!indices.contains(key)) {
					indices.add(key);
					/**
					 * subtract 1 from prevArtistListSize to compensate for progressbar null item 
					 * counted in prevArtistListSize
					 */
					alphaNumIndexer.put(key, prevArtistListSize - 1 + i);
				}
				i++;
			}
		}
	}
	
}
