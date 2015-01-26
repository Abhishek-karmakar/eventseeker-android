package com.wcities.eventseeker.interfaces;

import java.util.List;

import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.viewdata.SharedElement;

public interface ArtistListener {
	public void onArtistSelected(Artist artist);
	public void onArtistSelected(Artist artist, List<SharedElement> sharedElements);
}
