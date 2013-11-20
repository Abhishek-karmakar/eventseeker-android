package com.wcities.eventseeker;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.DatePicker;

import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.util.FragmentUtil;

public class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {
	
	private static final String TAG = "DatePickerFragment";
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Bundle args = getArguments();
		int year = args.getInt(BundleKeys.YEAR);
		int month = args.getInt(BundleKeys.MONTH);
		int day = args.getInt(BundleKeys.DAY);
		
		// Create a new instance of DatePickerDialog and return it
		DatePickerDialog datePickerDialog = new DatePickerDialog(FragmentUtil.getActivity(this), this, year, month, day); 
		datePickerDialog.setTitle("Set Event Start Date");
		return datePickerDialog;
	}

	public void onDateSet(DatePicker view, int year, int month, int day) {
		// Do something with the date chosen by the user
		((OnDateSelectedListener) getParentFragment()).onDateSelected(year, month, day);
	}
	
	public interface OnDateSelectedListener {
		public void onDateSelected(int year, int month, int day);
	}
}
