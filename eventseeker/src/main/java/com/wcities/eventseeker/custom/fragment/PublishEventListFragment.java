package com.wcities.eventseeker.custom.fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.login.LoginManager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.wcities.eventseeker.GeneralDialogFragment;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingItemType;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.UserTracker;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.FriendNewsItem;
import com.wcities.eventseeker.interfaces.PublishListener;
import com.wcities.eventseeker.util.CalendarUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.GPlusUtil;

public abstract class PublishEventListFragment extends ListFragment implements PublishListener, 
		ConnectionCallbacks, OnConnectionFailedListener {
	
	private static final String TAG = PublishEventListFragment.class.getSimpleName();
	
	protected Event event;
	protected FriendNewsItem friendNewsItem;
	
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
	        		if (event != null) {
	        			event.updateAttendingToNewAttending();
	        			/**
	        			 * Call onPublishPermissionGranted() afterwards, because for my events screen if user is saving
	        			 * event from following/recommended tab then we want to refresh saved events tab which we are doing from
	        			 * onPublishPermissionGranted() call sequence. If we call onPublishPermissionGranted() before actually
	        			 * sending updated usertracker value to eventseeker server, refreshed saved events call might not generate
	        			 * newly saved event since usertracker call might not have finished yet.
	        			 */
	        			Toast.makeText(FragmentUtil.getActivity(this), R.string.saving_event, Toast.LENGTH_SHORT).show();
	        			trackEvent();
	        			
	        		} else {
	        			friendNewsItem.updateUserAttendingToNewUserAttending();
		        		trackFriendNewsItem();
	        		}
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
	
	public void setEvent(Event event) {
		this.event = event;
	}

	public void setFriendNewsItem(FriendNewsItem friendNewsItem) {
		this.friendNewsItem = friendNewsItem;
	}

	private void trackEvent() {
		long id = event.getId();
		int	attending = event.getAttending().getValue();
		new UserTracker(Api.OAUTH_TOKEN, (EventSeekr) FragmentUtil.getActivity(this).getApplication(), 
        		UserTrackingItemType.event, id, attending, null, 
        		UserTrackingType.Add) {
			
			protected void onPostExecute(Void result) {
				onPublishPermissionGranted();
			}
			
		}.execute();
	}
	
	private void trackFriendNewsItem() {
		long id = friendNewsItem.getTrackId();
		int attending = friendNewsItem.getUserAttending().getValue();
		new UserTracker(Api.OAUTH_TOKEN, (EventSeekr) FragmentUtil.getActivity(this).getApplication(), 
        		UserTrackingItemType.event, id, attending, null, 
        		UserTrackingType.Add) {

			protected void onPostExecute(Void result) {
				onPublishPermissionGranted();
			}

		}.execute();
	}
	
	public void handlePublishEvent() {
		Log.d(TAG, "handlePublish()");
		int available = GooglePlayServicesUtil.isGooglePlayServicesAvailable(FragmentUtil.getActivity(this));
		if (available != ConnectionResult.SUCCESS) {
			GPlusUtil.showDialogForGPlayServiceUnavailability(available, this);
            return;
        }
		
		if (mGoogleApiClient.isConnected()) {
			if (event != null) {
				GPlusUtil.publishEvent(event, this);
				
			} else {
				GPlusUtil.publishFriendNewsItem(friendNewsItem, this);
			}
			
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
    	Log.d(TAG, "connectPlusClient()");
    	if (!mGoogleApiClient.isConnected() && !mGoogleApiClient.isConnecting()) {
    		Log.d(TAG, "try connecting");
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
			if (event != null) {
				GPlusUtil.publishEvent(event, this);
				
			} else {
				GPlusUtil.publishFriendNewsItem(friendNewsItem, this);
			}
		}
	}

	protected void addEventToCalendar() {
		if (event != null) {
			CalendarUtil.addEventToCalendar(this, event);

		} else {
			CalendarUtil.addEventToCalendar(this, friendNewsItem.toEvent());
		}
	}

	protected void showAddToCalendarDialog(GeneralDialogFragment.DialogBtnClickListener dialogBtnClickListener) {
		CalendarUtil.showAddToCalendarDialog(this, dialogBtnClickListener);
	}
}
