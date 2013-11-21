package com.wcities.eventseeker;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
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
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.ArtistApi;
import com.wcities.eventseeker.api.ArtistApi.Method;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.jsonparser.ArtistApiJSONParser;
import com.wcities.eventseeker.util.BitmapUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.TabBar;

public class ArtistDetailsFragment extends FragmentLoadableFromBackStack implements OnClickListener {

	private static final String TAG = ArtistDetailsFragment.class.getName();

	private static final String FRAGMENT_TAG_INFO = "info";
	private static final String FRAGMENT_TAG_EVENTS = "events";
	private static final String FRAGMENT_TAG_NEWS = "news";
	
	protected static enum FooterTxt {
		Follow,
		Following;
	}
	
	private SwipeTabsAdapter mTabsAdapter;
	private TabBar tabBar;
	private ShareActionProvider mShareActionProvider;

	private Artist artist;
	private LoadArtistDetails loadArtistDetails;
	private ContentResolver contentResolver;
	
	public interface ArtistDetailsFragmentChildListener {
        public void onArtistUpdatedByArtistDetailsFragment();
        public void onArtistFollowingUpdated();
    }
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
		setHasOptionsMenu(true);
		
		contentResolver = FragmentUtil.getActivity(this).getContentResolver();
		
		if (artist == null) {
			artist = (Artist) getArguments().getSerializable(BundleKeys.ARTIST);

			loadArtistDetails = new LoadArtistDetails();
			loadArtistDetails.execute();
			
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
		btnArtists.setText("INFO");
		btnArtists.setOnClickListener(this);
		
		Button btnEvents = (Button) vTabBar.findViewById(R.id.btnTab2);
		btnEvents.setText("EVENTS");
		btnEvents.setOnClickListener(this);
		
		Button btnVenues = (Button) vTabBar.findViewById(R.id.btnTab3);
		btnVenues.setText("NEWS");
		btnVenues.setOnClickListener(this);
		
		TabBar.Tab tabInfo = new TabBar.Tab(btnArtists, FRAGMENT_TAG_INFO, ArtistInfoFragment.class, 
				getArguments());
		mTabsAdapter.addTab(tabInfo, oldAdapter);

		TabBar.Tab tabEvents = new TabBar.Tab(btnEvents, FRAGMENT_TAG_EVENTS, ArtistEventsFragment.class, 
				getArguments());
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
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_artist_details, menu);
		
		MenuItem item = (MenuItem) menu.findItem(R.id.action_share);
	    mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
	    updateShareIntent();
	    
    	super.onCreateOptionsMenu(menu, inflater);
	}
	
	private void updateShareIntent() {
	    if (mShareActionProvider != null && artist != null) {
			Log.d(TAG, "updateShareIntent()");
	    	Intent shareIntent = new Intent(Intent.ACTION_SEND);
		    shareIntent.setType("image/*");
		    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Artist Details");
		    String message = "Checkout " + artist.getName() + " on eventseeker";
		    if (artist.getArtistUrl() != null) {
		    	message += ": " + artist.getArtistUrl();
		    }
		    shareIntent.putExtra(Intent.EXTRA_TEXT, message);
		    /*if (artist.doesValidImgUrlExist()) {
		    	Log.d(TAG, "url = " + artist.getValidImgUrl());
		    	shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(artist.getValidImgUrl()));
		    }*/
			
			String key = artist.getKey(ImgResolution.LOW);
	        BitmapCache bitmapCache = BitmapCache.getInstance();
			Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
			if (bitmap != null) {
				shareIntent.putExtra(Intent.EXTRA_STREAM, BitmapUtil.getImgFileUri(bitmap));
			}
		    
	        mShareActionProvider.setShareIntent(shareIntent);
	    }
	}
	
	private class LoadArtistDetails extends AsyncTask<Void, Void, Void> {
		
		@Override
		protected Void doInBackground(Void... params) {
			ArtistApi artistApi = new ArtistApi(Api.OAUTH_TOKEN);
			artistApi.setArtistId(artist.getId());
			artistApi.setMethod(Method.artistDetail);
			// null check is not required here, since if it's null, that's handled from eventApi
			artistApi.setUserId(((EventSeekr)FragmentUtil.getActivity(ArtistDetailsFragment.this).getApplication()).getWcitiesId());
			artistApi.setFriendsEnabled(true);
			
			try {
				JSONObject jsonObject = artistApi.getArtists();
				ArtistApiJSONParser jsonParser = new ArtistApiJSONParser();
				jsonParser.fillArtistDetails(artist, jsonObject);
				
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				
			} catch (IOException e) {
				e.printStackTrace();
				
			} catch (JSONException e) {
				e.printStackTrace();
			}

			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			//Log.d(TAG, "LoadEventDetails onPostExecute()");
			
			if (mTabsAdapter != null) {
				onArtistUpdated();
			}
		}    	
    }
	
	public void onArtistUpdated() {
		updateShareIntent();
		
		List<Fragment> pageFragments = mTabsAdapter.getTabFragments();
		for (Iterator<Fragment> iterator = pageFragments.iterator(); iterator.hasNext();) {
			ArtistDetailsFragmentChildListener fragment = (ArtistDetailsFragmentChildListener) iterator.next();
			fragment.onArtistUpdatedByArtistDetailsFragment();
		}
	}
	
	public void onArtistFollowingUpdated() {
		List<Fragment> pageFragments = mTabsAdapter.getTabFragments();
		for (Iterator<Fragment> iterator = pageFragments.iterator(); iterator.hasNext();) {
			ArtistDetailsFragmentChildListener fragment = (ArtistDetailsFragmentChildListener) iterator.next();
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
}
