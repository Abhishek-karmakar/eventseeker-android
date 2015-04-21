package com.wcities.eventseeker.core;

import java.io.Serializable;

public class ImageAttribution implements Serializable {
	
	private String highResPath;
	private String lowResPath;
	private String mobiResPath;
	
	public String getHighResPath() {
		return highResPath;
	}
	
	public void setHighResPath(String highResPath) {
		this.highResPath = highResPath;
	}
	
	public String getLowResPath() {
		return lowResPath;
	}
	
	public void setLowResPath(String lowResPath) {
		this.lowResPath = lowResPath;
	}
	
	public String getMobiResPath() {
		return mobiResPath;
	}
	
	public void setMobiResPath(String mobiResPath) {
		this.mobiResPath = mobiResPath;
	}
}
