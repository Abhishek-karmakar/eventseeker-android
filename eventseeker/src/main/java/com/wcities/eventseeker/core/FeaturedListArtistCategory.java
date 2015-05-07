package com.wcities.eventseeker.core;

import com.wcities.eventseeker.cache.BitmapCacheable;

/**
 * NOTE: It is extending 'PopularArtistCategory' just to be of same type, 
 * but it isn't extending any property from its SuperType.
 */
public class FeaturedListArtistCategory extends PopularArtistCategory implements BitmapCacheable {
	
	private int id;
	private String name;
	private String image;
	
	public FeaturedListArtistCategory(int id, String name) {
		this.id = id;
		this.name = name;
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

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	@Override
	public String getLowResImgUrl() {
		return getImage();
	}
	
	@Override
	public String getMobiResImgUrl() {
		return getImage();
	}
	
	@Override
	public String getHighResImgUrl() {
		return getImage();
	}

	@Override
	public String getKey(ImgResolution imgResolution) {
		return getClass().getName() + "_" + id + "_" + imgResolution;
	}	
}
