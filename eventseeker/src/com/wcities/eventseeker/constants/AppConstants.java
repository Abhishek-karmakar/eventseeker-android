package com.wcities.eventseeker.constants;

public class AppConstants {
	
	public static final boolean DEBUG = false;
	public static final boolean FORD_SYNC_APP = false;
	
	public static final int TCP_PORT = 50007;
	public static final String TCP_IP_ADDRESS = "192.168.1.66";
	
	public static final String TWITTER_CONSUMER_KEY = "NVT497UQPpKrtehHabyog";
	public static final String TWITTER_CONSUMER_SECRET = "kxAMzj2HGgQDtcoecwx35lK4etZhM6eQqQ3R9WgeZtI";
	public static final String TWITTER_CALLBACK_URL = "com.wcities.eventseeker://twitter_callback";
	
	public static final String RDIO_KEY = "x83dzkx2xdmxuqtguqdz2nj6";
	public static final String RDIO_SECRET = "rXNJ5ajSut";
	
	public static final String LASTFM_API_KEY = "5f7e82824ba8ba0fe1cbe2a6ea80472e";

	public static final String GCM_SENDER_ID = "802382771850";

	public static final String CHARSET_NAME = "UTF-8";
	
	public static final String SHARED_PREFERENCES_NAME = "eventseeker_prefs";

	public static final String NAVIGATION_DRAWER_TITLE = "eventseeker";
	
	public static final float NOT_ALLOWED_LAT = 91;
	public static final float NOT_ALLOWED_LON = 181;
	public static double lat = NOT_ALLOWED_LAT, lon = NOT_ALLOWED_LON;
	
	public static final String FRAGMENT_TAG_DISCOVER = "discoverFragment";
	public static final String FRAGMENT_TAG_CHANGE_LOCATION = "changeLocationFragment";
	public static final String FRAGMENT_TAG_DISCOVER_BY_CATEGORY = "discoverByCategoryFragment";
	public static final String FRAGMENT_TAG_EVENT_DETAILS = "eventDetailsFragment";
	public static final String FRAGMENT_TAG_ARTIST_DETAILS = "artistDetailsFragment";
	public static final String FRAGMENT_TAG_VENUE_DETAILS = "venueDetailsFragment";
	public static final String FRAGMENT_TAG_SEARCH = "searchFragment";
	public static final String FRAGMENT_TAG_DATE_WISE_EVENT_LIST = "dateWiseEventListFragment";
	public static final String FRAGMENT_TAG_FOLLOWING = "followingFragment";
	public static final String FRAGMENT_TAG_FB_LOGIN = "fbLoginFragment";
	public static final String FRAGMENT_TAG_ARTISTS_NEWS_LIST = "artistsNewsFragment";
	public static final String FRAGMENT_TAG_FRIEND_LIST = "friendListFragment";
	public static final String FRAGMENT_TAG_MY_EVENTS = "myEventsFragment";
	public static final String FRAGMENT_TAG_FRIENDS_ACTIVITY = "friendsActivityFragment";
	public static final String FRAGMENT_TAG_CONNECT_ACCOUNTS = "connectAccountsFragment";
	public static final String FRAGMENT_TAG_ABOUT_US = "aboutUsFragment";
	public static final String FRAGMENT_TAG_EULA = "eulaFragment";
	public static final String FRAGMENT_TAG_REP_CODE = "repCodeFragment";
	public static final String FRAGMENT_TAG_ADDRESS_MAP = "addressMapFragment";
	public static final String FRAGMENT_TAG_FULL_SCREEN_ADDRESS_MAP = "fullScreenAddressMapFragment";
	public static final String FRAGMENT_TAG_DEVICE_LIBRARY = "deviceLibraryFragment";
	public static final String FRAGMENT_TAG_TWITTER = "twitterFragment";
	public static final String FRAGMENT_TAG_RDIO = "rdioFragment";
	public static final String FRAGMENT_TAG_LASTFM = "lastfmFragment";
	public static final String FRAGMENT_TAG_PANDORA = "pandoraFragment";

	public static final int INVALID_INDEX = -1;
	
	public static final int CATEGORY_ID_START = 900;
	public static final int TOTAL_CATEGORIES = 12;
}