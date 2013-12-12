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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.core.Date;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.interfaces.EventListener;
import com.wcities.eventseeker.util.FragmentUtil;

public class DiscoverFragmentTab extends DiscoverParentFragment implements OnItemClickListener {

	public static final String TAG = DiscoverFragment.class.getName();

	private static final int DEFAULT_SELECTED_FEATURED_EVENT_POSITION = 2;

	private EcoGallery ecoGallery;
	private FeaturedEventsEcoGalleryAdapter featuredEventsEcoGalleryAdapter;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = super.onCreateView(inflater, container, savedInstanceState);

		// txtFeaturedEvtsTitle = (TextView)
		// v.findViewById(R.id.txtFeaturedEvtsTitle);
		// updateCityName();

		featuredEventsEcoGalleryAdapter = new FeaturedEventsEcoGalleryAdapter(
				this, FragmentUtil.getActivity(this));

		ecoGallery = (EcoGallery) v.findViewById(R.id.ecoglryFeaturedEvt);
		ecoGallery.setAdapter(featuredEventsEcoGalleryAdapter);
		ecoGallery.setOnItemClickListener(this);

		if(featuredEventsEcoGalleryAdapter.getCount() > DEFAULT_SELECTED_FEATURED_EVENT_POSITION) {
			ecoGallery.setSelection(DEFAULT_SELECTED_FEATURED_EVENT_POSITION);
		}
		/*
		 * imgPrev = (ImageView) v.findViewById(R.id.imgPrev); imgNext =
		 * (ImageView) v.findViewById(R.id.imgNext);
		 * imgPrev.setOnClickListener(this); imgNext.setOnClickListener(this);
		 */

		return v;
	}

	@Override
	public void onDestroyView() {
		// Log.d(TAG, "onDestroyView()");
		if (ecoGallery != null) {
			// viewPagerSelectedPos = ecoGallery.getCurrentItem();
		}
		super.onDestroyView();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
	}

	/*
	 * @Override public void onActivityResult(int requestCode, int resultCode,
	 * Intent data) { if (requestCode == CHANGE_LOCATION_REQUEST) { if
	 * (resultCode == Activity.RESULT_OK) { Bundle extras = data.getExtras();
	 * double newLat = extras.getDouble(BundleKeys.LAT); double newLon =
	 * extras.getDouble(BundleKeys.LON);
	 * 
	 * if (lat != newLat || lon != newLon) { lat = newLat; lon = newLon;
	 * 
	 * updateCityName(); featuredEvts.clear();
	 * discoverFragmentPagerAdapter.notifyDataSetChanged(); new
	 * LoadFeaturedEvts().execute(); } } } }
	 */

	/*
	 * private void updateCityName() { Geocoder geocoder = new
	 * Geocoder(((Activity)mListener).getApplicationContext(),
	 * Locale.getDefault()); try { List<Address> addresses =
	 * geocoder.getFromLocation(lat, lon, 1);
	 * 
	 * if (addresses != null && !addresses.isEmpty()) { Address address =
	 * addresses.get(0); cityName = address.getLocality();
	 * 
	 * String commonTitle =
	 * getResources().getString(R.string.title_featured_evts); String city =
	 * commonTitle + cityName; txtFeaturedEvtsTitle.setText(city);
	 * txtFeaturedEvtsTitle.setSelected(true);
	 * 
	 * } else { Log.w(TAG, "No relevant address found."); }
	 * 
	 * } catch (IOException e) { e.printStackTrace(); } }
	 */

	private static class FeaturedEventsEcoGalleryAdapter extends BaseAdapter {

		private WeakReference<DiscoverFragmentTab> discoverFragment;

		private static final String TAG_PROGRESS_INDICATOR = "progressIndicator";
		private static final String TAG_CONTENT = "content";

		private LayoutInflater mInflater;
		private BitmapCache bitmapCache;

		public FeaturedEventsEcoGalleryAdapter(
				DiscoverFragmentTab discoverFragment, Context context) {
			this.discoverFragment = new WeakReference<DiscoverFragmentTab>(
					discoverFragment);
			mInflater = LayoutInflater.from(context);
			bitmapCache = BitmapCache.getInstance();

		}

		/*
		 * @Override public Fragment getItem(int index) { // Log.d(TAG,
		 * "getItem() for index = " + index);
		 *//**
		 * When viewpager data source changes & notifyDataSetChanged() is
		 * refreshing viewpager, onPageSelected() method is not called up for
		 * first time unless we scroll in any direction. Hence to update left &
		 * right arrow buttons, we need to call this call back method explicitly
		 * for currently visible index.
		 */
		/*
		 * 
		 * if (index == discoverActivity.get().viewPager.getCurrentItem()) {
		 * discoverActivity.get().onPageSelected(index); }
		 * 
		 * 
		 * FeaturedEventsFragment featuredEventsFragment =
		 * FeaturedEventsFragment
		 * .newInstance(discoverFragment.get().featuredEvts.get(index)); return
		 * featuredEventsFragment; }
		 */

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
				
				DateFormat dateFormat = date.isStartTimeAvailable() ? new SimpleDateFormat("EEE MMM d h:mm a") :
					new SimpleDateFormat("EEE MMM d");
				
				TextView txtEvtSchedule = (TextView)convertView.findViewById(R.id.txtEvtSchedule);
				txtEvtSchedule.setText(dateFormat.format(date.getStartDate()));

				TextView txtEvtCity = (TextView)convertView.findViewById(R.id.txtCityName);
				txtEvtCity.setText("Recommended in " + event.getCityName());
				
				/*convertView.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						//Log.d(TAG, "onCLick() mListener = " + mListener);
						/**
						 * Here we can't use mListener directly, because for ViewPager, it throws "IllegalStateException: Activity has been destroyed"
						 * in following case.
						 * Change orientation before ViewPager children start loading & then click on any page
						 * resulting in above mentioned exception while committing fragment replacement
						 * from MainActivity due to following call which would have been 
						 * "mListener.onEventSelected(event);" if 
						 * we use mListener. This happens because onAttach() is not called initially for starting 
						 * orientation & called only second time when orientation changes, but this call passes activity's old
						 * reference with onAttach() which can be figured out by logs. And hence using mListener 
						 * will call following function replaceDiscoverFragmentByFragment() on old activity reference which 
						 * should not be the case. So prefer using "(EventListener)FragmentUtil.getActivity(FeaturedEventsFragment.this)"
						 * instead of mListener.
						 *//*
						((EventListener)FragmentUtil.getActivity(discoverFragment.get())).onEventSelected(event);
					}
				});*/
				
			}

			return convertView;
		}

	}

	@Override
	protected void notifyDataSetChanged() {
		featuredEventsEcoGalleryAdapter.notifyDataSetChanged();
		if(featuredEventsEcoGalleryAdapter.getCount() > DEFAULT_SELECTED_FEATURED_EVENT_POSITION) {
			ecoGallery.setSelection(DEFAULT_SELECTED_FEATURED_EVENT_POSITION);
		}
	}

	@Override
	public void onItemClick(EcoGalleryAdapterView<?> parent, View view,
			int position, long id) {
		Event event = (Event) parent.getItemAtPosition(position);
		((EventListener)FragmentUtil.getActivity(this)).onEventSelected(event);
	}

}
