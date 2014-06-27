package com.wcities.eventseeker.applink.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.widget.Toast;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.applink.service.AppLinkService;
import com.wcities.eventseeker.applink.util.ALUtil;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Event;

public class EventList {

	private static final String TAG = EventList.class.getName();
	private static final int DEFAULT_EVT_LIMIT = 10;
	private static final int MAX_EVENTS = 25;
	
	public static enum GetEventsFrom {
		EVENTS,
		FEATURED_EVENTS,
		SEARCH_EVENTS;
	}

	private List<Event> eventList;
	private LoadEventsListener loadEventsListener;
	private Object requestCode;

	private int currentEvtPos = -1;
	private int eventsAlreadyRequested;
	private int totalNoOfEvents;
	private int eventsLimit = DEFAULT_EVT_LIMIT;

	private boolean isMoreDataAvailable = true;
	
	public interface LoadEventsListener {
		public void loadEvents() throws IOException;
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
	
	public int updateAndGetEventsLimit() {
		if ((MAX_EVENTS - eventsAlreadyRequested) > DEFAULT_EVT_LIMIT) {
			eventsLimit = DEFAULT_EVT_LIMIT; 
			
		} else {
			eventsLimit = MAX_EVENTS - eventsAlreadyRequested;
		}
		return eventsLimit;
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

	public int getEventsAlreadyRequested() {
		return eventsAlreadyRequested;
	}

	public int getTotalNoOfEvents() {
		return totalNoOfEvents;
	}

	public void setTotalNoOfEvents(int totalNoOfEvents) {
		this.totalNoOfEvents = (totalNoOfEvents > MAX_EVENTS) ? MAX_EVENTS : totalNoOfEvents;
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
	
	public int size() {
		return eventList.size();
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
	
	public boolean hasNextEvent() throws IOException {
		//Log.d(TAG, "hasNextEvents");
		if (currentEvtPos + 1 < eventList.size()) {
			//Log.d(TAG, "hasNextEvents true");
			return true;
			
		} else if (isMoreDataAvailable && loadEventsListener != null && size() < MAX_EVENTS) {
			// 'size() < MAX_EVENTS' check is added as there should be at the most 25 result.
			// First show the loading message
			ALUtil.displayMessage(R.string.loading, AppConstants.INVALID_RES_ID);
			loadEventsListener.loadEvents();
			if (currentEvtPos + 1 < eventList.size()) {
				//Log.d(TAG, "hasNextEvents true");
				return true;
				
			} else {
				//Log.d(TAG, "hasNextEvents false");
				return false;
			}
			
		} else {
			//Log.d(TAG, "hasNextEvents false");
			return false;
		}
	}
	
	public boolean hasPreviousEvent() {
		if (currentEvtPos - 1 > -1) {
			return true;
			
		} else {
			return false;
		}
	}

	public boolean moveToNextEvent() throws IOException {
		//Log.d(TAG, "moveToNextEvent");
		if (hasNextEvent()) {
			currentEvtPos++;
			//Log.d(TAG, "moveToNextEvent true");
			return true;
		}
		//Log.d(TAG, "moveToNextEvent false");
		return false;
	}
	
	public boolean moveToPreviousEvent() {
		if (hasPreviousEvent()) {
			currentEvtPos--;
			return true;
		}
		return false;		
	}
	
	public void addAll(List<Event> events) {
		if (events != null && !events.isEmpty()) {
			eventList.addAll(events);
			eventsAlreadyRequested += events.size();
			
			//Log.d(TAG, "EVENT LIMIT IN EVENTLIST: " + eventsLimit);
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
