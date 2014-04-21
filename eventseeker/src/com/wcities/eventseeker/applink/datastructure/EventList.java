package com.wcities.eventseeker.applink.datastructure;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.wcities.eventseeker.core.Event;

public class EventList {

	private static final String TAG = EventList.class.getName();
	private static final int DEFAULT_EVT_LIMIT = 10;

	private List<Event> eventList;
	private LoadEventsListener loadEventsListener;
	private Object requestCode;

	private int currentEvtPos = -1;
	private int eventsAlreadyRequested;
	private int totalNoOfEvents;
	private int eventsLimit = DEFAULT_EVT_LIMIT;

	private boolean isMoreDataAvailable = true;

	public interface LoadEventsListener {
		public void loadEvents();
	}
	
	public EventList() {
		eventList = new ArrayList<Event>();
	}
	
	public LoadEventsListener getLoadEventsListener() {
		return loadEventsListener;
	}

	public void setLoadEventsListener(LoadEventsListener loadEventsListener) {
		this.loadEventsListener = loadEventsListener;
	}

	public void removeLoadEventsListener() {
		this.loadEventsListener = null;
	}
	
	public int getEventsLimit() {
		return eventsLimit;
	}

	public void setEventsLimit(int eventsLimit) {
		this.eventsLimit = eventsLimit;
	}
	
	public Object getRequestCode() {
		return requestCode;
	}

	public void setRequestCode(Object requestCode) {
		this.requestCode = requestCode;
	}

	/*public List<Event> getEventList() {
		return eventList;
	}

	public void setEventList(List<Event> eventsList) {
		this.eventList = eventsList;
	}*/
	
	public int getCurrentEventPosition() {
		return currentEvtPos;
	}

	public void setCurrentEvtPos(int currentEvtPos) {
		this.currentEvtPos = currentEvtPos;
	}

	public int getEventsAlreadyRequested() {
		return eventsAlreadyRequested;
	}

	public int getTotalNoOfEvents() {
		return totalNoOfEvents;
	}

	public void setTotalNoOfEvents(int totalNoOfEvents) {
		this.totalNoOfEvents = totalNoOfEvents;
	}

	public boolean isMoreDataAvailable() {
		return isMoreDataAvailable;
	}

	public void setMoreDataAvailable(boolean isMoreDataAvailable) {
		this.isMoreDataAvailable = isMoreDataAvailable;
	}
	
	public Event get(int position) {
		return eventList.get(position);
	}
	
	public Event getCurrentEvent() {
		if (currentEvtPos >= 0) {
			return eventList.get(currentEvtPos);
			
		} else {
			return null;
		}
	}
	
	public void getSize() {
		this.eventList.size();
	}
	
	public void resetEventList() {
		if (eventList != null) {
			eventList.clear();
		}
		currentEvtPos = -1;
		eventsAlreadyRequested = 0;
		totalNoOfEvents = 0;
		eventsLimit = DEFAULT_EVT_LIMIT;

		isMoreDataAvailable = true;
		
		loadEventsListener = null;
		requestCode = null;
	}
	
	public boolean hasNextEvents() {
		if (currentEvtPos + 1 < eventList.size()) {
			return true;
			
		} else if (isMoreDataAvailable && loadEventsListener != null) {
			loadEventsListener.loadEvents();
			if (currentEvtPos + 1 < eventList.size()) {
				return true;
				
			} else {
				return false;
			}
			
		} else {
			return false;
		}
	}
	
	public boolean hasPreviousEvents() {
		if (currentEvtPos - 1 > -1) {
			return true;
			
		} else {
			return false;
		}
	}

	public boolean moveToNextEvent() {
		if(hasNextEvents()) {
			currentEvtPos++;
			return true;
		}
		return false;
	}
	
	public boolean moveToPreviousEvent() {
		if(hasPreviousEvents()) {
			currentEvtPos--;
			return true;
		}
		return false;		
	}
	
	public void addAll(List<Event> events) {
		if (events != null && !events.isEmpty()) {
			eventList.addAll(events);
			eventsAlreadyRequested += events.size();
			
			Log.d(TAG, "EVENT LIMIT IN EVENTLIST: " + eventsLimit);
			if (events.size() < eventsLimit) {
				isMoreDataAvailable = false;
			}
			
		} else {
			isMoreDataAvailable = false;
		}
	}
	
	public boolean isEmpty() {
		return eventList.isEmpty();
	}
	
}
