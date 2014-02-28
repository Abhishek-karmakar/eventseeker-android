package com.wcities.eventseeker;

import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.LoggingBehavior;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.Settings;
import com.facebook.model.GraphUser;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.plus.PlusClient;
import com.google.android.gms.plus.model.people.Person;
import com.wcities.eventseeker.ConnectAccountsFragment.ConnectAccountsFragmentListener;
import com.wcities.eventseeker.ConnectAccountsFragment.Service;
import com.wcities.eventseeker.GeneralDialogFragment.DialogBtnClickListener;
import com.wcities.eventseeker.api.UserInfoApi.LoginType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.interfaces.ConnectionFailureListener;
import com.wcities.eventseeker.util.FbUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.GPlusUtil;
import com.wcities.eventseeker.util.NetworkUtil;

public class GetStartedFragment extends Fragment implements ConnectionCallbacks, OnConnectionFailedListener, 
		OnClickListener, DialogBtnClickListener {
	
	private static final String TAG = GetStartedFragment.class.getName();
	
	// List of additional write permissions being requested
	//private static final List<String> PERMISSIONS = Arrays.asList("publish_actions", "publish_stream");
	// Request code for facebook reauthorization requests.
	//private static final int FACEBOOK_REAUTH_ACTIVITY_CODE = 100;
	
	private static final String DIALOG_FRAGMENT_TAG_SKIP = "skipDialog";
	
	private Button btnSkip;
	private ImageView imgFbSignUp, imgGPlusSignIn;
    private Session.StatusCallback statusCallback;
    private TextView txtGPlusSignInStatus;
    
    private PlusClient mPlusClient;
    private ConnectionResult mConnectionResult;
    private boolean isGPlusSigningIn;
    
	// Container Activity must implement this interface
    public interface GetStartedFragmentListener {
        public void replaceGetStartedFragmentBy(String fragmentTag);
    }

    @Override
	public void onAttach(Activity activity) {
    	//Log.d(TAG, "onAttach()");
		super.onAttach(activity);
		if (!(activity instanceof GetStartedFragmentListener)) {
            throw new ClassCastException(activity.toString() + " must implement FbLogInFragmentListener");
        }
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setRetainInstance(true);
    	
    	mPlusClient = GPlusUtil.createPlusClientInstance(this, this, this);
    }
    
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		//Log.d(TAG, "onCreateView(), THIS:" + this);

		View v = inflater.inflate(R.layout.fragment_get_started, container, false);
		
		btnSkip = (Button) v.findViewById(R.id.btnSkip);
		btnSkip.setOnClickListener(this);
		
		imgFbSignUp = (ImageView) v.findViewById(R.id.imgFbSignUp);
		
		if (statusCallback == null) {
			
			statusCallback = new SessionStatusCallback();
			
			Context appContext = FragmentUtil.getActivity(this).getApplicationContext();
			if (!FbUtil.hasUserLoggedInBefore(appContext) && !GPlusUtil.hasUserLoggedInBefore(appContext)) {
				Log.d(TAG, "not logged in");
				Settings.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS);
				
				Session session = Session.getActiveSession();
		        if (session == null) {
		        	//Log.d(TAG, "session == null");
		            if (savedInstanceState != null) {
		                session = Session.restoreSession(FragmentUtil.getActivity(this), null, statusCallback, savedInstanceState);
		            }
		            if (session == null) {
		                session = new Session(FragmentUtil.getActivity(this));
		            }
		            Session.setActiveSession(session);
		            if (session.getState().equals(SessionState.CREATED_TOKEN_LOADED)) {
		            	Log.d(TAG, "CREATED_TOKEN_LOADED");
		                session.openForRead(new Session.OpenRequest(this).setCallback(statusCallback));
		            	//session.closeAndClearTokenInformation();
		            }
		        }
		        
		        updateView();
		        
			} else {
				((GetStartedFragmentListener)FragmentUtil.getActivity(GetStartedFragment.this))
						.replaceGetStartedFragmentBy(AppConstants.FRAGMENT_TAG_DISCOVER);
			}
			
		} else {
			if (!FbUtil.hasUserLoggedInBefore(FragmentUtil.getActivity(this).getApplicationContext())) {
		        updateView();
			}
		}
		
		imgGPlusSignIn = (ImageView) v.findViewById(R.id.imgGPlusSignIn);
		imgGPlusSignIn.setOnClickListener(this);
		txtGPlusSignInStatus = (TextView) v.findViewById(R.id.txtGPlusSignInStatus);
		
		setGooglePlusSigningInVisibility();
		return v;
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
    public void onStop() {
    	//Log.d(TAG, "onStop()");
        super.onStop();
        // In starting if user's credentials are available, then this active session will be null.
        if (Session.getActiveSession() != null) {
        	Session.getActiveSession().removeCallback(statusCallback);
        }
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
        super.onActivityResult(requestCode, resultCode, data);
		Log.d(TAG, "onActivityResult(), requestCode = " + requestCode + ", resultCode = " + resultCode);
        if (requestCode == AppConstants.REQ_CODE_GOOGLE_PLUS_RESOLVE_ERR || 
        		requestCode == AppConstants.REQ_CODE_GET_GOOGLE_PLAY_SERVICES) {
        	if (resultCode == Activity.RESULT_OK  && !mPlusClient.isConnected()
                    && !mPlusClient.isConnecting()) {
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Session session = Session.getActiveSession();
        Session.saveSession(session, outState);
    }
    
    private void connectPlusClient() {
    	//Log.d(TAG, "connectPlusClient()");
    	if (!mPlusClient.isConnected() && !mPlusClient.isConnecting()) {
    		//Log.d(TAG, "try connecting");
    		mConnectionResult = null;
    		mPlusClient.connect();
    	}
    }
    
	private void updateView() {
		Log.d(TAG, "updateView()");
        final Session session = Session.getActiveSession();
        if (session.isOpened()) {
        	Log.d(TAG, "session is opened");
        	/*if (!hasPublishPermission()) {
        		*//**
        		 * request for publish permissions now only so that in future user don't need to 
        		 * login again while using like/comment feature of friends activity screen.
        		 *//*
				requestPublishPermissions(session, PERMISSIONS, 0);
				
        	} else {*/
	        	FbUtil.makeMeRequest(session, new Request.GraphUserCallback() {
	
	    			@Override
	    			public void onCompleted(GraphUser user, Response response) {
	    				// If the response is successful
	    	            
	    				if (session == Session.getActiveSession()) {
	    	                if (user != null) {
	    	                	Bundle bundle = new Bundle();
	    	                	bundle.putSerializable(BundleKeys.LOGIN_TYPE, LoginType.facebook);
	    	                	bundle.putString(BundleKeys.FB_USER_ID, user.getId());
	    	                	bundle.putString(BundleKeys.FB_USER_NAME, user.getUsername());
	    	                	// this email property requires "email" permission while opening session
	    	                	bundle.putString(BundleKeys.FB_EMAIL_ID, user.getProperty("email").toString());
	    	                	ConnectAccountsFragmentListener listener = (ConnectAccountsFragmentListener)FragmentUtil.getActivity(
										GetStartedFragment.this);
	    	                	/**
	    	                	 * While changing orientation quickly sometimes listener returned is null, 
	    	                	 * hence the following check.
	    	                	 */
	    	                	if (listener != null) {
		    	                	((ConnectAccountsFragmentListener)listener).onServiceSelected(Service.Facebook, bundle, false);
	    	                	}
	    	                }
	    	            }
	    				
	    	            if (response.getError() != null) {
	    	                // Handle errors, will do so later.
	    	            }
	    			}
	    	    });
        	//}
        	
        } else {
        	Log.i(TAG, "session is not opened");
        	imgFbSignUp.setOnClickListener(new View.OnClickListener() {
    			
    			@Override
    			public void onClick(View v) {
    				ConnectionFailureListener connectionFailureListener = 
    						((ConnectionFailureListener) FragmentUtil.getActivity(GetStartedFragment.this));
    				if (!NetworkUtil.getConnectivityStatus((Context) connectionFailureListener)) {
    					connectionFailureListener.onConnectionFailure();
    					return;
    				}
    				FbUtil.onClickLogin(GetStartedFragment.this, statusCallback);
    			}
    		});
        }
    }
	
	private class SessionStatusCallback implements Session.StatusCallback {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
        	Log.d(TAG, "call() - state = " + state.name());
            updateView();
        }
    }
	
	private void setGooglePlusSigningInVisibility() {
		//Log.d(TAG, "updateGoogleButton(), isGPlusSigningIn = " + isGPlusSigningIn);
		
		if (isGPlusSigningIn) {
			txtGPlusSignInStatus.setVisibility(View.VISIBLE);
			imgGPlusSignIn.setVisibility(View.INVISIBLE);
			
		} else {
            // Enable the sign-in button
        	txtGPlusSignInStatus.setVisibility(View.INVISIBLE);
        	imgGPlusSignIn.setVisibility(View.VISIBLE);
		}
	}
	
	private void updateGoogleButton() {
		isGPlusSigningIn = false;
		setGooglePlusSigningInVisibility();
    }
	
	@Override
	public void onConnected(Bundle arg0) {
		//Log.d(TAG, "onConnected()");
        updateGoogleButton();

        //Log.d(TAG, "GPlusUserId : " + ((EventSeekr)FragmentUtil.getActivity(this).getApplication()).getGPlusUserId());
		if (((EventSeekr)FragmentUtil.getActivity(this).getApplication()).getGPlusUserId() == null) {
			
	        Person currentPerson = mPlusClient.getCurrentPerson();
	        //Log.d(TAG, "currentPerson = " + currentPerson);
	        if (currentPerson != null) {
	            String personId = currentPerson.getId();
	            //Log.d(TAG, "id = " + personId + ", accountName = " + mPlusClient.getAccountName());
	            Bundle bundle = new Bundle();
	            bundle.putSerializable(BundleKeys.LOGIN_TYPE, LoginType.googlePlus);
	        	bundle.putString(BundleKeys.GOOGLE_PLUS_USER_ID, personId);
	        	bundle.putString(BundleKeys.GOOGLE_PLUS_USER_NAME, currentPerson.getDisplayName());
	        	bundle.putString(BundleKeys.GOOGLE_PLUS_EMAIL_ID, mPlusClient.getAccountName());
	        	
	        	ConnectAccountsFragmentListener listener = (ConnectAccountsFragmentListener)FragmentUtil.getActivity(
						GetStartedFragment.this);
	        	
	        	/**
	        	 * While changing orientation quickly sometimes listener returned is null, 
	        	 * hence the following check.
	        	 */
	        	if (listener != null) {
	            	((ConnectAccountsFragmentListener)listener).onServiceSelected(Service.GooglePlus, bundle, false);
	        	}
	        }
	        
		} else {
			((GetStartedFragmentListener)FragmentUtil.getActivity(GetStartedFragment.this))
					.replaceGetStartedFragmentBy(AppConstants.FRAGMENT_TAG_DISCOVER);
		}
	}

	@Override
	public void onDisconnected() {
		//Log.d(TAG, "disconnected");
		updateGoogleButton();
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
			updateGoogleButton();
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.imgGPlusSignIn:
			if (!mPlusClient.isConnected()) {
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
			}
			break;
			
		case R.id.btnSkip:
			GeneralDialogFragment generalDialogFragment = GeneralDialogFragment.newInstance("Are you sure ?", 
					"Signing in allows for a better experience.", "Cancel", "Skip");
			generalDialogFragment.show(getChildFragmentManager(), DIALOG_FRAGMENT_TAG_SKIP);
			break;

		default:
			break;
		}
	}

	@Override
	public void doPositiveClick(String dialogTag) {
		if (dialogTag.equals(DIALOG_FRAGMENT_TAG_SKIP)) {
			((GetStartedFragmentListener)FragmentUtil.getActivity(GetStartedFragment.this))
					.replaceGetStartedFragmentBy(AppConstants.FRAGMENT_TAG_CONNECT_ACCOUNTS);
		}
	}

	@Override
	public void doNegativeClick(String dialogTag) {
		if (dialogTag.equals(DIALOG_FRAGMENT_TAG_SKIP)) {
			DialogFragment dialogFragment = (DialogFragment) getChildFragmentManager().findFragmentByTag(DIALOG_FRAGMENT_TAG_SKIP);
			if (dialogFragment != null) {
				dialogFragment.dismiss();
			}
		}
	}
}
