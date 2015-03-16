package com.wcities.eventseeker;

import android.os.Bundle;
import android.view.KeyEvent;

import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.util.FragmentUtil;

public class WebViewActivityTab extends BaseActivityTab {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_base_tab);
		
		setCommonUI();
		
		if (isOnCreateCalledFirstTime) {
			//Log.d(TAG, "add login fragment tab");
			WebViewFragmentTab webViewFragmentTab = new WebViewFragmentTab();
			webViewFragmentTab.setArguments(getIntent().getExtras());
			addFragment(R.id.content_frame, webViewFragmentTab, FragmentUtil.getTag(webViewFragmentTab), false);
		}
	}
	
	@Override
	public String getScreenName() {
		return ScreenNames.WEB_VIEW;
	}

	@Override
	protected String getScrnTitle() {
		return getResources().getString(R.string.title_web);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (isDrawerOpen()) {
				return super.onKeyDown(keyCode, event);
				
			} else {
				WebViewFragmentTab webViewFragmentTab = (WebViewFragmentTab) getSupportFragmentManager()
						.findFragmentByTag(FragmentUtil.getSupportTag(WebViewFragmentTab.class));
				if (webViewFragmentTab.onKeyDown()) {
					return true;
					
				} else {
					return super.onKeyDown(keyCode, event);
				}
			}
		}
		return super.onKeyDown(keyCode, event);
	}
}
