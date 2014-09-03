package com.wcities.eventseeker.analytics;

import java.util.HashMap;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.wcities.eventseeker.app.EventSeekr;

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
	
	private static final String TAG = GoogleAnalyticsTracker.class.getSimpleName();
	private static final String ANALYTICS_PROPERTY_ID = "UA-167953-49";
	
	public static final String EVENT_LABEL_TICKETS_BUTTON = "Tickets Button";

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
	
	public void sendApiCall(EventSeekr eventSeekr, String url) {
		//Log.d(TAG, "send screen view");
		if (url != null) {
			if (!url.contains("userId=") && eventSeekr.getWcitiesId() != null) {
				url = url.concat("&userId=").concat(eventSeekr.getWcitiesId());
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
	}
	
	public void sendShareEvent(EventSeekr eventSeekr, String screenName, String shareTarget, String shareItemName) {
		if (eventSeekr.getPackageName().equals(shareTarget)) {
			if ("Event".equals(shareItemName)) {
				sendEvent(eventSeekr, screenName, "Add " + shareItemName + " To Calendar");
			}
			
		} else if (shareTarget.contains("com.facebook")) {
			sendEvent(eventSeekr, screenName, "Facebook " + shareItemName + " Share Button");
			
		} else if (shareTarget.contains("com.android.mms")) {
			sendEvent(eventSeekr, screenName, "Share " + shareItemName + " Using Messaging");
			
		} else if (shareTarget.contains("com.google.android.apps.uploader")) {
			sendEvent(eventSeekr, screenName, "Share " + shareItemName + " Using Picasa");
			
		} else if (shareTarget.contains("com.android.bluetooth")) {
			sendEvent(eventSeekr, screenName, "Share " + shareItemName + " Using Bluetooth");
			
		} else if (shareTarget.contains("com.google.android.apps.plus")) {
			sendEvent(eventSeekr, screenName, "Share " + shareItemName + " Using Google Plus");
			
		} else if (shareTarget.contains("com.android.email")) {
			sendEvent(eventSeekr, screenName, "Share " + shareItemName + " Using Email");
			
		} else if (shareTarget.contains("com.google.android.gm")) {
			sendEvent(eventSeekr, screenName, "Share " + shareItemName + " Using Gmail");
			
		} else if (shareTarget.contains("com.google.android.talk")) {
			sendEvent(eventSeekr, screenName, "Share " + shareItemName + " Using Hangout");
			
		} else if (shareTarget.contains("com.skype")) {
			sendEvent(eventSeekr, screenName, "Share " + shareItemName + " Using Skype");
			
		} else if (shareTarget.contains("com.whatsapp")) {
			sendEvent(eventSeekr, screenName, "Share " + shareItemName + " Using WhatsApp");
		}
	}
}
