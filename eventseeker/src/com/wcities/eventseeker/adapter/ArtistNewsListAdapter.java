package com.wcities.eventseeker.adapter;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.asynctask.LoadArtistNews.ArtistNewsListItem;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.ArtistNewsItem;
import com.wcities.eventseeker.core.ArtistNewsItem.PostType;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.ConversionUtil;

public class ArtistNewsListAdapter extends BaseAdapter {
	
	private static final String TAG = ArtistNewsListAdapter.class.getSimpleName();
	private int IMG_MARGIN_B, pad;
	
	private Context mContext;
    private LayoutInflater mInflater;
    private int orientation;
    private boolean isTablet;
    private boolean is7InchTabletInPortrait;
    private AsyncTask<Void, Void, List<ArtistNewsItem>> loadArtistNews;
    private List<ArtistNewsListItem> artistsNewsListItems;
    private int imgWidth;
    
    private boolean isMoreDataAvailable = true;
    private LoadItemsInBackgroundListener mListener;
    private int itemsAlreadyRequested;
    private List<ArtistNewsListItem> batchLoaded;
    
    private Resources res;
	private boolean isTrending;
    
    public ArtistNewsListAdapter(Context context, AsyncTask<Void, Void, List<ArtistNewsItem>> loadArtistNews, 
    		LoadItemsInBackgroundListener listener, List<ArtistNewsListItem> artistsNewsListItems, 
    		int width, boolean isTrending) {
    	mContext = context;
        this.loadArtistNews = loadArtistNews;
        mListener = listener;
        this.artistsNewsListItems = artistsNewsListItems;
        this.imgWidth = width;
        this.batchLoaded = new ArrayList<ArtistNewsListItem>();
        this.isTrending = isTrending;
        res = context.getResources();
        
        mInflater = LayoutInflater.from(context);
        IMG_MARGIN_B = mContext.getResources().getDimensionPixelSize(R.dimen.img_photo_margin_b_artist_news_item);
		pad = mContext.getResources().getDimensionPixelSize(R.dimen.tab_bar_margin_fragment_custom_tabs);
        orientation = mContext.getResources().getConfiguration().orientation;
        isTablet = ((EventSeekr)mContext.getApplicationContext()).isTablet();
        is7InchTabletInPortrait = ((EventSeekr)mContext.getApplicationContext()).is7InchTabletAndInPortraitMode();
    }
    
    public void updateContext(Context context) {
        mContext = context;
        orientation = mContext.getResources().getConfiguration().orientation;
        is7InchTabletInPortrait = ((EventSeekr)mContext.getApplicationContext()).is7InchTabletAndInPortraitMode();
	}
    
    public void setLoadArtistNews(AsyncTask<Void, Void, List<ArtistNewsItem>> loadArtistNews) {
		this.loadArtistNews = loadArtistNews;
	}
    
	public int getItemsAlreadyRequested() {
		return itemsAlreadyRequested;
	}

	public void setItemsAlreadyRequested(int itemsAlreadyRequested) {
		this.itemsAlreadyRequested = itemsAlreadyRequested;
	}

	public void setMoreDataAvailable(boolean isMoreDataAvailable) {
		this.isMoreDataAvailable = isMoreDataAvailable;
	}

	public boolean isMoreDataAvailable() {
		return isMoreDataAvailable;
	}

	public void setBatchLoaded(List<ArtistNewsListItem> batchLoaded) {
		this.batchLoaded = batchLoaded;
	}

	public void setImgWidth(int imgWidth) {
		this.imgWidth = imgWidth;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		//Log.d(TAG, "pos = " + position);
		Object item = getItem(position);
		if (item == null || 
				((item instanceof List) && ((List<ArtistNewsListItem>)item).get(0) == null)) {
			if (convertView == null || convertView.getTag() instanceof ArtistNewsItemViewHolder) {
				convertView = mInflater.inflate(R.layout.progress_bar_eventseeker_fixed_ht/*list_progress_bar*/, null);
				convertView.setTag(AppConstants.TAG_PROGRESS_INDICATOR);
				convertView.setBackgroundColor(mContext.getResources().getColor(R.color.root_lnr_layout_bg_artists_news_list_item));
			}
			
			if (artistsNewsListItems.size() == 1) {
				// Instead of this limited height progress bar, we display full screen progress bar from fragment
				convertView.setVisibility(View.INVISIBLE);
				
			} else {
				convertView.setVisibility(View.VISIBLE);
			}
			
			if ((loadArtistNews == null || loadArtistNews.getStatus() == Status.FINISHED) && 
					isMoreDataAvailable && batchLoaded.isEmpty()) {
				mListener.loadItemsInBackground();
			}
			
		} else {
			ArtistNewsItemViewHolder holder;
			if (((item instanceof List) && ((List<ArtistNewsListItem>)item).get(0).getItem().getArtistName().equals(AppConstants.INVALID_STR_ID)) 
					|| ((item instanceof ArtistNewsListItem) && ((ArtistNewsListItem)item).getItem().getArtistName().equals(AppConstants.INVALID_STR_ID))) {
				
				convertView = mInflater.inflate(R.layout.list_no_items_found, null);
				convertView.setTag("");
		
				if (((EventSeekr)mContext.getApplicationContext()).getWcitiesId() == null) {
					((TextView)convertView).setText(mContext.getResources().getString(R.string.pls_login_to_see_artist_news));
					
				} else {
					((TextView)convertView).setText(R.string.no_artist_news_found);
				}
				
				return convertView;
				
			} else if (convertView == null || !(convertView.getTag() instanceof ArtistNewsItemViewHolder)) {
				convertView = mInflater.inflate(R.layout.artists_fix_size_news_list_item, null);
				
				holder = new ArtistNewsItemViewHolder();
				holder.rootLnrLayout = (LinearLayout) convertView.findViewById(R.id.rootLnrLayout);

				RelativeLayout rltLayoutNewsItemContainer =  holder.rltLayoutNewsItemContainer 
						= (RelativeLayout) convertView.findViewById(R.id.rltLayoutNewsItemContainer);
				holder.imgPhoto = (ImageView) rltLayoutNewsItemContainer.findViewById(R.id.imgPhoto);
				holder.imgVideo = (ImageView) rltLayoutNewsItemContainer.findViewById(R.id.imgVideo);
				holder.txtTitle = (TextView) rltLayoutNewsItemContainer.findViewById(R.id.txtTitle);
				holder.txtTime = (TextView) rltLayoutNewsItemContainer.findViewById(R.id.txtTime);
				holder.imgLink = (ImageView) rltLayoutNewsItemContainer.findViewById(R.id.imgLink);
				holder.txtLinkTitle = (TextView) rltLayoutNewsItemContainer.findViewById(R.id.txtLinkTitle);
				holder.txtDesc = (TextView) rltLayoutNewsItemContainer.findViewById(R.id.txtDesc);
				holder.txtTrending = (TextView) rltLayoutNewsItemContainer.findViewById(R.id.txtTrending);
				
				RelativeLayout rltLayoutNewsItem2Container = holder.rltLayoutNewsItem2Container 
						= (RelativeLayout) convertView.findViewById(R.id.rltLayoutNewsItemContainer2);
				holder.imgPhoto2 = (ImageView) rltLayoutNewsItem2Container.findViewById(R.id.imgPhoto);
				holder.imgVideo2 = (ImageView) rltLayoutNewsItem2Container.findViewById(R.id.imgVideo);
				holder.txtTitle2 = (TextView) rltLayoutNewsItem2Container.findViewById(R.id.txtTitle);
				holder.txtTime2 = (TextView) rltLayoutNewsItem2Container.findViewById(R.id.txtTime);
				holder.imgLink2 = (ImageView) rltLayoutNewsItem2Container.findViewById(R.id.imgLink);
				holder.txtLinkTitle2 = (TextView) rltLayoutNewsItem2Container.findViewById(R.id.txtLinkTitle);
				holder.txtDesc2 = (TextView) rltLayoutNewsItem2Container.findViewById(R.id.txtDesc);
				holder.txtTrending2 = (TextView) rltLayoutNewsItemContainer.findViewById(R.id.txtTrending);
				
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
		
		if (is7InchTabletInPortrait || (orientation == Configuration.ORIENTATION_PORTRAIT && !isTablet)) {
			return artistsNewsListItems.get(position);
			
		} else {
			List<ArtistNewsListItem> artistNewsItemList = new ArrayList<ArtistNewsListItem>();
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
		if (is7InchTabletInPortrait || (orientation == Configuration.ORIENTATION_PORTRAIT && !isTablet)) {
			//Log.d(TAG, "getCount() = " + artistsNewsListItems.size());
			return artistsNewsListItems.size();
			
		} else {
			//Log.d(TAG, "else getCount() = " + (int)Math.ceil(artistsNewsListItems.size() / 2.0));
			return (int) Math.ceil(artistsNewsListItems.size() / 2.0);
		}
	}
	
	public class ArtistNewsItemViewHolder {
		

		private LinearLayout rootLnrLayout;

		private RelativeLayout rltLayoutNewsItemContainer;
		private ImageView imgPhoto;
		private TextView txtTitle, txtTime, txtLinkTitle, txtDesc, txtTrending;
		private ImageView imgLink, imgVideo;
		
		private RelativeLayout rltLayoutNewsItem2Container;
		private ImageView imgPhoto2;
		private TextView txtTitle2, txtTime2, txtLinkTitle2, txtDesc2, txtTrending2;
		private ImageView imgLink2, imgVideo2;
		
		public ImageView getImgPhoto() {
			return imgPhoto;
		}

		public ImageView getImgLink() {
			return imgLink;
		}

		public ImageView getImgPhoto2() {
			return imgPhoto2;
		}

		public ImageView getImgLink2() {
			return imgLink2;
		}

		private void setContent(Object listItem, ViewGroup parent, int pos) {
			//if (mListener instanceof ArtistNewsListFragment) {
				/**
				 * Padding needs to be readjusted in case of ArtistNewsListFragment since it has tabs 
				 * having their own content padding as well.
				 */
				if (orientation == Configuration.ORIENTATION_PORTRAIT) {
					int padTop = (pos == 0) ? 0 : pad;
					int padLeftRight = 0;
					rootLnrLayout.setPadding(padLeftRight, padTop, padLeftRight, 0);
					
				} else {
					int padTop = (pos == 0) ? 0 : pad;
					int padLeftRight = pad;
					rootLnrLayout.setPadding(padLeftRight, padTop, padLeftRight, 0);
				}
			//}
			
			if (listItem instanceof ArtistNewsListItem) {
				//Log.d(TAG, "ArtistNewsItem");
				rltLayoutNewsItem2Container.setVisibility(View.GONE);
				setNewsItemContent((ArtistNewsListItem) listItem, parent, pos);
				
			} else {
				List<ArtistNewsListItem> artistNewsItemList = (List<ArtistNewsListItem>) listItem;
				setNewsItemContent((ArtistNewsListItem) artistNewsItemList.get(0), parent, pos);
				if (artistNewsItemList.size() == 2) {
					//Log.d(TAG, "not ArtistNewsItem, size = 2");
					rltLayoutNewsItem2Container.setVisibility(View.VISIBLE);
					setNewsItem2Content((ArtistNewsListItem) artistNewsItemList.get(1), parent, pos);
					
				} else {
					//Log.d(TAG, "not ArtistNewsItem, size = 1");
					rltLayoutNewsItem2Container.setVisibility(View.INVISIBLE);
				}
			}
		}
		
		private void setNewsItemContent(ArtistNewsListItem artistsNewsListItem, ViewGroup parent, int pos) {
			final ArtistNewsItem item = artistsNewsListItem.getItem();
			String title = item.getArtistName();
			//String time = ConversionUtil.getTimeDiffFromCurrentTime(item.getTimestamp());
			String time = ConversionUtil.getTimeDiffFromCurrentTime(item.getTimestamp(), res);
			
			switch (item.getPostType()) {
			
			case status:
				imgPhoto.setVisibility(View.GONE);
				imgVideo.setVisibility(View.GONE);
				txtLinkTitle.setVisibility(View.GONE);
				imgLink.setVisibility(View.GONE);
				
				title += " " + res.getString(R.string.posted_a_status);
				break;
				
			case link:
				imgPhoto.setVisibility(View.GONE);
				imgVideo.setVisibility(View.GONE);
				txtLinkTitle.setVisibility(View.VISIBLE);
				
				title += " " + res.getString(R.string.shared_a_link);
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
				
				title += " " + res.getString(R.string.posted_a_picture);
				updateImageView(artistsNewsListItem, imgPhoto, parent, pos);
				break;
				
			case video:
			case swf:
				imgPhoto.setVisibility(View.VISIBLE);
				imgVideo.setVisibility(View.VISIBLE);
				txtLinkTitle.setVisibility(View.GONE);
				imgLink.setVisibility(View.GONE);
				
				title += " " + res.getString(R.string.posted_a_video);
				updateImageView(artistsNewsListItem, imgPhoto, parent, pos);
				break;

			default:
				break;
			}
			
			txtTitle.setText(title);
			txtTime.setText(time);
			txtDesc.setText(item.getPostDesc());
			txtTrending.setVisibility(isTrending ? View.VISIBLE : View.GONE);
			
			rltLayoutNewsItemContainer.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.getPostUrl()));
					mContext.startActivity(browserIntent);
				}
			});
		}
		
		private void setNewsItem2Content(ArtistNewsListItem artistsNewsListItem, ViewGroup parent, int pos) {
			final ArtistNewsItem item = artistsNewsListItem.getItem();
			String title = item.getArtistName();
			//String time = ConversionUtil.getTimeDiffFromCurrentTime(item.getTimestamp());
			String time = ConversionUtil.getTimeDiffFromCurrentTime(item.getTimestamp(), res);
			
			switch (item.getPostType()) {
			
			case status:
				imgPhoto2.setVisibility(View.GONE);
				imgVideo2.setVisibility(View.GONE);
				txtLinkTitle2.setVisibility(View.GONE);
				imgLink2.setVisibility(View.GONE);
				
				title += " " + res.getString(R.string.posted_a_status);
				break;
				
			case link:
				imgPhoto2.setVisibility(View.GONE);
				imgVideo2.setVisibility(View.GONE);
				txtLinkTitle2.setVisibility(View.VISIBLE);
				
				title += " " + res.getString(R.string.shared_a_link);
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
				
				title += " " + res.getString(R.string.posted_a_picture);
				updateImageView(artistsNewsListItem, imgPhoto2, parent, pos);
				break;
				
			case video:
			case swf:
				imgPhoto2.setVisibility(View.VISIBLE);
				imgVideo2.setVisibility(View.VISIBLE);
				txtLinkTitle2.setVisibility(View.GONE);
				imgLink2.setVisibility(View.GONE);
				
				title += " " + res.getString(R.string.posted_a_video);
				updateImageView(artistsNewsListItem, imgPhoto2, parent, pos);
				break;

			default:
				break;
			}
			
			txtTitle2.setText(title);
			txtTime2.setText(time);
			txtDesc2.setText(item.getPostDesc());
			txtTrending2.setVisibility(isTrending ? View.VISIBLE : View.GONE);
			
			rltLayoutNewsItem2Container.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.getPostUrl()));
					mContext.startActivity(browserIntent);
				}
			});
		}
		
		private void updateImageView(ArtistNewsListItem artistsNewsListItem, ImageView imageView, 
				ViewGroup parent, int pos) {
			ArtistNewsItem item = artistsNewsListItem.getItem();
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
		        asyncLoadImg.loadImg(imageView, ImgResolution.DEFAULT, (AdapterView) parent, pos, item);
		    }
		}
		
		private void updateLayoutParams(ImageView imageView, ArtistNewsListItem artistsNewsListItem) {
			int setWidth = artistsNewsListItem.getWidth();
			int setHeight = artistsNewsListItem.getHeight();
			//Log.d(TAG, "before setWidth = " + setWidth + ", setHt = " + setHeight);
			if (imgWidth < artistsNewsListItem.getWidth()) {
				setWidth = imgWidth;
				setHeight = (int) Math.ceil((float) imgWidth * (float) artistsNewsListItem.getHeight() / 
						(float) artistsNewsListItem.getWidth());
				//Log.d(TAG, "in if");
			}
			//Log.d(TAG, "after setWidth = " + setWidth + ", setHt = " + setHeight);
			LayoutParams lp = new LayoutParams(setWidth, setHeight);
			lp.addRule(RelativeLayout.BELOW, R.id.txtTime);
			lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
			lp.bottomMargin = IMG_MARGIN_B;
			imageView.setLayoutParams(lp);
		}
	}
}
