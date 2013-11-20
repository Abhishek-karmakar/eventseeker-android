package com.wcities.eventseeker.core;

import java.io.Serializable;

import com.wcities.eventseeker.cache.BitmapCacheable;
import com.wcities.eventseeker.util.ConversionUtil;

public class Video implements BitmapCacheable, Serializable {
	
	private String videoUrl;
	
	public Video(String videoUrl) {
		this.videoUrl = videoUrl;
	}

	public String getVideoUrl() {
		return videoUrl;
	}
	
	private String getImgUrl() {
		return ConversionUtil.getYoutubeScreenShot(videoUrl, "hqdefault");
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
