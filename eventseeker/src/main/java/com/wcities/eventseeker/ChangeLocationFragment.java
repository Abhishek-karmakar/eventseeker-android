package com.wcities.eventseeker;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.GeoUtil;
import com.wcities.eventseeker.util.GeoUtil.GeoUtilListener;

public class ChangeLocationFragment extends FragmentLoadableFromBackStack implements OnQueryTextListener, 
		GeoUtilListener, OnClickListener {
	
	private static final String TAG = ChangeLocationFragment.class.getName();

	private static final String MAP_FRAGMENT_TAG = "mapFragment";

	private SearchView searchView;
	private GoogleMap mMap;
	private String strAddress = "";
	
	private double lat, lon;

	private boolean isMyLocationClicked;

	public interface ChangeLocationFragmentListener {
		public void onLocationChanged(Bundle args);
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (!(activity instanceof ChangeLocationFragmentListener)) {
            throw new ClassCastException(activity.toString() + " must implement ChangeLocationFragmentListener");
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		setRetainInstance(true);
		
		double[] latLon = DeviceUtil.getLatLon(FragmentUtil.getApplication(this));
		lat = latLon[0];
		lon = latLon[1];
	}
	
    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_change_location, null);
        
        v.findViewById(R.id.btnMyLocation).setOnClickListener(this);;
        
        MapFragment mMapFragment = new MapFragment();
        Bundle args = new Bundle();
        args.putDouble(BundleKeys.LAT, lat);
        args.putDouble(BundleKeys.LON, lon);
        mMapFragment.setArguments(args);
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.add(R.id.lnrLytMap, mMapFragment, MAP_FRAGMENT_TAG).commit();
        
        return v;
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    	inflater.inflate(R.menu.fragment_change_location, menu);
    	
		MenuItem searchItem = menu.findItem(R.id.action_search_view);
    	searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setQueryHint(getResources().getString(R.string.menu_search));
        searchView.setOnQueryTextListener(this);
        /**
         * 23-02-2015:
         * A bug was filed in the 'Edit' sheet mailed by Amir Sir on Feb 18. It says - 'When 
         * on the map, to change your location the keyboard pops up before you have time too 
         * look at map or set a marker. The keyboard should no pop up until search has been 
         * selected.'. So, to avoid expansion of SearchView on beginning the below line is commented
         */
        //MenuItemCompat.setOnActionExpandListener(searchItem, this);
        //MenuItemCompat.expandActionView(searchItem);
        
    	super.onCreateOptionsMenu(menu, inflater);
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
	    	
	    	mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
	    	mMap.animateCamera(CameraUpdateFactory.zoomTo(8), 2000, null);
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
    
    private void hideSoftKeypad() {
    	InputMethodManager imm = (InputMethodManager)FragmentUtil.getActivity(this).getSystemService(Context.INPUT_METHOD_SERVICE);
    	imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
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
    				((ChangeLocationFragment) getParentFragment()).updateStrAddress(address);
    				
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

            boolean mapSetUp = ((ChangeLocationFragment) getParentFragment()).setUpMapIfNeeded();
            Log.d(TAG, "map set up = " + mapSetUp);
            if (mapSetUp) {
            	 // The Map is verified. It is now safe to manipulate the map.
            	((ChangeLocationFragment) getParentFragment()).setMarker(lat, lon);
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
		//Log.i(TAG, "onQueryTextSubmit()");
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
		
		hideSoftKeypad();
		
		return true;
	}

	@Override
	public boolean onQueryTextChange(String newText) {
		// TODO Auto-generated method stub
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
	public String getScreenName() {
		return ScreenNames.CHANGE_LOCATION;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btnMyLocation:
			isMyLocationClicked = true;
			
			if (mMap != null) {
				/**
				 * Below lines are commented as it was giving error with:
				 * java.lang.IllegalStateException: MyLocation layer not enabled
				 * 
				 * 
				 */
				/*Location location = mMap.getMyLocation();
				if (location != null) {
					lat = location.getLatitude();
					lon = location.getLongitude();

				} else {*/
				AppConstants.lat = AppConstants.NOT_ALLOWED_LAT;
				AppConstants.lon = AppConstants.NOT_ALLOWED_LON;
				
				EventSeekr eventSeekr = FragmentUtil.getApplication(ChangeLocationFragment.this);
				double latLon[] = DeviceUtil.getLatLon(eventSeekr);
				lat = latLon[0];
				lon = latLon[1];
				//}
				List<Address> addresses = null;
				Geocoder geocoder = new Geocoder(FragmentUtil.getActivity(ChangeLocationFragment.this));
				try {
					addresses = geocoder.getFromLocation(lat, lon, 1);

					if (addresses != null && !addresses.isEmpty()) {
						Address address = addresses.get(0);
						ChangeLocationFragment.this.updateStrAddress(address);
						onAddressUpdated(address);

					} else {
						Log.w(TAG, "No relevant address found.");
					}

				} catch (IOException e) {
					e.printStackTrace();
				}

				// Alternative way to find String Address
				if (addresses == null || addresses.isEmpty()) {
					GeoUtil.getAddressFromLocation(lat, lon, ChangeLocationFragment.this);
				}
			}
			break;

		default:
			break;
		}
	}
}
