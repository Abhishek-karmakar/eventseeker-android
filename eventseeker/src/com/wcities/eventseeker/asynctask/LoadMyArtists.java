package com.wcities.eventseeker.asynctask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.BaseAdapter;

import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.api.UserInfoApi.Type;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.FollowingList;
import com.wcities.eventseeker.interfaces.ArtistAdapterListener;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser.MyItemsList;

public class LoadMyArtists extends AsyncTask<Void, Void, List<Artist>> {

	private static final String TAG = LoadMyArtists.class.getName();

	private static final int ARTISTS_LIMIT = 10;

	private String wcitiesId;
	
	private List<Artist> artistList;
	private ArtistAdapterListener<Void> artistAdapterListener;
	
	private FollowingList cachedFollowingList;
	private SortedSet<Integer> artistIds;
	
	private List<Character> indices;
	private Map<Character, Integer> alphaNumIndexer;
	
	private LoadMyArtistsListener loadMyArtistsListener;
	
	public interface LoadMyArtistsListener {
		public void showNoArtistFound();
	}
	
	public LoadMyArtists(String wcitiesId, List<Artist> artistList, ArtistAdapterListener<Void> artistAdapterListener,
			FollowingList cachedFollowingList, SortedSet<Integer> artistIds, List<Character> indices, 
			Map<Character, Integer> alphaNumIndexer, LoadMyArtistsListener loadMyArtistsListener) {
		this.wcitiesId = wcitiesId;
		this.artistList = artistList;
		this.artistAdapterListener = artistAdapterListener;
		this.cachedFollowingList = cachedFollowingList;
		this.artistIds = artistIds;
		this.indices = indices;
		this.alphaNumIndexer = alphaNumIndexer;
		this.loadMyArtistsListener = loadMyArtistsListener;
	}

	@Override
	protected List<Artist> doInBackground(Void... params) {
		List<Artist> tmpArtists = new ArrayList<Artist>();
		UserInfoApi userInfoApi = new UserInfoApi(Api.OAUTH_TOKEN);
		userInfoApi.setLimit(ARTISTS_LIMIT);
		userInfoApi.setAlreadyRequested(artistAdapterListener.getArtistsAlreadyRequested());
		userInfoApi.setUserId(wcitiesId);

		try {
			JSONObject jsonObject = userInfoApi.getMyProfileInfoFor(Type.myartists);
			UserInfoApiJSONParser jsonParser = new UserInfoApiJSONParser();
			MyItemsList<Artist> myArtistsList = jsonParser.getArtistList(jsonObject);
			tmpArtists = myArtistsList.getItems();

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
			if (artistList.isEmpty() && loadMyArtistsListener != null) {
				loadMyArtistsListener.showNoArtistFound();
			}
		}
		
		((BaseAdapter) artistAdapterListener).notifyDataSetChanged();
	}
	
	private void handleLoadedArtists(List<Artist> tmpArtists) {
		int prevArtistListSize = artistList.size();

		// Add cached followed artists if any
		String fromInclusive = (artistList.get(0) == null) ? 
				null : artistList.get(artistList.size() - 2).getName();
		String toExclusive = (tmpArtists.size() < ARTISTS_LIMIT) ? 
				null : tmpArtists.get(tmpArtists.size() - 1).getName();
		Collection<Artist> mergedArtists = cachedFollowingList.addFollowedArtistsIfAny(tmpArtists, 
				fromInclusive, toExclusive);
		
		artistList.addAll(artistList.size() - 1, mergedArtists);
		artistAdapterListener.setArtistsAlreadyRequested(artistAdapterListener.getArtistsAlreadyRequested() 
				+ tmpArtists.size());
		
		int i = 0;
		for (Iterator<Artist> iterator = mergedArtists.iterator(); iterator.hasNext();) {
			Artist artist = iterator.next();
			if (!artistIds.contains(artist.getId())) {
				artistIds.add(artist.getId());
				
			} else {
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