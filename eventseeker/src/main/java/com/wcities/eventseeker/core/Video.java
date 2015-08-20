package com.wcities.eventseeker.core;

import com.wcities.eventseeker.cache.BitmapCacheable;
import com.wcities.eventseeker.util.ConversionUtil;

import java.io.Serializable;

public class Video implements BitmapCacheable, Serializable {
	
	public static final String YOUTUBE_VIDEO_SIZE_HQDEFAULT = "hqdefault";
	
	private String videoUrl;
	
	public Video(String videoUrl) {
		this.videoUrl = videoUrl;
	}

	public String getVideoUrl() {
		return videoUrl;
	}
	
	private String getImgUrl() {
		return ConversionUtil.getYoutubeScreenShot(videoUrl, YOUTUBE_VIDEO_SIZE_HQDEFAULT);
	}

	@Override
	public String getKey(ImgResolution imgResolution) {
		return getClass().getName() + "_" + getImgUrl();
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
