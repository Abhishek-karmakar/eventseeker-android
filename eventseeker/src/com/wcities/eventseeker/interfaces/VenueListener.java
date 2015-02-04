package com.wcities.eventseeker.interfaces;

import java.util.List;

import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.viewdata.SharedElement;

public interface VenueListener {
	public void onVenueSelected(Venue venue);
	public void onVenueSelected(Venue venue, List<SharedElement> sharedElements);
}
