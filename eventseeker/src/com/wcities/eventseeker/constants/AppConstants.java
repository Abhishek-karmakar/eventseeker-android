package com.wcities.eventseeker.constants;

import java.util.Arrays;
import java.util.List;

import com.google.android.gms.common.Scopes;

public class AppConstants {
	
	public static final boolean IS_RELEASE_MODE = false;
	public static final boolean CHECK_CONNECTIVITY_STATUS = true;
	public static final boolean CRASH_REPORTING_ENABLED = false;
	public static final boolean STRICT_MODE_ENABLED = false;
	public static final boolean DEBUG = false;
	public static final boolean FORD_SYNC_APP = false;
	
	public static final int TCP_PORT = 50007;
	public static final String TCP_IP_ADDRESS = "192.168.1.66";
	
	public static String TWITTER_CONSUMER_KEY;
	public static String TWITTER_CONSUMER_SECRET;
	public static final String TWITTER_CALLBACK_URL = "com.wcities.eventseeker://twitter_callback";
	
	public static String RDIO_KEY;
	public static String RDIO_SECRET;
	
	public static String LASTFM_API_KEY;

	public static String GCM_SENDER_ID;

	public static final String CHARSET_NAME = "UTF-8";
	
	public static final String SHARED_PREFERENCES_NAME = "eventseeker_prefs";

	public static final String NAVIGATION_DRAWER_TITLE = "eventseeker";
	
	public static final float NOT_ALLOWED_LAT = 91;
	public static final float NOT_ALLOWED_LON = 181;
	public static double lat = NOT_ALLOWED_LAT, lon = NOT_ALLOWED_LON;
	public static boolean IS_CAR_STATIONARY = true;
	public static boolean IS_NIGHT_MODE_ENABLED;
	
	public static final String INVALID_DISTANCE = "0.0";
	
	public static final String FRAGMENT_TAG_DISCOVER = "discoverFragment";
	public static final String FRAGMENT_TAG_CHANGE_LOCATION = "changeLocationFragment";
	public static final String FRAGMENT_TAG_LANGUAGE = "language";
	public static final String FRAGMENT_TAG_DISCOVER_BY_CATEGORY = "discoverByCategoryFragment";
	public static final String FRAGMENT_TAG_EVENT_DETAILS = "eventDetailsFragment";
	public static final String FRAGMENT_TAG_ARTIST_DETAILS = "artistDetailsFragment";
	public static final String FRAGMENT_TAG_VENUE_DETAILS = "venueDetailsFragment";
	public static final String FRAGMENT_TAG_SEARCH = "searchFragment";
	public static final String FRAGMENT_TAG_DATE_WISE_EVENT_LIST = "dateWiseEventListFragment";
	public static final String FRAGMENT_TAG_FOLLOWING = "followingFragment";
	public static final String FRAGMENT_TAG_GET_STARTED = "getStartedFragment";
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
	public static final String FRAGMENT_TAG_LOGIN_SYNCING = "LoginSyncingFragment";
	public static final String FRAGMENT_TAG_TWITTER = "twitterFragment";
	public static final String FRAGMENT_TAG_RDIO = "rdioFragment";
	public static final String FRAGMENT_TAG_LASTFM = "lastfmFragment";
	public static final String FRAGMENT_TAG_PANDORA = "pandoraFragment";
	public static final String FRAGMENT_TAG_TICKET_PROVIDER_DIALOG = "ticketProviderDialogFragment";
	public static final String FRAGMENT_TAG_WEB_VIEW = "webViewFragment";
	public static final String FRAGMENT_TAG_TWITTER_SYNCING = "twitterSyncingFragment";
	public static final String FRAGMENT_TAG_GOOGLE_PLAY_MUSIC = "GooglePlayMusicFragment";
	
	public static final String DIALOG_FRAGMENT_TAG_LOGIN_TO_TRACK_EVENT = "loginToTrackEventDialog";
	public static final String DIALOG_FRAGMENT_TAG_LOGIN_TO_SUBMIT_REP_CODE = "loginToSubmitRepCodeDialog";
	
	public static final int INVALID_INDEX = -1;
	public static final int INVALID_ID = -1;
	public static final String INVALID_STR_ID = "-1";
	
	public static final int CATEGORY_ID_START = 900;
	public static final int TOTAL_CATEGORIES = 12;
	
	public static final String TMP_SHARE_IMG_FOLDER = "/share_img";
	public static final String TMP_SHARE_IMG_PREFIX = "img";
	
	public static final int REQ_CODE_INVITE_FRIENDS = 1001;
	public static final int REQ_CODE_RATE_APP = 1002;
	public static final int REQ_CODE_GOOGLE_ACCOUNT_CHOOSER_FOR_GOOGLE_MUSIC = 1003;
	
	public static final List<String> PERMISSIONS_FB_LOGIN = Arrays.asList("email");
	// List of additional write permissions being requested
	public static final List<String> PERMISSIONS_FB_PUBLISH_EVT = Arrays.asList("publish_actions");
	// Request code for facebook reauthorization requests. 
	public static final int REQ_CODE_FB_PUBLISH_EVT = 100;
	public static final int REQ_CODE_FB_LOGIN_EMAIL = 101;
	public static final int REQ_CODE_GOOGLE_PLUS_PUBLISH_EVT = 200;
	
	public static final int REQ_CODE_GOOGLE_PLUS_RESOLVE_ERR = 9000;
	public static final int REQ_CODE_GET_GOOGLE_PLAY_SERVICES = 9001;
	public static final int REQ_CODE_GOOGLE_AUTH_CODE_FOR_SERVER_ACCESS = 9002;

	/**
	 * To prevent infinite loop when network is off & we are calling requestPublishPermissions() of FbUtil.
	 * This is the max limit for the looping
	 */
	public static final int MAX_FB_CALL_COUNT_FOR_SAME_EVT = 20;
	
	public static final String TAG_PROGRESS_INDICATOR = "progressIndicator";
	public static final String TAG_CONTENT = "content";

	public static final int SCROLL_Y_BY = 100;
	
	public static final String[] GOOGLE_PLUS_ACTION = new String[] {"http://schemas.google.com/AddActivity"};
	public static final String[] GOOGLE_PLUS_SCOPES = new String[] {"https://www.googleapis.com/auth/userinfo.email", 
		Scopes.PLUS_LOGIN, Scopes.PLUS_PROFILE, "https://www.googleapis.com/auth/userinfo.profile", 
		"https://www.googleapis.com/auth/plus.profile.emails.read"};
	
	public static final String GOOGLE_PLUS_SCOPES_FOR_SERVER_ACCESS = "https://www.googleapis.com/auth/userinfo.email"  
		+ " " + Scopes.PLUS_LOGIN + " " + Scopes.PLUS_PROFILE + " " + "https://www.googleapis.com/auth/userinfo.profile" 
		+ " " + "https://www.googleapis.com/auth/plus.profile.emails.read";
	
	public static final String ACTION_GOING_TO = "eventseeker:going_to";
	public static final String ACTION_WANTS_TO_GO_TO = "eventseeker:wants_to_go_to";
}