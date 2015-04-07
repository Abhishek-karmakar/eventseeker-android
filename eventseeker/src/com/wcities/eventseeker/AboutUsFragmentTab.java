package com.wcities.eventseeker;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.util.FragmentUtil;

public class AboutUsFragmentTab extends Fragment implements OnClickListener {

	private String version;
	private Resources res;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		try {
			PackageManager manager = FragmentUtil.getActivity(this).getPackageManager();
			PackageInfo info = manager.getPackageInfo(FragmentUtil.getActivity(this).getPackageName(), 0);
			version = info.versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		res = getResources();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_about_us_tab, null);
		view.findViewById(R.id.imgFacebook).setOnClickListener(this);
		view.findViewById(R.id.imgInstgram).setOnClickListener(this);
		view.findViewById(R.id.imgTwitter).setOnClickListener(this);
		view.findViewById(R.id.imgBlog).setOnClickListener(this);
		view.findViewById(R.id.imgWeb).setOnClickListener(this);
		((TextView) view.findViewById(R.id.txtVersion)).setText(res.getString(R.string.version) + " " + version);
		return view;
	}

	private void openURL(String url) {
		Intent intent = new Intent(FragmentUtil.getApplication(this), WebViewActivityTab.class);
		intent.putExtra(BundleKeys.URL, url);
		startActivity(intent);
	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {
		
			case R.id.imgFacebook:
				openURL("https://www.facebook.com/eventseekr");
				break;
			case R.id.imgInstgram:
				openURL("http://www.instagram.com/eventseeker");
				break;
			case R.id.imgTwitter:
				openURL("http://www.twitter.com/eventseeker");
				break;
			case R.id.imgBlog:
				openURL("http://blog.eventseeker.com");
				break;
			case R.id.imgWeb:
				openURL("http://www.eventseeker.com");
				break;

		}
	}
}
