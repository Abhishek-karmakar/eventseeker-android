package com.wcities.eventseeker;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Toast;

import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.util.FragmentUtil;

public class NavigationFragmentTab extends Fragment implements OnClickListener {
	
	private static final String TAG = NavigationFragmentTab.class.getSimpleName();
	
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
		view.findViewById(R.id.imgMoovit).setOnClickListener(this);
		
		return view;
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
				String uri = "navicon://setPOI?ver=" + NavigationFragment.NAVICON_VERSION + "&ll=" + lat + "," + lon 
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
					uri = "market://details?id=" + NavigationFragment.NAVICON_PKG;
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
				
				intent = new Intent(FragmentUtil.getApplication(this), WebViewActivityTab.class);
				intent.putExtra(BundleKeys.URL, uri);
				startActivity(intent);
				
			} else {
				Toast.makeText(FragmentUtil.getActivity(this), R.string.address_isnt_available, 
					Toast.LENGTH_SHORT).show();
			}
			break;
			
		case R.id.imgMoovit:
			intent = FragmentUtil.getApplication(this).getPackageManager().getLaunchIntentForPackage("com.tranzmate");
			if (intent != null) {
				startActivity(intent);
				
			} else {
				intent = new Intent(Intent.ACTION_VIEW); 
				intent.setData(Uri.parse("market://details?id=com.tranzmate")); 
				startActivity(intent);
			}
			break; 

		default:
			break;
		}
	}
}
