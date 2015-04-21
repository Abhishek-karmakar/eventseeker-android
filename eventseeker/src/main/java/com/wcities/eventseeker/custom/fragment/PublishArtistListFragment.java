package com.wcities.eventseeker.custom.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;

import com.facebook.Session;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.interfaces.PublishListener;
import com.wcities.eventseeker.util.FragmentUtil;

public abstract class PublishArtistListFragment extends ListFragment implements PublishListener/*, 
		ConnectionCallbacks, OnConnectionFailedListener*/ {
	
	private static final String TAG = PublishArtistListFragment.class.getSimpleName();

	protected Artist artist;
	// Flag to represent if we are waiting for extended permissions
	private boolean pendingAnnounce = false;
	
	//protected GoogleApiClient mGoogleApiClient;
	//protected ConnectionResult mConnectionResult;

	private boolean isPublishPermissionDisplayed;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
    	//mGoogleApiClient = GPlusUtil.createPlusClientInstance(this, this, this);
	}
	
	@Override
	public void onStart() {
		//Log.d(TAG, "onStart()");
		Session session = Session.getActiveSession();
		if (session != null) {
			session.addCallback(this);
		}
		super.onStart();
	}
	
	@Override
	public void onStop() {
		//Log.d(TAG, "onStop()");
		Session session = Session.getActiveSession();
		if (session != null) {
			session.removeCallback(this);
		}
		super.onStop();
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
		Log.d(TAG, "onActivityResult(), requestCode = " + requestCode);
		if (pendingAnnounce) {
			/*if (requestCode == AppConstants.REQ_CODE_GOOGLE_PLUS_RESOLVE_ERR || 
	        		requestCode == AppConstants.REQ_CODE_GET_GOOGLE_PLAY_SERVICES) {
	        	if (resultCode == Activity.RESULT_OK  && !mGoogleApiClient.isConnected()
	                    && !mGoogleApiClient.isConnecting()) {
		            connectPlusClient();
	        	}
	            
	        } else if (GPlusUtil.isGPlusPublishPending) {
	        	GPlusUtil.isGPlusPublishPending = false;
	        	if (resultCode == Activity.RESULT_OK) {
	        		event.updateAttendingToNewAttending();
	    			onPublishPermissionGranted();
	        		trackEvent();
	        	}
	        	
	        } else {*/
	        	//Log.d(TAG, "handle fb");
	    		// don't compare request code here, since it normally returns 64206 (hardcoded value) for openActiveSession() request
				Session session = Session.getActiveSession();
		        if (session != null) {
		        	//Log.d(TAG, "session!=null");
		            session.onActivityResult(FragmentUtil.getActivity(this), requestCode, resultCode, data);
		        }
	        /*}*/
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
	
	public boolean isPermissionDisplayed() {
		return isPublishPermissionDisplayed;
	}
	
	public void setPermissionDisplayed(boolean isPublishPermissionDisplayed) {
		this.isPublishPermissionDisplayed = isPublishPermissionDisplayed;
	}
	
}
