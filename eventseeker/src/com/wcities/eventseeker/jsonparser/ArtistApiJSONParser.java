package com.wcities.eventseeker.jsonparser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.Html;
import android.util.Log;
import android.util.SparseArray;

import com.wcities.eventseeker.core.Address;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.BookingInfo;
import com.wcities.eventseeker.core.Country;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Friend;
import com.wcities.eventseeker.core.ImageAttribution;
import com.wcities.eventseeker.core.Schedule;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.core.Video;
import com.wcities.eventseeker.core.Artist.Attending;
import com.wcities.eventseeker.util.ConversionUtil;

public class ArtistApiJSONParser {
	
	private static final String TAG = ArtistApiJSONParser.class.getName();
	
	private static final String KEY_ARTIST_SEARCH = "artistSearch";
	private static final String KEY_ARTISTS = "artists";
	private static final String KEY_ARTIST = "artist";
	private static final String KEY_ID = "id";
	private static final String KEY_NAME = "name";
	private static final String KEY_IMAGE_URL = "image_url";
	private static final String KEY_ONTOUR = "ontour";
	
	private static final String KEY_ARTIST_DETAIL = "artistDetail";
	private static final String KEY_EVENTS = "events";
	private static final String KEY_EVENT_ID = "eventId";
	private static final String KEY_TITLE = "title";
	private static final String KEY_VENUE_ID = "venue_id";
	private static final String KEY_VENUE_NAME = "venue_name";
	private static final String KEY_VENUE_IMAGE = "venue_image";
	private static final String KEY_IMAGEFILE = "imagefile";
	private static final String KEY_IMAGE_ATTRIBUTION = "image_attribution";
	private static final String KEY_EVENT_TIME = "event_time";
	private static final String KEY_EVENT_DATE = "event_date";
	private static final String KEY_HIGH_RES_PATH = "high_res_path";
	private static final String KEY_LOW_RES_PATH = "low_res_path";
	private static final String KEY_MOBI_RES_PATH = "mobi_res_path";
	private static final String KEY_FRIENDS = "friends";
	private static final String KEY_IMAGE = "image";
	private static final String KEY_DESCRIPTION = "description";
	private static final String KEY_MEDIA = "media";
	private static final String KEY_VIDEO = "video";
	private static final String KEY_URL = "url";
	private static final String KEY_ATTENDING = "attending";

	private static final String KEY_TOTAL = "total";

	private static final String KEY_ARTIST_EVENT_DETAIL = "artistEventDetail";
	private static final String KEY_ARTIST_ID = "artistId";
	private static final String KEY_ARTIST_NAME = "artistName";

	private static final String KEY_VENUES = "venues";
	private static final String KEY_VENUE = "venue";
	private static final String KEY_ADDRESS = "address";

	private static final String KEY_ADDRESS1 = "address1";
	private static final String KEY_ADDRESS2 = "address2";

	private static final String KEY_CITY = "city";
	private static final String KEY_COUNTRY = "country";

	private static final String KEY_LATITUDE = "latitude";
	private static final String KEY_LONGITUDE = "longitude";

	private static final String KEY_SCHEDULE = "schedule";

	private static final String KEY_DATES = "dates";
	private static final String KEY_START = "start";

	private static final String KEY_BOOKINGINFO = "bookinginfo";
	private static final String KEY_BOOKINGLINK = "bookinglink";
	private static final String KEY_BOOKING_URL = "booking_url";
	private static final String KEY_PROVIDER = "provider";

	
	public void fillArtistDetails(Artist artist, JSONObject jsonObject) {
		if (jsonObject.has(KEY_ARTIST_DETAIL)) {
			try {
				JSONObject jObjArtist = jsonObject.getJSONObject(KEY_ARTIST_DETAIL).getJSONObject(KEY_ARTIST);
				if (jObjArtist.has(KEY_DESCRIPTION)) {
					artist.setDescription(ConversionUtil.removeBuggyTextsFromDesc(ConversionUtil.parseHtmlString(
							jObjArtist, KEY_DESCRIPTION)));
				}
				
				if (jObjArtist.has(KEY_MEDIA)) {
					JSONObject jObjMedia = jObjArtist.getJSONObject(KEY_MEDIA);
					if (jObjMedia.has(KEY_VIDEO)) {
						fillVideoUrls(artist, jObjMedia.get(KEY_VIDEO));
					}
				}
				
				if (jObjArtist.has(KEY_IMAGE)) {
					artist.setImageName(jObjArtist.getString(KEY_IMAGE));
					
					if (jObjArtist.has(KEY_IMAGE_ATTRIBUTION)) {
						artist.setImageAttribution(getImageAttribution(jObjArtist.getJSONObject(KEY_IMAGE_ATTRIBUTION)));
					}
				}
				
				List<Friend> friends = new ArrayList<Friend>();
				if (jObjArtist.has(KEY_FRIENDS)) {
					JSONArray jArrFriends = jObjArtist.getJSONArray(KEY_FRIENDS);
					
					for (int i = 0; i < jArrFriends.length(); i++) {
						Friend friend = getFriend(jArrFriends.getJSONObject(i));
						friends.add(friend);
					}
				}
				artist.setFriends(friends);
				
				List<Event> events = new ArrayList<Event>();
				if (jObjArtist.has(KEY_EVENTS)) {
					Object jsonEvents = jObjArtist.get(KEY_EVENTS);
					
					if (jsonEvents instanceof JSONArray) {
						JSONArray jArrEvts = (JSONArray) jsonEvents;
						for (int i = 0; i < jArrEvts.length(); i++) {
							Event event = getEvent(jArrEvts.getJSONObject(i));
							events.add(event);
						}
						
					} else {
						Event event = getEvent((JSONObject) jsonEvents);
						events.add(event);
					}
				}
				artist.setEvents(events);
				
				if (jObjArtist.has(KEY_URL)) {
					artist.setArtistUrl(jObjArtist.getString(KEY_URL));
				}
				
				Attending attending = jObjArtist.has(KEY_ATTENDING) ? 
						Attending.getAttending(jObjArtist.getInt(KEY_ATTENDING)) : Attending.NotTracked;
				artist.setAttending(attending);
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void fillVideoUrls(Artist artist, Object obj) throws JSONException {
		List<Video> videos = artist.getVideos();
		if (obj instanceof JSONArray) {
			JSONArray jArrVideos = (JSONArray) obj;
			for (int i = 0; i < jArrVideos.length(); i++) {
				String url = cleanUpYouTubeLink(jArrVideos.getJSONObject(i).getString(KEY_URL));
				videos.add(new Video(url));
			}
			
		} else {
			String url = cleanUpYouTubeLink(((JSONObject)obj).getString(KEY_URL));
			videos.add(new Video(url));
		}
	}
	
	private String cleanUpYouTubeLink(final String videoUrl) {
		String startTag = "value=\"";
		String endTag = "\"";
		int startIndex = videoUrl.indexOf(startTag);
		//Log.d(TAG, "startIndex = " + startIndex);
		if (startIndex != -1) {
			final int endIndex = videoUrl.indexOf(endTag, startIndex + startTag.length());
			final String url = videoUrl.substring(startIndex + startTag.length(), endIndex);
			return cleanUpLink(url);
			
		} else {
			startTag = "src=\"";
			endTag = "\"";
			startIndex = videoUrl.indexOf(startTag);
			if (startIndex != -1) {
				final int endIndex = videoUrl.indexOf(endTag, startIndex + startTag.length());
				final String url = videoUrl.substring(startIndex + startTag.length(), endIndex);
				return cleanUpLink(url);
			}
		}
		return videoUrl;
	}

	private String cleanUpLink(final String link) {
		if (link == null) {
			return null;
		}
		return link.replace("\\", "");
	}
	
	public ArtistDetails getUpcomingEvents(JSONObject jsonObject) {
		ArtistDetails artistDetails = new ArtistDetails();

		if (jsonObject.has(KEY_ARTIST_DETAIL)) {
			try {
				JSONObject jObjArtist = jsonObject.getJSONObject(KEY_ARTIST_DETAIL).getJSONObject(KEY_ARTIST);
				
				if (jObjArtist.has(KEY_EVENTS)) {
					List<Event> events = new ArrayList<Event>();
					Object jsonEvents = jObjArtist.get(KEY_EVENTS);
					
					if (jsonEvents instanceof JSONArray) {
						JSONArray jArrEvts = (JSONArray) jsonEvents;
						for (int i = 0; i < jArrEvts.length(); i++) {
							Event event = getEvent(jArrEvts.getJSONObject(i));
							events.add(event);
						}
						
					} else {
						Event event = getEvent((JSONObject) jsonEvents);
						events.add(event);
					}
					artistDetails.upcomingEvents = events;
				}
				
				if (jObjArtist.has(KEY_FRIENDS)) {
					List<Friend> friends = new ArrayList<Friend>();
					JSONArray jArrFriends = jObjArtist.getJSONArray(KEY_FRIENDS);
					
					for (int i = 0; i < jArrFriends.length(); i++) {
						Friend friend = getFriend(jArrFriends.getJSONObject(i));
						friends.add(friend);
					}
					artistDetails.followingFriends = friends;
				}
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		return artistDetails;
	}
	
	private Friend getFriend(JSONObject jsonObject) throws JSONException {
		Friend friend = new Friend();
		friend.setId(jsonObject.getString(KEY_ID));
		friend.setName(ConversionUtil.parseHtmlString(jsonObject, KEY_NAME));
		friend.setImgUrl(jsonObject.getString(KEY_IMAGE));
		return friend;
	}
	
	private Event getEvent(JSONObject jsonObject) throws JSONException {
		Event event = new Event(jsonObject.getInt(KEY_EVENT_ID), ConversionUtil.parseHtmlString(jsonObject, KEY_TITLE));
		event.setSchedule(getSchedule(jsonObject));
		return event;
	}
	
	private Schedule getSchedule(JSONObject jsonObject) throws JSONException {
		Venue venue = new Venue(jsonObject.getInt(KEY_VENUE_ID));
		venue.setName(ConversionUtil.parseHtmlString(jsonObject, KEY_VENUE_NAME));
		venue.setImagefile(jsonObject.getString(KEY_VENUE_IMAGE));
		venue.setImageAttribution(getImageAttribution(jsonObject.getJSONObject(KEY_IMAGE_ATTRIBUTION)));

		String eventTime = "";
		if (jsonObject.has(KEY_EVENT_TIME)) {
			eventTime = jsonObject.getString(KEY_EVENT_TIME);
		}

		List<String> dates = getDates(jsonObject);
		return buildSchedule(dates, eventTime, venue);
	}
	
	private Schedule getSchedule(JSONObject jsonObject, SparseArray<Venue> venues) throws JSONException {
		String eventTime = "";
		if (jsonObject.has(KEY_EVENT_TIME)) {
			eventTime = jsonObject.getString(KEY_EVENT_TIME);
		}

		List<String> dates = getDates(jsonObject.get(KEY_DATES));
		int venueId = jsonObject.getInt(KEY_VENUE_ID);
		
		Schedule schedule = buildSchedule(dates, eventTime, venueId, venues);
		
		if (jsonObject.has(KEY_BOOKINGINFO)) {
			fillBookingInfo(schedule, jsonObject);
			
		} else {
			//Log.i(TAG, "No booking info found for this event.");
		}
		return schedule;
	}
	
	private void fillBookingInfo(Schedule schedule, JSONObject jObjSchedule) throws JSONException {
		Object objBookinglink = jObjSchedule.getJSONObject(KEY_BOOKINGINFO).get(KEY_BOOKINGLINK);
		
		if (objBookinglink instanceof JSONArray) {
			JSONArray jArrBookingLinks = (JSONArray) objBookinglink;
			for (int i = 0; i < jArrBookingLinks.length(); i++) {
				JSONObject jObjBookingLink = jArrBookingLinks.getJSONObject(i);
				schedule.addBookingInfo(getBookingInfo(jObjBookingLink));
			}
			
		} else {
			schedule.addBookingInfo(getBookingInfo((JSONObject) objBookinglink));
		}
	}
	
	private BookingInfo getBookingInfo(JSONObject jObjBookinglink) throws JSONException {
		BookingInfo bookingInfo = new BookingInfo();
		bookingInfo.setBookingUrl(jObjBookinglink.getString(KEY_BOOKING_URL));
		if (jObjBookinglink.has(KEY_PROVIDER)) {
			bookingInfo.setProvider(ConversionUtil.parseHtmlString(jObjBookinglink, KEY_PROVIDER));
		}
		return bookingInfo;
	}
	
	
	private Schedule buildSchedule(List<String> startDates, String time, int venueId, SparseArray<Venue> venues) {
		Schedule schedule = new Schedule();
		
		Venue venue = venues.get(venueId);
		
		schedule.setVenue(venue);
		
		SimpleDateFormat format;  
		boolean startTimeAvailable;
		
		if (time.equals("")) {
			format = new SimpleDateFormat("yyyy-MM-dd");
			startTimeAvailable = false;
			
		} else {
			format = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss");
			startTimeAvailable = true;
		}

		for (Iterator<String> iterator = startDates.iterator(); iterator.hasNext();) {
			com.wcities.eventseeker.core.Date date = new com.wcities.eventseeker.core.Date(startTimeAvailable);
			String strStartDate = iterator.next();
			try {  
				date.setStartDate(format.parse(strStartDate + time));
			    
			} catch (ParseException e) {  
			    e.printStackTrace();  
			}
			schedule.addDate(date);
		}

		return schedule;
	}
	
	
	private List<String> getDates(Object json) throws JSONException {
		List<String> dates = new ArrayList<String>();
		
		if (json instanceof JSONObject) {
			JSONObject jObjDate = (JSONObject) json;
			String strStartDate = jObjDate.getString(KEY_START);
			dates.add(strStartDate);
			
		} else {
			JSONArray jArrDates = (JSONArray) json;

			for (int i = 0; i < jArrDates.length(); i++) {
				String strStartDate = jArrDates.getJSONObject(i).getString(KEY_START);
				dates.add(strStartDate);
			}
		}
		return dates;
	}
	
	private ImageAttribution getImageAttribution(JSONObject jsonObject) throws JSONException {
		ImageAttribution imageAttribution = new ImageAttribution();
		if (jsonObject.has(KEY_HIGH_RES_PATH)) {
			imageAttribution.setHighResPath(jsonObject.getString(KEY_HIGH_RES_PATH));
		}
		if (jsonObject.has(KEY_LOW_RES_PATH)) {
			imageAttribution.setLowResPath(jsonObject.getString(KEY_LOW_RES_PATH));
		}
		if (jsonObject.has(KEY_MOBI_RES_PATH)) {
			imageAttribution.setMobiResPath(jsonObject.getString(KEY_MOBI_RES_PATH));
		}
		return imageAttribution;
	}
	
	private Schedule buildSchedule(List<String> startDates, String time, Venue venue) {
		Schedule schedule = new Schedule();
		schedule.setVenue(venue);
		
		SimpleDateFormat format;  
		boolean startTimeAvailable;
		
		if (time.equals("")) {
			format = new SimpleDateFormat("yyyy-MM-dd");
			startTimeAvailable = false;
			
		} else {
			format = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss");
			startTimeAvailable = true;
		}

		for (Iterator<String> iterator = startDates.iterator(); iterator.hasNext();) {
			com.wcities.eventseeker.core.Date date = new com.wcities.eventseeker.core.Date(startTimeAvailable);
			String strStartDate = iterator.next();
			try {  
				date.setStartDate(format.parse(strStartDate + time));
			    
			} catch (ParseException e) {  
			    e.printStackTrace();  
			}
			schedule.addDate(date);
		}

		return schedule;
	}
	
	private List<String> getDates(JSONObject jsonObject) throws JSONException {
		List<String> dates = new ArrayList<String>();
		String strStartDate = jsonObject.getString(KEY_EVENT_DATE);
		dates.add(strStartDate);
		return dates;
	}
	
	public int getBatchArtistCount(JSONObject jsonObject) {
		int total = 0;
		
		try {
			JSONObject jObjArtistSearch = jsonObject.getJSONObject(KEY_ARTIST_SEARCH);
			total = jObjArtistSearch.optInt(KEY_TOTAL);
			
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return total;
	}
	
	public List<Artist> getArtistList(JSONObject jsonObject) {
		Log.d(TAG, "getArtistList()");
		List<Artist> artists = new ArrayList<Artist>();
		
		try {
			JSONObject jObjArtistSearch = jsonObject.getJSONObject(KEY_ARTIST_SEARCH);
			if (jObjArtistSearch.has(KEY_ARTISTS)) {
				Log.d(TAG, "KEY_ARTISTS");
				Object jsonArtists = jObjArtistSearch.get(KEY_ARTISTS);

				if (jsonArtists instanceof JSONArray) {
					JSONArray jArrArtists = (JSONArray) jsonArtists;
					for (int i = 0; i < jArrArtists.length(); i++) {
						Artist artist = getArtist(jArrArtists.getJSONObject(i).getJSONObject(KEY_ARTIST));
						artists.add(artist);
					}
					
				} else {
					Artist artist = getArtist(((JSONObject) jsonArtists).getJSONObject(KEY_ARTIST));
					artists.add(artist);
				}
			}
			
		} catch (JSONException e) {
			e.printStackTrace();
		}

		Log.d(TAG, "return artists size = " + artists.size());
		return artists;
	}
	
	private Artist getArtist(JSONObject jsonObject) throws JSONException {
		Artist artist = new Artist(jsonObject.getInt(KEY_ID), ConversionUtil.parseHtmlString(jsonObject, KEY_NAME));
		if (jsonObject.has(KEY_IMAGE_URL)) {
			String imageUrl = jsonObject.getString(KEY_IMAGE_URL);
			String[] urlParts = imageUrl.split("/");
			artist.setImageName(urlParts[urlParts.length - 1]);
			
			ImageAttribution imageAttribution = new ImageAttribution();
			artist.setImageAttribution(imageAttribution);

			if (imageUrl.startsWith(Artist.DEFAULT_MOBI_RES_PATH)) {
				imageAttribution.setMobiResPath(imageUrl.substring(0, imageUrl.lastIndexOf("/") + 1));
				
			} else if (imageUrl.startsWith(Artist.DEFAULT_LOW_RES_PATH)) {
				imageAttribution.setLowResPath(imageUrl.substring(0, imageUrl.lastIndexOf("/") + 1));
				
			} else if (imageUrl.startsWith(Artist.DEFAULT_HIGH_RES_PATH)) {
				imageAttribution.setHighResPath(imageUrl.substring(0, imageUrl.lastIndexOf("/") + 1));
				
			} else {
				/**
				 * this must be fallback image & hence url might be different. So, rather than using any 
				 * default hardcoded path specific to image resolution, directly set imageUrl which 
				 * will be used for all the resolutions.
				 */
				artist.setImageUrl(imageUrl);
			}
		}
		
		if (jsonObject.has(KEY_ONTOUR)) {
			artist.setOntour(true);
		}
		return artist;
	}
	
	public Artist getArtistUpcomingEvent(JSONObject jsonObject) throws JSONException {
		Artist artist = null;
		if (jsonObject.has(KEY_ARTIST_EVENT_DETAIL)) {
			JSONObject jObjArtistEventDetail = jsonObject.getJSONObject(KEY_ARTIST_EVENT_DETAIL);
			artist = new Artist(jObjArtistEventDetail.getInt(KEY_ARTIST_ID), 
					ConversionUtil.parseHtmlString(jObjArtistEventDetail, KEY_ARTIST_NAME));

			if (jObjArtistEventDetail.has(KEY_EVENTS)) {
				JSONObject jObjEvent = jObjArtistEventDetail.getJSONArray(KEY_EVENTS).getJSONObject(0);
				Event event = new Event(jObjEvent.getLong(KEY_ID), ConversionUtil.parseHtmlString(jObjEvent, KEY_NAME));
				List<Event> events = new ArrayList<Event>();
				events.add(event);
				artist.setEvents(events);
			}
		}
		return artist;
	}
	
	public List<Event> getArtistEvents(JSONObject jsonObject) throws JSONException {
		List<Event> events = new ArrayList<Event>();
		SparseArray<Venue> venues = null;
		
		if (jsonObject.has(KEY_ARTIST_EVENT_DETAIL)) {
			JSONObject jObjArtistEventDetail = jsonObject.getJSONObject(KEY_ARTIST_EVENT_DETAIL);
			
			if (jObjArtistEventDetail.has(KEY_VENUES)) {
				venues = getVenues(jObjArtistEventDetail);
			}
			
			if (jObjArtistEventDetail.has(KEY_EVENTS)) {
				JSONArray jArrEvents = jObjArtistEventDetail.getJSONArray(KEY_EVENTS);
				for(int i = 0; i < jArrEvents.length();i++ ) {
					Event event = getEvent(jArrEvents.getJSONObject(i), venues);
					events.add(event);
				}
			}
		}
		return events;
	}
	
	private Event getEvent(JSONObject jsonObject, SparseArray<Venue> venues) throws JSONException {
		Event event = new Event(jsonObject.getInt(KEY_ID), ConversionUtil.parseHtmlString(jsonObject, KEY_NAME));
		
		event.setSchedule(getSchedule(jsonObject.getJSONObject(KEY_SCHEDULE), venues));
		Event.Attending attending = jsonObject.has(KEY_ATTENDING) ? 
				Event.Attending.getAttending(jsonObject.getInt(KEY_ATTENDING)) : Event.Attending.NOT_GOING;
		event.setAttending(attending);
		
		return event;
	}
	
	
	private SparseArray<Venue> getVenues(JSONObject jObjArtistEventDetail) throws JSONException {
		SparseArray<Venue> venues = new SparseArray<Venue>();
		
		JSONArray jArrVenues  = jObjArtistEventDetail.getJSONArray(KEY_VENUES);

		for (int i = 0; i < jArrVenues.length(); i++) {
			Venue venue = getVenue(jArrVenues.getJSONObject(i).getJSONObject(KEY_VENUE));
			venues.append(venue.getId(), venue);
		}
		
		return venues;
	}
	
	private Venue getVenue(JSONObject jsonObject) throws JSONException {
		Venue venue = new Venue(jsonObject.getInt(KEY_ID));
		venue.setName(ConversionUtil.parseHtmlString(jsonObject, KEY_NAME));
		venue.setImagefile(jsonObject.getString(KEY_IMAGEFILE));
		if (jsonObject.has(KEY_ADDRESS)) {
			venue.setAddress(getAddress(jsonObject.getJSONObject(KEY_ADDRESS)));
		}
		return venue;
	}
	
	private Address getAddress(JSONObject jObjAddress) throws JSONException {
		Address address = new Address();
		address.setAddress1(ConversionUtil.parseHtmlString(jObjAddress, KEY_ADDRESS1));
		if (jObjAddress.has(KEY_ADDRESS2)) {
			address.setAddress2(ConversionUtil.parseHtmlString(jObjAddress, KEY_ADDRESS2));
		}
		address.setCity(ConversionUtil.parseHtmlString(jObjAddress, KEY_CITY));
		address.setCountry(getCountry(jObjAddress.getJSONObject(KEY_COUNTRY)));
		
		if (jObjAddress.has(KEY_LATITUDE)) {
			String strLat = jObjAddress.getString(KEY_LATITUDE);
			String strLon = jObjAddress.getString(KEY_LONGITUDE);
			
			if (strLat.length() != 0) {
				address.setLat(Double.parseDouble(strLat));
			}
			if (strLon.length() != 0) {
				address.setLon(Double.parseDouble(strLon));
			}
		}
		return address;
	}
	
	private Country getCountry(JSONObject jsonObject) throws JSONException {
		Country country = new Country();
		country.setName(ConversionUtil.parseHtmlString(jsonObject, KEY_NAME));
		return country;
	}

	public class ArtistDetails {
		
		private List<Event> upcomingEvents;
		private List<Friend> followingFriends;
		
		public List<Event> getUpcomingEvents() {
			return upcomingEvents;
		}
		
		public List<Friend> getFollowingFriends() {
			return followingFriends;
		}
	}
}
