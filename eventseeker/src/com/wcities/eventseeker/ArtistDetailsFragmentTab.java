package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.List;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.facebook.Session;
import com.facebook.SessionState;
import com.wcities.eventseeker.adapter.RVArtistDetailsAdapterTab;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.asynctask.LoadArtistDetails;
import com.wcities.eventseeker.asynctask.LoadArtistDetails.OnArtistUpdatedListener;
import com.wcities.eventseeker.asynctask.LoadArtistEvents;
import com.wcities.eventseeker.asynctask.LoadArtistEvents.LoadArtistEventsListener;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.custom.fragment.PublishEventFragmentRetainingChildFragmentManager;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.VersionUtil;

public class ArtistDetailsFragmentTab extends PublishEventFragmentRetainingChildFragmentManager implements 
		OnArtistUpdatedListener, AsyncTaskListener<Void>, LoadItemsInBackgroundListener, LoadArtistEventsListener {
	
	private static final String TAG = ArtistDetailsFragmentTab.class.getSimpleName();
	
	private static final int UNSCROLLED = -1;
	
	private String title = "", subTitle = "";
	
	private int totalScrolledDy = UNSCROLLED; // indicates layout not yet created
	private int actionBarElevation, limitScrollAt;
	private int txtArtistTitleDiffX, txtArtistTitleDiffCenterY;
	private boolean isScrollLimitReached;
	
	private Artist artist;
	private List<Event> eventList;
	private boolean allDetailsLoaded, isVDummyHtIncreased;
	
	private ImageView imgArtist;
	private TextView txtArtistTitle;
	private RelativeLayout rltLytTxtArtistTitle;
	private View vNoContentBG;
	private RecyclerView recyclerVArtists;
	private View vDummy;
	
	private RVArtistDetailsAdapterTab rvArtistDetailsAdapterTab;
	
	private Handler handler;
	
	private LoadArtistDetails loadArtistDetails;
	private LoadArtistEvents loadArtistEvents;
	
	private OnGlobalLayoutListener onGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
        	//Log.d(TAG, "onGlobalLayout()");
			if (VersionUtil.isApiLevelAbove15()) {
				recyclerVArtists.getViewTreeObserver().removeOnGlobalLayoutListener(this);

			} else {
				recyclerVArtists.getViewTreeObserver().removeGlobalOnLayoutListener(this);
			}
			
			onScrolled(0, true);
        }
    };
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		
		actionBarElevation = FragmentUtil.getResources(this).getDimensionPixelSize(R.dimen.action_bar_elevation);
		
		if (artist == null) {
			//Log.d(TAG, "event = null");
			artist = (Artist) getArguments().getSerializable(BundleKeys.ARTIST);
			artist.getVideos().clear();
			artist.getFriends().clear();
			
			loadArtistDetails = new LoadArtistDetails(Api.OAUTH_TOKEN, artist, this, this);
			AsyncTaskUtil.executeAsyncTask(loadArtistDetails, true);
		}
		
		handler = new Handler(Looper.getMainLooper());
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		/**
		 * on orientation change we need to recalculate this due to different values of 
		 * img ht on both orientations
		 */
		calculateScrollLimit();
		
		View rootView = inflater.inflate(R.layout.fragment_artist_details_tab, container, false);
		
		imgArtist = (ImageView) rootView.findViewById(R.id.imgArtist);
		updateArtistImg();
		if (getArguments().containsKey(BundleKeys.TRANSITION_NAME_SHARED_IMAGE)) {
			ViewCompat.setTransitionName(imgArtist, getArguments().getString(BundleKeys.TRANSITION_NAME_SHARED_IMAGE));
		}
		
		rltLytTxtArtistTitle = (RelativeLayout) rootView.findViewById(R.id.rltLytTxtArtistTitle);
		
		txtArtistTitle = (TextView) rootView.findViewById(R.id.txtArtistTitle);
		txtArtistTitle.setText(artist.getName());
		// for marquee to work
		txtArtistTitle.setSelected(true);
		ViewCompat.setTransitionName(txtArtistTitle, getArguments().getString(BundleKeys.TRANSITION_NAME_SHARED_TEXT));
		
		vNoContentBG = rootView.findViewById(R.id.vNoContentBG);
		vDummy = rootView.findViewById(R.id.vDummy);
		
		recyclerVArtists = (RecyclerView) rootView.findViewById(R.id.recyclerVArtists);
		// use a linear layout manager
		RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(FragmentUtil.getActivity(this));
		recyclerVArtists.setLayoutManager(layoutManager);
		
		recyclerVArtists.setOnScrollListener(new RecyclerView.OnScrollListener() {
	    	
	    	@Override
	    	public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
	    		super.onScrolled(recyclerView, dx, dy);
	    		//Log.d(TAG, "onScrolled - dx = " + dx + ", dy = " + dy);
	    		ArtistDetailsFragmentTab.this.onScrolled(dy, false);
	    	}
		});
		
		recyclerVArtists.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
		
		return rootView;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (rvArtistDetailsAdapterTab == null) {
			eventList = new ArrayList<Event>();
			/**
			 * 16-02-2015
			 * This check is added as per discussion with Amir Sir, the upcoming events should be queried
			 * only if Artist is on Tour.
			 */
			if (artist.isOntour()) {
				eventList.add(null);
			}
			rvArtistDetailsAdapterTab = new RVArtistDetailsAdapterTab(this);
			
		} else {
			/**
			 * required to prevent "IllegalArgumentException: No view found for id 0x7f110209 
			 * (com.example:id/vPagerVideos) for fragment VideoFragment{da512cf #0 id=0x7f110209}", 
			 * on orientation change.
			 * Ref: http://stackoverflow.com/questions/28366612/on-back-press-coming-back-to-recyclerview-having-fragments-causes-illegalargume
			 */
			rvArtistDetailsAdapterTab.detachFragments();
		}
		
		recyclerVArtists.setAdapter(rvArtistDetailsAdapterTab);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (loadArtistDetails != null && loadArtistDetails.getStatus() != Status.FINISHED) {
			loadArtistDetails.cancel(true);
		}
		if (loadArtistEvents != null && loadArtistEvents.getStatus() != Status.FINISHED) {
			loadArtistEvents.cancel(true);
		}
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_artist_details, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_share:
			ShareViaDialogFragment shareViaDialogFragment = ShareViaDialogFragment.newInstance(artist, 
					((BaseActivityTab)FragmentUtil.getActivity(this)).getScreenName());
			/**
			 * Passing activity fragment manager, since using this fragment's child fragment manager 
			 * doesn't retain dialog on orientation change
			 */
			shareViaDialogFragment.show(((ActionBarActivity) FragmentUtil.getActivity(this))
				.getSupportFragmentManager(), FragmentUtil.getTag(shareViaDialogFragment));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void calculateScrollLimit() {
		Resources res = FragmentUtil.getResources(this);
		limitScrollAt = res.getDimensionPixelSize(R.dimen.img_artist_ht_artist_details_tab) - res.getDimensionPixelSize(
				R.dimen.floating_double_line_toolbar_ht);
		
		int txtArtistTitleDestinationX = res.getDimensionPixelSize(R.dimen.txt_toolbar_double_line_title_pos_all_details);
		int txtArtistTitleSourceX = res.getDimensionPixelSize(R.dimen.rlt_lyt_txt_artist_title_pad_l_artist_details_tab);
		txtArtistTitleDiffX = txtArtistTitleDestinationX - txtArtistTitleSourceX;
		
		int txtArtistTitleDestinationCenterY = res.getDimensionPixelSize(R.dimen.txt_toolbar_title_ht_toolbar_floating_window_double_line) / 2;
		int txtArtistTitleSourceCenterY = res.getDimensionPixelSize(R.dimen.rlt_lyt_txt_artist_title_ht_artist_details_tab) 
				- res.getDimensionPixelSize(R.dimen.rlt_lyt_txt_artist_title_pad_b_artist_details_tab) 
				- (res.getDimensionPixelSize(R.dimen.txt_artist_title_ht_artist_details_tab) / 2);
		txtArtistTitleDiffCenterY = txtArtistTitleDestinationCenterY - txtArtistTitleSourceCenterY;
	}
	
	public void onScrolled(int dy, boolean forceUpdate) {
		BaseActivityTab baseActivityTab = (BaseActivityTab) FragmentUtil.getActivity(this);
		
		if (totalScrolledDy == UNSCROLLED) {
			totalScrolledDy = 0;
		}
		totalScrolledDy += dy;
		
		/**
		 * this is required to prevent changes in scrolled value due to automatic corrections in recyclerview size
		 * e.g.: 1) Due to event loading progressbar returning no events resulting in reduction of overall size
		 * & hence totalScrolledDy value must be changed but we don't have good way to calculate it & hence
		 * just update it to right value when position is 0 (when we are sure about exact totalScrolledDy value)
		 * It's actually needed for changing toolbar color which we do only when 1st visible position is 0.
		 * 2) When screen becomes scrollable after expanding description but not on collapsing, resulting in 
		 * automatic scroll to settle recyclerview.
		 */
		if (((LinearLayoutManager)recyclerVArtists.getLayoutManager()).findFirstVisibleItemPosition() == 0) {
			totalScrolledDy = -recyclerVArtists.getLayoutManager().findViewByPosition(0).getTop();
			//Log.d(TAG, "totalScrolledDy corrected = " + totalScrolledDy);
		}
		
		// Translate image
		imgArtist.setTranslationY((0 - totalScrolledDy) / 2);
		
		if (limitScrollAt == 0) {
			calculateScrollLimit();
		}
		
		int scrollY = (totalScrolledDy >= limitScrollAt) ? limitScrollAt : totalScrolledDy;
		rltLytTxtArtistTitle.setTranslationY(-totalScrolledDy);
		
		/**
		 * Following if-else is to prevent artist image's part being displayed after artist title ends in following case.
		 * e.g. - In landscape mode when net is low, & all details are not loaded yet, we display progressbar.
		 * But if we scroll when progress bar is being displayed then part of image after the title becomes 
		 * visible due to half scrolling amount used for image & since background for progressbar is transparent
		 * (Changed to transparent at runtime from rvArtistDetailsAdapterTab when progress is ON).
		 * Why progressbar background is transparent? To keep "no content bg" visible behind recyclerview.
		 * Hence we just have a dummy view with white background to fill up the scroll difference amount
		 * between image & artist title when progress bar is there & set its height to 0 when all details are loaded.
		 */
		if (!allDetailsLoaded) {
			RelativeLayout.LayoutParams lp = (LayoutParams) vDummy.getLayoutParams();
			/**
			 * +1 is done to prevent thin line of image visible at the end after vDummy (probably due to odd value
			 * for totalScrolledDy
			 */
			lp.height = totalScrolledDy / 2 + 1;
			vDummy.setLayoutParams(lp);
			vDummy.setTranslationY(-totalScrolledDy);
			isVDummyHtIncreased = true;
			//Log.d(TAG, "lp.height = " + lp.height + ", totalScrolledDy = " + totalScrolledDy);
			
		} else if (isVDummyHtIncreased) {
			RelativeLayout.LayoutParams lp = (LayoutParams) vDummy.getLayoutParams();
			lp.height = 0;
			vDummy.setLayoutParams(lp);
			vDummy.setTranslationY(0);
			isVDummyHtIncreased = false;
		}
		
		if ((!isScrollLimitReached || forceUpdate) && totalScrolledDy >= limitScrollAt) {
			baseActivityTab.animateToolbarElevation(0.0f, actionBarElevation);
			baseActivityTab.setToolbarBg(baseActivityTab.getResources().getColor(R.color.colorPrimary));
			
			title = artist.getName();
			baseActivityTab.updateTitle(title);
			
			subTitle = artist.isOntour() ? baseActivityTab.getResources().getString(R.string.artist_on_tour).replaceFirst(" ", "") : "";
			baseActivityTab.updateSubTitle(subTitle);
			
			isScrollLimitReached = true;
			
		} else if ((isScrollLimitReached || forceUpdate) && totalScrolledDy < limitScrollAt) {
			baseActivityTab.animateToolbarElevation(actionBarElevation, 0.0f);
			baseActivityTab.setToolbarBg(Color.TRANSPARENT);
			
			title = "";
			baseActivityTab.updateTitle(title);
			
			subTitle = "";
			baseActivityTab.updateSubTitle(subTitle);
			
			isScrollLimitReached = false;
		}
		
		if (scrollY < limitScrollAt) {
			txtArtistTitle.setTranslationX(scrollY * txtArtistTitleDiffX / (float) limitScrollAt);
			txtArtistTitle.setTranslationY(scrollY * txtArtistTitleDiffCenterY / (float) limitScrollAt);
		}
	}

	public String getTitle() {
		return title;
	}
	
	public String getSubTitle() {
		return subTitle;
	}

	public boolean isAllDetailsLoaded() {
		return allDetailsLoaded;
	}
	
	public void setVNoContentBgVisibility(int visibility) {
		vNoContentBG.setVisibility(visibility);
	}

	public Artist getArtist() {
		return artist;
	}
	
	public Handler getHandler() {
		return handler;
	}

	public List<Event> getEventList() {
		return eventList;
	}
	
	public void setEvent(Event event) {
		this.event = event;
	}
	
	public void handlePublishEvent() {
		super.handlePublishEvent();
	}

	private void updateArtistImg() {
		//Log.d(TAG, "updateEventImg(), url = " + event.getLowResImgUrl());
		if (artist.doesValidImgUrlExist()) {
			String key = artist.getKey(ImgResolution.LOW);
	        BitmapCache bitmapCache = BitmapCache.getInstance();
			Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
			if (bitmap != null) {
		        imgArtist.setImageBitmap(bitmap);
		        
		    } else {
		    	imgArtist.setImageBitmap(null);
		    	AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
		        asyncLoadImg.loadImg(imgArtist, ImgResolution.LOW, artist);
		    }
		}
	}

	@Override
	public void onArtistUpdated() {
		allDetailsLoaded = true;
		updateArtistImg();
		/*fabSave.setSelected(artist.getAttending() == Artist.Attending.Tracked);
		fabSave.setVisibility(View.VISIBLE);
		
		fabArtistNews.setVisibility(View.VISIBLE);*/
		
		if (artist.isOntour() && eventList != null && eventList.isEmpty()) {
			eventList.add(null);
		}
		rvArtistDetailsAdapterTab.notifyDataSetChanged();
	}

	@Override
	public void onPublishPermissionGranted() {
		rvArtistDetailsAdapterTab.onPublishPermissionGranted();
	}

	@Override
	public void call(Session session, SessionState state, Exception exception) {
		rvArtistDetailsAdapterTab.call(session, state, exception);
	}

	@Override
	public void onTaskCompleted(Void... params) {
		handler.post(new Runnable() {
			
			@Override
			public void run() {
				onScrolled(0, true);
			}
		});
	}

	@Override
	public void loadItemsInBackground() {
		loadArtistEvents = new LoadArtistEvents(Api.OAUTH_TOKEN, eventList, rvArtistDetailsAdapterTab, artist.getId(),
				((EventSeekr)FragmentUtil.getActivity(this).getApplicationContext()).getWcitiesId(), this);

		rvArtistDetailsAdapterTab.setLoadDateWiseEvents(loadArtistEvents);
		AsyncTaskUtil.executeAsyncTask(loadArtistEvents, true);
	}

	@Override
	public void onArtistEventsLoaded() {
		handler.post(new Runnable() {
			
			@Override
			public void run() {
				onScrolled(0, true);
			}
		});
	}
}