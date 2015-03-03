package com.wcities.eventseeker;

import java.io.IOException;
import java.util.HashMap;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;

import android.content.res.Resources;
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
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wcities.eventseeker.GeneralDialogFragment.DialogBtnClickListener;
import com.wcities.eventseeker.api.UserInfoApi.LoginType;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.registration.ForgotPassword;
import com.wcities.eventseeker.core.registration.Registration.RegistrationErrorListener;
import com.wcities.eventseeker.core.registration.Registration.RegistrationListener;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FieldValidationUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class LoginFragmentTab extends FbGPlusRegisterFragmentTab implements TextWatcher, 
		OnFocusChangeListener, OnClickListener, DialogBtnClickListener, RegistrationErrorListener {
	
	private static final String TAG = LoginFragmentTab.class.getSimpleName();

	private static final String DIALOG_FRAGMENT_TAG_EMAIL_OR_PASSWORD_INCORRECT = "dialogEmailOrPasswordIncorrect";
	private static final String DIALOG_FRAGMENT_TAG_UNKNOWN_ERROR = "unknownError";
	private static final String DIALOG_FRAGMENT_TAG_CHK_EMAIL = "dialogChkEmail";
	
	private static final String IMG_EMAIL = "email";
	private static final String IMG_PASSWORD = "password";
	
	private EditText edtEmail, edtPassword;
	private ImageView imgEmailIndicator, imgPasswordIndicator;
	private boolean isEmailValid, isPasswordValid;
	private TextView txtEmailInvalid;
	private Button btnLogin, btnForgotPassword;
	
	private Button imgFbSignUp, imgGPlusSignIn;
    private TextView txtGPlusSignInStatus;
    
    private RelativeLayout rltLytPrgsBar;
    private int progressBarVisibility = View.INVISIBLE;
    
    private HashMap<String, ImgIndicatorState> imgIndicatorStateMap;
    
    private enum ImgIndicatorState {
		IMG_INVISIBLE,
		IMG_CROSS,
		IMG_CHECK;
	}
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setRetainInstance(true);
    	
    	imgIndicatorStateMap = new HashMap<String, ImgIndicatorState>();
		imgIndicatorStateMap.put(IMG_EMAIL, ImgIndicatorState.IMG_INVISIBLE);
		imgIndicatorStateMap.put(IMG_PASSWORD, ImgIndicatorState.IMG_INVISIBLE);
    }
    
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		//Log.d(TAG, "onCreateView(), savedInstanceState = " + savedInstanceState);
		View v = inflater.inflate(R.layout.fragment_login, container, false);
		
		(edtEmail = (EditText) v.findViewById(R.id.edtEmail)).addTextChangedListener(this);
		(edtPassword = (EditText) v.findViewById(R.id.edtPassword)).addTextChangedListener(this);
		
		imgEmailIndicator = (ImageView) v.findViewById(R.id.imgEmailIndicator);
		imgPasswordIndicator = (ImageView) v.findViewById(R.id.imgPasswordIndicator);
		
		txtEmailInvalid = (TextView) v.findViewById(R.id.txtEmailInvalid);
		
		edtEmail.setOnFocusChangeListener(this);
		edtPassword.setOnFocusChangeListener(this);

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
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if (imgIndicatorStateMap.get(IMG_EMAIL) != ImgIndicatorState.IMG_INVISIBLE) {
			try {
				if (imgIndicatorStateMap.get(IMG_EMAIL) == ImgIndicatorState.IMG_CHECK) {
					imgEmailIndicator.setImageResource(R.drawable.ic_valid_check);
					txtEmailInvalid.setVisibility(View.INVISIBLE);
					
				} else {
					imgEmailIndicator.setImageResource(R.drawable.ic_invalid_cross);
					txtEmailInvalid.setVisibility(View.VISIBLE);
				}
				
			} catch (Resources.NotFoundException e) {
				e.printStackTrace();
			}
		}
		
		if (imgIndicatorStateMap.get(IMG_PASSWORD) != ImgIndicatorState.IMG_INVISIBLE) {
			imgPasswordIndicator.setImageResource(imgIndicatorStateMap.get(IMG_PASSWORD) == ImgIndicatorState.IMG_CHECK ?
					R.drawable.ic_valid_check : 0);			
		}
	}
    
	@Override
	public void onStart() {
		super.onStart();
		//Log.d(TAG, "onStart()");
		toggleSigninBtn();
		toggleForgotPwdBtn();
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
        	bundle.putString(BundleKeys.REGISTER_ERROR_LISTENER, FragmentUtil.getSupportTag(LoginFragmentTab.class));
        	
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

	private class ResetPassword extends AsyncTask<Void, Void, Integer> {
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progressBarVisibility = View.VISIBLE;
			updateProgressBarVisibility();
		}

		@Override
		protected Integer doInBackground(Void... params) {
			ForgotPassword forgotPassword = new ForgotPassword(FragmentUtil.getApplication(LoginFragmentTab.this), 
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
				// to remove the error icon if any present on it.
				imgEmailIndicator.setImageResource(0);
				txtEmailInvalid.setVisibility(View.INVISIBLE);
				imgIndicatorStateMap.put(IMG_EMAIL, ImgIndicatorState.IMG_INVISIBLE);
				
				isEmailValid = FieldValidationUtil.isValidEmail(edtEmail.getText().toString());
				if (isEmailValid) {
					imgEmailIndicator.setImageResource(R.drawable.ic_valid_check);
					imgIndicatorStateMap.put(IMG_EMAIL, ImgIndicatorState.IMG_CHECK);
				}
				break;
				
			case R.id.edtPassword:
				imgPasswordIndicator.setImageResource(0);
				imgIndicatorStateMap.put(IMG_PASSWORD, ImgIndicatorState.IMG_INVISIBLE);
				
				isPasswordValid = edtPassword.getText().toString().length() > 0;
				if (isPasswordValid) {
					imgPasswordIndicator.setImageResource(R.drawable.ic_valid_check);
					imgIndicatorStateMap.put(IMG_PASSWORD, ImgIndicatorState.IMG_CHECK);
				}
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
	}

	@Override
	public void doPositiveClick(String dialogTag) {}

	@Override
	public void doNegativeClick(String dialogTag) {}

	@Override
	public void onFocusChange(View v, boolean hasFocus) {
		//Log.d(TAG, "onFocusChange(), hasFocus = " + hasFocus);
		if (hasFocus) {
			//We need to show the cross icon(if value is improper) at the end when focus is gone from this EditText.
			return;
		}
		switch (v.getId()) {
			case R.id.edtEmail:
				if (edtEmail.getText().toString().length() > 0) {
					if (!isEmailValid) {
						imgEmailIndicator.setImageResource(R.drawable.ic_invalid_cross);
						txtEmailInvalid.setVisibility(View.VISIBLE);
						imgIndicatorStateMap.put(IMG_EMAIL, ImgIndicatorState.IMG_CROSS);
						
					} else {
						imgEmailIndicator.setImageResource(R.drawable.ic_valid_check);
						imgIndicatorStateMap.put(IMG_EMAIL, ImgIndicatorState.IMG_CHECK);
					}
				}
				break;
		}
	}
}
