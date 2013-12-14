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

import com.wcities.eventseeker.adapter.ArtistNewsListAdapter;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.LoadArtistNews;
import com.wcities.eventseeker.asynctask.LoadArtistNews.ArtistNewsListItem;
import com.wcities.eventseeker.custom.fragment.ListFragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class ArtistsNewsListFragment extends ListFragmentLoadableFromBackStack implements LoadItemsInBackgroundListener {
	
	protected static final String TAG = ArtistsNewsListFragment.class.getName();

	private LoadArtistNews loadArtistsNews;
	private ArtistNewsListAdapter artistNewsListAdapter;
	
	private int orientation;
	private String wcitiesId;
	private int imgWidth;
	private List<ArtistNewsListItem> artistsNewsListItems;
	
	private int firstVisibleNewsItemPosition;

	private boolean isTablet;
	private boolean is7InchTabletInPortrait;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
		isTablet = ((EventSeekr)FragmentUtil.getActivity(this).getApplicationContext()).isTablet();

        if (wcitiesId == null) {
        	wcitiesId = ((EventSeekr)FragmentUtil.getActivity(this).getApplication()).getWcitiesId();
        }
    }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		orientation = getResources().getConfiguration().orientation;
		is7InchTabletInPortrait = ((EventSeekr)FragmentUtil.getActivity(this).getApplicationContext())
				.is7InchTabletAndInPortraitMode();

		int screenW = getResources().getDisplayMetrics().widthPixels;
        //Log.d(TAG, "w = " + screenW);

		if (orientation == Configuration.ORIENTATION_PORTRAIT) {

	        if (isTablet && !is7InchTabletInPortrait) {
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
		
		return super.onCreateView(inflater, container, savedInstanceState);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if (artistsNewsListItems == null) {
			artistsNewsListItems = new ArrayList<ArtistNewsListItem>();
			artistNewsListAdapter = new ArtistNewsListAdapter(FragmentUtil.getActivity(this), null, 
					this, artistsNewsListItems, imgWidth);
	        
			artistsNewsListItems.add(null);
			loadItemsInBackground();
			
		} else {
			artistNewsListAdapter.updateContext(FragmentUtil.getActivity(this));
			artistNewsListAdapter.setImgWidth(imgWidth);
		}

		setListAdapter(artistNewsListAdapter);
		getListView().setDivider(null);
		getListView().setScrollingCacheEnabled(false);
		
		final int pos;
		if(is7InchTabletInPortrait) {
			pos = firstVisibleNewsItemPosition;
		} else if (orientation == Configuration.ORIENTATION_PORTRAIT && !isTablet) {
			pos = firstVisibleNewsItemPosition;			
		} else {
			pos = (int)Math.floor(firstVisibleNewsItemPosition / 2.0);
		}
		
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
		//Log.i(TAG, "onDestroyView()");
		
		if(is7InchTabletInPortrait) {
			firstVisibleNewsItemPosition = getListView().getFirstVisiblePosition();
		} else if (orientation == Configuration.ORIENTATION_PORTRAIT && !isTablet) {
			firstVisibleNewsItemPosition = getListView().getFirstVisiblePosition();			
		} else {
			firstVisibleNewsItemPosition = getListView().getFirstVisiblePosition() * 2;;
		}
		
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
		loadArtistsNews = new LoadArtistNews(artistNewsListAdapter, wcitiesId, artistsNewsListItems, null);
		artistNewsListAdapter.setLoadArtistNews(loadArtistsNews);
		AsyncTaskUtil.executeAsyncTask(loadArtistsNews, true);
	}
}
