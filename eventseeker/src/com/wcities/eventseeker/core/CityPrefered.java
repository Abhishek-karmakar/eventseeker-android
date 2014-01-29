package com.wcities.eventseeker.core;

public class CityPrefered {

	private String cityName;
	private String countryName;
	private double latitude;
	private double longitude;

	public CityPrefered(String countryName, String cityName, double lat, double lon) {

		this.cityName = cityName;
		this.countryName = countryName;
		this.longitude = lon;
		this.latitude = lat;

	}

	public String getCityName() {
		return cityName;
	}

	public void setCityName(String cityName) {
		this.cityName = cityName;
	}

	public String getCountryName() {
		return countryName;
	}

	public void setCountryName(String countryName) {
		this.countryName = countryName;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

}
