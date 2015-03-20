package com.wcities.eventseeker;

import android.os.Bundle;

import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.util.FragmentUtil;

public class LanguageActivityTab extends BaseActivityTab {
	
	private static final String TAG = LanguageActivityTab.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Log.d(TAG, "onCreate()");
		setContentView(R.layout.activity_base_tab);
		
		setCommonUI();
		
		if (isOnCreateCalledFirstTime) {
			//Log.d(TAG, "add settings fragment tab");
			LanguageFragmentTab languageFragmentTab = new LanguageFragmentTab();
			addFragment(R.id.content_frame, languageFragmentTab, FragmentUtil.getTag(languageFragmentTab), false);
		}
	}

	@Override
	public String getScreenName() {
		return ScreenNames.LANGUAGE_SCREEN;
	}

	@Override
	protected String getScrnTitle() {
		return getResources().getString(R.string.title_language);
	}
}
