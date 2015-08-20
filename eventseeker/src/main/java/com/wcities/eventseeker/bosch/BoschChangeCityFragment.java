package com.wcities.eventseeker.bosch;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.bosch.myspin.serversdk.maps.MySpinCameraUpdateFactory;
import com.bosch.myspin.serversdk.maps.MySpinLatLng;
import com.bosch.myspin.serversdk.maps.MySpinMap;
import com.bosch.myspin.serversdk.maps.MySpinMapView;
import com.bosch.myspin.serversdk.maps.MySpinMapView.OnMapLeftListener;
import com.bosch.myspin.serversdk.maps.MySpinMapView.OnMapLoadedListener;
import com.bosch.myspin.serversdk.maps.MySpinMarkerOptions;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.bosch.BoschDrawerListFragment.BoschDrawerListFragmentListener;
import com.wcities.eventseeker.bosch.BoschMainActivity.OnCarStationaryStatusChangedListener;
import com.wcities.eventseeker.bosch.BoschMainActivity.OnKeyboardVisibilityStateChangedListener;
import com.wcities.eventseeker.bosch.custom.fragment.BoschFragmentLoadableFromBackStack;
import com.wcities.eventseeker.bosch.interfaces.BoschEditTextListener;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.GeoUtil;
import com.wcities.eventseeker.util.GeoUtil.GeoUtilListener;

import java.io.IOException;
import java.util.List;

public class BoschChangeCityFragment extends BoschFragmentLoadableFromBackStack implements OnClickListener, 
		GeoUtilListener, BoschEditTextListener, OnMapLeftListener, OnMapLoadedListener, OnCarStationaryStatusChangedListener,
		OnKeyboardVisibilityStateChangedListener {

	private static final String TAG = BoschChangeCityFragment.class.getSimpleName();

	private FrameLayout frmlytMap;
	private EditText edtCity;
	private MySpinMapView mMapView;
	private MySpinMap mMap;

	private String cityName;
	private String strAddress;
	private String strInputcity;
	
	private double latitude, longitude;

	private View vDummy;

	private Handler mHandler;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mHandler = new Handler(FragmentUtil.getActivity(this).getMainLooper());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_bosch_change_city, null);

		vDummy = view.findViewById(R.id.vDummy);
		
		view.findViewById(R.id.btnSearchCity).setOnClickListener(this);

		edtCity = (EditText) view.findViewById(R.id.edtSearchCity);
		edtCity.setOnEditorActionListener(new TextView.OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_SEARCH) {
					onSearchClicked();
					return true;
				}
				return false;
			}
		});
		
		frmlytMap = (FrameLayout) view.findViewById(R.id.frmlytMap);
		
		addNewMapView();
		
		updateColors();
		return view;
	}
	
	private void addNewMapView() {
		mMapView = new MySpinMapView(FragmentUtil.getActivity(this));
		mMapView.setOnMapLoadedListener(this);
		mMapView.setOnMapLeftListener(this);
		
		/**
		 * We are adding new MapWiew each and every time as in Bosch's MapView/Map instance there aren't any
		 * methods available to remove the previous markers. So, when we do addMarker() it will add a new 
		 * marker without removing the previous one. So, it will show that many markers as many time we will 
		 * search a new city.
		 */
		if (frmlytMap.getChildCount() > 0) {
			frmlytMap.removeAllViews();
		}
		frmlytMap.addView(mMapView);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		double[] latlon = DeviceUtil.getLatLon(FragmentUtil.getApplication(this));
		
		latitude = latlon[0];
		longitude = latlon[1];
	}
	
	@Override
	public void onStart() {
		super.onStart();
		mMapView.onStartTemporaryDetach();
	}
	
	@Override
 	public void onResume() {
		mMapView.onResume();
		cityName = EventSeekr.getCityName();
		if (cityName == null) {
			GeoUtil.getCityName(this, FragmentUtil.getActivity(this));
		}
		super.onResume(BoschMainActivity.INDEX_NAV_ITEM_CHANGE_CITY, buildTitle());
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

		if (latitude == 0 || longitude == 0) {
			double[] latlon = DeviceUtil.getLatLon(FragmentUtil.getApplication(this));
			
			latitude = latlon[0];
			longitude = latlon[1];
		}
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				setMarker(latitude, longitude);
			}
		}, 500);
		//setMarker(latitude, longitude);
	}

	private String buildTitle() {
		return (cityName == null || cityName.length() == 0) ? "Change City" : cityName + " - Change City";
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {

			case R.id.btnSearchCity:
				onSearchClicked();
				break;
		}
	}

	private void onSearchClicked() {
		String city = edtCity.getText().toString().trim();
		city = city.replace("\\n", "");
		if(city.equals("")) {
			return;
		}

		searchFor(city);
		edtCity.clearFocus();//This is just to hide the keyboard.
	}
	
	public boolean searchFor(String query) {
		//Log.i(TAG, "onQueryTextSubmit()");
		strInputcity=query;
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
		
		//TODO: if it is needed
		//hideSoftKeypad();
		
		return true;
	}
	
	private void onAddressUpdated(Address address) {
		String locality = address.getLocality();
		String subLocality = address.getSubLocality();
		/*Log.e(TAG, "address:max line index "+address.getMaxAddressLineIndex());
		Log.e(TAG,"getAdminArea::> "+address.getAdminArea());
		Log.e(TAG,"getSubAdminArea::> "+address.getSubAdminArea());
		Log.e(TAG,"getSubLocality::> "+address.getSubLocality());
		Log.e(TAG,"getLocality::> "+address.getLocality());
		Log.e(TAG,"getAddressLine(0)::> "+address.getAddressLine(0));
		Log.e(TAG,"getAddressLine(1)::>  "+address.getAddressLine(1));
		Log.e(TAG,"getCountryCode::>  "+address.getCountryCode());*/

		/*We are doing check for locality and sublocality so that if user input random text
		* Locality and SubLocality will come null then it will give user dialog of city not found*/
		if((locality != null) || (subLocality != null )) {
			updateStrAddress(address);

			latitude = address.getLatitude();
			longitude = address.getLongitude();
			DeviceUtil.updateLatLon(latitude, longitude);
			//setMarker(latitude, longitude);

			addNewMapView();

			GeoUtil.getCityName(this, FragmentUtil.getActivity(this));
		}else{
			((BoschMainActivity) FragmentUtil.getActivity(this)).showBoschDialog("City not found.");
		}
		
    }
	
	private void setMarker(double lat, double lon) {
    	if (mMap != null) {
    		MySpinLatLng mySpinLatLng;
	    	MySpinMarkerOptions mySpinMarkerOptions = new MySpinMarkerOptions();
			mySpinMarkerOptions.position(mySpinLatLng = new MySpinLatLng(lat, lon)).title(strAddress);
			
			mMap.addMarker(mySpinMarkerOptions);
			mMap.moveCamera(MySpinCameraUpdateFactory.newLatLng(mySpinLatLng));
	    	mMap.moveCamera(MySpinCameraUpdateFactory.zoomTo(12));
    	}
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
	
	@Override
	public void onAddressSearchCompleted(String strAddress) {}

	@Override
	public void onCitySearchCompleted(final String city) {
		FragmentUtil.getActivity(this).runOnUiThread(new Runnable() {
			@Override
			public void run() {				
				if (city != null && city.length() != 0) {
					cityName = city;
					((BoschMainActivity)FragmentUtil.getActivity(BoschChangeCityFragment.this))
						.updateTitleForFragment(buildTitle(), BoschChangeCityFragment.class.getSimpleName());
				}
			}
		});
	}

	@Override
	public void onLatlngSearchCompleted(Address address) {
		if (address != null) {
			onAddressUpdated(address);
		
		} else {
			((BoschMainActivity) FragmentUtil.getActivity(this)).showBoschDialog("City not found.");
		}
	}

	private void updateColors() {
		if (AppConstants.IS_NIGHT_MODE_ENABLED) {
			edtCity.setBackgroundResource(R.drawable.bg_edt_search_night_mode);
			edtCity.setTextColor(getResources().getColor(android.R.color.white));
			edtCity.setHintTextColor(getResources().getColor(android.R.color.white));
		
		} else {
			edtCity.setBackgroundResource(R.drawable.bg_edt_search);
			edtCity.setTextColor(getResources().getColor(R.color.eventseeker_bosch_theme_grey));			
			edtCity.setHintTextColor(getResources().getColor(R.color.eventseeker_bosch_theme_grey));			
		}
	}

	@Override
	public EditText getEditText() {
		return edtCity;
	}

	@Override
	public void onMapLeftListener(String arg0) {
		
	}
	
	@Override
	public void onCarStationaryStatusChanged(boolean isCarStationary) {
		if (!isCarStationary) {
			if (edtCity != null && edtCity.hasFocus()) {
				edtCity.clearFocus();
			}
			((BoschDrawerListFragmentListener)FragmentUtil.getActivity(this)).onDrawerItemSelected(
					BoschMainActivity.INDEX_NAV_ITEM_HOME);
			((BoschMainActivity) FragmentUtil.getActivity(this)).showBoschDialog(
					R.string.dialog_city_cannot_be_changed_while_driving);
		}
	}

	@Override
	public void onKeyboardVisibilityStateChanged(boolean isKeyboardVisible) {
		vDummy.setVisibility(isKeyboardVisible ? View.INVISIBLE : View.GONE);
	}

}