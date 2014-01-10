package com.wcities.eventseeker;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.wcities.eventseeker.DatePickerFragment.OnDateSelectedListener;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.FragmentUtil;

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
        
        DateWiseEventListParentFragment dateWiseEventListFragment = (DateWiseEventListParentFragment) 
				getChildFragmentManager().findFragmentByTag(AppConstants.FRAGMENT_TAG_DATE_WISE_EVENT_LIST);
        
        if (dateWiseEventListFragment == null) {
        	addDateWiseEventListFragment(getArguments());
        }
        return v;
    }
    
    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (((EventSeekr)FragmentUtil.getActivity(this).getApplication()).isTablet()) {
        	Fragment fragment = getChildFragmentManager().findFragmentByTag(AppConstants.FRAGMENT_TAG_DATE_WISE_EVENT_LIST);
        	if (fragment != null) {
        		fragment.onActivityResult(requestCode, resultCode, data);
        	}
        }
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
        
        DateWiseEventListParentFragment dateWiseEventListFragment;
        if(((MainActivity)FragmentUtil.getActivity(this)).isTablet()) {
        	dateWiseEventListFragment = new DateWiseEventListFragmentTab();
        } else {
        	dateWiseEventListFragment = new DateWiseEventListFragment();
		}
        
        dateWiseEventListFragment.setArguments(bundle);
        fragmentTransaction.add(R.id.lnrLayoutEvtListContainer, dateWiseEventListFragment, AppConstants.FRAGMENT_TAG_DATE_WISE_EVENT_LIST);
        fragmentTransaction.commit();
    }
    
	@Override
	public void onDateSelected(int year, int month, int day) {
		this.year = year;
		this.month = month;
		this.day = day;
		Calendar calendar = new GregorianCalendar(year, month, day);
	    String startDate = ConversionUtil.getDay(calendar);
	    calendar.add(Calendar.YEAR, 1);
	    String endDate = ConversionUtil.getDay(calendar);
	    
	    DateWiseEventListParentFragment dateWiseEventListFragment = (DateWiseEventListParentFragment) 
				getChildFragmentManager().findFragmentByTag(AppConstants.FRAGMENT_TAG_DATE_WISE_EVENT_LIST);
		
		dateWiseEventListFragment.resetWith(startDate, endDate);
	}
}
