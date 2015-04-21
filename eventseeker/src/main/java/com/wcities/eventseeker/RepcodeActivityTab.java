package com.wcities.eventseeker;

import android.os.Bundle;
import android.view.Menu;

import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.util.FragmentUtil;

public class RepcodeActivityTab extends BaseActivityTab {
	
	private static final String TAG = RepcodeActivityTab.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Log.d(TAG, "onCreate()");
		setContentView(R.layout.activity_base_tab);
		
		setCommonUI();
		
		if (isOnCreateCalledFirstTime) {
			//Log.d(TAG, "add settings fragment tab");
			RepCodeFragmentTab repCodeFragmentTab = new RepCodeFragmentTab();
			addFragment(R.id.content_frame, repCodeFragmentTab, FragmentUtil.getTag(repCodeFragmentTab), false);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public String getScreenName() {
		return ScreenNames.REPCODE_SCREEN;
	}

	@Override
	protected String getScrnTitle() {
		return getResources().getString(R.string.title_rep_code);
	}
}
