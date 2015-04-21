package com.wcities.eventseeker.interfaces;

import android.os.Bundle;

public interface DrawerListFragmentListener {
	public void onDrawerListFragmentViewCreated();
	public void onDrawerItemSelected(int pos, Bundle args);
}
