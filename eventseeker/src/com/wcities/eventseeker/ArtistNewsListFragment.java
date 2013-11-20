package com.wcities.eventseeker;

import java.io.IOException;
import java.util.ArrayList;
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
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.ArtistNewsItem;
import com.wcities.eventseeker.custom.fragment.ListFragmentLoadableFromBackStack;
import com.wcities.eventseeker.custom.view.ResizableImageView;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class ArtistNewsListFragment extends ListFragmentLoadableFromBackStack {
	
	protected static final String TAG = ArtistNewsListFragment.class.getName();

	private Artist artist;
	private LoadArtistNews loadArtistNews;
	private ArtistNewsListAdapter artistNewsListAdapter;

	private int orientation;
	private String wcitiesId;
	private List<ArtistNewsItem> artistNewsItems;
	
	private int itemsAlreadyRequested;
	private boolean isMoreDataAvailable = true;
	private int firstVisibleNewsItemPosition;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		artist = (Artist) getArguments().getSerializable(BundleKeys.ARTIST);

		if (wcitiesId == null) {
			wcitiesId = ((EventSeekr)FragmentUtil.getActivity(this).getApplication()).getWcitiesId();
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		//Log.d(TAG, "onCreateView()");
		orientation = getResources().getConfiguration().orientation;
		View v = super.onCreateView(inflater, container, savedInstanceState);
		LayoutParams lp = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		lp.addRule(RelativeLayout.ABOVE, R.id.fragmentArtistDetailsFooter);
		v.setLayoutParams(lp);
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if (artistNewsItems == null) {
			artistNewsItems = new ArrayList<ArtistNewsItem>();
			artistNewsListAdapter = new ArtistNewsListAdapter(FragmentUtil.getActivity(this));
	        
			artistNewsItems.add(null);
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
		//Log.d(TAG, "onDestroyView()");
		/**
		 * We can not write following statement in onSaveInstanceState(), because 3 tabs are there on ArtistDetails screen & this
		 * is 3rd tab. If user swipes back to first tab, then view gets destroyed by this method onDestroyView() of list of 3rd tab.
		 * Then on orientation change onSaveInstanceState() will try to call getListView().getFirstVisiblePosition() throwing
		 * IllegalStateException: Content view not yet created, because listview is already destroyed.
		 */
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
	
			ResizableImageView imgPhoto = holder.imgPhoto;
			imgPhoto.setImageBitmap(null);
			
			ResizableImageView imgPhoto2 = holder.imgPhoto2;
			imgPhoto2.setImageBitmap(null);
			
			ImageView imgLink = holder.imgLink;
			imgLink.setImageBitmap(null);
			
			ImageView imgLink2 = holder.imgLink2;
			imgLink2.setImageBitmap(null);
		}
	}
	
	private void loadArtistsNewsInBackground() {
		loadArtistNews = new LoadArtistNews();
		loadArtistNews.execute();
	}
	
	private class LoadArtistNews extends AsyncTask<Void, Void, List<ArtistNewsItem>> {
		
		private static final int ARTISTS_NEWS_LIMIT = 10;
		
		@Override
		protected List<ArtistNewsItem> doInBackground(Void... params) {
			List<ArtistNewsItem> tmpArtistNewsItems = new ArrayList<ArtistNewsItem>();
			UserInfoApi userInfoApi = new UserInfoApi(Api.OAUTH_TOKEN);
			userInfoApi.setLimit(ARTISTS_NEWS_LIMIT);
			userInfoApi.setAlreadyRequested(itemsAlreadyRequested);
			userInfoApi.setUserId(wcitiesId);
			userInfoApi.setArtistId(artist.getId());

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
		protected void onPostExecute(List<ArtistNewsItem> itemsFetched) {
			if (itemsFetched.size() > 0) {
				artistNewsItems.addAll(artistNewsItems.size() - 1, itemsFetched);
				itemsAlreadyRequested += itemsFetched.size();
				
				if (itemsFetched.size() < ARTISTS_NEWS_LIMIT) {
					isMoreDataAvailable = false;
					artistNewsItems.remove(artistNewsItems.size() - 1);
				}
				
			} else {
				isMoreDataAvailable = false;
				artistNewsItems.remove(artistNewsItems.size() - 1);
			}
			artistNewsListAdapter.notifyDataSetChanged();
		}    	
    }
	
	private class ArtistNewsListAdapter extends BaseAdapter {
		
		private static final String TAG_PROGRESS_INDICATOR = "progressIndicator";
		
	    private LayoutInflater mInflater;
	    private int pad;

	    public ArtistNewsListAdapter(Context context) {
	        mInflater = LayoutInflater.from(context);
			pad = getResources().getDimensionPixelSize(R.dimen.tab_bar_margin_fragment_custom_tabs);
	    }
	    
	    public void setmInflater(Context context) {
	        mInflater = LayoutInflater.from(context);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			//Log.d(TAG, "pos = " + position);
			Object item = getItem(position);
			if (item == null || ((item instanceof List) && ((List<ArtistNewsItem>)item).get(0) == null)) {
				convertView = mInflater.inflate(R.layout.list_progress_bar, null);
				convertView.setTag(TAG_PROGRESS_INDICATOR);
				
				if ((loadArtistNews == null || loadArtistNews.getStatus() == Status.FINISHED) && 
						isMoreDataAvailable) {
					loadArtistsNewsInBackground();
				}
				
			} else {
				ArtistNewsItemViewHolder holder;
				
				if (convertView == null || !(convertView.getTag() instanceof ArtistNewsItemViewHolder)) {
					convertView = mInflater.inflate(R.layout.artists_news_list_item, null);
					
					holder = new ArtistNewsItemViewHolder();
					holder.rootLnrLayout = (LinearLayout) convertView.findViewById(R.id.rootLnrLayout);
					
					RelativeLayout rltLayoutNewsItemContainer =  holder.rltLayoutNewsItemContainer 
							= (RelativeLayout) convertView.findViewById(R.id.rltLayoutNewsItemContainer);
					holder.imgPhoto = (ResizableImageView) rltLayoutNewsItemContainer.findViewById(R.id.imgPhoto);
					holder.imgVideo = (ImageView) rltLayoutNewsItemContainer.findViewById(R.id.imgVideo);
					holder.txtTitle = (TextView) rltLayoutNewsItemContainer.findViewById(R.id.txtTitle);
					holder.txtTime = (TextView) rltLayoutNewsItemContainer.findViewById(R.id.txtTime);
					holder.imgLink = (ImageView) rltLayoutNewsItemContainer.findViewById(R.id.imgLink);
					holder.txtLinkTitle = (TextView) rltLayoutNewsItemContainer.findViewById(R.id.txtLinkTitle);
					holder.txtDesc = (TextView) rltLayoutNewsItemContainer.findViewById(R.id.txtDesc);
					
					RelativeLayout rltLayoutNewsItem2Container = holder.rltLayoutNewsItem2Container 
							= (RelativeLayout) convertView.findViewById(R.id.rltLayoutNewsItemContainer2);
					holder.imgPhoto2 = (ResizableImageView) rltLayoutNewsItem2Container.findViewById(R.id.imgPhoto);
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
				return artistNewsItems.get(position);
				
			} else {
				List<ArtistNewsItem> artistNewsItemList = new ArrayList<ArtistNewsItem>();
				artistNewsItemList.add(artistNewsItems.get(position * 2));
				if (artistNewsItems.size() > position * 2 + 1) {
					artistNewsItemList.add(artistNewsItems.get(position * 2 + 1));
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
				return artistNewsItems.size();
				
			} else {
				//Log.d(TAG, "count = " + (Math.ceil(artistNewsItems.size() - 1 / 2.0)));
				return (int) Math.ceil(artistNewsItems.size() / 2.0);
			}
		}
		
		private class ArtistNewsItemViewHolder {
			
			private LinearLayout rootLnrLayout;
			
			private RelativeLayout rltLayoutNewsItemContainer;
			private ResizableImageView imgPhoto;
			private TextView txtTitle, txtTime, txtLinkTitle, txtDesc;
			private ImageView imgLink, imgVideo;
			
			private RelativeLayout rltLayoutNewsItem2Container;
			private ResizableImageView imgPhoto2;
			private TextView txtTitle2, txtTime2, txtLinkTitle2, txtDesc2;
			private ImageView imgLink2, imgVideo2;
			
			private void setContent(Object listItem, ViewGroup parent, int pos) {
				if (orientation == Configuration.ORIENTATION_PORTRAIT) {
					int padTop = (pos == 0) ? 0 : pad;
					int padLeftRight = 0;
					rootLnrLayout.setPadding(padLeftRight, padTop, padLeftRight, 0);
					
				} else {
					int padTop = (pos == 0) ? 0 : pad;
					int padLeftRight = pad;
					rootLnrLayout.setPadding(padLeftRight, padTop, padLeftRight, 0);
				}
				
				if (listItem instanceof ArtistNewsItem) {
					//Log.d(TAG, "ArtistNewsItem");
					rltLayoutNewsItem2Container.setVisibility(View.GONE);
					setNewsItemContent((ArtistNewsItem) listItem, parent, pos);
					
				} else {
					List<ArtistNewsItem> artistNewsItemList = (List<ArtistNewsItem>) listItem;
					setNewsItemContent((ArtistNewsItem) artistNewsItemList.get(0), parent, pos);
					if (artistNewsItemList.size() == 2) {
						//Log.d(TAG, "not ArtistNewsItem, size = 2");
						rltLayoutNewsItem2Container.setVisibility(View.VISIBLE);
						setNewsItem2Content((ArtistNewsItem) artistNewsItemList.get(1), parent, pos);
						
					} else {
						//Log.d(TAG, "not ArtistNewsItem, size = 1");
						rltLayoutNewsItem2Container.setVisibility(View.INVISIBLE);
					}
				}
			}
			
			private void setNewsItemContent(final ArtistNewsItem item, ViewGroup parent, int pos) {
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
						updateImageView(item, imgLink, parent, pos);
						
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
					updateImageView(item, imgPhoto, parent, pos);
					break;
					
				case video:
					imgPhoto.setVisibility(View.VISIBLE);
					imgVideo.setVisibility(View.VISIBLE);
					txtLinkTitle.setVisibility(View.GONE);
					imgLink.setVisibility(View.GONE);
					
					title += " posted a video";
					updateImageView(item, imgPhoto, parent, pos);
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
			
			private void setNewsItem2Content(final ArtistNewsItem item, ViewGroup parent, int pos) {
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
						updateImageView(item, imgLink2, parent, pos);
						
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
					updateImageView(item, imgPhoto2, parent, pos);
					break;
					
				case video:
					imgPhoto2.setVisibility(View.VISIBLE);
					imgVideo2.setVisibility(View.VISIBLE);
					txtLinkTitle2.setVisibility(View.GONE);
					imgLink2.setVisibility(View.GONE);
					
					title += " posted a video";
					updateImageView(item, imgPhoto2, parent, pos);
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
			
			private void updateImageView(ArtistNewsItem item, ImageView imageView, 
					ViewGroup parent, int pos) {
				String key = item.getKey(ImgResolution.DEFAULT);
		        BitmapCache bitmapCache = BitmapCache.getInstance();
				Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
				if (bitmap != null) {
					//Log.i(TAG, "bitmap="+bitmap+", for pos="+pos);
					imageView.setImageBitmap(bitmap);
			        
			    } else {
					imageView.setImageBitmap(null);
			        AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
			        asyncLoadImg.loadImg(imageView, ImgResolution.DEFAULT, (AdapterView) parent, 
			        		pos, item);
			    }
			}
		}
	}
}
