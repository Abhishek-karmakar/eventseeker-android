package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import twitter4j.PagableResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.Toast;

import com.wcities.eventseeker.ConnectAccountsFragment.Service;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.SyncArtists;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.SyncArtistListener;
import com.wcities.eventseeker.util.FragmentUtil;

public class TwitterSyncingFragment extends FragmentLoadableFromBackStack {

	private static final String TAG = TwitterSyncingFragment.class.getSimpleName();
	
	private Twitter twitter;
	private Resources res;

	private SyncArtistListener syncArtistListener;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		res = FragmentUtil.getResources(this);
		
		syncArtistListener = (SyncArtistListener) getArguments().getSerializable(BundleKeys.SYNC_ARTIST_LISTENER);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		syncArtistListener.onArtistSyncStarted();
		
		if (twitter == null) {
			twitter = (Twitter) getArguments().getSerializable(BundleKeys.TWITTER);
			final String oauthVerifier = getArguments().getString(BundleKeys.OAUTH_VERIFIER);
			final EventSeekr eventSeekr = (EventSeekr) FragmentUtil.getActivity(TwitterSyncingFragment.this)
					.getApplication();
			
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					final List<String> artistNames = new ArrayList<String>();

					try {
						AccessToken at = twitter.getOAuthAccessToken(oauthVerifier);
						ConfigurationBuilder builder = new ConfigurationBuilder();
			            builder.setOAuthConsumerKey(AppConstants.TWITTER_CONSUMER_KEY);
			            builder.setOAuthConsumerSecret(AppConstants.TWITTER_CONSUMER_SECRET);
			            builder.setOAuthAccessToken(at.getToken());
			            builder.setOAuthAccessTokenSecret(at.getTokenSecret());
			            Configuration conf = builder.build();
			            Twitter t = new TwitterFactory(conf).getInstance();
			            
						long cursor = -1;
						int countPerPage = 200;
						PagableResponseList<User> responseList;
						do {
							responseList = t.getFriendsList(t.getScreenName(), cursor, countPerPage);
							for (Iterator<User> iterator = responseList.iterator(); iterator.hasNext();) {
								User user = (User) iterator.next();
								//Log.d(TAG, "username = " + user.getName());
								artistNames.add(user.getName());
							}
							
						} while ((cursor = responseList.getNextCursor()) != 0);
						
						onGetFriendsListSucceeded(artistNames, eventSeekr);
						
					} catch (TwitterException e) {
						e.printStackTrace();
						if (e.exceededRateLimitation()) {
							if (artistNames.isEmpty()) {
								FragmentUtil.getActivity(TwitterSyncingFragment.this).runOnUiThread(new Runnable() {
									
									@Override
									public void run() {
										Toast.makeText(eventSeekr, res.getString(R.string.twitter_call_limit_exceeded), Toast.LENGTH_LONG).show();
									}
								});
								onGetFriendsListFailed(eventSeekr);
								
							} else {
								onGetFriendsListSucceeded(artistNames, eventSeekr);
							}
							
						} else {
							onGetFriendsListFailed(eventSeekr);
						}
					}
				}
			}).start();
		}
	}
	
	private void onGetFriendsListSucceeded(final List<String> artistNames, final EventSeekr eventSeekr) {
		FragmentUtil.getActivity(TwitterSyncingFragment.this).runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				new SyncArtists(Api.OAUTH_TOKEN, artistNames, eventSeekr, 
						Service.Twitter, /*TwitterSyncingFragment.this,*/ Service.Twitter.getArtistSource()).execute();
			}
		});
	}
	
	private void onGetFriendsListFailed(final EventSeekr eventSeekr) {
		eventSeekr.setSyncCount(Service.Twitter, EventSeekr.UNSYNC_COUNT);
		FragmentUtil.getActivity(TwitterSyncingFragment.this).runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				FragmentUtil.getActivity(TwitterSyncingFragment.this).onBackPressed();
			}
		});
	}

	@Override
	public String getScreenName() {
		return null;
	}
}
