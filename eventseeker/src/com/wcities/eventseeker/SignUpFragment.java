package com.wcities.eventseeker;

import java.io.IOException;
import java.util.HashMap;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.wcities.eventseeker.GeneralDialogFragment.DialogBtnClickListener;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.api.UserInfoApi.LoginType;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.registration.Registration.RegistrationErrorListener;
import com.wcities.eventseeker.core.registration.Registration.RegistrationListener;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser.SignupResponse;
import com.wcities.eventseeker.util.FieldValidationUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class SignUpFragment extends FbGPlusRegisterFragment implements OnClickListener, TextWatcher, OnFocusChangeListener,
		RegistrationErrorListener, DialogBtnClickListener {

	private static final String TAG = SignUpFragment.class.getSimpleName();

	private static final String IMG_FIRST_NAME = "firstName";
	private static final String IMG_LAST_NAME = "lastName";
	private static final String IMG_EMAIL = "email";
	private static final String IMG_PASSWORD = "password";
	private static final String IMG_CONFIRM_PASSWORD = "confirmPassword";

	private static final String DIALOG_FRAGMENT_TAG_UNKNOWN_ERROR = "unknownError";
	
	private EditText edtFN, edtLN, edtEmail, edtPassword, edtConfirmPassword;
	private ImageView imgFNIndicator, imgLNIndicator, imgEmailIndicator, imgPasswordIndicator, imgConfirmPasswordIndicator, 
						imgFbSignUp, imgGPlusSignIn;
	private TextView txtEmailInvalid, txtConfirmPasswordInvalid, txtGPlusSignInStatus;
	private Button btnSignUp;

	private boolean isFNValid, isLNValid, isEmailValid, isConfirmPasswordValid;
	private HashMap<String, ImgIndicatorState> imgIndicatorStateMap;
	private int errorMsgEmail, errorMsgPassword;
	

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
		imgIndicatorStateMap.put(IMG_FIRST_NAME, ImgIndicatorState.IMG_INVISIBLE);
		imgIndicatorStateMap.put(IMG_LAST_NAME, ImgIndicatorState.IMG_INVISIBLE);
		imgIndicatorStateMap.put(IMG_EMAIL, ImgIndicatorState.IMG_INVISIBLE);
		imgIndicatorStateMap.put(IMG_PASSWORD, ImgIndicatorState.IMG_INVISIBLE);
		imgIndicatorStateMap.put(IMG_CONFIRM_PASSWORD, ImgIndicatorState.IMG_INVISIBLE);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		((MainActivity) FragmentUtil.getActivity(this)).setVStatusBarVisibility(View.VISIBLE);
		((MainActivity) FragmentUtil.getActivity(this)).setVStatusBarColor(R.color.colorPrimaryDark);
		
		View v = inflater.inflate(R.layout.fragment_sign_up, null);
		(edtFN = (EditText) v.findViewById(R.id.edtFN)).addTextChangedListener(this);
		(edtLN = (EditText) v.findViewById(R.id.edtLN)).addTextChangedListener(this);
		(edtEmail = (EditText) v.findViewById(R.id.edtEmail)).addTextChangedListener(this);
		(edtPassword = (EditText) v.findViewById(R.id.edtPassword)).addTextChangedListener(this);
		(edtConfirmPassword = (EditText) v.findViewById(R.id.edtConfirmPassword)).addTextChangedListener(this);

		imgFNIndicator = (ImageView) v.findViewById(R.id.imgFNIndicator);
		imgLNIndicator = (ImageView) v.findViewById(R.id.imgLNIndicator);
		imgEmailIndicator = (ImageView) v.findViewById(R.id.imgEmailIndicator);
		imgPasswordIndicator = (ImageView) v.findViewById(R.id.imgPasswordIndicator);
		imgConfirmPasswordIndicator = (ImageView) v.findViewById(R.id.imgConfirmPasswordIndicator);

		edtEmail.setOnFocusChangeListener(this);
		edtPassword.setOnFocusChangeListener(this);
		edtConfirmPassword.setOnFocusChangeListener(this);
		
		txtEmailInvalid = (TextView) v.findViewById(R.id.txtEmailInvalid);
		txtConfirmPasswordInvalid = (TextView) v.findViewById(R.id.txtConfirmPasswordInvalid);
		
		(btnSignUp = (Button) v.findViewById(R.id.btnSignUp)).setOnClickListener(this);
		
		imgFbSignUp = (ImageView) v.findViewById(R.id.imgFbSignUp);
		imgFbSignUp.setOnClickListener(this);
		
		imgGPlusSignIn = (ImageView) v.findViewById(R.id.imgGPlusSignIn);
		imgGPlusSignIn.setOnClickListener(this);
		txtGPlusSignInStatus = (TextView) v.findViewById(R.id.txtGPlusSignInStatus);
		
		setGooglePlusSigningInVisibility();
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (imgIndicatorStateMap.get(IMG_FIRST_NAME) != ImgIndicatorState.IMG_INVISIBLE) {
			imgFNIndicator.setImageResource(imgIndicatorStateMap.get(IMG_FIRST_NAME) == ImgIndicatorState.IMG_CHECK ?
					R.drawable.ic_valid_check : R.drawable.ic_invalid_cross);			
		}
		if (imgIndicatorStateMap.get(IMG_LAST_NAME) != ImgIndicatorState.IMG_INVISIBLE) {
			imgLNIndicator.setImageResource(imgIndicatorStateMap.get(IMG_LAST_NAME) == ImgIndicatorState.IMG_CHECK ?
					R.drawable.ic_valid_check : R.drawable.ic_invalid_cross);			
			
		}
		if (imgIndicatorStateMap.get(IMG_EMAIL) != ImgIndicatorState.IMG_INVISIBLE) {
			try {
				if (imgIndicatorStateMap.get(IMG_EMAIL) == ImgIndicatorState.IMG_CHECK) {
					imgEmailIndicator.setImageResource(R.drawable.ic_valid_check);
					txtEmailInvalid.setText("");
					txtEmailInvalid.setVisibility(View.INVISIBLE);
					
				} else {
					imgEmailIndicator.setImageResource(R.drawable.ic_invalid_cross);
					txtEmailInvalid.setText(errorMsgEmail);
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
		if (imgIndicatorStateMap.get(IMG_CONFIRM_PASSWORD) != ImgIndicatorState.IMG_INVISIBLE) {
			try {
				if (imgIndicatorStateMap.get(IMG_CONFIRM_PASSWORD) == ImgIndicatorState.IMG_CHECK) {
					imgConfirmPasswordIndicator.setImageResource(R.drawable.ic_valid_check);
					txtConfirmPasswordInvalid.setText("");
					txtConfirmPasswordInvalid.setVisibility(View.INVISIBLE);
					
				} else {
					imgConfirmPasswordIndicator.setImageResource(R.drawable.ic_invalid_cross);
					txtConfirmPasswordInvalid.setText(errorMsgPassword);
					txtConfirmPasswordInvalid.setVisibility(View.VISIBLE);
				}
				
			} catch (Resources.NotFoundException e) {
				e.printStackTrace();
			}
			
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		ma.setDrawerLockMode(true);
		ma.setDrawerIndicatorEnabled(false);
		if (ma.isTabletAndInLandscapeMode()) {
			ma.hideDrawerList();
		}
		toggleSignUpBtnState();
	}
	
	@Override
	public void onStop() {
		super.onStop();
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		if (ma.isTabletAndInLandscapeMode()) {
			ma.unHideDrawerList();
		}
	}
	
	@Override
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

	@Override
	public String getScreenName() {
		return "Account Sign Up Screen";
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
		
		case R.id.edtFN:
			isFNValid = FieldValidationUtil.isValidName(edtFN.getText().toString());
			if (isFNValid) {
				imgFNIndicator.setImageResource(R.drawable.ic_valid_check);
				imgIndicatorStateMap.put(IMG_FIRST_NAME, ImgIndicatorState.IMG_CHECK);

			} else {
				imgFNIndicator.setImageResource(0);
				imgIndicatorStateMap.put(IMG_FIRST_NAME, ImgIndicatorState.IMG_INVISIBLE);
			}
			break;

		case R.id.edtLN:
			isLNValid = FieldValidationUtil.isValidName(edtLN.getText().toString());
			if (isLNValid) {
				imgLNIndicator.setImageResource(R.drawable.ic_valid_check);
				imgIndicatorStateMap.put(IMG_LAST_NAME, ImgIndicatorState.IMG_CHECK);

			} else {
				imgLNIndicator.setImageResource(0);
				imgIndicatorStateMap.put(IMG_LAST_NAME, ImgIndicatorState.IMG_INVISIBLE);
			}
			break;
			
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
		case R.id.edtConfirmPassword:
			imgPasswordIndicator.setImageResource(0);
			imgConfirmPasswordIndicator.setImageResource(0);
			txtConfirmPasswordInvalid.setVisibility(View.INVISIBLE);
			imgIndicatorStateMap.put(IMG_PASSWORD, ImgIndicatorState.IMG_INVISIBLE);
			imgIndicatorStateMap.put(IMG_CONFIRM_PASSWORD, ImgIndicatorState.IMG_INVISIBLE);
			
			boolean isPasswordEntered = edtPassword.getText().toString().length() > 0;
			boolean isConfirmPasswordEntered = edtConfirmPassword.getText().toString().length() > 0;
			isConfirmPasswordValid = isPasswordEntered && isConfirmPasswordEntered && 
					FieldValidationUtil.isPasswordMatching(edtPassword.getText().toString(), edtConfirmPassword.getText().toString());
			if (isConfirmPasswordValid) {
				imgPasswordIndicator.setImageResource(R.drawable.ic_valid_check);
				imgConfirmPasswordIndicator.setImageResource(R.drawable.ic_valid_check);
				imgIndicatorStateMap.put(IMG_PASSWORD, ImgIndicatorState.IMG_CHECK);
				imgIndicatorStateMap.put(IMG_CONFIRM_PASSWORD, ImgIndicatorState.IMG_CHECK);
			}
			break;			
		}
		
		toggleSignUpBtnState();
	}

	private void toggleSignUpBtnState() {
		if (isFNValid && isLNValid && isEmailValid && isConfirmPasswordValid) {
			btnSignUp.setEnabled(true);
			
		} else {
			btnSignUp.setEnabled(false);
		}
	}

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
						txtEmailInvalid.setText(errorMsgEmail = R.string.error_email_invalid);
						txtEmailInvalid.setVisibility(View.VISIBLE);
						imgIndicatorStateMap.put(IMG_EMAIL, ImgIndicatorState.IMG_CROSS);
						
					} else {
						new CheckEmail(edtEmail.getText().toString()).execute();
					}
				}
				break;
			
			case R.id.edtPassword:
			case R.id.edtConfirmPassword:
				boolean isPasswordEntered = edtPassword.getText().toString().length() > 0;
				boolean isConfirmPasswordEntered = edtConfirmPassword.getText().toString().length() > 0;
				if(isPasswordEntered && isConfirmPasswordEntered && !isConfirmPasswordValid) {
					imgConfirmPasswordIndicator.setImageResource(R.drawable.ic_invalid_cross);// Password not matching case
					txtConfirmPasswordInvalid.setText(errorMsgPassword = R.string.error_pass_do_not_match);
					txtConfirmPasswordInvalid.setVisibility(View.VISIBLE);
					imgIndicatorStateMap.put(IMG_PASSWORD, ImgIndicatorState.IMG_INVISIBLE);
					imgIndicatorStateMap.put(IMG_CONFIRM_PASSWORD, ImgIndicatorState.IMG_CROSS);
				}	
				break;
		}
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
		
		case R.id.btnSignUp:
			Bundle bundle = new Bundle();
        	bundle.putSerializable(BundleKeys.LOGIN_TYPE, LoginType.emailSignup);
        	bundle.putString(BundleKeys.FIRST_NAME, edtFN.getText().toString());
        	bundle.putString(BundleKeys.LAST_NAME, edtLN.getText().toString());
        	bundle.putString(BundleKeys.EMAIL_ID, edtEmail.getText().toString());
        	bundle.putString(BundleKeys.PASSWORD, edtPassword.getText().toString());
        	bundle.putString(BundleKeys.REGISTER_ERROR_LISTENER, AppConstants.FRAGMENT_TAG_SIGN_UP);
        	
        	((RegistrationListener)FragmentUtil.getActivity(this)).onRegistration(LoginType.emailSignup, bundle, true);
			break;
			
		default:
			break;
		}
	}
	
	private class CheckEmail extends AsyncTask<Void, Void, Integer> {
		
		private String emailId;

		public CheckEmail(String emailId) {
			this.emailId = emailId;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			isEmailValid = false;
			toggleSignUpBtnState();
		}
		
		@Override
		protected Integer doInBackground(Void... params) {
			//Log.d(TAG, "doInBackground()");
			UserInfoApi userInfoApi = new UserInfoApi(Api.OAUTH_TOKEN);
			JSONObject jsonObject;
			try {
				jsonObject = userInfoApi.checkEmail(emailId);
				//Log.d(TAG, jsonObject.toString());
				UserInfoApiJSONParser jsonParser = new UserInfoApiJSONParser();
				SignupResponse signupResponse = jsonParser.parseSignup(jsonObject);
				return signupResponse.getMsgCode();
				
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
		protected void onPostExecute(Integer msgCode) {
			super.onPostExecute(msgCode);
			//Log.d(TAG, "onPostExecute(), msgCode = " + msgCode);
			if (msgCode == UserInfoApiJSONParser.MSG_CODE_EMAIL_ALREADY_EXISTS) {
				imgEmailIndicator.setImageResource(R.drawable.ic_invalid_cross);
				txtEmailInvalid.setText(errorMsgEmail = R.string.error_email_exists);
				txtEmailInvalid.setVisibility(View.VISIBLE);
				imgIndicatorStateMap.put(IMG_EMAIL, ImgIndicatorState.IMG_CROSS);
				
			} else {
				isEmailValid = true;
				imgEmailIndicator.setImageResource(R.drawable.ic_valid_check);
				toggleSignUpBtnState();
			}
		}
	}

	@Override
	public void onErrorOccured(int errorCode) {
		if (errorCode == UserInfoApiJSONParser.MSG_CODE_EMAIL_ALREADY_EXISTS) {
			isEmailValid = false;
			imgEmailIndicator.setImageResource(R.drawable.ic_invalid_cross);
			txtEmailInvalid.setText(errorMsgEmail = R.string.error_email_exists);
			txtEmailInvalid.setVisibility(View.VISIBLE);
			imgIndicatorStateMap.put(IMG_EMAIL, ImgIndicatorState.IMG_CROSS);
		
		} else if (errorCode == UserInfoApiJSONParser.MSG_CODE_NO_ACCESS_TOKEN 
				|| errorCode == UserInfoApiJSONParser.MSG_CODE_UNSUCCESS) {
			GeneralDialogFragment generalDialogFragment = GeneralDialogFragment.newInstance(this,
					FragmentUtil.getResources(this).getString(R.string.error_title), 
					FragmentUtil.getResources(this).getString(R.string.error_unknown_error), "Ok", null);
			generalDialogFragment.show(((ActionBarActivity) FragmentUtil.getActivity(this)).getSupportFragmentManager(), 
					DIALOG_FRAGMENT_TAG_UNKNOWN_ERROR);
			
		}
	}

	@Override
	public void doPositiveClick(String dialogTag) {
		
	}

	@Override
	public void doNegativeClick(String dialogTag) {
		
	}
}
