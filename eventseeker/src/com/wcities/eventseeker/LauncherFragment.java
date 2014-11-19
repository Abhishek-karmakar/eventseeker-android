package com.wcities.eventseeker;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.util.FragmentUtil;

public class LauncherFragment extends FragmentLoadableFromBackStack implements OnClickListener {

	private static final String TAG = LauncherFragment.class.getSimpleName();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		ma.setDrawerLockMode(true);
		ma.setDrawerIndicatorEnabled(false);
		ma.getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_HOME);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_launcher, null);
		
		view.findViewById(R.id.btnLogin).setOnClickListener(this);
		view.findViewById(R.id.btnSignUp).setOnClickListener(this);
		
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		//Log.d(TAG, "onResume");
		((ActionBarActivity) FragmentUtil.getActivity(this)).getSupportActionBar().hide();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		//Log.d(TAG, "onPause");
		((ActionBarActivity) FragmentUtil.getActivity(this)).getSupportActionBar().show();
	}
	
	@Override
	public void onClick(View v) {
		
		switch (v.getId()) {
		
		case R.id.btnLogin:
			((MainActivity) FragmentUtil.getActivity(this)).replaceByFragment(AppConstants.FRAGMENT_TAG_LOGIN, null);
			break;

		case R.id.btnSignUp:
			((MainActivity) FragmentUtil.getActivity(this)).replaceByFragment(AppConstants.FRAGMENT_TAG_SIGN_UP, null);			
			break;

		default:
			break;
		}
	}

	@Override
	public String getScreenName() {
		return "First Start Screen";
	}
}
