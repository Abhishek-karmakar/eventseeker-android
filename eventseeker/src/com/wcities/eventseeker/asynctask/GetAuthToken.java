package com.wcities.eventseeker.asynctask;

import java.io.IOException;

import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.util.FragmentUtil;

public class GetAuthToken extends AsyncTask<String, Void, String> {
	
	private static final String TAG = GetAuthToken.class.getSimpleName();
	
	private Fragment fragment;
	private AsyncTaskListener<Object> asyncTaskListener;

	public GetAuthToken(Fragment fragment, AsyncTaskListener<Object> asyncTaskListener) {
		this.fragment = fragment;
		this.asyncTaskListener = asyncTaskListener;
	}

	@Override
	protected String doInBackground(String... params) {
		try {
        	String authToken = GoogleAuthUtil.getToken(FragmentUtil.getActivity(fragment).getApplicationContext(), 
        			params[0], "sj");
        	//Log.d(TAG, "return authToken");
            return authToken;
            
        } catch (UserRecoverableAuthException e) {
        	//Log.d(TAG, "UserRecoverableAuthException");
        	asyncTaskListener.onTaskCompleted(e.getIntent(), AppConstants.REQ_CODE_GOOGLE_ACCOUNT_CHOOSER);
            e.printStackTrace();
            
        } catch (IOException e) {
			e.printStackTrace();
			
		} catch (GoogleAuthException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected void onPostExecute(String authToken) {
		super.onPostExecute(authToken);
		if (authToken != null) {
			asyncTaskListener.onTaskCompleted(authToken);
		}
	}
}
