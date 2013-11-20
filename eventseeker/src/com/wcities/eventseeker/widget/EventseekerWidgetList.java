package com.wcities.eventseeker.widget;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.wcities.eventseeker.core.Event;

public class EventseekerWidgetList {

	private static final String TAG = EventseekerWidgetList.class.getName();
	
	private static EventseekerWidgetList eventseekerWidgetList;
	private List<EventseekerWidget> widgetList;	
	private List<Event> events;
	
	private EventseekerWidgetList() {
		widgetList = new ArrayList<EventseekerWidget>();		
		events = new ArrayList<Event>();
	}
	
	public static EventseekerWidgetList getInstance() {
		if (eventseekerWidgetList == null) {
			synchronized (EventseekerWidgetList.class) {
				if (eventseekerWidgetList == null) {
					eventseekerWidgetList = new EventseekerWidgetList();
				}
			}
		}
		return eventseekerWidgetList;
	}

	public List<EventseekerWidget> getWidgetList() {
		return widgetList;
	}
	
	public void addWidget(EventseekerWidget widget) {
		widgetList.add(widget);
	}
	
	public boolean contains(int widgetId) {
		for (Iterator<EventseekerWidget> iterator = widgetList.iterator(); iterator.hasNext();) {
			EventseekerWidget eventseekerWidget = iterator.next();
			if (eventseekerWidget.getWidgetId() == widgetId) {
				return true;
			}
		}
		return false;
	}

	public List<Event> getEvents() {
		return events;
	}

	public void setEvents(List<Event> events) {
		this.events = events;
	}
	
	public EventseekerWidget getWidget(int widgetId) {
		EventseekerWidget widget = null;
		for (Iterator<EventseekerWidget> iterator = widgetList.iterator(); iterator.hasNext();) {
			widget = iterator.next();
			if (widget.getWidgetId() == widgetId) {
				break;
				
			} else {
				widget = null;
			}			
		}
		return widget;
	}
	
	public Event getNext(Event currentEvent) {
		int i = 0;
		for (; i < events.size(); i++) {
			Event event = events.get(i);
			if (event.getId() == currentEvent.getId()) {
				break;
			}
		}
		
		//Log.d(TAG, "getNext(), i = " + i);
		if (++i < events.size()) {
			return events.get(i);
			
		} else {
			return null;
		}
	}
	
	public Event getPrevious(Event currentEvent) {
		int i = events.size() - 1;
		for (; i >= 0; i--) {
			Event event = events.get(i);
			if (event.getId() == currentEvent.getId()) {
				break;
			}
		}
		
		//Log.d(TAG, "getPrevious(), i = " + i);
		if (--i >= 0) {
			return events.get(i);
			
		} else {
			return null;
		}
	}
	
	public void removeWidget(int widgetId){
		for (int i = 0; i < widgetList.size(); i++) {
			EventseekerWidget widget = widgetList.get(i);
			if (widgetId == widget.getWidgetId()) {
				widgetList.remove(i);
				break;
			}
		}
	}
}
