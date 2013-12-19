package com.wcities.eventseeker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.facebook.LoggingBehavior;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.Settings;
import com.facebook.model.GraphUser;
import com.wcities.eventseeker.ConnectAccountsFragment.ConnectAccountsFragmentListener;
import com.wcities.eventseeker.ConnectAccountsFragment.Service;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.util.FbUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class FbLogInFragment extends Fragment {
	
	private static final String TAG = FbLogInFragment.class.getName();
	
	// List of additional write permissions being requested
	//private static final List<String> PERMISSIONS = Arrays.asList("publish_actions", "publish_stream");
	// Request code for facebook reauthorization requests.
	//private static final int FACEBOOK_REAUTH_ACTIVITY_CODE = 100;

	private FbLogInFragmentListener mListener;
	
	private LinearLayout lnrLayoutProgress;
	private Button btnContinue;
	private ImageView imgFbSignUp;
    private Session.StatusCallback statusCallback = new SessionStatusCallback();
    
	// Container Activity must implement this interface
    public interface FbLogInFragmentListener {
        public void replaceFbLoginFragmentBy(String fragmentTag);
    }

    @Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (FbLogInFragmentListener) activity;
			
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement FbLogInFragmentListener");
        }
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	//setRetainInstance(true);
    	Log.d(TAG, "statusCallback : " + statusCallback);
    }
    
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.d(TAG, "THIS:" + this);
		
		View v = inflater.inflate(R.layout.fragment_fb_login, container, false);
		
		btnContinue = (Button) v.findViewById(R.id.btnContinue);
		btnContinue.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mListener.replaceFbLoginFragmentBy(AppConstants.FRAGMENT_TAG_CONNECT_ACCOUNTS);
			}
		});
		
		imgFbSignUp = (ImageView) v.findViewById(R.id.imgFbSignUp);
		lnrLayoutProgress = (LinearLayout) v.findViewById(R.id.lnrLayoutProgress);
		
		if (!FbUtil.hasUserLoggedInBefore(FragmentUtil.getActivity(this).getApplicationContext())) {
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
			mListener.replaceFbLoginFragmentBy(AppConstants.FRAGMENT_TAG_DISCOVER);
		}
		
		return v;
	}
	
	@Override
    public void onStart() {
        super.onStart();
        // In starting if user's credentials are available, then this active session will be null.
        if (Session.getActiveSession() != null) {
        	Session.getActiveSession().addCallback(statusCallback);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        // In starting if user's credentials are available, then this active session will be null.
        if (Session.getActiveSession() != null) {
        	Session.getActiveSession().removeCallback(statusCallback);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult()");
        Session.getActiveSession().onActivityResult(FragmentUtil.getActivity(this), requestCode, resultCode, data);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Session session = Session.getActiveSession();
        Session.saveSession(session, outState);
    }
    
    /*private void showProgress() {
    	imgFbSignUp.setVisibility(View.GONE);
    	btnContinue.setVisibility(View.GONE);
    	lnrLayoutProgress.setVisibility(View.VISIBLE);
    }*/
    
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
        	final EventSeekr eventSeekr = ((EventSeekr) (FragmentUtil.getActivity(FbLogInFragment.this))
        			.getApplicationContext());
	        	FbUtil.makeMeRequest(session, new Request.GraphUserCallback() {
	
	    			@Override
	    			public void onCompleted(GraphUser user, Response response) {
	    				// If the response is successful
	    	            
	    				if (session == Session.getActiveSession()) {
	    	                if (user != null) {
	    	                	Log.d(TAG, "User : " + user);
	    	                	Log.d(TAG, "(EventSeekr) (FragmentUtil.getActivity(FbLogInFragment.this))" +
	    	                			".getApplicationContext()): " 
	    	                			+ (FragmentUtil.getActivity(FbLogInFragment.this)));
	    	                			
	    	                	eventSeekr.updateFbUserId(user.getId(), 
	    	                					new AsyncTaskListener<Void>() {
	
											@Override
											public void onTaskCompleted(Void... params) {
												mListener.replaceFbLoginFragmentBy(AppConstants.FRAGMENT_TAG_CONNECT_ACCOUNTS);
											}
	    	                			});
	    	                	
	    	                	Bundle bundle = new Bundle();
	    	                	bundle.putString(BundleKeys.WCITIES_ID, user.getId());
	    	                	((ConnectAccountsFragmentListener)/*FragmentUtil.getActivity(FbLogInFragment.this)*/mListener)
	    	                	.onServiceSelected(Service.Facebook, bundle, true);
	    	                }
	    	            }
	    				
	    				/*if (session == Session.getActiveSession()) {
	    	                if (user != null) {
	    	                	showProgress();
	    	                	
	    	                	((EventSeekr) (FragmentUtil.getActivity(FbLogInFragment.this))
	    	                			.getApplicationContext()).updateFbUserId(user.getId(), new AsyncTaskListener<Void>() {
	
											@Override
											public void onTaskCompleted(Void... params) {
												mListener.replaceFbLoginFragmentBy(AppConstants.FRAGMENT_TAG_CONNECT_ACCOUNTS);
											}
	    	                			});
	    	                }
	    	            }*/
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
    				FbUtil.onClickLogin(FbLogInFragment.this, statusCallback);
    			}
    		});
        }
    }
	
	/*private boolean hasPublishPermission() {
        Session session = Session.getActiveSession();
        return session != null && session.getPermissions().containsAll(PERMISSIONS);
    }
	
	private void requestPublishPermissions(Session session, List<String> permissions,
		    int requestCode) {
		Log.d(TAG, "requestPublishPermissions()");
        Session.NewPermissionsRequest reauthRequest = new Session.NewPermissionsRequest(this, permissions)
        	.setRequestCode(requestCode);
        session.requestNewPublishPermissions(reauthRequest);
	}*/
	
	private class SessionStatusCallback implements Session.StatusCallback {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
        	Log.d(TAG, "call() - state = " + state.name());
            updateView();
        }
    }
}
