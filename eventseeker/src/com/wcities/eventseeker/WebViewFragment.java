package com.wcities.eventseeker;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;

public class WebViewFragment extends FragmentLoadableFromBackStack {

	private static final String TAG = WebViewFragment.class.getName();
	
	private WebView webView;
	private String url;
	private Bundle webViewBundle;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		
		url = getArguments().getString(BundleKeys.URL);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_web_view, null);
		webView = (WebView) v.findViewById(R.id.webview);
		final ProgressBar progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
		
		webView.setWebViewClient(new WebViewClient() {
			
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
		});
		
		webView.getSettings().setSupportZoom(true) ; 
		webView.getSettings().setBuiltInZoomControls(true);
		webView.getSettings().setUseWideViewPort(true) ; 
		webView.setInitialScale(1) ;

		if (webViewBundle == null) {
			webView.loadUrl(url);
			
		} else {
			//Log.d(TAG, "restore state");
			webView.restoreState(webViewBundle);
		}
		return v;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		//Log.d(TAG, "onSaveInstanceState()");
		if (webView != null) {
			webViewBundle = new Bundle();
			webView.saveState(webViewBundle);
		}
	}
	
	public boolean onKeyDown() {
		if (webView.canGoBack()) {
			webView.goBack();
			return true;
		}
		return false;
	}
}
