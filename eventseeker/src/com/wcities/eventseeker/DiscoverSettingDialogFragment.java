package com.wcities.eventseeker;

import java.io.Serializable;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.VersionUtil;

public class DiscoverSettingDialogFragment extends DialogFragment {
	
	private static final String TAG = DiscoverSettingDialogFragment.class.getSimpleName();
	private static final int SEEKBAR_MIN_VAL = 1;
	
	public static DiscoverSettingDialogFragment newInstance(DiscoverSettingChangedListener discoverSettingChangedListener, 
			int year, int month, int day, int miles) {
		DiscoverSettingDialogFragment frag = new DiscoverSettingDialogFragment();
		Bundle args = new Bundle();
		args.putSerializable(BundleKeys.LISTENER, discoverSettingChangedListener);
		args.putInt(BundleKeys.YEAR, year);
		args.putInt(BundleKeys.MONTH, month);
		args.putInt(BundleKeys.DAY, day);
		args.putInt(BundleKeys.MILES, miles);
		frag.setArguments(args);
		return frag;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_AppCompat_Light_NoActionBar);
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Bundle args = getArguments();
		int year = args.getInt(BundleKeys.YEAR);
		int month = args.getInt(BundleKeys.MONTH);
		int day = args.getInt(BundleKeys.DAY);
		int miles = args.getInt(BundleKeys.MILES);
		final DiscoverSettingChangedListener listener = (DiscoverSettingChangedListener) args.getSerializable(BundleKeys.LISTENER);
		
		// create ContextThemeWrapper from the original Activity Context with the custom theme
		Context contextThemeWrapper = new ContextThemeWrapper(FragmentUtil.getActivity(this), R.style.EventSeekr_Dialog_DatePicker);
		/**
		 * clone the inflater using the ContextThemeWrapper
		 * We need this to change font color (textColorPrimary) in datepicker, which we have overwritten 
		 * under main theme in themes.xml. This theme can't be changed once it's applied to an activity
		 * or application unless we are ready to restart activity.  
		 * So we just create clone of inflater with new context having 
		 * default style Theme_AppCompat_Light_NoActionBar but with overwritten textColorPrimary.
		 * So original textColorPrimary is retained for this dialog display.
		 */
		LayoutInflater localInflater = LayoutInflater.from(FragmentUtil.getActivity(this)).cloneInContext(contextThemeWrapper);
		// inflate using the cloned inflater, not the passed in default	
		View v = localInflater.inflate(R.layout.dialog_discover_setting, null);
		
		final TextView txtSelectedMiles = (TextView) v.findViewById(R.id.txtSelectedMiles);
		
		final SeekBar seekBar = (SeekBar) v.findViewById(R.id.seekBar);
		seekBar.setProgress(miles - SEEKBAR_MIN_VAL);
		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				txtSelectedMiles.setText("" + (progress + SEEKBAR_MIN_VAL));
			}
		});
		
		final DatePicker datePicker = (DatePicker) v.findViewById(R.id.datePicker);
		datePicker.init(year, month, day, null);
		if (VersionUtil.isApiLevelAbove10()) {
			// minimum date must be before, not equal to, the current date; hence subtracting 1 second
			datePicker.setMinDate(System.currentTimeMillis() - 1000);
		}
		
		AlertDialog.Builder builder = new AlertDialog.Builder(FragmentUtil.getActivity(this));
		builder.setView(v);
		builder.setPositiveButton(R.string.ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						listener.onSettingChanged(datePicker.getYear(), datePicker.getMonth(), 
								datePicker.getDayOfMonth(), seekBar.getProgress() + SEEKBAR_MIN_VAL);
						//dismiss();
					}
				});
		builder.setNegativeButton(R.string.cancel, null);
		
        // Create the AlertDialog object and return it
        Dialog dialog = builder.create();
        dialog.setTitle(FragmentUtil.getResources(this).getString(R.string.title_dialog_discover_setting));
		return dialog;
	}
	
	@Override
	public void onDestroyView() {
		if (getDialog() != null) {
			getDialog().setDismissMessage(null);
		}
		
		super.onDestroyView();
	}
	
	public interface DiscoverSettingChangedListener extends Serializable {
		public void onSettingChanged(int year, int month, int day, int miles);
	}
}
