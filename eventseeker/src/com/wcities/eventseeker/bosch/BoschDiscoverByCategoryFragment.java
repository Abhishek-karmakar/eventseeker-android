package com.wcities.eventseeker.bosch;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Category;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.util.FragmentUtil;

public class BoschDiscoverByCategoryFragment extends FragmentLoadableFromBackStack implements OnClickListener {

	private String categoryName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		categoryName = ((Category) getArguments().getSerializable(BundleKeys.CATEGORY)).getName();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_bosch_discover_by_category, null);
		
		BoschDateWiseEventListFragment boschDateWiseEventListFragment = (BoschDateWiseEventListFragment) 
				getChildFragmentManager().findFragmentByTag(BoschDateWiseEventListFragment.class.getSimpleName());
        
        if (boschDateWiseEventListFragment == null) {
        	addBoschDateWiseEventListFragment(getArguments());
        }
        
        view.findViewById(R.id.btnUp).setOnClickListener(this);
		view.findViewById(R.id.btnDown).setOnClickListener(this);
		
		return view;
	}
	
	private void addBoschDateWiseEventListFragment(Bundle bundle) {
    	FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        
        BoschDateWiseEventListFragment boschDateWiseEventListFragment = new BoschDateWiseEventListFragment();
        boschDateWiseEventListFragment.setArguments(bundle);
        fragmentTransaction.add(R.id.lnrLayoutEvtListContainer, boschDateWiseEventListFragment, 
        		BoschDateWiseEventListFragment.class.getSimpleName());
        fragmentTransaction.commit();
    }
	

	@Override
	public void onResume() {
		super.onResume();
		BoschMainActivity activity = (BoschMainActivity) FragmentUtil.getActivity(this);
		String title = activity.getCityName() + " - " + categoryName;
		activity.onFragmentResumed(this, AppConstants.INVALID_INDEX, title);
	}
	
	@Override
	public void onClick(View v) {
		
		switch (v.getId()) {
		
		case R.id.btnUp:
			BoschDateWiseEventListFragment boschDateWiseEventListFragment = (BoschDateWiseEventListFragment) 
				getChildFragmentManager().findFragmentByTag(BoschDateWiseEventListFragment.class.getSimpleName());
			boschDateWiseEventListFragment.scrollUp();
			break;
			
		case R.id.btnDown:
			boschDateWiseEventListFragment = (BoschDateWiseEventListFragment) getChildFragmentManager()
				.findFragmentByTag(BoschDateWiseEventListFragment.class.getSimpleName());
			boschDateWiseEventListFragment.scrollDown();
			break;

		default:
			break;
		}
	}
}
