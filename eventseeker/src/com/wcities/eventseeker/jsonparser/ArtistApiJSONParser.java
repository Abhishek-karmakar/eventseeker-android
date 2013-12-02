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

import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Friend;
import com.wcities.eventseeker.core.ImageAttribution;
import com.wcities.eventseeker.core.Schedule;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.core.Video;
import com.wcities.eventseeker.core.Artist.Attending;
import com.wcities.eventseeker.util.ConversionUtil;

public class ArtistApiJSONParser {
	
	private static final String TAG = "ArtistApiJSONParser";
	
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
	
	public void fillArtistDetails(Artist artist, JSONObject jsonObject) {
		if (jsonObject.has(KEY_ARTIST_DETAIL)) {
			try {
				JSONObject jObjArtist = jsonObject.getJSONObject(KEY_ARTIST_DETAIL).getJSONObject(KEY_ARTIST);
				if (jObjArtist.has(KEY_DESCRIPTION)) {
					artist.setDescription(ConversionUtil.removeBuggyTextsFromDesc(Html.fromHtml(
							jObjArtist.getString(KEY_DESCRIPTION)).toString()));
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
		friend.setName(jsonObject.getString(KEY_NAME));
		friend.setImgUrl(jsonObject.getString(KEY_IMAGE));
		return friend;
	}
	
	private Event getEvent(JSONObject jsonObject) throws JSONException {
		Event event = new Event(jsonObject.getInt(KEY_EVENT_ID), jsonObject.getString(KEY_TITLE));
		event.setSchedule(getSchedule(jsonObject));
		return event;
	}
	
	private Schedule getSchedule(JSONObject jsonObject) throws JSONException {
		Venue venue = new Venue(jsonObject.getInt(KEY_VENUE_ID));
		venue.setName(jsonObject.getString(KEY_VENUE_NAME));
		venue.setImagefile(jsonObject.getString(KEY_VENUE_IMAGE));
		venue.setImageAttribution(getImageAttribution(jsonObject.getJSONObject(KEY_IMAGE_ATTRIBUTION)));

		String eventTime = "";
		if (jsonObject.has(KEY_EVENT_TIME)) {
			eventTime = jsonObject.getString(KEY_EVENT_TIME);
		}

		List<String> dates = getDates(jsonObject);
		return buildSchedule(dates, eventTime, venue);
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
		Artist artist = new Artist(jsonObject.getInt(KEY_ID), jsonObject.getString(KEY_NAME));
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
	
	public Event getArtistUpcomingEvent(JSONObject jsonObject) throws JSONException {
		Event event = null;
		if (jsonObject.has(KEY_ARTIST_EVENT_DETAIL)) {
			JSONObject jObjArtistEventDetail = jsonObject.getJSONObject(KEY_ARTIST_EVENT_DETAIL);
			
			if (jObjArtistEventDetail.has(KEY_EVENTS)) {
				JSONObject jObjEvent = jObjArtistEventDetail.getJSONArray(KEY_EVENTS).getJSONObject(0);
				event = new Event(jObjEvent.getLong(KEY_ID), jObjEvent.getString(KEY_NAME));
			}
		}
		return event;
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
