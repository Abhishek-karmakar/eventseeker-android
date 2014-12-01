package com.wcities.eventseeker;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
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

public class LoginFragment extends FbGPlusRegisterFragment implements OnClickListener, TextWatcher {
	
	private static final String TAG = LoginFragment.class.getName();
	
	private EditText edtEmail, edtPassword;
	private boolean isEmailValid, isPasswordValid;
	private Button btnLogin;
	
	private ImageView imgFbSignUp, imgGPlusSignIn;
    private TextView txtGPlusSignInStatus;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setRetainInstance(true);
    }
    
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_login, container, false);
		
		(edtEmail = (EditText) v.findViewById(R.id.edtEmail)).addTextChangedListener(this);
		(edtPassword = (EditText) v.findViewById(R.id.edtPassword)).addTextChangedListener(this);

		(btnLogin = (Button) v.findViewById(R.id.btnLogin)).setOnClickListener(this);
		v.findViewById(R.id.btnForgotPassword).setOnClickListener(this);
		
		imgFbSignUp = (ImageView) v.findViewById(R.id.imgFbSignUp);
		imgFbSignUp.setOnClickListener(this);
		
		imgGPlusSignIn = (ImageView) v.findViewById(R.id.imgGPlusSignIn);
		imgGPlusSignIn.setOnClickListener(this);
		txtGPlusSignInStatus = (TextView) v.findViewById(R.id.txtGPlusSignInStatus);
		
		setGooglePlusSigningInVisibility();
		return v;
	}
    
	@Override
	public void onStart() {
		super.onStart();
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		ma.setDrawerLockMode(true);
		ma.setDrawerIndicatorEnabled(false);
		ma.getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_HOME |
				ActionBar.DISPLAY_HOME_AS_UP);
		if (ma.isTabletAndInLandscapeMode()) {
			ma.hideDrawerList();
		}
	}
	
	@Override
	public void onStop() {
		super.onStop();
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
        	
        	((RegistrationListener)FragmentUtil.getActivity(this)).onRegistration(LoginType.emailLogin, bundle, 
        			true);
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
