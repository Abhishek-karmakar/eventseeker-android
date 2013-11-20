package com.wcities.eventseeker.core;

import java.util.Date;

import com.wcities.eventseeker.cache.BitmapCacheable;

public class ArtistNewsItem implements BitmapCacheable {
	
	public static enum PostType {
		status,
		link,
		photo,
		video
	}

	private String artistName;
	private PostType postType;
	private String postTitle;
	private String postDesc;
	private String postUrl;
	private Date timestamp;
	/**
	 * this can be 
	 * 1) link image for postType=link or 
	 * 2) photo url for postType=photo or
	 * 3) first frame url for postType=video
	 */
	private String imgUrl;

	public String getArtistName() {
		return artistName;
	}

	public void setArtistName(String artistName) {
		this.artistName = artistName;
	}

	public PostType getPostType() {
		return postType;
	}

	public void setPostType(PostType postType) {
		this.postType = postType;
	}

	public String getPostTitle() {
		return postTitle;
	}

	public void setPostTitle(String postTitle) {
		this.postTitle = postTitle;
	}

	public String getPostDesc() {
		return postDesc;
	}

	public void setPostDesc(String postDesc) {
		this.postDesc = postDesc;
	}

	public String getPostUrl() {
		return postUrl;
	}

	public void setPostUrl(String postUrl) {
		this.postUrl = postUrl;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public String getImgUrl() {
		return imgUrl;
	}

	public void setImgUrl(String imgUrl) {
		this.imgUrl = imgUrl;
	}

	/**
	 * imgResolution is irrelevant here.
	 * @return key generated as: classname_imgUrl
	 */
	public String getKey(ImgResolution imgResolution) {
		return getClass().getName() + "_" + imgUrl;
	}

	@Override
	public String getMobiResImgUrl() {
		return imgUrl;
	}

	@Override
	public String getLowResImgUrl() {
		return imgUrl;
	}

	@Override
	public String getHighResImgUrl() {
		return imgUrl;
	}
}
