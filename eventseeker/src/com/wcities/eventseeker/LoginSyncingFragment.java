package com.wcities.eventseeker;

import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.wcities.eventseeker.DrawerListFragment.DrawerListFragmentListener;
import com.wcities.eventseeker.SettingsFragment.SettingsItem;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi.LoginType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.LoadMyEventsCount;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.registration.Registration.RegistrationErrorListener;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.interfaces.OnFragmentAliveListener;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.ViewUtil.AnimationUtil;

public class LoginSyncingFragment extends FragmentLoadableFromBackStack implements OnFragmentAliveListener, 
		AsyncTaskListener<Object> {

	private static final String TAG = LoginSyncingFragment.class.getName();

	private ImageView imgProgressBar;

	private LoginType loginType;
	private boolean isAlive;
	private boolean isForSignUp; // indicates if this fragment is called after fb/g+ signup or login
	
	private LoadMyEventsCount loadMyEventsCount;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Log.d(TAG, "onCreate()");
		setRetainInstance(true);
		isAlive = true;
		
		EventSeekr eventSeekr = (EventSeekr) FragmentUtil.getActivity(this).getApplication();
		
		Bundle args = getArguments();
		loginType = (LoginType) args.getSerializable(BundleKeys.LOGIN_TYPE);
		boolean isRegistrationInitiated = false;
		
		switch (loginType) {
		
		case facebook:
			isRegistrationInitiated = eventSeekr.updateFbUserInfo(args.getString(BundleKeys.FB_USER_ID), args.getString(BundleKeys.FB_USER_NAME), 
        			args.getString(BundleKeys.FB_EMAIL_ID), this);
        	isForSignUp = args.getBoolean(BundleKeys.IS_FOR_SIGN_UP);
			break;
			
		case googlePlus:
			isRegistrationInitiated = eventSeekr.updateGPlusUserInfo(args.getString(BundleKeys.GOOGLE_PLUS_USER_ID), 
					args.getString(BundleKeys.GOOGLE_PLUS_USER_NAME), args.getString(BundleKeys.GOOGLE_PLUS_EMAIL_ID), 
					this);
			isForSignUp = args.getBoolean(BundleKeys.IS_FOR_SIGN_UP);
			break;
			
		case emailSignup:
			isRegistrationInitiated = eventSeekr.updateEmailSignupInfo(args.getString(BundleKeys.EMAIL_ID), args.getString(BundleKeys.FIRST_NAME), 
					args.getString(BundleKeys.LAST_NAME), args.getString(BundleKeys.PASSWORD), this);
			isForSignUp = true;
			break;
			
		case emailLogin:
			isRegistrationInitiated = eventSeekr.updateEmailLoginInfo(args.getString(BundleKeys.EMAIL_ID), args.getString(BundleKeys.PASSWORD), 
					this);
			isForSignUp = false;
			break;

		default:
			break;
		}
		
		if (!isRegistrationInitiated) {
			/**
			 * Delaying this task; because otherwise it throws 
			 * "java.lang.IllegalStateException: Recursive entry to executePendingTransactions" 
			 */
			new Handler().postDelayed(new Runnable() {
				
				@Override
				public void run() {
					((MainActivity)FragmentUtil.getActivity(LoginSyncingFragment.this)).onBackPressed();
				}
			}, 1000);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		//Log.d(TAG, "onCreateView()");
		View v = inflater.inflate(R.layout.fragment_service_enter_credentials_layout, null);

		v.findViewById(R.id.rltMainView).setVisibility(View.GONE);
		v.findViewById(R.id.rltSyncAccount).setVisibility(View.VISIBLE);

		imgProgressBar = (ImageView) v.findViewById(R.id.progressBar);
		if (loginType == LoginType.facebook) {
			((ImageView) v.findViewById(R.id.imgAccount)).setImageResource(R.drawable.facebook_colored_big);
			((TextView)v.findViewById(R.id.txtLoading)).setText(R.string.syncing_fb);
			
		} else if (loginType == LoginType.googlePlus) {
			// It's for google plus
			((ImageView) v.findViewById(R.id.imgAccount)).setImageResource(R.drawable.g_plus_colored_big);
			((TextView)v.findViewById(R.id.txtLoading)).setText(R.string.syncing_google_plus);
			
		} else {
			((ImageView) v.findViewById(R.id.imgAccount)).setImageResource(R.drawable.ic_launcher_d);
			((TextView)v.findViewById(R.id.txtLoading)).setText(R.string.syncing_email);
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
		//Log.d(TAG, "onDestroyView()");
		AnimationUtil.stopRotationToView(imgProgressBar);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		//Log.d(TAG, "onDestroy()");
		isAlive = false;
		
		if (loadMyEventsCount != null && loadMyEventsCount.getStatus() != Status.FINISHED) {
			loadMyEventsCount.cancel(true);
		}
	}
	
	@Override
	public void onTaskCompleted(Object... params) {
		//Log.d(TAG, "onTaskCompleted");
		if (isAlive()) {
			//Log.d(TAG, "isAlive");
			String wcitiesId = ((EventSeekr)FragmentUtil.getActivity(this).getApplication()).getWcitiesId();
			
			if (wcitiesId != null) {
				//Log.d(TAG, "wcitiesId != null");
				double[] latLon = DeviceUtil.getLatLon(FragmentUtil.getApplication(this));

				if (isForSignUp) {
					Bundle args = new Bundle();
					args.putSerializable(BundleKeys.SETTINGS_ITEM, SettingsItem.SYNC_ACCOUNTS);
					((DrawerListFragmentListener) FragmentUtil.getActivity(this)).onDrawerItemSelected(
							MainActivity.INDEX_NAV_ITEM_SETTINGS, args);
					
				} else {
					loadMyEventsCount = new LoadMyEventsCount(Api.OAUTH_TOKEN, wcitiesId, latLon[0], latLon[1], new AsyncTaskListener<Integer>() {
						
						@Override
						public void onTaskCompleted(Integer... params) {
							Log.d(TAG, "params[0] = " + params[0]);
							if (params[0] > 0) {
								((DrawerListFragmentListener)FragmentUtil.getActivity(LoginSyncingFragment.this)).onDrawerItemSelected(
										MainActivity.INDEX_NAV_ITEM_MY_EVENTS, null);
								
							} else {
								((DrawerListFragmentListener)FragmentUtil.getActivity(LoginSyncingFragment.this)).onDrawerItemSelected(
										MainActivity.INDEX_NAV_ITEM_DISCOVER, null);
							}
						}
					});
					loadMyEventsCount.execute();
				}
				
			} else {
				//Log.d(TAG, "wcitiesId = null");
				//Log.d(TAG, "message code = " + params[0].toString());
				((MainActivity)FragmentUtil.getActivity(this)).onBackPressed();
				RegistrationErrorListener registrationErrorListener = (RegistrationErrorListener) 
						((ActionBarActivity) FragmentUtil.getActivity(this)).getSupportFragmentManager()
						.findFragmentByTag(getArguments().getString(BundleKeys.REGISTER_ERROR_LISTENER));
				registrationErrorListener.onErrorOccured((Integer)params[0]);
			}
		}
	}

	@Override
	public String getScreenName() {
		return null;
	}
}
