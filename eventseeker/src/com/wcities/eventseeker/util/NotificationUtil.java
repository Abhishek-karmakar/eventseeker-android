package com.wcities.eventseeker.util;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.wcities.eventseeker.MainActivity;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.constants.Enums.SettingsItem;
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
		/**
		 * 18-09-2014: See Commit: removed launchmode=singletask due to error in Bosch
		 * NOTE: added Action and Category of Launcher Activity for notification Intent so as to avoid
		 * creating 2 new task for notification and app launch event(when from app menu).
		 * For getting this issue, remove the below 2 lines and follow steps below:
		 * e.g.:- Launch app -> sync accounts -> get notification of NotificationType=FOLLOWING -> click on it.
		 *	      It redirects to 'following' screen -> press back to move out of app. Now from home screen click 
		 *	      app icon. It displays discover screen rather then last 'following' screen which was browsed. Same result
		 *	      can be observed now onwards for every launch. This happens due to onCreate() being called up on every 
		 *	      launch after this, even though one task is there in back stack for the app.
		 **/
		notificationIntent.setAction(Intent.ACTION_MAIN);
		notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		notificationIntent.putExtra(BundleKeys.NOTIFICATION_TYPE, notificationType);
		if (notificationType == NotificationType.EVENT_DETAILS) {
			notificationIntent.putExtra(BundleKeys.EVENT, event);
			
		} else if (notificationType == NotificationType.ARTIST_DETAILS) {
			notificationIntent.putExtra(BundleKeys.ARTIST, artist);
		
		} else if (notificationType == NotificationType.SYNC_ACCOUNTS) {
			notificationIntent.putExtra(BundleKeys.SETTINGS_ITEM_ORDINAL, SettingsItem.SYNC_ACCOUNTS.ordinal());
		
		} else if (notificationType == NotificationType.INVITE_FRIENDS) {
			notificationIntent.putExtra(BundleKeys.SETTINGS_ITEM_ORDINAL, SettingsItem.INVITE_FRIENDS.ordinal());
			
		}
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationId, 
				notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
		        .setSmallIcon(R.drawable.ic_notification_white)
		        .setColor(context.getResources().getColor(R.color.colorPrimary))
		        .setContentTitle(title)
		        .setContentText(message);
		mBuilder.setContentIntent(pendingIntent);
		mBuilder.setAutoCancel(true);
		mBuilder.setOnlyAlertOnce(true);
		
		final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(notificationId, mBuilder.build());
	}
}
