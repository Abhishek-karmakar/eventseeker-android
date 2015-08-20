package com.wcities.eventseeker.asynctask;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.BaseAdapter;

import com.wcities.eventseeker.adapter.ArtistNewsListAdapter;
import com.wcities.eventseeker.adapter.ArtistNewsListAdapterTab;
import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.api.UserInfoApi.Type;
import com.wcities.eventseeker.asynctask.AsyncLoadImg.AsyncLoadImageListener;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.Enums.SortArtistNewsBy;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.ArtistNewsItem;
import com.wcities.eventseeker.core.ArtistNewsItem.PostType;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LoadArtistNews extends AsyncTask<Void, Void, List<ArtistNewsItem>> {
	
	private static final int ARTISTS_NEWS_LIMIT = 10;

	private static final String TAG = LoadArtistNews.class.getSimpleName();
	
	private BaseAdapter artistNewsListAdapter;
	private String wcitiesId, oauthToken;
	private Artist artist;
	private List<ArtistNewsListItem> batchLoaded;
	private List<ArtistNewsListItem> artistsNewsListItems;
	private int count;
	private OnNewsLoadedListener newsLoadedListener;

	private SortArtistNewsBy sortBy;

	private boolean addSrcFromNotification;
	
	public interface OnNewsLoadedListener {
		public abstract void onNewsLoaded();
	}
	
	public LoadArtistNews(String oauthToken, BaseAdapter artistNewsListAdapter, String wcitiesId, 
			List<ArtistNewsListItem> artistsNewsListItems, Artist artist, OnNewsLoadedListener newsLoadedListener, 
			SortArtistNewsBy sortBy) {
		this.oauthToken = oauthToken;
		this.artistNewsListAdapter = artistNewsListAdapter;
		this.wcitiesId = wcitiesId;
		this.artist = artist;
		this.artistsNewsListItems = artistsNewsListItems;
		this.newsLoadedListener = newsLoadedListener;
		this.sortBy = sortBy;
		
		batchLoaded = new ArrayList<ArtistNewsListItem>();
		
		if (artistNewsListAdapter instanceof ArtistNewsListAdapter) {
			((ArtistNewsListAdapter) artistNewsListAdapter).setBatchLoaded(batchLoaded);
		
		} else {
			((ArtistNewsListAdapterTab) artistNewsListAdapter).setBatchLoaded(batchLoaded);
		}
	}
	
	public void setAddSrcFromNotification(boolean addSrcFromNotification) {
		this.addSrcFromNotification = addSrcFromNotification;
	}
	
	@Override
	protected List<ArtistNewsItem> doInBackground(Void... params) {
		List<ArtistNewsItem> tmpArtistNewsItems = new ArrayList<ArtistNewsItem>();
		UserInfoApi userInfoApi = new UserInfoApi(oauthToken);
		userInfoApi.setLimit(ARTISTS_NEWS_LIMIT);
		userInfoApi.setUserId(wcitiesId);
		userInfoApi.setSortBy(sortBy);
		userInfoApi.setSrcFromNotification(addSrcFromNotification);

		if (artistNewsListAdapter instanceof ArtistNewsListAdapter) {
			userInfoApi.setAlreadyRequested(((ArtistNewsListAdapter) artistNewsListAdapter).getItemsAlreadyRequested());
		
		} else {
			userInfoApi.setAlreadyRequested(((ArtistNewsListAdapterTab) artistNewsListAdapter).getItemsAlreadyRequested());
		}
		
		if (artist != null) {
			userInfoApi.setArtistId(artist.getId());
		}

		try {
			JSONObject jsonObject = userInfoApi.getMyProfileInfoFor(Type.artistsfeed);
			UserInfoApiJSONParser jsonParser = new UserInfoApiJSONParser();
			
			tmpArtistNewsItems = jsonParser.getArtistNews(jsonObject);

		} catch (ClientProtocolException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
			
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return tmpArtistNewsItems;
	}
	
	@Override
	protected void onPostExecute(List<ArtistNewsItem> result) {
		count = 0;
		
		if (result.size() > 0) {
			if (artistNewsListAdapter instanceof ArtistNewsListAdapter) {
				((ArtistNewsListAdapter) artistNewsListAdapter).setItemsAlreadyRequested(((ArtistNewsListAdapter) 
						artistNewsListAdapter).getItemsAlreadyRequested() + result.size());
			
			} else {
				((ArtistNewsListAdapterTab) artistNewsListAdapter).setItemsAlreadyRequested(((ArtistNewsListAdapterTab) 
						artistNewsListAdapter).getItemsAlreadyRequested() + result.size());
			}
			for (Iterator<ArtistNewsItem> iterator = result.iterator(); iterator.hasNext();) {
				ArtistNewsItem artistNewsItem = iterator.next();
				batchLoaded.add(new ArtistNewsListItem(artistNewsItem, this));
			}
			
			if (result.size() < ARTISTS_NEWS_LIMIT) {
				if (artistNewsListAdapter instanceof ArtistNewsListAdapter) {
					((ArtistNewsListAdapter) artistNewsListAdapter).setMoreDataAvailable(false);
				
				} else {
					((ArtistNewsListAdapterTab) artistNewsListAdapter).setMoreDataAvailable(false);
				}
			}
			
		} else {
			if (artistNewsListAdapter instanceof ArtistNewsListAdapter) {
				((ArtistNewsListAdapter) artistNewsListAdapter).setMoreDataAvailable(false);
			
			} else {
				((ArtistNewsListAdapterTab) artistNewsListAdapter).setMoreDataAvailable(false);
			}
		}
		chkCount();
	}    	
	
	private void chkCount() {
		if (count == batchLoaded.size()/*true*/) {
			//Log.d(TAG, "chkCount() addAll for count = " + count);
			artistsNewsListItems.addAll(artistsNewsListItems.size() - 1, batchLoaded);
			
			boolean isMoreDataAvailable;
			if (artistNewsListAdapter instanceof ArtistNewsListAdapter) {
				isMoreDataAvailable = ((ArtistNewsListAdapter) artistNewsListAdapter).isMoreDataAvailable();
			
			} else {
				isMoreDataAvailable = ((ArtistNewsListAdapterTab) artistNewsListAdapter).isMoreDataAvailable();
			}
			//artistNewsListAdapter.setMoreDataAvailable(false);
			if (!isMoreDataAvailable) {
				//Log.d(TAG, "!artistNewsListAdapter.isMoreDataAvailable");
				/**
				 * Remove null item indicating progressbar if we have finished loading all items,
				 * so that adapter won't try loading more items even though we are setting 
				 * isMoreDataAvailable = true always below.
				 */
				artistsNewsListItems.remove(artistsNewsListItems.size() - 1);
				//artistsNewsListItems.clear();
				if (artistsNewsListItems.isEmpty()) {
					ArtistNewsItem artistNewsItem = new ArtistNewsItem();
					artistNewsItem.createArtist(AppConstants.INVALID_ID, AppConstants.INVALID_STR_ID);
					artistsNewsListItems.add(new ArtistNewsListItem(artistNewsItem, this));
					if (newsLoadedListener != null) {
						newsLoadedListener.onNewsLoaded();
					}
				}
			}
			
			count = 0;
			batchLoaded.clear();
			if (artistNewsListAdapter instanceof ArtistNewsListAdapter) {
				((ArtistNewsListAdapter) artistNewsListAdapter).setMoreDataAvailable(true);
			
			} else {
				((ArtistNewsListAdapterTab) artistNewsListAdapter).setMoreDataAvailable(true);
			}
			artistNewsListAdapter.notifyDataSetChanged();
			if (newsLoadedListener instanceof AsyncTaskListener) {
				((AsyncTaskListener<Void>) newsLoadedListener).onTaskCompleted();
			}
		}
	}
	
	public static class ArtistNewsListItem implements AsyncLoadImageListener {
		
		private ArtistNewsItem item;
		private int width, height;
		private LoadArtistNews loadArtistNews;
		
		public ArtistNewsListItem(ArtistNewsItem item, LoadArtistNews loadArtistNews) {
			this.loadArtistNews = loadArtistNews;
			this.item = item;
			if (item.getImgUrl() != null && item.getPostType() != PostType.link && item.getPostType() != PostType.event) {
				loadImgDimension();
				
			} else {
				width = height = 0;
				loadArtistNews.count++;
				//Log.d(TAG, "width = height = 0, count = " + count);
			}
		}
		
		public ArtistNewsItem getItem() {
			return item;
		}

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}

		private void loadImgDimension() {
			String key = item.getKey(ImgResolution.DEFAULT);
	        BitmapCache bitmapCache = BitmapCache.getInstance();
			Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
			if (bitmap != null) {
				width = bitmap.getWidth();
				height = bitmap.getHeight();
				loadArtistNews.count++;
				
		    } else {
		    	width = height = -1;
		        AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
		        asyncLoadImg.loadImg(ImgResolution.DEFAULT, item, this);
		    }
		}

		@Override
		public void onImageLoaded() {
			//Log.d(TAG, "onImageLoaded()");
			String key = item.getKey(ImgResolution.DEFAULT);
	        BitmapCache bitmapCache = BitmapCache.getInstance();
			Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
			if (bitmap != null) {
				width = bitmap.getWidth();
				height = bitmap.getHeight();
			}
			loadArtistNews.count++;
			//Log.d(TAG, "count = " + count);
			loadArtistNews.chkCount();
		}

		@Override
		public void onImageCouldNotBeLoaded() {
			//Log.d(TAG, "onImageCouldNotBeLoaded()");
			width = height = 0;
			loadArtistNews.count++;
			//Log.d(TAG, "count = " + count);
			loadArtistNews.chkCount();
		}
	}
}
