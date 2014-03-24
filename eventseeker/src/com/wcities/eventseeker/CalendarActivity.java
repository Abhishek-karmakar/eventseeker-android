package com.wcities.eventseeker;

import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Event;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.Toast;

public class CalendarActivity extends ActionBarActivity {

	private static final String TAG = CalendarActivity.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);

	    SelectCalendarFragment selectCalendarFragment = (SelectCalendarFragment) getSupportFragmentManager()
	    		.findFragmentByTag(SelectCalendarFragment.class.getSimpleName());
	    if (selectCalendarFragment == null) {
	    	//Log.d(TAG, "selectCalendarFragment == null");
	    	Intent intent = getIntent();
		    Event eventToAddToCalendar = ((EventSeekr)getApplication()).getEventToAddToCalendar();
		    if (intent.getType().indexOf("image/") != -1 && eventToAddToCalendar != null) {
		    	selectCalendarFragment = new SelectCalendarFragment();
		    	Bundle args = new Bundle();
		    	args.putSerializable(BundleKeys.EVENT, eventToAddToCalendar);
		    	selectCalendarFragment.setArguments(args);
		    	selectCalendarFragment.show(getSupportFragmentManager(), SelectCalendarFragment.class.getSimpleName());
		    	
		    } else {
		    	Toast.makeText(this, R.string.event_not_found, Toast.LENGTH_LONG).show();
		    	finish();
		    }
	    }
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		//Log.d(TAG, "onDestroy()");
		((EventSeekr)getApplication()).setEventToAddToCalendar(null);
	}
}
