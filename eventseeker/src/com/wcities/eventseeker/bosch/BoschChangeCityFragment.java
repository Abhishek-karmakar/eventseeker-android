package com.wcities.eventseeker.bosch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.CityApi;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.bosch.BoschMainActivity.OnDisplayModeChangedListener;
import com.wcities.eventseeker.bosch.custom.fragment.BoschFragmentLoadableFromBackStack;
import com.wcities.eventseeker.bosch.interfaces.BoschEditTextListener;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.CityPrefered;
import com.wcities.eventseeker.jsonparser.CityApiJSONParser;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.GeoUtil;
import com.wcities.eventseeker.util.GeoUtil.GeoUtilListener;

public class BoschChangeCityFragment extends BoschFragmentLoadableFromBackStack implements OnClickListener, 
		OnItemClickListener, GeoUtilListener, OnDisplayModeChangedListener, BoschEditTextListener {

	private static final String TAG = BoschChangeCityFragment.class.getSimpleName();
	
	private EditText edtCity;
	private View prgSearchCity;
	
	private CitiesAdapter adapter;

	private double latitiude, longitude;

	private ListView lstCity;

	private String cityName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		adapter = new CitiesAdapter(null, FragmentUtil.getActivity(BoschChangeCityFragment.this));
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_bosch_change_city, null);

		view.findViewById(R.id.btnSearchCity).setOnClickListener(this);
		view.findViewById(R.id.btnNearbyCities).setOnClickListener(this);

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

		prgSearchCity = view.findViewById(R.id.prgSearchCity);
		prgSearchCity.setVisibility(View.GONE);
		
		view.findViewById(R.id.btnUp).setOnClickListener(this);
		view.findViewById(R.id.btnDown).setOnClickListener(this);
		
		lstCity = (ListView) view.findViewById(R.id.lstCities);
		lstCity.setOnItemClickListener(this);
		
		updateColors();
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
		cityName = EventSeekr.getCityName();
		if (cityName == null) {
			GeoUtil.getCityName(this, FragmentUtil.getActivity(this));
		}
		super.onResume(BoschMainActivity.INDEX_NAV_ITEM_CHANGE_CITY, buildTitle());
	}

	private String buildTitle() {
		return (cityName == null || cityName.length() == 0) ? "Change City" : cityName + " - Change City";
	}

	@Override
	public void onItemClick(AdapterView<?> adpV, View v, int position, long arg) {
		CityPrefered cityPrefered = (CityPrefered) adapter.getItem(position);

		if (cityPrefered.getCityName() != null) {
			latitiude = cityPrefered.getLatitude();
			longitude = cityPrefered.getLongitude();
			
			cityName = cityPrefered.getCityName();
			((BoschMainActivity)FragmentUtil.getActivity(this)).updateTitleForFragment(buildTitle(), 
					getClass().getSimpleName());
			
			DeviceUtil.setCitySet(true);
			EventSeekr.setCityName(cityName);
			
			DeviceUtil.unregisterLocationListener();
			DeviceUtil.updateLatLon(cityPrefered.getLatitude(), cityPrefered.getLongitude());
			
			adapter.setData(null);
			adapter.notifyDataSetChanged();
		}
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
			
			prgSearchCity.setVisibility(View.GONE);
			
			if (result == null) {
				CityPrefered cityPrefered = new CityPrefered(null, null, 
						AppConstants.NOT_ALLOWED_LAT, AppConstants.NOT_ALLOWED_LON);
				
				result = new ArrayList<CityPrefered>();
				result.add(cityPrefered);

				Toast.makeText(FragmentUtil.getActivity(BoschChangeCityFragment.this)
						, "Couldn't locate the City.", Toast.LENGTH_LONG).show();
			}
			adapter.setData(result);
			adapter.notifyDataSetChanged();
		}
	}
	
	private static class CitiesAdapter extends BaseAdapter {
		
		private List<CityPrefered> lstCities;
		private LayoutInflater inflater;
		private Resources res;
		
		public CitiesAdapter(List<CityPrefered> lstCities, Context ctx) {
			this.lstCities = lstCities;
			inflater = LayoutInflater.from(ctx);
			res = ctx.getResources();
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
			
			RelativeLayout rltRootLyt = (RelativeLayout) convertView.findViewById(R.id.rltRootLyt);
			
			TextView txtCity = (TextView) convertView.findViewById(R.id.txtCity);

			int txtClrResId = -1;
			Drawable txtDrwble;
			if (AppConstants.IS_NIGHT_MODE_ENABLED) {
				rltRootLyt.setBackgroundResource(
						R.drawable.slctr_item_lst_cities_fragment_change_city_night_mode);
				
				txtClrResId = R.color.slctr_black_on_white_off;
				txtDrwble = res.getDrawable(R.drawable.slctr_txt_city_fragment_change_city_night_mode);				
			} else {
				rltRootLyt.setBackgroundResource(R.drawable.slctr_item_lst_cities_fragment_change_city);				
				
				txtClrResId = R.color.slctr_white_on_black_off;
				txtDrwble = res.getDrawable(R.drawable.slctr_txt_city_fragment_change_city);				
			}
			
			txtCity.setCompoundDrawablesWithIntrinsicBounds(txtDrwble, null, null, null);
			
			try {
				XmlResourceParser parser = res.getXml(txtClrResId);
				ColorStateList colors = ColorStateList.createFromXml(res, parser);
				txtCity.setTextColor(colors);
			} catch (Exception e) {}

			CityPrefered cityPrefered = lstCities.get(position);

			if (cityPrefered.getCityName() == null) {
				txtCity.setText("No City Found");
				
			} else {
				txtCity.setText(cityPrefered.getCityName() + ", " + cityPrefered.getCountryName());
			
			}
			return convertView;
		}

		public void setData(List<CityPrefered> lstCities) {
			this.lstCities = lstCities;
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
	public void onLatlngSearchCompleted(Address address) {}

	@Override
	public void onDisplayModeChanged(boolean isNightModeEnabled) {
		updateColors();
		adapter.notifyDataSetChanged();
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

}