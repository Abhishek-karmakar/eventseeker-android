package com.wcities.eventseeker;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import us.feras.ecogallery.EcoGallery;
import us.feras.ecogallery.EcoGalleryAdapterView;
import us.feras.ecogallery.EcoGalleryAdapterView.OnItemClickListener;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.interfaces.EventListener;
import com.wcities.eventseeker.util.FragmentUtil;

public class DiscoverFragmentTab extends DiscoverParentFragment implements OnItemClickListener {

	public static final String TAG = DiscoverFragment.class.getName();

	private static final int DEFAULT_SELECTED_FEATURED_EVENT_POSITION = 2;

	private EcoGallery ecoGallery;
	private FeaturedEventsEcoGalleryAdapter featuredEventsEcoGalleryAdapter;

	public String cityName;
	public TextView txtCityName;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		View v = super.onCreateView(inflater, container, savedInstanceState);

		featuredEventsEcoGalleryAdapter = new FeaturedEventsEcoGalleryAdapter(this, FragmentUtil.getActivity(this));

		ecoGallery = (EcoGallery) v.findViewById(R.id.ecoglryFeaturedEvt);
		ecoGallery.setAdapter(featuredEventsEcoGalleryAdapter);
		ecoGallery.setOnItemClickListener(this);

		if(featuredEventsEcoGalleryAdapter.getCount() > DEFAULT_SELECTED_FEATURED_EVENT_POSITION) {
			ecoGallery.setSelection(DEFAULT_SELECTED_FEATURED_EVENT_POSITION);
		}

		return v;
	}

	@Override
	public void onDestroyView() {
		//if (ecoGallery != null) {
			// viewPagerSelectedPos = ecoGallery.getCurrentItem();
		//}
		super.onDestroyView();
		ActionBar actionBar = ((ActionBarActivity)FragmentUtil.getActivity(this)).getSupportActionBar();
		actionBar.setCustomView(null);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		createCustomActionBar();
	}		

	private void createCustomActionBar() {
		Log.i(TAG, "createCustomActionBar");
		Log.i(TAG, "txtCityName : " + txtCityName);
		MainActivity activity = (MainActivity) FragmentUtil.getActivity(this);
		ActionBar actionBar = activity.getSupportActionBar();

		ActionBar.LayoutParams params = new ActionBar.LayoutParams(
				ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.MATCH_PARENT);
		
		params.gravity = Gravity.CENTER;
		
		LayoutInflater lytInflater = (LayoutInflater) activity.getSystemService(activity.LAYOUT_INFLATER_SERVICE);
		View vActionBar = lytInflater.inflate(R.layout.action_bar_custom_view_item, null);
		
		txtCityName = (TextView) vActionBar.findViewById(R.id.txtCityName);

		actionBar.setDisplayShowCustomEnabled(true);
		if(((EventSeekr)FragmentUtil.getActivity(this).getApplicationContext()).isTabletAndInLandscapeMode()) {
			actionBar.setIcon(R.drawable.placeholder);
		}
		actionBar.setCustomView(vActionBar, params);
		
		updateCity();
	}
	
	public void updateCity() {
		Log.i(TAG, "City name : " + cityName);
		if(txtCityName != null && cityName != null) {
			txtCityName.setText(cityName);
		}
		
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
	}

	private static class FeaturedEventsEcoGalleryAdapter extends BaseAdapter {

		private WeakReference<DiscoverFragmentTab> discoverFragment;

		private static final String TAG_PROGRESS_INDICATOR = "progressIndicator";
		private static final String TAG_CONTENT = "content";

		private LayoutInflater mInflater;
		private BitmapCache bitmapCache;

		public FeaturedEventsEcoGalleryAdapter(DiscoverFragmentTab discoverFragment, Context context) {
			this.discoverFragment = new WeakReference<DiscoverFragmentTab>(discoverFragment);
			mInflater = LayoutInflater.from(context);
			bitmapCache = BitmapCache.getInstance();

		}

		@Override
		public Event getItem(int position) {
			return discoverFragment.get().featuredEvts.get(position);
		}

		@Override
		public int getCount() {
			// Log.i(TAG, "count = " +
			// discoverActivity.get().featuredEvts.size());
			return discoverFragment.get().featuredEvts.size();
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Log.i(TAG, "getView");
			if (discoverFragment.get().featuredEvts.get(position) == null) {
				
				if (convertView == null	|| !convertView.getTag().equals(TAG_PROGRESS_INDICATOR)) {
					convertView = mInflater.inflate(R.layout.list_progress_bar, null);
					convertView.setTag(TAG_PROGRESS_INDICATOR);
				}

			} else {
				
				if (convertView == null	|| !convertView.getTag().equals(TAG_CONTENT)) {
					convertView = mInflater.inflate(R.layout.discover_gallery_item, null);
					convertView.setTag(TAG_CONTENT);
				}

				final Event event = getItem(position);
				ImageView imgFeaturedEvt = (ImageView) convertView.findViewById(R.id.imgFeaturedEvt);
				
				String key = event.getKey(ImgResolution.LOW);
				Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
				if (bitmap != null) {
			        imgFeaturedEvt.setImageBitmap(bitmap);
			    } else {
			    	imgFeaturedEvt.setImageBitmap(null);
			    	AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
			        asyncLoadImg.loadImg(imgFeaturedEvt, ImgResolution.LOW, event);
			    }
				
				TextView txtEvtTitle = (TextView)convertView.findViewById(R.id.txtEvtTitle);
				txtEvtTitle.setText(event.getName());

				com.wcities.eventseeker.core.Date date = event.getSchedule().getDates().get(0);
				
				DateFormat dateFormat = date.isStartTimeAvailable() ? 
						new SimpleDateFormat("EEE MMM d h:mm a") : new SimpleDateFormat("EEE MMM d");
				
				TextView txtEvtSchedule = (TextView)convertView.findViewById(R.id.txtEvtSchedule);
				txtEvtSchedule.setText(dateFormat.format(date.getStartDate()));

				/*TextView txtEvtCity = (TextView)convertView.findViewById(R.id.txtCityName);
				txtEvtCity.setText("Recommended in " + event.getCityName());*/
				
				//discoverFragment.get().cityName = event.getCityName();
				//discoverFragment.get().updateCity();
			}

			return convertView;
		}

	}

	@Override
	protected void notifyDataSetChanged() {
		Log.i(TAG, "getView");
		featuredEventsEcoGalleryAdapter.notifyDataSetChanged();
		if(featuredEventsEcoGalleryAdapter.getCount() > DEFAULT_SELECTED_FEATURED_EVENT_POSITION) {
			ecoGallery.setSelection(DEFAULT_SELECTED_FEATURED_EVENT_POSITION);
		}
		Event event = featuredEvts.get(0);
		if(event != null) {
			cityName = event.getCityName();
			updateCity();
		}
	}

	@Override
	public void onItemClick(EcoGalleryAdapterView<?> parent, View view,
			int position, long id) {
		Event event = (Event) parent.getItemAtPosition(position);
		((EventListener)FragmentUtil.getActivity(this)).onEventSelected(event);
	}

}
