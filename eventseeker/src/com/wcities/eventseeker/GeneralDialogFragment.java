package com.wcities.eventseeker;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.util.FragmentUtil;

public class GeneralDialogFragment extends DialogFragment {
	
	public static GeneralDialogFragment newInstance(String title, String msg, String btn1Txt, String btn2Txt) {
		GeneralDialogFragment frag = new GeneralDialogFragment();
        Bundle args = new Bundle();
        args.putString(BundleKeys.DIALOG_TITLE, title);
        args.putString(BundleKeys.DIALOG_MSG, msg);
        args.putString(BundleKeys.BTN1_TXT, btn1Txt);
        args.putString(BundleKeys.BTN2_TXT, btn2Txt);
        frag.setArguments(args);
        return frag;
    }

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Bundle args = getArguments();
		String title = args.getString(BundleKeys.DIALOG_TITLE);
		String msg = args.getString(BundleKeys.DIALOG_MSG);
		final String dialogTag = getTag();
		
		Builder builder = new Builder(FragmentUtil.getActivity(this));
        builder.setTitle(title).setMessage(msg)
        .setNegativeButton(args.getString(BundleKeys.BTN1_TXT), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                ((DialogBtnClickListener)getParentFragment()).doNegativeClick(dialogTag);
            }
        });
        
        if (args.containsKey(BundleKeys.BTN2_TXT)) {
        	builder.setPositiveButton(args.getString(BundleKeys.BTN2_TXT), new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
	                ((DialogBtnClickListener)getParentFragment()).doPositiveClick(dialogTag);
	            }
	        });
        }
        Dialog dialog = builder.create();
		return dialog;
	}
	
	@Override
	public void onDestroyView() {
		if (getDialog() != null) {
			getDialog().setDismissMessage(null);
		}
		super.onDestroyView();
	}
	
	public interface DialogBtnClickListener {
		public void doPositiveClick(String dialogTag);
		public void doNegativeClick(String dialogTag);
	}
}