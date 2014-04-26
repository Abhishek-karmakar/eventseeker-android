package com.wcities.eventseeker.applink.util;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Vector;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;

import com.ford.syncV4.proxy.TTSChunkFactory;
import com.ford.syncV4.proxy.rpc.TTSChunk;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.applink.datastructure.EventList;
import com.wcities.eventseeker.applink.service.AppLinkService;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.BookingInfo;
import com.wcities.eventseeker.core.Date;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Venue;

public class EventALUtil {
	
	private static final String COUNTRY_NAME = "United States";
	private static final String TAG = EventALUtil.class.getName();
	
	public static void speakEventTitle(Event event, EventSeekr app) {
		/**
		 * after launching the app, each and every time system should speak the first 
		 * event's title and then append the 'plz press next or back' and then throughout 
		 * the current session it shouldn't append the second line.
		 */

		String simple = "Okay, " + event.getName();
		
		if (event.getSchedule() != null) {
			Venue venue = event.getSchedule().getVenue();
			if (venue != null) {
				String venueName = venue.getName();
				if (venueName != null) {
					simple += ", at " + venueName;			
				}
				
				List<Date> dates = event.getSchedule().getDates();
				if (dates != null && !dates.isEmpty()) {
					simple += ", on " + EventALUtil.getFormattedDateTime(dates.get(0), venue);
				}
			}
		}

		if (app.isFirstEventTitleForFordEventAL()) {
			simple += app.getResources().getString(R.string.plz_press_nxt_or_bck);
			app.setFirstEventTitleForFordEventAL(false);
		}
		
		Log.d(TAG, "simple = " + simple);
		Vector<TTSChunk> ttsChunks = TTSChunkFactory.createSimpleTTSChunks(simple);
		ALUtil.speakText(ttsChunks);				
	}
	
	public static void speakDetailsOfEvent(Event event, EventSeekr app) {
		Log.d(TAG, "speakDetailsOfCurrentEvent");
		/**
		 * after launching the app, each and every time system should speak the first 
		 * event's Details and then append the 'plz press next or back' and then throughout 
		 * the current session it shouldn't append the second line.
		 */
		String simple = "";

		if (event.hasArtists()) {
			List<Artist> artists = event.getArtists();
			String verb = " is ";
			
			simple += artists.get(0).getName();
			for (int i = 1; i < artists.size() - 1; i++) {
				Artist artist = artists.get(i);
				simple += ", " + artist.getName();
			}
			if (artists.size() > 1) {
				simple += " and, " + artists.get(artists.size() - 1).getName();
				verb = " are ";
			}
			simple += verb + "performing for this event";
		}

		if (event.getSchedule() != null) {
			List<BookingInfo> bookingInfoList = event.getSchedule().getBookingInfos();
			if (bookingInfoList.size() > 0) {
				float minPrice = Float.MAX_VALUE, maxPrice = 0;
				String currency = "";
				for (BookingInfo bookingInfo : bookingInfoList) {
					if (bookingInfo.getPrice() != 0) {
						currency = getCurrency(bookingInfo);
						maxPrice = (bookingInfo.getPrice() > maxPrice) ? bookingInfo.getPrice() : maxPrice;
						minPrice = (bookingInfo.getPrice() < minPrice) ? bookingInfo.getPrice() : minPrice;
					}
				}
				
				if (maxPrice != 0) {
					if (maxPrice == minPrice) {
						simple += ", price would be " + maxPrice + " " + currency + ".";
						
					} else {
						simple += ", price range would be approximately between "
								+ minPrice + ", and " + maxPrice + " " + currency + ".";
						
					}
				}
				
			} else {
				Log.d(TAG, "Price Details are not available.");
			}
		}
		
		if (simple.equals("")) {
			simple = app.getResources().getString(R.string.detail_not_available);
		} else {
			if (app.isFirstEventDetailsForFordEventAL()) {
				simple += app.getResources().getString(R.string.plz_press_nxt_or_bck);
				app.setFirstEventDetailsForFordEventAL(false);
			}
		}
		
		Log.d(TAG, "Details : " + simple);
		Vector<TTSChunk> ttsChunks = TTSChunkFactory.createSimpleTTSChunks(simple);
		ALUtil.speakText(ttsChunks);				
	}
	
	private static String getCurrency(BookingInfo bookingInfo) {
		String currency = bookingInfo.getCurrency();
		Log.d(TAG, "CURRENCY : " + currency);
		if (currency == null) {
			return "";
		}
		if (currency.equals("EUR")) {
			return "euro";
		} else if (currency.equals("USD")) {
			return "united states dollar";
		} else if (currency.equals("GBP")) {
			return "Pound sterling";
		} else if (currency.equals("CHF")) {
			return "Swiss franc";
		} else if (currency.equals("CAD")) {
			return "Canadian dollar";
		} else if (currency.equals("SEK")) {
			return "Swedish krona";
		}
		return "";
	}
	
	public static void displayCurrentEvent(EventList eventList) {
		ALUtil.displayMessage(eventList.getCurrentEvent().getName(), 
				(eventList.getCurrentEventPosition() + 1) + "/" + eventList.getTotalNoOfEvents());
	}
	
	private static String getFormattedDateTime(Date date, Venue venue) {
		if (date == null) {
			return null;			
		}
		
		String dateTime = "";
		
		java.util.Date dt = date.getStartDate();
		SimpleDateFormat format = new SimpleDateFormat("dd MMM");
		dateTime += format.format(dt);
		
		if (date.isStartTimeAvailable()) {
			format = new SimpleDateFormat("HH");
			
			dateTime += ", " + format.format(dt);
			
			format = new SimpleDateFormat("m");
			
			String min = format.format(dt);
			if (min.length() > 1) {
				dateTime += " " + min;		
			} else {
				if (min.equals("0")) {
					dateTime += " hundred";	
				} else {
					if (venue.getAddress().getCountry().getName().equals(COUNTRY_NAME)) {
						//if country is USA then " 0 " must be spelled as " Oh "
						dateTime += " Oh " + min;						
						
					} else {
						dateTime += " 0 " + min;						
						
					}
				}
			}
			
			dateTime += " hours";
		}
		Log.d(TAG, "dateTime : " + dateTime);
		return dateTime;
	}
	
	public static void onNextCommand(EventList eventList, EventSeekr context) {
		if (eventList.moveToNextEvent()) {
			displayCurrentEvent(eventList);
			speakEventTitle(eventList.getCurrentEvent(), context);
			
		} else {
			Resources res = context.getResources();
			ALUtil.alert(res.getString(R.string.alert_no_events_available), res.getString(
					R.string.event_no_evts_avail));
		}		
	}

	public static void onBackCommand(EventList eventList,EventSeekr context) {
		if (eventList.moveToPreviousEvent()) {
			displayCurrentEvent(eventList);
			speakEventTitle(eventList.getCurrentEvent(), context);
			
		} else {
			Resources res = context.getResources();
			ALUtil.alert(res.getString(R.string.alert_no_events_available), res.getString(
					R.string.event_no_evts_avail));
		}		
	}

	public static void callVenue(EventList eventList) {
		Event event = eventList.getCurrentEvent();
		if (event != null) {
			Venue venue = event.getSchedule().getVenue();
			if (venue != null && venue.getPhone() != null) {
				Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + venue.getPhone()));
				AppLinkService.getInstance().getCurrentActivity().startActivity(Intent.createChooser(
						intent, "Call..."));
				
			} else {
				ALUtil.speak(R.string.ford_phone_no_is_unavailable);
			}
		}
	}
}
