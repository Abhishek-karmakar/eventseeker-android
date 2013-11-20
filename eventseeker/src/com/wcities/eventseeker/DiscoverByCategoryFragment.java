package com.wcities.eventseeker;

import java.util.Calendar;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.wcities.eventseeker.DatePickerFragment.OnDateSelectedListener;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.util.ConversionUtil;

public class DiscoverByCategoryFragment extends FragmentLoadableFromBackStack implements OnDateSelectedListener {
	
	private static final String TAG = DiscoverByCategoryFragment.class.getName();

	private int year, month, day;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		setRetainInstance(true);
	}
	
    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_discover_by_category, container, false);
        
        if (year == 0) {
        	// initialize year, month, day values for calendar action item
      		Calendar c = Calendar.getInstance();
      		year = c.get(Calendar.YEAR);
      		month = c.get(Calendar.MONTH);
      		day = c.get(Calendar.DAY_OF_MONTH);
        }
        
		DateWiseEventListFragment dateWiseEventListFragment = (DateWiseEventListFragment) 
				getChildFragmentManager().findFragmentByTag(AppConstants.FRAGMENT_TAG_DATE_WISE_EVENT_LIST);
        if (dateWiseEventListFragment == null) {
        	addDateWiseEventListFragment(getArguments());
        }
        return v;
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    	inflater.inflate(R.menu.fragment_discover_by_category, menu);
    	super.onCreateOptionsMenu(menu, inflater);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	
		case R.id.action_calendar:
			DialogFragment newFragment = new DatePickerFragment();
			
			Bundle args = new Bundle();
			args.putInt(BundleKeys.YEAR, year);
			args.putInt(BundleKeys.MONTH, month);
			args.putInt(BundleKeys.DAY, day);
			newFragment.setArguments(args);
			
		    newFragment.show(getChildFragmentManager(), "datePicker");
			return true;

		default:
			break;
		}
    	return false;
    }
    
    private void addDateWiseEventListFragment(Bundle bundle) {
    	FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        
        DateWiseEventListFragment dateWiseEventListFragment = new DateWiseEventListFragment();
        dateWiseEventListFragment.setArguments(bundle);
        fragmentTransaction.add(R.id.lnrLayoutEvtListContainer, dateWiseEventListFragment, AppConstants.FRAGMENT_TAG_DATE_WISE_EVENT_LIST);
        fragmentTransaction.commit();
    }
    
	@Override
	public void onDateSelected(int year, int month, int day) {
		this.year = year;
		this.month = month;
		this.day = day;
		
	    String startDate = ConversionUtil.getDay(year, month, day);
		DateWiseEventListFragment dateWiseEventListFragment = (DateWiseEventListFragment) 
				getChildFragmentManager().findFragmentByTag(AppConstants.FRAGMENT_TAG_DATE_WISE_EVENT_LIST);
		dateWiseEventListFragment.resetWith(startDate);
	}
}
