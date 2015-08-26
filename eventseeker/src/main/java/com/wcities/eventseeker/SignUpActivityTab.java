package com.wcities.eventseeker;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import com.wcities.eventseeker.api.UserInfoApi.LoginType;
import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.core.registration.Registration.RegistrationListener;
import com.wcities.eventseeker.util.FragmentUtil;

public class SignUpActivityTab extends BaseActivityTab implements RegistrationListener {

	private static final String TAG = SignUpActivityTab.class.getSimpleName();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Log.d(TAG, "onCreate()");
		setContentView(R.layout.activity_base_tab);
		
		setCommonUI();
		
		if (isOnCreateCalledFirstTime) {
			//Log.d(TAG, "add login fragment tab");
			SignUpFragmentTab signUpFragmentTab = new SignUpFragmentTab();
			addFragment(R.id.content_frame, signUpFragmentTab, FragmentUtil.getTag(signUpFragmentTab), false);
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		setDrawerLockMode(true);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case android.R.id.home:
			if (getSupportFragmentManager().findFragmentByTag(FragmentUtil.getTag(LoginSyncingFragmentTab.class)) 
					!= null) {
				return true;
				
			} else {
				super.onOptionsItemSelected(item);
			}
			return true;
			
		default:
			break;
		}

		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// Log.d(TAG, "onKeyDown()");
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (getSupportFragmentManager().findFragmentByTag(FragmentUtil.getTag(LoginSyncingFragmentTab.class)) 
					!= null) {
				return true;
				
			} else {
				return super.onKeyDown(keyCode, event);
			}
		}
		return super.onKeyDown(keyCode, event);
	}
	
	@Override
	public String getScreenName() {
		return ScreenNames.ACCOUNT_SIGN_UP;
	}

	@Override
	protected String getScrnTitle() {
		return getResources().getString(R.string.title_signup);
	}

	@Override
	public void onRegistration(LoginType loginType, Bundle args, boolean addToBackStack) {
		LoginSyncingFragmentTab loginSyncingFragmentTab = new LoginSyncingFragmentTab();
		loginSyncingFragmentTab.setArguments(args);
		addFragment(R.id.content_frame, loginSyncingFragmentTab, FragmentUtil.getTag(loginSyncingFragmentTab), 
				addToBackStack);
	}
}
