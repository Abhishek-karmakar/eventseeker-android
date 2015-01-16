package com.wcities.eventseeker;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.nineoldandroids.view.ViewHelper;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.custom.view.ObservableScrollView;
import com.wcities.eventseeker.custom.view.ObservableScrollView.ObservableScrollViewListener;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.VersionUtil;

public class EventDetailsFragment extends FragmentLoadableFromBackStack implements ObservableScrollViewListener, 
		DrawerListener {
	
	private static final String TAG = EventDetailsFragment.class.getSimpleName();
	
	private static final int UNSCROLLED = -1;
	
	private ImageView imgEvent;
	private TextView txtEvtTitle;
	
	private int limitScrollAt, actionBarElevation, actionBarTitleTextSize, txtEvtTitleTextSize, prevScrollY = UNSCROLLED;
	private boolean isScrollLimitReached, isDrawerOpen;
	private String title = "";
	private float minTitleScale;
	
	private String imgEventTransitionName;
	
	private Event event;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setHasOptionsMenu(true);
		setRetainInstance(true);
		
		actionBarElevation = FragmentUtil.getResources(this).getDimensionPixelSize(R.dimen.action_bar_elevation);
		
		Bundle args = getArguments();
		if (args != null && args.containsKey(BundleKeys.SHARED_IMG_TRANSITION_NAME)) {
			imgEventTransitionName = args.getString(BundleKeys.SHARED_IMG_TRANSITION_NAME);
		}
		
		if (event == null) {
			//Log.d(TAG, "event = null");
			event = (Event) args.getSerializable(BundleKeys.EVENT);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		/**
		 * on orientation change we need to recalculate this due to different values of 
		 * action_bar_ht on both orientations
		 */
		calculateScrollLimit();
		
		View v = inflater.inflate(R.layout.fragment_event_details, container, false);
		
		imgEvent = (ImageView) v.findViewById(R.id.imgEvent);
		if (imgEventTransitionName != null) {
			imgEvent.setTransitionName(imgEventTransitionName);
		}
		txtEvtTitle = (TextView) v.findViewById(R.id.txtEvtTitle);
		
		updateEventImg();
		
		final ObservableScrollView obsrScrlV = (ObservableScrollView) v.findViewById(R.id.obsrScrlV);
		obsrScrlV.setListener(this);
		
		obsrScrlV.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
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
                        	obsrScrlV.scrollTo(0, prevScrollY);
                        	
                        	if (isDrawerOpen) {
                				onDrawerOpened();
                			}
                        }
                    }
                });
		
		return v;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		((MainActivity) FragmentUtil.getActivity(this)).setVStatusBarVisibility(View.GONE, AppConstants.INVALID_ID);
		if (prevScrollY != UNSCROLLED) {
			onScrollChanged(prevScrollY, true);
			
			if (isDrawerOpen) {
				onDrawerOpened();
			}
		}
	}
	
	@Override
	public void onStop() {
		super.onStop();
		
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		ma.setToolbarBg(ma.getResources().getColor(R.color.colorPrimary));
		ma.setVStatusBarVisibility(View.VISIBLE, R.color.colorPrimaryDark);
		ma.setVStatusBarLayeredVisibility(View.GONE, AppConstants.INVALID_ID);
	}
	
	private void updateEventImg() {
		//Log.d(TAG, "updateEventImg(), url = " + event.getLowResImgUrl());
		if (event.doesValidImgUrlExist()) {
			String key = event.getKey(ImgResolution.LOW);
	        BitmapCache bitmapCache = BitmapCache.getInstance();
			Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
			if (bitmap != null) {
		        imgEvent.setImageBitmap(bitmap);
		        
		    } else {
		    	imgEvent.setImageBitmap(null);
		    	AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
		        asyncLoadImg.loadImg(imgEvent, ImgResolution.LOW, event);
		    }
		}
	}
	
	private void calculateScrollLimit() {
		Resources res = FragmentUtil.getResources(this);
		limitScrollAt = res.getDimensionPixelSize(R.dimen.img_evt_ht_event_details) - res.getDimensionPixelSize(
				R.dimen.action_bar_ht);
		
		if (VersionUtil.isApiLevelAbove18()) {
			limitScrollAt -= ((MainActivity) FragmentUtil.getActivity(this)).getStatusBarHeight();
		}
		
		actionBarTitleTextSize = res.getDimensionPixelSize(R.dimen.abc_text_size_title_material_toolbar);
		txtEvtTitleTextSize = res.getDimensionPixelSize(R.dimen.txt_evt_title_txt_size_event_details);
		minTitleScale = actionBarTitleTextSize / (float) txtEvtTitleTextSize;
	}
	
	private void onScrollChanged(int scrollY, boolean forceUpdate) {
		// Translate image
		ViewHelper.setTranslationY(imgEvent, scrollY / 2);
		
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		
		if (limitScrollAt == 0) {
			calculateScrollLimit();
		}
		
		if ((!isScrollLimitReached || forceUpdate) && scrollY >= limitScrollAt) {
			ma.animateToolbarElevation(0.0f, actionBarElevation);
			
			ma.setVStatusBarLayeredVisibility(View.VISIBLE, R.color.colorPrimaryDark);
			ma.setToolbarBg(ma.getResources().getColor(R.color.colorPrimary));
			
			title = "Cut Copy";
			ma.updateTitle(title);
			
			isScrollLimitReached = true;
			
		} else if ((isScrollLimitReached || forceUpdate) && scrollY < limitScrollAt) {
			ma.animateToolbarElevation(actionBarElevation, 0.0f);
			
			ma.setVStatusBarLayeredVisibility(View.GONE, AppConstants.INVALID_ID);
			ma.setToolbarBg(Color.TRANSPARENT);
			
			title = "";
			ma.updateTitle(title);
			
			isScrollLimitReached = false;
		}
		
		if (scrollY < limitScrollAt) {
			float scale = 1 - (((1 - minTitleScale) / limitScrollAt) * scrollY);
			//Log.d(TAG, "scale = " + scale);
			
			ViewHelper.setPivotX(txtEvtTitle, 0);
	        ViewHelper.setPivotY(txtEvtTitle, txtEvtTitle.getHeight() / 2);
	        ViewHelper.setScaleX(txtEvtTitle, scale);
	        ViewHelper.setScaleY(txtEvtTitle, scale);
		}
        
		prevScrollY = scrollY;
	}
	
	public String getCurrentTitle() {
		return title;
	}
	
	private void onDrawerOpened() {
		isDrawerOpen = true;
		
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		ma.setToolbarBg(ma.getResources().getColor(R.color.colorPrimary));
		ma.setToolbarElevation(ma.getResources().getDimensionPixelSize(R.dimen.action_bar_elevation));
		ma.setVStatusBarLayeredVisibility(View.VISIBLE, R.color.colorPrimaryDark);
		title = "Cut Copy";
		ma.updateTitle(title);
	}
	
	@Override
	public String getScreenName() {
		return "Event Detail Screen";
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
	public void onDrawerClosed(View arg0) {
		isDrawerOpen = false;
		onScrollChanged(prevScrollY, true);
	}

	@Override
	public void onDrawerOpened(View arg0) {
		onDrawerOpened();
	}

	@Override
	public void onDrawerSlide(View drawerView, float slideOffset) {
		if (!isScrollLimitReached) {
			((MainActivity)FragmentUtil.getActivity(this)).updateToolbarOnDrawerSlide(slideOffset);
		}
	}

	@Override
	public void onDrawerStateChanged(int arg0) {
		// TODO Auto-generated method stub
	}
}
