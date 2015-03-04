package com.wcities.eventseeker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.wcities.eventseeker.api.UserInfoApi.LoginType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.registration.Registration.RegistrationListener;
import com.wcities.eventseeker.interfaces.ConnectionFailureListener;
import com.wcities.eventseeker.util.FbUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.GPlusUtil;
import com.wcities.eventseeker.util.NetworkUtil;

public abstract class FbGPlusRegisterFragmentTab extends Fragment implements ConnectionCallbacks, 
		OnConnectionFailedListener {
	
    private static final String TAG = FbGPlusRegisterFragment.class.getSimpleName();

	private GoogleApiClient mGoogleApiClient;
	private ConnectionResult mConnectionResult;
	protected boolean isGPlusSigningIn;
	
	private Session.StatusCallback statusCallback = new SessionStatusCallback();
	private boolean isPermissionDisplayed;
	
	private boolean isForSignUp;
	
	private class SessionStatusCallback implements Session.StatusCallback {

		@Override
        public void call(Session session, SessionState state, Exception exception) {
        	Log.d(TAG, "call() - state = " + state.name() + ", session = " + session);
        	if (state == SessionState.OPENED || state == SessionState.OPENED_TOKEN_UPDATED) {
        		updateView();
        	}
        }
    }
	
	protected abstract void setGooglePlusSigningInVisibility();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		EventSeekr.mGoogleApiClient = mGoogleApiClient = GPlusUtil.createPlusClientInstance(this, this, this);
    	//Settings.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS);
		
		isForSignUp = (this instanceof SignUpFragmentTab) ? true : false;
	}
	
	@Override
    public void onStart() {
		//Log.d(TAG, "onStart()");
        super.onStart();
        
        // In starting if user's credentials are available, then this active session will be null.
        if (Session.getActiveSession() != null) {
        	Session.getActiveSession().addCallback(statusCallback);
        }
    }
	
	@Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Session session = Session.getActiveSession();
        Session.saveSession(session, outState);
    }
	
	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
		Log.d(TAG, "onActivityResult(), requestCode = " + requestCode + ", resultCode = " + resultCode);
        if (requestCode == AppConstants.REQ_CODE_GOOGLE_PLUS_RESOLVE_ERR || 
        		requestCode == AppConstants.REQ_CODE_GET_GOOGLE_PLAY_SERVICES) {
        	if (resultCode == Activity.RESULT_OK  && !mGoogleApiClient.isConnected() && !mGoogleApiClient.isConnecting()) {
        		//Log.d(TAG, "connect");
	            connectPlusClient();
	            
        	} else {
        		updateGoogleButton();
        	}
            
        } else {
        	Session.getActiveSession().onActivityResult(FragmentUtil.getActivity(this), requestCode, resultCode, data);
        }
    }
	
	@Override
    public void onStop() {
        super.onStop();
    	//Log.d(TAG, "onStop()");

        // In starting if user's credentials are available, then this active session will be null.
        if (Session.getActiveSession() != null) {
        	Session.getActiveSession().removeCallback(statusCallback);
        }
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	if (mGoogleApiClient.isConnected()) {
			mGoogleApiClient.disconnect();
		}
    }
    
    protected void onFbClicked() {
    	ConnectionFailureListener connectionFailureListener = ((ConnectionFailureListener) 
				FragmentUtil.getActivity(FbGPlusRegisterFragmentTab.this));
		if (!NetworkUtil.getConnectivityStatus((Context) connectionFailureListener)) {
			connectionFailureListener.onConnectionFailure();
			return;
		}
		FbUtil.onClickLogin(FbGPlusRegisterFragmentTab.this, statusCallback);
    }
    
    protected void onGPlusClicked() {
    	if (!mGoogleApiClient.isConnected()) {
			//Log.d(TAG, "sign in");
            int available = GooglePlayServicesUtil.isGooglePlayServicesAvailable(FragmentUtil.getActivity(this));
			if (available != ConnectionResult.SUCCESS) {
				GPlusUtil.showDialogForGPlayServiceUnavailability(available, this);
                return;
            }
			
			isGPlusSigningIn = true;
			setGooglePlusSigningInVisibility();
			
			if (mConnectionResult != null) {
	            try {
	            	//Log.d(TAG, "startResolutionForResult()");
	                mConnectionResult.startResolutionForResult(FragmentUtil.getActivity(this), AppConstants.REQ_CODE_GOOGLE_PLUS_RESOLVE_ERR);
	                
	            } catch (SendIntentException e) {
	                // Try connecting again.
	                connectPlusClient();
	            }
	            
	        } else {
	        	connectPlusClient();
	        }
			
		} else {
			onGPlusConnected();
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
	
	private void onGPlusConnected() {
		//Log.d(TAG, "onGPlusConnected()");
        updateGoogleButton();

        //Log.d(TAG, "GPlusUserId : " + ((EventSeekr)FragmentUtil.getActivity(this).getApplication()).getGPlusUserId());
        Person currentPerson = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
        Log.d(TAG, "currentPerson = " + currentPerson);
        if (currentPerson != null) {
            String personId = currentPerson.getId();
            //Log.d(TAG, "id = " + personId + ", accountName = " + mPlusClient.getAccountName());
            Bundle bundle = new Bundle();
            bundle.putBoolean(BundleKeys.IS_FOR_SIGN_UP, isForSignUp);
            bundle.putSerializable(BundleKeys.LOGIN_TYPE, LoginType.googlePlus);
        	bundle.putString(BundleKeys.GOOGLE_PLUS_USER_ID, personId);
        	bundle.putString(BundleKeys.GOOGLE_PLUS_USER_NAME, currentPerson.getDisplayName());
        	bundle.putString(BundleKeys.GOOGLE_PLUS_EMAIL_ID, Plus.AccountApi.getAccountName(mGoogleApiClient));
        	String registerErrorListener = isForSignUp ? AppConstants.FRAGMENT_TAG_SIGN_UP 
        			: FragmentUtil.getSupportTag(LoginFragmentTab.class);
        	bundle.putString(BundleKeys.REGISTER_ERROR_LISTENER, registerErrorListener);
        	
        	RegistrationListener listener = (RegistrationListener)FragmentUtil.getActivity(FbGPlusRegisterFragmentTab.this);
        	
        	/**
        	 * While changing orientation quickly sometimes listener returned is null, 
        	 * hence the following check.
        	 */
        	if (listener != null) {
            	((RegistrationListener)listener).onRegistration(LoginType.googlePlus, bundle, true);
        	}
        }
	}
	
	private void updateView() {
		//Log.d(TAG, "updateView()");
        final Session session = Session.getActiveSession();
        if (session.isOpened()) {
        	//Log.d(TAG, "session is opened");
        	/*if (!hasPublishPermission()) {
        		*//**
        		 * request for publish permissions now only so that in future user don't need to 
        		 * login again while using like/comment feature of friends activity screen.
        		 *//*
				requestPublishPermissions(session, PERMISSIONS, 0);
				
        	} else {*/
        	if (FbUtil.hasPermission(AppConstants.PERMISSIONS_FB_LOGIN)) {
	        	FbUtil.makeMeRequest(session, new Request.GraphUserCallback() {
	
	    			@Override
	    			public void onCompleted(GraphUser user, Response response) {
	    				// If the response is successful
	    	            
	    				if (session == Session.getActiveSession()) {
	    	                if (user != null) {
	    	                	Bundle bundle = new Bundle();
	    	                	bundle.putBoolean(BundleKeys.IS_FOR_SIGN_UP, isForSignUp);
	    	                	bundle.putSerializable(BundleKeys.LOGIN_TYPE, LoginType.facebook);
	    	                	bundle.putString(BundleKeys.FB_USER_ID, user.getId());
	    	                	bundle.putString(BundleKeys.FB_USER_NAME, user.getUsername());
	    	                	/**
	    	                	 * this email property requires "email" permission while opening session.
	    	                	 * Email comes null if user has not verified his primary emailId on fb account
	    	                	 */
	    	                	String email = (user.getProperty("email") == null) ? "" : user.getProperty("email").toString();
	    	                	bundle.putString(BundleKeys.FB_EMAIL_ID, email);
	    	                	String registerErrorListener = isForSignUp ? AppConstants.FRAGMENT_TAG_SIGN_UP 
	    	                			: AppConstants.FRAGMENT_TAG_LOGIN;
	    	                	bundle.putString(BundleKeys.REGISTER_ERROR_LISTENER, registerErrorListener);
	    	                	
	    	                	RegistrationListener listener = (RegistrationListener)FragmentUtil.getActivity(
										FbGPlusRegisterFragmentTab.this);
	    	                	/**
	    	                	 * While changing orientation quickly sometimes listener returned is null, 
	    	                	 * hence the following check.
	    	                	 */
	    	                	if (listener != null) {
		    	                	((RegistrationListener)listener).onRegistration(LoginType.facebook, bundle, true);
	    	                	}
	    	                }
	    	            }
	    				
	    	            if (response.getError() != null) {
	    	                // Handle errors, will do so later.
	    	            }
	    			}
	    	    });
	        	
        	} else {
        		if (!isPermissionDisplayed) {
	        		//Log.d(TAG, "request email permission");
	        		FbUtil.requestEmailPermission(session, AppConstants.PERMISSIONS_FB_LOGIN, 
	        				AppConstants.REQ_CODE_FB_LOGIN_EMAIL, this);
	        		isPermissionDisplayed = true;
	        		
        		} else {
        			//Log.d(TAG, "permission is already displayed");
        			isPermissionDisplayed = false;
        		}
        	}
        } 
    }
	
	private void updateGoogleButton() {
		//Log.d(TAG, "updateGoogleButton()");
		isGPlusSigningIn = false;
		setGooglePlusSigningInVisibility();
    }
	
	@Override
	public void onConnectionFailed(ConnectionResult result) {
		//Log.d(TAG, "onConnectionFailed()");
		// Save the result and resolve the connection failure upon a user click.
		mConnectionResult = result;
		if (mConnectionResult.hasResolution()) {
			//Log.d(TAG, "if");
            try {
				mConnectionResult.startResolutionForResult(FragmentUtil.getActivity(this), AppConstants.REQ_CODE_GOOGLE_PLUS_RESOLVE_ERR);
				
			} catch (SendIntentException e) {
				e.printStackTrace();
				// Try connecting again.
                connectPlusClient();
			}
			
		} else {
			//Log.d(TAG, "else");
			updateGoogleButton();
		}
	}

	@Override
	public void onConnected(Bundle arg0) {
		//Log.d(TAG, "onConnected()");
        onGPlusConnected();
	}

	@Override
	public void onConnectionSuspended(int cause) {
		//Log.d(TAG, "onConnectionSuspended");
		updateGoogleButton();
	}
}
