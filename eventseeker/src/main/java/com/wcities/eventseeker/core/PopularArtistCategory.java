package com.wcities.eventseeker.core;

import com.wcities.eventseeker.R;

import java.util.ArrayList;
import java.util.List;

public class PopularArtistCategory {

	protected PopularArtistsType popularArtistsType;
	private int strResIdCategory;
	private int drwResIdCategory;

	/**
	 * 24-04-2015:
	 * This is added due to the introduction of the 'FeaturedListArtists' before the popular Artists section
	 * refer mail : Artist Featured List API for Api
	 * @author win2
	 *
	 */
	public static enum PopularArtistsType {
		FeaturedListArtists,
		//FeaturedArtists,
		MusicArtists,
		ComedyArtists,
		TheaterArtists,
		SportsArtists
	}
	
	public PopularArtistCategory() {}
	
	public PopularArtistCategory(PopularArtistsType popularArtistType, int strResIdCategory, int drwResIdCategory) {
		this.popularArtistsType = popularArtistType;
		this.strResIdCategory = strResIdCategory;
		this.drwResIdCategory = drwResIdCategory;
	}

	public final PopularArtistsType getPopularArtistsType() {
		return popularArtistsType;
	}
	
	public final void setPopularArtistsType(PopularArtistsType popularArtistsType) {
		this.popularArtistsType = popularArtistsType;
	}
	
	public final int getStrResIdCategory() {
		return strResIdCategory;
	}

	public final int getDrwResIdCategory() {
		return drwResIdCategory;
	}
	
	public static final List<PopularArtistCategory> getPopularArtistCategories() {
		List<PopularArtistCategory> popularArtistCategories = new ArrayList<PopularArtistCategory>();
		for (PopularArtistsType popularArtistType : PopularArtistsType.values()) {
			
			PopularArtistCategory popularArtistCategory = null; 
			switch (popularArtistType) {
				case FeaturedListArtists:
					continue;
					
				/*case FeaturedArtists:
					popularArtistCategory = new PopularArtistCategory(popularArtistType, R.string.btn_featured, 
							R.drawable.ic_featured); 
					break;*/

				case MusicArtists:
					popularArtistCategory = new PopularArtistCategory(popularArtistType, R.string.btn_music, 
							R.drawable.ic_music); 
					break;
					
				case ComedyArtists:
					popularArtistCategory = new PopularArtistCategory(popularArtistType, R.string.btn_comedy, 
							R.drawable.ic_comedy); 
					break;
					
				case TheaterArtists:
					popularArtistCategory = new PopularArtistCategory(popularArtistType, R.string.btn_theater, 
							R.drawable.ic_theater); 
					break;
					
				case SportsArtists:
					popularArtistCategory = new PopularArtistCategory(popularArtistType, R.string.btn_sports, 
							R.drawable.ic_sports); 
					break;
			}
			popularArtistCategories.add(popularArtistCategory);
		}
		return popularArtistCategories;
	}
}
