package com.wcities.eventseeker.gcm;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.wcities.eventseeker.MainActivity;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.util.NotificationUtil;

public class GcmBroadcastReceiver extends BroadcastReceiver {

	private static final String TAG = GcmBroadcastReceiver.class.getName();

	@Override
	public void onReceive(Context context, Intent intent) {
		GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
		String messageType = gcm.getMessageType(intent);
		
        if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
            Log.e(TAG, "Send error: " + intent.getExtras().toString());
            
        } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
        	Log.e(TAG, "Deleted messages on server: " + intent.getExtras().toString());
            
        } else {
            handleMessage(context, intent);
        }
        setResultCode(Activity.RESULT_OK);
	}
	
	private void handleMessage(final Context context, final Intent intent) {
		final String type = intent.getStringExtra("type");
		final String title = intent.getStringExtra("title");
		final String id = intent.getStringExtra("id");
		final String message = intent.getStringExtra("msg");
		Log.i(TAG, "handleMessage() type: " + type + ", title: " + title + ", id: " + id + ", msg: " + message);

		try {
			if (type.equals("2")) {
				String eventId = intent.getStringExtra("eventid");
				Event event = new Event(Long.parseLong(eventId), title);
				NotificationUtil.addNotification(context, event, message, Integer.parseInt(id));
				
			} else {
				Log.e(TAG, "handleMessage() type is unknown and therefor not handled: " + type);
			}
			
		} catch (final Exception e) {
			Log.e(TAG, "handleMessage() ERROR: ", e);
		}
	}
}
