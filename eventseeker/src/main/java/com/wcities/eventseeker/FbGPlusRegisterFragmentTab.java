package com.wcities.eventseeker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
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

import org.json.JSONException;
import org.json.JSONObject;

public abstract class FbGPlusRegisterFragmentTab extends Fragment implements ConnectionCallbacks, 
		OnConnectionFailedListener, FacebookCallback<LoginResult> {
	
    private static final String TAG = FbGPlusRegisterFragment.class.getSimpleName();

	private GoogleApiClient mGoogleApiClient;
	private ConnectionResult mConnectionResult;
	protected boolean isGPlusSigningIn;

	private CallbackManager callbackManager;

	private boolean isForSignUp;
	
	protected abstract void setGooglePlusSigningInVisibility();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		EventSeekr.mGoogleApiClient = mGoogleApiClient = GPlusUtil.createPlusClientInstance(this, this, this);
    	//Settings.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS);

		callbackManager = CallbackManager.Factory.create();
		LoginManager.getInstance().registerCallback(callbackManager, this);

		isForSignUp = (this instanceof SignUpFragmentTab) ? true : false;
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
			callbackManager.onActivityResult(requestCode, resultCode, data);
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
		FbUtil.login(this);
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
        			: FragmentUtil.getTag(LoginFragmentTab.class);
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

	private void updateView(AccessToken accessToken) {
		//Log.d(TAG, "updateView()");
		FbUtil.makeMeRequest(accessToken, new GraphRequest.GraphJSONObjectCallback() {
			@Override
			public void onCompleted(JSONObject object, GraphResponse response) {
				if (object != null) {
					try {
						Bundle bundle = new Bundle();
						bundle.putBoolean(BundleKeys.IS_FOR_SIGN_UP, isForSignUp);
						bundle.putSerializable(BundleKeys.LOGIN_TYPE, LoginType.facebook);
						bundle.putString(BundleKeys.FB_USER_ID, object.getString("id"));
						bundle.putString(BundleKeys.FB_USER_NAME, object.getString("name"));
						bundle.putString(BundleKeys.FB_EMAIL_ID, object.getString("email"));

						String registerErrorListener = isForSignUp ? AppConstants.FRAGMENT_TAG_SIGN_UP
								: AppConstants.FRAGMENT_TAG_LOGIN;
						bundle.putString(BundleKeys.REGISTER_ERROR_LISTENER, registerErrorListener);

						RegistrationListener listener = (RegistrationListener) FragmentUtil.getActivity(
								FbGPlusRegisterFragmentTab.this);
						/**
						 * While changing orientation quickly sometimes listener returned is null,
						 * hence the following check.
						 */
						if (listener != null) {
							((RegistrationListener) listener).onRegistration(LoginType.facebook, bundle, true);
						}

					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			}
		});
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

	@Override
	public void onSuccess(LoginResult loginResult) {
		Log.d(TAG, "fb onSuccess()");
		updateView(loginResult.getAccessToken());
	}

	@Override
	public void onCancel() {
		Log.d(TAG, "fb onCancel()");
	}

	@Override
	public void onError(FacebookException exception) {
		Log.d(TAG, "fb onError()");
	}
}
