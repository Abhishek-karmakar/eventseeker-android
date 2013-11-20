package com.wcities.eventseeker.util;

import java.util.Iterator;
import java.util.List;

import com.wcities.eventseeker.EventDetailsFragment;
import com.wcities.eventseeker.EventFeaturingFragment;
import com.wcities.eventseeker.FeaturedEventsFragment;
import com.wcities.eventseeker.interfaces.ActivityImmediateFragmentLoadableFromBackStack;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;

public class FragmentUtil {

	private static final String TAG = FragmentUtil.class.getName();

	/**
	 * <p>Rather than directly calling getActivity on any fragment, use this function. Because it loops until it reaches immediate
	 * fragment under activity & then finds right activity instance. Otherwise directly calling getActivity() on any
	 * fragment might result in a crash throwing IllegalStateException: Activity has been destroyed.</p> 
	 * 
	 * <p>For instance, crash can be observed in following situation:
	 * Suppose we have an activity with parentfragment = A, having child fragment B. If we want to pass any callback from 
	 * fragment B to activity, use getParentFragment().getActivity().callback() rather than getActivity().callback(), 
	 * otherwise following situation might result in a crash. Load all elements where fragment B is existing. 
	 * Change orientation & execute an event triggering callback. App crashes at this point throwing 
	 * IllegalStateException - Activity has been destroyed.</p>
	 * @param fragment
	 * @return Activity holding this fragment
	 */
	public static Activity getActivity(Fragment fragment) {
		while (fragment.getParentFragment() != null) {
			fragment = fragment.getParentFragment();
		}
		
		if (fragment instanceof ActivityImmediateFragmentLoadableFromBackStack) {
			//Log.d(TAG, "ActivityImmediateFragmentLoadableFromBackStack for " + fragment.getTag());
			return ((ActivityImmediateFragmentLoadableFromBackStack)fragment).getActivityRef();
			
		} else {
			return fragment.getActivity();
		}
	}
	
	public static void updateActivityReferenceInAllFragments(FragmentManager fm, Activity activity) {
		List<Fragment> fragments = fm.getFragments();
		if (fragments != null) {
			
			for (Iterator<Fragment> iterator = fragments.iterator(); iterator.hasNext();) {
				Fragment fragment = (Fragment) iterator.next();
				
				if (fragment != null) {
					//Log.d(TAG, "Found fragment: " + fragment.getTag());
					if (fragment instanceof ActivityImmediateFragmentLoadableFromBackStack) {
						((ActivityImmediateFragmentLoadableFromBackStack)fragment).setActivityRef(activity);
					}
					/*if (fragment instanceof EventDetailsFragment) {
						((EventDetailsFragment)fragment).setActivity(activity);
						
					} else if (fragment instanceof EventFeaturingFragment) {
						((EventFeaturingFragment)fragment).setActivity(activity);
						
					} else if (fragment instanceof FeaturedEventsFragment) {
						((FeaturedEventsFragment)fragment).setActivity(activity);
					}
					updateActivityReferenceInAllFragments(fragment.getChildFragmentManager(), activity);*/
				}
			}
		}
	}
}