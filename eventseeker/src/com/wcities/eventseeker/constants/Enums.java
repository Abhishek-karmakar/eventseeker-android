package com.wcities.eventseeker.constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.support.v4.app.Fragment;

import com.ford.syncV4.proxy.rpc.enums.Language;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.util.FragmentUtil;

public class Enums {

	public static enum Service {
    	Title(0, R.string.service_title, R.drawable.placeholder, 0, false, null, AppConstants.INVALID_ID),
    	GooglePlay(1, R.string.service_google_play, R.drawable.ic_google_play, 
    			R.drawable.ic_google_play_pressed, true, "googleplay", 1),
    	DeviceLibrary(2, R.string.service_device_library, R.drawable.ic_device_library, 
    			R.drawable.ic_device_library_pressed, true, "devicelibrary", 2),
    	Twitter(3, R.string.service_twitter, R.drawable.ic_twitter, 
    			R.drawable.ic_twitter_pressed, true, "twitter", 3),
    	Spotify(4, R.string.service_spotify, R.drawable.ic_spotify, 
    			R.drawable.ic_spotify_pressed, true, "spotify", 7),
    	Rdio(5, R.string.service_rdio, R.drawable.ic_rdio, 
    			R.drawable.ic_rdio_pressed, true, "rdio", 4),
    	Lastfm(6, R.string.service_last_fm, R.drawable.ic_lastfm, 
    			R.drawable.ic_lastfm_pressed, true, "lastfm", 5),
    	Pandora(7, R.string.service_pandora, R.drawable.ic_pandora, 
    			R.drawable.ic_pandora_pressed, true, "pandora", 6),
    	/*Beats(8, R.string.service_beats, R.drawable.ic_beats, 
    			R.drawable.ic_beats_pressed, true, "beatsmusic", 8),*/
    	Button(9, R.string.service_button, R.drawable.placeholder, 0, false, null, AppConstants.INVALID_ID);
    	
    	private int intId;
    	private int strResId;
    	private int normalDrwResId;
    	private int pressedDrwResId;
    	private boolean isService;
    	private String artistSource;
    	/**
    	 * 20-03-2015:
    	 * This stores the Server-side id for the corresponding Service.
    	 */
    	private int serverMappingId;
    	
    	private Service(int intId, int strResId, int normalDrwResId, int pressedDrwResId, boolean isService, 
    			String artistSource, int serverMappingId) {
    		this.intId = intId;
    		this.strResId = strResId;
    		this.normalDrwResId = normalDrwResId;
    		this.pressedDrwResId = pressedDrwResId;
    		this.isService = isService;
    		this.artistSource = artistSource;
    		this.serverMappingId = serverMappingId;
		}
    	
    	public int getNormalDrwResId() {
			return normalDrwResId;
		}
    	
    	public int getPressedDrwResId() {
    		return pressedDrwResId;
    	}
    	
    	public String getStr(Fragment fragment) {
			return FragmentUtil.getResources(fragment).getString(strResId);
		}
    	
    	public String getArtistSource() {
    		return artistSource;
    	}
    	
    	public int getIntId() {
			return intId;
		}
    	
    	public int getServerMappingId() {
			return serverMappingId;
		}

		public boolean equals(Service s, Fragment fragment) {
    		return getStringFromResId(strResId, fragment).equals(s.getStr(fragment));
		}
    	
    	public boolean isOf(String s, Fragment fragment) {
    		return getStringFromResId(strResId, fragment).equals(s);
    	}
    	
    	private String getStringFromResId(int strResId, Fragment fragment) {
			return FragmentUtil.getResources(fragment).getString(strResId);
		}
    	
    	public static Service getService(String s, Fragment fragment) {
    		Service[] services = Service.values();
    		for (int i = 0; i < services.length; i++) {
    			Service service = services[i];
    			if (service.isOf(s, fragment)) {
					return service;
				}
			}
    		return null;
    	}
    	
    	public boolean isService() {
			return isService;
		}
    	
    	public static int getServiceCount() {
    		int count = 0;
    		for (Service service : Service.values()) {
				if (service.isService()) {
					++count;
				}
			}
    		return count;
    	}
    }
	
	public enum SettingsItem {
		SYNC_ACCOUNTS(R.drawable.selector_sync, R.string.navigation_drawer_item_sync_accounts),	  	
		CHANGE_LOCATION(R.drawable.selector_changelocation, R.string.navigation_drawer_item_change_location),
		LANGUAGE(R.drawable.selector_language, R.string.navigation_drawer_item_language),
		INVITE_FRIENDS(R.drawable.selector_invitefriends, R.string.navigation_drawer_item_invite_friends),
		RATE_APP(R.drawable.selector_store, R.string.navigation_drawer_item_rate_app),
		ABOUT(R.drawable.selector_info, R.string.navigation_drawer_item_about),
		EULA(R.drawable.selector_eula, R.string.navigation_drawer_item_eula),
		REPCODE(R.drawable.selector_repcode, R.string.navigation_drawer_item_enter_rep_code);
		
		private int icon, title;
		private SettingsItem(int icon, int title) {
			this.icon = icon; 
			this.title = title;			
		}
		
		public int getIcon() {
			return icon;
		}
		
		public int getTitle() {
			return title;
		}
	}
	
	public enum Locales {
		ENGLISH("en", R.string.lang_english),
		FRENCH("fr", R.string.lang_french),
		GERMAN("de", R.string.lang_german),
		ITALIAN("it", R.string.lang_italian),
		SPANISH("es", R.string.lang_spanish),
		PORTUGUESE("pt", R.string.lang_portuguese),
		
		ENGLISH_AUSTRALIA("en", "AU", Language.EN_AU),
		ENGLISH_UNITED_KINGDOM("en", "GB", Language.EN_GB),
		ENGLISH_UNITED_STATES("en", "US", Language.EN_US),
		FRENCH_CANADA("fr", "CA", Language.FR_CA),
		FRENCH_FRANCE("fr", "FR", Language.FR_FR),
		GERMAN_GERMANY("de", "DE", Language.DE_DE),
		ITALIAN_ITALY("it", "IT", Language.IT_IT),
		SPANISH_SPAIN("es", "ES", Language.ES_ES),
		SPANISH_MEXICO("es", "MX", Language.ES_MX),
		PORTUGUESE_BRAZIL("pt", "BR", Language.PT_BR),
		PORTUGUESE_PORTUGAL("pt", "PT", Language.PT_PT);
		
		private Locales(String localeCode, int localeLanguage) {
			this.localeCode = localeCode;
			this.localeLanguage = localeLanguage;
		}
		
		private Locales(String localeCode, String countryCode, Language fordLanguage) {
			this.localeCode = localeCode;
			this.countryCode = countryCode;
			this.fordLanguage = fordLanguage;
		}
		
		private String localeCode;
		private int localeLanguage;
		private String countryCode;
		private Language fordLanguage;
		
		public String getLocaleCode() {
			return localeCode;
		}
		
		public void setLocaleCode(String localeCode) {
			this.localeCode = localeCode;
		}
		
		public int getLocaleLanguage() {
			return localeLanguage;
		}

		public String getCountryCode() {
			return countryCode;
		}

		public void setLocaleLanguage(int localeLanguage) {
			this.localeLanguage = localeLanguage;
		}
		
		// this function should not be used for ford
		public static Locales getLocaleByLocaleCode(String localeCode) {
			List<Locales> locales = Arrays.asList(Locales.values());
			for (Locales locale : locales) {
				if (locale.getLocaleCode().equals(localeCode) && locale.countryCode == null) {
					return locale;
				}
			}
			return ENGLISH;
		}

		public static boolean isDefaultLocale(Context context, Locales locale) {
			EventSeekr app = (EventSeekr) context.getApplicationContext();
			return locale.equals(app.getLocale());
		}
		
		public static Locales getFordLocaleByLanguage(Language language) {
			if (language == null) {
				return ENGLISH_UNITED_STATES;
			}
			List<Locales> locales = Arrays.asList(Locales.values());
			for (Locales locale : locales) {
				if (locale.fordLanguage == language) {
					return locale;
				}
			}
			return ENGLISH_UNITED_STATES;
		}
		
		public static Locales getFordLocaleByAppLocale(Locale locale) {
			String countryCode = locale.getCountry();
			String languageCode = locale.getLanguage();
			List<Locales> locales = Arrays.asList(Locales.values());
			for (Locales tmpLocale : locales) {
				if (tmpLocale.localeCode.equals(languageCode) && tmpLocale.countryCode != null && 
						tmpLocale.countryCode.equals(countryCode)) {
					return tmpLocale;
				}
			}
			return ENGLISH_UNITED_STATES;
		}
		
		public Language getFordLanguage() {
			return fordLanguage;
		}
		
		public static List<Locales> getMobileLocales() {
			List<Locales> mobileLocales = new ArrayList<Locales>();
			List<Locales> locales = Arrays.asList(Locales.values());
			for (Iterator<Locales> iterator = locales.iterator(); iterator.hasNext();) {
				Locales tmpLocale = iterator.next();
				if (tmpLocale.countryCode == null) {
					mobileLocales.add(tmpLocale);
					
				} else {
					break;
				}
			}
			return mobileLocales;
		}
	}
	
	public enum SortArtistNewsBy {
		chronological(0),
		trending(1);
		
		int value;
		private SortArtistNewsBy(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
		
		public static SortArtistNewsBy getSortTypeBy(int value) {
			for (SortArtistNewsBy sortBy : values()) {
				if (sortBy.getValue() == value) {
					return sortBy;
				}
			}
			return null;
		}
	}

	public static enum PublishRequest {
		LIKE,
		COMMENT;
	}

	public static enum SortRecommendedArtist {
		name(0),
		score(1);
		
		int value;
		private SortRecommendedArtist(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
		
		public static SortRecommendedArtist getSortTypeBy(int value) {
			for (SortRecommendedArtist sortBy : values()) {
				if (sortBy.getValue() == value) {
					return sortBy;
				}
			}
			return null;
		}
	}
}
