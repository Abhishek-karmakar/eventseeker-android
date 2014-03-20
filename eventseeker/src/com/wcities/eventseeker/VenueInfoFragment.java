package com.wcities.eventseeker;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bosch.myspin.serversdk.IPhoneCallStateListener;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.RecordApi;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.asynctask.AsyncLoadImg.AsyncLoadImageListener;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Address;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.jsonparser.RecordApiJSONParser;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class VenueInfoFragment extends Fragment implements OnClickListener, AsyncLoadImageListener {
	
	private static final String TAG = VenueInfoFragment.class.getName();
	
	private static final int MAX_LINES_VENUE_DESC_PORTRAIT = 3;
	private static final int MAX_LINES_VENUE_DESC_LANDSCAPE = 5;
	
	private ProgressBar progressBar;
	private Venue venue;
	private boolean isVenueDescExpanded, allDetailsLoaded;
	private int orientation;
	private LoadVenue loadVenue;
	
	private RelativeLayout rltLayoutDesc, rltLayoutLoadedContent;
	private TextView txtDesc, txtAddress;
	private ImageView imgDown;
	private ImageView imgItem;

	private boolean isTablet;

	private TextView txtVenueAddress;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (venue == null) {
			venue = (Venue) getArguments().getSerializable(BundleKeys.VENUE);
			loadVenue = new LoadVenue();
			AsyncTaskUtil.executeAsyncTask(loadVenue, true);
		}
		
		isTablet = ((MainActivity)FragmentUtil.getActivity(this)).isTablet();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		orientation = getResources().getConfiguration().orientation;

		View v = inflater.inflate(R.layout.fragment_venue_info, null);
		
		progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
		rltLayoutLoadedContent = (RelativeLayout) v.findViewById(R.id.rltLayoutLoadedContent);
		imgItem = (ImageView) v.findViewById(R.id.imgItem);
		
		boolean isImgLoaded = updateImgVisibility();
		if (!isImgLoaded) {
			Log.d(TAG, "!isImgLoaded");
			rltLayoutLoadedContent.setVisibility(View.GONE);
			progressBar.setVisibility(View.VISIBLE);
		}
		
		((TextView)v.findViewById(R.id.txtItemTitle)).setText(venue.getName());
		
		rltLayoutDesc = (RelativeLayout) v.findViewById(R.id.rltLayoutDesc);
		txtDesc = (TextView) v.findViewById(R.id.txtDesc);
		imgDown = (ImageView) v.findViewById(R.id.imgDown);
		
		if(isTablet) {
			imgDown.setVisibility(View.GONE);
			isVenueDescExpanded = true;
		}
		
		updateDescVisibility();
		
		txtAddress = (TextView) v.findViewById(R.id.txtAddress);
		if(isTablet) {
			txtVenueAddress = (TextView) v.findViewById(R.id.txtVenueAddress);
		}
		updateAddressTxt();
		
		
		AddressMapFragment fragment = (AddressMapFragment) getChildFragmentManager().findFragmentByTag(
				AppConstants.FRAGMENT_TAG_ADDRESS_MAP);
        if (fragment == null) {
        	addAddressMapFragment();
        }
        
        v.findViewById(R.id.btnPhone).setOnClickListener(this);
        v.findViewById(R.id.btnWeb).setOnClickListener(this);
        v.findViewById(R.id.btnDrive).setOnClickListener(this);
		
		return v;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (loadVenue != null && loadVenue.getStatus() != Status.FINISHED) {
			loadVenue.cancel(true);
		}
	}
	
	private void updateAddressTxt() {
		String address = "";
		if (venue.getAddress() != null) {
			if (venue.getAddress().getAddress1() != null) {
				address += venue.getAddress().getAddress1();
			}
			
			if (venue.getAddress().getCity() != null) {
				if (address.length() != 0) {
					address += ", ";
				}
				address += venue.getAddress().getCity();
			} 
			
			if (venue.getAddress().getCountry() != null) {
				if (address.length() != 0) {
					address += ", ";
				}
				address += venue.getAddress().getCountry().getName();
			} 
		}
		txtAddress.setText(address);
		if(isTablet) {
			txtVenueAddress.setText(address);
		}

	}
	
	private void addAddressMapFragment() {
    	FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        
        AddressMapFragment fragment = new AddressMapFragment();
        fragment.setArguments(getArguments());
        fragmentTransaction.add(R.id.frmLayoutMapContainer, fragment, AppConstants.FRAGMENT_TAG_ADDRESS_MAP);
        fragmentTransaction.commit();
    } 
	
	private boolean updateImgVisibility() {
		if (venue.doesValidImgUrlExist() || allDetailsLoaded) {
			String key = venue.getKey(ImgResolution.LOW);
	        BitmapCache bitmapCache = BitmapCache.getInstance();
			Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
			if (bitmap != null) {
		        imgItem.setImageBitmap(bitmap);
		        return true;
		        
		    } else {
		    	imgItem.setImageBitmap(null);
		    	AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
		        asyncLoadImg.loadImg(imgItem, ImgResolution.LOW, venue, this);
		        return false;
		    }
			
		} else {
			return false;
		}
	}
	
	private void updateDescVisibility() {
		if (venue.getLongDesc() == null) {
			rltLayoutDesc.setVisibility(View.GONE);
			
		} else {
			rltLayoutDesc.setVisibility(View.VISIBLE);
			imgDown.setOnClickListener(this);

			txtDesc.setText(Html.fromHtml(venue.getLongDesc()));

			if (isVenueDescExpanded) {
				expandDesc();
				
			} else {
				collapseDesc();
			}
		}
	}
	
	private void expandDesc() {
		txtDesc.setMaxLines(Integer.MAX_VALUE);
		txtDesc.setEllipsize(null);
		
		imgDown.setImageDrawable(getResources().getDrawable(R.drawable.less));

		isVenueDescExpanded = true;
	}
	
	private void collapseDesc() {
		int maxLines = orientation == Configuration.ORIENTATION_PORTRAIT ? MAX_LINES_VENUE_DESC_PORTRAIT : 
			MAX_LINES_VENUE_DESC_LANDSCAPE;
		txtDesc.setMaxLines(maxLines);
		txtDesc.setEllipsize(TruncateAt.END);
		
		imgDown.setImageDrawable(getResources().getDrawable(R.drawable.down));
		
		isVenueDescExpanded = false;
	}
	
	private void updateAddress() {
		updateAddressTxt();
		
		Address address = venue.getAddress();
		AddressMapFragment fragment = (AddressMapFragment) getChildFragmentManager().findFragmentByTag(
				AppConstants.FRAGMENT_TAG_ADDRESS_MAP);
        if (address != null && fragment != null) {
        	if (address.getLat() != 0 && address.getLon() != 0) {
            	fragment.updateLatLon(address.getLat(), address.getLon());
            	
        	} else {
        		fragment.updateAddress(address);
        	}
        }
	}
	
	protected void onDriveClicked() {
		AddressMapFragment fragment = (AddressMapFragment) getChildFragmentManager().findFragmentByTag(
				AppConstants.FRAGMENT_TAG_ADDRESS_MAP);
        if (fragment != null) {
        	fragment.displayDrivingDirection();
        }
	}
	
	private class LoadVenue extends AsyncTask<String, Void, Void> {
		
		@Override
		protected Void doInBackground(String... params) {
			RecordApi recordApi = new RecordApi(Api.OAUTH_TOKEN, venue.getId());

			try {
				JSONObject jsonObject = recordApi.getRecords();
				RecordApiJSONParser jsonParser = new RecordApiJSONParser();
				jsonParser.fillVenueDetails(jsonObject, venue);
				
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				
			} catch (IOException e) {
				e.printStackTrace();
				
			} catch (JSONException e) {
				e.printStackTrace();
			}

			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			allDetailsLoaded = true;
			
			updateImgVisibility();
			updateDescVisibility();
			updateAddress();
			((VenueDetailsFragment)getParentFragment()).updateShareIntent();
		}    	
    }

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.imgDown:
			if (isVenueDescExpanded) {
				collapseDesc();
				
			} else {
				expandDesc();
			}
			break;
			
		case R.id.btnPhone:
			if (venue.getPhone() != null) {
				Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + venue.getPhone()));
				startActivity(Intent.createChooser(intent, "Call..."));
				
			} else {
				Toast.makeText(FragmentUtil.getActivity(this), R.string.phone_number_not_available, 
						Toast.LENGTH_SHORT).show();
			}
			break;
			
		case R.id.btnWeb:
			if (venue.getUrl() != null) {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(venue.getUrl()));
				startActivity(intent);
				
			} else {
				Toast.makeText(FragmentUtil.getActivity(this), "Web address is not available for this venue.", 
						Toast.LENGTH_SHORT).show();
			}
			break;
			
		case R.id.btnDrive:
			onDriveClicked();
			break;
			
		default:
			break;
		}
	}

	@Override
	public void onImageLoaded() {
		//Log.d(TAG, "onImageLoaded()");
		rltLayoutLoadedContent.setVisibility(View.VISIBLE);
		progressBar.setVisibility(View.GONE);
	}

	@Override
	public void onImageCouldNotBeLoaded() {
		rltLayoutLoadedContent.setVisibility(View.VISIBLE);
		progressBar.setVisibility(View.GONE);
	}
}
