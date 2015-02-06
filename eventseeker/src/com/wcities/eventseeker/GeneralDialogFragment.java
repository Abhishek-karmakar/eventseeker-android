package com.wcities.eventseeker;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.Gravity;
import android.widget.Button;

import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.util.FragmentUtil;

public class GeneralDialogFragment extends DialogFragment {
	
	private static final String TAG = GeneralDialogFragment.class.getSimpleName();

	private static DialogBtnClickListener dialogBtnClickListener;
	
	private static boolean isAlreadyShown;
	
	public static GeneralDialogFragment newInstance(DialogBtnClickListener dialogBtnClickListener, String title, 
			String msg) {
		GeneralDialogFragment.dialogBtnClickListener = dialogBtnClickListener;
		
		GeneralDialogFragment frag = new GeneralDialogFragment();
		Bundle args = new Bundle();
		args.putString(BundleKeys.DIALOG_TITLE, title);
		args.putString(BundleKeys.DIALOG_MSG, msg);
		args.putString(BundleKeys.BTN1_TXT, " ");
		args.putBoolean(BundleKeys.DIALOG_FB_SHARE, true);
		frag.setArguments(args);
		
		return frag;
	}

	public static GeneralDialogFragment newInstance(DialogBtnClickListener dialogBtnClickListener, String msg, 
			String btn1Txt, boolean isCancellable) {
		GeneralDialogFragment.dialogBtnClickListener = dialogBtnClickListener;
		
		GeneralDialogFragment frag = new GeneralDialogFragment();
		Bundle args = new Bundle();
		args.putString(BundleKeys.DIALOG_MSG, msg);
		args.putString(BundleKeys.BTN1_TXT, btn1Txt);
		args.putBoolean(BundleKeys.DIALOG_IS_CANCELLABLE, isCancellable);
		frag.setArguments(args);
		return frag;
	}

	public static GeneralDialogFragment newInstance(DialogBtnClickListener dialogBtnClickListener, 
			String title, String msg, String btn1Txt, String btn2Txt, boolean isCancellable) {
		GeneralDialogFragment.dialogBtnClickListener = dialogBtnClickListener;
		
		GeneralDialogFragment frag = new GeneralDialogFragment();
		Bundle args = new Bundle();
		args.putString(BundleKeys.DIALOG_TITLE, title);
		args.putString(BundleKeys.DIALOG_MSG, msg);
		args.putString(BundleKeys.BTN1_TXT, btn1Txt);
		if (btn2Txt != null) {
			args.putString(BundleKeys.BTN2_TXT, btn2Txt);
		}
		args.putBoolean(BundleKeys.DIALOG_IS_CANCELLABLE, isCancellable);
		frag.setArguments(args);
		return frag;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Bundle args = getArguments();
		String title = null;
		if (args.containsKey(BundleKeys.DIALOG_TITLE)) {
			title = args.getString(BundleKeys.DIALOG_TITLE);
		}
		String msg = args.getString(BundleKeys.DIALOG_MSG);
		final String dialogTag = getTag();
		
		Builder builder = new Builder(FragmentUtil.getActivity(this));
		if (title != null) {
			builder.setTitle(title);
		}
        builder.setMessage(msg)
        .setNegativeButton(args.getString(BundleKeys.BTN1_TXT), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            	//isAlreadyShown = false;
            	if (dialogBtnClickListener != null) {
            		dialogBtnClickListener.doNegativeClick(dialogTag);
            		
            	} else {
            		((DialogBtnClickListener) getParentFragment()).doNegativeClick(dialogTag);
            	}
            }
        });
        
        if (args.containsKey(BundleKeys.BTN2_TXT)) {
        	builder.setPositiveButton(args.getString(BundleKeys.BTN2_TXT), new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
	            	//isAlreadyShown = false;
	            	if (dialogBtnClickListener != null) {
	            		dialogBtnClickListener.doPositiveClick(dialogTag);
	            		
	            	} else {
	            		((DialogBtnClickListener) getParentFragment()).doPositiveClick(dialogTag);
					}
	            }
	        });
        }
        Dialog dialog = builder.create();
        setCancelable(args.getBoolean(BundleKeys.DIALOG_IS_CANCELLABLE, true));
		return dialog;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		if (getArguments().getBoolean(BundleKeys.DIALOG_FB_SHARE, false)) {
			Button btnFBShare = ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_NEGATIVE);
			btnFBShare.setBackgroundResource(R.drawable.ic_fb_continue);
			btnFBShare.setText(R.string.share_on_facebook);
			btnFBShare.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
			btnFBShare.setSingleLine(true);
			btnFBShare.setTextColor(getResources().getColor(android.R.color.white));
			btnFBShare.setTextSize(getResources().getDimensionPixelSize(R.dimen.txt_size_fb_dialog));
		}
	}
	
	@Override
	public void onDestroyView() {
		isAlreadyShown = false;
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
