package com.wcities.eventseeker.core;

import java.io.Serializable;

import com.wcities.eventseeker.cache.BitmapCacheable;

public class Venue implements Serializable, BitmapCacheable {
	
	private static final String DEFAULT_HIGH_RES_PATH = "http://c0056906.cdn2.cloudfiles.rackspacecloud.com/";
	private static final String DEFAULT_LOW_RES_PATH = "http://c0056904.cdn2.cloudfiles.rackspacecloud.com/";
	private static final String DEFAULT_MOBI_RES_PATH = "http://c416814.r14.cf2.rackcdn.com/";
	
	private int id;
	private String name;
	private String imagefile;
	private String imageUrl;
	private Address address;
	private ImageAttribution imageAttribution;
	private String longDesc;
	private String phone;
	private String url;
	
	public Venue(int id) {
		this.id = id;
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

	public String getImagefile() {
		return imagefile;
	}

	public void setImagefile(String imagefile) {
		this.imagefile = imagefile;
	}
	
	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public ImageAttribution getImageAttribution() {
		return imageAttribution;
	}

	public void setImageAttribution(ImageAttribution imageAttribution) {
		this.imageAttribution = imageAttribution;
	}

	public String getLongDesc() {
		return longDesc;
	}

	public void setLongDesc(String longDesc) {
		this.longDesc = longDesc;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
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
		if (imageUrl != null) {
			return imageUrl;
			
		} else if (imageAttribution == null || imageAttribution.getLowResPath() == null) {
			return DEFAULT_LOW_RES_PATH + getImagefile();
			
		} else {
			return imageAttribution.getLowResPath() + getImagefile();
		}
	}
	
	@Override
	public String getMobiResImgUrl() {
		if (imageUrl != null) {
			return imageUrl;
			
		} else if (imageAttribution == null || imageAttribution.getMobiResPath() == null) {
			return DEFAULT_MOBI_RES_PATH + getImagefile();
			
		} else {
			return imageAttribution.getMobiResPath() + getImagefile();
		}
	}
	
	@Override
	public String getHighResImgUrl() {
		if (imageUrl != null) {
			return imageUrl;
			
		} else if (imageAttribution == null || imageAttribution.getHighResPath() == null) {
			return DEFAULT_HIGH_RES_PATH + getImagefile();
			
		} else {
			return imageAttribution.getHighResPath() + getImagefile();
		}
	}
	
	@Override
	public String getKey(ImgResolution imgResolution) {
		return new StringBuilder(getClass().getName()).append("_").append(id).append("_").append(imgResolution).toString();
	}
}
