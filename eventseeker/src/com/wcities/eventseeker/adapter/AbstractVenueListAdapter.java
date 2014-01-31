package com.wcities.eventseeker.adapter;

import java.util.List;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.widget.AdapterView;
import android.widget.BaseAdapter;

import com.wcities.eventseeker.asynctask.LoadVenues;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;

public abstract class AbstractVenueListAdapter extends BaseAdapter {
	
	protected static final String TAG_PROGRESS_INDICATOR = "progressIndicator";
	protected static final String TAG_CONTENT = "content";
	
	protected List<Venue> venueList;
	
    protected LayoutInflater mInflater;
    
    protected BitmapCache bitmapCache;
	
    protected LoadVenues loadVenues;
	
    protected int venuesAlreadyRequested;
	
    protected boolean isMoreDataAvailable = true;
    
    protected LoadItemsInBackgroundListener listener;
    
    protected AdapterView adapterView;

	protected Fragment fragment;
    
    public AbstractVenueListAdapter(Context context, Fragment fragment, AdapterView adapterView, 
    	LoadItemsInBackgroundListener listener, List<Venue> venueList, LoadVenues loadVenues) {
        mInflater = LayoutInflater.from(context);
        bitmapCache = BitmapCache.getInstance();
        
        this.fragment = fragment;
        this.adapterView = adapterView;
        this.listener = listener;
        this.venueList = venueList;
        this.loadVenues = loadVenues;
    }
    
    public void setmInflater(Context context) {
        mInflater = LayoutInflater.from(context);
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

	public LoadVenues getLoadVenues() {
		return loadVenues;
	}

	public void setLoadVenues(LoadVenues loadVenues) {
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
