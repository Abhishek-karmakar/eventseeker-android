package com.wcities.eventseeker.adapter;

import java.util.List;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.widget.AdapterView;
import android.widget.BaseAdapter;

import com.wcities.eventseeker.asynctask.LoadVenues;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.interfaces.VenueAdapterListener;

public abstract class AbstractVenueListAdapter<T> extends BaseAdapter implements VenueAdapterListener<T> {
	
	protected List<Venue> venueList;
	
    protected LayoutInflater mInflater;
    
    protected BitmapCache bitmapCache;
	
    protected AsyncTask<T, Void, List<Venue>> loadVenues;
	
    protected int venuesAlreadyRequested;
	
    protected boolean isMoreDataAvailable = true;
    
    protected LoadItemsInBackgroundListener listener;
    
    protected AdapterView adapterView;

	protected Fragment fragment;
    
    public AbstractVenueListAdapter(Context context, Fragment fragment, AdapterView adapterView, 
    	LoadItemsInBackgroundListener listener, List<Venue> venueList, AsyncTask<T, Void, List<Venue>> loadVenues) {
        mInflater = LayoutInflater.from(context);
        bitmapCache = BitmapCache.getInstance();
        
        this.fragment = fragment;
        this.adapterView = adapterView;
        this.listener = listener;
        this.venueList = venueList;
        this.loadVenues = loadVenues;
    }
    
	public int getVenuesAlreadyRequested() {
		return venuesAlreadyRequested;
	}

	public void setVenuesAlreadyRequested(int venuesAlreadyRequested) {
		this.venuesAlreadyRequested = venuesAlreadyRequested;
	}

	public boolean isMoreDataAvailable() {
		return isMoreDataAvailable;
	}

	public void setMoreDataAvailable(boolean isMoreDataAvailable) {
		this.isMoreDataAvailable = isMoreDataAvailable;
	}

	public AsyncTask<T, Void, List<Venue>> getLoadVenues() {
		return loadVenues;
	}

	@Override
	public void updateContext(Context context) {
		 mInflater = LayoutInflater.from(context);
	}

	@Override
	public void setLoadVenues(AsyncTask<T, Void, List<Venue>> loadVenues) {
		this.loadVenues = loadVenues;
	}

	@Override
	public Venue getItem(int position) {
		return venueList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public int getCount() {
		return venueList.size();
	}
}
