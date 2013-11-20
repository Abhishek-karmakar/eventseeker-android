package com.wcities.eventseeker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;

public class RepCodeFragment extends FragmentLoadableFromBackStack {
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_rep_code, null);
		return v;
	}
}
