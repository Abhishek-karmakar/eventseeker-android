package com.wcities.eventseeker.core;

import java.io.Serializable;

public class ArtistLink implements Serializable{
	
	private LinkType linkType;
	private String url;
	
	public static enum LinkType {
		FACEBOOK,
		TWITTER,
		WEBSITE
	}
	
	public ArtistLink(LinkType linkType, String url) {
		this.linkType = linkType;
		this.url = url;
	}

	public LinkType getLinkType() {
		return linkType;
	}

	public void setLinkType(LinkType linkType) {
		this.linkType = linkType;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	
}
