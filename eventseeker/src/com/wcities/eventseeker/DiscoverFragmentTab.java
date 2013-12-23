package com.wcities.eventseeker;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import us.feras.ecogallery.EcoGallery;
import us.feras.ecogallery.EcoGalleryAdapterView;
import us.feras.ecogallery.EcoGalleryAdapterView.OnItemClickListener;
import us.feras.ecogallery.EcoGalleryAdapterView.OnItemSelectedListener;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.interfaces.EventListener;
import com.wcities.eventseeker.util.FragmentUtil;

public class DiscoverFragmentTab extends DiscoverParentFragment implements OnItemClickListener {

	public static final String TAG = DiscoverFragmentTab.class.getName();

	private EcoGallery ecoGallery;
	private FeaturedEventsEcoGalleryAdapter featuredEventsEcoGalleryAdapter;

	private String cityName = "Loading...";
	private TextView txtCityName;
	private int txtCityNameWInLandscape;
	
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
		//Log.d(TAG, "onCreateView");
		createCustomActionBar();

		View v = super.onCreateView(inflater, container, savedInstanceState);

		featuredEventsEcoGalleryAdapter = new FeaturedEventsEcoGalleryAdapter(this, FragmentUtil.getActivity(this));
		
		ecoGallery = (EcoGallery) v.findViewById(R.id.ecoglryFeaturedEvt);
		ecoGallery.setAdapter(featuredEventsEcoGalleryAdapter);
		int numOfFeaturedImages = featuredEventsEcoGalleryAdapter.getCount(); 
		if(numOfFeaturedImages > 0) {
			ecoGallery.setSelection(numOfFeaturedImages / 2);
		}
		ecoGallery.setOnItemClickListener(this);
		ecoGallery.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(EcoGalleryAdapterView<?> parent, View view, int position, long id) {
				
				position = position % featuredEvts.size();
				
				cityName = featuredEvts.get(position).getCityName();
				//Log.d(TAG, "pos = " + position + ", cityName = " + cityName);
				txtCityName.setText(cityName);
				//createCustomActionBar();
			}

			@Override
			public void onNothingSelected(EcoGalleryAdapterView<?> parent) {
				
			}
		});

		/*if (featuredEventsEcoGalleryAdapter.getCount() > DEFAULT_SELECTED_FEATURED_EVENT_POSITION) {
			ecoGallery.setSelection(DEFAULT_SELECTED_FEATURED_EVENT_POSITION);
		}*/
		
		return v;
	}

	@Override
	public void onDestroyView() {
		//if (ecoGallery != null) {
			// viewPagerSelectedPos = ecoGallery.getCurrentItem();
		//}
		//Log.d(TAG, "onDestroyView");
		super.onDestroyView();
		ActionBar actionBar = ((ActionBarActivity)FragmentUtil.getActivity(this)).getSupportActionBar();
		actionBar.setCustomView(null);
	}

	private void createCustomActionBar() {
		//Log.d(TAG, "createCustomActionBar");
		
		ActionBarActivity actionBarActivity = (ActionBarActivity) FragmentUtil.getActivity(this);

		ActionBar actionBar = ((ActionBarActivity) actionBarActivity).getSupportActionBar();
		actionBar.setDisplayShowCustomEnabled(true);
		
		LayoutInflater lytInflater = (LayoutInflater) actionBarActivity.getSystemService(
				Context.LAYOUT_INFLATER_SERVICE);
		View vActionBar = lytInflater.inflate(R.layout.action_bar_custom_view_item, null);
		txtCityName = (TextView) vActionBar.findViewById(R.id.txtCityName);

		ActionBar.LayoutParams params;
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			params = new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, 
					ActionBar.LayoutParams.MATCH_PARENT);
			params.gravity = Gravity.CENTER;
			
		} else {
			/**
			 * Initially txtCityNameWInLandscape = 0 & sometimes txtCityName.getWidth() for first call 
			 * returns entire actionbar (screen) width not considering any action items on right hand side,
			 * hence following 2 conditions' check.
			 */
			if (txtCityNameWInLandscape == 0 || txtCityNameWInLandscape == getResources().getDisplayMetrics().widthPixels) {
				txtCityName.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
				    @Override
				    public void onGlobalLayout() {
			    		txtCityNameWInLandscape = txtCityName.getWidth();
						txtCityName.getViewTreeObserver().removeGlobalOnLayoutListener(this);	
			    		createCustomActionBar();
				    }
				});
				params = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);
				params.gravity = Gravity.RIGHT;
				
			} else {
				int actionItemsWidth = getResources().getDisplayMetrics().widthPixels - txtCityNameWInLandscape;
				int customViewWidth = getResources().getDisplayMetrics().widthPixels - 
						getResources().getDimensionPixelSize(R.dimen.root_navigation_drawer_w_main) - 2 * actionItemsWidth;
				params = new ActionBar.LayoutParams(customViewWidth, ActionBar.LayoutParams.MATCH_PARENT);
				params.gravity = Gravity.RIGHT;
			}
		}
		
		txtCityName.setText(cityName);
		actionBar.setCustomView(vActionBar, params);
		
		if (((EventSeekr)actionBarActivity.getApplicationContext()).isTabletAndInLandscapeMode()) {
			actionBar.setIcon(R.drawable.placeholder);
		}
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
			int size = discoverFragment.get().featuredEvts.size();
			if(size > 0) {
				position = position % size;
			}
			return discoverFragment.get().featuredEvts.get(position);
		}
		
		@Override
		public int getCount() {
			// Log.i(TAG, "count = " +
			// discoverActivity.get().featuredEvts.size());
			
			int size = (discoverFragment.get().featuredEvts.size() > 0) ? Integer.MAX_VALUE : 0;
			return size;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			//Log.d(TAG, "getView(), pos = " + position);
			final Event event = getItem(position);

			if (event == null) {
				//Log.d(TAG, "event is null");
				if (convertView == null	|| !convertView.getTag().equals(TAG_PROGRESS_INDICATOR)) {
					convertView = mInflater.inflate(R.layout.list_progress_bar, null);
					convertView.setTag(TAG_PROGRESS_INDICATOR);
				}

			} else {
				//Log.d(TAG, "event is not null");
				if (convertView == null	|| !convertView.getTag().equals(TAG_CONTENT)) {
					convertView = mInflater.inflate(R.layout.discover_gallery_item, null);
					convertView.setTag(TAG_CONTENT);
				}

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
				
				//Log.d(TAG, "event title = " + event.getName());
				TextView txtEvtTitle = (TextView)convertView.findViewById(R.id.txtEvtTitle);
				txtEvtTitle.setText(event.getName());

				com.wcities.eventseeker.core.Date date = event.getSchedule().getDates().get(0);
				
				DateFormat dateFormat = date.isStartTimeAvailable() ? 
						new SimpleDateFormat("EEE MMM d h:mm a") : new SimpleDateFormat("EEE MMM d");
				
				TextView txtEvtSchedule = (TextView)convertView.findViewById(R.id.txtEvtSchedule);
				txtEvtSchedule.setText(dateFormat.format(date.getStartDate()));
			}

			//Log.d(TAG, "getView() return");
			return convertView;
		}

	}

	@Override
	protected void notifyDataSetChanged() {
		//Log.d(TAG, "notifyDataSetChanged");
		featuredEventsEcoGalleryAdapter.notifyDataSetChanged();
		/*if(featuredEventsEcoGalleryAdapter.getCount() > DEFAULT_SELECTED_FEATURED_EVENT_POSITION) {
			ecoGallery.setSelection(DEFAULT_SELECTED_FEATURED_EVENT_POSITION);
		}*/
		int numOfFeaturedImages = featuredEventsEcoGalleryAdapter.getCount(); 
		if(numOfFeaturedImages > 0) {
			ecoGallery.setSelection(numOfFeaturedImages / 2);
		}
		/*if (!featuredEvts.isEmpty()) {
			cityName = featuredEvts.get(0).getCityName();
			//updateCity();
		}*/
	}

	@Override
	public void onItemClick(EcoGalleryAdapterView<?> parent, View view, int position, long id) {
		Event event = (Event) parent.getItemAtPosition(position);
		((EventListener)FragmentUtil.getActivity(this)).onEventSelected(event);
	}
}
