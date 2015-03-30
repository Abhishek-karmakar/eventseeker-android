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
import android.widget.TextView;

import com.facebook.Session;
import com.facebook.SessionState;
import com.wcities.eventseeker.adapter.RVVenueDetailsAdapterTab;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.asynctask.LoadEvents;
import com.wcities.eventseeker.asynctask.LoadVenueDetails;
import com.wcities.eventseeker.asynctask.LoadVenueDetails.OnVenueUpdatedListener;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.custom.fragment.PublishEventFragmentRetainingChildFragmentManager;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.VersionUtil;

public class VenueDetailsFragmentTab extends PublishEventFragmentRetainingChildFragmentManager implements 
		OnVenueUpdatedListener, LoadItemsInBackgroundListener, AsyncTaskListener<Void> {

	private static final int UNSCROLLED = -1;
	
	private String title = "";
	
	private int totalScrolledDy = UNSCROLLED; // indicates layout not yet created
	private int actionBarElevation, limitScrollAt;
	private int txtVenueTitleSourceX, txtVenueTitleDiffX;
	private boolean isScrollLimitReached;
	
	private Venue venue;
	private LoadVenueDetails loadVenueDetails;
	private boolean allDetailsLoaded;

	private LoadEvents loadEvents;
	private List<Event> eventList;
	
	private ImageView imgVenue;
	private RelativeLayout rltLytTxtVenueTitle;
	private TextView txtVenueTitle;
	private View vNoContentBG;
	private RecyclerView recyclerVVenue;
	
	private RVVenueDetailsAdapterTab rvVenueDetailsAdapterTab;
	
	private Handler handler;
	
	private OnGlobalLayoutListener onGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
        	//Log.d(TAG, "onGlobalLayout()");
        	
			if (VersionUtil.isApiLevelAbove15()) {
				recyclerVVenue.getViewTreeObserver().removeOnGlobalLayoutListener(this);

			} else {
				recyclerVVenue.getViewTreeObserver().removeGlobalOnLayoutListener(this);
			}
			
			onScrolled(0, true, true);
        }
    };
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setHasOptionsMenu(true);
		
		actionBarElevation = FragmentUtil.getResources(this).getDimensionPixelSize(R.dimen.action_bar_elevation);
		
		if (venue == null) {
			//Log.d(TAG, "event = null");
			venue = (Venue) getArguments().getSerializable(BundleKeys.VENUE);
			
			loadVenueDetails = new LoadVenueDetails(Api.OAUTH_TOKEN, venue, this);
			AsyncTaskUtil.executeAsyncTask(loadVenueDetails, true);
		}
		
		handler = new Handler(Looper.getMainLooper());
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_venue_details_tab, container, false);
		
		imgVenue = (ImageView) rootView.findViewById(R.id.imgVenue);
		updateVenueImg();
		if (getArguments().containsKey(BundleKeys.TRANSITION_NAME_SHARED_IMAGE)) {
			ViewCompat.setTransitionName(imgVenue, getArguments().getString(BundleKeys.TRANSITION_NAME_SHARED_IMAGE));
		}
		
		rltLytTxtVenueTitle = (RelativeLayout) rootView.findViewById(R.id.rltLytTxtVenueTitle);
		
		txtVenueTitle = (TextView) rootView.findViewById(R.id.txtVenueTitle);
		txtVenueTitle.setText(venue.getName());
		// for marquee to work
		txtVenueTitle.setSelected(true);
		ViewCompat.setTransitionName(txtVenueTitle, getArguments().getString(BundleKeys.TRANSITION_NAME_SHARED_TEXT));

		vNoContentBG = rootView.findViewById(R.id.vNoContentBG);
		
		recyclerVVenue = (RecyclerView) rootView.findViewById(R.id.recyclerVVenue);
		// use a linear layout manager
		RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(FragmentUtil.getActivity(this));
		recyclerVVenue.setLayoutManager(layoutManager);
		
		recyclerVVenue.setOnScrollListener(new RecyclerView.OnScrollListener() {
	    	
	    	@Override
	    	public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
	    		super.onScrolled(recyclerView, dx, dy);
	    		//Log.d(TAG, "onScrolled - dx = " + dx + ", dy = " + dy);
	    		VenueDetailsFragmentTab.this.onScrolled(dy, false, false);
	    	}
		});
		
		recyclerVVenue.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
		
		return rootView;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (rvVenueDetailsAdapterTab == null) {
			eventList = new ArrayList<Event>();
			eventList.add(null);
			
			rvVenueDetailsAdapterTab = new RVVenueDetailsAdapterTab(this);
		}
		recyclerVVenue.setAdapter(rvVenueDetailsAdapterTab);
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		
		if (VersionUtil.isApiLevelAbove15()) {
			recyclerVVenue.getViewTreeObserver().removeOnGlobalLayoutListener(onGlobalLayoutListener);

		} else {
			recyclerVVenue.getViewTreeObserver().removeGlobalOnLayoutListener(onGlobalLayoutListener);
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (loadVenueDetails != null && loadVenueDetails.getStatus() != Status.FINISHED) {
			loadVenueDetails.cancel(true);
		}
		if (loadEvents != null && loadEvents.getStatus() != Status.FINISHED) {
			loadEvents.cancel(true);
		}
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_venue_details, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_share:
			ShareViaDialogFragment shareViaDialogFragment = ShareViaDialogFragment.newInstance(venue, 
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
	
	public boolean isAllDetailsLoaded() {
		return allDetailsLoaded;
	}

	public String getTitle() {
		return title;
	}
	
	public Venue getVenue() {
		return venue;
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

	public void setVNoContentBgVisibility(int visibility) {
		vNoContentBG.setVisibility(visibility);
	}
	
	private void updateVenueImg() {
		//Log.d(TAG, "updateEventImg(), url = " + event.getLowResImgUrl());
		if (venue.doesValidImgUrlExist()) {
			String key = venue.getKey(ImgResolution.LOW);
	        BitmapCache bitmapCache = BitmapCache.getInstance();
			Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
			if (bitmap != null) {
		        imgVenue.setImageBitmap(bitmap);
		        
		    } else {
		    	imgVenue.setImageBitmap(null);
		    	AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
		        asyncLoadImg.loadImg(imgVenue, ImgResolution.LOW, venue, null);
		    }
		}
	}
	
	public void onScrolled(int dy, boolean forceUpdate, boolean chkForOpenDrawer) {
		//Log.d(TAG, "dy = " + dy);
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
		if (((LinearLayoutManager)recyclerVVenue.getLayoutManager()).findFirstVisibleItemPosition() == 0) {
			totalScrolledDy = -recyclerVVenue.getLayoutManager().findViewByPosition(0).getTop();
			//Log.d(TAG, "totalScrolledDy corrected = " + totalScrolledDy);
		}
		
		// Translate image
		imgVenue.setTranslationY((0 - totalScrolledDy) / 2);
		
		calculateScrollLimit();
		
		int scrollY = (totalScrolledDy >= limitScrollAt) ? limitScrollAt : totalScrolledDy;
		//Log.d(TAG, "totalScrolledDy = " + totalScrolledDy + ", limitScrollAt = " + limitScrollAt + ", scrollY = " + scrollY);
		
		rltLytTxtVenueTitle.setTranslationY(-totalScrolledDy);
		
		if ((!isScrollLimitReached || forceUpdate) && totalScrolledDy >= limitScrollAt) {
			baseActivityTab.animateToolbarElevation(0.0f, actionBarElevation);
			baseActivityTab.setToolbarBg(baseActivityTab.getResources().getColor(R.color.colorPrimary));
			
			title = venue.getName();
			baseActivityTab.updateTitle(title);
			
			isScrollLimitReached = true;
			
		} else if ((isScrollLimitReached || forceUpdate) && totalScrolledDy < limitScrollAt) {
			//Log.d(TAG, "totalScrolledDy < limitScrollAt");
			baseActivityTab.animateToolbarElevation(actionBarElevation, 0.0f);
			baseActivityTab.setToolbarBg(Color.TRANSPARENT);
			
			title = "";
			baseActivityTab.updateTitle(title);
			
			isScrollLimitReached = false;
		}
		
		if (scrollY < limitScrollAt) {
			txtVenueTitle.setTranslationX(scrollY * txtVenueTitleDiffX / (float) limitScrollAt);
		}
	}
	
	private void calculateScrollLimit() {
		Resources res = FragmentUtil.getResources(this);
		limitScrollAt = res.getDimensionPixelSize(R.dimen.img_venue_ht_venue_details_tab) - res.getDimensionPixelSize(
				R.dimen.action_bar_ht);
		
		int txtVenueTitleDestinationX = res.getDimensionPixelSize(R.dimen.txt_toolbar_title_pos_all_details);
		txtVenueTitleSourceX = res.getDimensionPixelSize(R.dimen.rlt_lyt_txt_venue_title_pad_l_venue_details_tab);
		
		txtVenueTitleDiffX = txtVenueTitleDestinationX - txtVenueTitleSourceX;
	}
	
	@Override
	public void onPublishPermissionGranted() {
		// TODO Auto-generated method stub

	}

	@Override
	public void call(Session session, SessionState state, Exception exception) {
		// TODO Auto-generated method stub

	}
	
	@Override
	public void loadItemsInBackground() {
		loadEvents = new LoadEvents(Api.OAUTH_TOKEN, eventList, rvVenueDetailsAdapterTab, ((EventSeekr)FragmentUtil
				.getApplication(this)).getWcitiesId(), venue.getId(), this);
		rvVenueDetailsAdapterTab.setLoadDateWiseEvents(loadEvents);
		AsyncTaskUtil.executeAsyncTask(loadEvents, true);
	}

	@Override
	public void onVenueUpdated() {
		allDetailsLoaded = true;
		updateVenueImg();
		rvVenueDetailsAdapterTab.notifyDataSetChanged();
	}

	@Override
	public void onTaskCompleted(Void... params) {
		handler.post(new Runnable() {
			
			@Override
			public void run() {
				onScrolled(0, true, true);
			}
		});
	}
}
