package com.wcities.eventseeker;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wcities.eventseeker.GeneralDialogFragment.DialogBtnClickListener;
import com.wcities.eventseeker.api.UserInfoApi.LoginType;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.registration.ForgotPassword;
import com.wcities.eventseeker.core.registration.Registration.RegistrationErrorListener;
import com.wcities.eventseeker.core.registration.Registration.RegistrationListener;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FieldValidationUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class LoginFragment extends FbGPlusRegisterFragment implements OnClickListener, TextWatcher, 
		RegistrationErrorListener, DialogBtnClickListener {
	
	private static final String TAG = LoginFragment.class.getName();

	private static final String DIALOG_FRAGMENT_TAG_EMAIL_OR_PASSWORD_INCORRECT = "dialogEmailOrPasswordIncorrect";
	private static final String DIALOG_FRAGMENT_TAG_UNKNOWN_ERROR = "unknownError";
	private static final String DIALOG_FRAGMENT_TAG_CHK_EMAIL = "dialogChkEmail";
	
	private EditText edtEmail, edtPassword;
	private boolean isEmailValid, isPasswordValid;
	private Button btnLogin, btnForgotPassword;
	
	private Button imgFbSignUp, imgGPlusSignIn;
    private TextView txtGPlusSignInStatus;
    
    private RelativeLayout rltLytPrgsBar;
    private int progressBarVisibility = View.INVISIBLE;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setRetainInstance(true);
    }
    
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		//Log.d(TAG, "onCreateView()");
		View v = inflater.inflate(R.layout.fragment_login, container, false);
		
		(edtEmail = (EditText) v.findViewById(R.id.edtEmail)).addTextChangedListener(this);
		(edtPassword = (EditText) v.findViewById(R.id.edtPassword)).addTextChangedListener(this);

		(btnLogin = (Button) v.findViewById(R.id.btnLogin)).setOnClickListener(this);
		(btnForgotPassword = (Button) v.findViewById(R.id.btnForgotPassword)).setOnClickListener(this);
		
		imgFbSignUp = (Button) v.findViewById(R.id.imgFbSignUp);
		imgFbSignUp.setOnClickListener(this);
		
		imgGPlusSignIn = (Button) v.findViewById(R.id.imgGPlusSignIn);
		imgGPlusSignIn.setOnClickListener(this);
		txtGPlusSignInStatus = (TextView) v.findViewById(R.id.txtGPlusSignInStatus);
		
		rltLytPrgsBar = (RelativeLayout) v.findViewById(R.id.rltLytPrgsBar);
		rltLytPrgsBar.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return true;
			}
		});
		
		setGooglePlusSigningInVisibility();
		updateProgressBarVisibility();
		
		return v;
	}
    
	@Override
	public void onStart() {
		super.onStart();
		//Log.d(TAG, "onStart()");
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		ma.setDrawerLockMode(true);
		ma.setDrawerIndicatorEnabled(false);
		if (ma.isTabletAndInLandscapeMode()) {
			ma.hideDrawerList();
		}
		toggleSigninBtn();
		toggleForgotPwdBtn();
	}
	
	@Override
	public void onStop() {
		super.onStop();
		//Log.d(TAG, "onStop()");
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		if (ma.isTabletAndInLandscapeMode()) {
			ma.unHideDrawerList();
		}
	}
	
	protected void setGooglePlusSigningInVisibility() {
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
	
	private void updateProgressBarVisibility() {
		rltLytPrgsBar.setVisibility(progressBarVisibility);
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.imgFbSignUp:
			onFbClicked();
			break;
		
		case R.id.imgGPlusSignIn:
			onGPlusClicked();
			break;
			
		case R.id.btnLogin:
			Bundle bundle = new Bundle();
        	bundle.putSerializable(BundleKeys.LOGIN_TYPE, LoginType.emailLogin);
        	bundle.putString(BundleKeys.EMAIL_ID, edtEmail.getText().toString());
        	bundle.putString(BundleKeys.PASSWORD, edtPassword.getText().toString());
        	bundle.putString(BundleKeys.REGISTER_ERROR_LISTENER, AppConstants.FRAGMENT_TAG_LOGIN);
        	
        	((RegistrationListener)FragmentUtil.getActivity(this)).onRegistration(LoginType.emailLogin, bundle, 
        			true);
			break;

		case R.id.btnForgotPassword:
			AsyncTaskUtil.executeAsyncTask(new ResetPassword(), true);
			break;

		default:
			break;
		}
	}

	@Override
	public String getScreenName() {
		return "Account Login Screen";
	}
	
	private class ResetPassword extends AsyncTask<Void, Void, Integer> {
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progressBarVisibility = View.VISIBLE;
			updateProgressBarVisibility();
		}

		@Override
		protected Integer doInBackground(Void... params) {
			ForgotPassword forgotPassword = new ForgotPassword(FragmentUtil.getApplication(LoginFragment.this), 
					edtEmail.getText().toString());
			try {
				return forgotPassword.perform();
				
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				
			} catch (IOException e) {
				e.printStackTrace();
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return UserInfoApiJSONParser.MSG_CODE_UNSUCCESS;
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);
			progressBarVisibility = View.INVISIBLE;
			updateProgressBarVisibility();
			handleResetPwdResult(result);
		}
	}
	
	private void handleResetPwdResult(int msgCode) {
		try {
			if (msgCode == UserInfoApiJSONParser.MSG_CODE_CHK_EMAIL_TO_RESET_PWD) {
				GeneralDialogFragment generalDialogFragment = GeneralDialogFragment.newInstance(this,
						FragmentUtil.getResources(this).getString(R.string.dialog_msg_chk_email_to_reset_pwd), "Ok", false);
				generalDialogFragment.show(((ActionBarActivity) FragmentUtil.getActivity(this)).getSupportFragmentManager(), 
						DIALOG_FRAGMENT_TAG_CHK_EMAIL);
			
			} else if (msgCode == UserInfoApiJSONParser.MSG_CODE_USER_EMAIL_DOESNT_EXIST) {
				GeneralDialogFragment generalDialogFragment = GeneralDialogFragment.newInstance(this,
						FragmentUtil.getResources(this).getString(R.string.dialog_msg_user_email_doesnt_exist), "Ok", false);
				generalDialogFragment.show(((ActionBarActivity) FragmentUtil.getActivity(this)).getSupportFragmentManager(), 
						DIALOG_FRAGMENT_TAG_CHK_EMAIL);
				
			} else if (msgCode == UserInfoApiJSONParser.MSG_CODE_UNSUCCESS) {
				GeneralDialogFragment generalDialogFragment = GeneralDialogFragment.newInstance(this,
						FragmentUtil.getResources(this).getString(R.string.error_title), 
						FragmentUtil.getResources(this).getString(R.string.error_unknown_error), "Ok", null, false);
				generalDialogFragment.show(((ActionBarActivity) FragmentUtil.getActivity(this)).getSupportFragmentManager(), DIALOG_FRAGMENT_TAG_UNKNOWN_ERROR);
			}
			
		} catch (IllegalStateException e) {
			Log.e(TAG, "Fragment is already removed...");
			e.printStackTrace();
		}
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
		
		toggleSigninBtn();
		toggleForgotPwdBtn();
	}

	private void toggleSigninBtn() {
		btnLogin.setEnabled(isEmailValid && isPasswordValid);			
	}
	
	private void toggleForgotPwdBtn() {
		btnForgotPassword.setEnabled(isEmailValid);			
	}

	@Override
	public void onErrorOccured(int errorCode) {
		if (errorCode == UserInfoApiJSONParser.MSG_CODE_EMAIL_OR_PWD_INCORRECT) {
			GeneralDialogFragment generalDialogFragment 
				= GeneralDialogFragment.newInstance(this,
					FragmentUtil.getResources(this).getString(R.string.error_title), 
					FragmentUtil.getResources(this).getString(R.string.error_email_or_password_incorrect), "Ok", null, false);
			generalDialogFragment.show(((ActionBarActivity) FragmentUtil.getActivity(this)).getSupportFragmentManager(), 
					DIALOG_FRAGMENT_TAG_EMAIL_OR_PASSWORD_INCORRECT);
		
		} else if (errorCode == UserInfoApiJSONParser.MSG_CODE_NO_ACCESS_TOKEN 
				|| errorCode == UserInfoApiJSONParser.MSG_CODE_UNSUCCESS) {
			GeneralDialogFragment generalDialogFragment 
				= GeneralDialogFragment.newInstance(this,
					FragmentUtil.getResources(this).getString(R.string.error_title), 
					FragmentUtil.getResources(this).getString(R.string.error_unknown_error), "Ok", null, false);
			generalDialogFragment.show(((ActionBarActivity) FragmentUtil.getActivity(this)).getSupportFragmentManager(), 
					DIALOG_FRAGMENT_TAG_UNKNOWN_ERROR);
		}
		
		// to call onFragmentResumed(Fragment) of MainActivity (to update current fragment tag, etc.)
		onResume();
	}

	@Override
	public void doPositiveClick(String dialogTag) {}

	@Override
	public void doNegativeClick(String dialogTag) {}
}
