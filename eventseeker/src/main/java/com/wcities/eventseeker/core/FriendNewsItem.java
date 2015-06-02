package com.wcities.eventseeker.core;

import com.wcities.eventseeker.cache.BitmapCacheable;
import com.wcities.eventseeker.core.Event.Attending;

public class FriendNewsItem implements BitmapCacheable {

	private Friend friend;
	private long trackId;
	private String trackName;
	private Attending attending;
	private ImageAttribution imageAttribution;
	private String imgName;
	private String fbPostId;
	private Attending userAttending, newUserAttending;
	private String bookingUrl;
	private Schedule schedule;

	public Friend getFriend() {
		return friend;
	}

	public void setFriend(Friend friend) {
		this.friend = friend;
	}

	public long getTrackId() {
		return trackId;
	}

	public void setTrackId(long trackId) {
		this.trackId = trackId;
	}

	public String getTrackName() {
		return trackName;
	}

	public void setTrackName(String trackName) {
		this.trackName = trackName;
	}

	public Attending getAttending() {
		return attending;
	}

	public void setAttending(Attending attending) {
		this.attending = attending;
	}

	public Schedule getSchedule() {
		return schedule;
	}

	public void setSchedule(Schedule schedule) {
		this.schedule = schedule;
	}

	public ImageAttribution getImageAttribution() {
		return imageAttribution;
	}

	public void setImageAttribution(ImageAttribution imageAttribution) {
		this.imageAttribution = imageAttribution;
	}

	public String getImgName() {
		return imgName;
	}

	public void setImgName(String imgName) {
		this.imgName = imgName;
	}

	public String getFbPostId() {
		return fbPostId;
	}

	public void setFbPostId(String fbPostId) {
		this.fbPostId = fbPostId;
	}
	
	public Attending getUserAttending() {
		return userAttending;
	}

	public void setUserAttending(Attending userAttending) {
		this.userAttending = userAttending;
	}

	public Attending getNewUserAttending() {
		return newUserAttending;
	}

	public void setNewUserAttending(Attending newUserAttending) {
		this.newUserAttending = newUserAttending;
	}
	
	public void updateUserAttendingToNewUserAttending() {
		if (newUserAttending != null) {
			userAttending = newUserAttending;
			newUserAttending = null;
		}
	}

	public String getBookingUrl() {
		return bookingUrl;
	}

	public void setBookingUrl(String bookingUrl) {
		this.bookingUrl = bookingUrl;
	}

	public Event toEvent() {
		Event event = new Event(trackId, trackName);
		
		event.setImgName(imgName);		
		event.setImageAttribution(imageAttribution);
		
		/*Schedule schedule = new Schedule();
		schedule.setVenue(venue);
		//schedule.addDate(startTime);
		schedule.setDates(this.schedule.getDates());*/
		event.setSchedule(this.schedule);

		event.setAttending(userAttending);
		
		return event;
	}
	
	public boolean doesValidImgUrlExist() {
		if (getLowResImgUrl() != null || getMobiResImgUrl() != null || getHighResImgUrl() != null) {
			return true;
			
		} else {
			return false;
		}
	}

	@Override
	public String getKey(ImgResolution imgResolution) {
		return getClass().getName() + "_" + friend.getId() + "_" + trackId;
	}
	
	@Override
	public String getMobiResImgUrl() {
		if (getImageAttribution() != null) {
			return getImageAttribution().getMobiResPath() + getImgName();
		}
		return null;
	}
	
	@Override
	public String getLowResImgUrl() {
		if (getImageAttribution() != null) {
			return getImageAttribution().getLowResPath() + getImgName();
		}
		return null;
	}
	
	@Override
	public String getHighResImgUrl() {
		if (getImageAttribution() != null) {
			return getImageAttribution().getHighResPath() + getImgName();
		}
		return null;
	}
}
