package com.wcities.eventseeker.interfaces;

import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.viewdata.SharedElement;

import java.util.List;

public interface EventListener {
	public void onEventSelected(Event event);
	public void onEventSelected(Event event, List<SharedElement> sharedElements);
}
