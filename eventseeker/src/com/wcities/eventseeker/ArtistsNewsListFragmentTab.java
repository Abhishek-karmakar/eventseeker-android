package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.List;

import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.RecyclerListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wcities.eventseeker.RadioGroupDialogFragment.OnValueSelectedListener;
import com.wcities.eventseeker.adapter.ArtistNewsListAdapter;
import com.wcities.eventseeker.adapter.ArtistNewsListAdapterTab;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.LoadArtistNews;
import com.wcities.eventseeker.asynctask.LoadArtistNews.ArtistNewsListItem;
import com.wcities.eventseeker.asynctask.LoadArtistNews.OnNewsLoadedListener;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.Enums.SortArtistNewsBy;
import com.wcities.eventseeker.core.ArtistNewsItem;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.interfaces.FullScrnProgressListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class ArtistsNewsListFragmentTab extends ListFragment implements LoadItemsInBackgroundListener, 
		OnNewsLoadedListener, AsyncTaskListener<Void>, FullScrnProgressListener {
	
	protected static final String TAG = ArtistsNewsListFragmentTab.class.getName();

	private LoadArtistNews loadArtistsNews;
	private ArtistNewsListAdapterTab artistNewsListAdapterTab;
	
	private String wcitiesId;
	private int imgWidth;
	private List<ArtistNewsListItem> artistsNewsListItems;
	
	private int firstVisibleNewsItemPosition;

	private View rltRootNoContentFound;
	private RelativeLayout rltLytPrgsBar;
	
	private SortArtistNewsBy sortBy = SortArtistNewsBy.chronological;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        if (wcitiesId == null) {
        	wcitiesId = ((EventSeekr)FragmentUtil.getActivity(this).getApplication()).getWcitiesId();
        }
    }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		int screenW = getResources().getDisplayMetrics().widthPixels;
        //Log.d(TAG, "w = " + screenW);
		imgWidth = screenW - (getResources().getDimensionPixelSize(R.dimen.root_lnr_layout_pad_l_artists_news_list_item) * 2)
				- (getResources().getDimensionPixelSize(R.dimen.rlt_layout_news_item_container_pad_artist_news_item) * 2);
		
		super.onCreateView(inflater, container, savedInstanceState);
		View v = inflater.inflate(R.layout.fragment_artists_news_list_tab, null);
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
		}
		if (artistsNewsListItems == null) {
			artistsNewsListItems = new ArrayList<ArtistNewsListItem>();
			artistNewsListAdapterTab = new ArtistNewsListAdapterTab(this, null, this, artistsNewsListItems, 
					imgWidth, (sortBy == SortArtistNewsBy.trending));
	        
			artistsNewsListItems.add(null);
			loadItemsInBackground();
			
		} else {
			artistNewsListAdapterTab.setImgWidth(imgWidth);
			changeRltDummyLytVisibility();
		}

		setListAdapter(artistNewsListAdapterTab);
		
		getListView().setDivider(null);
		getListView().setScrollingCacheEnabled(false);
		getListView().post(new Runnable() {
			
			@Override
			public void run() {
				// TODO: remove following try-catch handling if not required
				try {
					setSelection(firstVisibleNewsItemPosition);
					
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
					if (ArtistsNewsListFragmentTab.this.sortBy == sortBy) {
						return;
					}
					ArtistsNewsListFragmentTab.this.sortBy = sortBy;
					artistsNewsListItems = null;
					
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
		firstVisibleNewsItemPosition = getListView().getFirstVisiblePosition();			
		
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
		loadArtistsNews = new LoadArtistNews(Api.OAUTH_TOKEN, artistNewsListAdapterTab, wcitiesId, artistsNewsListItems, 
				null, this, sortBy);
		artistNewsListAdapterTab.setLoadArtistNews(loadArtistsNews);
		AsyncTaskUtil.executeAsyncTask(loadArtistsNews, true);
	}
	
	@Override
	public void onNewsLoaded() {
		changeRltDummyLytVisibility();
	}

	private void changeRltDummyLytVisibility() {
		if (artistsNewsListItems.size() == 1 && artistsNewsListItems.get(0) != null
			&& ((ArtistNewsItem)((ArtistNewsListItem)artistsNewsListItems.get(0))
					.getItem()).getArtist().getName().equals(AppConstants.INVALID_STR_ID)) {
			setNoItemsLayout();

		} else {
			getListView().setVisibility(View.VISIBLE);
			rltRootNoContentFound.setVisibility(View.GONE);
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
		if (loadArtistsNews != null
				&& loadArtistsNews.getStatus() != Status.FINISHED) {
			loadArtistsNews.cancel(true);
		}
	}
}