package com.wcities.eventseeker.util;

import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.wcities.eventseeker.GeneralDialogFragment;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.analytics.IGoogleAnalyticsTracker;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.interfaces.ActivityImmediateFragmentLoadableFromBackStack;

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
	
	public static Resources getResources(Fragment fragment) {
		return getActivity(fragment).getResources();
	}
	
	public static EventSeekr getApplication(Fragment fragment) {
		return (EventSeekr) getActivity(fragment).getApplication();
	}
	
	public static Fragment getTopLevelParentFragment(Fragment fragment) {
		while (fragment.getParentFragment() != null) {
			fragment = fragment.getParentFragment();
		}
		return fragment;
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
				}
			}
		}
	}
	
	public static void showLoginNeededForTrackingEventDialog(FragmentManager fm, Context context) {
		Resources res = context.getResources();
		GeneralDialogFragment generalDialogFragment = GeneralDialogFragment.newInstance(
				res.getString(R.string.go_to_login), res.getString(R.string.pls_login_to_track_evt), 
				res.getString(R.string.cancel), res.getString(R.string.yes));
		generalDialogFragment.show(fm, AppConstants.DIALOG_FRAGMENT_TAG_LOGIN_TO_TRACK_EVENT);
	}
	
	public static String getScreenName(Fragment fragment) {
		while ((fragment != null && !(fragment instanceof IGoogleAnalyticsTracker))) {
			fragment = fragment.getParentFragment();
		}
		
		if (fragment != null && fragment instanceof IGoogleAnalyticsTracker) {
			return ((IGoogleAnalyticsTracker)fragment).getScreenName();
		}
		return null;
	}
}
