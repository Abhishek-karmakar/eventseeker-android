package com.wcities.eventseeker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.AbsListView.RecyclerListener;

import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.api.UserInfoApi.Type;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.ArtistListener;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser.MyItemsList;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public abstract class FollowingParentFragment extends FragmentLoadableFromBackStack {

	private static final String TAG = FollowingParentFragment.class.getName();

	private static final int ARTISTS_LIMIT = 10;

	private String wcitiesId;

	private LoadArtists loadArtists;
	protected ArtistListAdapter artistListAdapter;

	private int artistsAlreadyRequested;
	private boolean isMoreDataAvailable = true;

	private List<Artist> artistList;
	
	// only required when child class of this fragment is having listView
	private String sections = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private Map<Character, Integer> alphaNumIndexer;
	private List<Character> indices;

	private AbsListView absListView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);

		if (wcitiesId == null) {
			wcitiesId = ((EventSeekr) FragmentUtil.getActivity(this)
					.getApplication()).getWcitiesId();
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		// Log.d(TAG, "onActivityCreated()");

		if (artistList == null) {
			artistList = new ArrayList<Artist>();
			artistList.add(null);
			
			alphaNumIndexer = new HashMap<Character, Integer>();
			indices = new ArrayList<Character>();

			artistListAdapter = new ArtistListAdapter(FragmentUtil.getActivity(this));

			loadArtistsInBackground();

		} else {
			artistListAdapter.setmInflater(FragmentUtil.getActivity(this));
		}
		
		absListView = getScrollableView();
		
		absListView.setRecyclerListener(new RecyclerListener() {
			
			@Override
			public void onMovedToScrapHeap(View view) {
				freeUpBitmapMemory(view);
			}
		});
		
		
		
		absListView.setAdapter(artistListAdapter);
		absListView.setScrollingCacheEnabled(false);
		absListView.setFastScrollEnabled(true);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			absListView.setFastScrollAlwaysVisible(true);
		}
	}

	private void loadArtistsInBackground() {
		loadArtists = new LoadArtists();
		AsyncTaskUtil.executeAsyncTask(loadArtists, true);
	}

	private class LoadArtists extends AsyncTask<Void, Void, List<Artist>> {

		@Override
		protected List<Artist> doInBackground(Void... params) {
			List<Artist> tmpArtists = new ArrayList<Artist>();
			UserInfoApi userInfoApi = new UserInfoApi(Api.OAUTH_TOKEN);
			userInfoApi.setLimit(ARTISTS_LIMIT);
			userInfoApi.setAlreadyRequested(artistsAlreadyRequested);
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
				int prevArtistListSize = artistList.size();
				artistList.addAll(artistList.size() - 1, tmpArtists);
				artistsAlreadyRequested += tmpArtists.size();

				if (tmpArtists.size() < ARTISTS_LIMIT) {
					isMoreDataAvailable = false;
					artistList.remove(artistList.size() - 1);
				}
				
				for (int i = 0; i < tmpArtists.size(); i++) {
					Artist artist = tmpArtists.get(i);
					char key = artist.getName().charAt(0);
					if (!indices.contains(key)) {
						indices.add(key);
						/**
						 * subtract 1 from prevArtistListSize to compensate for progressbar null item 
						 * counted in prevArtistListSize
						 */
						alphaNumIndexer.put(key, prevArtistListSize - 1 + i);
					}
				}

			} else {
				isMoreDataAvailable = false;
				artistList.remove(artistList.size() - 1);
			}
			
			artistListAdapter.notifyDataSetChanged();
		}
	}

	/**
	 * SectionIndexer is only required when child class of this fragment has set this adapter on listview 
	 * (not on gridview)
	 */
	protected class ArtistListAdapter extends BaseAdapter implements SectionIndexer {

		private static final String TAG_PROGRESS_INDICATOR = "progressIndicator";
		public static final String TAG_CONTENT = "content";

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
					if(((EventSeekr)FragmentUtil.getActivity(FollowingParentFragment.this).getApplicationContext())
							.isTablet()) {
						convertView = mInflater.inflate(R.layout.grd_progress_bar, null);
					} else {
						convertView = mInflater.inflate(R.layout.list_progress_bar, null);
					}
					convertView.setTag(TAG_PROGRESS_INDICATOR);
				}

				if ((loadArtists == null || loadArtists.getStatus() == Status.FINISHED)
						&& isMoreDataAvailable) {
					loadArtistsInBackground();
				}

			} else {
				
				if (convertView == null || !convertView.getTag().equals(TAG_CONTENT)) {
					if (((EventSeekr)FragmentUtil.getActivity(FollowingParentFragment.this).getApplicationContext())
							.isTablet()) {
						convertView = mInflater.inflate(R.layout.fragment_following_artists_list_item_tab, null);
					} else {
						convertView = mInflater.inflate(R.layout.fragment_search_artists_list_item, null);
					}
					convertView.setTag(TAG_CONTENT);
				}

				final Artist artist = getItem(position);
				((TextView) convertView.findViewById(R.id.txtArtistName)).setText(artist.getName());

				if (artist.isOntour()) {
					convertView.findViewById(R.id.txtOnTour).setVisibility(View.VISIBLE);

				} else {
					convertView.findViewById(R.id.txtOnTour).setVisibility(View.INVISIBLE);
				}

				String key = artist.getKey(ImgResolution.LOW);
				Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
				if (bitmap != null) {
					((ImageView) convertView.findViewById(R.id.imgItem)).setImageBitmap(bitmap);

				} else {
					ImageView imgArtist = (ImageView) convertView.findViewById(R.id.imgItem);
					imgArtist.setImageBitmap(null);

					AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
					asyncLoadImg.loadImg(imgArtist, ImgResolution.LOW, (AdapterView) parent, position, artist);
				}

				convertView.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						((ArtistListener) FragmentUtil.getActivity(FollowingParentFragment.this))
								.onArtistSelected(artist);
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

		@Override
		public int getPositionForSection(int sectionIndex) {
			//Log.d(TAG, "index = " + alphaNumIndexer.get(indices.get(sectionIndex)));
			return alphaNumIndexer.get(indices.get(sectionIndex));
		}

		@Override
		public int getSectionForPosition(int position) {
			return 0;
		}

		@Override
		public Object[] getSections() {
			return indices.toArray();
		}
	}
	
	protected void freeUpBitmapMemory(View view) {
		if (view.getTag().equals(ArtistListAdapter.TAG_CONTENT)) {
			((ImageView) view.findViewById(R.id.imgItem)).setImageBitmap(null);
		}
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		for (int i = absListView.getFirstVisiblePosition(), j = 0; 
				i <= absListView.getLastVisiblePosition(); 
				i++, j++) {
			freeUpBitmapMemory(absListView.getChildAt(j));
		}
		super.onDestroyView();
	}

	protected abstract AbsListView getScrollableView();
	
}
