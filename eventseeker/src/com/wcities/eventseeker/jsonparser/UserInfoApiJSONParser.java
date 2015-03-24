package com.wcities.eventseeker.jsonparser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.wcities.eventseeker.api.UserInfoApi.RepCodeResponse;
import com.wcities.eventseeker.constants.Enums.Service;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.ArtistNewsItem;
import com.wcities.eventseeker.core.ArtistNewsItem.PostType;
import com.wcities.eventseeker.core.BookingInfo;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Event.Attending;
import com.wcities.eventseeker.core.Friend;
import com.wcities.eventseeker.core.FriendNewsItem;
import com.wcities.eventseeker.core.ImageAttribution;
import com.wcities.eventseeker.core.ItemsList;
import com.wcities.eventseeker.core.Schedule;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.core.Video;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.FbUtil;

public class UserInfoApiJSONParser {
	
	private static final String TAG = "UserInfoApiJSONParser";

	private static final String KEY_ARTISTFEED = "artistfeed";
	private static final String KEY_ARTIST = "artist";
	private static final String KEY_NAME = "name";
	private static final String KEY_POST_TYPE = "postType";
	private static final String KEY_POST_TITLE = "postTitle";
	private static final String KEY_POST_DESC = "postDesc";
	private static final String KEY_POST_URL = "postUrl";
	private static final String KEY_TIMESTAMP = "timestamp";
	private static final String KEY_LINK_TITLE = "link_title";
	private static final String KEY_LINK_IMAGE = "link_image";
	private static final String KEY_PHOTO = "photo";
	private static final String KEY_VIDEO = "video";
	private static final String KEY_LINK = "link";
	private static final String KEY_ARTIST_IMAGE = "artist_image";

	private static final String KEY_FRIENDSFEED = "friendsfeed";
	private static final String KEY_TRACK_INFO = "trackInfo";
	private static final String KEY_FRIEND_ID = "friendId";
	private static final String KEY_FRIEND_NAME = "friendName";
	private static final String KEY_TRACK_ID = "trackId";
	private static final String KEY_FB_POSTID = "fb_postid";
	private static final String KEY_TRACK_NAME = "trackName";
	private static final String KEY_ATTENDING = "attending";
	private static final String KEY_USER_ATTENDING = "user_attending";
	private static final String KEY_VENUE_ID = "venue_id";
	private static final String KEY_MEDIA = "media";
	private static final String KEY_HIGH_RES_PATH = "high_res_path";
	private static final String KEY_LOW_RES_PATH = "low_res_path";
	private static final String KEY_MOBI_RES_PATH = "mobi_res_path";
	private static final String KEY_IMAGE = "image";
	private static final String KEY_VENUE_NAME = "venue_name";
	private static final String KEY_PHONE = "phone";
	private static final String KEY_EVENT_DATE = "event_date";
	private static final String KEY_EVENT_TIME = "event_time";

	private static final String KEY_RECOMMENDED_ARTIST = "recommendedArtist";
	private static final String KEY_POPULAR_ARTIST = "popularArtist";
	private static final String KEY_TRACKED = "tracked";
	private static final String KEY_ID = "id";
	private static final String KEY_CITY = "city";
	private static final String KEY_IMAGE_ATTRIBUTION = "image_attribution";
	private static final String KEY_TOTAL = "total";
	private static final String KEY_URL = "url";

	private static final String KEY_ONTOUR = "ontour";

	private static final String KEY_SIGN_UP = "signUp";
	private static final String KEY_USER_ID = "userId";
	private static final String KEY_SYNCACCOUNT = "syncaccount";
	private static final String KEY_UPDATE_REPCODE = "updaterepcode";
	private static final String KEY_WCITIES_ID = "wcities_id";
	private static final String KEY_REP_CODE = "repCode";
	private static final String KEY_SYNC = "sync";
	private static final String KEY_FB_ID = "fb_id";
	private static final String KEY_GOOGLE_ID = "google_id";
	
	private static final String KEY_SIGNUP = "signup";
	private static final String KEY_MSG_CODE = "msg_code";
	private static final String KEY_WCITIES_ID2 = "wcitiesId";
	
	private static final String KEY_ARTIST_ID = "artist_id";
	private static final String KEY_ARTIST_NAME = "artist_name";
	private static final String KEY_PRICE = "price";
	private static final String KEY_VALUE = "value";
	private static final String KEY_CURRENCY = "currency";
	private static final String KEY_SCHEDULE = "schedule";
	private static final String KEY_BOOKING_URL = "booking_url";
	private static final String KEY_PROVIDER = "provider";
	private static final String KEY_BOOKINGINFO = "bookinginfo";
	private static final String KEY_BOOKINGLINK = "bookinglink";

	private static final String KEY_RECOMMENDED_EVENT = "recommendedEvent";
	private static final String KEY_EVENT_INFO = "eventInfo";
	
	private static final String KEY_LINKS = "links";
	private static final String KEY_TRACKBACK_URL = "trackback_url";

	private static final String KEY_SYNC_SERVICE = "syncService";
	
	public static final int MSG_CODE_SUCCESS = -1;
	public static final int MSG_CODE_UNSUCCESS = -2;
	public static final int MSG_CODE_NO_ACCESS_TOKEN = -3;
	public static final int MSG_CODE_INVALID_REQUEST = 1;
	public static final int MSG_CODE_EMAIL_OR_PWD_INCORRECT = 2;
	public static final int MSG_CODE_CHK_EMAIL_TO_RESET_PWD = 3;
	public static final int MSG_CODE_EMAIL_ALREADY_EXISTS = 15;
	public static final int MSG_CODE_NEW_USER = 16;
	public static final int MSG_CODE_USER_EMAIL_DOESNT_EXIST = 17;

	public String getUserId(JSONObject jsonObject) throws JSONException {
		JSONObject jObjSignup = jsonObject.getJSONObject(KEY_SIGN_UP);
		String userId = jObjSignup.getString(KEY_USER_ID);
		return userId;
	}
	
	public String getWcitiesId(JSONObject jsonObject) throws JSONException {
		JSONObject jObjSyncaccount = jsonObject.getJSONObject(KEY_SYNCACCOUNT);
		String wcitiesId = jObjSyncaccount.getString(KEY_WCITIES_ID);
		return wcitiesId;
	}
	
	public SyncAccountResponse parseSyncAccount(JSONObject jsonObject) throws JSONException {
		JSONObject jObjSyncaccount = jsonObject.getJSONObject(KEY_SYNCACCOUNT);
		if (jObjSyncaccount.has(KEY_FB_ID)) {
			return new SyncAccountResponse(jObjSyncaccount.getBoolean(KEY_SYNC), 
					jObjSyncaccount.getString(KEY_WCITIES_ID), jObjSyncaccount.getString(KEY_FB_ID));
			
		} else if (jObjSyncaccount.has(KEY_GOOGLE_ID)) {
			return new SyncAccountResponse(jObjSyncaccount.getBoolean(KEY_SYNC), 
					jObjSyncaccount.getString(KEY_WCITIES_ID), jObjSyncaccount.getString(KEY_GOOGLE_ID));
			
		} else {
			return new SyncAccountResponse(jObjSyncaccount.getBoolean(KEY_SYNC), jObjSyncaccount.getString(KEY_WCITIES_ID));
		}
	}
	
	public SignupResponse parseSignup(JSONObject jsonObject) throws JSONException {
		JSONObject jObjSignup = jsonObject.getJSONObject(KEY_SIGNUP);
		if (jObjSignup.has(KEY_MSG_CODE)) {
			return new SignupResponse(jObjSignup.getInt(KEY_MSG_CODE));
			
		} else if (jObjSignup.has(KEY_WCITIES_ID2)) {
			return new SignupResponse(jObjSignup.getString(KEY_WCITIES_ID2));
			
		} else {
			return new SignupResponse(MSG_CODE_SUCCESS);
		}
	}
	
	public int getRepCodeResponse(JSONObject jsonObject) throws JSONException {
		int repCodeResponse = RepCodeResponse.UNKNOWN_ERROR.getRepCode();
		JSONObject jObj = null;
		if (jsonObject.has(KEY_SYNCACCOUNT)) {
			jObj = jsonObject.getJSONObject(KEY_SYNCACCOUNT);

		} else if (jsonObject.has(KEY_UPDATE_REPCODE)) {
			jObj = jsonObject.getJSONObject(KEY_UPDATE_REPCODE);	
		}
		
		if (jObj != null && jObj.has(KEY_REP_CODE)) {
			String repCode = jObj.getString(KEY_REP_CODE);
			repCodeResponse = Integer.parseInt(repCode);
		}
		return repCodeResponse;
	}
	
	public ItemsList<Artist> getArtistList(JSONObject jsonObject) {
		ItemsList<Artist> myArtistsList = new ItemsList<Artist>();
		List<Artist> artists = new ArrayList<Artist>();
		//myArtistsList.items = artists;
		myArtistsList.setItems(artists);
		
		try {
			JSONObject jObjTracked = jsonObject.getJSONObject(KEY_TRACKED);
			//myArtistsList.totalCount = jObjTracked.getInt(KEY_TOTAL);
			myArtistsList.setTotalCount(jObjTracked.getInt(KEY_TOTAL));
			
			if (jObjTracked.has(KEY_TRACK_INFO)) {
				Object jTrackedInfo = jObjTracked.get(KEY_TRACK_INFO);
				
				if (jTrackedInfo instanceof JSONArray) {
					JSONArray jArrTrackedInfos = (JSONArray) jTrackedInfo;
					for (int i = 0; i < jArrTrackedInfos.length(); i++) {
						Artist artist = getArtist(jArrTrackedInfos.getJSONObject(i));
						artist.setAttending(Artist.Attending.Tracked);//As this artist is Followed
						artists.add(artist);
					}
					
				} else {
					Artist artist = getArtist((JSONObject) jTrackedInfo);
					artist.setAttending(Artist.Attending.Tracked);//As this artist is Followed
					artists.add(artist);
				}
			}
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return myArtistsList;
	}

	public ItemsList<Artist> getRecommendedArtistList(JSONObject jsonObject) {
		ItemsList<Artist> recommendedArtistsList = new ItemsList<Artist>();
		List<Artist> artists = new ArrayList<Artist>();
		recommendedArtistsList.setItems(artists);
		
		try {
			JSONObject jObjTracked = jsonObject.getJSONObject(KEY_RECOMMENDED_ARTIST);
			recommendedArtistsList.setTotalCount(jObjTracked.getInt(KEY_TOTAL));
			fillArtists(artists, jObjTracked);
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return recommendedArtistsList;
	}
	
	private void fillArtists(List<Artist> artists, JSONObject jObj) throws JSONException {
		if (jObj.has(KEY_ARTIST)) {
			Object jArtist = jObj.get(KEY_ARTIST);
			
			if (jArtist instanceof JSONArray) {
				JSONArray jArrTrackedInfos = (JSONArray) jArtist;
				for (int i = 0; i < jArrTrackedInfos.length(); i++) {
					Artist artist = getArtist(jArrTrackedInfos.getJSONObject(i));
					artists.add(artist);
				}
				
			} else {
				Artist artist = getArtist((JSONObject) jArtist);
				artists.add(artist);
			}
		}		
	}
	
	public ItemsList<Artist> getPopularArtistList(JSONObject jsonObject) {
		ItemsList<Artist> popularArtistsList = new ItemsList<Artist>();
		List<Artist> artists = new ArrayList<Artist>();
		popularArtistsList.setItems(artists);
		
		try {
			JSONObject jObjTracked = jsonObject.getJSONObject(KEY_POPULAR_ARTIST);
			popularArtistsList.setTotalCount(jObjTracked.getInt(KEY_TOTAL));
			fillArtists(artists, jObjTracked);
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return popularArtistsList;
	}
	
	public List<FriendNewsItem> getFriendNews(JSONObject jsonObject) {
		List<FriendNewsItem> friendNewsItems = new ArrayList<FriendNewsItem>();
		
		try {
			JSONObject jObjFriendsfeed = jsonObject.getJSONObject(KEY_FRIENDSFEED);
			if (jObjFriendsfeed.has(KEY_TRACK_INFO)) {
				Object jTrackInfo = jObjFriendsfeed.get(KEY_TRACK_INFO);
				
				if (jTrackInfo instanceof JSONArray) {
					JSONArray jArrTrackInfo = (JSONArray) jTrackInfo;
					for (int i = 0; i < jArrTrackInfo.length(); i++) {
						FriendNewsItem friendNewsItem = getFriendNewsItem(jArrTrackInfo
								.getJSONObject(i));
						friendNewsItems.add(friendNewsItem);
					}
					
				} else {
					FriendNewsItem friendNewsItem = getFriendNewsItem((JSONObject) jTrackInfo);
					friendNewsItems.add(friendNewsItem);
				}
			}
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return friendNewsItems;
	}
	
	private FriendNewsItem getFriendNewsItem(JSONObject jsonObject) throws JSONException {
		FriendNewsItem friendNewsItem = new FriendNewsItem();
		
		Friend friend = new Friend();
		friend.setId(jsonObject.getString(KEY_FRIEND_ID));
		friend.setName(jsonObject.getString(KEY_FRIEND_NAME));
		friend.setAttending(Attending.getAttending(jsonObject.getInt(KEY_ATTENDING)));
		friend.setImgUrl(FbUtil.getFriendImgUrl(friend.getId()));
		
		friendNewsItem.setFriend(friend);
		
		friendNewsItem.setTrackId(jsonObject.getLong(KEY_TRACK_ID));
		if (jsonObject.has(KEY_FB_POSTID)) {
			friendNewsItem.setFbPostId(jsonObject.getString(KEY_FB_POSTID));
		}
		friendNewsItem.setTrackName(jsonObject.getString(KEY_TRACK_NAME));
		
		Attending userAttending = Attending.NOT_GOING;
		if (jsonObject.has(KEY_USER_ATTENDING)) {
			userAttending = Attending.getAttending(jsonObject.getInt(KEY_USER_ATTENDING));
		}
		friendNewsItem.setUserAttending(userAttending);
		if (jsonObject.has(KEY_BOOKING_URL)) {
			friendNewsItem.setBookingUrl(jsonObject.getString(KEY_BOOKING_URL));
		}
		if (jsonObject.has(KEY_VENUE_ID)) {
			friendNewsItem.setVenueId(jsonObject.getInt(KEY_VENUE_ID));
		}
		if (jsonObject.has(KEY_VENUE_NAME)) {
			friendNewsItem.setVenueName(jsonObject.getString(KEY_VENUE_NAME));
		}

		if (jsonObject.has(KEY_MEDIA)) {
			JSONObject jObjMedia = jsonObject.getJSONObject(KEY_MEDIA);
			ImageAttribution imageAttribution = new ImageAttribution();
			imageAttribution.setHighResPath(jObjMedia.getString(KEY_HIGH_RES_PATH));
			imageAttribution.setLowResPath(jObjMedia.getString(KEY_LOW_RES_PATH));
			imageAttribution.setMobiResPath(jObjMedia.getString(KEY_MOBI_RES_PATH));
			friendNewsItem.setImageAttribution(imageAttribution);
		
			String imgName = "";
			Object jImage = jObjMedia.get(KEY_IMAGE);
			if (jImage instanceof JSONArray) {
				JSONArray jArrImage = (JSONArray) jImage;
				imgName = jArrImage.getString(0);
				
			} else {
				imgName = (String) jImage;
			}
			friendNewsItem.setImgName(imgName);
		}
		
		if (jsonObject.has(KEY_EVENT_DATE)) {
			String strStartDate = jsonObject.getString(KEY_EVENT_DATE);
			
			String eventTime = "";
			if (jsonObject.has(KEY_EVENT_TIME)) {
				eventTime = jsonObject.getString(KEY_EVENT_TIME);
			}
			
			SimpleDateFormat format;  
			boolean startTimeAvailable;
			
			if (eventTime.equals("")) {
				format = new SimpleDateFormat("yyyy-MM-dd");
				startTimeAvailable = false;
				
			} else {
				format = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss");
				startTimeAvailable = true;
			}
			com.wcities.eventseeker.core.Date date = new com.wcities.eventseeker.core.Date(startTimeAvailable);
			try {
				date.setStartDate(format.parse(strStartDate + eventTime));
				
			} catch (ParseException e) {
				e.printStackTrace();
			}
			
			friendNewsItem.setStartTime(date);
		}
		
		return friendNewsItem;
	}

	public List<ArtistNewsItem> getArtistNews(JSONObject jsonObject) {
		List<ArtistNewsItem> artistNewsItems = new ArrayList<ArtistNewsItem>();
		
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		try {
			JSONObject jObjArtistfeed = jsonObject.getJSONObject(KEY_ARTISTFEED);
			if (jObjArtistfeed.has(KEY_ARTIST)) {
				Object jArtist = jObjArtistfeed.get(KEY_ARTIST);
				
				if (jArtist instanceof JSONArray) {
					JSONArray jArrArtist = (JSONArray) jArtist;
					for (int i = 0; i < jArrArtist.length(); i++) {
						ArtistNewsItem artistNewsItem = getArtistNewsItem(
								jArrArtist.getJSONObject(i), format);
						if (artistNewsItem != null) {
							artistNewsItems.add(artistNewsItem);
						}
					}
					
				} else {
					ArtistNewsItem artistNewsItem = getArtistNewsItem((JSONObject) jArtist, format);
					if (artistNewsItem != null) {
						artistNewsItems.add(artistNewsItem);
					}
				}
			}
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return artistNewsItems;
	}
	
	private ArtistNewsItem getArtistNewsItem(JSONObject jsonObject, SimpleDateFormat format) 
			throws JSONException {
		ArtistNewsItem artistNewsItem = new ArtistNewsItem();
		artistNewsItem.createArtist(jsonObject.getInt(KEY_ID), jsonObject.getString(KEY_NAME));
		/**
		 * ImageAttibution needs to be initialized else it would be null.
		 */
		artistNewsItem.getArtist().setImageAttribution(new ImageAttribution());
		artistNewsItem.getArtist().setImageName(jsonObject.getString(KEY_ARTIST_IMAGE));
		
		String postType = jsonObject.getString(KEY_POST_TYPE);
		/*if (postType.equals("swf")) {
			return null;
		}*/ 
		artistNewsItem.setPostType(PostType.valueOf(postType));
		
		if (jsonObject.has(KEY_POST_TITLE)) {
			artistNewsItem.setPostTitle(jsonObject.getString(KEY_POST_TITLE));
			
		} else if (jsonObject.has(KEY_LINK_TITLE)) {
			artistNewsItem.setPostTitle(jsonObject.getString(KEY_LINK_TITLE));
		}
		
		String postDesc = "";
		if (jsonObject.has(KEY_POST_DESC)) {
			Object jPostDesc = jsonObject.get(KEY_POST_DESC);
			if (jPostDesc instanceof JSONArray) {
				//Log.i(TAG, "it's jArr");
				JSONArray jArrPostDesc = (JSONArray) jPostDesc;
				postDesc = jArrPostDesc.getString(jArrPostDesc.length() - 1);
				
			} else {
				//Log.i(TAG, "it's string");
				postDesc = jsonObject.getString(KEY_POST_DESC);
			}
		}
		artistNewsItem.setPostDesc(postDesc);
		artistNewsItem.setPostUrl(jsonObject.getString(KEY_POST_URL));
		try {
			artistNewsItem.setTimestamp(format.parse(jsonObject.getString(KEY_TIMESTAMP)));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		if (jsonObject.has(KEY_LINK_IMAGE)) {
			artistNewsItem.setImgUrl(jsonObject.getString(KEY_LINK_IMAGE));
			
		} else if (jsonObject.has(KEY_PHOTO)) {
			artistNewsItem.setImgUrl(jsonObject.getString(KEY_PHOTO));
			
		} else if (jsonObject.has(KEY_VIDEO)) {
			String videoUrl = jsonObject.getString(KEY_VIDEO);
			//String[] videoUrlParts = videoUrl.split("[/?]");
			//Log.i(TAG, "video url parts=" + videoUrlParts.length);
			//String imgUrl = "http://img.youtube.com/vi/" + videoUrlParts[4] + "/0.jpg";
			artistNewsItem.setImgUrl(ConversionUtil.getYoutubeScreenShot(videoUrl, Video.YOUTUBE_VIDEO_SIZE_HQDEFAULT));
			
		} else if (jsonObject.has(KEY_LINK)) {
			String videoUrl = jsonObject.getString(KEY_LINK);
			artistNewsItem.setImgUrl(ConversionUtil.getYoutubeScreenShot(videoUrl, Video.YOUTUBE_VIDEO_SIZE_HQDEFAULT));
		}
		return artistNewsItem;
	}
	
	public ItemsList<Event> getRecommendedEventList(JSONObject jsonObject) {
		ItemsList<Event> recommendedEvtList = new ItemsList<Event>();
		List<Event> events = new ArrayList<Event>();
		recommendedEvtList.setItems(events);
		
		try {
			JSONObject jObjRecommendedEvent = jsonObject.getJSONObject(KEY_RECOMMENDED_EVENT);
			if (jObjRecommendedEvent.has(KEY_TOTAL)) {
				recommendedEvtList.setTotalCount(jObjRecommendedEvent.getInt(KEY_TOTAL));
			}
			
			if (jObjRecommendedEvent.has(KEY_EVENT_INFO)) {
				Object jEventInfo = jObjRecommendedEvent.get(KEY_EVENT_INFO);
				
				if (jEventInfo instanceof JSONArray) {
					JSONArray jEventInfos = (JSONArray) jEventInfo;
					for (int i = 0; i < jEventInfos.length(); i++) {
						Event event = getEvent(jEventInfos.getJSONObject(i));
						events.add(event);
					}
					
				} else {
					Event event = getEvent((JSONObject) jEventInfo);
					events.add(event);
				}
			}
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return recommendedEvtList;
	} 
	
	public ItemsList<Event> getEventList(JSONObject jsonObject) {
		ItemsList<Event> myEventsList = new ItemsList<Event>();
		List<Event> events = new ArrayList<Event>();
		myEventsList.setItems(events);
		
		try {
			JSONObject jObjTracked = jsonObject.getJSONObject(KEY_TRACKED);
			if (jObjTracked.has(KEY_TOTAL)) {
				myEventsList.setTotalCount(jObjTracked.getInt(KEY_TOTAL));
			}
			
			if (jObjTracked.has(KEY_TRACK_INFO)) {
				Object jTrackedInfo = jObjTracked.get(KEY_TRACK_INFO);
				
				if (jTrackedInfo instanceof JSONArray) {
					JSONArray jArrTrackedInfos = (JSONArray) jTrackedInfo;
					for (int i = 0; i < jArrTrackedInfos.length(); i++) {
						Event event = getEvent(jArrTrackedInfos.getJSONObject(i));
						events.add(event);
					}
					
				} else {
					Event event = getEvent((JSONObject) jTrackedInfo);
					events.add(event);
				}
			}
			
		} catch (JSONException e) {
			Log.e(TAG, "load JSONException, " + e.getMessage());
			e.printStackTrace();
		}
		
		return myEventsList;
	}
	
	public int getMyEventsCount(JSONObject jsonObject) throws JSONException {
		JSONObject jObjTracked = jsonObject.getJSONObject(KEY_TRACKED);
		return jObjTracked.getInt(KEY_TOTAL);
	}
	
	private Artist getArtist(JSONObject jsonObject) throws JSONException {
		Artist artist = new Artist(jsonObject.getInt(KEY_ID), jsonObject.getString(KEY_NAME));
		artist.setImageName(jsonObject.getString(KEY_IMAGE));
		if (jsonObject.has(KEY_ONTOUR)) {
			artist.setOntour(true);
		}
		artist.setImageAttribution(getImageAttribution(jsonObject.getJSONObject(KEY_IMAGE_ATTRIBUTION)));
		if (jsonObject.has(KEY_URL)) {
			artist.setArtistUrl(jsonObject.getString(KEY_URL));
		}
		return artist;
	}
	
	private Event getEvent(JSONObject jsonObject) throws JSONException {
		Event event = new Event(jsonObject.getInt(KEY_ID), ConversionUtil.decodeHtmlEntities(jsonObject, KEY_NAME));
		
		boolean hasArtists = jsonObject.has(KEY_ARTIST) ? true : false;
        event.setHasArtists(hasArtists);
		
		List<String> startDates = new ArrayList<String>();
		String date = jsonObject.getString(KEY_EVENT_DATE);
		startDates.add(date);
		
		String eventTime = "";
		if (jsonObject.has(KEY_EVENT_TIME)) {
			eventTime = jsonObject.getString(KEY_EVENT_TIME);
		}
		
		Venue venue = new Venue(jsonObject.getInt(KEY_VENUE_ID));
		venue.setName(ConversionUtil.decodeHtmlEntities(jsonObject, KEY_VENUE_NAME));
		if (jsonObject.has(KEY_PHONE)) {
			Object jPhone = jsonObject.get(KEY_PHONE);
			if (jPhone instanceof JSONArray) {
				//Log.d(TAG, "array");
				String phone = ((JSONArray) jPhone).getString(0);
				phone = ConversionUtil.parseForPhone(phone);
				venue.setPhone(phone);
				
			} else {
				//Log.d(TAG, "not array");
				String phone = (String) jPhone;
				phone = ConversionUtil.parseForPhone(phone);
				venue.setPhone(phone);
			}
		}
		event.setSchedule(buildSchedule(startDates, eventTime, venue));
		
		if (jsonObject.has(KEY_PRICE)) {
			fillBookingInfo(event.getSchedule(), jsonObject);
			
		} else if (jsonObject.has(KEY_SCHEDULE)) {
			JSONObject jObjSchedule = jsonObject.getJSONObject(KEY_SCHEDULE);
			fillBookingInfo(event.getSchedule(), jObjSchedule);
			
		} else {
			//Log.d(TAG, "No price found for this event.");
		}
		
		event.setCityName(jsonObject.getString(KEY_CITY));
		event.setImgName(jsonObject.getString(KEY_IMAGE));
		event.setImageAttribution(getImageAttribution(jsonObject.getJSONObject(KEY_IMAGE_ATTRIBUTION)));
		
		if (jsonObject.has(KEY_ARTIST)) {
			fillArtists(event, jsonObject);
			
		} else {
			//Log.d(TAG, "No artist found belonging to this event.");
		}
		
		if (jsonObject.has(KEY_LINKS)) {
			JSONObject jObjLinks = jsonObject.getJSONObject(KEY_LINKS);
			if (jObjLinks.has(KEY_TRACKBACK_URL)) {
				event.setEventUrl(jObjLinks.getString(KEY_TRACKBACK_URL));
			}
		}
		
		Attending attending = jsonObject.has(KEY_ATTENDING) ? 
				Attending.getAttending(jsonObject.getInt(KEY_ATTENDING)) : Attending.NOT_GOING;
		event.setAttending(attending);
		return event;
	}
	
	private void fillBookingInfo(Schedule schedule, JSONObject jsonObject) throws JSONException {
		Object jBookingLink = null;
		if (jsonObject.has(KEY_PRICE)) {
			jBookingLink = jsonObject.get(KEY_PRICE);
			
		} else if (jsonObject.has(KEY_BOOKINGINFO)) {
			jBookingLink = jsonObject.getJSONObject(KEY_BOOKINGINFO).get(KEY_BOOKINGLINK);
		}
		
		if (jBookingLink instanceof JSONArray) {
			JSONArray jArrBookingLinks = (JSONArray) jBookingLink;
			for (int i = 0; i < jArrBookingLinks.length(); i++) {
				JSONObject jObjBookingLink = jArrBookingLinks.getJSONObject(i);
				schedule.addBookingInfo(getBookingInfo(jObjBookingLink));
			}
			
		} else if (jBookingLink instanceof JSONObject) {
			schedule.addBookingInfo(getBookingInfo((JSONObject) jBookingLink));
		}
	}
	
	private BookingInfo getBookingInfo(JSONObject jsonObject) throws JSONException {
		BookingInfo bookingInfo = new BookingInfo();
		if (jsonObject.has(KEY_BOOKING_URL)) {
			bookingInfo.setBookingUrl(jsonObject.optString(KEY_BOOKING_URL));
		}
		bookingInfo.setProvider(jsonObject.optString(KEY_PROVIDER));
		if (jsonObject.has(KEY_PRICE)) {
			JSONObject jObjPrice = jsonObject.getJSONObject(KEY_PRICE);
			String strPriceVal = jObjPrice.getString(KEY_VALUE);
			bookingInfo.setPrice(ConversionUtil.stringToFloat(strPriceVal));
			bookingInfo.setCurrency(jObjPrice.getString(KEY_CURRENCY));
		}
		return bookingInfo;
	}
	
	private void fillArtists(Event event, JSONObject jObjEvent) throws JSONException {
		List<Artist> artists = event.getArtists();
		Object jArtist = jObjEvent.get(KEY_ARTIST);
		
		if (jArtist instanceof JSONArray) {
			JSONArray jArrArtists = (JSONArray) jArtist;
			for (int i = 0; i < jArrArtists.length(); i++) {
				JSONObject jObjArtist = jArrArtists.getJSONObject(i);
				artists.add(new Artist(jObjArtist.getInt(KEY_ARTIST_ID), jObjArtist.getString(KEY_ARTIST_NAME)));
			}
			
		} else {
			JSONObject jObjArtist = (JSONObject) jArtist;
			artists.add(new Artist(jObjArtist.getInt(KEY_ARTIST_ID), jObjArtist.getString(KEY_ARTIST_NAME)));
		}
	}
	
	private ImageAttribution getImageAttribution(JSONObject jsonObject) throws JSONException {
		ImageAttribution imageAttribution = new ImageAttribution();
		imageAttribution.setHighResPath(jsonObject.getString(KEY_HIGH_RES_PATH));
		imageAttribution.setLowResPath(jsonObject.getString(KEY_LOW_RES_PATH));
		imageAttribution.setMobiResPath(jsonObject.getString(KEY_MOBI_RES_PATH));
		return imageAttribution;
	}
	
	public List<Service> getAvailableSyncServiceList(JSONObject jsonObject) throws JSONException {
		List<Service> list = new ArrayList<Service>();
		
		if (jsonObject.has(KEY_SYNC_SERVICE)) {
			JSONObject jsonSyncService = jsonObject.getJSONObject(KEY_SYNC_SERVICE);
			
			Service services[] = Service.values();
			for (Service service : services) {
				if (jsonSyncService.has(service.getServerMappingId() + "")) {
					list.add(service);
				}	
			}
		}
		return 	list;
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
	
	public class SignupResponse {
		
		private int msgCode;
		private String wcitiesId;
		
		public SignupResponse(int msgCode) {
			this.msgCode = msgCode;
		}

		public SignupResponse(String wcitiesId) {
			this.wcitiesId = wcitiesId;
			msgCode = MSG_CODE_SUCCESS;
		}

		public int getMsgCode() {
			return msgCode;
		}
		
		public String getWcitiesId() {
			return wcitiesId;
		}
	}
	
	public class SyncAccountResponse {
		
		private boolean sync;
		private String wcitiesId;
		private String fbGoogleId;
		
		public SyncAccountResponse(boolean sync, String wcitiesId) {
			this.sync = sync;
			this.wcitiesId = wcitiesId;
		}
		
		public SyncAccountResponse(boolean sync, String wcitiesId, String fbGoogleId) {
			this(sync, wcitiesId);
			this.fbGoogleId = fbGoogleId;
		}

		public boolean isSync() {
			return sync;
		}

		public String getWcitiesId() {
			return wcitiesId;
		}

		public String getFbGoogleId() {
			return fbGoogleId;
		}
	}
}
