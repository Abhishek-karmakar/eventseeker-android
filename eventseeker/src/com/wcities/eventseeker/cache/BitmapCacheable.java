package com.wcities.eventseeker.cache;

public interface BitmapCacheable {
	
	public enum ImgResolution {
		LOW,
		MOBILE,
		HIGH,
		DEFAULT
	}

	/**
	 * @return key generated as: classname_id_imgResolution
	 */
	public String getKey(ImgResolution imgResolution);
	
	public String getMobiResImgUrl();
	public String getLowResImgUrl();
	public String getHighResImgUrl();
}
