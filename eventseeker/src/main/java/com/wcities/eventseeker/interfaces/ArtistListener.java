package com.wcities.eventseeker.interfaces;

import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.viewdata.SharedElement;

import java.util.List;

public interface ArtistListener {
	public void onArtistSelected(Artist artist);
	public void onArtistSelected(Artist artist, List<SharedElement> sharedElements);
}
