package com.wcities.eventseeker.custom.fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.facebook.CallbackManager;
import com.facebook.login.LoginManager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.wcities.eventseeker.api.Api;
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
		ConnectionCallbacks, OnConnectionFailedListener {
	
	private static final String TAG = PublishEventFragment.class.getSimpleName();

	protected Event event;
	// Flag to represent if we are waiting for extended permissions
	private boolean pendingAnnounce = false;
	
	protected GoogleApiClient mGoogleApiClient;
	protected ConnectionResult mConnectionResult;

	protected boolean callOnlySuperOnStart;

	private CallbackManager callbackManager;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
    	mGoogleApiClient = GPlusUtil.createPlusClientInstance(this, this, this);

		callbackManager = CallbackManager.Factory.create();
		LoginManager.getInstance().registerCallback(callbackManager, this);
	}
	
	@Override
	public void onStart() {
		//Log.d(TAG, "onStart()");
		if (callOnlySuperOnStart) {
			callOnlySuperOnStart = false;
		}
		super.onStart();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mGoogleApiClient.isConnected()) {
			mGoogleApiClient.disconnect();
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult(), requestCode = " + requestCode);
		boolean processed = false;
		if (pendingAnnounce) {
			if (requestCode == AppConstants.REQ_CODE_GOOGLE_PLUS_RESOLVE_ERR || 
	        		requestCode == AppConstants.REQ_CODE_GET_GOOGLE_PLAY_SERVICES) {
	        	if (resultCode == Activity.RESULT_OK  && !mGoogleApiClient.isConnected()
	                    && !mGoogleApiClient.isConnecting()) {
		            connectPlusClient();
	        	}
				processed = true;
	            
	        } else if (GPlusUtil.isGPlusPublishPending) {
	        	GPlusUtil.isGPlusPublishPending = false;
	        	if (resultCode == Activity.RESULT_OK) {
	        		event.updateAttendingToNewAttending();
	    			onPublishPermissionGranted();
	        		trackEvent();
	        	}
				processed = true;
	        }
		}

		if (!processed) {
			callbackManager.onActivityResult(requestCode, resultCode, data);
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
	
	private void trackEvent() {
		new UserTracker(Api.OAUTH_TOKEN, (EventSeekr) FragmentUtil.getActivity(this).getApplication(), 
        		UserTrackingItemType.event, event.getId(), event.getAttending().getValue(), null, 
        		UserTrackingType.Add).execute();
	}
	
	public void handlePublishEvent() {
		//Log.d(TAG, "handlePublish()");
		int available = GooglePlayServicesUtil.isGooglePlayServicesAvailable(FragmentUtil.getActivity(this));
		if (available != ConnectionResult.SUCCESS) {
			GPlusUtil.showDialogForGPlayServiceUnavailability(available, this);
            return;
        }
		
		if (mGoogleApiClient.isConnected()) {
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
    	if (!mGoogleApiClient.isConnected() && !mGoogleApiClient.isConnecting()) {
    		//Log.d(TAG, "try connecting");
    		mConnectionResult = null;
    		mGoogleApiClient.connect();
    	}
    }
	
	@Override
	public void onConnectionSuspended(int cause) {
		//Log.d(TAG, "onConnectionSuspended()");
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
}
