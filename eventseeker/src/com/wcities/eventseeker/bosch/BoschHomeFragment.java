package com.wcities.eventseeker.bosch;

import android.app.Activity;
import android.location.Address;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.ReplaceFragmentListener;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.GeoUtil;
import com.wcities.eventseeker.util.GeoUtil.GeoUtilListener;

public class BoschHomeFragment extends FragmentLoadableFromBackStack implements OnClickListener, 
		GeoUtilListener {
	
	private static final String TAG = BoschHomeFragment.class.getSimpleName();
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (!(activity instanceof ReplaceFragmentListener)) {
			throw new ClassCastException(activity.toString() + " must implement ReplaceFragmentListener");
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_bosch_home, null);
		view.findViewById(R.id.btnDiscover).setOnClickListener(this);
		view.findViewById(R.id.btnFeatured).setOnClickListener(this);
		view.findViewById(R.id.btnSetting).setOnClickListener(this);
		return view;
	}

	@Override
	public void onResume() {
		super.onResume(BoschMainActivity.INDEX_NAV_ITEM_HOME, buildTitle(GeoUtil.getCityName(this, 
				(EventSeekr) FragmentUtil.getActivity(this).getApplication())));
	}
	
	private String buildTitle(String cityName) {
		return (cityName == null || cityName.length() == 0) ? "What's up" : "What's up in " + cityName;
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.btnDiscover:
			((ReplaceFragmentListener)FragmentUtil.getActivity(this)).replaceByFragment(
				BoschDiscoverFragment.class.getSimpleName(), null);
			break;
			
		case R.id.btnFeatured:
			((ReplaceFragmentListener)FragmentUtil.getActivity(this)).replaceByFragment(
				BoschFeaturedEventsFragment.class.getSimpleName(), null);
			break;
			
		case R.id.btnSetting:
			((ReplaceFragmentListener)FragmentUtil.getActivity(this)).replaceByFragment(
				BoschSettingsFragment.class.getSimpleName(), null);			
			break;

		default:
			break;
		}
	}

	@Override
	public void onAddressSearchCompleted(String strAddress) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onCitySearchCompleted(String city) {
		if (city != null && city.length() != 0) {
			((BoschMainActivity)FragmentUtil.getActivity(this)).updateTitleForFragment(buildTitle(city), 
					getClass().getSimpleName());
		}
	}

	@Override
	public void onLatlngSearchCompleted(Address address) {
		// TODO Auto-generated method stub
	}
}
