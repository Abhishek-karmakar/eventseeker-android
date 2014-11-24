package com.wcities.eventseeker;

import android.os.Bundle;
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

import com.wcities.eventseeker.api.UserInfoApi.LoginType;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.registration.Registration.RegistrationListener;
import com.wcities.eventseeker.util.FieldValidationUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class SignUpFragment extends FbGPlusRegisterFragment implements OnClickListener, TextWatcher, OnFocusChangeListener {

	private static final String TAG = SignUpFragment.class.getSimpleName();
	
	private EditText edtFN, edtLN, edtEmail, edtPassword, edtConfirmPassword;
	private ImageView imgFNIndicator, imgLNIndicator, imgEmailIndicator, imgPasswordIndicator, imgConfirmPasswordIndicator;
	private TextView txtEmailInvalid, txtConfirmPasswordInvalid;
	private boolean isFNValid, isLNValid, isEmailValid, isConfirmPasswordValid;
	private Button btnSignUp;
	
	private ImageView imgFbSignUp, imgGPlusSignIn;
    private TextView txtGPlusSignInStatus;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
	public void onResume() {
		super.onResume();
		toggleSignUpBtnState();
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

			} else {
				imgFNIndicator.setImageResource(0);
			}
			break;

		case R.id.edtLN:
			isLNValid = FieldValidationUtil.isValidName(edtLN.getText().toString());
			if (isLNValid) {
				imgLNIndicator.setImageResource(R.drawable.ic_valid_check);

			} else {
				imgLNIndicator.setImageResource(0);
			}
			break;
			
		case R.id.edtEmail:
			// to remove the error icon if any present on it.
			imgEmailIndicator.setImageResource(0);
			txtEmailInvalid.setVisibility(View.INVISIBLE);

			isEmailValid = FieldValidationUtil.isValidEmail(edtEmail.getText().toString());
			if (isEmailValid) {
				imgEmailIndicator.setImageResource(R.drawable.ic_valid_check);
			} 
			break;
			
		case R.id.edtPassword:
		case R.id.edtConfirmPassword:
			imgPasswordIndicator.setImageResource(0);
			imgConfirmPasswordIndicator.setImageResource(0);
			txtConfirmPasswordInvalid.setVisibility(View.INVISIBLE);
			
			boolean isPasswordEntered = edtPassword.getText().toString().length() > 0;
			boolean isConfirmPasswordEntered = edtConfirmPassword.getText().toString().length() > 0;
			isConfirmPasswordValid = isPasswordEntered && isConfirmPasswordEntered && 
					FieldValidationUtil.isPasswordMatching(edtPassword.getText().toString(), edtConfirmPassword.getText().toString());
			if (isConfirmPasswordValid) {
				imgPasswordIndicator.setImageResource(R.drawable.ic_valid_check);
				imgConfirmPasswordIndicator.setImageResource(R.drawable.ic_valid_check);
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
		if (hasFocus) {
			//We need to show the cross icon(if value is improper) at the end when focus is gone from this EditText.
			return;
		}
		switch (v.getId()) {
			case R.id.edtEmail:
				if (edtEmail.getText().toString().length() > 0 && !isEmailValid) {
					imgEmailIndicator.setImageResource(R.drawable.ic_invalid_cross);
					txtEmailInvalid.setVisibility(View.VISIBLE);
				}
				break;
			
			case R.id.edtPassword:
			case R.id.edtConfirmPassword:
				boolean isPasswordEntered = edtPassword.getText().toString().length() > 0;
				boolean isConfirmPasswordEntered = edtConfirmPassword.getText().toString().length() > 0;
				if(isPasswordEntered && isConfirmPasswordEntered && !isConfirmPasswordValid) {
					imgConfirmPasswordIndicator.setImageResource(R.drawable.ic_invalid_cross);// Password not matching case
					txtConfirmPasswordInvalid.setVisibility(View.VISIBLE);
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
        	
        	((RegistrationListener)FragmentUtil.getActivity(this)).onRegistration(LoginType.emailSignup, bundle, 
        			true);
			break;
			
		default:
			break;
		}
	}
}