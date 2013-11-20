package com.wcities.eventseeker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView.RecyclerListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.api.UserInfoApi.Type;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.asynctask.AsyncLoadImg.AsyncLoadImageListener;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.core.ArtistNewsItem;
import com.wcities.eventseeker.core.ArtistNewsItem.PostType;
import com.wcities.eventseeker.custom.fragment.ListFragmentLoadableFromBackStack;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class ArtistsNewsListFragment extends ListFragmentLoadableFromBackStack {
	
	protected static final String TAG = ArtistsNewsListFragment.class.getName();

	private LoadArtistsNews loadArtistsNews;
	private ArtistNewsListAdapter artistNewsListAdapter;
	
	private int orientation;
	private String wcitiesId;
	private int count, widthPort, widthLan;
	private List<ArtistsNewsListItem> batchLoaded;
	private List<ArtistsNewsListItem> artistsNewsListItems;
	
	private int itemsAlreadyRequested;
	private boolean isMoreDataAvailable = true;
	private int firstVisibleNewsItemPosition;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		
		widthPort = getResources().getDisplayMetrics().widthPixels 
				- (getResources().getDimensionPixelSize(R.dimen.root_lnr_layout_pad_l_artists_news_list_item) * 2)
				- (getResources().getDimensionPixelSize(R.dimen.rlt_layout_news_item_container_pad_artist_news_item) * 2);
		widthLan = (getResources().getDisplayMetrics().heightPixels 
				- (getResources().getDimensionPixelSize(R.dimen.root_lnr_layout_pad_l_artists_news_list_item) * 2)
				- (getResources().getDimensionPixelSize(R.dimen.rlt_layout_news_item_container_pad_artist_news_item) * 4)
				- (getResources().getDimensionPixelSize(R.dimen.rlt_layout_news_item_container2_margin_l_artists_news_list_item))) / 2;
		
		if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
			int temp = widthPort;
			widthPort = widthLan;
			widthLan = temp;
		}
		
		if (wcitiesId == null) {
			wcitiesId = ((EventSeekr)FragmentUtil.getActivity(this).getApplication()).getWcitiesId();
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		orientation = getResources().getConfiguration().orientation;
		return super.onCreateView(inflater, container, savedInstanceState);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if (artistsNewsListItems == null) {
			artistsNewsListItems = new ArrayList<ArtistsNewsListItem>();
			artistNewsListAdapter = new ArtistNewsListAdapter(FragmentUtil.getActivity(this));
	        
			artistsNewsListItems.add(null);
			batchLoaded = new ArrayList<ArtistsNewsListFragment.ArtistsNewsListItem>();
			loadArtistsNewsInBackground();
			
		} else {
			artistNewsListAdapter.setmInflater(FragmentUtil.getActivity(this));
		}

		setListAdapter(artistNewsListAdapter);
		getListView().setDivider(null);
		getListView().setScrollingCacheEnabled(false);
		
		final int pos = (orientation == Configuration.ORIENTATION_PORTRAIT) ? 
				firstVisibleNewsItemPosition : (int)Math.floor(firstVisibleNewsItemPosition / 2.0);
		//Log.d(TAG, "onActivityCreated() firstVisibleNewsItemPosition = " + firstVisibleNewsItemPosition + ", pos = " + pos);
		getListView().post(new Runnable() {
			
			@Override
			public void run() {
				getListView().setSelection(pos);
			}
		});
		
		getListView().setRecyclerListener(new RecyclerListener() {
			
			@Override
			public void onMovedToScrapHeap(View view) {
				freeUpBitmapMemory(view);
			}
		});
	}
	
	@Override
	public void onDestroyView() {
		//Log.i(TAG, "onDestroyView()");
		
		firstVisibleNewsItemPosition = (orientation == Configuration.ORIENTATION_PORTRAIT) ? 
				getListView().getFirstVisiblePosition() : getListView().getFirstVisiblePosition() * 2;
				
		for (int i = getListView().getFirstVisiblePosition(), j = 0; 
				i <= getListView().getLastVisiblePosition(); i++, j++) {
			freeUpBitmapMemory(getListView().getChildAt(j));
		}
		super.onDestroyView();
	}
	
	private void freeUpBitmapMemory(View view) {
		if (view.getTag() instanceof ArtistNewsListAdapter.ArtistNewsItemViewHolder) {
			ArtistNewsListAdapter.ArtistNewsItemViewHolder holder = (ArtistNewsListAdapter.ArtistNewsItemViewHolder) view.getTag();
			//Log.d(TAG, "recycle bitmaps for pos = " + holder.pos);
	
			ImageView imgPhoto = holder.imgPhoto;
			imgPhoto.setImageBitmap(null);
			
			ImageView imgLink = holder.imgLink;
			imgLink.setImageBitmap(null);
		}
	}
	
	private void loadArtistsNewsInBackground() {
		loadArtistsNews = new LoadArtistsNews();
		loadArtistsNews.execute();
	}
	
	private class LoadArtistsNews extends AsyncTask<Void, Void, List<ArtistNewsItem>> {
		
		private static final int ARTISTS_NEWS_LIMIT = 10;
		
		@Override
		protected List<ArtistNewsItem> doInBackground(Void... params) {
			List<ArtistNewsItem> tmpArtistNewsItems = new ArrayList<ArtistNewsItem>();
			UserInfoApi userInfoApi = new UserInfoApi(Api.OAUTH_TOKEN);
			userInfoApi.setLimit(ARTISTS_NEWS_LIMIT);
			userInfoApi.setAlreadyRequested(itemsAlreadyRequested);
			userInfoApi.setUserId(wcitiesId);

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
			batchLoaded = new ArrayList<ArtistsNewsListFragment.ArtistsNewsListItem>();
			count = 0;
			
			if (result.size() > 0) {
				itemsAlreadyRequested += result.size();

				for (Iterator<ArtistNewsItem> iterator = result.iterator(); iterator.hasNext();) {
					ArtistNewsItem artistNewsItem = iterator.next();
					batchLoaded.add(new ArtistsNewsListItem(artistNewsItem));
				}
				
				if (result.size() < ARTISTS_NEWS_LIMIT) {
					isMoreDataAvailable = false;
				}
				
			} else {
				isMoreDataAvailable = false;
			}
			chkCount();
		}    	
    }
	
	private class ArtistNewsListAdapter extends BaseAdapter {
		
		private static final String TAG_PROGRESS_INDICATOR = "progressIndicator";
		private int IMG_MARGIN_B;
		
	    private LayoutInflater mInflater;

	    public ArtistNewsListAdapter(Context context) {
	        mInflater = LayoutInflater.from(context);
	        IMG_MARGIN_B = getResources().getDimensionPixelSize(R.dimen.img_photo_margin_b_artist_news_item);
	    }
	    
	    public void setmInflater(Context context) {
	        mInflater = LayoutInflater.from(context);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			//Log.d(TAG, "pos = " + position);
			Object item = getItem(position);
			if (item == null || 
					((item instanceof List) && ((List<ArtistsNewsListItem>)item).get(0) == null)) {
				if (convertView == null || convertView.getTag() instanceof ArtistNewsItemViewHolder) {
					convertView = mInflater.inflate(R.layout.list_progress_bar, null);
					convertView.setTag(TAG_PROGRESS_INDICATOR);
					convertView.setBackgroundColor(getResources().getColor(R.color.root_lnr_layout_bg_artists_news_list_item));
				}
				
				if ((loadArtistsNews == null || loadArtistsNews.getStatus() == Status.FINISHED) && 
						isMoreDataAvailable && batchLoaded.isEmpty()) {
					loadArtistsNewsInBackground();
				}
				
			} else {
				ArtistNewsItemViewHolder holder;
				
				if (convertView == null || !(convertView.getTag() instanceof ArtistNewsItemViewHolder)) {
					convertView = mInflater.inflate(R.layout.artists_fix_size_news_list_item, null);
					holder = new ArtistNewsItemViewHolder();
					
					RelativeLayout rltLayoutNewsItemContainer =  holder.rltLayoutNewsItemContainer 
							= (RelativeLayout) convertView.findViewById(R.id.rltLayoutNewsItemContainer);
					holder.imgPhoto = (ImageView) rltLayoutNewsItemContainer.findViewById(R.id.imgPhoto);
					holder.imgVideo = (ImageView) rltLayoutNewsItemContainer.findViewById(R.id.imgVideo);
					holder.txtTitle = (TextView) rltLayoutNewsItemContainer.findViewById(R.id.txtTitle);
					holder.txtTime = (TextView) rltLayoutNewsItemContainer.findViewById(R.id.txtTime);
					holder.imgLink = (ImageView) rltLayoutNewsItemContainer.findViewById(R.id.imgLink);
					holder.txtLinkTitle = (TextView) rltLayoutNewsItemContainer.findViewById(R.id.txtLinkTitle);
					holder.txtDesc = (TextView) rltLayoutNewsItemContainer.findViewById(R.id.txtDesc);
					
					RelativeLayout rltLayoutNewsItem2Container = holder.rltLayoutNewsItem2Container 
							= (RelativeLayout) convertView.findViewById(R.id.rltLayoutNewsItemContainer2);
					holder.imgPhoto2 = (ImageView) rltLayoutNewsItem2Container.findViewById(R.id.imgPhoto);
					holder.imgVideo2 = (ImageView) rltLayoutNewsItem2Container.findViewById(R.id.imgVideo);
					holder.txtTitle2 = (TextView) rltLayoutNewsItem2Container.findViewById(R.id.txtTitle);
					holder.txtTime2 = (TextView) rltLayoutNewsItem2Container.findViewById(R.id.txtTime);
					holder.imgLink2 = (ImageView) rltLayoutNewsItem2Container.findViewById(R.id.imgLink);
					holder.txtLinkTitle2 = (TextView) rltLayoutNewsItem2Container.findViewById(R.id.txtLinkTitle);
					holder.txtDesc2 = (TextView) rltLayoutNewsItem2Container.findViewById(R.id.txtDesc);
					
					convertView.setTag(holder);
					
				} else {
					holder = (ArtistNewsItemViewHolder) convertView.getTag();
				}
				
				final Object listItem = getItem(position);
				holder.setContent(listItem, parent, position);
			}
			
			return convertView;
		}

		@Override
		public Object getItem(int position) {
			if (orientation == Configuration.ORIENTATION_PORTRAIT) {
				return artistsNewsListItems.get(position);
				
			} else {
				List<ArtistsNewsListItem> artistNewsItemList = new ArrayList<ArtistsNewsListItem>();
				artistNewsItemList.add(artistsNewsListItems.get(position * 2));
				if (artistsNewsListItems.size() > position * 2 + 1) {
					artistNewsItemList.add(artistsNewsListItems.get(position * 2 + 1));
				}
				return artistNewsItemList;
			}
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public int getCount() {
			if (orientation == Configuration.ORIENTATION_PORTRAIT) {
				//Log.d(TAG, "getCount() = " + artistsNewsListItems.size());
				return artistsNewsListItems.size();
				
			} else {
				//Log.d(TAG, "else getCount() = " + (int)Math.ceil(artistsNewsListItems.size() / 2.0));
				return (int) Math.ceil(artistsNewsListItems.size() / 2.0);
			}
		}
		
		private class ArtistNewsItemViewHolder {
			
			private RelativeLayout rltLayoutNewsItemContainer;
			private ImageView imgPhoto;
			private TextView txtTitle, txtTime, txtLinkTitle, txtDesc;
			private ImageView imgLink, imgVideo;
			
			private RelativeLayout rltLayoutNewsItem2Container;
			private ImageView imgPhoto2;
			private TextView txtTitle2, txtTime2, txtLinkTitle2, txtDesc2;
			private ImageView imgLink2, imgVideo2;
			
			private void setContent(Object listItem, ViewGroup parent, int pos) {
				if (listItem instanceof ArtistsNewsListItem) {
					//Log.d(TAG, "ArtistNewsItem");
					rltLayoutNewsItem2Container.setVisibility(View.GONE);
					setNewsItemContent((ArtistsNewsListItem) listItem, parent, pos);
					
				} else {
					List<ArtistsNewsListItem> artistNewsItemList = (List<ArtistsNewsListItem>) listItem;
					setNewsItemContent((ArtistsNewsListItem) artistNewsItemList.get(0), parent, pos);
					if (artistNewsItemList.size() == 2) {
						//Log.d(TAG, "not ArtistNewsItem, size = 2");
						rltLayoutNewsItem2Container.setVisibility(View.VISIBLE);
						setNewsItem2Content((ArtistsNewsListItem) artistNewsItemList.get(1), parent, pos);
						
					} else {
						//Log.d(TAG, "not ArtistNewsItem, size = 1");
						rltLayoutNewsItem2Container.setVisibility(View.INVISIBLE);
					}
				}
			}
			
			private void setNewsItemContent(ArtistsNewsListItem artistsNewsListItem, ViewGroup parent, int pos) {
				final ArtistNewsItem item = artistsNewsListItem.item;
				String title = item.getArtistName();
				String time = ConversionUtil.getTimeDiffFromCurrentTime(item.getTimestamp());
				
				switch (item.getPostType()) {
				
				case status:
					imgPhoto.setVisibility(View.GONE);
					imgVideo.setVisibility(View.GONE);
					txtLinkTitle.setVisibility(View.GONE);
					imgLink.setVisibility(View.GONE);
					
					title += " posted a status";
					break;
					
				case link:
					imgPhoto.setVisibility(View.GONE);
					imgVideo.setVisibility(View.GONE);
					txtLinkTitle.setVisibility(View.VISIBLE);
					
					title += " shared a link";
					txtLinkTitle.setText(item.getPostTitle());
					
					if (item.getMobiResImgUrl() != null) {
						imgLink.setVisibility(View.VISIBLE);
						updateImageView(artistsNewsListItem, imgLink, parent, pos);
						
					} else {
						imgLink.setVisibility(View.GONE);
					}
					break;
					
				case photo:
					imgPhoto.setVisibility(View.VISIBLE);
					imgVideo.setVisibility(View.GONE);
					txtLinkTitle.setVisibility(View.GONE);
					imgLink.setVisibility(View.GONE);
					
					title += " posted a new photo";
					updateImageView(artistsNewsListItem, imgPhoto, parent, pos);
					break;
					
				case video:
					imgPhoto.setVisibility(View.VISIBLE);
					imgVideo.setVisibility(View.VISIBLE);
					txtLinkTitle.setVisibility(View.GONE);
					imgLink.setVisibility(View.GONE);
					
					title += " posted a video";
					updateImageView(artistsNewsListItem, imgPhoto, parent, pos);
					break;

				default:
					break;
				}
				
				txtTitle.setText(title);
				txtTime.setText(time);
				txtDesc.setText(item.getPostDesc());
				
				rltLayoutNewsItemContainer.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						Intent browserIntent = new Intent(Intent.ACTION_VIEW, 
								Uri.parse(item.getPostUrl()));
						startActivity(browserIntent);
					}
				});
			}
			
			private void setNewsItem2Content(ArtistsNewsListItem artistsNewsListItem, ViewGroup parent, int pos) {
				final ArtistNewsItem item = artistsNewsListItem.item;
				String title = item.getArtistName();
				String time = ConversionUtil.getTimeDiffFromCurrentTime(item.getTimestamp());
				
				switch (item.getPostType()) {
				
				case status:
					imgPhoto2.setVisibility(View.GONE);
					imgVideo2.setVisibility(View.GONE);
					txtLinkTitle2.setVisibility(View.GONE);
					imgLink2.setVisibility(View.GONE);
					
					title += " posted a status";
					break;
					
				case link:
					imgPhoto2.setVisibility(View.GONE);
					imgVideo2.setVisibility(View.GONE);
					txtLinkTitle2.setVisibility(View.VISIBLE);
					
					title += " shared a link";
					txtLinkTitle2.setText(item.getPostTitle());
					
					if (item.getMobiResImgUrl() != null) {
						imgLink2.setVisibility(View.VISIBLE);
						updateImageView(artistsNewsListItem, imgLink2, parent, pos);
						
					} else {
						imgLink2.setVisibility(View.GONE);
					}
					break;
					
				case photo:
					imgPhoto2.setVisibility(View.VISIBLE);
					imgVideo2.setVisibility(View.GONE);
					txtLinkTitle2.setVisibility(View.GONE);
					imgLink2.setVisibility(View.GONE);
					
					title += " posted a new photo";
					updateImageView(artistsNewsListItem, imgPhoto2, parent, pos);
					break;
					
				case video:
					imgPhoto2.setVisibility(View.VISIBLE);
					imgVideo2.setVisibility(View.VISIBLE);
					txtLinkTitle2.setVisibility(View.GONE);
					imgLink2.setVisibility(View.GONE);
					
					title += " posted a video";
					updateImageView(artistsNewsListItem, imgPhoto2, parent, pos);
					break;

				default:
					break;
				}
				
				txtTitle2.setText(title);
				txtTime2.setText(time);
				txtDesc2.setText(item.getPostDesc());
				
				rltLayoutNewsItem2Container.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						Intent browserIntent = new Intent(Intent.ACTION_VIEW, 
								Uri.parse(item.getPostUrl()));
						startActivity(browserIntent);
					}
				});
			}
			
			private void updateImageView(ArtistsNewsListItem artistsNewsListItem, ImageView imageView, 
					ViewGroup parent, int pos) {
				ArtistNewsItem item = artistsNewsListItem.item;
				if (item.getPostType() != PostType.link) {
					updateLayoutParams(imageView, artistsNewsListItem);
				}
				String key = item.getKey(ImgResolution.DEFAULT);
		        BitmapCache bitmapCache = BitmapCache.getInstance();
				Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
				if (bitmap != null) {
					//Log.i(TAG, "bitmap="+bitmap+", for pos="+pos);
					imageView.setImageBitmap(bitmap);
					//Log.d(TAG, "width = " + imageView.getWidth() + ", " + imageView.getDrawable().getIntrinsicWidth());
					//Log.d(TAG, "height = " + imageView.getHeight() + ", " + imageView.getDrawable().getIntrinsicHeight());
			        
			    } else {
					imageView.setImageBitmap(null);
			        AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
			        asyncLoadImg.loadImg(imageView, ImgResolution.DEFAULT, (AdapterView) parent, 
			        		pos, item);
			    }
			}
			
			private void updateLayoutParams(ImageView imageView, ArtistsNewsListItem artistsNewsListItem) {
				int setWidth = artistsNewsListItem.width;
				int setHeight = artistsNewsListItem.height;
				int width = (orientation == Configuration.ORIENTATION_PORTRAIT) ? widthPort : widthLan;
				if (width < artistsNewsListItem.width) {
					setWidth = width;
					setHeight = (int) Math.ceil((float) width * (float) artistsNewsListItem.height / (float) artistsNewsListItem.width);
				}
				//Log.d(TAG, "setWidth = " + setWidth + ", setHt = " + setHeight);
				LayoutParams lp = new LayoutParams(setWidth, setHeight);
				lp.addRule(RelativeLayout.BELOW, R.id.txtTime);
				lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
				lp.bottomMargin = IMG_MARGIN_B;
				imageView.setLayoutParams(lp);
			}
		}
	}
	
	private void chkCount() {
		//Log.d(TAG, "chkCount()");
		if (count == batchLoaded.size()) {
			//Log.d(TAG, "chkCount() addAll for count = " + count);
			artistsNewsListItems.addAll(artistsNewsListItems.size() - 1, batchLoaded);
			
			if (!isMoreDataAvailable) {
				artistsNewsListItems.remove(artistsNewsListItems.size() - 1);
			}
			count = 0;
			batchLoaded.clear();
			artistNewsListAdapter.notifyDataSetChanged();
		}
	}
	
	private class ArtistsNewsListItem implements AsyncLoadImageListener {
		private ArtistNewsItem item;
		private int width, height;
		
		public ArtistsNewsListItem(ArtistNewsItem item) {
			this.item = item;
			if (item.getImgUrl() != null && item.getPostType() != PostType.link) {
				loadImgDimension();
				
			} else {
				width = height = 0;
				count++;
				//Log.d(TAG, "width = height = 0, count = " + count);
			}
		}
		
		private void loadImgDimension() {
			String key = item.getKey(ImgResolution.DEFAULT);
	        BitmapCache bitmapCache = BitmapCache.getInstance();
			Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
			if (bitmap != null) {
				width = bitmap.getWidth();
				height = bitmap.getHeight();
				count++;
				
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
			count++;
			//Log.d(TAG, "count = " + count);
			chkCount();
		}

		@Override
		public void onImageCouldNotBeLoaded() {
			//Log.d(TAG, "onImageCouldNotBeLoaded()");
			width = height = 0;
			count++;
			//Log.d(TAG, "count = " + count);
			chkCount();
		}
	}
}
