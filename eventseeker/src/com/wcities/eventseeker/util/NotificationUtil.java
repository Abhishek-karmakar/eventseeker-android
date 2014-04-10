package com.wcities.eventseeker.util;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.wcities.eventseeker.MainActivity;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.gcm.GcmBroadcastReceiver.NotificationType;

public class NotificationUtil {

	private static final String TAG = NotificationUtil.class.getName();

	public static void addNotification(Context context, String message, int notificationId, 
			NotificationType notificationType, Event event) {
		addNotification(context, message, notificationId, notificationType, event, null);
		
		/*final Notification notification = new Notification(R.drawable.ic_launcher, message, System.currentTimeMillis());
		notification.setLatestEventInfo(context, title, message, pendingIntent);
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notification.flags |= Notification.FLAG_ONLY_ALERT_ONCE;*/
		
		/*NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
		String[] events = new String[] {"a", "b"};
		// Sets a title for the Inbox style big view
		inboxStyle.setBigContentTitle("Event list:");
		// Moves events into the big view
		for (int i=0; i < events.length; i++) {
		    inboxStyle.addLine(events[i]);
		}
		// Moves the big view style object into the notification object.
		mBuilder.setStyle(inboxStyle);*/
	}
	
	public static void addNotification(Context context, String message, int notificationId, 
			NotificationType notificationType, Artist artist) {
		addNotification(context, message, notificationId, notificationType, null, artist);
	}
	
	public static void addNotification(Context context, String message, int notificationId, 
			NotificationType notificationType) {
		addNotification(context, message, notificationId, notificationType, null, null);
	}
	
	private static void addNotification(Context context, String message, int notificationId, 
			NotificationType notificationType, Event event, Artist artist) {
		//Log.d(TAG, "addNotification() Message: " + message);
		String title = context.getString(R.string.app_name);
		Intent notificationIntent = new Intent(context, MainActivity.class);
		notificationIntent.putExtra(BundleKeys.NOTIFICATION_TYPE, notificationType);
		if (notificationType == NotificationType.EVENT_DETAILS) {
			notificationIntent.putExtra(BundleKeys.EVENT, event);
			
		} else if (notificationType == NotificationType.ARTIST_DETAILS) {
			notificationIntent.putExtra(BundleKeys.ARTIST, artist);
		}
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationId, 
				notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
		        .setSmallIcon(R.drawable.ic_notification)
		        .setContentTitle(title)
		        .setContentText(message);
		mBuilder.setContentIntent(pendingIntent);
		mBuilder.setAutoCancel(true);
		mBuilder.setOnlyAlertOnce(true);
		
		final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(notificationId, mBuilder.build());
	}
}
