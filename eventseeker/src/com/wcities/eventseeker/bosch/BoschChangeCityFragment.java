package com.wcities.eventseeker.bosch;

import java.io.IOException;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.CityApi;
import com.wcities.eventseeker.core.CityPrefered;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.jsonparser.CityApiJSONParser;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class BoschChangeCityFragment extends FragmentLoadableFromBackStack implements OnClickListener, 
	OnItemClickListener {

	private static final String TAG = BoschChangeCityFragment.class.getName();
	
	private EditText edtCity;
	private View prgSearchCity;
	
	private CitiesAdapter adapter;

	private double latitiude, longitude;
	private String cityName;

	private ListView lstCity;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		adapter = new CitiesAdapter(null, FragmentUtil.getActivity(BoschChangeCityFragment.this));
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.bosch_fragment_change_city, null);

		view.findViewById(R.id.btnSearchCity).setOnClickListener(this);
		view.findViewById(R.id.btnNearbyCities).setOnClickListener(this);

		edtCity = (EditText) view.findViewById(R.id.edtSearchCity);

		prgSearchCity = view.findViewById(R.id.prgSearchCity);
		prgSearchCity.setVisibility(View.GONE);
		
		view.findViewById(R.id.btnUp).setOnClickListener(this);
		view.findViewById(R.id.btnDown).setOnClickListener(this);
		
		lstCity = (ListView) view.findViewById(R.id.lstCities);
		lstCity.setOnItemClickListener(this);
		
		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		lstCity.setAdapter(adapter);
		
		double[] latlon = DeviceUtil.getLatLon(FragmentUtil.getActivity(this));
		
		latitiude = latlon[0];
		longitude = latlon[1];
	}
	
	@Override
	public void onResume() {
		super.onResume();
		setCityName(((BOSCHMainActivity) FragmentUtil.getActivity(this)).getCityName());
	}

	private void setCityName(String cityName) {
		BOSCHMainActivity activity = (BOSCHMainActivity) FragmentUtil.getActivity(this);
		activity.onFragmentResumed(this, BOSCHMainActivity.INDEX_NAV_ITEM_CHANGE_CITY ,
				cityName + " - Change City");		
	}

	@Override
	public void onItemClick(AdapterView<?> adpV, View v, int position, long arg) {
		CityPrefered cityPrefered = (CityPrefered) adapter.getItem(position);
		
		latitiude = cityPrefered.getLatitude();
		longitude = cityPrefered.getLongitude();
		
		cityName = cityPrefered.getCityName();
		setCityName(cityName);
		
		DeviceUtil.updateLatLon(cityPrefered.getLatitude(), cityPrefered.getLongitude());
		
		adapter.setData(null);
		adapter.notifyDataSetChanged();
	}
	
	@Override
	public void onClick(View v) {
		
		
		switch (v.getId()) {

			case R.id.btnUp:
				lstCity.smoothScrollByOffset(-1);
				break;
			case R.id.btnDown:
				lstCity.smoothScrollByOffset(1);
				break;
		
			case R.id.btnSearchCity:
				onSearchClicked();
				break;

			case R.id.btnNearbyCities:
				onNearByCitiesClicked();
				break;
		}
	}

	private void onNearByCitiesClicked() {

		adapter.setData(null);
		adapter.notifyDataSetChanged();
		
		LoadCities loadCities = new LoadCities(latitiude, longitude);
		AsyncTaskUtil.executeAsyncTask(loadCities, true, LoadCities.GET_NEARBY_CITIES);		
	
	}

	private void onSearchClicked() {

		adapter.setData(null);
		adapter.notifyDataSetChanged();
		
		String city = edtCity.getText().toString().trim();
		city = city.replace("\\n", "");
		if(city.equals("")) {
			return;
		}

		LoadCities loadCities = new LoadCities(city);
		AsyncTaskUtil.executeAsyncTask(loadCities, true, LoadCities.GET_CITIES);
	
	}
	
	private class LoadCities extends AsyncTask<Integer, Void, List<CityPrefered>> {

		private final String TAG = LoadCities.class.getName();

		private static final int GET_CITIES= 0;
		private static final int GET_NEARBY_CITIES= 1;
		
		private String city;
		
		private double lon, lat;

		public LoadCities(String city) {
			this.city = city;
		}

		public LoadCities(double lat, double lon) {
			this.lat = lat;
			this.lon = lon;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			prgSearchCity.setVisibility(View.VISIBLE);
		}
		
		@Override
		protected List<CityPrefered> doInBackground(Integer... params) {
			
			int selectedApiCall = params[0];
			
			try {
				
				CityApi api = new CityApi(Api.OAUTH_TOKEN);
				JSONObject jsonObject = null;
				
				switch (selectedApiCall) {
				
				case GET_CITIES :
					jsonObject = api.getCities(city);
					break;
				
				case GET_NEARBY_CITIES :
					jsonObject = api.getNearbyCities(lat, lon);
					break;
				
				}
				
				CityApiJSONParser jsonParser = new CityApiJSONParser();
				return jsonParser.parseCities(FragmentUtil.getActivity(BoschChangeCityFragment.this), jsonObject);
			
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
		protected void onPostExecute(List<CityPrefered> result) {
			super.onPostExecute(result);

			try {
				if (isAdded()) {
					prgSearchCity.setVisibility(View.GONE);
					
					if (result != null) {
						adapter.setData(result);
						adapter.notifyDataSetChanged();
					} else {
						Toast.makeText(FragmentUtil.getActivity(BoschChangeCityFragment.this),
							"Couldn't locate the City.", Toast.LENGTH_LONG).show();
					}
				}
			} catch (Exception e) {
				Log.e(TAG, "ERROR : " + e.toString());
			}
		}
	}
	
	private static class CitiesAdapter extends BaseAdapter {
		
		private List<CityPrefered> lstCities;
		private LayoutInflater inflater;
		
		public CitiesAdapter(List<CityPrefered> lstCities, Context ctx) {
			this.lstCities = lstCities;
			inflater = LayoutInflater.from(ctx);
		}

		@Override
		public int getCount() {
			if (lstCities == null) {
				return 0;
			}
			return lstCities.size();
		}

		@Override
		public Object getItem(int position) {
			return lstCities.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			convertView = inflater.inflate(R.layout.bosch_list_item_cities, null);
			
			TextView txtCity = (TextView) convertView.findViewById(R.id.txtCity);

			CityPrefered cityPrefered = lstCities.get(position);
			txtCity.setText(cityPrefered.getCityName() + ", " + cityPrefered.getCountryName());
			
			return convertView;
		}

		public void setData(List<CityPrefered> lstCities) {
			this.lstCities = lstCities;
		}

	}

}