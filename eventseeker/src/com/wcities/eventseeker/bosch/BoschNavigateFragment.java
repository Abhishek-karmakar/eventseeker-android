package com.wcities.eventseeker.bosch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;

import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bosch.myspin.serversdk.maps.MySpinBitmapDescriptor;
import com.bosch.myspin.serversdk.maps.MySpinBitmapDescriptorFactory;
import com.bosch.myspin.serversdk.maps.MySpinCameraUpdateFactory;
import com.bosch.myspin.serversdk.maps.MySpinLatLng;
import com.bosch.myspin.serversdk.maps.MySpinMap;
import com.bosch.myspin.serversdk.maps.MySpinMapView;
import com.bosch.myspin.serversdk.maps.MySpinMapView.OnMapLeftListener;
import com.bosch.myspin.serversdk.maps.MySpinMapView.OnMapLoadedListener;
import com.bosch.myspin.serversdk.maps.MySpinMarkerOptions;
import com.bosch.myspin.serversdk.maps.MySpinPolylineOptions;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.map.GMapV2Direction;
import com.wcities.eventseeker.map.MySpinGMapV3Direction;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.GeoUtil;
import com.wcities.eventseeker.util.GeoUtil.GeoUtilListener;

public class BoschNavigateFragment extends FragmentLoadableFromBackStack implements OnMapLoadedListener, 
	OnMapLeftListener, GeoUtilListener {

	private static final String TAG = BoschNavigateFragment.class.getName();
	
	private MySpinMapView mMapView;

	private MySpinMap mMap;
	
	private double venueLon, venueLat, currentLat, currentLon;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		
		Venue venue = (Venue) getArguments().getSerializable(BundleKeys.VENUE);
		
		venueLat = venue.getAddress().getLat();
		venueLon = venue.getAddress().getLon();
	
		double[] latLon = DeviceUtil.getLatLon(FragmentUtil.getActivity(this));
		currentLat = latLon[0];
		currentLon = latLon[1];
		
		com.wcities.eventseeker.core.Address address = venue.getAddress();
		if (address != null) {
			venueLat = address.getLat();
			venueLon = address.getLon();
			
			if (venueLat == 0 && venueLon == 0) {
				findLatLonFromAddress(address);
			}
			
		} else {
			findLatLonFromName(venue);
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mMapView = new MySpinMapView(FragmentUtil.getActivity(this));
		mMapView.setOnMapLoadedListener(this);
		mMapView.setOnMapLeftListener(this);
		return mMapView;
	}

	@Override
	public void onStart() {
		super.onStart();
		mMapView.onStartTemporaryDetach();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		mMapView.onResume();
		super.onResume(AppConstants.INVALID_INDEX, getResources().getString(R.string.title_map));
	}
	
	@Override
	public void onPause() {
		super.onPause();
		mMapView.onPause();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		mMapView.onDestroy();
	}

	@Override
	public void onMapLoadedListener() {
		mMap = mMapView.getMap();
		
		drawDrivingRoute();
	}

	private void findLatLonFromName(Venue venue) {
		List<Address> addresses = null;
		
		Geocoder geocoder = new Geocoder(FragmentUtil.getActivity(this));
		try {
			addresses = geocoder.getFromLocationName(venue.getName(), 1);
			if (addresses != null && !addresses.isEmpty()) {
				Address address = addresses.get(0);
				venueLat = address.getLatitude();
				venueLon = address.getLongitude();
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
			//Log.d(TAG, "findLatLonFromAddress(str), addresses != null");
			
			Address address = addresses.get(0);
			venueLat = address.getLatitude();
			venueLon = address.getLongitude();

			return true;
		}
		//Log.d(TAG, "findLatLonFromAddress(str) done");
		return false;
	}

	private void drawDrivingRoute() {
		if (currentLat != AppConstants.NOT_ALLOWED_LAT && currentLon != AppConstants.NOT_ALLOWED_LON && 
			venueLat != 0 && venueLon != 0 &&	mMap != null) {

			/*MySpinBitmapDescriptor bitmapDescriptor = MySpinBitmapDescriptorFactory.fromResource("ic_destination");

			MySpinMarkerOptions mySpinMarkerOptions = new MySpinMarkerOptions();
			mySpinMarkerOptions.position(new MySpinLatLng(venueLat, venueLon));
			mySpinMarkerOptions.icon(bitmapDescriptor);
			mMap.addMarker(mySpinMarkerOptions);

			bitmapDescriptor = MySpinBitmapDescriptorFactory.fromResource("ic_source");

			mySpinMarkerOptions = new MySpinMarkerOptions();
			mySpinMarkerOptions.position(new MySpinLatLng(currentLat, currentLon));
			mySpinMarkerOptions.icon(bitmapDescriptor);
			mMap.addMarker(mySpinMarkerOptions);*/

			new GetDrivingDirection(currentLat, currentLon, venueLat, venueLon).execute();
		}
	}
	
	 private class GetDrivingDirection extends AsyncTask<Void, Void, MySpinPolylineOptions> {
			private double currentLat;
			private double currentLon;
			private double lon;
			private double lat;
			
			public GetDrivingDirection(double currLat, double currLon, double destLat, double destLon) {
				currentLat = currLat;
				currentLon = currLon;
				lat = destLat;
				lon = destLon;
			}

			@Override
			protected MySpinPolylineOptions doInBackground(Void... params) {
				MySpinLatLng fromPosition = new MySpinLatLng(currentLat, currentLon);
				MySpinLatLng toPosition = new MySpinLatLng(lat, lon);

				MySpinGMapV3Direction md = new MySpinGMapV3Direction();

				Document doc = md.getDocument(fromPosition, toPosition, GMapV2Direction.MODE_DRIVING);
				ArrayList<MySpinLatLng> directionPoint = md.getDirection(doc);
				MySpinPolylineOptions polylineOptions = new MySpinPolylineOptions().width(2).color(Color.BLUE);
				
				for (int i = 0 ; i < directionPoint.size() ; i++) {          
					polylineOptions.add(directionPoint.get(i));
				}
				return polylineOptions;
			}
			
			@Override
			protected void onPostExecute(MySpinPolylineOptions result) {
				mMap.addPolyline(result);
				fixZoom(result);
			}
			
			private void fixZoom(MySpinPolylineOptions polylineOptions) {
			    List<MySpinLatLng> points = polylineOptions.getPoints(); // route is instance of PolylineOptions 

			    if (points.isEmpty()) {
			    	((BoschMainActivity)FragmentUtil.getActivity(BoschNavigateFragment.this)).showBoschDialog(
			    			"Could not find the driving direction for this venue.");
			    	
			    } else {
			    	mMap.moveCamera(MySpinCameraUpdateFactory.newLatLng(points.get(0)));
<<<<<<< HEAD
			    	mMap.moveCamera(MySpinCameraUpdateFactory.zoomTo(12));
			    	
					MySpinBitmapDescriptor bitmapDescriptor = MySpinBitmapDescriptorFactory.fromResource("ic_des");
					
					MySpinMarkerOptions mySpinMarkerOptions = new MySpinMarkerOptions();
					mySpinMarkerOptions.position(new MySpinLatLng(venueLat, venueLon));
					mySpinMarkerOptions.icon(bitmapDescriptor);
					mMap.addMarker(mySpinMarkerOptions);

					bitmapDescriptor = MySpinBitmapDescriptorFactory.fromResource("ic_src");
					
					mySpinMarkerOptions = new MySpinMarkerOptions();
					mySpinMarkerOptions.position(new MySpinLatLng(currentLat, currentLon));
					mySpinMarkerOptions.icon(bitmapDescriptor);
					mMap.addMarker(mySpinMarkerOptions);
=======
			    	mMap.moveCamera(MySpinCameraUpdateFactory.zoomTo(9));
>>>>>>> bc7e79614441f30f3aa7d9d280efa87a6a3aff6e
			    }
			}
		}


	@Override
	public void onMapLeftListener(String arg0) {}

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

	public void updateLatLon(double lat, double lon) {
		//Log.d(TAG, "updateLatLon(), lat = " + lat + ", lon = " + lon);
		venueLat = lat;
		venueLon = lon;
		
		drawDrivingRoute();
	}
	
}
