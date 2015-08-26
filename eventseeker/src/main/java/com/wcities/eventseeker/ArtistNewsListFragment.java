package com.wcities.eventseeker;

import android.content.res.Configuration;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AbsListView.RecyclerListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wcities.eventseeker.RadioGroupDialogFragment.OnValueSelectedListener;
import com.wcities.eventseeker.adapter.ArtistNewsListAdapter;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.LoadArtistNews;
import com.wcities.eventseeker.asynctask.LoadArtistNews.ArtistNewsListItem;
import com.wcities.eventseeker.asynctask.LoadArtistNews.OnNewsLoadedListener;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.constants.Enums.SortArtistNewsBy;
import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.ArtistNewsItem;
import com.wcities.eventseeker.custom.fragment.ListFragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.interfaces.FullScrnProgressListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FragmentUtil;

import java.util.ArrayList;
import java.util.List;

public class ArtistNewsListFragment extends ListFragmentLoadableFromBackStack implements 
		LoadItemsInBackgroundListener, OnNewsLoadedListener, AsyncTaskListener<Void>, 
		FullScrnProgressListener {
	
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
	private boolean is7InchTabletInPortrait;

	private View rltRootNoContentFound;

	private RelativeLayout rltLytPrgsBar;

	private SortArtistNewsBy sortBy = SortArtistNewsBy.chronological;
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
        setHasOptionsMenu(true);
		artist = (Artist) getArguments().getSerializable(BundleKeys.ARTIST);
		isTablet = ((EventSeekr)FragmentUtil.getActivity(this).getApplicationContext()).isTablet();

		if (wcitiesId == null) {
			wcitiesId = ((EventSeekr)FragmentUtil.getActivity(this).getApplication()).getWcitiesId();
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		//Log.d(TAG, "onCreateView()");
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
		rltRootNoContentFound = v.findViewById(R.id.rltRootNoContentFound);
		rltLytPrgsBar = (RelativeLayout) v.findViewById(R.id.rltLytPrgsBar);
		rltLytPrgsBar.setBackgroundResource(R.drawable.bg_no_content_overlay);
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		initListView();
	}
	
	private void initListView() {
		if (getListView().getVisibility() != View.VISIBLE) {
			getListView().setVisibility(View.VISIBLE);
			rltRootNoContentFound.setVisibility(View.GONE);
			((ImageView) rltRootNoContentFound.findViewById(R.id.imgPhone)).setImageDrawable(null);
		}
		if (artistNewsListItems == null) {
			artistNewsListItems = new ArrayList<ArtistNewsListItem>();
			artistNewsListAdapter = new ArtistNewsListAdapter(FragmentUtil.getActivity(this), null, 
					this, artistNewsListItems, imgWidth, (sortBy == SortArtistNewsBy.trending));
	        
			artistNewsListItems.add(null);
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
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_artist_news, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_sort_by:
			Bundle args = new Bundle();
			args.putInt(RadioGroupDialogFragment.DEFAULT_VALUE, sortBy.getValue());
			args.putString(RadioGroupDialogFragment.DIALOG_TITLE, 
					FragmentUtil.getResources(this).getString(R.string.sort_by));
			args.putString(RadioGroupDialogFragment.DIALOG_RDB_ZEROTH_TEXT, 
					FragmentUtil.getResources(this).getString(R.string.chronological));
			args.putString(RadioGroupDialogFragment.DIALOG_RDB_FIRST_TEXT, 
					FragmentUtil.getResources(this).getString(R.string.trending));
			args.putSerializable(RadioGroupDialogFragment.ON_VALUE_SELECTED_LISETER, new OnValueSelectedListener() {

				@Override
				public void onValueSelected(int selectedValue) {
					SortArtistNewsBy sortBy = SortArtistNewsBy.getSortTypeBy(selectedValue);
					if (ArtistNewsListFragment.this.sortBy == sortBy) {
						return;
					}
					ArtistNewsListFragment.this.sortBy = sortBy;
					artistNewsListItems = null;
					
					initListView();
				}
			});
			
			RadioGroupDialogFragment dialogFragment = new RadioGroupDialogFragment();
			dialogFragment.setArguments(args);
			dialogFragment.show(((ActionBarActivity) FragmentUtil.getActivity(this))
					.getSupportFragmentManager(), "dialogSortBy");
			return true;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}	

	@Override
	public void onDestroyView() {
		//Log.i(TAG, "onDestroyView()");
		
		if (is7InchTabletInPortrait) {
			firstVisibleNewsItemPosition = getListView().getFirstVisiblePosition();
			
		} else if (orientation == Configuration.ORIENTATION_PORTRAIT && !isTablet) {
			firstVisibleNewsItemPosition = getListView().getFirstVisiblePosition();
			
		} else {
			firstVisibleNewsItemPosition = getListView().getFirstVisiblePosition() * 2;
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
		loadArtistNews = new LoadArtistNews(Api.OAUTH_TOKEN, artistNewsListAdapter, wcitiesId, artistNewsListItems, 
				artist, this, sortBy);
		artistNewsListAdapter.setLoadArtistNews(loadArtistNews);
		AsyncTaskUtil.executeAsyncTask(loadArtistNews, true);
	}

	@Override
	public void onNewsLoaded() {
		changeRltDummyLytVisibility();
	}

	private void changeRltDummyLytVisibility() {
		if (artistNewsListItems.size() == 1 && artistNewsListItems.get(0) != null
				&& ((ArtistNewsItem)((ArtistNewsListItem) artistNewsListItems.get(0))
					.getItem()).getArtist().getName().equals(AppConstants.INVALID_STR_ID)) {
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
			
		} else {
			rltRootNoContentFound.setVisibility(View.GONE);
			((ImageView) rltRootNoContentFound.findViewById(R.id.imgPhone)).setImageDrawable(null);
		}		
	}
	
	private void setNoItemsLayout() {
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
		
		TextView txtNoContentMsg = (TextView) rltRootNoContentFound.findViewById(R.id.txtNoItemsMsg);
		txtNoContentMsg.setText(R.string.artists_news_no_content);
		txtNoContentMsg.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_list_follow, 0, 0);
		
		((ImageView) rltRootNoContentFound.findViewById(R.id.imgPhone))
			.setImageDrawable(FragmentUtil.getResources(this).getDrawable(R.drawable.ic_artist_news_no_content));
		
		rltRootNoContentFound.setVisibility(View.VISIBLE);
		/**
		 * 04-03-2015:
		 * 'OnTouchListener' is added to 'rltRootNoContentFound' as if it is not there then the touch
		 * events would be given to the ArtistDetailsFragment(which is alive behind this fragment) and
		 * hence if user swipes on 'rltRootNoContentFound' when NO ARTIST NEWS are there then the toolbar
		 * color changes from colorPrimay to transparent and from transparent to colorPrimay and even
		 * the title will get update with the Artist Name
		 */
		rltRootNoContentFound.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return true;
			}
		});
	}

	@Override
	public String getScreenName() {
		return ScreenNames.ARTIST_MORE_NEWS_SCREEN;
	}

	@Override
	public void onTaskCompleted(Void... params) {
		// remove full screen progressbar
		rltLytPrgsBar.setVisibility(View.INVISIBLE);
	}

	@Override
	public void displayFullScrnProgress() {
		rltLytPrgsBar.setVisibility(View.VISIBLE);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		// Log.d(TAG, "onDestroy()");
		if (loadArtistNews != null
				&& loadArtistNews.getStatus() != Status.FINISHED) {
			loadArtistNews.cancel(true);
		}
	}
}
