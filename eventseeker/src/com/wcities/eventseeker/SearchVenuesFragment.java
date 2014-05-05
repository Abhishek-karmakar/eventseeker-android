package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import com.wcities.eventseeker.SearchFragment.SearchFragmentChildListener;
import com.wcities.eventseeker.adapter.AbstractVenueListAdapter;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.asynctask.LoadVenues;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Address;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.interfaces.VenueListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class SearchVenuesFragment extends ListFragment implements SearchFragmentChildListener, 
	LoadItemsInBackgroundListener {
	
	private static final String TAG = SearchVenuesFragment.class.getName();
	
	protected static final String VENUE_LIST_FRAGMENT_TAG = "venueListFragment";

	private String query;

	private LoadVenues loadVenues;

	private VenueListAdapter venueListAdapter;
	
	private List<Venue> venueList;
	private double[] latLng;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = super.onCreateView(inflater, container, savedInstanceState);

		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			int pad = getResources().getDimensionPixelSize(R.dimen.tab_bar_margin_fragment_custom_tabs);
			v.setPadding(pad, 0, pad, 0);
		}
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if (venueList == null) {
			venueList = new ArrayList<Venue>();
			venueListAdapter = new VenueListAdapter(FragmentUtil.getActivity(this), this, getListView(), 
				this, venueList, loadVenues);
	        
	        Bundle args = getArguments();
			if (args != null && args.containsKey(BundleKeys.QUERY)) {
				venueList.add(null);
				query = args.getString(BundleKeys.QUERY);
				loadItemsInBackground();
			}
			
		} else {
			venueListAdapter.setmInflater(FragmentUtil.getActivity(this));
		}

		setListAdapter(venueListAdapter);
        getListView().setDivider(null);
        getListView().setBackgroundResource(R.drawable.story_space);
	}
	
	@Override
	public void loadItemsInBackground() {
		if (latLng == null) {
			latLng = DeviceUtil.getLatLon(FragmentUtil.getApplication(this));
		}
		loadVenues = new LoadVenues(this, venueListAdapter, venueList, latLng);
		venueListAdapter.setLoadVenues(loadVenues);
        AsyncTaskUtil.executeAsyncTask(loadVenues, true, query);
	}
	
	private void refresh(String newQuery) {
		//Log.i(TAG, "refresh()");
		// if user selection has changed then only reset the list
		if (query == null || !query.equals(newQuery)) {
			query = newQuery;
			venueListAdapter.setVenuesAlreadyRequested(0);
			venueListAdapter.setMoreDataAvailable(true);
			
			if (loadVenues != null && loadVenues.getStatus() != Status.FINISHED) {
				loadVenues.cancel(true);
			}
			
			venueList.clear();
			venueList.add(null);
			venueListAdapter.notifyDataSetChanged();
			
			loadItemsInBackground();
		}
	}
	
	private static class VenueListAdapter extends AbstractVenueListAdapter {
		
	    public VenueListAdapter(Context context, Fragment fragment, AdapterView adapterView, 
	    	LoadItemsInBackgroundListener listener, List<Venue> venueList, LoadVenues loadVenues) {
	    	super(context, fragment, adapterView, listener, venueList, loadVenues);
	    }
	    
	    @Override
		public View getView(int position, View convertView, ViewGroup parent) {
	    	
			if (venueList.get(position) == null) {
				if (convertView == null || !convertView.getTag().equals(AppConstants.TAG_PROGRESS_INDICATOR)) {
					convertView = mInflater.inflate(R.layout.list_progress_bar, null);
					convertView.setTag(AppConstants.TAG_PROGRESS_INDICATOR);
				}
				
				if ((loadVenues == null || loadVenues.getStatus() == Status.FINISHED) && 
						isMoreDataAvailable) {
					listener.loadItemsInBackground();
				}
				
			} else {
				
				final Venue venue = getItem(position);
				
				if (venue.getId() == AppConstants.INVALID_ID) {
					convertView = mInflater.inflate(R.layout.list_no_items_found, null);
					((TextView)convertView).setText("No Venue Found.");
					convertView.setTag("");
					return convertView;
				
				} else if (convertView == null || !convertView.getTag().equals(AppConstants.TAG_CONTENT)) {
					convertView = mInflater.inflate(R.layout.fragment_discover_by_category_list_item_evt, null);
					convertView.setTag(AppConstants.TAG_CONTENT);
					convertView.findViewById(R.id.imgEvtTime).setVisibility(View.INVISIBLE);
					convertView.findViewById(R.id.txtEvtTime).setVisibility(View.INVISIBLE);
				}
				
				((TextView)convertView.findViewById(R.id.txtEvtTitle)).setText(venue.getName());
				
				Address address = venue.getAddress();
				((TextView)convertView.findViewById(R.id.txtEvtLocation)).setText(address.getCity() + ", " 
						+ address.getCountry().getName());
				
				String key = venue.getKey(ImgResolution.LOW);
				Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
				
				if (bitmap != null) {
			        ((ImageView)convertView.findViewById(R.id.imgEvent)).setImageBitmap(bitmap);

				} else {
			    	ImageView imgVenue = (ImageView)convertView.findViewById(R.id.imgEvent); 
			        imgVenue.setImageBitmap(null);
			        
			        //Log.i(TAG, "url = " + venue.getMobiResImgUrl());
			        AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
			        asyncLoadImg.loadImg(imgVenue, ImgResolution.LOW, adapterView, position, venue);
			    }
				
				convertView.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						((VenueListener)FragmentUtil.getActivity(fragment)).onVenueSelected(venue);
					}
				});
			}
			
			return convertView;
		}
	}

	@Override
	public void onQueryTextSubmit(String query) {
		refresh(query);
	}
}
