package com.wcities.eventseeker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;

public class TicketProvidersFragment extends FragmentLoadableFromBackStack {

	private WebView webView;
	private String url;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		
		url = getArguments().getString(BundleKeys.URL);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_ticket_providers, null);
		webView = (WebView) v.findViewById(R.id.webview);
		webView.setWebViewClient(new WebViewClient());
		webView.loadUrl(url);
		return v;
	}
	
	public boolean onKeyDown() {
		if (webView.canGoBack()) {
			webView.goBack();
			return true;
		}
		return false;
	}
}
