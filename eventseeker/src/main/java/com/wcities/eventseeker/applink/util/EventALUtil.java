package com.wcities.eventseeker.applink.util;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;

import com.ford.syncV4.proxy.TTSChunkFactory;
import com.ford.syncV4.proxy.rpc.TTSChunk;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.applink.core.EventList;
import com.wcities.eventseeker.applink.service.AppLinkService;
import com.wcities.eventseeker.core.Address;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.BookingInfo;
import com.wcities.eventseeker.core.Date;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Venue;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Vector;

public class EventALUtil {
	
	private static final String COUNTRY_NAME = "United States";
	private static final String TAG = EventALUtil.class.getName();
	private static final int ALERT_SLEEP_FOR_CALL = 500;

	/**
     * This flag is added because whenever user presses next/previous button continuously
     * which results in repetation of alert dialog 'No Points Available'
     */
    public static boolean isAlertForNoEventsAvailable;
	
	public static void speakEventTitle(Event event, EventSeekr app) {
		/**
		 * after launching the app, each and every time system should speak the first 
		 * event's title and then append the 'plz press next or back' and then throughout 
		 * the current session it shouldn't append the second line.
		 */

		String simple = event.getName();
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
			simple += app.getResources().getString(R.string.plz_press_nxt_bck_dtls_cll);
			app.setFirstEventTitleForFordEventAL(false);
		}
		
		//Log.d(TAG, "simple = " + simple);
		Vector<TTSChunk> ttsChunks = TTSChunkFactory.createSimpleTTSChunks(simple);
		ALUtil.speakText(ttsChunks);				
	}
	
	public static void speakDetailsOfEvent(Event event, EventSeekr app) {
		//Log.d(TAG, "speakDetailsOfCurrentEvent");
		/**
		 * after launching the app, each and every time system should speak the first 
		 * event's Details and then append the 'plz press next or back' and then throughout 
		 * the current session it shouldn't append the second line.
		 */
		String simple = "";

		if (event.hasArtists()) {
			List<Artist> artists = event.getArtists();
			int appendResId = R.string.is_performing_for_this_event;
			
			simple += artists.get(0).getName();
			for (int i = 1; i < artists.size() - 1; i++) {
				Artist artist = artists.get(i);
				simple += ", " + artist.getName();
			}
			if (artists.size() > 1) {
				simple += " " + app.getResources().getString(R.string.and) + ", " + artists.get(artists.size() - 1).getName();
				appendResId = R.string.are_performing_for_this_event;
			}
			simple += " " + app.getResources().getString(appendResId);
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
					if (simple.trim().length() != 0) {
						simple += ", ";
					}
					if (maxPrice == minPrice) {
						simple += app.getResources().getString(R.string.price_would_be) + " " + maxPrice + 
								" " + currency + ".";
						
					} else {
						simple += app.getResources().getString(R.string.price_range_would_be) + " "
								+ minPrice + ", " + app.getResources().getString(R.string.and) + " " + maxPrice + " " + currency + ".";
					}
				}
				
			} else {
				Log.d(TAG, "Price Details are not available.");
			}
		}
		if (simple.equals("")) {
			simple = app.getResources().getString(R.string.detail_not_available);
			String description = app.getResources().getString(R.string.description);
			String notAvail = app.getResources().getString(R.string.not_available);
			ALUtil.alert(description, notAvail, "", simple);
			return;
		}
		/**
		 * 10-07-2014 : removed the 'plz press voice btn...' text from the details of 1ST EVENT to match
		 * the functionality similar to IOS Ford eventseeker app.
		else {
			if (app.isFirstEventDetailsForFordEventAL()) {
				simple += app.getResources().getString(R.string.plz_press_nxt_or_bck);
				app.setFirstEventDetailsForFordEventAL(false);
			}
		}*/
		/**
		final String tempsimple = simple;
		AppLinkService.getInstance().getCurrentActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(AppLinkService.getInstance().getCurrentActivity(), 
					"Simple : " + tempsimple, 
					Toast.LENGTH_SHORT).show();
			}
		});*/
		//Log.d(TAG, "Details : " + simple);
		Vector<TTSChunk> ttsChunks = TTSChunkFactory.createSimpleTTSChunks(simple);
		ALUtil.speakText(ttsChunks);				
	}
	
	private static String getCurrency(BookingInfo bookingInfo) {
		String currency = bookingInfo.getCurrency();
		//Log.d(TAG, "CURRENCY : " + currency);
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
		//Log.d(TAG, "dateTime : " + dateTime);
		return dateTime;
	}
	
	public static void onNextCommand(EventList eventList, EventSeekr context) throws IOException {
		if (isAlertForNoEventsAvailable) {
			return;
		}
		if (eventList.moveToNextEvent()) {
			displayCurrentEvent(eventList);
			speakEventTitle(eventList.getCurrentEvent(), context);

		} else {
			/**
			 * The below check is for the case when total no. of events are multiples of 10.
			 * In this case, suppose if total events are 20 and when user presses next button
			 * after the 20th event then 'loading...' text would appear on screen and then it
			 * will show alert saying 'No events available'. Then alert gets dismissed but the
			 * loading text stays forever. So, to avoid that we will show current event in
			 * this scenario and hence the screen gets refreshed.
			 */
			isAlertForNoEventsAvailable = true;
			Resources res = context.getResources();
			if (eventList.size() > 0) {
				displayCurrentEvent(eventList);
				ALUtil.alert(res.getString(R.string.you_have_reached), res.getString(R.string.the_last_element),
						res.getString(R.string.of_the_list), res.getString(R.string.you_have_reached_the_last_element_of_the_list));

			} else {
				/**
				 * This is the case when after loading the list, '0' elements are there in entire list and then we have called
				 * onNextCommand to show the first element of the list
				 */
				ALUtil.alert(res.getString(R.string.alert_no_events_available), res.getString(R.string.event_no_evts_avail));
			}
		}
	}

	public static void onBackCommand(EventList eventList, EventSeekr context) {
        if (isAlertForNoEventsAvailable) {
            return;
        }
		if (eventList.moveToPreviousEvent()) {
			displayCurrentEvent(eventList);
			speakEventTitle(eventList.getCurrentEvent(), context);
			
		} else {
            isAlertForNoEventsAvailable = true;
			Resources res = context.getResources();
			ALUtil.alert(res.getString(R.string.you_are_on), res.getString(R.string.the_first_element),
				res.getString(R.string.of_the_list), res.getString(R.string.you_are_on_the_first_element_of_the_list));
		}		
	}

	public static void callVenue(EventList eventList) {
		Event event = eventList.getCurrentEvent();
		Resources res = AppLinkService.getInstance().getResources();
		if (event != null) {
			final Venue venue = event.getSchedule().getVenue();
			if (venue != null && venue.getPhone() != null) {
				String simple = res.getString(R.string.calling);
				ALUtil.alert(simple + " " + venue.getName(), simple);
				//speak(R.string.leaving_app_for_call);
				new Thread(new Runnable() {

					@Override
					public void run() {
						do {
							try {
								Thread.sleep(ALERT_SLEEP_FOR_CALL);

							} catch (InterruptedException e) {
								e.printStackTrace();
							}

						} while (AppLinkService.getInstance().isAlertCurrentlyVisible());

						Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + venue.getPhone()));
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						AppLinkService.getInstance().getCurrentActivity().startActivity(intent);
					}
				}).start();

			} else {
				ALUtil.alert(res.getString(R.string.ford_phone_no), res.getString(R.string.not_available), "", 
					res.getString(R.string.ford_phone_no_is_unavailable));
			}
		}
	}

	public static void speakVenueAddress(Venue venue, EventSeekr context) {
		String simple = context.getResources().getString(R.string.venue_address_not_available);
		if(venue != null) {
			Address address = venue.getAddress();
			if (address != null) {
				String address1 = address.getAddress1();
				String address2 = address.getAddress2();
				String city = address.getCity();
				String zipcode = address.getZip();

				StringBuilder addressToSpeak = new StringBuilder();

				if (address1 != null && !address1.trim().equals("")) {
					addressToSpeak.append(address1);
				}

				if (address2 != null && !address2.trim().equals("")) {
					if (!addressToSpeak.toString().equals("")) {
						addressToSpeak.append(", ");
					}
					addressToSpeak.append(address2);
				}

				if (city != null && !city.trim().equals("")) {
					if (!addressToSpeak.toString().equals("")) {
						addressToSpeak.append(", ");
					}
					addressToSpeak.append(city);
				}

				if (zipcode != null && !zipcode.trim().equals("")) {
					if (!addressToSpeak.toString().equals("")) {
						addressToSpeak.append(", ");
					}
					addressToSpeak.append(zipcode);
				}

				if (!addressToSpeak.toString().equals("")) {
					simple = addressToSpeak.toString();
				}
			}
		}
		Log.d(TAG, "Address : " + simple);
		Vector<TTSChunk> ttsChunks = TTSChunkFactory.createSimpleTTSChunks(simple);
		ALUtil.speakText(ttsChunks);
	}
}
