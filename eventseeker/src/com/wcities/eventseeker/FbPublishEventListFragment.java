package com.wcities.eventseeker;

import android.content.Intent;
import android.support.v4.app.ListFragment;
import android.util.Log;

import com.facebook.Session;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.interfaces.FbPublishListener;
import com.wcities.eventseeker.util.FragmentUtil;

public abstract class FbPublishEventListFragment extends ListFragment implements FbPublishListener {
	
	private static final String TAG = FbPublishEventListFragment.class.getSimpleName();
	
	// Flag to represent if we are waiting for extended permissions
	private boolean pendingAnnounce = false;
	
	@Override
	public void onStart() {
		Session session = Session.getActiveSession();
		if (session != null) {
			session.addCallback(this);
		}
		super.onStart();
	}
	
	@Override
	public void onStop() {
		Session session = Session.getActiveSession();
		if (session != null) {
			session.removeCallback(this);
		}
		super.onStop();
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult(), requestCode = " + requestCode);
		// don't compare request code here, since it normally returns 64206 (hardcoded value) for openActiveSession() request
		if (pendingAnnounce) {
			Session session = Session.getActiveSession();
	        if (session != null) {
	        	Log.d(TAG, "session!=null");
	            session.onActivityResult(FragmentUtil.getActivity(this), requestCode, resultCode, data);
	        }
		}
	}

	@Override
	public void setPendingAnnounce(boolean pendingAnnounce) {
		this.pendingAnnounce = pendingAnnounce;		
	}

	@Override
	public boolean isPendingAnnounce() {
		return pendingAnnounce;
	}
}
