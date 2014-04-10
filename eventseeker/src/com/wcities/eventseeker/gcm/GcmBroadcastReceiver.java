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
	
	public static enum NotificationType {
		DISCOVER (MainActivity.INDEX_NAV_ITEM_DISCOVER),
		ARTIST_DETAILS (AppConstants.INVALID_INDEX),
		EVENT_DETAILS (AppConstants.INVALID_INDEX),
		FRIENDS_NEWS (MainActivity.INDEX_NAV_ITEM_FRIENDS_ACTIVITY),
		RECOMMENDED_EVENTS (MainActivity.INDEX_NAV_ITEM_MY_EVENTS),
		FOLLOWING (MainActivity.INDEX_NAV_ITEM_FOLLOWING),
		ARTIST_NEWS (MainActivity.INDEX_NAV_ITEM_ARTISTS_NEWS),
		SYNC_ACCOUNTS (MainActivity.INDEX_NAV_ITEM_CONNECT_ACCOUNTS),
		INVITE_FRIENDS (MainActivity.INDEX_NAV_ITEM_INVITE_FRIENDS);
		
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
	
	private void handleMessage(final Context context, final Intent intent) {
		final String type = intent.getStringExtra("type");
		final String title = intent.getStringExtra("title");
		final int notificationId = ((EventSeekr)context.getApplicationContext()).getUniqueGcmNotificationId();
		final String message = intent.getStringExtra("msg");
		Log.i(TAG, "handleMessage() type: " + type + ", title: " + title + ", notificationId: " + 
				notificationId + ", msg: " + message);
		
		NotificationType notificationType = NotificationType.values()[Integer.parseInt(type)];

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
