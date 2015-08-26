package com.wcities.eventseeker;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

import com.wcities.eventseeker.util.FragmentUtil;

import java.io.Serializable;

public class RadioGroupDialogFragment extends DialogFragment implements OnCheckedChangeListener {

	//private static boolean isShowing;

	public static final String ON_VALUE_SELECTED_LISETER = "onValueSelectedListener";
	public static final String DEFAULT_VALUE = "defaultValue";
	public static final String DIALOG_TITLE = "dialogTitle";
	public static final String DIALOG_RDB_ZEROTH_TEXT = "rdbZerothTxt";
	public static final String DIALOG_RDB_FIRST_TEXT = "rdbFirstTxt";

	private int defaultValue = 0;
	private OnValueSelectedListener onValueSelectedListener;
	
	public interface OnValueSelectedListener extends Serializable {
		public void onValueSelected(int selectedValue);
	}
			
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		onValueSelectedListener = (OnValueSelectedListener) getArguments().getSerializable(ON_VALUE_SELECTED_LISETER);
		defaultValue = getArguments().getInt(DEFAULT_VALUE);
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		View v = LayoutInflater.from(FragmentUtil.getActivity(this)).inflate(R.layout.fragment_radio_group_dialog, null);

		setupViews(v);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setView(v);
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dismiss();
				}
			});
        // Create the AlertDialog object and return it
        Dialog dialog = builder.create();
        dialog.setTitle(getArguments().getString(DIALOG_TITLE));
        dialog.setCancelable(false);
		return dialog;
	}
	
	private void setupViews(View v) {
		RadioButton rdbZeroth = (RadioButton) v.findViewById(R.id.rdbZeroth);
		rdbZeroth.setText(getArguments().getString(DIALOG_RDB_ZEROTH_TEXT));
		rdbZeroth.setChecked(defaultValue == 0);

		RadioButton rdbFirst = (RadioButton) v.findViewById(R.id.rdbFirst);
		rdbFirst.setText(getArguments().getString(DIALOG_RDB_FIRST_TEXT));
		rdbFirst.setChecked(defaultValue == 1);
		
		RadioGroup rdg = (RadioGroup) v.findViewById(R.id.rdgDialog);
		rdg.setOnCheckedChangeListener(this);
	}

	/*@Override
	public void show(FragmentManager manager, String tag) {
		if (!isShowing) {
			isShowing = true;
			super.show(manager, tag);
		}
	}*/

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		defaultValue = (checkedId == R.id.rdbZeroth) ? 0 : 1;
		onValueSelectedListener.onValueSelected(defaultValue);
		
		//isShowing = false;
		dismiss();
	}
	
}
