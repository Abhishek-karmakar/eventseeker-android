package com.wcities.eventseeker;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import com.wcities.eventseeker.util.FragmentUtil;

public class ShareOnFBDialogFragment extends DialogFragment {
	
	private static final String TAG = ShareOnFBDialogFragment.class.getSimpleName();

	private static OnFacebookShareClickedListener onFacebookShareClickedListener;
	
	private static boolean isAlreadyShown;
	
	public static ShareOnFBDialogFragment newInstance(OnFacebookShareClickedListener onFacebookShareClickedListener) {
		ShareOnFBDialogFragment.onFacebookShareClickedListener = onFacebookShareClickedListener;
		return new ShareOnFBDialogFragment();
	}

	private Button btnFBShare;  
	  
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final Dialog dialog = new Dialog(FragmentUtil.getActivity(this));
		dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		dialog.setCancelable(true);
		dialog.setContentView(R.layout.fragment_share_on_fb_dialog);
		//dialog.show();
		
		final String dialogTag = getTag();
		
		btnFBShare = (Button) dialog.findViewById(R.id.btnFbShare);
		btnFBShare.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				onFacebookShareClickedListener.onFacebookShareClicked(dialogTag);
				dismiss();
			}
		});
		
		return dialog;
	}
	
	/*@Override
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
	}*/
	
	@Override
	public void onDestroyView() {
		isAlreadyShown = false;
		if (getDialog() != null) {
			getDialog().setDismissMessage(null);
		}
		super.onDestroyView();
	}
	
	@Override
	public void show(FragmentManager manager, String tag) {
		if (!isAlreadyShown) {
			super.show(manager, tag);
			isAlreadyShown = true;
		}
	}
	
	public interface OnFacebookShareClickedListener {
		public void  onFacebookShareClicked(String dialogTag);
	}
}
