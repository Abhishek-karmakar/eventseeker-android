package com.wcities.eventseeker.core;

import java.io.Serializable;

import android.location.Location;
import android.util.Log;

import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.app.EventSeekr.ProximityUnit;
import com.wcities.eventseeker.cache.BitmapCacheable;

public class Venue implements Serializable, BitmapCacheable {
	
	private static final String TAG = Venue.class.getName();
	private static final String DEFAULT_HIGH_RES_PATH = "http://c0056906.cdn2.cloudfiles.rackspacecloud.com/";
	private static final String DEFAULT_LOW_RES_PATH = "http://c0056904.cdn2.cloudfiles.rackspacecloud.com/";
	private static final String DEFAULT_MOBI_RES_PATH = "http://c416814.r14.cf2.rackcdn.com/";
	
	private int id;
	private String name;
	private String imagefile;
	private String imageUrl;
	private Address address;
	private ImageAttribution imageAttribution;
	private String longDesc;
	private String phone;
	private String url;
    private String fbLink;
	
	public Venue(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getImagefile() {
		return imagefile;
	}

	public void setImagefile(String imagefile) {
		this.imagefile = imagefile;
	}
	
	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public ImageAttribution getImageAttribution() {
		return imageAttribution;
	}

	public void setImageAttribution(ImageAttribution imageAttribution) {
		this.imageAttribution = imageAttribution;
	}

	public String getLongDesc() {
		return longDesc;
	}

	public void setLongDesc(String longDesc) {
		this.longDesc = longDesc;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

    public String getFbLink() {
        return fbLink;
    }

    public void setFbLink(String fbLink) {
        this.fbLink = fbLink;
    }

    public boolean doesValidImgUrlExist() {
		if (getLowResImgUrl() != null || getMobiResImgUrl() != null || getHighResImgUrl() != null) {
			return true;
			
		} else {
			return false;
		}
	}

	@Override
	public String getLowResImgUrl() {
		if (imageUrl != null) {
			return imageUrl;
			
		} else if (imageAttribution == null || imageAttribution.getLowResPath() == null) {
			return DEFAULT_LOW_RES_PATH + getImagefile();
			
		} else {
			return imageAttribution.getLowResPath() + getImagefile();
		}
	}
	
	@Override
	public String getMobiResImgUrl() {
		if (imageUrl != null) {
			return imageUrl;
			
		} else if (imageAttribution == null || imageAttribution.getMobiResPath() == null) {
			return DEFAULT_MOBI_RES_PATH + getImagefile();
			
		} else {
			return imageAttribution.getMobiResPath() + getImagefile();
		}
	}
	
	@Override
	public String getHighResImgUrl() {
		if (imageUrl != null) {
			return imageUrl;
			
		} else if (imageAttribution == null || imageAttribution.getHighResPath() == null) {
			return DEFAULT_HIGH_RES_PATH + getImagefile();
			
		} else {
			return imageAttribution.getHighResPath() + getImagefile();
		}
	}
	
	@Override
	public String getKey(ImgResolution imgResolution) {
		return new StringBuilder(getClass().getName()).append("_").append(id).append("_").append(imgResolution).toString();
	}
	
	public String getFormatedAddress(boolean includeCountry) {
		String adrs = "";
		if (address != null) {
			if (address.getAddress1() != null) {
				adrs += address.getAddress1();
			}
			
			if (address.getCity() != null) {
				if (adrs.length() != 0) {
					adrs += ", ";
				}
				adrs += address.getCity();
			} 

			//TODO: Right now we are not parsing the zip code for EventSeeker app, 
			// if in future we start parsing zip code then uncomment the below lines
			/*if (address.getZip() != null) {
				if (adrs.length() != 0) {
					adrs += ", ";
				}
				adrs += address.getZip();
			}*/ 
			
			if (includeCountry && address.getCountry() != null) {
				if (adrs.length() != 0) {
					adrs += ", ";
				}
				adrs += address.getCountry().getName();
			} 
		}
		return adrs;
	}

	/**
	 * calculates the APPROXIMATE distance of venue location from the given GeoLocation Coordinates
	 * @param lat
	 * @param lon
	 * @return
	 */
	/*public double getDistanceFrom(double lat, double lon) {
		// 1 Meter = 0.000621371 Mile
		double mileConversionFactor = 0.000621371;
		
		Location venueLocation = new Location("");
		venueLocation.setLatitude(address.getLat());
		venueLocation.setLongitude(address.getLon());

		Location deviceLocation = new Location("");
		deviceLocation.setLatitude(lat);
		deviceLocation.setLongitude(lon);

		float distanceInMeter = venueLocation.distanceTo(deviceLocation);
		
		return distanceInMeter * mileConversionFactor;
	}*/
	
	/**
	 * calculates the APPROXIMATE distance of venue location from the given GeoLocation Coordinates
	 * @param lat
	 * @param lon
	 * @param ctx TODO
	 * @param prxmtyUnt TODO
	 * @return
	 */
	public double getDistanceFrom(double lat, double lon, EventSeekr eventSeekr) {
		// 1 Meter = 0.000621371 Mile
		//double mileConversionFactor = 0.000621371;
		
		Location venueLocation = new Location("");
		venueLocation.setLatitude(address.getLat());
		venueLocation.setLongitude(address.getLon());

		Location savedLocation = new Location("");
		savedLocation.setLatitude(lat);
		savedLocation.setLongitude(lon);

		float distance = venueLocation.distanceTo(savedLocation);
		Log.i(TAG, "DISTANCE IN METERS: " + distance);
		/******************************
		 * this is distance in meters.*
		 ******************************/
		Log.i(TAG, "SAVED UNIT: " + eventSeekr.getSavedProximityUnit().toString());
		if (EventSeekr.isConnectedWithBosch()) {
			ProximityUnit prxmtyUnt = eventSeekr.getSavedProximityUnit();
			if (prxmtyUnt == ProximityUnit.MI) {
				return distance * (ProximityUnit.CONVERSION_FACTOR / 1000);//meters to miles
			} else {
				return distance / 1000;//meters to kilometers
			}
		}
		return distance * (ProximityUnit.CONVERSION_FACTOR / 1000);//meters to miles
		//return distanceInMeter * mileConversionFactor;
	}

}
