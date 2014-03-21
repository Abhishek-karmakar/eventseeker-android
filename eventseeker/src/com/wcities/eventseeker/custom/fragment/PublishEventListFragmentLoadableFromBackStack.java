package com.wcities.eventseeker.custom.fragment;

import android.app.Activity;

import com.wcities.eventseeker.MainActivity;
import com.wcities.eventseeker.PublishEventListFragment;
import com.wcities.eventseeker.bosch.BoschMainActivity;
import com.wcities.eventseeker.interfaces.ActivityImmediateFragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.FragmentLoadedFromBackstackListener;
import com.wcities.eventseeker.util.FragmentUtil;

public abstract class PublishEventListFragmentLoadableFromBackStack extends PublishEventListFragment implements 
		ActivityImmediateFragmentLoadableFromBackStack {

	private Activity activityRef;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (!(activity instanceof FragmentLoadedFromBackstackListener)) {
			throw new ClassCastException(activity.toString()
					+ " must implement FragmentLoadedFromBackstackListener");
		}
		activityRef = activity;
	}

	@Override
	public void onResume() {
		super.onResume();
		
		Activity activity = FragmentUtil.getActivity(this);
		
		if (activity instanceof FragmentLoadedFromBackstackListener) {
			if (activity instanceof MainActivity) {
				((FragmentLoadedFromBackstackListener)activity).onFragmentResumed(this);
				
			} else if (activity instanceof BoschMainActivity) {
				// it's handled from within child fragment, so nothing here
			}
		}
	}
	
	public void onResume(int drawerPosition, String actionBarTitle) {
		super.onResume();
		
		Activity activity = FragmentUtil.getActivity(this);
		
		if (activity instanceof FragmentLoadedFromBackstackListener) {
			if (activity instanceof BoschMainActivity) {
				((FragmentLoadedFromBackstackListener)activity).onFragmentResumed(this, drawerPosition, 
						actionBarTitle);
				
			} else if (activity instanceof MainActivity) {
				// not yet called for this case ever
			}
		}
	}
	
	@Override
	public void setActivityRef(Activity activity) {
		activityRef = activity;
	}

	@Override
	public Activity getActivityRef() {
		return activityRef;
	}
}