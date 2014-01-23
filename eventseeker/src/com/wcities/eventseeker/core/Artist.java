package com.wcities.eventseeker.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.cache.BitmapCacheable;
import com.wcities.eventseeker.core.ArtistLink.LinkType;

public class Artist implements Serializable, BitmapCacheable {
	
	public static final String DEFAULT_MOBI_RES_PATH = "http://c394756.r56.cf2.rackcdn.com/";
	public static final String DEFAULT_LOW_RES_PATH = "http://c0364947.cdn2.cloudfiles.rackspacecloud.com/";
	public static final String DEFAULT_HIGH_RES_PATH = "http://c0364946.cdn2.cloudfiles.rackspacecloud.com/";
	
	public static enum Attending {
		NotTracked(0),
		Tracked(1);
		
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
			return NotTracked;
		}
	}
	
	private int id;
	private String name;
	private String imageName;
	private String imageUrl;
	private boolean ontour;
	private ImageAttribution imageAttribution;
	private List<Event> events;
	private List<Friend> friends;
	private String description;
	private List<Video> videos;
	private Attending attending;
	private String artistUrl;
	private List<ArtistLink> listArtistLink;
	
	public Artist(int id, String name) {
		this.id = id;
		this.name = name;
		events = new ArrayList<Event>();
		friends = new ArrayList<Friend>();
		videos = new ArrayList<Video>();
		listArtistLink = new ArrayList<ArtistLink>();
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getImageName() {
		return imageName;
	}

	public void setImageName(String imageName) {
		this.imageName = imageName;
	}

	public boolean isOntour() {
		return ontour;
	}

	public void setOntour(boolean ontour) {
		this.ontour = ontour;
	}
	
	public ImageAttribution getImageAttribution() {
		return imageAttribution;
	}

	public void setImageAttribution(ImageAttribution imageAttribution) {
		this.imageAttribution = imageAttribution;
	}

	private String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public List<Friend> getFriends() {
		return friends;
	}

	public void setFriends(List<Friend> friends) {
		this.friends = friends;
	}

	public List<Event> getEvents() {
		return events;
	}

	public void setEvents(List<Event> events) {
		this.events = events;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<Video> getVideos() {
		return videos;
	}

	public void setVideos(List<Video> videos) {
		this.videos = videos;
	}

	public Attending getAttending() {
		return attending;
	}

	public void setAttending(Attending attending) {
		this.attending = attending;
	}
	
	public void updateAttending(Attending attending, EventSeekr eventSeekr) {
		this.attending = attending;
		if (attending == Attending.Tracked) {
			eventSeekr.getCachedFollowingList().followArtist(this);
			
		} else {
			eventSeekr.getCachedFollowingList().unfollowArtist(this);
		}
	}

	public String getArtistUrl() {
		return artistUrl;
	}

	public void setArtistUrl(String artistUrl) {
		this.artistUrl = artistUrl;
	}
	
	public boolean doesValidImgUrlExist() {
		if (getLowResImgUrl() != null || getMobiResImgUrl() != null || getHighResImgUrl() != null) {
			return true;
			
		} else {
			return false;
		}
	}

	public void addArtistLink(ArtistLink artistLink) {
		listArtistLink.add(artistLink);
	}
	
	public String getArtistLinkByType(LinkType link) {
		for (int i = 0; i < listArtistLink.size(); i++) {
			ArtistLink artistLink = listArtistLink.get(i);
			if(artistLink.getLinkType() == link) {
				return artistLink.getUrl();
			}
		}
		return null;
	}
	
	/*public String getValidImgUrl() {
		if (getLowResImgUrl() != null) {
			return getLowResImgUrl();
			
		} else if (getHighResImgUrl() != null) {
			return getHighResImgUrl();
			
		} else if (getLowResImgUrl() != null) {
			return getLowResImgUrl();
		}
		return null;
	}*/

	@Override
	public String getLowResImgUrl() {
		if (getImageUrl() != null) {
			return getImageUrl();
			
		} else if (imageAttribution == null) {
			return null;
			
		} else if (imageAttribution.getLowResPath() == null) {
			return DEFAULT_LOW_RES_PATH + getImageName();
			
		} else {
			return imageAttribution.getLowResPath() + getImageName();
		}
	}
	
	@Override
	public String getMobiResImgUrl() {
		if (getImageUrl() != null) {
			return getImageUrl();
			
		} else if (imageAttribution == null) {
			return null;
			
		} else if (imageAttribution.getMobiResPath() == null) {
			return DEFAULT_MOBI_RES_PATH + getImageName();
			
		} else {
			return imageAttribution.getMobiResPath() + getImageName();
		}
	}
	
	@Override
	public String getHighResImgUrl() {
		if (getImageUrl() != null) {
			return getImageUrl();
			
		} else if (imageAttribution == null) {
			return null;
			
		} else if (imageAttribution.getHighResPath() == null) {
			return DEFAULT_HIGH_RES_PATH + getImageName();
			
		} else {
			return imageAttribution.getHighResPath() + getImageName();
		}
	}

	@Override
	public String getKey(ImgResolution imgResolution) {
		return getClass().getName() + "_" + id + "_" + imgResolution;
	}
}
