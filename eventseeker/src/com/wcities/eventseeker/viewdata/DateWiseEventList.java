package com.wcities.eventseeker.viewdata;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import android.os.AsyncTask;
import android.util.Log;

import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.util.ConversionUtil;

public class DateWiseEventList {

	private static final String TAG = DateWiseEventList.class.getName();
	
	private SortedMap<Date, List<EventListItem>> dateToEventListMap;
	private int totalSize, limitedCount;
	private Date currentDate;
	private Date dummyItemKey;
	
	public static enum LIST_ITEM_TYPE {
		PROGRESS, HEADER, CONTENT
	};
	
	public DateWiseEventList() {
		dateToEventListMap = new TreeMap<Date, List<EventListItem>>();
	}
	
	public void addDummyItem() {
		// insert 1 default value for representing 'loading progressbar'
		dummyItemKey = new Date(Long.MAX_VALUE);
		dateToEventListMap.put(dummyItemKey, null);
		totalSize++;
	}
	
	public void updateDummyItem() {
		if (dummyItemKey != null) {
			dateToEventListMap.remove(dummyItemKey);
			dummyItemKey = new Date(currentDate.getTime() + 1);
			dateToEventListMap.put(dummyItemKey, null);
		}
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
		Log.d(TAG, "removeProgressBarIndicator()");
		if (!loadEvents.isCancelled()) {
			Log.d(TAG, "remove");
			dateToEventListMap.remove(dummyItemKey);
			totalSize--;
			currentDate = new Date(Long.MAX_VALUE);
		}
	}
	
	private void updateCurrentDate(Date date) {
		currentDate = date;
	}
	
	public void addEventListItems(List<Event> events, AsyncTask loadEvents) {
		SortedMap<Date, List<EventListItem>> mapCopy = getDateToEventListMapCopy();
		int tmpSize = totalSize;
		
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
				updateCurrentDate(calendar.getTime());
				
				for (Iterator<com.wcities.eventseeker.core.Date> iterator2 = dates.iterator(); iterator2.hasNext();) {
					Date date = iterator2.next().getStartDate();
					calendar.setTime(date);
					calendar.set(Calendar.HOUR, 0);
					calendar.set(Calendar.MINUTE, 0);
					calendar.set(Calendar.SECOND, 0);
					calendar.set(Calendar.AM_PM, Calendar.AM);
					tmpSize = addMapping(calendar.getTime(), new EventListItem(event), mapCopy, tmpSize);
				}
			}
		}
		
		/**
		 * Update real copy of map only if this loadEvents task was (not passed at all) / (passed but not cancelled), because otherwise it's possible 
		 * that another newly started loadEvents task has already updated dateToEventListMap with right latest 
		 * values, but following code overwrites it.
		 */
		if (loadEvents == null || !loadEvents.isCancelled()) {
			dateToEventListMap = mapCopy;
			updateDummyItem();
			totalSize = tmpSize;
			
		} else {
			currentDate = new Date(Long.MAX_VALUE);
		}
	}
	
	public LIST_ITEM_TYPE getItemViewType(int position) {
		int index = -1;
		Set<Date> keys = dateToEventListMap.keySet();
		
		for (Iterator<Date> iterator = keys.iterator(); iterator.hasNext();) {

			index++;
			Date date = iterator.next();
			
			if (index == position) {
				LIST_ITEM_TYPE listItemType = (position == limitedCount - 1) ? LIST_ITEM_TYPE.PROGRESS : LIST_ITEM_TYPE.HEADER;
				return listItemType;
			}
			
			List<EventListItem> eventListItems = dateToEventListMap.get(date);
			if (position > index + eventListItems.size()) {
				index += eventListItems.size();
				continue;
				
			} else {
				return LIST_ITEM_TYPE.CONTENT;
			}
		}
		return null;
    }
	
	private int getTotalSize() {
		return totalSize;
	}
	
	public int getCount() {
		//Log.d(TAG, "getCount(), doneForLoading size = " + doneLoadingForDates.size());
		int count = 0;
		Set<Date> dates = dateToEventListMap.keySet();
		for (Date date : dates) {
			if ((currentDate != null && !date.after(currentDate)) || date.equals(dummyItemKey)) {
				count++;
				
				//Log.d(TAG, "date = " + date + ", dateToEventListMap.get(date) = " + dateToEventListMap.get(date));

				if (dateToEventListMap.get(date) != null) {
					count += dateToEventListMap.get(date).size();
				}
			}
		}
		//Log.d(TAG, "count = " + count + ", size = " + totalSize);
		limitedCount = count;
		return count;
	}
	
	public EventListItem getItem(int position) {
		int index = -1;
		Set<Date> keys = dateToEventListMap.keySet();
		
		for (Iterator<Date> iterator = keys.iterator(); iterator.hasNext();) {
			index++;
			Date date = iterator.next();
			
			if (index == position) {
				// null eventListItem represents progressbar
				EventListItem eventListItem = (position == limitedCount - 1) ? null : new EventListItem(ConversionUtil.getDay(date));
				return eventListItem;
			}
			
			List<EventListItem> eventListItems = dateToEventListMap.get(date);
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
		
		dummyItemKey = new Date(Long.MAX_VALUE);
		dateToEventListMap.put(dummyItemKey, null);
		totalSize = 1;
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
