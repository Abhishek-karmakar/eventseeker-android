package com.wcities.eventseeker.interfaces;

import android.widget.ImageView;
import android.widget.TextView;

import com.wcities.eventseeker.core.Venue;

public interface VenueListenerTab {
	public void onVenueSelected(Venue venue, ImageView imageView, TextView textView);
}
