package com.wcities.eventseeker.constants;

import android.support.v4.app.Fragment;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.util.FragmentUtil;

public class Enums {
	
	public static enum Service {
    	Title(0, R.string.service_title, R.drawable.placeholder, 0, false, null),
    	GooglePlay(1, R.string.service_google_play, R.drawable.ic_google_play, 
    			R.drawable.ic_google_play_pressed, true, "googleplay"),
    	DeviceLibrary(2, R.string.service_device_library, R.drawable.ic_device_library, 
    			R.drawable.ic_device_library_pressed, true, "devicelibrary"),
    	Twitter(3, R.string.service_twitter, R.drawable.ic_twitter, 
    			R.drawable.ic_twitter_pressed, true, "twitter"),
    	Spotify(4, R.string.service_spotify, R.drawable.ic_spotify, 
    			R.drawable.ic_spotify_pressed, true, "spotify"),
    	Rdio(5, R.string.service_rdio, R.drawable.ic_rdio, 
    			R.drawable.ic_rdio_pressed, true, "rdio"),
    	Lastfm(6, R.string.service_last_fm, R.drawable.ic_lastfm, 
    			R.drawable.ic_lastfm_pressed, true, "lastfm"),
    	Pandora(7, R.string.service_pandora, R.drawable.ic_pandora, 
    			R.drawable.ic_pandora_pressed, true, "pandora"),
    	Button(8, R.string.service_button, R.drawable.placeholder, 0, false, null);
    	
    	private int intId;
    	private int strResId;
    	private int normalDrwResId;
    	private int pressedDrwResId;
    	private boolean isService;
    	private String artistSource;
    	
    	private Service(int intId, int strResId, int normalDrwResId, int pressedDrwResId, boolean isService, String artistSource) {
    		this.intId = intId;
    		this.strResId = strResId;
    		this.normalDrwResId = normalDrwResId;
    		this.pressedDrwResId = pressedDrwResId;
    		this.isService = isService;
    		this.artistSource = artistSource;
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
		
		public static SettingsItem getSettingsItemByOrdinal(int ordinal) {
			for (SettingsItem settingsItem : SettingsItem.values()) {
				if (settingsItem.ordinal() == ordinal) {
					return settingsItem;
				}
			}
			return null;
		}
	}
}
