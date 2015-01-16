package com.wcities.eventseeker.interfaces;

import java.util.List;

import android.view.View;

import com.wcities.eventseeker.core.Event;

public interface EventListener {
	public void onEventSelected(Event event);
	public void onEventSelected(Event event, List<View> sharedElements);
}
