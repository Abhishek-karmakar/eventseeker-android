package com.wcities.eventseeker;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.melnykov.fab.FloatingActionButton;
import com.wcities.eventseeker.adapter.FeaturingArtistPagerAdapterTab;
import com.wcities.eventseeker.adapter.FriendsRVAdapter;
import com.wcities.eventseeker.analytics.GoogleAnalyticsTracker;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingItemType;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.asynctask.LoadEventDetails;
import com.wcities.eventseeker.asynctask.LoadEventDetails.OnEventUpdatedListner;
import com.wcities.eventseeker.asynctask.UserTracker;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Date;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Event.Attending;
import com.wcities.eventseeker.core.Schedule;
import com.wcities.eventseeker.custom.fragment.PublishEventFragmentRetainingChildFragmentManager;
import com.wcities.eventseeker.custom.view.ObservableScrollView;
import com.wcities.eventseeker.custom.view.ObservableScrollView.ObservableScrollViewListener;
import com.wcities.eventseeker.interfaces.VenueListenerTab;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.FbUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.VersionUtil;

public class EventDetailsFragmentTab extends PublishEventFragmentRetainingChildFragmentManager implements 
		ObservableScrollViewListener, OnEventUpdatedListner, OnClickListener {

	private static final String TAG = EventDetailsFragmentTab.class.getSimpleName();
	private static final int UNSCROLLED = -1;
	private static final int MAX_LINES_EVENT_DESC = 3;
	
	private ObservableScrollView obsrScrlV;
	private ImageView imgEvt, imgDown, imgPrgOverlay;
	private TextView txtEvtTitle, txtEvtTime, txtEvtDesc, txtVenue;
	private RelativeLayout rltLytPrgsBar, rltLytFeaturing, rltLytVenue, rltLytFriends;
	private RecyclerView recyclerVFriends;
	private FloatingActionButton fabTickets, fabSave, fabWeb, fabFb;
    private View vFabSeparator;
	
	private FeaturingArtistPagerAdapterTab featuringArtistPagerAdapterTab;
	private FriendsRVAdapter friendsRVAdapter;
	
	private String title = "", subTitle = "";
	
	private int txtEvtTitleDiffX, txtEvtTitleSourceX, fabMarginT;
	private int limitScrollAt, actionBarElevation, prevScrollY = UNSCROLLED;
	private boolean isScrollLimitReached;
	
	private LoadEventDetails loadEventDetails;
	private boolean allDetailsLoaded, isEvtDescExpanded;
	
	private OnGlobalLayoutListener onGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
        	//Log.d(TAG, "onGlobalLayout()");
			if (VersionUtil.isApiLevelAbove15()) {
				obsrScrlV.getViewTreeObserver().removeOnGlobalLayoutListener(this);

			} else {
				obsrScrlV.getViewTreeObserver().removeGlobalOnLayoutListener(this);
			}
			
            if (prevScrollY == UNSCROLLED) {
            	onScrollChanged(obsrScrlV.getScrollY(), true);
            	
            } else {
            	//Log.d(TAG, "scrollTo");
            	obsrScrlV.scrollTo(0, prevScrollY);
            	/**
            	 * On orientation change forcefully (forceUpdate = true is passed) reset toolbar bg, title & subtitle
            	 * as per the scrolling position.
            	 */
            	onScrollChanged(obsrScrlV.getScrollY(), true);
            }
        }
    };
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setHasOptionsMenu(true);
		
		actionBarElevation = FragmentUtil.getResources(this).getDimensionPixelSize(R.dimen.action_bar_elevation);
		
		Bundle args = getArguments();
		if (event == null) {
			//Log.d(TAG, "event = null");
			event = (Event) args.getSerializable(BundleKeys.EVENT);
			event.getFriends().clear();
			
			loadEventDetails = new LoadEventDetails(Api.OAUTH_TOKEN, this, this, event);
			if (FragmentUtil.getActivity(this).getIntent().hasExtra(BundleKeys.IS_FROM_NOTIFICATION)) {
				loadEventDetails.setAddSrcFromNotification(true);
				FragmentUtil.getActivity(this).getIntent().removeExtra(BundleKeys.IS_FROM_NOTIFICATION);
			}
			AsyncTaskUtil.executeAsyncTask(loadEventDetails, true);
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		//Log.d(TAG, "onCreateView()");
		calculateScrollLimit();
		
		View rootView = inflater.inflate(R.layout.fragment_event_details_tab, container, false);
		
		txtEvtTitle = (TextView) rootView.findViewById(R.id.txtEvtTitle);
		txtEvtTitle.setText(event.getName());
		// for marquee to work
		txtEvtTitle.setSelected(true);
		if (getArguments().containsKey(BundleKeys.TRANSITION_NAME_SHARED_TEXT)) {
			ViewCompat.setTransitionName(txtEvtTitle, getArguments().getString(BundleKeys.TRANSITION_NAME_SHARED_TEXT));
		}
		
		txtEvtTime = (TextView) rootView.findViewById(R.id.txtEvtTime);
		txtVenue = (TextView) rootView.findViewById(R.id.txtVenue);
		ViewCompat.setTransitionName(txtVenue, "txtVenueEventDetails");
		updateEventSchedule();

        fabWeb = (FloatingActionButton) rootView.findViewById(R.id.fabWeb);
        fabFb = (FloatingActionButton) rootView.findViewById(R.id.fabFb);
        fabWeb.setOnClickListener(this);
        fabFb.setOnClickListener(this);
        updateFabLinks();

        vFabSeparator = rootView.findViewById(R.id.vFabSeparator);
		txtEvtDesc = (TextView) rootView.findViewById(R.id.txtDesc);
		imgDown = (ImageView) rootView.findViewById(R.id.imgDown);
        imgDown.setOnClickListener(this);
		updateDescVisibility();
		
		imgEvt = (ImageView) rootView.findViewById(R.id.imgEvt);
		updateEventImg();
		if (getArguments().containsKey(BundleKeys.TRANSITION_NAME_SHARED_IMAGE)) {
			ViewCompat.setTransitionName(imgEvt, getArguments().getString(BundleKeys.TRANSITION_NAME_SHARED_IMAGE));
		}
		
		obsrScrlV = (ObservableScrollView) rootView.findViewById(R.id.obsrScrlV);
		obsrScrlV.setListener(this);
		obsrScrlV.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
		
		rltLytPrgsBar = (RelativeLayout) rootView.findViewById(R.id.rltLytPrgsBar);
		imgPrgOverlay = (ImageView) rltLytPrgsBar.findViewById(R.id.imgPrgOverlay);
		updateProgressBarVisibility();

		rltLytFeaturing = (RelativeLayout) rootView.findViewById(R.id.rltLytFeaturing);
		rltLytVenue = (RelativeLayout) rootView.findViewById(R.id.rltLytVenue);
		rltLytFriends = (RelativeLayout) rootView.findViewById(R.id.rltLytFriends);
		
		EventDetailsActivityTab eventDetailsActivityTab = (EventDetailsActivityTab) FragmentUtil.getActivity(this);
		fabSave = (FloatingActionButton) eventDetailsActivityTab.getViewById(R.id.fab1);
		fabTickets = (FloatingActionButton) eventDetailsActivityTab.getViewById(R.id.fab2);
		fabSave.setOnClickListener(this);
		fabTickets.setOnClickListener(this);
        rootView.findViewById(R.id.fabNavigate).setOnClickListener(this);
		
		updateDetailsVisibility();
		
		recyclerVFriends = (RecyclerView) rootView.findViewById(R.id.recyclerVFriends);
		// use a linear layout manager
		RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(FragmentUtil.getActivity(this), 
				LinearLayoutManager.HORIZONTAL, false);
		recyclerVFriends.setLayoutManager(layoutManager);
		
		ViewPager vPagerFeaturing = (ViewPager) rootView.findViewById(R.id.vPagerFeaturing);
		if (featuringArtistPagerAdapterTab == null) {
			featuringArtistPagerAdapterTab = new FeaturingArtistPagerAdapterTab(childFragmentManager(), event.getArtists());
		} 
		vPagerFeaturing.setAdapter(featuringArtistPagerAdapterTab);
		vPagerFeaturing.setOnPageChangeListener(featuringArtistPagerAdapterTab);
		
		vPagerFeaturing.setCurrentItem(featuringArtistPagerAdapterTab.getCurrentPosition());
		
		// Necessary or the pager will only have one extra page to show make this at least however many pages you can see
		vPagerFeaturing.setOffscreenPageLimit(7);
		
		// Set margin for pages as a negative number, so a part of next and previous pages will be showed
		Resources res = FragmentUtil.getResources(this);
		vPagerFeaturing.setPageMargin(res.getDimensionPixelSize(R.dimen.rlt_lyt_root_w_featuring_artist) - 
				res.getDimensionPixelSize(R.dimen.floating_window_w) + res.getDimensionPixelSize(R.dimen.v_pager_featuring_margin_l_event_details_tab)
				+ res.getDimensionPixelSize(R.dimen.v_pager_featuring_margin_r_event_details_tab));
		
		return rootView;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if (friendsRVAdapter == null) {
			friendsRVAdapter = new FriendsRVAdapter(event.getFriends());
		} 
		
		recyclerVFriends.setAdapter(friendsRVAdapter);
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		
		/**
		 * Following call is required to prevent non-removal of onGlobalLayoutListener. If onGlobalLayout() 
		 * is not called yet & screen gets destroyed, then removal of onGlobalLayoutListener will not happen ever 
		 * since fragment won't be able to find its view tree observer. So, better to make sure
		 * that it gets removed at the end from onDestroyView()
		 */
		if (VersionUtil.isApiLevelAbove15()) {
			obsrScrlV.getViewTreeObserver().removeOnGlobalLayoutListener(onGlobalLayoutListener);

		} else {
			obsrScrlV.getViewTreeObserver().removeGlobalOnLayoutListener(onGlobalLayoutListener);
		}
	}
	
	@Override
	public void onDestroy() {
		//Log.d(TAG, "onDestroy()");
		super.onDestroy();
		
		if (loadEventDetails != null && loadEventDetails.getStatus() != Status.FINISHED) {
			loadEventDetails.cancel(true);
		}
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_event_details, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_share:
			ShareViaDialogFragment shareViaDialogFragment = ShareViaDialogFragment.newInstance(event, 
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
	
	public String getTitle() {
		return title;
	}

	public String getSubTitle() {
		return subTitle;
	}
	
	private void calculateScrollLimit() {
		Resources res = FragmentUtil.getResources(this);
		limitScrollAt = res.getDimensionPixelSize(R.dimen.img_evt_ht_event_details_tab) - res.getDimensionPixelSize(
				R.dimen.floating_double_line_toolbar_ht);
		
		int txtEvtTitleDestinationX = res.getDimensionPixelSize(R.dimen.txt_toolbar_double_line_title_pos_all_details);
		txtEvtTitleSourceX = res.getDimensionPixelSize(R.dimen.rlt_lyt_txt_evt_title_pad_l_event_details_tab);
		txtEvtTitleDiffX = txtEvtTitleDestinationX - txtEvtTitleSourceX;
		
		fabMarginT = res.getDimensionPixelSize(R.dimen.fab_margin_t_base_tab_floating);
		if (!VersionUtil.isApiLevelAbove20()) {
			fabMarginT -= res.getDimensionPixelSize(R.dimen.fab_shadow_size);
		}
		//Log.d(TAG, "fabMarginT = " + fabMarginT);
	}
	
	private void updateProgressBarVisibility() {
		if (allDetailsLoaded) {
			// free up memory
			imgPrgOverlay.setBackgroundResource(0);
			imgPrgOverlay.setVisibility(View.GONE);
			rltLytPrgsBar.setVisibility(View.GONE);
			
		} else {
			rltLytPrgsBar.setVisibility(View.VISIBLE);
			imgPrgOverlay.setBackgroundResource(R.drawable.ic_no_content_background_overlay_cropped);
			imgPrgOverlay.setVisibility(View.VISIBLE);
		}
	}
	
	private void updateDescVisibility() {
		if (event.getDescription() != null) {
            txtEvtDesc.setText(Html.fromHtml(event.getDescription()));
            txtEvtDesc.setVisibility(View.VISIBLE);

		} else {
            txtEvtDesc.setVisibility(View.GONE);
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) vFabSeparator.getLayoutParams();
            lp.topMargin = ConversionUtil.toPx(FragmentUtil.getResources(this), 28);
            vFabSeparator.setLayoutParams(lp);
        }

        if (isEvtDescExpanded) {
            expandEvtDesc();

        } else {
            collapseEvtDesc();
        }
	}
	
	private void expandEvtDesc() {
		txtEvtDesc.setMaxLines(Integer.MAX_VALUE);
		txtEvtDesc.setEllipsize(null);
		imgDown.setImageDrawable(FragmentUtil.getResources(this).getDrawable(R.drawable.ic_description_collapse));

        fabWeb.setVisibility(View.VISIBLE);
        fabFb.setVisibility(View.VISIBLE);
        vFabSeparator.setVisibility(View.VISIBLE);

		isEvtDescExpanded = true;
	}
	
	private void collapseEvtDesc() {
		txtEvtDesc.setMaxLines(MAX_LINES_EVENT_DESC);
		txtEvtDesc.setEllipsize(TruncateAt.END);
		imgDown.setImageDrawable(FragmentUtil.getResources(this).getDrawable(R.drawable.ic_description_expand));

        fabWeb.setVisibility(View.GONE);
        fabFb.setVisibility(View.GONE);
        vFabSeparator.setVisibility(View.GONE);

		isEvtDescExpanded = false;
	}
	
	private void updateEventImg() {
		//Log.d(TAG, "updateEventImg(), url = " + event.getLowResImgUrl());
		if (event.doesValidImgUrlExist()) {
			//Log.d(TAG, "updateEventImg(), ValidImgUrlExist");
			String key = event.getKey(ImgResolution.LOW);
	        BitmapCache bitmapCache = BitmapCache.getInstance();
			Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
			if (bitmap != null) {
				imgEvt.setImageBitmap(bitmap);
		        
		    } else {
		    	imgEvt.setImageBitmap(null);
		    	AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
		        asyncLoadImg.loadImg(imgEvt, ImgResolution.LOW, event);
		    }
		}
	}
	
	private void updateFeaturingVisibility() {
		if (event.hasArtists()) {
			rltLytFeaturing.setVisibility(View.VISIBLE);
			featuringArtistPagerAdapterTab.notifyDataSetChanged();
			
		} else {
			rltLytFeaturing.setVisibility(View.GONE);
		}
	}
	
	private void updateAddressMapVisibility() {
		if (event.getSchedule() == null || event.getSchedule().getVenue() == null) {
			/**
			 * set again to gone since it won't be set from updateDetailsVisibility() in case user has moved to
			 * say tickets screen & comes back to this. 
			 */
			rltLytVenue.setVisibility(View.GONE);
			
		} else {
			rltLytVenue.setVisibility(View.VISIBLE);
			AddressMapFragment fragment = (AddressMapFragment) childFragmentManager().findFragmentByTag(
					FragmentUtil.getTag(AddressMapFragment.class));
	        if (fragment == null) {
	        	addAddressMapFragment();
	        }
		}
	}
	
	private void updateFriendsVisibility() {
		if (!event.getFriends().isEmpty()) {
			//Log.d(TAG, "event.getFriends() = " + event.getFriends());
			rltLytFriends.setVisibility(View.VISIBLE);
			friendsRVAdapter.notifyDataSetChanged();
			
		} else {
			rltLytFriends.setVisibility(View.GONE);
		}
	}
	
	private void addAddressMapFragment() {
    	FragmentManager fragmentManager = childFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        
        AddressMapFragment fragment = new AddressMapFragment();
        fragment.setArguments(getArguments());
        fragmentTransaction.add(R.id.frmLayoutMapContainer, fragment, FragmentUtil.getTag(fragment));
        try {
        	fragmentTransaction.commit();
        	
        } catch (IllegalStateException e) {
        	/**
        	 * This catch is to prevent possible "java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState"
        	 * when it's called from callback method updateDetailsVisibility() & if user has already left this screen.
        	 */
			Log.e(TAG, "IllegalStateException: " + e.getMessage());
			e.printStackTrace();
		}
    }

    private void updateFabLinks() {
        final Resources res = FragmentUtil.getResources(this);
        if (event.getWebsite() == null) {
            fabWeb.setImageDrawable(res.getDrawable(R.drawable.ic_web_link_unavailable));
            fabWeb.setEnabled(false);

        } else {
            fabWeb.setImageDrawable(res.getDrawable(R.drawable.ic_web_link_available));
            fabWeb.setEnabled(true);
        }

        if (event.getFbLink() == null) {
            fabFb.setImageDrawable(res.getDrawable(R.drawable.ic_facebook_link_unavailable));
            fabFb.setEnabled(false);

        } else {
            fabFb.setImageDrawable(res.getDrawable(R.drawable.ic_facebook_link_available));
            fabFb.setEnabled(true);
        }
    }
	
	private void updateFabs() {
		fabTickets.setVisibility(View.VISIBLE);
		fabSave.setVisibility(View.VISIBLE);
		
		final Resources res = FragmentUtil.getResources(this);
		if (event.getSchedule() == null || event.getSchedule().getBookingInfos().isEmpty()) {
			fabTickets.setImageDrawable(res.getDrawable(R.drawable.ic_ticket_unavailable_floating));
			fabTickets.setEnabled(false);
			
		} else {
			fabTickets.setImageDrawable(res.getDrawable(R.drawable.ic_ticket_available_floating));
			fabTickets.setEnabled(true);
		}
		
		updateFabSaveSrc(res);
	}
	
	private void updateFabSaveSrc(Resources res) {
		//Log.d(TAG, "event.getAttending() = " + event.getAttending().getValue());
		int drawableId = (event.getAttending() == Attending.SAVED) ? R.drawable.ic_saved_event_floating 
				: R.drawable.ic_unsaved_event_floating;
		fabSave.setImageDrawable(res.getDrawable(drawableId));
	}
	
	private String getEvtTimeIfAvailable() {
		Schedule schedule = event.getSchedule();
		if (schedule != null) {
			if (schedule.getDates().size() > 0) {
				Date date = schedule.getDates().get(0);
				return ConversionUtil.getDateTime(FragmentUtil.getApplication(this),
                        date.getStartDate(), date.isStartTimeAvailable(), true, false, false);
			}
		}
		return "";
	}
	
	private void updateEventSchedule() {
		Schedule schedule = event.getSchedule();
		if (schedule != null) {
			if (schedule.getVenue() != null) {
				txtVenue.setText(event.getSchedule().getVenue().getName());
				txtVenue.setOnClickListener(this);
			}
		}
		txtEvtTime.setText(getEvtTimeIfAvailable());
	}
	
	private void updateDetailsVisibility() {
		updateProgressBarVisibility();
		
		if (allDetailsLoaded) {
            updateFabLinks();
			updateDescVisibility();
			updateEventImg();
			updateEventSchedule();
			updateFeaturingVisibility();
			updateAddressMapVisibility();
			updateFriendsVisibility();
			updateFabs();
			
		} else {
			rltLytFeaturing.setVisibility(View.GONE);
			rltLytVenue.setVisibility(View.GONE);
			rltLytFriends.setVisibility(View.GONE);
			fabTickets.setVisibility(View.INVISIBLE);
			fabSave.setVisibility(View.INVISIBLE);
		}
	}
	
	private void onScrollChanged(int scrollY, boolean forceUpdate) {
		//Log.d(TAG, "scrollY = " + scrollY);
		// Translate image
		imgEvt.setTranslationY(scrollY / 2);
        
		BaseActivityTab baseActivityTab = (BaseActivityTab) FragmentUtil.getActivity(this);
		
		if ((!isScrollLimitReached || forceUpdate) && scrollY >= limitScrollAt) {
			//Log.d(TAG, "if");
			baseActivityTab.animateToolbarElevation(0.0f, actionBarElevation);
			baseActivityTab.setToolbarBg(baseActivityTab.getResources().getColor(R.color.colorPrimary));
			
			title = event.getName();
			baseActivityTab.updateTitle(title);

			subTitle = getEvtTimeIfAvailable();
			baseActivityTab.updateSubTitle(subTitle);
			
			isScrollLimitReached = true;
			
		} else if ((isScrollLimitReached || forceUpdate) && scrollY < limitScrollAt) {
			//Log.d(TAG, "else if");
			baseActivityTab.animateToolbarElevation(actionBarElevation, 0.0f);
			baseActivityTab.setToolbarBg(Color.TRANSPARENT);
			
			title = "";
			baseActivityTab.updateTitle(title);
			
			subTitle = "";
			baseActivityTab.updateSubTitle(subTitle);
			
			isScrollLimitReached = false;
		}
		
		if (scrollY <= limitScrollAt) {
			float translationX = scrollY * txtEvtTitleDiffX / (float) limitScrollAt;
			txtEvtTitle.setTranslationX(translationX);
			txtEvtTime.setTranslationX(translationX);
		}
		
		int topMargin = (scrollY <= limitScrollAt) ? scrollY : limitScrollAt;
		FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) fabSave.getLayoutParams();
		//Log.d(TAG, "lp.topMargin = " + lp.topMargin);
		lp.topMargin = fabMarginT - topMargin;
		fabSave.setLayoutParams(lp);
		
		lp = (FrameLayout.LayoutParams) fabTickets.getLayoutParams();
		lp.topMargin = fabMarginT - topMargin;
		fabTickets.setLayoutParams(lp);
		//Log.d(TAG, "lp.topMargin = " + lp.topMargin);

		prevScrollY = scrollY;
	}

	@Override
	public void onScrollChanged(int scrollY) {
		//Log.d(TAG, "onScrollChanged(), scrollY = " + scrollY);
		onScrollChanged(scrollY, false);
	}

	@Override
	public void onDownMotionEvent() {
		// TODO Auto-generated method stub
	}

	@Override
	public void onUpOrCancelMotionEvent() {
		// TODO Auto-generated method stub
	}

	@Override
	public void onEventUpdated() {
		allDetailsLoaded = true;
		updateDetailsVisibility();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.txtVenue:
			((VenueListenerTab) FragmentUtil.getActivity(this)).onVenueSelected(event.getSchedule().getVenue(), 
					null, txtVenue);
			break;
			
		case R.id.imgDown:
			if (isEvtDescExpanded) {
				collapseEvtDesc();
				
			} else {
				expandEvtDesc();
			}
			break;
			
		case R.id.fab2:
			EventSeekr eventSeekr = FragmentUtil.getApplication(this);
			BaseActivityTab baseActivityTab = (BaseActivityTab) FragmentUtil.getActivity(this);
					
			Intent intent = new Intent(eventSeekr, WebViewActivityTab.class);
			intent.putExtra(BundleKeys.URL, event.getSchedule().getBookingInfos().get(0).getBookingUrl()
                + "&lang=" + ((EventSeekr) FragmentUtil.getApplication(this)).getLocale().getLocaleCode());
			baseActivityTab.startActivity(intent);
			
			GoogleAnalyticsTracker.getInstance().sendEvent(eventSeekr, 
					baseActivityTab.getScreenName(), GoogleAnalyticsTracker.EVENT_LABEL_TICKETS_BUTTON, 
					GoogleAnalyticsTracker.Type.Event.name(), null, event.getId());
			break;
			
		case R.id.fab1:
			eventSeekr = (EventSeekr) FragmentUtil.getApplication(this);
			if (event.getAttending() == Attending.SAVED) {
				event.setAttending(Attending.NOT_GOING);
				new UserTracker(Api.OAUTH_TOKEN, eventSeekr, UserTrackingItemType.event, event.getId(), 
						event.getAttending().getValue(), UserTrackingType.Add).execute();
    			updateFabSaveSrc(FragmentUtil.getResources(this));
				
			} else {
				if (eventSeekr.getGPlusUserId() != null) {
					event.setNewAttending(Attending.SAVED);
					handlePublishEvent();
					
				} else {
					event.setNewAttending(Attending.SAVED);
					FbUtil.handlePublishEvent(this, this, AppConstants.PERMISSIONS_FB_PUBLISH_EVT_OR_ART,
							event);
				}
			}
			break;

        case R.id.fabNavigate:
            intent = new Intent(FragmentUtil.getApplication(this), NavigationActivityTab.class);
            intent.putExtra(BundleKeys.VENUE, event.getSchedule().getVenue());
            startActivity(intent);
            break;

        case R.id.fabWeb:
            eventSeekr = FragmentUtil.getApplication(this);
            baseActivityTab = (BaseActivityTab) FragmentUtil.getActivity(this);

            intent = new Intent(eventSeekr, WebViewActivityTab.class);
            intent.putExtra(BundleKeys.URL, event.getWebsite());
            baseActivityTab.startActivity(intent);

            break;

        case R.id.fabFb:
            eventSeekr = FragmentUtil.getApplication(this);
            baseActivityTab = (BaseActivityTab) FragmentUtil.getActivity(this);

            intent = new Intent(eventSeekr, WebViewActivityTab.class);
            intent.putExtra(BundleKeys.URL, event.getFbLink());
            baseActivityTab.startActivity(intent);

            break;
			
		default:
			break;
		}
	}

	@Override
	public void onPublishPermissionGranted() {
		updateFabSaveSrc(FragmentUtil.getResources(this));
	}

	@Override
	public void onSuccess(LoginResult loginResult) {
		Log.d(TAG, "onSuccess()");
		FbUtil.handlePublishEvent(this, this, AppConstants.PERMISSIONS_FB_PUBLISH_EVT_OR_ART,
				event);
	}

	@Override
	public void onCancel() {
		Log.d(TAG, "onCancel()");
	}

	@Override
	public void onError(FacebookException e) {
		Log.d(TAG, "onError()");
	}
}
