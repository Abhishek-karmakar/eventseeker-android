package com.wcities.eventseeker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.util.FragmentUtil;

public class SportsArtistsFragment extends FragmentLoadableFromBackStack implements View.OnClickListener {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		((MainActivity) FragmentUtil.getActivity(this)).setVStatusBarColor(R.color.colorPrimaryDark);
		((MainActivity) FragmentUtil.getActivity(this)).setVStatusBarVisibility(View.VISIBLE);
		
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_popular_artists, null);
		
		view.findViewById(R.id.btnFeatured).setOnClickListener(this);
		view.findViewById(R.id.btnMusic).setOnClickListener(this);
		view.findViewById(R.id.btnComedy).setOnClickListener(this);
		view.findViewById(R.id.btnTheater).setOnClickListener(this);
		view.findViewById(R.id.btnSports).setOnClickListener(this);
		
		return view;
	}
	
	@Override
	public String getScreenName() {
		// TODO Auto-generated method stub
		return "";
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.btnFeatured:
				break;
				
			case R.id.btnMusic:				
				break;
				
			case R.id.btnComedy:
				break;
				
			case R.id.btnTheater:
				break;
				
			case R.id.btnSports:
				break;
		}
	}

}
