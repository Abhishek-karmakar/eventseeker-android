package com.wcities.eventseeker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;

import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.map.GMapV2Direction;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.GeoUtil;
import com.wcities.eventseeker.util.GeoUtil.GeoUtilListener;

public class FullScreenAddressMapFragment extends FragmentLoadableFromBackStack implements GeoUtilListener {
	
	private static final String TAG = FullScreenAddressMapFragment.class.getName();

	private static final String MAP_FRAGMENT_TAG = "mapFragment";

	private GoogleMap mMap;
	private String strAddress = "";
	
	private double lat, lon, currentLat, currentLon;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		double[] latLon = DeviceUtil.getLatLon(FragmentUtil.getActivity(this));
		currentLat = latLon[0];
		currentLon = latLon[1];
		
		Bundle args = getArguments();
		lat = args.getDouble(BundleKeys.LAT);
		lon = args.getDouble(BundleKeys.LON);
			
		setRetainInstance(true);
	}
	
    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_change_location, null);
        
        MapFragment mMapFragment = new MapFragment();
        Bundle args = new Bundle();
        args.putDouble(BundleKeys.LAT, lat);
        args.putDouble(BundleKeys.LON, lon);
        mMapFragment.setArguments(args);
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.add(R.id.rootLnrLayout, mMapFragment, MAP_FRAGMENT_TAG).commit();
        
        return v;
    }
    
    private void updateStrAddress(Address address) {
    	strAddress = "";
    	int maxIndex = address.getMaxAddressLineIndex() > 1 ? 1 : address.getMaxAddressLineIndex();
    	for (int i = 0; i <= maxIndex; i++) {
			if (!strAddress.equals("")) {
				strAddress = strAddress.concat(", ");
			}
			strAddress = strAddress.concat(address.getAddressLine(i));
		}
    }
    
    private void setMarker(double lat, double lon) {
    	LatLng latLng = new LatLng(lat, lon);
    	mMap.addMarker(new MarkerOptions().position(latLng).title(strAddress));
    	
    	mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
    	mMap.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);
    	
    	if (getArguments().getBoolean(BundleKeys.DRAW_DRIVING_DIRECTION)) {
    		new GetDrivingDirection().execute();
    	}
    }
    
    private boolean setUpMapIfNeeded() {
    	boolean mapSetUp = true;
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
        	MapFragment mMapFragment = (MapFragment) getChildFragmentManager().findFragmentByTag(MAP_FRAGMENT_TAG);
            mMap = mMapFragment.getMap();
            
            // Check if we were successful in obtaining the map.
            if (mMap == null) {
                // Map is not verified. Hence we cannot manipulate the map.
            	mapSetUp = false;
            } 
        }
        return mapSetUp;
    }
    
    public static class MapFragment extends SupportMapFragment {
    	
    	@Override
    	public void onActivityCreated(Bundle savedInstanceState) {
    		super.onActivityCreated(savedInstanceState);
    		
    		double lat = getArguments().getDouble(BundleKeys.LAT);
    		double lon = getArguments().getDouble(BundleKeys.LON);
            
    		List<Address> addresses = null;
            Geocoder geocoder = new Geocoder(FragmentUtil.getActivity(this));
    		try {
    			addresses = geocoder.getFromLocation(lat, lon, 1);
    			
    			if (addresses != null && !addresses.isEmpty()) {
    				Address address = addresses.get(0);
    				//Log.i(TAG, "address=" + address);
    				((FullScreenAddressMapFragment) getParentFragment()).updateStrAddress(address);
    				
    			} else {
            		Log.w(TAG, "No relevant address found.");
    			}
    			
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
    		
    		// Alternative way to find lat-lon
    		if (addresses == null || addresses.isEmpty()) {
    			GeoUtil.getAddressFromLocation(lat, lon, (GeoUtilListener) getParentFragment());
    		}

            boolean mapSetUp = ((FullScreenAddressMapFragment) getParentFragment()).setUpMapIfNeeded();
            Log.i(TAG, "map set up = " + mapSetUp);
            if (mapSetUp) {
            	 // The Map is verified. It is now safe to manipulate the map.
            	((FullScreenAddressMapFragment) getParentFragment()).setMarker(lat, lon);
            }
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
		    	Toast.makeText(FragmentUtil.getActivity(FullScreenAddressMapFragment.this), "Could not find the driving direction for this venue.", Toast.LENGTH_LONG).show();
		    	
		    } else {
			    LatLngBounds.Builder bc = new LatLngBounds.Builder();
	
			    for (LatLng item : points) {
			        bc.include(item);
			    }
	
			    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bc.build(), ConversionUtil.toPx(getResources(), 50)));
		    }
		}
	}

	@Override
	public void onAddressSearchCompleted(String strAddress) {
		this.strAddress = strAddress;
		boolean mapSetUp = setUpMapIfNeeded();
        if (mapSetUp) {
        	 // The Map is verified. It is now safe to manipulate the map.
        	setMarker(lat, lon);
        }
	}

	@Override
	public void onLatlngSearchCompleted(Address address) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCitySearchCompleted(String city) {
		// TODO Auto-generated method stub
		
	}
}
