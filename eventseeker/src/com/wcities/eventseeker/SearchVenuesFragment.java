package com.wcities.eventseeker;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.wcities.eventseeker.SearchFragment.SearchFragmentChildListener;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.RecordApi;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Address;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.interfaces.VenueListener;
import com.wcities.eventseeker.jsonparser.RecordApiJSONParser;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class SearchVenuesFragment extends ListFragment implements SearchFragmentChildListener {
	
	private static final String TAG = SearchVenuesFragment.class.getName();
	
	protected static final String VENUE_LIST_FRAGMENT_TAG = "venueListFragment";

	private String query;
	private LoadVenues loadVenues;
	private VenueListAdapter venueListAdapter;
	private int venuesAlreadyRequested;
	private boolean isMoreDataAvailable = true;
	
	private List<Venue> venueList;
	
	private static final int VENUES_LIMIT = 10;
	
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
			venueListAdapter = new VenueListAdapter(FragmentUtil.getActivity(this));
	        
	        Bundle args = getArguments();
			if (args != null && args.containsKey(BundleKeys.QUERY)) {
				venueList.add(null);
				query = args.getString(BundleKeys.QUERY);
				loadVenuesInBackground();
			}
			
		} else {
			venueListAdapter.setmInflater(FragmentUtil.getActivity(this));
		}

		setListAdapter(venueListAdapter);
        getListView().setDivider(null);
        getListView().setBackgroundResource(R.drawable.story_space);
	}
	
	private void loadVenuesInBackground() {
		loadVenues = new LoadVenues();
        loadVenues.execute(query);
	}
	
	private void refresh(String newQuery) {
		//Log.i(TAG, "refresh()");
		// if user selection has changed then only reset the list
		if (query == null || !query.equals(newQuery)) {
			query = newQuery;
			venuesAlreadyRequested = 0;
			isMoreDataAvailable = true;
			
			if (loadVenues != null && loadVenues.getStatus() != Status.FINISHED) {
				loadVenues.cancel(true);
			}
			
			venueList.clear();
			venueList.add(null);
			venueListAdapter.notifyDataSetChanged();
			
			loadVenuesInBackground();
		}
	}
	
	private class LoadVenues extends AsyncTask<String, Void, List<Venue>> {
		
		private static final int MILES = 50000;
		
		@Override
		protected List<Venue> doInBackground(String... params) {
			double[] latLon = DeviceUtil.getLatLon(FragmentUtil.getActivity(SearchVenuesFragment.this));
			RecordApi recordApi = new RecordApi(Api.OAUTH_TOKEN, latLon[0], latLon[1]);
			recordApi.setLimit(VENUES_LIMIT);
			recordApi.setAlreadyRequested(venuesAlreadyRequested);
			recordApi.setMiles(MILES);

			List<Venue> tmpVenues = new ArrayList<Venue>();
			try {
				recordApi.setSearchFor(URLEncoder.encode(params[0], AppConstants.CHARSET_NAME));
				
				JSONObject jsonObject = recordApi.getRecords();
				RecordApiJSONParser jsonParser = new RecordApiJSONParser();
				
				tmpVenues = jsonParser.getVenueList(jsonObject);
				
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				
			} catch (IOException e) {
				e.printStackTrace();
				
			} catch (JSONException e) {
				e.printStackTrace();
			}

			return tmpVenues;
		}
		
		@Override
		protected void onPostExecute(List<Venue> tmpVenues) {
			if (tmpVenues.size() > 0) {
				venueList.addAll(venueList.size() - 1, tmpVenues);
				venuesAlreadyRequested += tmpVenues.size();
				
				if (tmpVenues.size() < VENUES_LIMIT) {
					isMoreDataAvailable = false;
					venueList.remove(venueList.size() - 1);
				}
				
			} else {
				isMoreDataAvailable = false;
				venueList.remove(venueList.size() - 1);
			}
			venueListAdapter.notifyDataSetChanged();
		}    	
    }
	
	public class VenueListAdapter extends BaseAdapter {
		
		private static final String TAG_PROGRESS_INDICATOR = "progressIndicator";
		private static final String TAG_CONTENT = "content";
		
	    private LayoutInflater mInflater;
	    private BitmapCache bitmapCache;

	    public VenueListAdapter(Context context) {
	        mInflater = LayoutInflater.from(context);
	        bitmapCache = BitmapCache.getInstance();
	    }
	    
	    public void setmInflater(Context context) {
	        mInflater = LayoutInflater.from(context);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (venueList.get(position) == null) {
				if (convertView == null || !convertView.getTag().equals(TAG_PROGRESS_INDICATOR)) {
					convertView = mInflater.inflate(R.layout.list_progress_bar, null);
					convertView.setTag(TAG_PROGRESS_INDICATOR);
				}
				
				if ((loadVenues == null || loadVenues.getStatus() == Status.FINISHED) && 
						isMoreDataAvailable) {
					loadVenuesInBackground();
				}
				
			} else {
				if (convertView == null || !convertView.getTag().equals(TAG_CONTENT)) {
					convertView = mInflater.inflate(R.layout.fragment_discover_by_category_list_item_evt, null);
					convertView.setTag(TAG_CONTENT);
					
					convertView.findViewById(R.id.imgEvtTime).setVisibility(View.INVISIBLE);
					convertView.findViewById(R.id.txtEvtTime).setVisibility(View.INVISIBLE);
					convertView.findViewById(R.id.txtEvtTimeAMPM).setVisibility(View.INVISIBLE);
				}
				
				final Venue venue = getItem(position);
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
			        asyncLoadImg.loadImg(imgVenue, ImgResolution.LOW, getListView(), 
			        		position, venue);
			    }
				
				convertView.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						((VenueListener)FragmentUtil.getActivity(SearchVenuesFragment.this)).onVenueSelected(venue);
					}
				});
			}
			
			return convertView;
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

	@Override
	public void onQueryTextSubmit(String query) {
		refresh(query);
	}
}
