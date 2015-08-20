package com.wcities.eventseeker.adapter;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.asynctask.LoadArtistNews.ArtistNewsListItem;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.ArtistNewsItem;
import com.wcities.eventseeker.core.ArtistNewsItem.PostType;
import com.wcities.eventseeker.custom.view.CircleImageView;
import com.wcities.eventseeker.interfaces.FullScrnProgressListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.FragmentUtil;

import java.util.ArrayList;
import java.util.List;

public class ArtistNewsListAdapterTab extends BaseAdapter {
	
	private static final String TAG = ArtistNewsListAdapterTab.class.getSimpleName();
	private int IMG_MARGIN_B, pad;
	
    private int orientation;
    private int imgWidth;
    private int itemsAlreadyRequested;
    
    private boolean isMoreDataAvailable = true;
	private boolean isTrending;
	
	private LoadItemsInBackgroundListener mListener;

	private List<ArtistNewsListItem> artistsNewsListItems;
	private List<ArtistNewsListItem> batchLoaded;

	private AsyncTask<Void, Void, List<ArtistNewsItem>> loadArtistNews;
	
	private Fragment fragment;
    
    public ArtistNewsListAdapterTab(Fragment fragment, AsyncTask<Void, Void, List<ArtistNewsItem>> loadArtistNews, 
    		LoadItemsInBackgroundListener listener, List<ArtistNewsListItem> artistsNewsListItems, 
    		int width, boolean isTrending) {
    	this.fragment = fragment;
        this.loadArtistNews = loadArtistNews;
        mListener = listener;
        this.artistsNewsListItems = artistsNewsListItems;
        this.imgWidth = width;
        this.batchLoaded = new ArrayList<ArtistNewsListItem>();
        this.isTrending = isTrending;
        Resources res = FragmentUtil.getResources(fragment);
        
        IMG_MARGIN_B = res.getDimensionPixelSize(R.dimen.img_photo_margin_b_artist_news_item);
		pad = res.getDimensionPixelSize(R.dimen.tab_bar_margin_fragment_custom_tabs);
        orientation = res.getConfiguration().orientation;
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
		LayoutInflater inflater = LayoutInflater.from(FragmentUtil.getActivity(fragment));
		Object item = getItem(position);
		if (item == null) {
			if (convertView == null || convertView.getTag() instanceof ArtistNewsItemViewHolder) {
				convertView = inflater.inflate(R.layout.progress_bar_eventseeker_fixed_ht, null);
				convertView.setTag(AppConstants.TAG_PROGRESS_INDICATOR);
			}
			
			if (artistsNewsListItems.size() == 1) {
				// Instead of this limited height progress bar, we display full screen progress bar from fragment
				convertView.setVisibility(View.INVISIBLE);
				if (mListener instanceof FullScrnProgressListener) {
					((FullScrnProgressListener) mListener).displayFullScrnProgress();
				}
				
			} else {
				convertView.setVisibility(View.VISIBLE);
			}
			
			if ((loadArtistNews == null || loadArtistNews.getStatus() == Status.FINISHED) && 
					isMoreDataAvailable && batchLoaded.isEmpty()) {
				mListener.loadItemsInBackground();
			}
			
		} else {
			ArtistNewsItemViewHolder holder;
			if (((ArtistNewsListItem)item).getItem().getArtist().getName().equals(AppConstants.INVALID_STR_ID)) {
				convertView = inflater.inflate(R.layout.list_no_items_found, null);
				convertView.setTag("");
				
				((TextView)convertView).setText(R.string.no_artist_news_found);
				
				return convertView;
				
			} else if (convertView == null || !(convertView.getTag() instanceof ArtistNewsItemViewHolder)) {
				convertView = inflater.inflate(R.layout.artists_fix_size_news_list_item_tab, null);
				
				holder = new ArtistNewsItemViewHolder();
				holder.rltRootLayout = (RelativeLayout) convertView.findViewById(R.id.rltRootLayout);

				RelativeLayout rltLayoutNewsItemContainer =  holder.rltLayoutNewsItemContainer 
						= (RelativeLayout) convertView.findViewById(R.id.rltLayoutNewsItemContainer);
				holder.imgCrclArtist = (CircleImageView) rltLayoutNewsItemContainer.findViewById(R.id.imgCrclArtist);
				holder.imgPhoto = (ImageView) rltLayoutNewsItemContainer.findViewById(R.id.imgPhoto);
				holder.imgVideo = (ImageView) rltLayoutNewsItemContainer.findViewById(R.id.imgVideo);
				holder.txtTitle = (TextView) rltLayoutNewsItemContainer.findViewById(R.id.txtTitle);
				holder.txtTime = (TextView) rltLayoutNewsItemContainer.findViewById(R.id.txtTime);
				holder.imgLink = (ImageView) rltLayoutNewsItemContainer.findViewById(R.id.imgLink);
				holder.txtLinkTitle = (TextView) rltLayoutNewsItemContainer.findViewById(R.id.txtLinkTitle);
				holder.txtDesc = (TextView) rltLayoutNewsItemContainer.findViewById(R.id.txtDesc);
				holder.txtTrending = (TextView) rltLayoutNewsItemContainer.findViewById(R.id.txtTrending);

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
		return artistsNewsListItems.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public int getCount() {
		return artistsNewsListItems.size();
	}
	
	public class ArtistNewsItemViewHolder {
		private RelativeLayout rltRootLayout, rltLayoutNewsItemContainer;
		private CircleImageView imgCrclArtist;
		private ImageView imgPhoto;
		private TextView txtTitle, txtTime, txtLinkTitle, txtDesc, txtTrending;
		private ImageView imgLink, imgVideo;
		
		public ImageView getImgPhoto() {
			return imgPhoto;
		}

		public ImageView getImgLink() {
			return imgLink;
		}

		private void setContent(Object listItem, ViewGroup parent, int pos) {
			/**
			 * Padding needs to be readjusted in case of ArtistNewsListFragment since it has tabs 
			 * having their own content padding as well.
			 */
			if (orientation == Configuration.ORIENTATION_PORTRAIT) {
				int padTop = (pos == 0) ? 0 : pad;
				int padLeftRight = 0;
				rltRootLayout.setPadding(padLeftRight, padTop, padLeftRight, 0);
				
			}
			setNewsItemContent((ArtistNewsListItem) listItem, parent, pos);
		}
		
		private void setNewsItemContent(ArtistNewsListItem artistsNewsListItem, ViewGroup parent, int pos) {
			final ArtistNewsItem item = artistsNewsListItem.getItem();
			String title = item.getArtist().getName();
			//String time = ConversionUtil.getTimeDiffFromCurrentTime(item.getTimestamp());
			String time = ConversionUtil.getTimeDiffFromCurrentTime(item.getTimestamp(), FragmentUtil.getResources(fragment));
						
			String key = item.getArtist().getKey(ImgResolution.LOW);
			BitmapCache bitmapCache = BitmapCache.getInstance();
			Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
			if (bitmap != null) {
				imgCrclArtist.setImageBitmap(bitmap);
				
			} else {
				imgCrclArtist.setImageResource(R.drawable.ic_profile);
				AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
				asyncLoadImg.loadFBUserImg(imgCrclArtist, ImgResolution.LOW, (AdapterView) parent, pos, item.getArtist());
			}
			
			switch (item.getPostType()) {
			
			case status:
				imgPhoto.setVisibility(View.GONE);
				imgVideo.setVisibility(View.GONE);
				txtLinkTitle.setVisibility(View.GONE);
				imgLink.setVisibility(View.GONE);
				
				title += " " + FragmentUtil.getResources(fragment).getString(R.string.posted_a_status);
				break;
				
			case link:
				imgPhoto.setVisibility(View.GONE);
				imgVideo.setVisibility(View.GONE);
				txtLinkTitle.setVisibility(View.VISIBLE);
				
				title += " " + FragmentUtil.getResources(fragment).getString(R.string.shared_a_link);
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
				
				title += " " + FragmentUtil.getResources(fragment).getString(R.string.posted_a_picture);
				updateImageView(artistsNewsListItem, imgPhoto, parent, pos);
				break;
				
			case video:
			case swf:
				imgPhoto.setVisibility(View.VISIBLE);
				imgVideo.setVisibility(View.VISIBLE);
				txtLinkTitle.setVisibility(View.GONE);
				imgLink.setVisibility(View.GONE);
				
				title += " " + FragmentUtil.getResources(fragment).getString(R.string.posted_a_video);
				updateImageView(artistsNewsListItem, imgPhoto, parent, pos);
				break;
				
			case event:
				imgPhoto.setVisibility(View.GONE);
				imgVideo.setVisibility(View.GONE);
				txtLinkTitle.setVisibility(View.GONE);
				imgLink.setVisibility(View.GONE);
				
				title = FragmentUtil.getResources(fragment).getString(R.string.latest_from) + " " + title;
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
					FragmentUtil.getActivity(fragment).startActivity(browserIntent);
				}
			});
		}
		
		private void updateImageView(ArtistNewsListItem artistsNewsListItem, ImageView imageView, 
				ViewGroup parent, int pos) {
			ArtistNewsItem item = artistsNewsListItem.getItem();
			if (item.getPostType() != PostType.link && item.getPostType() != PostType.event) {
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
			RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(setWidth, setHeight);
			lp.addRule(RelativeLayout.BELOW, R.id.txtTime);
			lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
			lp.bottomMargin = IMG_MARGIN_B;
			imageView.setLayoutParams(lp);
		}
	}
}
