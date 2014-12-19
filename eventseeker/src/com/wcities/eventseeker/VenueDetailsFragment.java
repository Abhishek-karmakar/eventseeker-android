package com.wcities.eventseeker;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
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
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.util.FileUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.TabBar;

public class VenueDetailsFragment extends FragmentLoadableFromBackStack implements OnClickListener {

	private static final String TAG = VenueDetailsFragment.class.getName();

	private static final String FRAGMENT_TAG_INFO = "info";
	private static final String FRAGMENT_TAG_EVENTS = "events";
	
	private Venue venue;
	
	private SwipeTabsAdapter mTabsAdapter;
	private TabBar tabBar;
	private ShareActionProvider mShareActionProvider;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
		setHasOptionsMenu(true);
		
		if (venue == null) {
			venue = (Venue) getArguments().getSerializable(BundleKeys.VENUE);
			updateShareIntent();
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_custom_tabs, null);
		
		int orientation = getResources().getConfiguration().orientation;

		ViewPager viewPager = (ViewPager) v.findViewById(R.id.tabContentFrame);
		SwipeTabsAdapter oldAdapter = mTabsAdapter;
		tabBar = new TabBar(getChildFragmentManager());
		mTabsAdapter = new SwipeTabsAdapter(this, viewPager, tabBar, orientation);
		
		View vTabBar;
		
		if (orientation == Configuration.ORIENTATION_PORTRAIT) {
			vTabBar = v;
			
		} else {
			ActionBar actionBar = ((ActionBarActivity)FragmentUtil.getActivity(this)).getSupportActionBar();
			actionBar.setDisplayShowCustomEnabled(true);
			
			vTabBar = inflater.inflate(R.layout.custom_actionbar_tabs, null);
			actionBar.setCustomView(vTabBar);
		}
		
		Button btnArtists = (Button) vTabBar.findViewById(R.id.btnTab1);
		btnArtists.setText(R.string.info);
		btnArtists.setOnClickListener(this);
		
		Button btnEvents = (Button) vTabBar.findViewById(R.id.btnTab2);
		btnEvents.setText(R.string.events);
		btnEvents.setOnClickListener(this);
		
		vTabBar.findViewById(R.id.btnTab3).setVisibility(View.GONE);
		vTabBar.findViewById(R.id.vDivider2).setVisibility(View.GONE);
		
		TabBar.Tab tabInfo = new TabBar.Tab(btnArtists, FRAGMENT_TAG_INFO, VenueInfoFragment.class, 
				getArguments());
		mTabsAdapter.addTab(tabInfo, oldAdapter);

		TabBar.Tab tabEvents = new TabBar.Tab(btnEvents, FRAGMENT_TAG_EVENTS, VenueEventsFragment.class, 
				getArguments());
		mTabsAdapter.addTab(tabEvents, oldAdapter);
		
		return v;
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		ActionBar actionBar = ((ActionBarActivity)FragmentUtil.getActivity(this)).getSupportActionBar();
		actionBar.setCustomView(null);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		List<Fragment> pageFragments = mTabsAdapter.getTabFragments();
		for (Iterator<Fragment> iterator = pageFragments.iterator(); iterator.hasNext();) {
			Fragment fragment = iterator.next();
			if (fragment instanceof VenueEventsFragment) {
				fragment.onActivityResult(requestCode, resultCode, data);
			}
		}
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_artist_details, menu);
		
		MenuItem item = (MenuItem) menu.findItem(R.id.action_share);
	    mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
	    updateShareIntent();
	    
    	super.onCreateOptionsMenu(menu, inflater);
	}
	
	protected void updateShareIntent() {
	    if (mShareActionProvider != null && venue != null) {
	    	Intent shareIntent = new Intent(Intent.ACTION_SEND);
		    shareIntent.setType("image/*");
		    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Venue Details");
		    shareIntent.putExtra(Intent.EXTRA_TEXT, venue.getName());
			
			String key = venue.getKey(ImgResolution.LOW);
	        BitmapCache bitmapCache = BitmapCache.getInstance();
			Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
			if (bitmap != null) {
				Log.d(TAG, "bitmap != null");
				File tmpFile = FileUtil.createTempShareImgFile(FragmentUtil.getActivity(this).getApplication(), bitmap);
				if (tmpFile != null) {
					shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(tmpFile));
				}
				
			} else {
				Log.d(TAG, "bitmap = null");
			}
		    
	        mShareActionProvider.setShareIntent(shareIntent);
	        
	        mShareActionProvider.setOnShareTargetSelectedListener(new OnShareTargetSelectedListener() {
				
				@Override
				public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
					String shareTarget = intent.getComponent().getPackageName();
					GoogleAnalyticsTracker.getInstance().sendShareEvent(FragmentUtil.getApplication(VenueDetailsFragment.this), 
							getScreenName(), shareTarget, Type.Venue, venue.getId());
					return false;
				}
			});
	    }
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.btnTab1:
			tabBar.select(tabBar.getTabByTag(FRAGMENT_TAG_INFO));
			break;
			
		case R.id.btnTab2:
			tabBar.select(tabBar.getTabByTag(FRAGMENT_TAG_EVENTS));
			break;
			
		default:
			break;
		}
	}
	
	protected void onDriveClicked() {
		List<Fragment> pageFragments = mTabsAdapter.getTabFragments();
		for (Iterator<Fragment> iterator = pageFragments.iterator(); iterator.hasNext();) {
			Fragment fragment = iterator.next();
			if (fragment instanceof VenueInfoFragment) {
				((VenueInfoFragment)fragment).onDriveClicked();
			}
		}
	}

	@Override
	public String getScreenName() {
		return "Venue Detail Screen";
	}
}
