package com.wcities.eventseeker;

import twitter4j.Twitter;
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
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.ReplaceFragmentListener;
import com.wcities.eventseeker.util.FragmentUtil;

public class TwitterFragment extends FragmentLoadableFromBackStack {
	
	private static final String TAG = TwitterFragment.class.getName();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
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
				if (url.contains(AppConstants.TWITTER_CALLBACK_URL) && url.contains("oauth_token")) {
					//Log.d(TAG, "url = " + url);
					ServiceAccount serviceAccount = (ServiceAccount) getArguments().getSerializable(
							BundleKeys.SERVICE_ACCOUNTS);
					serviceAccount.isInProgress = true;
					
					Uri uri = Uri.parse(url);
					final String oauthVerifier = uri.getQueryParameter("oauth_verifier");
					Bundle args = new Bundle();
					args.putString(BundleKeys.OAUTH_VERIFIER, oauthVerifier);
					args.putSerializable(BundleKeys.TWITTER, twitter);
					
					//Log.d(TAG, "Syncying : oauthVerifier : " + oauthVerifier + ", twitter : " + twitter);
					((ReplaceFragmentListener)FragmentUtil.getActivity(TwitterFragment.this))
						.replaceByFragment(AppConstants.FRAGMENT_TAG_TWITTER_SYNCING, args);
					
					return true;
				}
				
				EventSeekr eventSeekr = (EventSeekr) FragmentUtil.getActivity(TwitterFragment.this).getApplicationContext();
				eventSeekr.setSyncCount(Service.Twitter, EventSeekr.UNSYNC_COUNT);
				FragmentUtil.getActivity(TwitterFragment.this).onBackPressed();
				return false;
			}
		});
		webView.loadUrl(url);
		
		return v;
	}
}

