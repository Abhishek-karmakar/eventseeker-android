package com.wcities.eventseeker;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;

import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterAuthToken;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterAuthClient;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.constants.Enums.Service;
import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.ReplaceFragmentListener;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.ServiceAccount;

import io.fabric.sdk.android.Fabric;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterFragment extends FragmentLoadableFromBackStack {
	
	private static final String TAG = TwitterFragment.class.getSimpleName();
	
	private Twitter twitter;
	private String url;
	private Bundle webViewBundle;

	private WebView webView;
	private RelativeLayout rltProgressBar;

    private boolean isTwitterAppFound = true;
    private TwitterAuthClient mTwitterAuthClient;

    private Handler handler;
    private int lastRequestCode, lastResultCode;
    private Intent lastData;
    private boolean isOnActivityResultCalled;

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
					args.putString(BundleKeys.SYNC_ARTIST_LISTENER, 
							getArguments().getString(BundleKeys.SYNC_ARTIST_LISTENER));
					
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
			rltProgressBar.setVisibility(View.VISIBLE);
		}
		
		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);
			rltProgressBar.setVisibility(View.INVISIBLE);
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);

        handler = new Handler(Looper.getMainLooper());

        TwitterAuthConfig authConfig = new TwitterAuthConfig(AppConstants.TWITTER_CONSUMER_KEY,
                AppConstants.TWITTER_CONSUMER_SECRET);
        Fabric.with(FragmentUtil.getActivity(this), new com.twitter.sdk.android.Twitter(authConfig));

        mTwitterAuthClient = new TwitterAuthClient();
        try {
            //Log.d(TAG, "authorize()");
            mTwitterAuthClient.authorize(FragmentUtil.getActivity(this), new Callback<TwitterSession>() {

                @Override
                public void success(final Result<TwitterSession> twitterSessionResult) {
                    // Success
                    //Log.d(TAG, "success()");
                    ServiceAccount serviceAccount = (ServiceAccount) getArguments().getSerializable(
                            BundleKeys.SERVICE_ACCOUNTS);
                    serviceAccount.isInProgress = true;

                    TwitterAuthToken authToken = twitterSessionResult.data.getAuthToken();
                    Bundle args = new Bundle();
                    args.putParcelable(BundleKeys.AUTH_TOKEN, authToken);
                    args.putString(BundleKeys.SYNC_ARTIST_LISTENER,
                            getArguments().getString(BundleKeys.SYNC_ARTIST_LISTENER));

                    ((ReplaceFragmentListener)FragmentUtil.getActivity(TwitterFragment.this))
                            .replaceByFragment(AppConstants.FRAGMENT_TAG_TWITTER_SYNCING, args);
                }

                @Override
                public void failure(com.twitter.sdk.android.core.TwitterException e) {
                    //Log.d(TAG, "failure()");
                    e.printStackTrace();
                    FragmentUtil.getActivity(TwitterFragment.this).onBackPressed();
                }
            });

        } catch (ActivityNotFoundException e) {
            Log.i(TAG, "Twitter app not found");

            isTwitterAppFound = false;

            ConfigurationBuilder builder = new ConfigurationBuilder();
            builder.setOAuthConsumerKey(AppConstants.TWITTER_CONSUMER_KEY);
            builder.setOAuthConsumerSecret(AppConstants.TWITTER_CONSUMER_SECRET);
            twitter4j.conf.Configuration configuration = builder.build();

            TwitterFactory factory = new TwitterFactory(configuration);
            twitter = factory.getInstance();
        }

        if (!isTwitterAppFound) {
            new LoadRequestToken().execute();
        }
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_twitter_login, null);
		webView = (WebView) v.findViewById(R.id.webView);
		rltProgressBar = (RelativeLayout) v.findViewById(R.id.rltProgressBar);
		
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
		
		if (!isTwitterAppFound && webViewBundle != null) {
			webView.restoreState(webViewBundle);
		}
		
		return v;
	}

    @Override
    public void onResume() {
        super.onResume();
        if (isOnActivityResultCalled) {
            mTwitterAuthClient.onActivityResult(lastRequestCode, lastResultCode, lastData);
            isOnActivityResultCalled = false;
        }
    }

    @Override
	public void onSaveInstanceState(Bundle outState) {
        //Log.d(TAG, "onSaveInstanceState()");
		super.onSaveInstanceState(outState);
		if (webView != null) {
			webViewBundle = new Bundle();
			webView.saveState(webViewBundle);
		}
	}

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult() - requestCode = " + requestCode);
        /**
         * Instead of calling onActivityResult() on mTwitterAuthClient from here, we are calling from onResume()
         * to ensure that onResume() is called before failure()/success() callback methods' execution which in turn
         * are using onBackPressed() internally for which activity must be visible; otherwise it throws
         * IllegalStateException: Cannot perform this action after onSaveInstanceState().
         * Although we can use handlers to call onBackPressed(), in rare cases onResume() is called after
         * handler's task execution. As a better solution this trick of ensuring first onResume() execution & then only
         * updating mTwitterAuthClient would help us.
         */
        lastRequestCode = requestCode;
        lastResultCode = resultCode;
        lastData = data;
        isOnActivityResultCalled = true;
    }

	@Override
	public String getScreenName() {
		return ScreenNames.TWITTER_SYNC;
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

