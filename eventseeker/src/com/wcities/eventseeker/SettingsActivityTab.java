package com.wcities.eventseeker;

import android.os.Bundle;

import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.constants.Enums.SettingsItem;
import com.wcities.eventseeker.util.FragmentUtil;

public class SettingsActivityTab extends BaseActivityTab {
	
	private static final String TAG = SettingsActivityTab.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_base_tab);
		
		setCommonUI();
		
		if (isOnCreateCalledFirstTime) {
			SettingsFragmentTab settingsFragmentTab = new SettingsFragmentTab();
			Bundle bundle = getIntent().getExtras();
			if (bundle  != null && bundle.containsKey(BundleKeys.SETTINGS_ITEM)) {
				settingsFragmentTab.setArguments(bundle);
			}
			addFragment(R.id.content_frame, settingsFragmentTab, FragmentUtil.getTag(settingsFragmentTab), false);
		}
	}

	@Override
	public String getScreenName() {
		return ScreenNames.SETTINGS;
	}

	@Override
	protected String getScrnTitle() {
		return getResources().getString(R.string.title_settings_mobile_app);
	}
	
	@Override
	protected int getDrawerItemPos() {
		return INDEX_NAV_ITEM_SETTINGS;
	}
}
