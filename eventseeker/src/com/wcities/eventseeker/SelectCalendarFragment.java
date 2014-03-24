package com.wcities.eventseeker;

import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Date;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Schedule;
import com.wcities.eventseeker.util.FragmentUtil;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

public class SelectCalendarFragment extends DialogFragment {
	
	protected static final String TAG = SelectCalendarFragment.class.getSimpleName();
	
	private static final String CALENDARS_COLUMN_ID = "_id";
	private static final String CALENDARS_COLUMN_NAME = "name";
	
	private static final long MILLIS_FOR_1_hr = 60 * 60 * 1000;
	private static final int DEFAULT_START_HRS = 9;
	
	private Event event;

	private Resources res;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		res = getResources();
		if (event == null) {
			event = (Event) getArguments().getSerializable(BundleKeys.EVENT);
		}
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final ContentResolver contentResolver = FragmentUtil.getActivity(this).getContentResolver();
		String[] projection = new String[] {CALENDARS_COLUMN_ID, CALENDARS_COLUMN_NAME};
	    Uri calendars = Uri.parse("content://com.android.calendar/calendars");
	         
	    final Cursor cursor = contentResolver.query(calendars, projection, null, null, null);

		Builder builder = new AlertDialog.Builder(FragmentUtil.getActivity(this));
		builder.setCursor(cursor, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String msg;
				if (event.getSchedule() == null || event.getSchedule().getDates().size() == 0) {
					msg = res.getString(R.string.can_not_add_event_to_calender);
					
				} else {
					ContentValues values = new ContentValues();
					cursor.moveToPosition(which);
					values.put("calendar_id", cursor.getInt(cursor.getColumnIndex(CALENDARS_COLUMN_ID)));
					//Log.d(TAG, "onClick(), which = " + which + ", calId = " + cursor.getInt(cursor.getColumnIndex("_id")));
					values.put("title", event.getName());
					if (event.getDescription() != null) {
						values.put("description", event.getDescription());
					}
					Schedule schedule = event.getSchedule();
					if (schedule.getVenue() != null) {
						values.put("eventLocation", schedule.getVenue().getName());
					}
					
					long startTime = schedule.getDates().get(0).getStartDate().getTime();
					if (!schedule.getDates().get(0).isStartTimeAvailable()) {
						startTime += DEFAULT_START_HRS * MILLIS_FOR_1_hr;
					}
					values.put("dtstart", startTime);
					values.put("dtend", startTime + MILLIS_FOR_1_hr);
					values.put("eventTimezone", Time.getCurrentTimezone());
					values.put("hasAlarm", 1);
					//Log.d(TAG, "time zone = " + Time.getCurrentTimezone());
					
					Uri eventsUri = Uri.parse("content://com.android.calendar/events");
					Uri url = contentResolver.insert(eventsUri, values);
					
					long eventID = Long.parseLong(url.getLastPathSegment());
					String reminderUriString = "content://com.android.calendar/reminders";

			        ContentValues reminderValues = new ContentValues();
			        reminderValues.put("event_id", eventID);
			        reminderValues.put("minutes", 30); // Default value of the system. Minutes is an integer
			        reminderValues.put("method", 1); // Alert Methods: Default(0), Alert(1), Email(2), SMS(3)

			        Uri reminderUri = contentResolver.insert(Uri.parse(reminderUriString), reminderValues);
					msg = "Event added to calendar successfully!";
					//Log.d(TAG, "event uri = " + url);
				}
				
				Toast.makeText(FragmentUtil.getActivity(SelectCalendarFragment.this), msg, Toast.LENGTH_LONG).show();
				FragmentUtil.getActivity(SelectCalendarFragment.this).finish();
			}
		}, CALENDARS_COLUMN_NAME).setTitle(res.getString(R.string.select_calender))
		.setNegativeButton("Cancel", new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				FragmentUtil.getActivity(SelectCalendarFragment.this).finish();
			}
		});
		AlertDialog dialog = builder.create();
		setCancelable(false);
		return dialog;
	}
	
	@Override
	public void onDestroyView() {
		if (getDialog() != null) {
			getDialog().setDismissMessage(null);
		}
		super.onDestroyView();
	}
}
