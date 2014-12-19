package com.wcities.eventseeker;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import android.content.Intent;
import android.content.res.Configuration;
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
import com.wcities.eventseeker.asynctask.LoadArtistDetails;
import com.wcities.eventseeker.asynctask.LoadArtistDetails.OnArtistUpdatedListener;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FileUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.TabBar;

public class ArtistDetailsFragment extends FragmentLoadableFromBackStack implements OnClickListener,
		OnArtistUpdatedListener {

	private static final String TAG = ArtistDetailsFragment.class.getName();

	private static final String FRAGMENT_TAG_INFO = "info";
	private static final String FRAGMENT_TAG_EVENTS = "events";
	private static final String FRAGMENT_TAG_NEWS = "news";
	
	protected static enum FooterTxt {
		Follow(R.string.footer_follow),
		Following(R.string.footer_following);
		
		private int id;
		
		private FooterTxt(int id) {
			this.id = id;
		}
		
		public String getStringForm(Fragment fragment) {
			return FragmentUtil.getActivity(fragment).getResources().getString(id);
		}
	}
	
	private SwipeTabsAdapter mTabsAdapter;
	private TabBar tabBar;
	private ShareActionProvider mShareActionProvider;

	private Artist artist;
	private LoadArtistDetails loadArtistDetails;
	
	public interface ArtistDetailsFragmentListener {
        public void onArtistUpdatedByArtistDetailsFragment();
        public void onArtistFollowingUpdated();
    }
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
		setHasOptionsMenu(true);
		
		if (artist == null) {
			artist = (Artist) getArguments().getSerializable(BundleKeys.ARTIST);

			loadArtistDetails = new LoadArtistDetails(Api.OAUTH_TOKEN, artist, this, this);
			AsyncTaskUtil.executeAsyncTask(loadArtistDetails, true);
			
			artist.getVideos().clear();
			
			artist.getFriends().clear();
			//artist.getFriends().add(null);
			
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
		
		Button btnVenues = (Button) vTabBar.findViewById(R.id.btnTab3);
		btnVenues.setText(R.string.news);
		btnVenues.setOnClickListener(this);
		
		TabBar.Tab tabInfo = new TabBar.Tab(btnArtists, FRAGMENT_TAG_INFO, ArtistInfoFragment.class, getArguments());
		mTabsAdapter.addTab(tabInfo, oldAdapter);

		TabBar.Tab tabEvents;
		
		if (((MainActivity)FragmentUtil.getActivity(this)).isTablet()) {
			tabEvents= new TabBar.Tab(btnEvents, FRAGMENT_TAG_EVENTS, ArtistEventsFragmentTab.class, getArguments());			
		} else {
			tabEvents= new TabBar.Tab(btnEvents, FRAGMENT_TAG_EVENTS, ArtistEventsFragment.class, getArguments());			
		}
		
		mTabsAdapter.addTab(tabEvents, oldAdapter);
		
		TabBar.Tab tabNews = new TabBar.Tab(btnVenues, FRAGMENT_TAG_NEWS, ArtistNewsFragment.class, 
				getArguments());
		mTabsAdapter.addTab(tabNews, oldAdapter);
		
		return v;
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		ActionBar actionBar = ((ActionBarActivity)FragmentUtil.getActivity(this)).getSupportActionBar();
		actionBar.setCustomView(null);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (loadArtistDetails != null && loadArtistDetails.getStatus() != Status.FINISHED) {
			loadArtistDetails.cancel(true);
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (((EventSeekr)FragmentUtil.getActivity(this).getApplication()).isTablet()) {
			List<Fragment> pageFragments = mTabsAdapter.getTabFragments();
			for (Iterator<Fragment> iterator = pageFragments.iterator(); iterator.hasNext();) {
				Fragment fragment = iterator.next();
				if (fragment instanceof ArtistEventsFragmentTab) {
					fragment.onActivityResult(requestCode, resultCode, data);
				}
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
	
	private void updateShareIntent() {
	    if (mShareActionProvider != null && artist != null) {
			//Log.d(TAG, "updateShareIntent()");
	    	Intent shareIntent = new Intent(Intent.ACTION_SEND);
		    shareIntent.setType("image/*");
		    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Artist Details");
		    String message = "Checkout " + artist.getName() + " on eventseeker";
		    if (artist.getArtistUrl() != null) {
		    	message += ": " + artist.getArtistUrl();
		    }
		    shareIntent.putExtra(Intent.EXTRA_TEXT, message);
			
			String key = artist.getKey(ImgResolution.LOW);
	        BitmapCache bitmapCache = BitmapCache.getInstance();
			Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
			if (bitmap != null) {
				File tmpFile = FileUtil.createTempShareImgFile(FragmentUtil.getActivity(this).getApplication(), bitmap);
				if (tmpFile != null) {
					shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(tmpFile));
				}
			}
		    
	        mShareActionProvider.setShareIntent(shareIntent);
	        
	        mShareActionProvider.setOnShareTargetSelectedListener(new OnShareTargetSelectedListener() {
				
				@Override
				public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
					String shareTarget = intent.getComponent().getPackageName();
					GoogleAnalyticsTracker.getInstance().sendShareEvent(FragmentUtil.getApplication(ArtistDetailsFragment.this), 
							getScreenName(), shareTarget, Type.Artist, artist.getId());
					return false;
				}
			});
	    }
	}
	
	@Override
	public void onArtistUpdated() {

		if (mTabsAdapter != null) {
			updateShareIntent();
			
			List<Fragment> pageFragments = mTabsAdapter.getTabFragments();
			for (Iterator<Fragment> iterator = pageFragments.iterator(); iterator.hasNext();) {
				ArtistDetailsFragmentListener fragment = (ArtistDetailsFragmentListener) iterator.next();
				fragment.onArtistUpdatedByArtistDetailsFragment();
			}
		}
	}
	
	public void onArtistFollowingUpdated() {
		List<Fragment> pageFragments = mTabsAdapter.getTabFragments();
		for (Iterator<Fragment> iterator = pageFragments.iterator(); iterator.hasNext();) {
			ArtistDetailsFragmentListener fragment = (ArtistDetailsFragmentListener) iterator.next();
			fragment.onArtistFollowingUpdated();
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
			
		case R.id.btnTab3:
			tabBar.select(tabBar.getTabByTag(FRAGMENT_TAG_NEWS));
			break;
			
		default:
			break;
		}
	}

	@Override
	public String getScreenName() {
		return "Artist Detail Screen";
	}
}
