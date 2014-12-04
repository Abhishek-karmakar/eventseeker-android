package com.wcities.eventseeker.custom.fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.ListFragment;
import android.util.Log;

import com.facebook.Session;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.wcities.eventseeker.GeneralDialogFragment.DialogBtnClickListener;
import com.wcities.eventseeker.SettingsFragment.OnSettingsItemClickedListener;
import com.wcities.eventseeker.SettingsFragment.SettingsItem;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingItemType;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.UserTracker;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.FriendNewsItem;
import com.wcities.eventseeker.interfaces.PublishListener;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.GPlusUtil;

public abstract class PublishEventListFragment extends ListFragment implements PublishListener, 
		ConnectionCallbacks, OnConnectionFailedListener, DialogBtnClickListener {
	
	private static final String TAG = PublishEventListFragment.class.getSimpleName();
	
	protected Event event;
	protected FriendNewsItem friendNewsItem;
	
	// Flag to represent if we are waiting for extended permissions
	private boolean pendingAnnounce = false;
	
	protected GoogleApiClient mGoogleApiClient;
	protected ConnectionResult mConnectionResult;

	private boolean isPublishPermissionDisplayed;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
    	mGoogleApiClient = GPlusUtil.createPlusClientInstance(this, this, this);
	}
	
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
	public void onDestroy() {
		super.onDestroy();
		if (mGoogleApiClient.isConnected()) {
			mGoogleApiClient.disconnect();
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult(), requestCode = " + requestCode);
		if (pendingAnnounce) {
			if (requestCode == AppConstants.REQ_CODE_GOOGLE_PLUS_RESOLVE_ERR || 
	        		requestCode == AppConstants.REQ_CODE_GET_GOOGLE_PLAY_SERVICES) {
	        	if (resultCode == Activity.RESULT_OK  && !mGoogleApiClient.isConnected()
	                    && !mGoogleApiClient.isConnecting()) {
		            connectPlusClient();
	        	}
	            
	        } else if (GPlusUtil.isGPlusPublishPending) {
	        	GPlusUtil.isGPlusPublishPending = false;
	        	if (resultCode == Activity.RESULT_OK) {
	        		if (event != null) {
	        			event.updateAttendingToNewAttending();
	        			
	        		} else {
	        			friendNewsItem.updateUserAttendingToNewUserAttending();
	        		}
	    			onPublishPermissionGranted();
	        		trackEvent();
	        	}
	        	
	        } else {
	    		// don't compare request code here, since it normally returns 64206 (hardcoded value) for openActiveSession() request
				Session session = Session.getActiveSession();
		        if (session != null) {
		        	Log.d(TAG, "session!=null");
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
	
	public void setEvent(Event event) {
		this.event = event;
	}

	public void setFriendNewsItem(FriendNewsItem friendNewsItem) {
		this.friendNewsItem = friendNewsItem;
	}

	protected void trackEvent() {
		long id;
		int attending;
		if (event != null) {
			id = event.getId();
			attending = event.getAttending().getValue();
			
		} else {
			id = friendNewsItem.getTrackId();
			attending = friendNewsItem.getUserAttending().getValue();
		}
		new UserTracker(Api.OAUTH_TOKEN, (EventSeekr) FragmentUtil.getActivity(this).getApplication(), 
        		UserTrackingItemType.event, id, attending, null, 
        		UserTrackingType.Add).execute();
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
	
	@Override
	public void doPositiveClick(String dialogTag) {
		if (dialogTag.equals(AppConstants.DIALOG_FRAGMENT_TAG_LOGIN_TO_TRACK_EVENT)) {
			// set firstTimeLaunch=false so as to keep facebook & google sign in rows visible.
			((EventSeekr)FragmentUtil.getActivity(this).getApplication()).updateFirstTimeLaunch(false);
			/*((DrawerListFragmentListener)FragmentUtil.getActivity(this)).onDrawerItemSelected(
					MainActivity.INDEX_NAV_ITEM_CONNECT_ACCOUNTS, null);*/
			((OnSettingsItemClickedListener) FragmentUtil.getActivity(this)).onSettingsItemClicked(SettingsItem.SYNC_ACCOUNTS, null);
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
