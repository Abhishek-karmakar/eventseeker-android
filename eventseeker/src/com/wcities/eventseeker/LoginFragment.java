package com.wcities.eventseeker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

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
import com.wcities.eventseeker.ConnectAccountsFragment.ConnectAccountsFragmentListener;
import com.wcities.eventseeker.ConnectAccountsFragment.Service;
import com.wcities.eventseeker.api.UserInfoApi.LoginType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.ConnectionFailureListener;
import com.wcities.eventseeker.util.FbUtil;
import com.wcities.eventseeker.util.FieldValidationUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.GPlusUtil;
import com.wcities.eventseeker.util.NetworkUtil;

public class LoginFragment extends FragmentLoadableFromBackStack implements ConnectionCallbacks, OnConnectionFailedListener, 
		OnClickListener, TextWatcher {
	
	private static final String TAG = LoginFragment.class.getName();
	
	private static final String DIALOG_FRAGMENT_TAG_SKIP = "skipDialog";

	private EditText edtEmail, edtPassword;
	private boolean isEmailValid, isPasswordValid;
	private Button btnLogin;
	
	//private Button btnSkip;
	private ImageView imgFbSignUp, imgGPlusSignIn;
    private TextView txtGPlusSignInStatus;
    
    private GoogleApiClient mGoogleApiClient;
    private ConnectionResult mConnectionResult;
    private boolean isGPlusSigningIn;
    
	private boolean isPermissionDisplayed;
	
    private Session.StatusCallback statusCallback = new SessionStatusCallback();

	//private Resources res;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setRetainInstance(true);
    	
    	EventSeekr.mGoogleApiClient = mGoogleApiClient = GPlusUtil.createPlusClientInstance(this, this, this);
    	//Settings.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS);
    }
    
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_login, container, false);
		
		(edtEmail = (EditText) v.findViewById(R.id.edtEmail)).addTextChangedListener(this);
		(edtPassword = (EditText) v.findViewById(R.id.edtPassword)).addTextChangedListener(this);

		(btnLogin = (Button) v.findViewById(R.id.btnLogin)).setOnClickListener(this);
		v.findViewById(R.id.btnForgotPassword).setOnClickListener(this);
		
		imgFbSignUp = (ImageView) v.findViewById(R.id.imgFbSignUp);
		imgFbSignUp.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				ConnectionFailureListener connectionFailureListener = 
						((ConnectionFailureListener) FragmentUtil.getActivity(LoginFragment.this));
				if (!NetworkUtil.getConnectivityStatus((Context) connectionFailureListener)) {
					connectionFailureListener.onConnectionFailure();
					return;
				}
				FbUtil.onClickLogin(LoginFragment.this, statusCallback);
			}
		});
		
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
        
        MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		ma.getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_HOME 
				| ActionBar.DISPLAY_HOME_AS_UP);
				
        // In starting if user's credentials are available, then this active session will be null.
        if (Session.getActiveSession() != null) {
        	Session.getActiveSession().addCallback(statusCallback);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
		Log.d(TAG, "onActivityResult(), requestCode = " + requestCode + ", resultCode = " + resultCode);
        if (requestCode == AppConstants.REQ_CODE_GOOGLE_PLUS_RESOLVE_ERR || 
        		requestCode == AppConstants.REQ_CODE_GET_GOOGLE_PLAY_SERVICES) {
        	if (resultCode == Activity.RESULT_OK  && !mGoogleApiClient.isConnected()
                    && !mGoogleApiClient.isConnecting()) {
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
    	if (!mGoogleApiClient.isConnected() && !mGoogleApiClient.isConnecting()) {
    		//Log.d(TAG, "try connecting");
    		mConnectionResult = null;
    		mGoogleApiClient.connect();
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
	    	                	bundle.putSerializable(BundleKeys.LOGIN_TYPE, LoginType.facebook);
	    	                	bundle.putString(BundleKeys.FB_USER_ID, user.getId());
	    	                	bundle.putString(BundleKeys.FB_USER_NAME, user.getUsername());
	    	                	/**
	    	                	 * this email property requires "email" permission while opening session.
	    	                	 * Email comes null if user has not verified his primary emailId on fb account
	    	                	 */
	    	                	String email = (user.getProperty("email") == null) ? "" : user.getProperty("email").toString();
	    	                	bundle.putString(BundleKeys.FB_EMAIL_ID, email);
	    	                	ConnectAccountsFragmentListener listener = (ConnectAccountsFragmentListener)FragmentUtil.getActivity(
										LoginFragment.this);
	    	                	/**
	    	                	 * While changing orientation quickly sometimes listener returned is null, 
	    	                	 * hence the following check.
	    	                	 */
	    	                	if (listener != null) {
		    	                	((ConnectAccountsFragmentListener)listener).onServiceSelected(Service.Facebook, bundle, true);
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
	
	private class SessionStatusCallback implements Session.StatusCallback {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
        	Log.d(TAG, "call() - state = " + state.name() + ", session = " + session);
        	if (state == SessionState.OPENED || state == SessionState.OPENED_TOKEN_UPDATED) {
        		updateView();
        	}
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
            bundle.putSerializable(BundleKeys.LOGIN_TYPE, LoginType.googlePlus);
        	bundle.putString(BundleKeys.GOOGLE_PLUS_USER_ID, personId);
        	bundle.putString(BundleKeys.GOOGLE_PLUS_USER_NAME, currentPerson.getDisplayName());
        	bundle.putString(BundleKeys.GOOGLE_PLUS_EMAIL_ID, Plus.AccountApi.getAccountName(mGoogleApiClient));
        	
        	ConnectAccountsFragmentListener listener = (ConnectAccountsFragmentListener)FragmentUtil.getActivity(
					LoginFragment.this);
        	
        	/**
        	 * While changing orientation quickly sometimes listener returned is null, 
        	 * hence the following check.
        	 */
        	if (listener != null) {
            	((ConnectAccountsFragmentListener)listener).onServiceSelected(Service.GooglePlus, bundle, true);
        	}
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
			break;
			
		/*case R.id.btnSkip:
			GeneralDialogFragment generalDialogFragment = GeneralDialogFragment.newInstance(
					res.getString(R.string.are_you_sure), res.getString(R.string.signin_for_better_experience), 
					res.getString(R.string.cancel), res.getString(R.string.skip));
			generalDialogFragment.show(getChildFragmentManager(), DIALOG_FRAGMENT_TAG_SKIP);
			break;*/
			
		case R.id.btnLogin:
			break;

		case R.id.btnForgotPassword:
			break;

		default:
			break;
		}
	}

	@Override
	public String getScreenName() {
		return "Account Login Screen";
	}

	/*@Override
	public void doPositiveClick(String dialogTag) {
		if (dialogTag.equals(DIALOG_FRAGMENT_TAG_SKIP)) {
			((GetStartedFragmentListener)FragmentUtil.getActivity(LoginFragment.this))
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
	}*/

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {}

	@Override
	public void afterTextChanged(Editable s) {
		View v = FragmentUtil.getActivity(this).getCurrentFocus();
		if (!(v instanceof EditText)) {
			return;
		}
		
		switch (v.getId()) {
			case R.id.edtEmail:
				isEmailValid = FieldValidationUtil.isValidEmail(edtEmail.getText().toString());
				break;
				
			case R.id.edtPassword:
				isPasswordValid = edtPassword.getText().toString().length() > 0;
				break;			
		}
		
		if (isEmailValid && isPasswordValid) {
			btnLogin.setEnabled(true);			
			
		} else {
			btnLogin.setEnabled(false);
		}
	}
}
