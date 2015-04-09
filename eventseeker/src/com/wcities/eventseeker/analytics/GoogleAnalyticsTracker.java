package com.wcities.eventseeker.analytics;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.AppConstants;

public class GoogleAnalyticsTracker {
	
	/**
	 * Enum used to identify the tracker that needs to be used for tracking.
	 *
	 * A single tracker is usually enough for most purposes. In case you do need multiple trackers,
	 * store them to ensure that they are created only once.
	 */
	public enum TrackerName {
	    APP_TRACKER; // Tracker used only in this app.
	}
	
	public enum Type {
		Event,
		Artist,
		Venue;
	}
	
	private static final String TAG = GoogleAnalyticsTracker.class.getSimpleName();
	private static final String ANALYTICS_PROPERTY_ID = "UA-167953-49";
	
	public static final String EVENT_LABEL_TICKETS_BUTTON = "Tickets Button";
	public static final String ARTIST_VIDEO_CLICK = "ARTIST_VIDEO_CLICK";

	private static GoogleAnalyticsTracker googleAnalyticsTracker;

	private HashMap<TrackerName, Tracker> mTrackers;
	
	private GoogleAnalyticsTracker() {
		mTrackers = new HashMap<TrackerName, Tracker>();
	}
	
	public static GoogleAnalyticsTracker getInstance() {
		if (googleAnalyticsTracker == null) {
			synchronized (GoogleAnalyticsTracker.class) {
				if (googleAnalyticsTracker == null) {
					googleAnalyticsTracker = new GoogleAnalyticsTracker();
				}
			}
		}
		return googleAnalyticsTracker;
	}

	public synchronized Tracker getTracker(EventSeekr eventSeekr, TrackerName trackerId) {
		if (!mTrackers.containsKey(trackerId)) {
			GoogleAnalytics analytics = GoogleAnalytics.getInstance(eventSeekr);
			Tracker t = analytics.newTracker(ANALYTICS_PROPERTY_ID);
			mTrackers.put(trackerId, t);
		}
		return mTrackers.get(trackerId);
	}

	public void sendScreenView(EventSeekr eventSeekr, String screenName) {
		//Log.d(TAG, "send screen view");
		if (screenName != null) {
			Tracker t = getTracker(eventSeekr, TrackerName.APP_TRACKER);
			// Set screen name, where path is a String representing the screen name.
			t.setScreenName(screenName);
			/********************************** NOTE: ****************************************
			 * Setting 'null' for below values is to reset these values. Otherwise, it would *
			 * reuse the same values from previous tracking for these fields, as the same 	 *
			 * Tracker is being used each time.												 *
			 *********************************************************************************/
			t.setPage(null);
			t.setTitle(null);
			// Send a screen view.
			t.send(new HitBuilders.AppViewBuilder().build());
		}
	}
	
	public void sendEvent(EventSeekr eventSeekr, String screenName, String label) {
		//Log.d(TAG, "send screen view");
		if (screenName != null) {
			Tracker t = getTracker(eventSeekr, TrackerName.APP_TRACKER);
			// Set screen name, where path is a String representing the screen name.
			t.setScreenName(screenName);
			t.setPage(screenName);
			t.setTitle(screenName);
			t.send(new HitBuilders.EventBuilder()
				.setCategory("uiAction")
				.setAction("buttonPress")
				.setLabel(label)
				.build());
		}
	}
	
	public void sendEvent(EventSeekr eventSeekr, String screenName, String label, String type, String videoUrl, long typeId) {
		//Log.d(TAG, "send screen view");
		if (screenName != null) {
			Tracker t = getTracker(eventSeekr, TrackerName.APP_TRACKER);
			// Set screen name, where path is a String representing the screen name.
			t.setScreenName(screenName);
			t.setPage(getExtraInfoApiUrl(type, label, videoUrl, typeId, eventSeekr.getWcitiesId()));
			t.setTitle(screenName);
			t.send(new HitBuilders.EventBuilder()
			.setCategory("uiAction")
			.setAction("buttonPress")
			.setLabel(label)
			.build());
		}
	}
	
	public void sendApiCall(EventSeekr eventSeekr, String url, byte[] postData) {
		//Log.d(TAG, "send screen view");
		try {
			if (url != null) {
				if (postData != null) {
					String token = (url.contains("?")) ? "&" : "?";
					url = url.concat(token).concat(URLDecoder.decode(new String(postData), AppConstants.CHARSET_NAME));
				}
				if (eventSeekr.getWcitiesId() != null) {
					String token = (url.contains("?")) ? "&" : "?";
					url = url.concat(token).concat("_u=").concat(eventSeekr.getWcitiesId());
				}
				Tracker t = getTracker(eventSeekr, TrackerName.APP_TRACKER);
				// Set screen name, where path is a String representing the screen name.
				t.setScreenName("Api call");
				t.setPage(url);
				t.setTitle("Api call");
				t.send(new HitBuilders.EventBuilder()
					.setCategory("apiCall")
					.build());
			}
			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	public void sendShareEvent(EventSeekr eventSeekr, String screenName, String shareTarget, Type type, long typeId) {
		if (eventSeekr.getPackageName().equals(shareTarget)) {
			if (Type.Event.name().equals(type.name())) {
				sendEvent(eventSeekr, screenName, "Add " + type.name() + " To Calendar", type.name(), null, typeId);
			}
			
		} else if (shareTarget.contains("com.facebook")) {
			sendEvent(eventSeekr, screenName, "Facebook " + type.name() + " Share Button", type.name(), null, typeId);
			
		} else if (shareTarget.contains("com.android.mms")) {
			sendEvent(eventSeekr, screenName, "Share " + type.name() + " Using Messaging", type.name(), null, typeId);
			
		} else if (shareTarget.contains("com.google.android.apps.uploader")) {
			sendEvent(eventSeekr, screenName, "Share " + type.name() + " Using Picasa", type.name(), null, typeId);
			
		} else if (shareTarget.contains("com.android.bluetooth")) {
			sendEvent(eventSeekr, screenName, "Share " + type.name() + " Using Bluetooth", type.name(), null, typeId);
			
		} else if (shareTarget.contains("com.google.android.apps.plus")) {
			sendEvent(eventSeekr, screenName, "Share " + type.name() + " Using Google Plus", type.name(), null, typeId);
			
		} else if (shareTarget.contains("com.android.email")) {
			sendEvent(eventSeekr, screenName, "Share " + type.name() + " Using Email", type.name(), null, typeId);
			
		} else if (shareTarget.contains("com.google.android.gm")) {
			sendEvent(eventSeekr, screenName, "Share " + type.name() + " Using Gmail", type.name(), null, typeId);
			
		} else if (shareTarget.contains("com.google.android.talk")) {
			sendEvent(eventSeekr, screenName, "Share " + type.name() + " Using Hangout", type.name(), null, typeId);
			
		} else if (shareTarget.contains("com.skype")) {
			sendEvent(eventSeekr, screenName, "Share " + type.name() + " Using Skype", type.name(), null, typeId);
			
		} else if (shareTarget.contains("com.whatsapp")) {
			sendEvent(eventSeekr, screenName, "Share " + type.name() + " Using WhatsApp", type.name(), null, typeId);
		}
	}
	
	/**
	 * added on 15-12-2014: It will create api call Url.
	 * @param type
	 * @param requestType
	 * @param typeId
	 * @param wcitiesId
	 * @return
	 */
	private String getExtraInfoApiUrl(String type /*artist/event/venue*/, 
			String requestType /*[SHARE_TYPE/ARIST_VIDEO_CLICK/BUY_TICKET]*/, 
			String videoUrl /*This is only if type is artist*/,
			long typeId /*artist/event/venue - id*/, String wcitiesId) {
		String url = Api.COMMON_URL + "extraInfo_ga.php?oauth_token=" + Api.OAUTH_TOKEN + "&type=" + type + 
				"&requestType=" + requestType + "&id=" + typeId + "&_u=" + wcitiesId;
		if (videoUrl != null) {
			url += "&video_url=" + videoUrl;
		}
		return url;
	}
	
}
