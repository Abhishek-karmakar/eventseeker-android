package com.wcities.eventseeker;

import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.wcities.eventseeker.util.FieldValidationUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class SignUpFragment extends Fragment implements OnClickListener, TextWatcher, OnFocusChangeListener {

	private static final String TAG = SignUpFragment.class.getSimpleName();
	
	private EditText edtFN, edtLN, edtEmail, edtPassword, edtConfirmPassword;
	private ImageView imgFNIndicator, imgLNIndicator, imgEmailIndicator, imgPasswordIndicator, imgConfirmPasswordIndicator;
	private TextView txtEmailInvalid, txtConfirmPasswordInvalid;
	private boolean isFNValid, isLNValid, isEmailValid, isConfirmPasswordValid;
	private Button btnSignUp;
	
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
		return v;
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
			isConfirmPasswordValid = FieldValidationUtil.isPasswordMatching(edtPassword.getText().toString(), 
					edtConfirmPassword.getText().toString());
			if (isPasswordEntered && isConfirmPasswordEntered && isConfirmPasswordValid) {
				imgPasswordIndicator.setImageResource(R.drawable.ic_valid_check);
				imgConfirmPasswordIndicator.setImageResource(R.drawable.ic_valid_check);
			}
			break;			
		}
		
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
		case R.id.btnSignUp:
			
			break;
		default:
			break;
		}
	}
}
