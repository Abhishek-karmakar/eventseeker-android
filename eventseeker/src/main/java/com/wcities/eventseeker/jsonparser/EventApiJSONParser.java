package com.wcities.eventseeker.jsonparser;

import android.util.SparseArray;

import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.applink.core.EventList.GetEventsFrom;
import com.wcities.eventseeker.core.Address;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.BookingInfo;
import com.wcities.eventseeker.core.Country;
import com.wcities.eventseeker.core.Date;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Event.Attending;
import com.wcities.eventseeker.core.Friend;
import com.wcities.eventseeker.core.ImageAttribution;
import com.wcities.eventseeker.core.ItemsList;
import com.wcities.eventseeker.core.Schedule;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.util.ConversionUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class EventApiJSONParser {
	
	private static final String TAG = EventApiJSONParser.class.getName();
	
	private static final String KEY_FEATURED_EVENT = "featured_event";
	private static final String KEY_EVENTS = "events";
	private static final String KEY_ID = "id";
	private static final String KEY_NAME = "name";
	private static final String KEY_IMAGE = "image";
	private static final String KEY_IMAGE_ATTRIBUTION = "image_attribution";
	private static final String KEY_DISTANCE = "distance";
	private static final String KEY_HIGH_RES_PATH = "high_res_path";
	private static final String KEY_LOW_RES_PATH = "low_res_path";
	private static final String KEY_MOBI_RES_PATH = "mobi_res_path";
	private static final String KEY_CITY_ID = "city_id";
	private static final String KEY_CITY_NAME = "city_name";
	private static final String KEY_ZIP = "zip";
	private static final String KEY_DATE = "date";
	private static final String KEY_DESC = "desc";
	private static final String KEY_LATITUDE = "latitude";
	private static final String KEY_LONGITUDE = "longitude";

	private static final String KEY_TOTAL = "total";
	private static final String KEY_CITYEVENT = "cityevent";
	private static final String KEY_EVENT = "event";
	private static final String KEY_SCHEDULE = "schedule";
    private static final String KEY_WEBSITE = "website";
	private static final String KEY_VENUE_ID = "venue_id";
	private static final String KEY_DATES = "dates";
	private static final String KEY_START = "start";
    private static final String KEY_END = "end";
	private static final String KEY_EVENT_TIME = "event_time";
	private static final String KEY_VENUES = "venues";
	private static final String KEY_VENUE = "venue";
	private static final String KEY_VENUE_NAME = "venue_name";
	private static final String KEY_ATTENDING = "attending";
	private static final String KEY_ADDRESS = "address";
	private static final String KEY_PHONE = "phone";
	private static final String KEY_ADDRESS1 = "address1";
	private static final String KEY_ADDRESS2 = "address2";
	private static final String KEY_CITY = "city";
	private static final String KEY_COUNTRY = "country";
	private static final String KEY_FRIENDS = "friends";
	private static final String KEY_GOINGTO = "goingto";
	private static final String KEY_WANTTO = "wantto";

	private static final String KEY_ARTIST = "artist";
	private static final String KEY_ARTIST_ID = "artist_id";
	private static final String KEY_ARTIST_NAME = "artist_name";
	private static final String KEY_ARTIST_IMAGE = "artist_image";
	private static final String KEY_MOBIRES = "mobires";
	private static final String KEY_LOWRES = "lowres";
	private static final String KEY_HIGHRES = "highres";
	private static final String KEY_BOOKINGINFO = "bookinginfo";
	private static final String KEY_BOOKINGLINK = "bookinglink";
	private static final String KEY_BOOKING_URL = "booking_url";
	private static final String KEY_PROVIDER = "provider";
	private static final String KEY_PRICE = "price";
	private static final String KEY_CURRENCY = "currency";
	private static final String KEY_VALUE = "value";
	private static final String KEY_LINKS = "links";
	private static final String KEY_TRACKBACK_URL = "trackback_url";
    private static final String KEY_SOCIAL_URL = "social_url";

	private static final String KEY_SUGGESTED_INVITES = "suggested_invites";

	private static final String KEY_ERROR_CODE = "error_code";

	public void fillEventDetails(Event event, JSONObject jsonObject) {
		try {
			JSONObject jObjCityevent = jsonObject.getJSONObject(KEY_CITYEVENT);
			if (jObjCityevent.has(KEY_ERROR_CODE)) {
				int errorCode = jObjCityevent.getInt(KEY_ERROR_CODE);
				
				if (errorCode == Api.ERROR_CODE_NO_RECORDS_FOUND) {
					event.setDeletedOrExpired(true);
					return;
				}
			}
			JSONObject jObjEvent = jObjCityevent.getJSONObject(KEY_EVENTS).getJSONObject(KEY_EVENT);
			
			if (jObjEvent.has(KEY_ARTIST)) {
				event.setHasArtists(true);
				fillArtists(event, jObjEvent);
				
			} else {
				event.setHasArtists(false);
				//Log.i(TAG, "No artist found belonging to this event.");
			}
			
			if (event.getLowResImgUrl() == null) {
				//Log.d(TAG, "getLowResImgUrl() = null");
				if (jObjEvent.has(KEY_IMAGE)) {
					event.setImageUrl(jObjEvent.getString(KEY_IMAGE));
				}
				if (jObjEvent.has(KEY_IMAGE_ATTRIBUTION)) {
					event.setImageAttribution(getImageAttribution(jObjEvent.getJSONObject(KEY_IMAGE_ATTRIBUTION)));
				}
			}
			
			if (jObjEvent.has(KEY_DESC)) {
				event.setDescription(ConversionUtil.removeBuggyTextsFromDesc(ConversionUtil.decodeHtmlEntities(jObjEvent, KEY_DESC)));
			}
			
			JSONObject jObjSchedule = jObjEvent.getJSONObject(KEY_SCHEDULE); 
			if (event.getSchedule() == null) {
				SparseArray<Venue> venues = getVenues(jObjCityevent);
				event.setSchedule(getSchedule(jObjSchedule, venues));
			}

            if (jObjEvent.has(KEY_WEBSITE)) {
                event.setWebsite(jObjEvent.getString(KEY_WEBSITE));
            }
			
			if (jObjSchedule.has(KEY_BOOKINGINFO) && event.getSchedule().getBookingInfos().isEmpty()) {
				fillBookingInfo(event.getSchedule(), jObjSchedule);
				
			} else {
				//Log.i(TAG, "No booking info found for this event.");
			}
			
			//Log.d(TAG, "AT issue fillEventDetails event = " + event);
			/*if (jObjEvent.has(KEY_ATTENDING)) {
				Log.d(TAG, "AT issue fillEventDetails jObjEvent.has(KEY_ATTENDING) = " + jObjEvent.getInt(KEY_ATTENDING));
				
			} else {
				Log.d(TAG, "AT issue fillEventDetails !jObjEvent.has(KEY_ATTENDING)");
			}*/
			Attending attending = jObjEvent.has(KEY_ATTENDING) ? 
					Attending.getAttending(jObjEvent.getInt(KEY_ATTENDING)) : Attending.NOT_GOING;
			event.setAttending(attending);
			//Log.d(TAG, "AT issue fillEventDetails event.getAttending() = " + event.getAttending().getValue());

            JSONObject jObjVenue = jObjCityevent.getJSONObject(KEY_VENUES).getJSONObject(KEY_VENUE);
			JSONObject jObjAddress = jObjVenue.getJSONObject(KEY_ADDRESS);
            Venue venue = event.getSchedule().getVenue();
			venue.setAddress(getAddress(jObjAddress));
            if (venue.getPhone() == null) {
                // for navigation button on event details screen (via NaviBridge app, which displays phone number as well)
                venue.setPhone(getPhone(jObjVenue));
            }

			if (jObjEvent.has(KEY_FRIENDS) || jObjEvent.has(KEY_SUGGESTED_INVITES)) {
				JSONObject jObjFriends = jObjEvent.has(KEY_FRIENDS) ? jObjEvent.getJSONObject(KEY_FRIENDS) 
						: null;
				Object jSuggestedInvites = jObjEvent.has(KEY_SUGGESTED_INVITES) ? 
						jObjEvent.get(KEY_SUGGESTED_INVITES) : null;
				/**
				 * use getFriends followed by addAll; otherwise using setFriends directly will just
				 * change the entire list object & it might cause issues if we have previously passed friends list
				 * object somewhere & it's watching for changes in same list object. 
				 */
				event.getFriends().addAll(getFriends(jObjFriends, jSuggestedInvites));
				
			} else {
				/**
				 * clear list so that if dummy placeholders are added by screens indicating loading, 
				 * then it gets removed.
				 */
				event.getFriends().clear();
			}
			
			if (jObjEvent.has(KEY_LINKS)) {
				JSONObject jObjLinks = jObjEvent.getJSONObject(KEY_LINKS);
				if (jObjLinks.has(KEY_TRACKBACK_URL)) {
					event.setEventUrl(jObjLinks.getString(KEY_TRACKBACK_URL));
				}

                if (jObjLinks.has(KEY_SOCIAL_URL)) {
                    Object objSocialUrl = jObjLinks.get(KEY_SOCIAL_URL);

                    if (objSocialUrl instanceof JSONArray) {
                        JSONArray jArrSocialUrl = (JSONArray) objSocialUrl;

                        for (int i = 0; i < jArrSocialUrl.length(); i++) {
                            String link = jArrSocialUrl.getString(i);
                            if (link.contains("//www.facebook.com/")) {
                                event.setFbLink(link);
                                break;
                            }
                        }

                    } else if (((String) objSocialUrl).contains("//www.facebook.com/")) {
                        event.setFbLink((String) objSocialUrl);
                    }
                }
			}
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

    private String getPhone(JSONObject jObjVenue) throws JSONException {
        String phone = null;
        if (jObjVenue.has(KEY_PHONE)) {
            Object jPhone = jObjVenue.get(KEY_PHONE);
            if (jPhone instanceof JSONArray) {
                //Log.d(TAG, "array");
                phone = ((JSONArray) jPhone).getString(0);
                phone = ConversionUtil.parseForPhone(phone);

            } else {
                //Log.d(TAG, "not array");
                phone = (String) jPhone;
                phone = ConversionUtil.parseForPhone(phone);
            }
        }
        return phone;
    }
	
	private List<Friend> getFriends(JSONObject jObjFriends, Object jSuggestedInvites) throws JSONException {
		List<Friend> friends = new ArrayList<Friend>();
		
		if (jObjFriends != null) {
			if (jObjFriends.has(KEY_GOINGTO)) {
				Object jGoingto = jObjFriends.get(KEY_GOINGTO);
				
				if (jGoingto instanceof JSONArray) {
					JSONArray jArrGoingto = (JSONArray) jGoingto;
					for (int i = 0; i < jArrGoingto.length(); i++) {
						JSONObject jObjFriend = jArrGoingto.getJSONObject(i);
						Friend friend = getFriend(jObjFriend);
						friend.setAttending(Attending.GOING);
						friends.add(friend);
					}
					
				} else {
					Friend friend = getFriend((JSONObject) jGoingto);
					friend.setAttending(Attending.GOING);
					friends.add(friend);
				}
			}
			
			if (jObjFriends.has(KEY_WANTTO)) {
				Object jWantto = jObjFriends.get(KEY_WANTTO);
				
				if (jWantto instanceof JSONArray) {
					JSONArray jArrWantto = (JSONArray) jWantto;
					for (int i = 0; i < jArrWantto.length(); i++) {
						JSONObject jObjFriend = jArrWantto.getJSONObject(i);
						Friend friend = getFriend(jObjFriend);
						friend.setAttending(Attending.WANTS_TO_GO);
						friends.add(friend);
					}
					
				} else {
					Friend friend = getFriend((JSONObject) jWantto);
					friend.setAttending(Attending.WANTS_TO_GO);
					friends.add(friend);
				}
			}
		}
		
		if (jSuggestedInvites != null) {
			
			if (jSuggestedInvites instanceof JSONArray) {
				JSONArray jArrSuggestedInvites = (JSONArray) jSuggestedInvites;
				for (int i = 0; i < jArrSuggestedInvites.length(); i++) {
					JSONObject jObjFriend = jArrSuggestedInvites.getJSONObject(i);
					Friend friend = getFriend(jObjFriend);
					friend.setAttending(Attending.WANTS_TO_GO);
					friends.add(friend);
				}
				
			} else {
				Friend friend = getFriend((JSONObject) jSuggestedInvites);
				friend.setAttending(Attending.WANTS_TO_GO);
				friends.add(friend);
			}
		}
		return friends;
	}
	
	private Friend getFriend(JSONObject jsonObject) throws JSONException {
		Friend friend = new Friend();
		friend.setId(jsonObject.getString(KEY_ID));
		friend.setName(ConversionUtil.decodeHtmlEntities(jsonObject, KEY_NAME));
		friend.setImgUrl(jsonObject.getString(KEY_IMAGE));
		return friend;
	}
	
	private Address getAddress(JSONObject jObjAddress) throws JSONException {
		Address address = new Address();
		address.setAddress1(ConversionUtil.decodeHtmlEntities(jObjAddress, KEY_ADDRESS1));
		if (jObjAddress.has(KEY_ADDRESS2)) {
			address.setAddress2(ConversionUtil.decodeHtmlEntities(jObjAddress, KEY_ADDRESS2));
		}
		address.setCity(ConversionUtil.decodeHtmlEntities(jObjAddress, KEY_CITY));
		address.setCountry(getCountry(jObjAddress.getJSONObject(KEY_COUNTRY)));
		if (jObjAddress.has(KEY_ZIP)) {
			address.setZip(jObjAddress.getString(KEY_ZIP));
		}
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
		country.setName(ConversionUtil.decodeHtmlEntities(jsonObject, KEY_NAME));
		return country;
	}
	
	private void fillArtists(Event event, JSONObject jObjEvent) throws JSONException {
		List<Artist> artists = event.getArtists();
		artists.clear();
		
		Object objArtist = jObjEvent.get(KEY_ARTIST);
		
		if (objArtist instanceof JSONArray) {
			JSONArray jArrArtists = (JSONArray) objArtist;
			for (int i = 0; i < jArrArtists.length(); i++) {
				JSONObject jObjArtist = jArrArtists.getJSONObject(i);
				artists.add(getArtist(jObjArtist));
			}
			
		} else {
			artists.add(getArtist((JSONObject) objArtist));
		}
	}
	
	private void fillBookingInfo(Schedule schedule, JSONObject jObjSchedule) throws JSONException {
		JSONObject jObjBookingInfo = jObjSchedule.getJSONObject(KEY_BOOKINGINFO);
		if (jObjBookingInfo.has(KEY_BOOKINGLINK)) {
			Object objBookinglink = jObjBookingInfo.get(KEY_BOOKINGLINK);
			
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
	}
	
	private BookingInfo getBookingInfo(JSONObject jObjBookinglink) throws JSONException {
		BookingInfo bookingInfo = new BookingInfo();
		bookingInfo.setBookingUrl(jObjBookinglink.getString(KEY_BOOKING_URL));
		if (jObjBookinglink.has(KEY_PROVIDER)) {
			bookingInfo.setProvider(ConversionUtil.decodeHtmlEntities(jObjBookinglink, KEY_PROVIDER));
		}
		if (jObjBookinglink.has(KEY_PRICE)) {
			JSONObject jObjPrice = jObjBookinglink.getJSONObject(KEY_PRICE);
			String strPriceVal = jObjPrice.getString(KEY_VALUE);
			bookingInfo.setPrice(ConversionUtil.stringToFloat(strPriceVal));
			bookingInfo.setCurrency(jObjPrice.getString(KEY_CURRENCY));
		}
		return bookingInfo;
	}
	
	private Artist getArtist(JSONObject jsonObject) throws JSONException {
		Artist artist = new Artist(jsonObject.getInt(KEY_ARTIST_ID), ConversionUtil.decodeHtmlEntities(jsonObject, KEY_ARTIST_NAME));
		if (jsonObject.has(KEY_ARTIST_IMAGE)) {
			JSONObject jObjArtistImage = jsonObject.getJSONObject(KEY_ARTIST_IMAGE);
			
			String lowResImgUrl = jObjArtistImage.getString(KEY_LOWRES);
			String[] urlParts = lowResImgUrl.split("/");
			artist.setImageName(urlParts[urlParts.length - 1]);
			
			ImageAttribution imageAttribution = new ImageAttribution();
			imageAttribution.setLowResPath(lowResImgUrl.substring(0, lowResImgUrl.lastIndexOf("/") + 1));
			artist.setImageAttribution(imageAttribution);
			
			String mobiResImgUrl = jObjArtistImage.getString(KEY_MOBIRES);
			imageAttribution.setMobiResPath(mobiResImgUrl.substring(0, mobiResImgUrl.lastIndexOf("/") + 1));
			
			if (jObjArtistImage.has(KEY_HIGHRES)) {
				String highResImgUrl = jObjArtistImage.getString(KEY_HIGHRES);
				imageAttribution.setHighResPath(highResImgUrl.substring(0, highResImgUrl.lastIndexOf("/") + 1));
			}
		}
		
		return artist;
	}

	public List<Event> getEventList(JSONObject jsonObject) {
		List<Event> events = new ArrayList<Event>();
		SparseArray<Venue> venues = null;
		
		try {
			JSONObject jObjCityevent = jsonObject.getJSONObject(KEY_CITYEVENT);
			if (jObjCityevent.has(KEY_VENUES)) {
				venues = getVenues(jObjCityevent);
			}
			
			if (jObjCityevent.has(KEY_EVENTS)) {
				Object jsonEvent = jObjCityevent.getJSONObject(KEY_EVENTS).get(KEY_EVENT);
				
				if (jsonEvent instanceof JSONArray) {
					JSONArray jArrEvts = (JSONArray) jsonEvent;
					for (int i = 0; i < jArrEvts.length(); i++) {
						try {
							Event event = getEvent(jArrEvts.getJSONObject(i), venues);
							events.add(event);
							
						} catch (JSONException e) {
							e.printStackTrace();
							continue;
						}
					}
					
				} else {
					Event event = getEvent((JSONObject) jsonEvent, venues);
					events.add(event);
				}
			}
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return events;
	}
	
	public ItemsList<Event> getEventItemList(JSONObject jsonObject, GetEventsFrom from) {
		ItemsList<Event> itemsList = new ItemsList<Event>();
		try {
			if (from == GetEventsFrom.FEATURED_EVENTS) {
				JSONObject jObjFeaturedEvents = jsonObject.getJSONObject(KEY_FEATURED_EVENT);
				if (!jObjFeaturedEvents.has(KEY_TOTAL)) {
					return itemsList;
				}
				itemsList.setTotalCount(jObjFeaturedEvents.getInt(KEY_TOTAL));
				itemsList.setItems(getFeaturedEventList(jsonObject));
				
			} else {
				JSONObject jObjCityevent = jsonObject.getJSONObject(KEY_CITYEVENT);
				if (jObjCityevent.has(KEY_EVENTS)) {
					JSONObject jsonEvents = jObjCityevent.getJSONObject(KEY_EVENTS);
					if (!jsonEvents.has(KEY_TOTAL)) {
						return itemsList;
					}
					itemsList.setTotalCount(jsonEvents.getInt(KEY_TOTAL));
					itemsList.setItems(getEventList(jsonObject));
				}
			} 
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return itemsList;
	}
	
	private SparseArray<Venue> getVenues(JSONObject jObjCityevent) throws JSONException {
		SparseArray<Venue> venues = new SparseArray<Venue>();
		
		Object jsonVenue = jObjCityevent.getJSONObject(KEY_VENUES).get(KEY_VENUE);
		if (jsonVenue instanceof JSONArray) {
			JSONArray jArrVenues = (JSONArray) jsonVenue;
			
			for (int i = 0; i < jArrVenues.length(); i++) {
				Venue venue = getVenue(jArrVenues.getJSONObject(i));
				venues.append(venue.getId(), venue);
			}
			
		} else {
			Venue venue = getVenue((JSONObject) jsonVenue);
			venues.append(venue.getId(), venue);
		}
		
		return venues;
	}
	
	private Venue getVenue(JSONObject jsonObject) throws JSONException {
		Venue venue = new Venue(jsonObject.getInt(KEY_ID));
		venue.setName(ConversionUtil.decodeHtmlEntities(jsonObject, KEY_NAME));
		if (jsonObject.has(KEY_ADDRESS)) {
			venue.setAddress(getAddress(jsonObject.getJSONObject(KEY_ADDRESS)));
		}
        venue.setPhone(getPhone(jsonObject));
		return venue;
	}

	public List<Event> getFeaturedEventList(JSONObject jsonObject) {
		List<Event> featuredEvts = new ArrayList<Event>();
		
		try {
			JSONObject jObjFeaturedEvents = jsonObject.getJSONObject(KEY_FEATURED_EVENT);
			if (jObjFeaturedEvents.has(KEY_EVENTS)) {
				Object jEvents = jsonObject.getJSONObject(KEY_FEATURED_EVENT).get(KEY_EVENTS);
				if (jEvents instanceof JSONArray) {
					JSONArray jArrEvts = (JSONArray) jEvents;
					
					for (int i = 0; i < jArrEvts.length(); i++) {
						Event event = getEvent(jArrEvts.getJSONObject(i), null);
						featuredEvts.add(event);
					}
					
				} else {
					Event event = getEvent((JSONObject) jEvents, null);
					featuredEvts.add(event);
				}
				
			}
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return featuredEvts;
	}
	
	private Event getEvent(JSONObject jsonObject, SparseArray<Venue> venues) throws JSONException {
		Event event = new Event(jsonObject.getInt(KEY_ID), ConversionUtil.decodeHtmlEntities(jsonObject, KEY_NAME));
		boolean hasArtists = jsonObject.has(KEY_ARTIST) ? true : false;
		event.setHasArtists(hasArtists);
		
		if (jsonObject.has(KEY_DESC)) {
			event.setDescription(ConversionUtil.removeBuggyTextsFromDesc(ConversionUtil.decodeHtmlEntities(
					jsonObject, KEY_DESC)));
		}
		if (jsonObject.has(KEY_CITY_ID)) {
			event.setCityId(jsonObject.getInt(KEY_CITY_ID));
		}
		if (jsonObject.has(KEY_CITY_NAME)) {
			event.setCityName(ConversionUtil.decodeHtmlEntities(jsonObject, KEY_CITY_NAME));
		}
		if (jsonObject.has(KEY_IMAGE)) {
			event.setImageUrl(jsonObject.getString(KEY_IMAGE));
		}
		if (jsonObject.has(KEY_IMAGE_ATTRIBUTION)) {
			event.setImageAttribution(getImageAttribution(jsonObject.getJSONObject(KEY_IMAGE_ATTRIBUTION)));
		}
		if (jsonObject.has(KEY_SCHEDULE) && jsonObject.has(KEY_DATE)) {
			/**
			 * This case is for Featured Events in Ford
			 */
			buildSchedule(jsonObject, event);
			fillBookingInfo(event.getSchedule(), jsonObject.getJSONObject(KEY_SCHEDULE));
			
		} else if (jsonObject.has(KEY_SCHEDULE)) {
			event.setSchedule(getSchedule(jsonObject.getJSONObject(KEY_SCHEDULE), venues));	
			
		} else if (jsonObject.has(KEY_DATE)) {
			buildSchedule(jsonObject, event);
		}

		if (jsonObject.has(KEY_ARTIST)) {
			fillArtists(event, jsonObject);
			
		} else {
			//Log.i(TAG, "No artist found belonging to this event.");
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

	private void buildSchedule(JSONObject jsonObject, Event event) {
		try {
			List<Date> dates = new ArrayList<Date>();
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
            Date date = new Date(startTimeAvailable);
			String strDate = jsonObject.getString(KEY_DATE);
            try {
                date.setStartDate(format.parse(strDate + eventTime));

            } catch (ParseException e) {
                e.printStackTrace();
            }
			dates.add(date);

			int venueId = jsonObject.getInt(KEY_VENUE_ID);
			String venueName = ConversionUtil.decodeHtmlEntities(jsonObject, KEY_VENUE_NAME);
			event.setSchedule(buildSchedule(dates, venueId, venueName, null));
			
			Venue venue = event.getSchedule().getVenue();
			if (jsonObject.has(KEY_LATITUDE)) {
				Address address = venue.getAddress();
				if (address == null) {
					address = new Address();
					venue.setAddress(address);
				}
				address.setLat(jsonObject.getDouble(KEY_LATITUDE));
				address.setLon(jsonObject.getDouble(KEY_LONGITUDE));
				if (jsonObject.has(KEY_ADDRESS1)) {
					address.setAddress1(jsonObject.getString(KEY_ADDRESS1));
				}
				if (jsonObject.has(KEY_ADDRESS2)) {
					address.setAddress2(jsonObject.getString(KEY_ADDRESS2));
				}
				if (jsonObject.has(KEY_ZIP)) {
					address.setZip(jsonObject.getString(KEY_ZIP));
				}
				if (jsonObject.has(KEY_CITY_NAME)) {
					address.setCity(ConversionUtil.decodeHtmlEntities(jsonObject, KEY_CITY_NAME));
				}
				if (jsonObject.has(KEY_COUNTRY)) {
					Country c = new Country();
					c.setName(ConversionUtil.decodeHtmlEntities(jsonObject, KEY_COUNTRY));
					address.setCountry(c);
				}
			}
			
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
			
			if (jsonObject.has(KEY_COUNTRY)) {
				Country country = new Country();
				country.setName(jsonObject.getString(KEY_COUNTRY));
					
				if (venue.getAddress() == null) {
					Address address = new Address();
					address.setCountry(country);
					venue.setAddress(address);
					
				} else {
					venue.getAddress().setCountry(country);
					
				}
			}		
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private ImageAttribution getImageAttribution(JSONObject jsonObject) throws JSONException {
		ImageAttribution imageAttribution = new ImageAttribution();
		imageAttribution.setHighResPath(jsonObject.getString(KEY_HIGH_RES_PATH));
		imageAttribution.setLowResPath(jsonObject.getString(KEY_LOW_RES_PATH));
		imageAttribution.setMobiResPath(jsonObject.getString(KEY_MOBI_RES_PATH));
		return imageAttribution;
	}
	
	private Schedule getSchedule(JSONObject jsonObject, SparseArray<Venue> venues) throws JSONException {
		String eventTime = "";
		if (jsonObject.has(KEY_EVENT_TIME)) {
			eventTime = jsonObject.getString(KEY_EVENT_TIME);
		}

		List<Date> dates = getDates(jsonObject.get(KEY_DATES), eventTime);
		int venueId = jsonObject.getInt(KEY_VENUE_ID);

		Schedule schedule = buildSchedule(dates, venueId, null, venues);
		
		if (jsonObject.has(KEY_BOOKINGINFO)) {
			fillBookingInfo(schedule, jsonObject);
			
		} else {
			//Log.i(TAG, "No booking info found for this event.");
		}
		return schedule;
	}
	
	private Schedule buildSchedule(List<Date> dates, int venueId, String venueName, SparseArray<Venue> venues) {
		Schedule schedule = new Schedule();
		
		Venue venue;
		if (venues == null) {
			venue = new Venue(venueId);
			venue.setName(venueName);
			
		} else {
			venue = venues.get(venueId);
		}
		schedule.setVenue(venue);
		schedule.setDates(dates);
		return schedule;
	}

    private List<Date> getDates(Object json, String time) throws JSONException {
        List<Date> dates = new ArrayList<Date>();

        SimpleDateFormat format;
        boolean startTimeAvailable;

        if (time.equals("")) {
            format = new SimpleDateFormat("yyyy-MM-dd");
            startTimeAvailable = false;

        } else {
            format = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss");
            startTimeAvailable = true;
        }

        if (json instanceof JSONObject) {
            com.wcities.eventseeker.core.Date date = new com.wcities.eventseeker.core.Date(startTimeAvailable);

            JSONObject jObjDate = (JSONObject) json;
            String strStartDate = jObjDate.getString(KEY_START);
            String strEndDate = jObjDate.getString(KEY_END);

            try {
                date.setStartDate(format.parse(strStartDate + time));
                // for festivals start & end dates in single json object may vary
                if (!strStartDate.equals(strEndDate)) {
                    date.setEndDate(format.parse(strEndDate + time));
                }

            } catch (ParseException e) {
                e.printStackTrace();
            }

            dates.add(date);

        } else {
            JSONArray jArrDates = (JSONArray) json;

            for (int i = 0; i < jArrDates.length(); i++) {
                com.wcities.eventseeker.core.Date date = new com.wcities.eventseeker.core.Date(startTimeAvailable);

                String strStartDate = jArrDates.getJSONObject(i).getString(KEY_START);
                try {
                    date.setStartDate(format.parse(strStartDate + time));

                } catch (ParseException e) {
                    e.printStackTrace();
                }
                dates.add(date);
            }
        }
        return dates;
    }
}
