package com.wcities.eventseeker.interfaces;

import android.support.v4.app.Fragment;

public interface FragmentLoadedFromBackstackListener {
	public void onFragmentResumed(Fragment fragment);
	public void onFragmentResumed(Fragment fragment, int drawerPosition, String actionBarTitle);
}
