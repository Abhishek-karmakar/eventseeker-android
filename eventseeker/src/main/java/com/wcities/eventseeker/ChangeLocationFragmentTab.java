package com.wcities.eventseeker;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.custom.fragment.FragmentRetainingChildFragmentManager;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.GeoUtil;
import com.wcities.eventseeker.util.GeoUtil.GeoUtilListener;

import java.io.IOException;
import java.util.List;

public class ChangeLocationFragmentTab extends FragmentRetainingChildFragmentManager implements OnQueryTextListener, GeoUtilListener, 
		OnClickListener {
	
	private static final String TAG = ChangeLocationFragmentTab.class.getSimpleName();

	private GoogleMap mMap;
	private String strAddress = "";
	
	private double lat, lon;

	private boolean isMyLocationClicked;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		
		double[] latLon = DeviceUtil.getLatLon(FragmentUtil.getApplication(this));
		lat = latLon[0];
		lon = latLon[1];
	}
	
    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_change_location, null);
        
        v.findViewById(R.id.btnMyLocation).setOnClickListener(this);
        
        MapFragment mMapFragment = new MapFragment();
        Bundle args = new Bundle();
        args.putDouble(BundleKeys.LAT, lat);
        args.putDouble(BundleKeys.LON, lon);
        mMapFragment.setArguments(args);
        FragmentTransaction transaction = childFragmentManager().beginTransaction();
        transaction.add(R.id.lnrLytMap, mMapFragment, FragmentUtil.getTag(MapFragment.class)).commit();
        
        return v;
    }
    
    @Override
    public void onDestroyView() {
    	super.onDestroyView();
    	/**
    	 * 17-03-2015:
    	 * mMap instance is made null. So, that after the orientation change it can be reinitialized with the new Map 
    	 */
    	//mMap = null;
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
    	/**
    	 * 'mMap != null' check is added below as sometime the map comes out to be null
    	 *  if call is made from onAddressSearchCompleted
    	 */
    	if (mMap != null) {
    		mMap.clear();
	    	
	    	LatLng latLng = new LatLng(lat, lon);
	    	mMap.addMarker(new MarkerOptions().position(latLng).title(strAddress));
	    	
	    	mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 8));
    	}
    }
    
    private boolean setUpMapIfNeeded() {
    	boolean mapSetUp = true;
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
        	MapFragment mMapFragment = (MapFragment) childFragmentManager().findFragmentByTag(FragmentUtil.getTag(MapFragment.class));
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
    				((ChangeLocationFragmentTab) getParentFragment()).updateStrAddress(address);
    				
    			} else {
            		Log.w(TAG, "No relevant address found.");
    			}
    			
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
    		
    		// Alternative way to find String Address
    		if (addresses == null || addresses.isEmpty()) {
    			GeoUtil.getAddressFromLocation(lat, lon, (GeoUtilListener) getParentFragment());
    		}

            boolean mapSetUp = ((ChangeLocationFragmentTab) getParentFragment()).setUpMapIfNeeded();
            if (mapSetUp) {
            	 // The Map is verified. It is now safe to manipulate the map.
            	((ChangeLocationFragmentTab) getParentFragment()).setMarker(lat, lon);
            }
    	}
    }
    
    private void onAddressUpdated(Address address) {
    	updateStrAddress(address);
		lat = address.getLatitude();
		lon = address.getLongitude();
		
    	DeviceUtil.updateLatLon(lat, lon);
		
		setMarker(lat, lon);
    }

	@Override
	public boolean onQueryTextSubmit(String query) {
		isMyLocationClicked = false;
		Geocoder geocoder = new Geocoder(FragmentUtil.getActivity(this));
		List<Address> addresses = null;
		try {
			addresses = geocoder.getFromLocationName(query, 1);
			if (addresses != null && !addresses.isEmpty()) {
				Address address = addresses.get(0);
				onAddressUpdated(address);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Alternative way to find string address
		if (addresses == null || addresses.isEmpty()) {
			GeoUtil.getFromAddress(query, this);
		}
		
		//hideSoftKeypad();
		
		return true;
	}

	@Override
	public boolean onQueryTextChange(String newText) {
		return false;
	}

	@Override
	public void onAddressSearchCompleted(String address) {
		if (address != null && address.length() != 0) {
			strAddress = address;
			setMarker(lat, lon);
			if (isMyLocationClicked) {
				DeviceUtil.updateLatLon(lat, lon);
			}
		}
	}
	
	@Override
	public void onCitySearchCompleted(String city) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onLatlngSearchCompleted(Address address) {
		if (address != null) {
			onAddressUpdated(address);
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btnMyLocation:
			isMyLocationClicked = true;
			
			if (mMap != null) {
				AppConstants.lat = AppConstants.NOT_ALLOWED_LAT;
				AppConstants.lon = AppConstants.NOT_ALLOWED_LON;
				
				EventSeekr eventSeekr = FragmentUtil.getApplication(ChangeLocationFragmentTab.this);
				double latLon[] = DeviceUtil.getLatLon(eventSeekr);
				lat = latLon[0];
				lon = latLon[1];

				List<Address> addresses = null;
				Geocoder geocoder = new Geocoder(FragmentUtil.getActivity(ChangeLocationFragmentTab.this));
				try {
					addresses = geocoder.getFromLocation(lat, lon, 1);

					if (addresses != null && !addresses.isEmpty()) {
						Address address = addresses.get(0);
						ChangeLocationFragmentTab.this.updateStrAddress(address);
						onAddressUpdated(address);

					} else {
						Log.w(TAG, "No relevant address found.");
					}

				} catch (IOException e) {
					e.printStackTrace();
				}

				// Alternative way to find String Address
				if (addresses == null || addresses.isEmpty()) {
					GeoUtil.getAddressFromLocation(lat, lon, ChangeLocationFragmentTab.this);
				}
			}
			break;

		default:
			break;
		}
	}
}
