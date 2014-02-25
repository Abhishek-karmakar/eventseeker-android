package com.wcities.eventseeker;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;

import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.util.FragmentUtil;

public class GeneralDialogFragment extends DialogFragment {
	
	private static DialogBtnClickListener dialogBtnClickListener;
	
	private static boolean isAlreadyShown;
	
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

	public static GeneralDialogFragment newInstance(DialogBtnClickListener dialogBtnClickListener, 
			String title, String msg, String btn1Txt, String btn2Txt) {
		GeneralDialogFragment.dialogBtnClickListener = dialogBtnClickListener;
		
		GeneralDialogFragment frag = new GeneralDialogFragment();
		Bundle args = new Bundle();
		args.putString(BundleKeys.DIALOG_TITLE, title);
		args.putString(BundleKeys.DIALOG_MSG, msg);
		args.putString(BundleKeys.BTN1_TXT, btn1Txt);
		if (btn2Txt != null) {
			args.putString(BundleKeys.BTN2_TXT, btn2Txt);
		}
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
            	isAlreadyShown = false;
                ((DialogBtnClickListener) getParentFragment()).doNegativeClick(dialogTag);
            }
        });
        
        if (args.containsKey(BundleKeys.BTN2_TXT)) {
        	builder.setPositiveButton(args.getString(BundleKeys.BTN2_TXT), new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
	            	isAlreadyShown = false;
	            	if (getParentFragment() == null) {
	            		dialogBtnClickListener.doPositiveClick(dialogTag);
	            	} else {
	            		((DialogBtnClickListener) getParentFragment()).doPositiveClick(dialogTag);
					}
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
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	@Override
	public void show(FragmentManager manager, String tag) {
		if (!isAlreadyShown) {
			super.show(manager, tag);
			isAlreadyShown = true;
		}
	}
	
	public interface DialogBtnClickListener {
		public void doPositiveClick(String dialogTag);
		public void doNegativeClick(String dialogTag);
	}
}
