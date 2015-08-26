package com.wcities.eventseeker.viewdata;

import android.os.AsyncTask;

import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.util.ConversionUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class DateWiseEventList {

	private static final String TAG = DateWiseEventList.class.getName();
	private static final Date DUMMY_KEY = new Date(Long.MAX_VALUE);
	
	private SortedMap<Date, List<EventListItem>> dateToEventListMap;
	private int size;
	
	public static enum LIST_ITEM_TYPE {
		PROGRESS, HEADER, CONTENT, NO_EVENTS
	};
	
	public DateWiseEventList() {
		dateToEventListMap = new TreeMap<Date, List<EventListItem>>();
	}
	
	public void addDummyItem() {
		// insert 1 default value for representing 'loading progressbar'
		dateToEventListMap.put(DUMMY_KEY, null);
		size++;
	}
	
	public void addNoEventsMsg() {
		Event event = new Event(AppConstants.INVALID_ID, null);
		ArrayList<EventListItem> list = new ArrayList<DateWiseEventList.EventListItem>();
		list.add(new EventListItem(event));
		
		dateToEventListMap.put(DUMMY_KEY, list);
		size++;
	}
	
	private SortedMap<Date, List<EventListItem>> getDateToEventListMapCopy() {
		return new TreeMap<Date, List<EventListItem>>(dateToEventListMap);
	}
	
	private int addMapping(Date date, EventListItem eventListItem, SortedMap<Date, List<EventListItem>> mapCopy, int tmpSize) {
		List<EventListItem> eventListItems = mapCopy.get(date);
		if (eventListItems == null) {
			eventListItems = new ArrayList<DateWiseEventList.EventListItem>();
			mapCopy.put(date, eventListItems);
			tmpSize++;
		}
		eventListItems.add(eventListItem);
		tmpSize++;
		return tmpSize;
	}
	
	public void removeProgressBarIndicator(AsyncTask loadEvents) {
		//Log.d(TAG, "removeProgressBarIndicator()");
		if (!loadEvents.isCancelled()) {
			//Log.d(TAG, "remove");
			dateToEventListMap.remove(DUMMY_KEY);
			size--;
			
			if (size == 0) {
				addNoEventsMsg();
			}
		}
	}
	
	public void addEventListItems(List<Event> events, AsyncTask loadEvents) {
		SortedMap<Date, List<EventListItem>> mapCopy = getDateToEventListMapCopy();
		int tmpSize = size;
		
		Calendar calendar = Calendar.getInstance();

		for (Iterator<Event> iterator = events.iterator(); iterator.hasNext();) {
			Event event = iterator.next();
			
			if (event.getSchedule() != null) {
				List<com.wcities.eventseeker.core.Date> dates = event.getSchedule().getDates();
				calendar.setTime(dates.get(0).getStartDate());
				calendar.set(Calendar.HOUR, 0);
				calendar.set(Calendar.MINUTE, 0);
				calendar.set(Calendar.SECOND, 0);
				calendar.set(Calendar.AM_PM, Calendar.AM);
				tmpSize = addMapping(calendar.getTime(), new EventListItem(event), mapCopy, tmpSize);
			}
		}
		
		/**
		 * Update real copy of map only if this loadEvents task was (not passed at all) / (passed but not cancelled), because otherwise it's possible 
		 * that another newly started loadEvents task has already updated dateToEventListMap with right latest 
		 * values, but following code overwrites it.
		 */
		if (loadEvents == null || !loadEvents.isCancelled()) {
			dateToEventListMap = mapCopy;
			size = tmpSize;
		}
	}
	
	public LIST_ITEM_TYPE getItemViewType(int position) {
		int index = -1;
		Set<Date> keys = dateToEventListMap.keySet();
		
		for (Iterator<Date> iterator = keys.iterator(); iterator.hasNext();) {

			index++;
			Date date = iterator.next();
			
			List<EventListItem> eventListItems = dateToEventListMap.get(date);
			if (index == position) {
				LIST_ITEM_TYPE listItemType;
				if (position == size - 1) {
					listItemType = (eventListItems == null) ? LIST_ITEM_TYPE.PROGRESS : LIST_ITEM_TYPE.NO_EVENTS;
					
				} else {
					listItemType = LIST_ITEM_TYPE.HEADER;					
				}
				
				return listItemType;
			}
			
			if (position > index + eventListItems.size()) {
				index += eventListItems.size();
				continue;
				
			} else {
				return LIST_ITEM_TYPE.CONTENT;
			}
		}
		return null;
    }
	
	public int getCount() {
		return size;
	}
	
	public EventListItem getItem(int position, String dateFormat) {
		return getItemWithDateFormat(position, dateFormat);
	}
	
	public EventListItem getItem(int position) {
		return getItemWithDateFormat(position, null);
	}
	
	private EventListItem getItemWithDateFormat(int position, String dateFormat) {
		int index = -1;
		Set<Date> keys = dateToEventListMap.keySet();
		
		for (Iterator<Date> iterator = keys.iterator(); iterator.hasNext();) {
			index++;
			Date date = iterator.next();
			
			List<EventListItem> eventListItems = dateToEventListMap.get(date);
			if (index == position) {
				// null eventListItem represents progressbar
				EventListItem eventListItem;
				if (position == size - 1) {
					eventListItem = (eventListItems == null) ? null : eventListItems.get(0);	
					
				} else {
					if (dateFormat == null) {
						eventListItem = new EventListItem(ConversionUtil.getDay(date));
						
					} else {
						eventListItem = new EventListItem(new SimpleDateFormat(dateFormat).format(date));	
					}
				}
				return eventListItem;
			}
			
			if (position > index + eventListItems.size()) {
				index += eventListItems.size();
				continue;
				
			} else {
				for (Iterator<EventListItem> iterator2 = eventListItems.iterator(); iterator2.hasNext();) {
					EventListItem eventListItem = iterator2.next();
					index++;
					if (position == index) {
						return eventListItem;
					}
				}
			}
		}
		return null;
	}
	
	public void reset() {
		dateToEventListMap.clear();
		dateToEventListMap.put(DUMMY_KEY, null);
		size = 1;
	}

	/**
	 * Class representing item in an event list which can be either actual item or section title representing
	 * date of such events
	 */
	public class EventListItem {
		private Event event;
		private String date;
		private boolean isEvent;
		
		public EventListItem(Event event) {
			this.event = event;
			isEvent = true;
		}

		public EventListItem(String date) {
			this.date = date;
			isEvent = false;
		}

		public Event getEvent() {
			return event;
		}

		public String getDate() {
			return date;
		}

		public boolean isEvent() {
			return isEvent;
		}
	}
}
