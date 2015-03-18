package com.wcities.eventseeker.interfaces;

import android.widget.ImageView;

import com.wcities.eventseeker.core.Event;

public interface EventListenerTab {
	public void onEventSelected(Event event, ImageView imageView);
}
