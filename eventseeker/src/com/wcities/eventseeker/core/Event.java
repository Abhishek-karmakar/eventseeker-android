package com.wcities.eventseeker.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.wcities.eventseeker.cache.BitmapCacheable;

public class Event implements Serializable, BitmapCacheable {
	
	private static final String TAG = Event.class.getName();
	
	public static enum Attending {
		GOING(1),
		WANTS_TO_GO(2),
		NOT_GOING(3);
		
		private int value;
		
		private Attending(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
		
		public static Attending getAttending(int value) {
			Attending[] vals = values();
			for (Attending attending : vals) {
				if (attending.getValue() == value) {
					return attending;
				}
			}
			return NOT_GOING;
		}
	}

	private long id;
	private String name;
	private String imgName;
	private String imageUrl;
	private ImageAttribution imageAttribution;
	private int cityId;
	private String cityName;
	private Schedule schedule;
	private List<Artist> artists;
	private Attending attending;
	private String description;
	private List<Friend> friends;
	private String eventUrl;
	private boolean hasArtists = true;
	
	public Event(long id, String name) {
		this.id = id;
		this.name = name;
		artists = new ArrayList<Artist>();
		friends = new ArrayList<Friend>();
		attending = Attending.NOT_GOING;
	}

	public long getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public void setImgName(String imgName) {
		this.imgName = imgName;
	}

	public String getImageUrl() {
		return imageUrl;
	}
	
	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}
	
	public ImageAttribution getImageAttribution() {
		return imageAttribution;
	}
	
	public void setImageAttribution(ImageAttribution imageAttribution) {
		this.imageAttribution = imageAttribution;
	}
	
	public int getCityId() {
		return cityId;
	}
	
	public void setCityId(int cityId) {
		this.cityId = cityId;
	}
	
	public String getCityName() {
		return cityName;
	}
	
	public void setCityName(String cityName) {
		this.cityName = cityName;
	}

	public Schedule getSchedule() {
		return schedule;
	}

	public void setSchedule(Schedule schedule) {
		this.schedule = schedule;
	}
	
	public List<Artist> getArtists() {
		return artists;
	}
	
	public void setArtists(List<Artist> artists) {
		this.artists = artists;
	}

	public Attending getAttending() {
		return attending;
	}

	public void setAttending(Attending attending) {
		this.attending = attending;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	public List<Friend> getFriends() {
		return friends;
	}

	public void setFriends(List<Friend> friends) {
		this.friends = friends;
	}

	public String getEventUrl() {
		return eventUrl;
	}

	public void setEventUrl(String eventUrl) {
		this.eventUrl = eventUrl;
	}

	public boolean hasArtists() {
		return hasArtists;
	}

	public void setHasArtists(boolean hasArtists) {
		this.hasArtists = hasArtists;
	}

	private String getImgName() {
		//Log.d(TAG, "getImgName()");
		if (imgName != null) {
			return imgName;
			
		} else if (getImageUrl() != null) {
			String[] imgParts = getImageUrl().split("/");
			return imgParts[imgParts.length - 1];
		}
		return null;
	}
	
	public boolean doesValidImgUrlExist() {
		if (getLowResImgUrl() != null || getMobiResImgUrl() != null || getHighResImgUrl() != null) {
			return true;
			
		} else {
			return false;
		}
	}
	
	@Override
	public String getLowResImgUrl() {
		//Log.d(TAG, "getLowResImgUrl()");
		if (getImageAttribution() != null && getImgName() != null) {
			return getImageAttribution().getLowResPath() + getImgName();
		}
		return null;
	}
	
	@Override
	public String getMobiResImgUrl() {
		if (getImageAttribution() != null && getImgName() != null) {
			return getImageAttribution().getMobiResPath() + getImgName();
		}
		return null;
	}
	
	@Override
	public String getHighResImgUrl() {
		if (getImageAttribution() != null && getImgName() != null) {
			return getImageAttribution().getHighResPath() + getImgName();
		}
		return null;
	}

	@Override
	public String getKey(ImgResolution imgResolution) {
		return new StringBuilder(getClass().getName()).append("_").append(id).append("_").append(imgResolution).toString();
	}
}
