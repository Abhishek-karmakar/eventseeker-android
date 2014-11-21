package com.wcities.eventseeker.gcm;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.wcities.eventseeker.MainActivity;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.util.NotificationUtil;

public class GcmBroadcastReceiver extends BroadcastReceiver {

	private static final String TAG = GcmBroadcastReceiver.class.getName();
	
	/**
	 * It is needed just for testing Dummy Notifications, in non production builds.
	 */
	private static GcmBroadcastReceiver gcmBroadcastReceiver;
	
	public static enum NotificationType {
		DISCOVER (MainActivity.INDEX_NAV_ITEM_DISCOVER),
		ARTIST_DETAILS (AppConstants.INVALID_INDEX),
		EVENT_DETAILS (AppConstants.INVALID_INDEX),
		FRIENDS_NEWS (MainActivity.INDEX_NAV_ITEM_FRIENDS_ACTIVITY),
		RECOMMENDED_EVENTS (MainActivity.INDEX_NAV_ITEM_MY_EVENTS),
		FOLLOWING (MainActivity.INDEX_NAV_ITEM_FOLLOWING),
		ARTIST_NEWS (MainActivity.INDEX_NAV_ITEM_ARTISTS_NEWS),
		/**
		 * 20-11-2014:
		 * SYNC_ACCOUNTS is now removed from the Drawer Items and hence changed its index to INVALID_INDEX
		 */
		SYNC_ACCOUNTS (AppConstants.INVALID_INDEX),
		/**
		 * 20-11-2014:
		 * INVITE_FRIENDS is now removed from the Drawer Items and hence changed its index to INVALID_INDEX
		 */
		INVITE_FRIENDS (AppConstants.INVALID_INDEX);
		/*SETTINGS_ITEM_ORDINAL (MainActivity.INDEX_NAV_ITEM_CONNECT_ACCOUNTS),
		IS_INVITE_FRIENDS (MainActivity.INDEX_NAV_ITEM_INVITE_FRIENDS);*/
		
		private int navDrawerindex;
		
		private NotificationType(int navDrawerindex) {
			this.navDrawerindex = navDrawerindex;
		}

		public int getNavDrawerIndex() {
			return navDrawerindex;
		}
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
		String messageType = gcm.getMessageType(intent);
		
        if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
            Log.e(TAG, "Send error: " + intent.getExtras().toString());
            
        } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
        	Log.e(TAG, "Deleted messages on server: " + intent.getExtras().toString());
            
        } else if (((EventSeekr)context.getApplicationContext()).getWcitiesId() != null) {
            handleMessage(context, intent);
        }
        setResultCode(Activity.RESULT_OK);
	}
	
	/**
	 * This method is for Debugging purpose
	 * @param context
	 * @param intent
	 */
	public static void createDummyNotificationForTesting(Context context, String type, String title, String msg) {
		if (!AppConstants.IS_RELEASE_MODE) {
			if (gcmBroadcastReceiver == null) {
				gcmBroadcastReceiver = new GcmBroadcastReceiver();
			}
			Intent intent = new Intent();
			intent.putExtra("type", type);
			intent.putExtra("title", title);
			intent.putExtra("msg", msg);
			if (("" + NotificationType.EVENT_DETAILS.ordinal()).equals(type)) {
				intent.putExtra("eventid", 98129422 + "");
			}
			if (("" + NotificationType.ARTIST_DETAILS.ordinal()).equals(type)) {
				intent.putExtra("artist_id", 14 + "");
				
			}
			gcmBroadcastReceiver.handleMessage(context, intent);
		}
	}
	
	private void handleMessage(final Context context, final Intent intent) {
		final String type = intent.getStringExtra("type");
		final String title = intent.getStringExtra("title");
		final int notificationId = ((EventSeekr)context.getApplicationContext()).getUniqueGcmNotificationId();
		final String message = intent.getStringExtra("msg");
		Log.i(TAG, "handleMessage() type: " + type + ", title: " + title + ", notificationId: " + 
				notificationId + ", msg: " + message);
		
		NotificationType notificationType;
		int notificationTypeOrdinal = Integer.parseInt(type);
		if (notificationTypeOrdinal < NotificationType.values().length) {
			notificationType = NotificationType.values()[notificationTypeOrdinal];
			
		} else {
			Log.e(TAG, "Unknown notification type");
			return;
		}

		try {
			switch (notificationType) {
			
			case DISCOVER:
			case FRIENDS_NEWS:
			case RECOMMENDED_EVENTS:
			case FOLLOWING:
			case ARTIST_NEWS:
			case SYNC_ACCOUNTS:
			case INVITE_FRIENDS:
				NotificationUtil.addNotification(context, message, notificationId, notificationType);
				break;
			
			case EVENT_DETAILS:
				String eventId = intent.getStringExtra("eventid");
				Event event = new Event(Long.parseLong(eventId), title);
				NotificationUtil.addNotification(context, message, notificationId, notificationType, event);
				break;
				
			case ARTIST_DETAILS:
				String artistId = intent.getStringExtra("artist_id");
				Artist artist = new Artist(Integer.parseInt(artistId), title);
				NotificationUtil.addNotification(context, message, notificationId, notificationType, artist);
				break;

			default:
				Log.e(TAG, "handleMessage() type is unknown and therefor not handled: " + type);
				break;
			}
			
		} catch (final Exception e) {
			Log.e(TAG, "handleMessage() ERROR: ", e);
		}
	}
}
