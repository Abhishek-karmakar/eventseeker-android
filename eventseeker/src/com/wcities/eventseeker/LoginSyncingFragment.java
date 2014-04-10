package com.wcities.eventseeker;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.wcities.eventseeker.api.UserInfoApi.LoginType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.interfaces.OnFragmentAliveListener;
import com.wcities.eventseeker.interfaces.ReplaceFragmentListener;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.ViewUtil.AnimationUtil;

public class LoginSyncingFragment extends FragmentLoadableFromBackStack implements OnFragmentAliveListener, 
		AsyncTaskListener<Object> {

	private static final String TAG = LoginSyncingFragment.class.getName();

	private ImageView imgProgressBar;

	private LoginType loginType;
	private boolean isAlive;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		isAlive = true;
		
		EventSeekr eventSeekr = (EventSeekr) FragmentUtil.getActivity(this).getApplication();
		
		Bundle args = getArguments();
		loginType = (LoginType) args.getSerializable(BundleKeys.LOGIN_TYPE);
		
		if (loginType == LoginType.facebook) {
        	eventSeekr.updateFbUserInfo(args.getString(BundleKeys.FB_USER_ID), args.getString(BundleKeys.FB_USER_NAME), 
        			args.getString(BundleKeys.FB_EMAIL_ID), this);
			
		} else {
			eventSeekr.updateGPlusUserInfo(args.getString(BundleKeys.GOOGLE_PLUS_USER_ID), 
					args.getString(BundleKeys.GOOGLE_PLUS_USER_NAME), args.getString(BundleKeys.GOOGLE_PLUS_EMAIL_ID), 
					this);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate( R.layout.fragment_service_enter_credentials_layout, null);

		v.findViewById(R.id.rltMainView).setVisibility(View.GONE);
		v.findViewById(R.id.rltSyncAccount).setVisibility(View.VISIBLE);

		imgProgressBar = (ImageView) v.findViewById(R.id.progressBar);
		if (loginType == LoginType.facebook) {
			((ImageView) v.findViewById(R.id.imgAccount)).setImageResource(R.drawable.facebook_colored_big);
			((TextView)v.findViewById(R.id.txtLoading)).setText(R.string.syncing_fb);
			
		} else {
			// It's for google plus
			((ImageView) v.findViewById(R.id.imgAccount)).setImageResource(R.drawable.g_plus_colored_big);
			((TextView)v.findViewById(R.id.txtLoading)).setText(R.string.syncing_google_plus);
		}
		
		v.findViewById(R.id.btnConnectOtherAccuonts).setVisibility(View.GONE);

		AnimationUtil.startRotationToView(imgProgressBar, 0f, 360f, 0.5f, 0.5f, 1000);
		
		return v;
	}

	@Override
	public boolean isAlive() {
		return isAlive;
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		AnimationUtil.stopRotationToView(imgProgressBar);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		isAlive = false;
	}
	
	@Override
	public void onTaskCompleted(Object... params) {
		//Log.d(TAG, "onTaskCompleted");
		if (isAlive()) {
			if (((ActionBarActivity)FragmentUtil.getActivity(this)).getSupportFragmentManager()
					.getBackStackEntryCount() > 0) {
				// if user has landed on this screen from Connect Accounts screen (ConnectAccountsFragment)
				FragmentUtil.getActivity(this).onBackPressed();
				
			} else {
				// if user has landed on this screen from FbLoginFragment
				((ReplaceFragmentListener)FragmentUtil.getActivity(LoginSyncingFragment.this)).replaceByFragment(
						AppConstants.FRAGMENT_TAG_CONNECT_ACCOUNTS, null);
			}
		}
	}

	@Override
	public String getScreenName() {
		return null;
	}
}
