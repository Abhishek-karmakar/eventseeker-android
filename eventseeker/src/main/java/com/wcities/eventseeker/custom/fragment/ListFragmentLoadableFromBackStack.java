package com.wcities.eventseeker.custom.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;

import com.wcities.eventseeker.MainActivity;
import com.wcities.eventseeker.analytics.GoogleAnalyticsTracker;
import com.wcities.eventseeker.analytics.IGoogleAnalyticsTracker;
import com.wcities.eventseeker.bosch.BoschMainActivity;
import com.wcities.eventseeker.interfaces.ActivityImmediateFragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.FragmentLoadedFromBackstackListener;
import com.wcities.eventseeker.util.FragmentUtil;

/**
 * Its purpose is to update screen actionbar when subclass of this fragment is
 * resumed (loaded from backstack)
 * 
 * @author win6
 */
public abstract class ListFragmentLoadableFromBackStack extends ListFragment implements
		ActivityImmediateFragmentLoadableFromBackStack, IGoogleAnalyticsTracker {

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
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		GoogleAnalyticsTracker.getInstance().sendScreenView(FragmentUtil.getApplication(this), getScreenName());
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
		/**
		 * Currently this is been implemented for ConnectAccountsActivity
		 */
		if (activity instanceof FragmentLoadedFromBackstackListener) {
			((FragmentLoadedFromBackstackListener)activity).onFragmentResumed(this, drawerPosition, 
				actionBarTitle);
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
