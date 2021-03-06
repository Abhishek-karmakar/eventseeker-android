package com.wcities.eventseeker;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.ShareActionProvider.OnShareTargetSelectedListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.wcities.eventseeker.adapter.SwipeTabsAdapter;
import com.wcities.eventseeker.analytics.GoogleAnalyticsTracker;
import com.wcities.eventseeker.analytics.GoogleAnalyticsTracker.Type;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.LoadEventDetails;
import com.wcities.eventseeker.asynctask.LoadEventDetails.OnEventUpdatedListner;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FileUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.TabBar;

import java.io.File;
import java.util.Iterator;
import java.util.List;

public class EventDetailsFragment1 extends FragmentLoadableFromBackStack implements OnClickListener, 
		OnEventUpdatedListner {

	private static final String TAG = EventDetailsFragment.class.getName();

	private static final String FRAGMENT_TAG_INFO = "info";
	private static final String FRAGMENT_TAG_FEATURING = "featuring";
	
	private SwipeTabsAdapter mTabsAdapter;

	private Event event;
	private LoadEventDetails loadEventDetails;
	private int orientation;
	private boolean enableTabs;//, isEvtDeatilsLoaded;

	private TabBar tabBar;
	private ShareActionProvider mShareActionProvider;
	private View vTabBar;
	
	private Resources res;
	
    public interface EventDetailsFragmentChildListener {
        public void onEventUpdatedByEventDetailsFragment();
        public void onEventAttendingUpdated();
    }
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		//Log.d(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
		setHasOptionsMenu(true);
		
		res = FragmentUtil.getResources(this);
		
		if (event == null) {
			//Log.d(TAG, "event = null");
			event = (Event) getArguments().getSerializable(BundleKeys.EVENT);
			//Log.d(TAG, "lat = " + event.getSchedule().getVenue().getAddress().getLat() + ", lon = " + event.getSchedule().getVenue().getAddress().getLon());
			enableTabs = event.hasArtists();
			//Log.d(TAG, "enableTabs = " + enableTabs);
			
			loadEventDetails = new LoadEventDetails(Api.OAUTH_TOKEN, this, this, event);
			AsyncTaskUtil.executeAsyncTask(loadEventDetails, true);
			
			event.getFriends().clear();
			//event.getFriends().add(null);
			
			updateShareIntent();
		}
		//Log.d(TAG, "onCreate() done");
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		//Log.d(TAG, "onCreateView()");
		View v = inflater.inflate(R.layout.fragment_custom_tabs, null);
		
		orientation = getResources().getConfiguration().orientation;

		ViewPager viewPager = (ViewPager) v.findViewById(R.id.tabContentFrame);
		SwipeTabsAdapter oldAdapter = mTabsAdapter;
		tabBar = new TabBar(getChildFragmentManager());
		mTabsAdapter = new SwipeTabsAdapter(this, viewPager, tabBar, orientation);
		
		if (orientation == Configuration.ORIENTATION_PORTRAIT) {
			vTabBar = v;
			
			if (!enableTabs) {
				vTabBar.findViewById(R.id.tabBar).setVisibility(View.GONE);
			}
			
		} else {
			vTabBar = inflater.inflate(R.layout.custom_actionbar_tabs, null);
			//LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER);
			
			if (enableTabs) {
				ActionBar actionBar = ((ActionBarActivity)FragmentUtil.getActivity(this)).getSupportActionBar();
				actionBar.setDisplayShowCustomEnabled(true);
				actionBar.setCustomView(vTabBar);
			}
		}
		
		Button btnInfo = (Button) vTabBar.findViewById(R.id.btnTab1);
		btnInfo.setText(R.string.info);
		btnInfo.setOnClickListener(this);
		
		TabBar.Tab tabInfo = new TabBar.Tab(btnInfo, FRAGMENT_TAG_INFO, EventInfoFragment.class, getArguments());
		mTabsAdapter.addTab(tabInfo, oldAdapter);
		
		if (enableTabs) {
			Button btnFeaturing = (Button) vTabBar.findViewById(R.id.btnTab2);
			btnFeaturing.setText(R.string.featuring);
			btnFeaturing.setOnClickListener(this);
			
			TabBar.Tab tabFeaturing = new TabBar.Tab(btnFeaturing, FRAGMENT_TAG_FEATURING, 
					EventFeaturingFragment.class, getArguments());
			mTabsAdapter.addTab(tabFeaturing, oldAdapter);
			
			vTabBar.findViewById(R.id.btnTab3).setVisibility(View.GONE);
			vTabBar.findViewById(R.id.vDivider2).setVisibility(View.GONE);
		}
		
		//Log.d(TAG, "onCreateView() done - " + (System.currentTimeMillis() / 1000));
		return v;
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		//Log.d(TAG, "onDestroyView()");
		ActionBar actionBar = ((ActionBarActivity)FragmentUtil.getActivity(this)).getSupportActionBar();
		actionBar.setDisplayShowCustomEnabled(false);
		
		/**
		 * set null listener, otherwise even for artist/venue details screen when selecting 
		 * "add to calendar" option it calls this listener's onShareTargetSelected() method which in turn 
		 * sets eventToAddToCalendar on EventSeekr class. This results in sharing event wrongly from 
		 * artist/venue details screen.
		 */
		if (mShareActionProvider != null) {
			mShareActionProvider.setOnShareTargetSelectedListener(null);
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		//Log.d(TAG, "onDestroy()");
		
		if (loadEventDetails != null && loadEventDetails.getStatus() != Status.FINISHED) {
			loadEventDetails.cancel(true);
		}
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_event_details, menu);
		
		MenuItem item = (MenuItem) menu.findItem(R.id.action_share);
	    mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
	    updateShareIntent();
	    
    	super.onCreateOptionsMenu(menu, inflater);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult()");
		super.onActivityResult(requestCode, resultCode, data);
		List<Fragment> pageFragments = mTabsAdapter.getTabFragments();
		for (Iterator<Fragment> iterator = pageFragments.iterator(); iterator.hasNext();) {
			Fragment fragment = iterator.next();
			fragment.onActivityResult(requestCode, resultCode, data);
		}
	}
	
	private void updateShareIntent() {
		//Log.d(TAG, "updateShareIntent()");
	    if (mShareActionProvider != null && event != null) {
	    	Intent shareIntent = new Intent(Intent.ACTION_SEND);
		    shareIntent.setType("image/*");
		    shareIntent.putExtra(Intent.EXTRA_SUBJECT, res.getString(R.string.title_event_details));
		    String message = "Checkout " + event.getName();
		    if (event.getSchedule() != null && event.getSchedule().getVenue() != null) {
		    	message += " @ " + event.getSchedule().getVenue().getName();
		    }
		    if (event.getEventUrl() != null) {
		    	message += ": " + event.getEventUrl();
		    }
		    shareIntent.putExtra(Intent.EXTRA_TEXT, message);
			
			String key = event.getKey(ImgResolution.LOW);
	        BitmapCache bitmapCache = BitmapCache.getInstance();
			Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
			if (bitmap != null) {
				File tmpFile = FileUtil.createTempShareImgFile(FragmentUtil.getActivity(this).getApplication(), bitmap);
				if (tmpFile != null) {
					shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(tmpFile));
				}
			}
		    
	        mShareActionProvider.setShareIntent(shareIntent);
	        
			final EventSeekr eventSeekr = (EventSeekr) FragmentUtil.getActivity(this).getApplication();
	        mShareActionProvider.setOnShareTargetSelectedListener(new OnShareTargetSelectedListener() {
				
				@Override
				public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
					String shareTarget = intent.getComponent().getPackageName();
					//Log.d(TAG, "shareTarget = " + shareTarget);
					if (eventSeekr.getPackageName().equals(shareTarget)) {
						//Log.d(TAG, "shareTarget = " + shareTarget);
						// required to handle "add to calendar" action
						eventSeekr.setEventToAddToCalendar(event);
					}
					
					GoogleAnalyticsTracker.getInstance().sendShareEvent(eventSeekr, getScreenName(), 
							shareTarget, Type.Event, event.getId());
					return false;
				}
			});
	    }
	}
	
	@Override
	public void onEventUpdated() {
		//Log.d(TAG, "onEventUpdated()");
		updateShareIntent();
		
		if (mTabsAdapter != null) {
			List<Fragment> pageFragments = mTabsAdapter.getTabFragments();
			for (Iterator<Fragment> iterator = pageFragments.iterator(); iterator.hasNext();) {
				EventDetailsFragmentChildListener fragment = (EventDetailsFragmentChildListener) iterator.next();
				fragment.onEventUpdatedByEventDetailsFragment();
			}
		}
		//isEvtDeatilsLoaded = true;
	}
	
	public void onEventAttendingUpdated() {
		List<Fragment> pageFragments = mTabsAdapter.getTabFragments();
		for (Iterator<Fragment> iterator = pageFragments.iterator(); iterator.hasNext();) {
			EventDetailsFragmentChildListener fragment = (EventDetailsFragmentChildListener) iterator.next();
			fragment.onEventAttendingUpdated();
		}
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.btnTab1:
			tabBar.select(tabBar.getTabByTag(FRAGMENT_TAG_INFO));
			break;
			
		case R.id.btnTab2:
			tabBar.select(tabBar.getTabByTag(FRAGMENT_TAG_FEATURING));
			break;
			
		default:
			break;
		}
	}

	@Override
	public String getScreenName() {
		return "Event Detail Screen";
	}
}
