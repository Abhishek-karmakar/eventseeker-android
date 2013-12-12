package com.wcities.eventseeker;

import java.util.Iterator;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.BookingInfo;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.util.FragmentUtil;

public class TicketProviderDialogFragment extends DialogFragment {
	
	protected static final String TAG = TicketProviderDialogFragment.class.getName();

	public static TicketProviderDialogFragment newInstance(Event event) {
		TicketProviderDialogFragment fragment = new TicketProviderDialogFragment();
		Bundle bundle = new Bundle();
		bundle.putSerializable(BundleKeys.EVENT, event);
		fragment.setArguments(bundle);
        return fragment;
    }
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Event event = (Event) getArguments().getSerializable(BundleKeys.EVENT);
		final List<BookingInfo> bookingInfos = event.getSchedule().getBookingInfos();
		String[] items = new String[bookingInfos.size()];

		int i = 0;
		for (Iterator<BookingInfo> iterator = bookingInfos.iterator(); iterator.hasNext();) {
			BookingInfo bookingInfo = iterator.next();
			items[i++] = bookingInfo.getProvider();
		}
		return new AlertDialog.Builder(FragmentUtil.getActivity(this))
        .setTitle("Select Ticket Provider")
        .setItems(items, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(bookingInfos.get(which)
						.getBookingUrl()));
				startActivity(browserIntent);
				dismiss();
			}
		})
        .create();
	}
	
	@Override
    public void onDestroyView() {
    	/**
    	 * We add this code to stop our dialog from being dismissed on rotation, 
    	 * due to a bug with the compatibility library.
    	 * If it's a top level fragment then we need to update this if condition as
    	 * "if (getDialog() != null && getRetainInstance())"
    	 */
        if (getDialog() != null) {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }
}
