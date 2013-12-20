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
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.wcities.eventseeker.ConnectAccountsFragment.Service;
import com.wcities.eventseeker.ConnectAccountsFragment.ServiceAccount;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.SyncArtists;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.OnFragmentAliveListener;
import com.wcities.eventseeker.util.FragmentUtil;

public class TwitterFragment extends FragmentLoadableFromBackStack implements OnFragmentAliveListener {
	
	private static final String TAG = TwitterFragment.class.getName();
	
	private boolean isAlive;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		isAlive = true;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_twitter_login, null);
		String url = getArguments().getString(BundleKeys.URL);
		final Twitter twitter = (Twitter) getArguments().getSerializable(BundleKeys.TWITTER);
		
		WebView webView = (WebView) v.findViewById(R.id.webView);
		
		webView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				if (url.contains(AppConstants.TWITTER_CALLBACK_URL)) {
					Log.d(TAG, "url = " + url);
					ServiceAccount serviceAccount = (ServiceAccount) getArguments().getSerializable(
							BundleKeys.SERVICE_ACCOUNTS);
					serviceAccount.isInProgress = true;
					
					Uri uri = Uri.parse(url);
					final String oauthVerifier = uri.getQueryParameter("oauth_verifier");
					/*Bundle args = new Bundle();
					args.putString(BundleKeys.OAUTH_VERIFIER, oauthVerifier);
					args.putSerializable(BundleKeys.TWITTER, twitter);
					
					((ReplaceFragmentListener)FragmentUtil.getActivity(TwitterFragment.this))
						.replaceByFragment(AppConstants.FRAGMENT_TAG_TWITTER_SYNCING, args);*/
					
					// Pair up our request with the response
					new Thread(new Runnable() {
						
						@Override
						public void run() {
							try {
								AccessToken at = twitter.getOAuthAccessToken(oauthVerifier);
								ConfigurationBuilder builder = new ConfigurationBuilder();
					            builder.setOAuthConsumerKey(AppConstants.TWITTER_CONSUMER_KEY);
					            builder.setOAuthConsumerSecret(AppConstants.TWITTER_CONSUMER_SECRET);
					            builder.setOAuthAccessToken(at.getToken());
					            builder.setOAuthAccessTokenSecret(at.getTokenSecret());
					            Configuration conf = builder.build();
					            Twitter t = new TwitterFactory(conf).getInstance();
					            
								List<String> artistNames = new ArrayList<String>();
								long cursor = -1;
								PagableResponseList<User> responseList;
								do {
									responseList = t.getFriendsList(t.getScreenName(), cursor);
									for (Iterator<User> iterator = responseList.iterator(); iterator.hasNext();) {
										User user = (User) iterator.next();
										//Log.d(TAG, "username = " + user.getName());
										artistNames.add(user.getName());
									}
									
								} while ((cursor = responseList.getNextCursor()) != 0);
								
								new SyncArtists(artistNames, (EventSeekr) FragmentUtil.getActivity(TwitterFragment.this).getApplication(), 
										Service.Twitter, TwitterFragment.this).execute();
								
							} catch (TwitterException e) {
								e.printStackTrace();
							}
						}
					}).start();
					return true;
				}
				
				EventSeekr eventSeekr = (EventSeekr) FragmentUtil.getActivity(TwitterFragment.this).getApplicationContext();
				eventSeekr.setSyncCount(Service.Twitter, EventSeekr.UNSYNC_COUNT);

				return false;
			}
		});
		webView.loadUrl(url);
		
		return v;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		isAlive = false;
	}

	@Override
	public boolean isAlive() {
		return isAlive;
	}
}

