package com.wcities.eventseeker.bosch;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.ReplaceFragmentListener;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.GeoUtil;

public class BoschPlanTravelFragment extends FragmentLoadableFromBackStack implements OnClickListener {
	
	private static final String TAG = BoschPlanTravelFragment.class.getSimpleName();
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (!(activity instanceof ReplaceFragmentListener)) {
			throw new ClassCastException(activity.toString() + " must implement ReplaceFragmentListener");
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_bosch_plan_travel, null);
		view.findViewById(R.id.imgDiscover).setOnClickListener(this);
		view.findViewById(R.id.btnDiscover).setOnClickListener(this);
		view.findViewById(R.id.imgFeatured).setOnClickListener(this);
		view.findViewById(R.id.btnFeatured).setOnClickListener(this);
		view.findViewById(R.id.imgSetting).setOnClickListener(this);
		view.findViewById(R.id.btnSetting).setOnClickListener(this);
		return view;
	}

	@Override
	public void onResume() {
		super.onResume(BoschMainActivity.INDEX_NAV_ITEM_HOME, getResources().getString(R.string.title_plan_travel));
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.imgDiscover:
		case R.id.btnDiscover:
			((ReplaceFragmentListener)FragmentUtil.getActivity(this)).replaceByFragment(
				BoschDiscoverFragment.class.getSimpleName(), null);
			break;
			
		case R.id.imgFeatured:
		case R.id.btnFeatured:
			((ReplaceFragmentListener)FragmentUtil.getActivity(this)).replaceByFragment(
				BoschFeaturedEventsFragment.class.getSimpleName(), null);
			break;
			
		case R.id.imgSetting:
		case R.id.btnSetting:
			
			break;

		default:
			break;
		}
	}
}
