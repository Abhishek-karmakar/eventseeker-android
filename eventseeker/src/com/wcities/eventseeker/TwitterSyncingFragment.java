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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.wcities.eventseeker.ConnectAccountsFragment.Service;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.SyncArtists;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.OnFragmentAliveListener;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.ViewUtil.AnimationUtil;

public class TwitterSyncingFragment extends FragmentLoadableFromBackStack implements OnClickListener, OnFragmentAliveListener {

	private static final String TAG = TwitterSyncingFragment.class.getName();
	
	private ImageView imgProgressBar, imgAccount;
	private TextView txtLoading;
	private Button btnConnectOtherAccounts;

	private Twitter twitter;
	
	private boolean isAlive;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		isAlive = true;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		//Log.d(TAG, "onCreateView()");
		View v = inflater.inflate(R.layout.fragment_twitter_syncing, null);

		imgProgressBar = (ImageView) v.findViewById(R.id.progressBar);
		imgAccount = (ImageView) v.findViewById(R.id.imgAccount);
		txtLoading = (TextView) v.findViewById(R.id.txtLoading);
		btnConnectOtherAccounts = (Button) v.findViewById(R.id.btnConnectOtherAccuonts);
		
		updateVisibility();
		
		btnConnectOtherAccounts.setOnClickListener(this);
		
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (twitter == null) {
			twitter = (Twitter) getArguments().getSerializable(BundleKeys.TWITTER);
			final String oauthVerifier = getArguments().getString(BundleKeys.OAUTH_VERIFIER);
			final EventSeekr eventSeekr = (EventSeekr) FragmentUtil.getActivity(TwitterSyncingFragment.this)
					.getApplication();
			
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
			            
						final List<String> artistNames = new ArrayList<String>();
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
						
						FragmentUtil.getActivity(TwitterSyncingFragment.this).runOnUiThread(new Runnable() {
							
							@Override
							public void run() {
								new SyncArtists(artistNames, eventSeekr, 
										Service.Twitter, TwitterSyncingFragment.this).execute();
							}
						});
												
					} catch (TwitterException e) {
						e.printStackTrace();
						eventSeekr.setSyncCount(Service.Twitter, EventSeekr.UNSYNC_COUNT);
						FragmentUtil.getActivity(TwitterSyncingFragment.this).runOnUiThread(new Runnable() {
							
							@Override
							public void run() {
								FragmentUtil.getActivity(TwitterSyncingFragment.this).onBackPressed();
							}
						});
					}
				}
			}).start();
		}
	}

	private void updateVisibility() {
		AnimationUtil.startRotationToView(imgProgressBar, 0f, 360f, 0.5f, 0.5f, 1000);
		txtLoading.setText(R.string.syncing_twitter);
		imgAccount.setImageResource(R.drawable.twitter_big);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.btnConnectOtherAccuonts:
			//Log.d(TAG, "btnConnectOtherAccuonts");
			FragmentUtil.getActivity(this).onBackPressed();
			break;

		default:
			break;
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		//Log.d(TAG, "onDestroy()");
		isAlive = false;
	}

	@Override
	public boolean isAlive() {
		return isAlive;
	}

	@Override
	public String getScreenName() {
		return null;
	}
}
