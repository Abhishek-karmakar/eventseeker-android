package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.List;

import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.RecyclerListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import com.wcities.eventseeker.adapter.ArtistNewsListAdapter;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.LoadArtistNews;
import com.wcities.eventseeker.asynctask.LoadArtistNews.ArtistNewsListItem;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.ArtistNewsItem;
import com.wcities.eventseeker.custom.fragment.ListFragmentLoadableFromBackStack;
import com.wcities.eventseeker.custom.view.ResizableImageView;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class ArtistNewsListFragment extends ListFragmentLoadableFromBackStack implements LoadItemsInBackgroundListener {
	
	protected static final String TAG = ArtistNewsListFragment.class.getName();

	private Artist artist;
	private LoadArtistNews loadArtistNews;
	private ArtistNewsListAdapter artistNewsListAdapter;

	private int orientation;
	private String wcitiesId;
	private int imgWidth;
	private List<ArtistNewsListItem> artistNewsListItems;
	
	private int firstVisibleNewsItemPosition;
	private boolean isTablet;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		artist = (Artist) getArguments().getSerializable(BundleKeys.ARTIST);
		isTablet = ((MainActivity)FragmentUtil.getActivity(this)).isTablet();
		
		if (wcitiesId == null) {
			wcitiesId = ((EventSeekr)FragmentUtil.getActivity(this).getApplication()).getWcitiesId();
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		//Log.d(TAG, "onCreateView()");
		orientation = getResources().getConfiguration().orientation;
		View v = super.onCreateView(inflater, container, savedInstanceState);
		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		lp.addRule(RelativeLayout.ABOVE, R.id.fragmentArtistDetailsFooter);
		v.setLayoutParams(lp);
		
		int screenW = getResources().getDisplayMetrics().widthPixels;
        //Log.d(TAG, "w = " + screenW);

		if (orientation == Configuration.ORIENTATION_PORTRAIT) {

	        if (isTablet) {
	        	imgWidth = (screenW - (getResources().getDimensionPixelSize(R.dimen.root_lnr_layout_pad_l_artists_news_list_item) * 2)
						- (getResources().getDimensionPixelSize(R.dimen.rlt_layout_news_item_container_pad_artist_news_item) * 4)
						- (getResources().getDimensionPixelSize(R.dimen.rlt_layout_news_item_container2_margin_l_artists_news_list_item)))/2;
				
			} else {			
				imgWidth = screenW - (getResources().getDimensionPixelSize(R.dimen.root_lnr_layout_pad_l_artists_news_list_item) * 2)
	                        - (getResources().getDimensionPixelSize(R.dimen.rlt_layout_news_item_container_pad_artist_news_item) * 2);
			}	
	        
		} else {
			if (isTablet) {
				imgWidth = (screenW - (getResources().getDimensionPixelSize(R.dimen.root_lnr_layout_pad_l_artists_news_list_item) * 2)
						- (getResources().getDimensionPixelSize(R.dimen.rlt_layout_news_item_container_pad_artist_news_item) * 4)
						- (getResources().getDimensionPixelSize(R.dimen.rlt_layout_news_item_container2_margin_l_artists_news_list_item))
						- (getResources().getDimensionPixelSize(R.dimen.root_navigation_drawer_w_main)))/2;
				
			} else {			
				imgWidth = (screenW - (getResources().getDimensionPixelSize(R.dimen.root_lnr_layout_pad_l_artists_news_list_item) * 2)
						- (getResources().getDimensionPixelSize(R.dimen.rlt_layout_news_item_container_pad_artist_news_item) * 4)
						- (getResources().getDimensionPixelSize(R.dimen.rlt_layout_news_item_container2_margin_l_artists_news_list_item))) / 2;
			}	
		}
        //Log.d(TAG, "width = " + imgWidth);
        
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if (artistNewsListItems == null) {
			artistNewsListItems = new ArrayList<ArtistNewsListItem>();
			artistNewsListAdapter = new ArtistNewsListAdapter(FragmentUtil.getActivity(this), null, 
					this, artistNewsListItems, imgWidth);
	        
			artistNewsListItems.add(null);
			loadItemsInBackground();
	        
		} else {
			artistNewsListAdapter.updateContext(FragmentUtil.getActivity(this));
			artistNewsListAdapter.setImgWidth(imgWidth);
		}

		setListAdapter(artistNewsListAdapter);
		getListView().setDivider(null);
		getListView().setScrollingCacheEnabled(false);
		
		final int pos = (orientation == Configuration.ORIENTATION_PORTRAIT && !isTablet) ? 
				firstVisibleNewsItemPosition : (int)Math.floor(firstVisibleNewsItemPosition / 2.0);
		//Log.d(TAG, "onActivityCreated() firstVisibleNewsItemPosition = " + firstVisibleNewsItemPosition + ", pos = " + pos);
		getListView().post(new Runnable() {
			
			@Override
			public void run() {
				// TODO: remove following try-catch handling if not required
				try {
					setSelection(pos);
					
				} catch (IllegalStateException e) {
					Log.e(TAG, "" + e.getMessage());
					e.printStackTrace();
				}
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
	
			ImageView imgPhoto = holder.getImgPhoto();
			imgPhoto.setImageBitmap(null);
			
			ImageView imgLink = holder.getImgLink();
			imgLink.setImageBitmap(null);
			
			imgPhoto = holder.getImgPhoto2();
			imgPhoto.setImageBitmap(null);
			
			imgLink = holder.getImgLink2();
			imgLink.setImageBitmap(null);
		}
	}
	
	@Override
	public void loadItemsInBackground() {
		loadArtistNews = new LoadArtistNews(artistNewsListAdapter, wcitiesId, artistNewsListItems, artist);
		artistNewsListAdapter.setLoadArtistNews(loadArtistNews);
		AsyncTaskUtil.executeAsyncTask(loadArtistNews, true);
	}
}
