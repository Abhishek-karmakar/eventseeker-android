package com.wcities.eventseeker.interfaces;

import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.viewdata.SharedElement;

import java.util.List;

public interface VenueListener {
	public void onVenueSelected(Venue venue);
	public void onVenueSelected(Venue venue, List<SharedElement> sharedElements);
}
