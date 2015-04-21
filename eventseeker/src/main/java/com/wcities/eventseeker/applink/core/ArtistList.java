package com.wcities.eventseeker.applink.core;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.wcities.eventseeker.core.Artist;

public class ArtistList {

	private static final String TAG = ArtistList.class.getName();
	private static final int DEFAULT_ARTIST_LIMIT = 10;
	
	private List<Artist> artistList;

	private int currentArtistPos = -1;
	private int artistsAlreadyRequested;
	private int totalNoOfArtists;
	private int artistsLimit = DEFAULT_ARTIST_LIMIT;

	//private boolean isMoreDataAvailable = true;
	
	public ArtistList() {
		artistList = new ArrayList<Artist>();
	}
	
	public int getArtistsLimit() {
		return artistsLimit;
	}

	public void setArtistsLimit(int artistsLimit) {
		this.artistsLimit = artistsLimit;
	}
	
	public int getCurrentArtistPosition() {
		return currentArtistPos;
	}

	public int getArtistsAlreadyRequested() {
		return artistsAlreadyRequested;
	}

	public int getTotalNoOfArtists() {
		return totalNoOfArtists;
	}

	public void setTotalNoOfArtists(int totalNoOfArtists) {
		this.totalNoOfArtists = totalNoOfArtists;
	}

	/*public boolean isMoreDataAvailable() {
		return isMoreDataAvailable;
	}

	public void setMoreDataAvailable(boolean isMoreDataAvailable) {
		this.isMoreDataAvailable = isMoreDataAvailable;
	}*/
	
	public Artist get(int position) {
		return artistList.get(position);
	}
	
	public Artist getCurrentArtist() {
		if (currentArtistPos >= 0) {
			return artistList.get(currentArtistPos);
			
		} else {
			return null;
		}
	}
	
	public int size() {
		return artistList.size();
	}
	
	public void resetArtistList() {
		if (artistList != null) {
			artistList.clear();
		}
		currentArtistPos = -1;
		artistsAlreadyRequested = 0;
		totalNoOfArtists = 0;
		artistsLimit = DEFAULT_ARTIST_LIMIT;

		//isMoreDataAvailable = true;
	}
	
	public boolean hasNextArtist() {
		Log.d(TAG, "hasNextArtists");
		if (currentArtistPos + 1 < artistList.size()) {
			return true;
			
		} /*else if (isMoreDataAvailable) {
			if (currentArtistPos + 1 < artistList.size()) {
				return true;
				
			} else {
				return false;
			}
			
		}*/ else {
			return false;
		}
	}
	
	public boolean hasPreviousArtist() {
		if (currentArtistPos - 1 > -1) {
			return true;
			
		} else {
			return false;
		}
	}

	public boolean moveToNextArtist() {
		Log.d(TAG, "moveToNextArtist");
		if(hasNextArtist()) {
			currentArtistPos++;
			Log.d(TAG, "moveToNextArtist true");
			return true;
		}
		Log.d(TAG, "moveToNextArtist false");
		return false;
	}
	
	public boolean moveToPreviousArtist() {
		if(hasPreviousArtist()) {
			currentArtistPos--;
			return true;
		}
		return false;		
	}
	
	public void addAll(List<Artist> artists) {
		if (artists != null && !artists.isEmpty()) {
			artistList.addAll(artists);
			artistsAlreadyRequested += artists.size();
			
			Log.d(TAG, "ARTIST LIMIT IN LIST: " + artistsLimit);
			if (artists.size() < artistsLimit) {
				//isMoreDataAvailable = false;
			}
			
		} else {
			//isMoreDataAvailable = false;
		}
	}
	
	public boolean isEmpty() {
		return artistList.isEmpty();
	}
	
}
