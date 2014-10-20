package com.wcities.eventseeker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;

import android.content.res.Resources;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.interfaces.MapListener;
import com.wcities.eventseeker.map.GMapV2Direction;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.GeoUtil;
import com.wcities.eventseeker.util.GeoUtil.GeoUtilListener;

public class AddressMapFragment extends SupportMapFragment implements GeoUtilListener {
	
	private static final String TAG = AddressMapFragment.class.getName();
	
	private double lat, lon, currentLat, currentLon;
	private GoogleMap mMap;
	private boolean drawDrivingDirection;
	private String venueName;

	private Resources res;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		res = getResources();
		
		double[] latLon = DeviceUtil.getCurrentLatLon(FragmentUtil.getApplication(this));
		currentLat = latLon[0];
		currentLon = latLon[1];
		
		Venue venue;
		if (getArguments().containsKey(BundleKeys.EVENT)) {
			// Called from event details screen
			Event event = (Event) getArguments().getSerializable(BundleKeys.EVENT);
			venue = event.getSchedule().getVenue();
			
		} else {
			// called from venue details screen
			venue = (Venue) getArguments().getSerializable(BundleKeys.VENUE);
		}
		
		venueName = venue.getName();
		com.wcities.eventseeker.core.Address address = venue.getAddress();
		if (address != null) {
			lat = address.getLat();
			lon = address.getLon();
			
			if (lat == 0 && lon == 0) {
				findLatLonFromAddress(address);
			}
			
		} else {
			findLatLonFromName(venue);
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		return super.onCreateView(inflater, container, savedInstanceState);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		//Log.d(TAG, "onActivityCreated()");
		super.onActivityCreated(savedInstanceState);
		boolean mapSetUp = setUpMapIfNeeded();
        //Log.d(TAG, "map set up = " + mapSetUp);
        if (mapSetUp && lat != 0 && lon != 0) {
        	 // The Map is verified. It is now safe to manipulate the map.
        	setMarker();
        }
	}
	
	public void updateAddress(com.wcities.eventseeker.core.Address venueAddress) {
		findLatLonFromAddress(venueAddress);
		boolean mapSetUp = setUpMapIfNeeded();
        if (mapSetUp) {
        	 // The Map is verified. It is now safe to manipulate the map.
        	setMarker();
        }
	}
	
	public void updateLatLon(double lat, double lon) {
		//Log.d(TAG, "updateLatLon(), lat = " + lat + ", lon = " + lon);
		this.lat = lat;
		this.lon = lon;
		boolean mapSetUp = setUpMapIfNeeded();
        if (mapSetUp) {
        	 // The Map is verified. It is now safe to manipulate the map.
        	setMarker();
        }
	}
	
	private void findLatLonFromName(Venue venue) {
		List<Address> addresses = null;
		Geocoder geocoder = new Geocoder(FragmentUtil.getActivity(this));
		try {
			addresses = geocoder.getFromLocationName(venue.getName(), 1);
			if (addresses != null && !addresses.isEmpty()) {
				Address address = addresses.get(0);
				lat = address.getLatitude();
				lon = address.getLongitude();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Alternative way to find string address
		if (addresses == null || addresses.isEmpty()) {
			GeoUtil.getFromAddress(venue.getName(), this);
		}
	}
	
	private void findLatLonFromAddress(com.wcities.eventseeker.core.Address venueAddress) {
		//Log.d(TAG, "findLatLonFromAddress()");
		String strAddress = "";
		strAddress = (venueAddress.getAddress1() != null) ? strAddress + venueAddress.getAddress1() : strAddress;
		try {
			if (!findLatLonFromAddress(strAddress)) {
				
				if (venueAddress.getAddress2() != null) {
					//Log.d(TAG, "findLatLonFromAddress() - address2");
					strAddress = venueAddress.getAddress2();
					
					if (!findLatLonFromAddress(strAddress) && venueAddress.getCity() != null) {
						//Log.d(TAG, "findLatLonFromAddress() - city");
						findLatLonFromAddress(venueAddress.getCity()); 
					}
					
				} else if (venueAddress.getCity() != null) {
					//Log.d(TAG, "findLatLonFromAddress() - city");
					findLatLonFromAddress(venueAddress.getCity());
				}
			}
			//Log.d(TAG, "findLatLonFromAddress(), done for strAddress = " + strAddress);
			
		} catch (IOException e) {
			e.printStackTrace();
			GeoUtil.getFromAddress(strAddress, this);
		}
	}
	
	private boolean findLatLonFromAddress(String strAddress) throws IOException {
		//Log.d(TAG, "findLatLonFromAddress(str)");
		Geocoder geocoder = new Geocoder(FragmentUtil.getActivity(this));
		List<Address> addresses = geocoder.getFromLocationName(strAddress, 1);
		if (addresses != null && !addresses.isEmpty()) {
			Log.d(TAG, "findLatLonFromAddress(str), addresses != null");
			Address address = addresses.get(0);
			lat = address.getLatitude();
			lon = address.getLongitude();
			return true;
		}
		//Log.d(TAG, "findLatLonFromAddress(str) done");
		return false;
	}

	private boolean setUpMapIfNeeded() {
		boolean mapSetUp = true;
		// Do a null check to confirm that we have not already instantiated the map.
		if (mMap == null) {
			mMap = getMap();

			// Check if we were successful in obtaining the map.
			if (mMap == null) {
				// Map is not verified. Hence we cannot manipulate the map.
				mapSetUp = false;
				
			} else {
				mMap.getUiSettings().setAllGesturesEnabled(false);
				mMap.getUiSettings().setZoomControlsEnabled(false);
				mMap.setOnMapClickListener(new OnMapClickListener() {
					
					@Override
					public void onMapClick(LatLng arg0) {
						onMapClicked();
					}
				});
				mMap.setOnMarkerClickListener(new OnMarkerClickListener() {
					
					@Override
					public boolean onMarkerClick(Marker arg0) {
						onMapClicked();
						return true;
					}
				});
			}
		}
		return mapSetUp;
	}
	
	private void onMapClicked() {
		//Log.d(TAG, "onMapClick()");
		Bundle args = new Bundle();
		args.putString(BundleKeys.VENUE_NAME, venueName);
		args.putDouble(BundleKeys.LAT, lat);
		args.putDouble(BundleKeys.LON, lon);
		args.putBoolean(BundleKeys.DRAW_DRIVING_DIRECTION, drawDrivingDirection);
		((MapListener)FragmentUtil.getActivity(AddressMapFragment.this)).onMapClicked(args);
	}
	
	private void setMarker() {
		//Log.d(TAG, "setMarker()");
    	LatLng latLng = new LatLng(lat, lon);
    	mMap.clear();
    	mMap.addMarker(new MarkerOptions().position(latLng));
    	mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14), 100, null);
    	
    	if (drawDrivingDirection) {
    		new GetDrivingDirection().execute();
    	}
    }
	
	public void displayDrivingDirection() {
		if (currentLat != AppConstants.NOT_ALLOWED_LAT && currentLon != AppConstants.NOT_ALLOWED_LON 
				&& mMap != null) {
			new GetDrivingDirection().execute();
		}
	}
	
	private class GetDrivingDirection extends AsyncTask<Void, Void, PolylineOptions> {
		

		@Override
		protected PolylineOptions doInBackground(Void... params) {
			LatLng fromPosition = new LatLng(currentLat, currentLon);
			LatLng toPosition = new LatLng(lat, lon);

			GMapV2Direction md = new GMapV2Direction();

			Document doc = md.getDocument(fromPosition, toPosition, GMapV2Direction.MODE_DRIVING);
			ArrayList<LatLng> directionPoint = md.getDirection(doc);
			PolylineOptions polylineOptions = new PolylineOptions().width(3).color(Color.RED);

			for (int i = 0 ; i < directionPoint.size() ; i++) {          
				polylineOptions.add(directionPoint.get(i));
			}
			return polylineOptions;
		}
		
		@Override
		protected void onPostExecute(PolylineOptions result) {
			mMap.addPolyline(result);
			fixZoom(result);
		}
		
		private void fixZoom(PolylineOptions polylineOptions) {
		    List<LatLng> points = polylineOptions.getPoints(); // route is instance of PolylineOptions 

		    if (points.isEmpty()) {
		    	Toast.makeText(FragmentUtil.getActivity(AddressMapFragment.this), 
		    		res.getString(R.string.couldnt_find_drive_location), Toast.LENGTH_LONG).show();
		    	
		    } else {
			    LatLngBounds.Builder bc = new LatLngBounds.Builder();
	
			    for (LatLng item : points) {
			        bc.include(item);
			    }
	
			    CameraUpdate lastCameraUpdate = CameraUpdateFactory.newLatLngBounds(bc.build(), ConversionUtil.toPx(getResources(), 50));
			    mMap.moveCamera(lastCameraUpdate);
			    drawDrivingDirection = true;
		    }
		}
	}

	@Override
	public void onAddressSearchCompleted(String strAddress) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onLatlngSearchCompleted(Address address) {
		if (address != null) {
			updateLatLon(address.getLatitude(), address.getLongitude());
		}
	}

	@Override
	public void onCitySearchCompleted(String city) {
		// TODO Auto-generated method stub
		
	}
}
