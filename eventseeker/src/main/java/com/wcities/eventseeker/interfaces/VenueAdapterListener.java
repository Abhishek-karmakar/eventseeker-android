package com.wcities.eventseeker.interfaces;

import android.content.Context;
import android.os.AsyncTask;

import com.wcities.eventseeker.core.Venue;

import java.util.List;

public interface VenueAdapterListener<T> {
	public void setMoreDataAvailable(boolean isMoreDataAvailable);
	public void setVenuesAlreadyRequested(int venuesAlreadyRequested);
	public int getVenuesAlreadyRequested();
	public void updateContext(Context context);
	public void setLoadVenues(AsyncTask<T, Void, List<Venue>> loadVenues);
}
