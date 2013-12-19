package com.wcities.eventseeker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.wcities.eventseeker.ConnectAccountsFragment.ServiceAccount;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.interfaces.OnFragmentAliveListener;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.ViewUtil.AnimationUtil;

public class FacebookFragment extends FragmentLoadableFromBackStack implements OnFragmentAliveListener, AsyncTaskListener<Void> {

	private static final String TAG = FacebookFragment.class.getName();

	private ImageView imgProgressBar;

	private ServiceAccount serviceAccount;

	private boolean isAlive;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		isAlive = true;
		((EventSeekr) (FragmentUtil.getActivity(FacebookFragment.this))
				.getApplicationContext()).updateFbUserId(getArguments().getString(BundleKeys.WCITIES_ID), this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate( R.layout.fragment_service_enter_credentials_layout, null);

		v.findViewById(R.id.rltMainView).setVisibility(View.GONE);
		v.findViewById(R.id.rltSyncAccount).setVisibility(View.VISIBLE);

		imgProgressBar = (ImageView) v.findViewById(R.id.progressBar);
		((ImageView) v.findViewById(R.id.imgAccount)).setImageResource(R.drawable.facebook_colored_big);
		
		((TextView)v.findViewById(R.id.txtLoading)).setText("Syncing Facebook");
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
	public void onTaskCompleted(Void... params) {
		if (isAlive()) {
			FragmentUtil.getActivity(this).onBackPressed();
		}
	}
	
}
