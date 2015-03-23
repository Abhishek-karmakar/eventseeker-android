package com.wcities.eventseeker;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.text.Html;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.asynctask.LoadEventDetails;
import com.wcities.eventseeker.asynctask.LoadEventDetails.OnEventUpdatedListner;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Date;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Schedule;
import com.wcities.eventseeker.custom.view.ObservableScrollView;
import com.wcities.eventseeker.custom.view.ObservableScrollView.ObservableScrollViewListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.VersionUtil;

public class EventDetailsFragmentTab extends Fragment implements ObservableScrollViewListener, OnEventUpdatedListner, 
		OnClickListener {

	private static final String TAG = EventDetailsFragmentTab.class.getSimpleName();
	private static final int UNSCROLLED = -1;
	private static final int MAX_LINES_EVENT_DESC = 3;
	
	private ObservableScrollView obsrScrlV;
	private ImageView imgEvt, imgDown;
	private TextView txtEvtTitle, txtEvtTime, txtEvtDesc;
	private RelativeLayout rltLytPrgsBar;
	
	private Event event;
	private String title = "", subTitle = "";
	
	private int txtEvtTitleDiffX, txtEvtTitleSourceX;
	private int limitScrollAt, actionBarElevation, prevScrollY = UNSCROLLED;
	private boolean isScrollLimitReached;
	
	private LoadEventDetails loadEventDetails;
	private boolean allDetailsLoaded, isEvtDescExpanded;;
	
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
            	 * Following call is required to prevent wrong scroll amount usage on orientation change in following case.
            	 * User opens event details screen in landscape where it's scrollable by some amount & hence
            	 * user scrolls it. Now on changing orientation, same scrollY is applied from onStart() 
            	 * due to prevScrollY's stored value. But suppose if screen is not scrollable in portrait, then 
            	 * we need to revert these wrong alterations by following call by passing obsrScrlV.getScrollY()
            	 * which is 0 in this case & forceUpdate=true as second argument.
            	 */
            	onScrollChanged(obsrScrlV.getScrollY(), true);
            }
        }
    };
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
		
		actionBarElevation = FragmentUtil.getResources(this).getDimensionPixelSize(R.dimen.action_bar_elevation);
		
		Bundle args = getArguments();
		if (event == null) {
			//Log.d(TAG, "event = null");
			event = (Event) args.getSerializable(BundleKeys.EVENT);
			event.getFriends().clear();
			
			loadEventDetails = new LoadEventDetails(Api.OAUTH_TOKEN, this, this, event);
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
		ViewCompat.setTransitionName(txtEvtTitle, getArguments().getString(BundleKeys.TRANSITION_NAME_SHARED_TEXT));
		
		txtEvtTime = (TextView) rootView.findViewById(R.id.txtEvtTime);
		updateEventSchedule();
		
		txtEvtDesc = (TextView) rootView.findViewById(R.id.txtDesc);
		imgDown = (ImageView) rootView.findViewById(R.id.imgDown);
		updateDescVisibility();
		
		imgEvt = (ImageView) rootView.findViewById(R.id.imgEvt);
		updateEventImg();
		ViewCompat.setTransitionName(imgEvt, getArguments().getString(BundleKeys.TRANSITION_NAME_SHARED_IMAGE));
		
		obsrScrlV = (ObservableScrollView) rootView.findViewById(R.id.obsrScrlV);
		obsrScrlV.setListener(this);
		obsrScrlV.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
		
		rltLytPrgsBar = (RelativeLayout) rootView.findViewById(R.id.rltLytPrgsBar);
		rltLytPrgsBar.setBackgroundResource(R.drawable.bg_no_content_overlay_tab);
		updateProgressBarVisibility();
		
		return rootView;
	}
	
	/*@Override
	public void onStart() {
		//Log.d(TAG, "onStart()");
		super.onStart();
		// to apply screen changes based on last scroll state on orientation change
		if (prevScrollY != UNSCROLLED) {
			onScrollChanged(prevScrollY, true);
		}
	}*/
	
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
		super.onDestroy();
		
		if (loadEventDetails != null && loadEventDetails.getStatus() != Status.FINISHED) {
			loadEventDetails.cancel(true);
		}
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
	}
	
	private void updateProgressBarVisibility() {
		if (allDetailsLoaded) {
			rltLytPrgsBar.setVisibility(View.GONE);
			
		} else {
			rltLytPrgsBar.setVisibility(View.VISIBLE);
		}
	}
	
	private void updateDescVisibility() {
		if (event.getDescription() != null) {
			makeDescVisible();
			
		} else {
			txtEvtDesc.setVisibility(View.GONE);
			imgDown.setVisibility(View.GONE);
		}
	}
	
	private void makeDescVisible() {
		txtEvtDesc.setText(Html.fromHtml(event.getDescription()));
		imgDown.setVisibility(View.VISIBLE);
		imgDown.setOnClickListener(this);
		
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
		isEvtDescExpanded = true;
	}
	
	private void collapseEvtDesc() {
		txtEvtDesc.setMaxLines(MAX_LINES_EVENT_DESC);
		txtEvtDesc.setEllipsize(TruncateAt.END);
		imgDown.setImageDrawable(FragmentUtil.getResources(this).getDrawable(R.drawable.ic_description_expand));
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
	
	private String getEvtTimeIfAvailable() {
		Schedule schedule = event.getSchedule();
		if (schedule != null) {
			if (schedule.getDates().size() > 0) {
				Date date = schedule.getDates().get(0);
				return ConversionUtil.getDateTime(date.getStartDate(), date.isStartTimeAvailable());
			}
		}
		return "";
	}
	
	private void updateEventSchedule() {
		Schedule schedule = event.getSchedule();
		if (schedule != null) {
			if (schedule.getVenue() != null) {
				/*txtEvtLoc.setText(event.getSchedule().getVenue().getName());
				txtEvtLoc.setOnClickListener(this);
				txtVenue.setText(event.getSchedule().getVenue().getName());
				txtVenue.setOnClickListener(this);*/
			}
		}
		txtEvtTime.setText(getEvtTimeIfAvailable());
	}
	
	private void updateDetailsVisibility() {
		updateProgressBarVisibility();
		
		if (allDetailsLoaded) {
			updateEventImg();
			//updateFeaturingVisibility();
			updateEventSchedule();
			/*updateAddressMapVisibility();
			updateFriendsVisibility();
			updateFabs();*/
			
		} else {
			/*rltLytFeaturing.setVisibility(View.GONE);
			rltLytVenue.setVisibility(View.GONE);
			rltLytFriends.setVisibility(View.GONE);
			fabTickets.setVisibility(View.GONE);
			fabSave.setVisibility(View.GONE);*/
		}
	}
	
	private void onScrollChanged(int scrollY, boolean forceUpdate) {
		//Log.d(TAG, "scrollY = " + scrollY);
		// Translate image
		imgEvt.setTranslationY(scrollY / 2);
        
		BaseActivityTab baseActivityTab = (BaseActivityTab) FragmentUtil.getActivity(this);
		
		/*if (limitScrollAt == 0) {
			calculateScrollLimit();
		}*/
		
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
		
		if (scrollY < limitScrollAt) {
			float translationX = scrollY * txtEvtTitleDiffX / (float) limitScrollAt;
			txtEvtTitle.setTranslationX(translationX);
			txtEvtTime.setTranslationX(translationX);
		}
        
		// We take the last child in the scrollview
	    /*View lastChild = (View) obsrScrlV.getChildAt(obsrScrlV.getChildCount() - 1);
	    int diff = (lastChild.getBottom() - (obsrScrlV.getHeight() + obsrScrlV.getScrollY()));
	    Log.d(TAG, "btm = " + view.getBottom() + ", ht = " + obsrScrlV.getHeight() + ", scrollY = " 
	    		+ obsrScrlV.getScrollY() + ", diff = " + diff);
	    if (diff == 0) {
		    // if diff is zero, then the bottom has been reached, where we need to show floating action buttons
	    	fabTickets.show(true);
            fabSave.show(true);
            
	    } else {
			boolean isSignificantDelta = Math.abs(scrollY - prevScrollY) > fabScrollThreshold;
	        if (isSignificantDelta) {
	            if (scrollY > prevScrollY) {
	                fabTickets.hide(true);
	                fabSave.hide(true);
	                
	            } else {
	                fabTickets.show(true);
	                fabSave.show(true);
	            }
	        }
	    }*/
        
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
		
		/*case R.id.txtEvtLoc:
		case R.id.txtVenue:
			((VenueListener)FragmentUtil.getActivity(this)).onVenueSelected(event.getSchedule().getVenue());
			break;*/
			
		case R.id.imgDown:
			if (isEvtDescExpanded) {
				collapseEvtDesc();
				
			} else {
				expandEvtDesc();
			}
			break;
			
		/*case R.id.fabTickets:
			Bundle args = new Bundle();
			args.putString(BundleKeys.URL, event.getSchedule().getBookingInfos().get(0).getBookingUrl());
			((ReplaceFragmentListener)FragmentUtil.getActivity(this)).replaceByFragment(
					AppConstants.FRAGMENT_TAG_WEB_VIEW, args);
			GoogleAnalyticsTracker.getInstance().sendEvent(FragmentUtil.getApplication(this), 
					getScreenName(), GoogleAnalyticsTracker.EVENT_LABEL_TICKETS_BUTTON, 
					GoogleAnalyticsTracker.Type.Event.name(), null, event.getId());
			break;
			
		case R.id.fabSave:
			EventSeekr eventSeekr = (EventSeekr) FragmentUtil.getApplication(this);
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
					fbCallCountForSameEvt = 0;
					event.setNewAttending(Attending.SAVED);
					FbUtil.handlePublishEvent(this, this, AppConstants.PERMISSIONS_FB_PUBLISH_EVT_OR_ART, 
							AppConstants.REQ_CODE_FB_PUBLISH_EVT_OR_ART, event);
				}
			}
			break;*/
			
		default:
			break;
		}
	}
}
