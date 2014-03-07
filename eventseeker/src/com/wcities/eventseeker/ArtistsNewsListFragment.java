package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView.RecyclerListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.wcities.eventseeker.DrawerListFragment.DrawerListFragmentListener;
import com.wcities.eventseeker.adapter.ArtistNewsListAdapter;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.LoadArtistNews;
import com.wcities.eventseeker.asynctask.LoadArtistNews.ArtistNewsListItem;
import com.wcities.eventseeker.asynctask.LoadArtistNews.OnNewsLoadedListener;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.ArtistNewsItem;
import com.wcities.eventseeker.custom.fragment.ListFragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class ArtistsNewsListFragment extends ListFragmentLoadableFromBackStack implements 
		LoadItemsInBackgroundListener, OnNewsLoadedListener, OnClickListener {
	
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

	private View rltDummyLyt;
	private ScrollView scrlVRootNoItemsFoundWithAction;
	
	/**
	 * Using its instance variable since otherwise calling getResources() directly from fragment from 
	 * callback methods is dangerous in a sense that it may throw java.lang.IllegalStateException: 
	 * Fragment not attached to Activity, if user has already left this fragment & 
	 * then changed the orientation.
	 */
	private Resources res;
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (!(activity instanceof DrawerListFragmentListener)) {
            throw new ClassCastException(activity.toString() + " must implement DrawerListFragmentListener");
        }
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
		isTablet = ((EventSeekr)FragmentUtil.getActivity(this).getApplicationContext()).isTablet();

        if (wcitiesId == null) {
        	wcitiesId = ((EventSeekr)FragmentUtil.getActivity(this).getApplication()).getWcitiesId();
        }
        res = getResources();
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
		
		super.onCreateView(inflater, container, savedInstanceState);
		View v = inflater.inflate(R.layout.fragment_artists_news_list, null);
		rltDummyLyt = v.findViewById(R.id.rltDummyLyt);
		scrlVRootNoItemsFoundWithAction = (ScrollView) v.findViewById(R.id.scrlVRootNoItemsFoundWithAction);
		v.findViewById(R.id.btnAction).setOnClickListener(this);
		return v;
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
			changeRltDummyLytVisibility();
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
		loadArtistsNews = new LoadArtistNews(artistNewsListAdapter, wcitiesId, artistsNewsListItems, null, this);
		artistNewsListAdapter.setLoadArtistNews(loadArtistsNews);
		AsyncTaskUtil.executeAsyncTask(loadArtistsNews, true);
	}
	
	@Override
	public void onNewsLoaded() {
		changeRltDummyLytVisibility();
	}

	private void changeRltDummyLytVisibility() {
		if (artistsNewsListItems.size() == 1 && artistsNewsListItems.get(0) != null
				&& ((ArtistNewsItem)((ArtistNewsListItem)artistsNewsListItems.get(0))
						.getItem()).getArtistName().equals(AppConstants.INVALID_STR_ID)) {
			if (wcitiesId == null) {
				rltDummyLyt.setVisibility(View.VISIBLE);
				
			} else {
				setNoItemsLayout();
				/**
				 * try-catch is used to handle case where even before we get call back to this function, user leaves 
				 * this screen.
				 */
				try {
					getListView().setVisibility(View.GONE);
					
				} catch (IllegalStateException e) {
					Log.e(TAG, "" + e.getMessage());
					e.printStackTrace();
				}
			}
			
		} else {
			rltDummyLyt.setVisibility(View.GONE);
			scrlVRootNoItemsFoundWithAction.setVisibility(View.GONE);
		}				
	}
	
	private void setNoItemsLayout() {
		scrlVRootNoItemsFoundWithAction.setVisibility(View.VISIBLE);
		((TextView)scrlVRootNoItemsFoundWithAction.findViewById(R.id.txtNoItemsHeading)).setText(
				R.string.search_artists);
		((TextView)scrlVRootNoItemsFoundWithAction.findViewById(R.id.txtNoItemsMsg)).setText(
				R.string.follow_artists_for_updates);
		((Button)scrlVRootNoItemsFoundWithAction.findViewById(R.id.btnAction)).setText(
				R.string.search_artists);
		((ImageView)scrlVRootNoItemsFoundWithAction.findViewById(R.id.imgNoItems)).setImageDrawable(
				res.getDrawable(R.drawable.no_artists_news));
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.btnAction:
			((DrawerListFragmentListener)FragmentUtil.getActivity(this)).onDrawerItemSelected(
					MainActivity.INDEX_NAV_ITEM_FOLLOWING);
			break;

		default:
			break;
		}
	}
}
