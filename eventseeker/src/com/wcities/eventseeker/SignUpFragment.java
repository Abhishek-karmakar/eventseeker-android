package com.wcities.eventseeker;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.util.FragmentUtil;

public class SignUpFragment extends FragmentLoadableFromBackStack {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}
	
	@Override
    public void onStart() {
        super.onStart();
        
        MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		ma.getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_HOME 
				| ActionBar.DISPLAY_HOME_AS_UP);
    }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_sign_up, null);
		return v;
	}

	@Override
	public String getScreenName() {
		return "Account Sign Up Screen";
	}
}
