package com.wcities.eventseeker.widget;

import java.io.Serializable;

import com.wcities.eventseeker.core.Event;

public class EventseekerWidget implements Serializable {

	public static final String WIDGET_UPDATE = "eventseeker.appwidget.action.WIDGET_UPDATE";
	//public static final String WIDGET_PREV_EVENT = "eventseeker.appwidget.action.WIDGET_PREV_EVENT";
	public static final String WIDGET_NEXT_EVENT = "eventseeker.appwidget.action.WIDGET_NEXT_EVENT";
	
	public static enum UpdateType {
		REFRESH_WIDGET,
		REFRESH_IMAGE;
	}

	private int widgetId;
	private Event currentEvent;
	
	public EventseekerWidget(int widgetId) {
		this.widgetId = widgetId;
	}

	public int getWidgetId() {
		return widgetId;
	}
	
	public Event getCurrentEvent() {
		return currentEvent;
	}

	public void setCurrentEvent(Event currentEvent) {
		this.currentEvent = currentEvent;
	}
}
