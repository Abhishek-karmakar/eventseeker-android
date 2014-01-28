package com.wcities.eventseeker.core;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import android.util.Log;

public class FollowingList {

	private static final String TAG = FollowingList.class.getSimpleName();
	
	private TreeMap<String, Artist> followedArtists;

	public FollowingList() {
		followedArtists = new TreeMap<String, Artist>();
	}
	
	public void followArtist(Artist artist) {
		Set<String> artistNames = followedArtists.keySet();
		if (!artistNames.contains(artist.getName())) {
			followedArtists.put(artist.getName(), artist);
		}
	}
	
	public void unfollowArtist(Artist artist) {
		followedArtists.remove(artist.getName());
	}
	
	public Collection<Artist> addFollowedArtistsIfAny(Collection<Artist> artists, String fromInclusive, String toExclusive) {
		SortedMap<String, Artist> limitedArtistsMap;
		// initially fromInclusive would be null
		if (fromInclusive == null && toExclusive == null) {
			limitedArtistsMap = followedArtists;
			
		} else if (fromInclusive == null) {
			limitedArtistsMap = followedArtists.headMap(toExclusive);
			
		} else if (toExclusive == null) {
			// in the end toExclusive would be null
			limitedArtistsMap = followedArtists.tailMap(fromInclusive);

		} else {
			limitedArtistsMap = followedArtists.subMap(fromInclusive, toExclusive);
		}
		
		if (limitedArtistsMap.isEmpty()) {
			return artists;
		}
		
		SortedMap<String, Artist> mergedArtistMap = new TreeMap<String, Artist>(limitedArtistsMap);
		if (artists == null || artists.isEmpty()) {
			/**
			 * in the end after loading api calls are complete for all artists, if any pending cached 
			 * artists are found, we return those from here.
			 */
			return mergedArtistMap.values();
		}
		
		for (Iterator<Artist> iterator = artists.iterator(); iterator.hasNext();) {
			Artist artist = iterator.next();
			/**
			 * Remove this artist name from local followedArtists map, because now it's available in response
			 * & hence we need not track it here in local copy.
			 */
			followedArtists.remove(artist.getName());
			mergedArtistMap.put(artist.getName(), artist);
		}
		
		return mergedArtistMap.values();
	}
}
