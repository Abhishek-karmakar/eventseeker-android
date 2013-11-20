package com.wcities.eventseeker;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.util.FileUtil;

public class EULAFragment extends FragmentLoadableFromBackStack {

	private static final String TAG = EULAFragment.class.getName();
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_eula, null);
		WebView webView = (WebView) v.findViewById(R.id.webview);
		String data = null;
		try {
			data = new String(FileUtil.load(getResources().openRawResource(R.raw.terms)));
			
		} catch (Exception e) {
			Log.e(TAG, "onCreateView() ERROR", e);
			data = "Error: " + e.getMessage();
		}
		
		webView.loadDataWithBaseURL(null, data, "text/html", AppConstants.CHARSET_NAME, null);
		return v;
	}
}
