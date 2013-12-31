package com.wcities.eventseeker;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.wcities.eventseeker.SearchFragment.SearchFragmentChildListener;
import com.wcities.eventseeker.adapter.ArtistListAdapter;
import com.wcities.eventseeker.adapter.DateWiseMyEventListAdapter;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.ArtistApi;
import com.wcities.eventseeker.api.ArtistApi.Method;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.interfaces.ArtistListener;
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.jsonparser.ArtistApiJSONParser;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.DateWiseEventList;

public class SearchArtistsFragment extends ListFragment implements SearchFragmentChildListener, LoadItemsInBackgroundListener {

	private static final String TAG = SearchArtistsFragment.class.getName();

	private static final int ARTISTS_LIMIT = 10;
	
	private String query;
	private LoadArtists loadArtists;
	private ArtistListAdapter<String> artistListAdapter;
	
	private List<Artist> artistList;
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (!(activity instanceof ArtistListener)) {
            throw new ClassCastException(activity.toString() + " must implement ArtistListener");
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = super.onCreateView(inflater, container, savedInstanceState);

		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			int pad = getResources().getDimensionPixelSize(R.dimen.tab_bar_margin_fragment_custom_tabs);
			v.setPadding(pad, 0, pad, 0);
		}
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		//Log.d(TAG, "onActivityCreated()");
		
		if (artistList == null) {
			artistList = new ArrayList<Artist>();
			artistListAdapter = new ArtistListAdapter<String>(FragmentUtil.getActivity(this), artistList, null, this);
	        Bundle args = getArguments();
			if (args != null && args.containsKey(BundleKeys.QUERY)) {
				artistList.add(null);
				query = args.getString(BundleKeys.QUERY);
				loadItemsInBackground();
			}
			
		} else {
			artistListAdapter.updateContext(FragmentUtil.getActivity(this));
		}

		setListAdapter(artistListAdapter);
        getListView().setDivider(null);
        getListView().setBackgroundResource(R.drawable.story_space);
	}
	
	@Override
	public void loadItemsInBackground() {
		loadArtists = new LoadArtists(artistList, artistListAdapter);
		artistListAdapter.setLoadArtists(loadArtists);
		AsyncTaskUtil.executeAsyncTask(loadArtists, true, query);
	}
	
	private void refresh(String newQuery) {
		Log.d(TAG, "refresh()");
		// if user selection has changed then only reset the list
		if (query == null || !query.equals(newQuery)) {
			Log.d(TAG, "query == null || !query.equals(newQuery)");

			query = newQuery;
			artistListAdapter.setArtistsAlreadyRequested(0);
			artistListAdapter.setMoreDataAvailable(true);
			
			if (loadArtists != null && loadArtists.getStatus() != Status.FINISHED) {
				loadArtists.cancel(true);
			}
			
			artistList.clear();
			artistList.add(null);
			artistListAdapter.notifyDataSetChanged();
			
			loadItemsInBackground();
		}
	}
	
	private static class LoadArtists extends AsyncTask<String, Void, List<Artist>> {
		
		private List<Artist> artistList;
		private ArtistListAdapter<String> artistListAdapter;
		
		public LoadArtists(List<Artist> artistList, ArtistListAdapter<String> artistListAdapter) {
			this.artistList = artistList;
			this.artistListAdapter = artistListAdapter;
		}

		@Override
		protected List<Artist> doInBackground(String... params) {
			List<Artist> tmpArtists = new ArrayList<Artist>();
			ArtistApi artistApi = new ArtistApi(Api.OAUTH_TOKEN);
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
	
	@Override
	public void onQueryTextSubmit(String query) {
		Log.d(TAG, "onQueryTextSubmit(), query = " + query);
		refresh(query);
	}
}
