package com.wcities.eventseeker.custom.fragment;

import android.app.Activity;
import android.support.v4.app.ListFragment;

import com.wcities.eventseeker.MainActivity;
import com.wcities.eventseeker.interfaces.ActivityImmediateFragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.FragmentLoadedFromBackstackListener;
import com.wcities.eventseeker.util.FragmentUtil;

/**
 * Its purpose is to update screen actionbar when subclass of this fragment is
 * resumed (loaded from backstack)
 * 
 * @author win6
 */
public class ListFragmentLoadableFromBackStack extends ListFragment implements
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
