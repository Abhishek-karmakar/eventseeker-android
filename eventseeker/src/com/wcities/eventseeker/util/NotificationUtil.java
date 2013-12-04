package com.wcities.eventseeker.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.wcities.eventseeker.MainActivity;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Event;

public class NotificationUtil {

	private static final String TAG = NotificationUtil.class.getName();

	public static void addNotification(Context context, Event event, String message, int notificationId) {
		Log.d(TAG, "addNotification() Message: " + message);

		String title = context.getString(R.string.app_name);
		Intent notificationIntent = new Intent(context, MainActivity.class);
		notificationIntent.putExtra(BundleKeys.EVENT, event);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
		final PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationId, 
				notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

		final Notification notification = new Notification(R.drawable.ic_launcher, message, System.currentTimeMillis());
		notification.setLatestEventInfo(context, title, message, pendingIntent);
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notification.flags |= Notification.FLAG_ONLY_ALERT_ONCE;

		final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(notificationId, notification);
	}
}
