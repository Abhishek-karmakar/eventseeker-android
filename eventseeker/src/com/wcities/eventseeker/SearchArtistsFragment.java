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
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.ArtistApi;
import com.wcities.eventseeker.api.ArtistApi.Method;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.interfaces.ArtistListener;
import com.wcities.eventseeker.jsonparser.ArtistApiJSONParser;
import com.wcities.eventseeker.util.FragmentUtil;

public class SearchArtistsFragment extends ListFragment implements SearchFragmentChildListener {

	private static final String TAG = SearchArtistsFragment.class.getName();

	private static final int ARTISTS_LIMIT = 10;
	
	private String query;
	private LoadArtists loadArtists;
	private ArtistListAdapter artistListAdapter;
	private int artistsAlreadyRequested;
	private boolean isMoreDataAvailable = true;
	
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
			artistListAdapter = new ArtistListAdapter(FragmentUtil.getActivity(this));
	        
	        Bundle args = getArguments();
			if (args != null && args.containsKey(BundleKeys.QUERY)) {
				artistList.add(null);
				query = args.getString(BundleKeys.QUERY);
				loadArtistsInBackground();
			}
			
		} else {
			artistListAdapter.setmInflater(FragmentUtil.getActivity(this));
		}

		setListAdapter(artistListAdapter);
        getListView().setDivider(null);
        getListView().setBackgroundResource(R.drawable.story_space);
	}
	
	private void loadArtistsInBackground() {
		loadArtists = new LoadArtists();
        loadArtists.execute(query);
	}
	
	private void refresh(String newQuery) {
		Log.d(TAG, "refresh()");
		// if user selection has changed then only reset the list
		if (query == null || !query.equals(newQuery)) {
			Log.d(TAG, "query == null || !query.equals(newQuery)");

			query = newQuery;
			artistsAlreadyRequested = 0;
			isMoreDataAvailable = true;
			
			if (loadArtists != null && loadArtists.getStatus() != Status.FINISHED) {
				loadArtists.cancel(true);
			}
			
			artistList.clear();
			artistList.add(null);
			artistListAdapter.notifyDataSetChanged();
			
			loadArtistsInBackground();
		}
	}
	
	private class LoadArtists extends AsyncTask<String, Void, Void> {
		
		@Override
		protected Void doInBackground(String... params) {
			ArtistApi artistApi = new ArtistApi(Api.OAUTH_TOKEN);
			artistApi.setLimit(ARTISTS_LIMIT);
			artistApi.setAlreadyRequested(artistsAlreadyRequested);
			artistApi.setStrictSearchEnabled(true);
			artistApi.setMethod(Method.artistSearch);

			try {
				artistApi.setArtist(URLEncoder.encode(params[0], AppConstants.CHARSET_NAME));

				JSONObject jsonObject = artistApi.getArtists();
				ArtistApiJSONParser jsonParser = new ArtistApiJSONParser();
				
				List<Artist> tmpArtists = jsonParser.getArtistList(jsonObject);

				if (!tmpArtists.isEmpty()) {
					artistList.addAll(artistList.size() - 1, tmpArtists);
					artistsAlreadyRequested += tmpArtists.size();
					
					if (tmpArtists.size() < ARTISTS_LIMIT) {
						isMoreDataAvailable = false;
						artistList.remove(artistList.size() - 1);
					}
					
				} else {
					isMoreDataAvailable = false;
					artistList.remove(artistList.size() - 1);
				}
				
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
			artistListAdapter.notifyDataSetChanged();
		}    	
    }
	
	private class ArtistListAdapter extends BaseAdapter {
		
		private static final String TAG_PROGRESS_INDICATOR = "progressIndicator";
		private static final String TAG_CONTENT = "content";
		
	    private LayoutInflater mInflater;
	    private BitmapCache bitmapCache;

	    public ArtistListAdapter(Context context) {
	        mInflater = LayoutInflater.from(context);
	        bitmapCache = BitmapCache.getInstance();
	    }
	    
	    public void setmInflater(Context context) {
	        mInflater = LayoutInflater.from(context);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (artistList.get(position) == null) {
				if (convertView == null || !convertView.getTag().equals(TAG_PROGRESS_INDICATOR)) {
					convertView = mInflater.inflate(R.layout.list_progress_bar, null);
					convertView.setTag(TAG_PROGRESS_INDICATOR);
				}
				
				if ((loadArtists == null || loadArtists.getStatus() == Status.FINISHED) && 
						isMoreDataAvailable) {
					loadArtistsInBackground();
				}
				
			} else {
				if (convertView == null || !convertView.getTag().equals(TAG_CONTENT)) {
					convertView = mInflater.inflate(R.layout.fragment_search_artists_list_item, null);
					convertView.setTag(TAG_CONTENT);
				}
				
				final Artist artist = getItem(position);
				((TextView)convertView.findViewById(R.id.txtArtistName)).setText(artist.getName());
				
				if (artist.isOntour()) {
					convertView.findViewById(R.id.txtOnTour).setVisibility(View.VISIBLE);
					
				} else {
					convertView.findViewById(R.id.txtOnTour).setVisibility(View.INVISIBLE);
				}
				
				String key = artist.getKey(ImgResolution.LOW);
				Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
				if (bitmap != null) {
			        ((ImageView)convertView.findViewById(R.id.imgItem)).setImageBitmap(bitmap);
			        
			    } else {
			    	ImageView imgArtist = (ImageView)convertView.findViewById(R.id.imgItem); 
			        imgArtist.setImageBitmap(null);

			        AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
			        asyncLoadImg.loadImg(imgArtist, ImgResolution.LOW, 
			        		(AdapterView) parent, position, artist);
			    }
				
				convertView.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						((ArtistListener)FragmentUtil.getActivity(SearchArtistsFragment.this)).onArtistSelected(artist);
					}
				});
			}
			
			return convertView;
		}

		@Override
		public Artist getItem(int position) {
			return artistList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public int getCount() {
			return artistList.size();
		}
	}
	
	@Override
	public void onQueryTextSubmit(String query) {
		Log.d(TAG, "onQueryTextSubmit(), query = " + query);
		refresh(query);
	}
}
