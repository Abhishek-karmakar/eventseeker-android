package com.wcities.eventseeker.bosch;

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
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.util.FbUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class BoschSettingsFragment extends FragmentLoadableFromBackStack implements OnClickListener {

	public static final String LOGIN = "Log In", LOGOUT = "Log Out";

	private static final String TAG = BoschMainActivity.class.getName();

	private TextView txtAccountsResults;

	private Button btnLogIn;

	private boolean isFbLoggedIn;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_bosch_settings, null);

		txtAccountsResults = (TextView) view.findViewById(R.id.txtAccountsResults);

		(btnLogIn = (Button) view.findViewById(R.id.btnLogIn)).setOnClickListener(this);

		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		isFbLoggedIn = FbUtil.hasUserLoggedInBefore(FragmentUtil.getActivity(this).getApplicationContext());
		
		setFBUserName();
		updateLoginBtn();
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
				((BoschMainActivity) FragmentUtil.getActivity(this)).showBoschDialog("Please log in through mobile app.");
			}

			break;
		}
	}

}
