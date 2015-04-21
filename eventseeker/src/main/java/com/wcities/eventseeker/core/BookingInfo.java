package com.wcities.eventseeker.core;

import java.io.Serializable;

public class BookingInfo implements Serializable {

	private String bookingUrl;
	private String provider;
	private float price;
	private String currency;
	
	public String getBookingUrl() {
		return bookingUrl;
	}
	
	public void setBookingUrl(String bookingUrl) {
		this.bookingUrl = bookingUrl;
	}
	
	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public float getPrice() {
		return price;
	}

	public void setPrice(float price) {
		this.price = price;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}
	
	public String getFullCurrencyString() {
		if (currency.equals("EUR")) {
			return "euro";
			
		} else if (currency.equals("USD")) {
			return "united states dollar";
			
		} else if (currency.equals("GBP")) {
			return "Pound sterling";
			
		} else if (currency.equals("CHF")) {
			return "Swiss franc";
			
		} else if (currency.equals("CAD")) {
			return "Canadian dollar";
			
		} else if (currency.equals("SEK")) {
			return "Swedish krona";
		}
		return currency;
	}
}
