package com.wcities.eventseeker;

import android.content.Intent;
import android.os.Bundle;

import com.wcities.eventseeker.constants.ScreenNames;

public class LoginActivityTab extends BaseActivityTab {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Log.d(TAG, "onCreate()");
		setContentView(R.layout.activity_base_tab);
		
		setCommonUI();
	}

	@Override
	public String getScreenName() {
		return ScreenNames.ACCOUNT_LOGIN;
	}

	@Override
	protected String getScrnTitle() {
		return getResources().getString(R.string.title_login);
	}
}
