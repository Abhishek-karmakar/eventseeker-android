package com.wcities.eventseeker.bosch;

import android.support.v4.app.ListFragment;

import com.wcities.eventseeker.api.UserInfoApi.Type;

public class BoschMyEventsListFragment extends ListFragment {

	public static String getTag(Type loadType) {
		return BoschMyEventsListFragment.class.getSimpleName() + loadType.name(); 
	}
}
