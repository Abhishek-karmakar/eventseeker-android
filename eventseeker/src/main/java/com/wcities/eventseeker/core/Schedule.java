package com.wcities.eventseeker.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Schedule implements Serializable {
	
	private Venue venue;
	private List<Date> dates;
	private List<BookingInfo> bookingInfos;
	
	public Schedule() {
		dates = new ArrayList<Date>();
		bookingInfos = new ArrayList<BookingInfo>();
	}

	public Venue getVenue() {
		return venue;
	}

	public void setVenue(Venue venue) {
		this.venue = venue;
	}

	public List<Date> getDates() {
		return dates;
	}
	
	public void addDate(Date date) {
		dates.add(date);
	}

	public List<BookingInfo> getBookingInfos() {
		return bookingInfos;
	}
	
	public void addBookingInfo(BookingInfo bookingInfo) {
		bookingInfos.add(bookingInfo);
	}
	
	/**
	 * @return list containing 2 or less than 2 price values in ascending order - 1st minimum & 2nd maximum
	 */
	public List<Float> getPriceRange() {
		List<Float> priceRange = new ArrayList<Float>();
		for (Iterator<BookingInfo> iterator = bookingInfos.iterator(); iterator.hasNext();) {
			BookingInfo bookingInfo = iterator.next();
			if (bookingInfo.getPrice() != 0) {
				priceRange.add(bookingInfo.getPrice());
			}
		}
		Collections.sort(priceRange);
		
		if (priceRange.size() > 2) {
			// remove extra values in between first & last values
			List<Float> tmpPriceRange = new ArrayList<Float>();
			tmpPriceRange.add(priceRange.get(0));
			tmpPriceRange.add(priceRange.get(priceRange.size() - 1));
			priceRange = tmpPriceRange;
		}
		return priceRange;
	}
}
