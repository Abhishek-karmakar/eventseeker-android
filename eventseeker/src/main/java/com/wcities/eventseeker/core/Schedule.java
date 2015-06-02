package com.wcities.eventseeker.core;

import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.util.ConversionUtil;

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

    public void setDates(List<Date> dates) {
        this.dates = dates;
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

	public String getDateRangeOrDateToDisplay(EventSeekr eventSeekr, boolean parseYear, boolean amPmCaps,
				  boolean spaceBeforeAmPm) {
		Date date1 = dates.get(0);
		String strDate;

		if (dates.size() == 1 && date1.getEndDate() != null) {
			// for festivals dates size is 1 & endDate can be non-null which is different than startDate.
			// display date range
			strDate = ConversionUtil.getDateTime(eventSeekr,
					date1.getStartDate(), false, parseYear, amPmCaps, spaceBeforeAmPm);
			strDate += " - " + ConversionUtil.getDateTime(eventSeekr,
					date1.getEndDate(), date1.isStartTimeAvailable(), parseYear, amPmCaps, spaceBeforeAmPm);

		} else if (dates.size() > 1) {
			Date dateN = dates.get(dates.size() - 1);
			//Log.d(TAG, "" + ((dateN.getStartDate().getTime() - date1.getStartDate().getTime()) / ConversionUtil.MILLI_SECONDS_PER_DAY) + ", " + dates.size());
			// Check if dates are all sequential, if yes then display date range
			if (((dateN.getStartDate().getTime() - date1.getStartDate().getTime()) / ConversionUtil.MILLI_SECONDS_PER_DAY) + 1 == dates.size()) {
				strDate = ConversionUtil.getDateTime(eventSeekr,
						date1.getStartDate(), false, parseYear, amPmCaps, spaceBeforeAmPm);
				strDate += " - " + ConversionUtil.getDateTime(eventSeekr,
						dateN.getStartDate(), dateN.isStartTimeAvailable(), parseYear, amPmCaps, spaceBeforeAmPm);

			} else {
				// display first date only
				strDate = ConversionUtil.getDateTime(eventSeekr,
						date1.getStartDate(), date1.isStartTimeAvailable(), parseYear, amPmCaps, spaceBeforeAmPm);
			}

		} else {
			// display single date
			strDate = ConversionUtil.getDateTime(eventSeekr,
					date1.getStartDate(), date1.isStartTimeAvailable(), parseYear, amPmCaps, spaceBeforeAmPm);
		}

		return strDate;
	}
}
