package com.wcities.eventseeker.core;

import java.io.Serializable;

import com.wcities.eventseeker.cache.BitmapCacheable;
import com.wcities.eventseeker.core.Event.Attending;

public class Friend implements Serializable, BitmapCacheable {

	private String id;
	private String name;
	private String imgUrl;
	private Attending attending;
	
	public Friend() {
		attending = Attending.NOT_GOING;
	}

	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getImgUrl() {
		return imgUrl;
	}
	
	public void setImgUrl(String imgUrl) {
		this.imgUrl = imgUrl;
	}

	public Attending getAttending() {
		return attending;
	}

	public void setAttending(Attending attending) {
		this.attending = attending;
	}

	@Override
	public String getKey(ImgResolution imgResolution) {
		return getClass().getName() + "_" + id + "_" + imgResolution;
	}

	@Override
	public String getMobiResImgUrl() {
		return getImgUrl();
	}

	@Override
	public String getLowResImgUrl() {
		return getImgUrl();
	}

	@Override
	public String getHighResImgUrl() {
		return getImgUrl();
	}
}
