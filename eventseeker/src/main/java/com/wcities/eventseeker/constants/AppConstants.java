package com.wcities.eventseeker.constants;

import com.google.android.gms.common.Scopes;
import com.wcities.eventseeker.DrawerListFragment;

import java.util.Arrays;
import java.util.List;

public class AppConstants {
	
	public static final boolean IS_RELEASE_MODE = false;
	public static final boolean CHECK_CONNECTIVITY_STATUS = true;
	public static final boolean SEND_GOOGLE_ANALYTICS = false;
	/**
	 * 'DEBUG' and 'FORD_SYNC_APP' variable is only for ford implementation
	 */
	public static final boolean DEBUG = false;
	public static final boolean FORD_SYNC_APP = true;
	
	public static final boolean CRASH_REPORTING_ENABLED = false;
	public static final boolean STRICT_MODE_ENABLED = false;
	
	public static final int TCP_PORT = 50007;
	public static final String TCP_IP_ADDRESS = "192.168.1.166"; //"192.168.1.173";
	
	public static String TWITTER_CONSUMER_KEY;
	public static String TWITTER_CONSUMER_SECRET;
	public static final String TWITTER_CALLBACK_URL = "com.wcities.eventseeker://twitter_callback";
	
	public static String SPOTIFY_CLIENT_ID;
	public static String SPOTIFY_CLIENT_SECRET;
	public static final String SPOTIFY_REDIRECT_URI = "com.wcities.eventseeker://spotify_callback";
	
	public static final String BEATS_MUSIC_REDIRECT_URI = "com.wcities.eventseeker://beats_music_callback";
	
	public static String RDIO_KEY;
	public static String RDIO_SECRET;
	
	public static String LASTFM_API_KEY;

	public static String GCM_SENDER_ID;
	
	public static String BROWSER_KEY_FOR_PLACES_API;

	public static final String CHARSET_NAME = "UTF-8";
	
	public static final String SHARED_PREFERENCES_NAME = "eventseeker_prefs";

	public static final String NAVIGATION_DRAWER_TITLE = "eventseeker";
	
	public static final float NOT_ALLOWED_LAT = 91;
	public static final float NOT_ALLOWED_LON = 181;
	public static double lat = NOT_ALLOWED_LAT, lon = NOT_ALLOWED_LON;
	public static boolean IS_CAR_STATIONARY = true;
	public static boolean IS_NIGHT_MODE_ENABLED;
	
	/**
	 * Removed these constants from BaseActivityTab and MainActivity. 
	 * Because of the Notification implementation.
	 */
	public static final int INDEX_NAV_ITEM_DISCOVER = 0;
	public static final int INDEX_NAV_ITEM_MY_EVENTS = INDEX_NAV_ITEM_DISCOVER + 1;
	public static final int INDEX_NAV_ITEM_FOLLOWING = INDEX_NAV_ITEM_MY_EVENTS + 1;
	public static final int INDEX_NAV_ITEM_ARTISTS_NEWS = INDEX_NAV_ITEM_FOLLOWING + 1;
	public static final int INDEX_NAV_ITEM_FRIENDS_ACTIVITY = INDEX_NAV_ITEM_ARTISTS_NEWS + 1;
	public static final int INDEX_NAV_ITEM_SETTINGS = DrawerListFragment.DIVIDER_POS + 1;
	public static final int INDEX_NAV_ITEM_LOG_OUT = INDEX_NAV_ITEM_SETTINGS + 1;
	
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
	public static final String FRAGMENT_TAG_LOGIN = "loginFragment";
	public static final String FRAGMENT_TAG_SIGN_UP = "signUpFragment";
	public static final String FRAGMENT_TAG_LAUNCHER = "launcherFragment";
	public static final String FRAGMENT_TAG_SETTINGS = "settingsFragment";
	public static final String FRAGMENT_TAG_FOLLOW_MORE_ARTISTS = "followMoreArtistsFragment";
	public static final String FRAGMENT_TAG_POPULAR_ARTISTS = "popularArtistsFragment";
	public static final String FRAGMENT_TAG_SPORTS_ARTISTS = "SportsArtistsFragment";
	public static final String FRAGMENT_TAG_MUSIC_ARTISTS = "musicArtistsFragment";
	public static final String FRAGMENT_TAG_RECOMMENDED_ARTISTS = "RecommendedArtistsFragment";
	public static final String FRAGMENT_TAG_SELECTED_ARTIST_CATEGORY_FRAGMENT = "selectedArtistCategoryFragment";
	public static final String FRAGMENT_TAG_SELECTED_FEATURED_LIST_ARTISTS_FRAGMENT = "selectedFeaturedListArtistsFragment";
	public static final String FRAGMENT_TAG_ARTISTS_NEWS_LIST = "artistsNewsFragment";
	public static final String FRAGMENT_TAG_ARTIST_NEWS_LIST = "artistNewsFragment";
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
	public static final String FRAGMENT_TAG_BEATS_MUSIC = "beatsMusicFragment";
	public static final String FRAGMENT_TAG_TICKET_PROVIDER_DIALOG = "ticketProviderDialogFragment";
	public static final String FRAGMENT_TAG_WEB_VIEW = "webViewFragment";
	public static final String FRAGMENT_TAG_TWITTER_SYNCING = "twitterSyncingFragment";
	public static final String FRAGMENT_TAG_GOOGLE_PLAY_MUSIC = "GooglePlayMusicFragment";
	public static final String FRAGMENT_TAG_NAVIGATION = "NavigationFragment";
	
	public static final String DIALOG_FRAGMENT_TAG_LOGIN_TO_SUBMIT_REP_CODE = "loginToSubmitRepCodeDialog";
	public static final String DIALOG_FRAGMENT_TAG_SUBMIT_REP_CODE_RESPONSE = "submitRepCodeResponseDialog";
	public static final String DIALOG_FRAGMENT_TAG_CONNECTION_LOST = "connectionLostDialog";
	public static final String DIALOG_FRAGMENT_TAG_EVENT_SAVED = "eventSavedDialog";
	
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
	public static final int REQ_CODE_SPOTIFY = 1004;
	public static final int REQ_CODE_BEATS = 1005;
    // requestCode observed in logs for twitter SSO is 140
    public static final int REQ_CODE_TWITTER = 140;
	
	public static final int MUSIC_NOTIFICATION_ID = 1;
	public static final int UNIQUE_GCM_NOTIFICATION_ID_START = 100;
	
	public static final List<String> PERMISSIONS_FB_LOGIN = Arrays.asList("email");
	// List of additional write permissions being requested
	public static final List<String> PERMISSIONS_FB_PUBLISH_EVT_OR_ART = Arrays.asList("publish_actions");
	public static final int REQ_CODE_GOOGLE_PLUS_PUBLISH_EVT = 200;
	
	public static final int REQ_CODE_GOOGLE_PLUS_RESOLVE_ERR = 9000;
	public static final int REQ_CODE_GET_GOOGLE_PLAY_SERVICES = 9001;
	public static final int REQ_CODE_GOOGLE_AUTH_CODE_FOR_SERVER_ACCESS = 9002;

	public static final String LAUNCHER_FRAGMENT_TITLE = "launcherFragmentTitle";
	public static final String LAUNCHER_FRAGMENT_DESC = "launcherFragmentDesc";
	
	public static final String LIST_OF_ARTISTS_NAMES = "ListArtistsNames";

	public static final String TAG_PROGRESS_INDICATOR = "progressIndicator";
	public static final String TAG_CONTENT = "content";

	public static final int SCROLL_Y_BY = 100;
	
	public static final String SCOPE_URI_USERINFO_EMAIL = "https://www.googleapis.com/auth/userinfo.email";
	public static final String SCOPE_URI_USERINFO_PROFILE = "https://www.googleapis.com/auth/userinfo.profile";
	public static final String SCOPE_URI_PLUS_PROFILE_EMAILS_READ = "https://www.googleapis.com/auth/plus.profile.emails.read";
	
	public static final String GOOGLE_PLUS_SCOPES_FOR_SERVER_ACCESS = SCOPE_URI_USERINFO_EMAIL  
		+ " " + Scopes.PLUS_LOGIN + " " + Scopes.PLUS_ME + " " + SCOPE_URI_USERINFO_PROFILE 
		+ " " + SCOPE_URI_PLUS_PROFILE_EMAILS_READ;
	
	public static final String ACTION_ADD = "eventseeker:add";
	
	/************************   starts for bosch   *******************************/
	
	public static final int REDUCE_TITLE_TXT_SIZE_BY_SP_FOR_BOSCH_DETAIL_SCREENS = 2;
	
	/************************    ends for bosch    *******************************/
	
	/************************   starts for Ford   *******************************/

	public static final String FORD_APP_ID = "3260118906";
	public static final int INTERACTION_TIME_OUT_AL = 5000;
	public static final int INVALID_RES_ID = -1;
	
	/************************    ends for Ford    *******************************/
}