package com.wcities.eventseeker;

import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.wcities.eventseeker.DrawerListFragment.DrawerListFragmentListener;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi.LoginType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.LoadMyEventsCount;
import com.wcities.eventseeker.constants.BundleKeys;
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
	
	private LoadMyEventsCount loadMyEventsCount;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate()");
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
		//Log.d(TAG, "onCreateView()");
		View v = inflater.inflate(R.layout.fragment_service_enter_credentials_layout, null);

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
				
			} else {
				//Log.d(TAG, "wcitiesId = null");
				((MainActivity)FragmentUtil.getActivity(this)).onBackPressed();
			}
		}
	}

	@Override
	public String getScreenName() {
		return null;
	}
}
