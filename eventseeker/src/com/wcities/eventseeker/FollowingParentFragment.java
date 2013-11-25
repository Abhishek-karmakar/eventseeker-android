package com.wcities.eventseeker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

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
import com.wcities.eventseeker.util.FragmentUtil;

public class FollowingParentFragment extends FragmentLoadableFromBackStack {

	private static final String TAG = FollowingParentFragment.class.getName();

	private static final int ARTISTS_LIMIT = 10;

	private String wcitiesId;

	private LoadArtists loadArtists;
	protected ArtistListAdapter artistListAdapter;

	private int artistsAlreadyRequested;
	private boolean isMoreDataAvailable = true;

	private List<Artist> artistList;

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

			artistListAdapter = new ArtistListAdapter(
					FragmentUtil.getActivity(this));

			loadArtistsInBackground();

		} else {
			artistListAdapter.setmInflater(FragmentUtil.getActivity(this));
		}

	}

	private void loadArtistsInBackground() {
		loadArtists = new LoadArtists();
		loadArtists.execute();
	}

	private class LoadArtists extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			UserInfoApi userInfoApi = new UserInfoApi(Api.OAUTH_TOKEN);
			userInfoApi.setLimit(ARTISTS_LIMIT);
			userInfoApi.setAlreadyRequested(artistsAlreadyRequested);
			userInfoApi.setUserId(wcitiesId);

			try {
				JSONObject jsonObject = userInfoApi
						.getMyProfileInfoFor(Type.myartists);
				UserInfoApiJSONParser jsonParser = new UserInfoApiJSONParser();
				MyItemsList<Artist> myArtistsList = jsonParser
						.getArtistList(jsonObject);
				List<Artist> tmpArtists = myArtistsList.getItems();

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

	protected class ArtistListAdapter extends BaseAdapter {

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
				if (convertView == null
						|| !convertView.getTag().equals(TAG_PROGRESS_INDICATOR)) {
					convertView = mInflater.inflate(R.layout.list_progress_bar,
							null);
					convertView.setTag(TAG_PROGRESS_INDICATOR);
				}

				if ((loadArtists == null || loadArtists.getStatus() == Status.FINISHED)
						&& isMoreDataAvailable) {
					loadArtistsInBackground();
				}

			} else {
				if (convertView == null
						|| !convertView.getTag().equals(TAG_CONTENT)) {
					if (((MainActivity) FragmentUtil
							.getActivity(FollowingParentFragment.this))
							.isTablet()) {
						convertView = mInflater.inflate(
								R.layout.fragment_search_artists_list_item_tab,
								null);
					} else {
						convertView = mInflater.inflate(
								R.layout.fragment_search_artists_list_item,
								null);
					}
					convertView.setTag(TAG_CONTENT);
				}

				final Artist artist = getItem(position);
				((TextView) convertView.findViewById(R.id.txtArtistName))
						.setText(artist.getName());

				if (artist.isOntour()) {
					convertView.findViewById(R.id.txtOnTour).setVisibility(
							View.VISIBLE);

				} else {
					convertView.findViewById(R.id.txtOnTour).setVisibility(
							View.INVISIBLE);
				}

				String key = artist.getKey(ImgResolution.LOW);
				Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
				if (bitmap != null) {
					((ImageView) convertView.findViewById(R.id.imgItem))
							.setImageBitmap(bitmap);

				} else {
					ImageView imgArtist = (ImageView) convertView
							.findViewById(R.id.imgItem);
					imgArtist.setImageBitmap(null);

					AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
					asyncLoadImg.loadImg(imgArtist, ImgResolution.LOW,
							(AdapterView) parent, position, artist);
				}

				convertView.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						((ArtistListener) FragmentUtil
								.getActivity(FollowingParentFragment.this))
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
	}
}
