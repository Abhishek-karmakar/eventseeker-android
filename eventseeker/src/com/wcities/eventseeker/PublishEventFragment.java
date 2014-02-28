package com.wcities.eventseeker;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.facebook.Session;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.plus.PlusClient;
import com.wcities.eventseeker.DrawerListFragment.DrawerListFragmentListener;
import com.wcities.eventseeker.GeneralDialogFragment.DialogBtnClickListener;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingItemType;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.UserTracker;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.interfaces.PublishListener;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.GPlusUtil;

public abstract class PublishEventFragment extends Fragment implements PublishListener, 
		ConnectionCallbacks, OnConnectionFailedListener, DialogBtnClickListener {
	
	private static final String TAG = PublishEventFragment.class.getSimpleName();

	protected Event event;
	// Flag to represent if we are waiting for extended permissions
	private boolean pendingAnnounce = false;
	
	protected PlusClient mPlusClient;
	protected ConnectionResult mConnectionResult;

	private boolean isPublishPermissionDisplayed;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
    	mPlusClient = GPlusUtil.createPlusClientInstance(this, this, this);
	}
	
	@Override
	public void onStart() {
		Log.d(TAG, "onStart()");
		Session session = Session.getActiveSession();
		if (session != null) {
			session.addCallback(this);
		}
		super.onStart();
	}
	
	@Override
	public void onStop() {
		Log.d(TAG, "onStop()");
		Session session = Session.getActiveSession();
		if (session != null) {
			session.removeCallback(this);
		}
		super.onStop();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mPlusClient.isConnected()) {
			mPlusClient.disconnect();
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult(), requestCode = " + requestCode);
		if (pendingAnnounce) {
			if (requestCode == AppConstants.REQ_CODE_GOOGLE_PLUS_RESOLVE_ERR || 
	        		requestCode == AppConstants.REQ_CODE_GET_GOOGLE_PLAY_SERVICES) {
	        	if (resultCode == Activity.RESULT_OK  && !mPlusClient.isConnected()
	                    && !mPlusClient.isConnecting()) {
		            connectPlusClient();
	        	}
	            
	        } else if (GPlusUtil.isGPlusPublishPending) {
	        	GPlusUtil.isGPlusPublishPending = false;
	        	if (resultCode == Activity.RESULT_OK) {
	        		event.updateAttendingToNewAttending();
	    			onPublishPermissionGranted();
	        		trackEvent();
	        	}
	        	
	        } else {
	        	//Log.d(TAG, "handle fb");
	    		// don't compare request code here, since it normally returns 64206 (hardcoded value) for openActiveSession() request
				Session session = Session.getActiveSession();
		        if (session != null) {
		        	//Log.d(TAG, "session!=null");
		            session.onActivityResult(FragmentUtil.getActivity(this), requestCode, resultCode, data);
		        }
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
	
	protected void trackEvent() {
		new UserTracker((EventSeekr) FragmentUtil.getActivity(this).getApplication(), 
        		UserTrackingItemType.event, event.getId(), event.getAttending().getValue(), null, 
        		UserTrackingType.Add).execute();
	}
	
	protected void handlePublishEvent() {
		//Log.d(TAG, "handlePublish()");
		int available = GooglePlayServicesUtil.isGooglePlayServicesAvailable(FragmentUtil.getActivity(this));
		if (available != ConnectionResult.SUCCESS) {
			GPlusUtil.showDialogForGPlayServiceUnavailability(available, this);
            return;
        }
		
		if (mPlusClient.isConnected()) {
			GPlusUtil.publishEvent(event, this);
			
		} else {
			pendingAnnounce = true;
			
			if (mConnectionResult != null) {
	        	//Log.d(TAG, "mConnectionResult is not null");
	            try {
	            	mConnectionResult.startResolutionForResult(FragmentUtil.getActivity(this), 
	            			AppConstants.REQ_CODE_GOOGLE_PLUS_RESOLVE_ERR);
	                
	            } catch (SendIntentException e) {
	                // Try connecting again.
	                connectPlusClient();
	            }
	            
	        } else {
	        	connectPlusClient();
	        }
		}
	}
	
	private void connectPlusClient() {
    	//Log.d(TAG, "connectPlusClient()");
    	if (!mPlusClient.isConnected() && !mPlusClient.isConnecting()) {
    		//Log.d(TAG, "try connecting");
    		mConnectionResult = null;
    		mPlusClient.connect();
    	}
    }
	
	@Override
	public void onDisconnected() {
		//Log.d(TAG, "onDisconnected()");
		pendingAnnounce = false;
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		//Log.d(TAG, "onConnectionFailed()");
		// Save the result and resolve the connection failure upon a user click.
		mConnectionResult = result;
		if (mConnectionResult.hasResolution()) {
            try {
				mConnectionResult.startResolutionForResult(FragmentUtil.getActivity(this), AppConstants.REQ_CODE_GOOGLE_PLUS_RESOLVE_ERR);
				
			} catch (SendIntentException e) {
				e.printStackTrace();
				// Try connecting again.
                connectPlusClient();
			}
            
		} else {
			pendingAnnounce = false;
		}
	}
	
	@Override
	public void onConnected(Bundle arg0) {
		if (pendingAnnounce) {
			GPlusUtil.publishEvent(event, this);
		}
	}
	
	@Override
	public void doPositiveClick(String dialogTag) {
		if (dialogTag.equals(AppConstants.DIALOG_FRAGMENT_TAG_LOGIN_TO_TRACK_EVENT)) {
			((DrawerListFragmentListener)FragmentUtil.getActivity(this)).onDrawerItemSelected(
					MainActivity.INDEX_NAV_ITEM_CONNECT_ACCOUNTS);
		}
	}
	
	@Override
	public void doNegativeClick(String dialogTag) {
		if (dialogTag.equals(AppConstants.DIALOG_FRAGMENT_TAG_LOGIN_TO_TRACK_EVENT)) {
			DialogFragment dialogFragment = (DialogFragment) getChildFragmentManager().findFragmentByTag(
					AppConstants.DIALOG_FRAGMENT_TAG_LOGIN_TO_TRACK_EVENT);
			if (dialogFragment != null) {
				dialogFragment.dismiss();
			}
		}
	}
	
	public boolean isPermissionDisplayed() {
		return isPublishPermissionDisplayed;
	}
	
	public void setPermissionDisplayed(boolean isPublishPermissionDisplayed) {
		this.isPublishPermissionDisplayed = isPublishPermissionDisplayed;
	}
	
}
