package com.wcities.eventseeker.bosch;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
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

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.adapter.AbstractVenueListAdapter;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.asynctask.LoadVenues;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.interfaces.BoschOnChildFragmentDisplayModeChangedListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.interfaces.VenueListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.ViewUtil;

public class BoschSearchVenuesFragment extends ListFragment implements LoadItemsInBackgroundListener, OnClickListener, 
		BoschOnChildFragmentDisplayModeChangedListener {

	private static final String TAG = BoschSearchVenuesFragment.class.getName();

	private String query;

	private LoadVenues loadVenues;

	private VenueListAdapter venueListAdapter;
	
	private List<Venue> venueList;
	private double[] latLng;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		
		View view = inflater.inflate(R.layout.bosch_common_list_layout, null);

		view.findViewById(R.id.btnUp).setOnClickListener(this);
		view.findViewById(R.id.btnDown).setOnClickListener(this);

		return view;
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

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
			case R.id.btnUp:
				getListView().smoothScrollByOffset(-1);
				break;
			
			case R.id.btnDown:
				getListView().smoothScrollByOffset(1);
				break;
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
					convertView = mInflater.inflate(R.layout.bosch_venue_list_item, null);
					convertView.setTag(AppConstants.TAG_CONTENT);
				}

				((TextView) convertView.findViewById(R.id.txtVenueName)).setText(venue.getName());
				
				String key = venue.getKey(ImgResolution.LOW);
				Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
				if (bitmap != null) {
			        ((ImageView)convertView.findViewById(R.id.imgVenue)).setImageBitmap(bitmap);
			        
			    } else {
			    	ImageView imgVenue = ((ImageView)convertView.findViewById(R.id.imgVenue));
			        imgVenue.setImageBitmap(null);
			        
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
			
			ViewUtil.updateViewColor(fragment.getResources(), convertView);
			return convertView;
		}
	}

	@Override
	public void onChildFragmentDisplayModeChanged() {
		if (venueListAdapter != null) {
			venueListAdapter.notifyDataSetChanged();
		}
	}
}
