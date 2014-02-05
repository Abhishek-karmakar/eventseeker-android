package com.wcities.eventseeker.bosch;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.bosch.BoschMainActivity.OnDisplayModeChangedListener;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.util.FbUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.ViewUtil;

public class BoschSettingsFragment extends FragmentLoadableFromBackStack implements OnClickListener, 
		OnDisplayModeChangedListener {

	public static final String LOGIN = "Log In", LOGOUT = "Log Out";

	private static final String TAG = BoschMainActivity.class.getName();

	private TextView txtAccountsResults;

	private Button btnLogIn;

	private boolean isFbLoggedIn;
	
	private Resources res;
	private View parentView;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		res = FragmentUtil.getActivity(this).getResources();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		parentView = inflater.inflate(R.layout.fragment_bosch_settings, null);

		txtAccountsResults = (TextView) parentView.findViewById(R.id.txtAccountsResults);

		(btnLogIn = (Button) parentView.findViewById(R.id.btnLogIn)).setOnClickListener(this);

		updateColors();
		return parentView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		isFbLoggedIn = FbUtil.hasUserLoggedInBefore(FragmentUtil.getActivity(this).getApplicationContext());
		
		setFBUserName();
		updateLoginBtn();
	}

	@Override
	public void onResume() {
		super.onResume(AppConstants.INVALID_INDEX, getResources().getString(R.string.title_settings));
	}
	
	private void updateLoginBtn() {
		Drawable drawable;
		String msg;

		if (isFbLoggedIn) {
			msg = LOGOUT;
			drawable = getResources().getDrawable(R.drawable.slctr_btn_logout);
		} else {
			msg = LOGIN;
			drawable = getResources().getDrawable(R.drawable.slctr_btn_login);
		}

		if (btnLogIn != null) {
			btnLogIn.setText(msg);
			btnLogIn.setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null);
		}
	}

	private void setFBUserName() {
		if (isFbLoggedIn) {
			txtAccountsResults.setVisibility(LinearLayout.VISIBLE);
			txtAccountsResults.setText("Signed in as " + ((EventSeekr) FragmentUtil.getActivity(this).getApplication())
				.getFbUserName());
		} else {			
			txtAccountsResults.setVisibility(LinearLayout.INVISIBLE);
			txtAccountsResults.setText("");
		}
	}
	
	private void updateColors() {
		ViewUtil.updateViewColor(res, parentView);
		if (AppConstants.IS_NIGHT_MODE_ENABLED) {
			txtAccountsResults.setBackgroundColor(res.getColor(R.color.txt_signed_in_as_bg_fragment_preferences_night_mode));
			
		} else {
			txtAccountsResults.setBackgroundColor(res.getColor(R.color.txt_signed_in_as_bg_fragment_preferences));
		}
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {

		case R.id.btnLogIn:

			if (isFbLoggedIn) {
				FbUtil.callFacebookLogout((EventSeekr) FragmentUtil.getActivity(this).getApplication());
				isFbLoggedIn = !isFbLoggedIn;
				
				setFBUserName();
				updateLoginBtn();
			} else {
				((BoschMainActivity) FragmentUtil.getActivity(this))
					.showBoschDialog(getResources().getString(R.string.pls_login_through_mobile_app));
			}

			break;
		}
	}

	@Override
	public void onDisplayModeChanged(boolean isNightModeEnabled) {
		updateColors();
	}
}
