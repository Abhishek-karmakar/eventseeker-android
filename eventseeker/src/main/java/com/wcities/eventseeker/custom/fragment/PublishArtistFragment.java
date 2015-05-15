package com.wcities.eventseeker.custom.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.wcities.eventseeker.core.Artist;

public abstract class PublishArtistFragment extends Fragment implements FacebookCallback<LoginResult>/*,
		ConnectionCallbacks, OnConnectionFailedListener*/ {
	
	private static final String TAG = PublishArtistFragment.class.getSimpleName();

	protected Artist artist;

	//protected GoogleApiClient mGoogleApiClient;
	//protected ConnectionResult mConnectionResult;

	protected boolean callOnlySuperOnStart;

	private CallbackManager callbackManager;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
    	//mGoogleApiClient = GPlusUtil.createPlusClientInstance(this, this, this);

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
	
	/*@Override
	public void onDestroy() {
		super.onDestroy();
		if (mGoogleApiClient.isConnected()) {
			mGoogleApiClient.disconnect();
		}
	}*/
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult(), requestCode = " + requestCode + ", resultCode = " + resultCode);
		callbackManager.onActivityResult(requestCode, resultCode, data);
	}

	/*protected void handlePublishEvent() {
		//Log.d(TAG, "handlePublish()");
		/*int available = GooglePlayServicesUtil.isGooglePlayServicesAvailable(FragmentUtil.getActivity(this));
		if (available != ConnectionResult.SUCCESS) {
			GPlusUtil.showDialogForGPlayServiceUnavailability(available, this);
            return;
        }*/
		
		/*if (mGoogleApiClient.isConnected()) {
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
	}*/
	
	/*private void connectPlusClient() {
    	//Log.d(TAG, "connectPlusClient()");
    	if (!mGoogleApiClient.isConnected() && !mGoogleApiClient.isConnecting()) {
    		//Log.d(TAG, "try connecting");
    		mConnectionResult = null;
    		mGoogleApiClient.connect();
    	}
    }*/
	
	/*@Override
	public void onConnectionSuspended(int cause) {
		//Log.d(TAG, "onConnectionSuspended()");
		pendingAnnounce = false;
	}*/
	
	/*@Override
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
	}*/
	
	/*@Override
	public void onConnected(Bundle arg0) {
		if (pendingAnnounce) {
			GPlusUtil.publishEvent(event, this);
		}
	}*/
}
