package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.wcities.eventseeker.SearchFragment.SearchFragmentChildListener;
import com.wcities.eventseeker.adapter.AbstractVenueListAdapter;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.asynctask.LoadVenues;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.interfaces.CustomSharedElementTransitionSource;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.interfaces.VenueListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.ViewUtil;
import com.wcities.eventseeker.viewdata.SharedElement;
import com.wcities.eventseeker.viewdata.SharedElementPosition;

public class SearchVenuesFragment extends ListFragment implements SearchFragmentChildListener, 
		LoadItemsInBackgroundListener, CustomSharedElementTransitionSource, AsyncTaskListener<Void> {
	
	private static final String TAG = SearchVenuesFragment.class.getSimpleName();
	
	protected static final String VENUE_LIST_FRAGMENT_TAG = "venueListFragment";

	private String query;

	private LoadVenues loadVenues;

	private RelativeLayout rltLytPrgsBar;
	private VenueListAdapter venueListAdapter;
	
	private List<Venue> venueList;
	private double[] latLng;
	
	private List<View> hiddenViews;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		hiddenViews = new ArrayList<View>();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		//Log.d(TAG, "onCreateView()");
		View v = inflater.inflate(R.layout.list_with_centered_progress, null);

		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			int pad = getResources().getDimensionPixelSize(R.dimen.tab_bar_margin_fragment_custom_tabs);
			v.findViewById(android.R.id.list).setPadding(pad, 0, pad, 0);
		}
		
		rltLytPrgsBar = (RelativeLayout) v.findViewById(R.id.rltLytPrgsBar);
		rltLytPrgsBar.setBackgroundResource(R.drawable.bg_no_content_overlay);
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		//Log.d(TAG, "onActivityCreated()");
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
			if (!venueList.isEmpty() && venueList.get(0) != null && venueList.get(0).getId() == AppConstants.INVALID_ID) {
				getListView().setBackgroundResource(R.drawable.bg_no_content_overlay);
			}
		}

		setListAdapter(venueListAdapter);
        getListView().setDivider(null);
	}
	
	@Override
	public void loadItemsInBackground() {
		if (latLng == null) {
			latLng = DeviceUtil.getLatLon(FragmentUtil.getApplication(this));
		}
		loadVenues = new LoadVenues(Api.OAUTH_TOKEN, venueListAdapter, venueList, latLng, this);
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
	    	final Venue venue = getItem(position);
	    	
			if (venueList.get(position) == null) {
				if (convertView == null || !convertView.getTag().equals(AppConstants.TAG_PROGRESS_INDICATOR)) {
					convertView = mInflater.inflate(R.layout.progress_bar_eventseeker_fixed_ht, null);
					convertView.setTag(AppConstants.TAG_PROGRESS_INDICATOR);
				}
				
				if (venueList.size() == 1) {
					((SearchVenuesFragment) fragment).rltLytPrgsBar.setVisibility(View.VISIBLE);
					convertView.setVisibility(View.INVISIBLE);
					
				} else {
					convertView.setVisibility(View.VISIBLE);
				}
				
				if ((loadVenues == null || loadVenues.getStatus() == Status.FINISHED) && isMoreDataAvailable) {
					listener.loadItemsInBackground();
				}
				
			} else {
				
				if (venue.getId() == AppConstants.INVALID_ID) {
					convertView = mInflater.inflate(R.layout.list_no_items_found, null);
					((TextView)convertView).setText("No Venue Found.");
					convertView.setTag("");
					return convertView;
				
				} else if (convertView == null || !convertView.getTag().equals(AppConstants.TAG_CONTENT)) {
					convertView = mInflater.inflate(R.layout.item_list_search_venue, null);
					convertView.setTag(AppConstants.TAG_CONTENT);
				}
				
				((TextView)convertView.findViewById(R.id.txtVenueTitle)).setText(venue.getName());
				
				String[] address = getVenueAddress(venue);
				((TextView)convertView.findViewById(R.id.txtVenueLocation)).setText(address[0]);
				((TextView)convertView.findViewById(R.id.txtVenueCity)).setText(address[1]);
				
		    	final ImageView imgVenue = (ImageView)convertView.findViewById(R.id.imgEvent); 
				String key = venue.getKey(ImgResolution.LOW);
				Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
				if (bitmap != null) {
			        imgVenue.setImageBitmap(bitmap);

				} else {
			        imgVenue.setImageBitmap(null);
			        
			        //Log.i(TAG, "url = " + venue.getMobiResImgUrl());
			        AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
			        asyncLoadImg.loadImg(imgVenue, ImgResolution.LOW, adapterView, position, venue);
			    }
				
				convertView.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						List<SharedElement> sharedElements = new ArrayList<SharedElement>();

						int[] loc = ViewUtil.getLocationOnScreen(v, FragmentUtil.getResources(fragment));
						RelativeLayout.LayoutParams lp = (LayoutParams) imgVenue.getLayoutParams();
						
						SharedElementPosition sharedElementPosition = new SharedElementPosition(lp.leftMargin, 
								loc[1] + imgVenue.getPaddingTop(), lp.width, lp.height - 
								imgVenue.getPaddingTop() - imgVenue.getPaddingBottom());
						SharedElement sharedElement = new SharedElement(sharedElementPosition, imgVenue);
						sharedElements.add(sharedElement);
						((CustomSharedElementTransitionSource) fragment).addViewsToBeHidden(imgVenue);
						
						((VenueListener)FragmentUtil.getActivity(fragment)).onVenueSelected(venue, sharedElements);

						((CustomSharedElementTransitionSource) fragment).onPushedToBackStack();
					}
				});
			}
			
			return convertView;
		}
	    
	    private String[] getVenueAddress(Venue venue) {
			String location = "";
			String city = "";
			if (venue.getAddress() != null) {
				if (venue.getAddress().getAddress1() != null) {
					location += venue.getAddress().getAddress1();
				}

				if (venue.getAddress().getAddress2() != null) {
					if (location.length() != 0) {
						location += ", ";
					}
					location += venue.getAddress().getAddress2();
				}
				
				if (venue.getAddress().getCity() != null) {
					city += venue.getAddress().getCity();
				} 
				
				if (venue.getAddress().getState() != null) {
					if (city.length() != 0) {
						city += ", ";
					}
					city += venue.getAddress().getState();
				} 
			}
			return new String[]{location, city};
		}
	}
	
	@Override
	public void onQueryTextSubmit(String query) {
		refresh(query);
	}

	@Override
	public void addViewsToBeHidden(View... views) {
		for (int i = 0; i < views.length; i++) {
			hiddenViews.add(views[i]);
		}
	}

	@Override
	public void hideSharedElements() {
		for (Iterator<View> iterator = hiddenViews.iterator(); iterator.hasNext();) {
			View view = iterator.next();
			view.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	public void onPushedToBackStack() {
		((CustomSharedElementTransitionSource) getParentFragment()).onPushedToBackStack();
	}

	@Override
	public void onPoppedFromBackStack() {
		for (Iterator<View> iterator = hiddenViews.iterator(); iterator.hasNext();) {
			View view = iterator.next();
			view.setVisibility(View.VISIBLE);
		}
		hiddenViews.clear();
	}

	@Override
	public boolean isOnTop() {
		return false;
	}

	@Override
	public void onTaskCompleted(Void... params) {
		// remove full screen progressbar
		rltLytPrgsBar.setVisibility(View.INVISIBLE);
		if (!venueList.isEmpty() && venueList.get(0).getId() == AppConstants.INVALID_ID) {
			getListView().setBackgroundResource(R.drawable.bg_no_content_overlay);
		}
	}
}
