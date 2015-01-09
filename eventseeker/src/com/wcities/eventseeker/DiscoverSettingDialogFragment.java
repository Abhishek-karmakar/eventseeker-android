package com.wcities.eventseeker;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.DatePicker;

import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.util.FragmentUtil;

public class DiscoverSettingDialogFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {
	
	private static final String TAG = DiscoverSettingDialogFragment.class.getSimpleName();
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Bundle args = getArguments();
		int year = args.getInt(BundleKeys.YEAR);
		int month = args.getInt(BundleKeys.MONTH);
		int day = args.getInt(BundleKeys.DAY);
		
		// Create a new instance of DatePickerDialog and return it
		DatePickerDialog datePickerDialog = new DatePickerDialog(FragmentUtil.getActivity(this), this, year, month, day); 
		datePickerDialog.setTitle(FragmentUtil.getResources(this).getString(R.string.set_start_date));
		return datePickerDialog;
	}

	public void onDateSet(DatePicker view, int year, int month, int day) {
		// Do something with the date chosen by the user
		((OnDateSelectedListener) getParentFragment()).onDateSelected(year, month, day);
	}
	
	@Override
	public void onDestroyView() {
		if (getDialog() != null) {
			getDialog().setDismissMessage(null);
		}
		super.onDestroyView();
	}
	
	public interface OnDateSelectedListener {
		public void onDateSelected(int year, int month, int day);
	}
}
