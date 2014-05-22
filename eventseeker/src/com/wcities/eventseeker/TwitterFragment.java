package com.wcities.eventseeker;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.wcities.eventseeker.ConnectAccountsFragment.Service;
import com.wcities.eventseeker.ConnectAccountsFragment.ServiceAccount;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.ReplaceFragmentListener;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.LogUtil;

public class TwitterFragment extends FragmentLoadableFromBackStack {
	
	private static final String TAG = TwitterFragment.class.getSimpleName();
	
	private Twitter twitter;
	private String url;
	private Bundle webViewBundle;

	private WebView webView;
	private ProgressBar progressBar;
	
	private WebViewClient webViewClient = new WebViewClient() {
		
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			//Log.d(TAG, "twitter url1 = " + url);
			if (url.contains(AppConstants.TWITTER_CALLBACK_URL)) {
				if (url.contains("oauth_token")) {
					//Log.d(TAG, "twitter url2 = " + url);
					ServiceAccount serviceAccount = (ServiceAccount) getArguments().getSerializable(
							BundleKeys.SERVICE_ACCOUNTS);
					serviceAccount.isInProgress = true;
					
					Uri uri = Uri.parse(url);
					String oauthVerifier = uri.getQueryParameter("oauth_verifier");
					Bundle args = new Bundle();
					args.putString(BundleKeys.OAUTH_VERIFIER, oauthVerifier);
					args.putSerializable(BundleKeys.TWITTER, twitter);
					
					//Log.d(TAG, "twitter Syncying : oauthVerifier : " + oauthVerifier + ", twitter : " + twitter);
					((ReplaceFragmentListener)FragmentUtil.getActivity(TwitterFragment.this))
						.replaceByFragment(AppConstants.FRAGMENT_TAG_TWITTER_SYNCING, args);
					
					return true;
					
				} else {
					//Log.d(TAG, "twitter else");
					EventSeekr eventSeekr = (EventSeekr) FragmentUtil.getActivity(TwitterFragment.this).getApplicationContext();
					eventSeekr.setSyncCount(Service.Twitter, EventSeekr.UNSYNC_COUNT);
					FragmentUtil.getActivity(TwitterFragment.this).onBackPressed();
					return false;
				}
				
			} else {
				/**
				 * when twitter sign in page appears in production build, url contains only oauth_token 
				 * & not the AppConstants.TWITTER_CALLBACK_URL; whereas for development build we don't get 
				 * this call back for sign in page.
				 */
				//Log.d(TAG, "twitter last else");
				EventSeekr eventSeekr = (EventSeekr) FragmentUtil.getActivity(TwitterFragment.this).getApplicationContext();
				eventSeekr.setSyncCount(Service.Twitter, EventSeekr.UNSYNC_COUNT);
				return false;
			}
		}
		
		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			super.onPageStarted(view, url, favicon);
			progressBar.setVisibility(View.VISIBLE);
		}
		
		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);
			progressBar.setVisibility(View.INVISIBLE);
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		
		ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setOAuthConsumerKey(AppConstants.TWITTER_CONSUMER_KEY);
        builder.setOAuthConsumerSecret(AppConstants.TWITTER_CONSUMER_SECRET);
        twitter4j.conf.Configuration configuration = builder.build();
        
        TwitterFactory factory = new TwitterFactory(configuration);
        twitter = factory.getInstance();
        
        new LoadRequestToken().execute();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_twitter_login, null);
		webView = (WebView) v.findViewById(R.id.webView);
		progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
		
		webView.setWebViewClient(webViewClient);
		webView.getSettings().setUseWideViewPort(true);
		
		/**
		 * The below onTouch listener has been added as in Samsung Galaxy S(Android 2.3.3) device,
		 * the Soft Keyboard wasn't appearing when pressed on EditText inside WebView 
		 */
		webView.setOnTouchListener(new View.OnTouchListener() {
		    public boolean onTouch(View v, MotionEvent event) {
		        switch (event.getAction()) {
		            case MotionEvent.ACTION_DOWN:
		            case MotionEvent.ACTION_UP:
		                if (!v.hasFocus()) {
		                    v.requestFocus();
		                }
		                break;
		        }
		        return false;
		    }
		});  
		
		if (webViewBundle != null) {
			webView.restoreState(webViewBundle);
		}
		
		return v;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (webView != null) {
			webViewBundle = new Bundle();
			webView.saveState(webViewBundle);
		}
	}

	@Override
	public String getScreenName() {
		return "Twitter Sync Screen";
	}
	
	private class LoadRequestToken extends AsyncTask<Void, Void, Void> {
		
		@Override
		protected Void doInBackground(Void... params) {
			try {
				RequestToken requestToken = twitter.getOAuthRequestToken(AppConstants.TWITTER_CALLBACK_URL);
				url = requestToken.getAuthenticationURL();
				
			} catch (TwitterException e) {
				e.printStackTrace();
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			webView.loadUrl(url);
		}
	}
}

