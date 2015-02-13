package com.wcities.eventseeker;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Toast;

import com.wcities.eventseeker.analytics.IGoogleAnalyticsTracker;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.ReplaceFragmentListener;
import com.wcities.eventseeker.util.FragmentUtil;

public class NavigationFragment extends FragmentLoadableFromBackStack implements IGoogleAnalyticsTracker, OnClickListener {
	
	private static final String TAG = NavigationFragment.class.getSimpleName();
	
	// navicon
	private static final String NAVICON_VERSION = "1.4";
	private static final String NAVICON_PKG = "jp.co.denso.navicon.view";
	
	private Venue venue;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
		
		Bundle args = getArguments();		
		venue = (Venue) args.getSerializable(BundleKeys.VENUE);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		//Log.d(TAG, "onCreateView()");
		View view = inflater.inflate(R.layout.fragment_navigation, container, false);
		
		view.findViewById(R.id.imgGMaps).setOnClickListener(this);
		view.findViewById(R.id.imgNaviBridge).setOnClickListener(this);
		view.findViewById(R.id.imgScout).setOnClickListener(this);
		
		// add touch listener, otherwise touch events are rendered by previous screen in backstack (venue details)
		view.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return true;
			}
		});
		return view;
	}
	
	@Override
	public void onStart() {
		//Log.d(TAG, "onStart()");
		super.onStart();
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		ma.setToolbarBg(ma.getResources().getColor(R.color.colorPrimary));
		ma.setVStatusBarVisibility(View.VISIBLE, R.color.colorPrimaryDark);
		ma.setVStatusBarLayeredVisibility(View.GONE, AppConstants.INVALID_ID);
	}
	
	@Override
	public String getScreenName() {
		return "Navigation Selection Screen";
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.imgGMaps:
			Intent intent = null;
			double lat, lon;
			
			if (venue.getAddress() != null) {
				lat = venue.getAddress().getLat();
				lon = venue.getAddress().getLon();
				
				if (lat == 0 && lon == 0) {
					if (venue.getAddress().getAddress1() != null) {
						//Log.d(TAG, "fabNavigate address - " + venue.getAddress().getAddress1());
						intent = new Intent(android.content.Intent.ACTION_VIEW, 
							    Uri.parse("google.navigation:q=" + venue.getAddress().getAddress1()));
						
					} else if (venue.getAddress().getCity() != null) {
						//Log.d(TAG, "fabNavigate city - " + venue.getAddress().getCity());
						intent = new Intent(android.content.Intent.ACTION_VIEW, 
							    Uri.parse("google.navigation:q=" + venue.getAddress().getCity()));
						
					} else {
						//Log.d(TAG, "fabNavigate name - " + venue.getName());
						intent = new Intent(android.content.Intent.ACTION_VIEW, 
							    Uri.parse("google.navigation:q=" + venue.getName()));
					}
					
				} else {
					//Log.d(TAG, "fabNavigate lat, lon - " + lat + "," + lon);
					intent = new Intent(android.content.Intent.ACTION_VIEW, 
						    Uri.parse("google.navigation:q=" + lat + "," + lon));
				}
				
			} else {
				//Log.d(TAG, "fabNavigate name - " + venue.getName());
				intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" 
						+ venue.getName()));
			}
			
			intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
			startActivity(intent);
			
			break;
			
		case R.id.imgNaviBridge:
			if (venue.getAddress() != null) {
				lat = venue.getAddress().getLat();
				lon = venue.getAddress().getLon();
				String uri = "navicon://setPOI?ver=" + NAVICON_VERSION + "&ll=" + lat + "," + lon 
			    		+ "&appName=pb6Nlvh1&title=" + venue.getName() 
			    		+ "&radKM=15";
				if (venue.getPhone() != null) {
					uri = uri + "&tel=" + venue.getPhone();
				}
				uri += "&callURL=com.wcities.eventseekrapp://";
				intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(uri));
				try {
					startActivity(intent);
					
				} catch (ActivityNotFoundException e) {
					uri = "market://details?id=" + NAVICON_PKG;
					intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
					try {
						startActivity(intent);
						
					} catch (ActivityNotFoundException e1) {
						Toast.makeText(FragmentUtil.getApplication(this), R.string.error_this_action_couldnt_be_completed_at_this_time,
								Toast.LENGTH_SHORT).show();
					}
				}
				
			} else {
				Toast.makeText(FragmentUtil.getActivity(this), R.string.address_isnt_available, 
					Toast.LENGTH_SHORT).show();
			}
			break;
			
		case R.id.imgScout:
			if (venue.getAddress() != null) {
				lat = venue.getAddress().getLat();
				lon = venue.getAddress().getLon();
				String uri = "http://apps.scout.me/v1/driveto?dt=";
				if (venue.getAddress().getAddress1() != null) {
					uri += venue.getAddress().getAddress1() + "@";
				}
				uri += lat + ", " + lon + "&title=" + venue.getName() 
			    		+ "&token=6T5HI14ZzJdKRk-PUhWzT7Zn-enFiGsUYskrN5EnXENaQnBUE3GDalgi8SN0x2J4aTxvvZuTwDfGx9WHtdwmJeJpzFprUq79p4gf54Yiq9jM6wFwHaZSBp1k1AYtzdcfhlWvjLcKWCpqe9juykeaHSTsRr-cJde4uYeWGDSFerI";
				Bundle args = new Bundle();
				args.putString(BundleKeys.URL, uri);
				((ReplaceFragmentListener)FragmentUtil.getActivity(this)).replaceByFragment(
						AppConstants.FRAGMENT_TAG_WEB_VIEW, args);
				
			} else {
				Toast.makeText(FragmentUtil.getActivity(this), R.string.address_isnt_available, 
					Toast.LENGTH_SHORT).show();
			}
			break;

		default:
			break;
		}
	}
}
