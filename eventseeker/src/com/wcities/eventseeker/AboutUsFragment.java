package com.wcities.eventseeker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;

public class AboutUsFragment extends FragmentLoadableFromBackStack {
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_about_us, null);
	}
}
