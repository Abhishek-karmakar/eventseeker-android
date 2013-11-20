package com.wcities.eventseeker;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.asynctask.LoadDateWiseVenueEventsList;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Schedule;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.interfaces.EventListener;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.DateWiseEventList;
import com.wcities.eventseeker.viewdata.DateWiseEventList.EventListItem;
import com.wcities.eventseeker.viewdata.DateWiseEventList.LIST_ITEM_TYPE;

public class VenueEventsListFragment extends ListFragment {
	
	private static final String TAG = VenueEventsListFragment.class.getName();

	private Venue venue;

	private LoadDateWiseVenueEventsList loadEvents;
	private DateWiseEventList dateWiseEvtList;
	private DateWiseVenueEventsListAdapter adapter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		venue = (Venue) getArguments().getSerializable(BundleKeys.VENUE);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		int orientation = getResources().getConfiguration().orientation;

		View v = super.onCreateView(inflater, container, savedInstanceState);
		LayoutParams lp = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		lp.addRule(RelativeLayout.ABOVE, R.id.lnrLayoutBtns);
		if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
			lp.leftMargin = lp.rightMargin = getResources().getDimensionPixelSize(R.dimen.tab_bar_margin_fragment_custom_tabs);
		}
		v.setLayoutParams(lp);
		
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if (dateWiseEvtList == null) {
			dateWiseEvtList = new DateWiseEventList();
			dateWiseEvtList.addDummyItem();
			
	        adapter = new DateWiseVenueEventsListAdapter(FragmentUtil.getActivity(this));

			loadEventsInBackground();
			
		} else {
			adapter.updateContext(FragmentUtil.getActivity(this));
		}

		setListAdapter(adapter);
        getListView().setDivider(null);
        getListView().setBackgroundResource(R.drawable.story_space);
	}
	
	public void loadEventsInBackground() {
		loadEvents = new LoadDateWiseVenueEventsList(dateWiseEvtList, adapter, venue.getId());
        loadEvents.execute();
	}
	
	public class DateWiseVenueEventsListAdapter extends BaseAdapter {

		private Context mContext;
	    private BitmapCache bitmapCache;
	    private int eventsAlreadyRequested;
		private boolean isMoreDataAvailable = true;
		
	    public DateWiseVenueEventsListAdapter(Context context) {
	    	mContext = context;
	        bitmapCache = BitmapCache.getInstance();
	    }
	    
	    public void updateContext(Context context) {
	    	mContext = context;
		}

		@Override
	    public int getViewTypeCount() {
	    	return LIST_ITEM_TYPE.values().length;
	    }

	    @Override
	    public int getItemViewType(int position) {
	    	return dateWiseEvtList.getItemViewType(position).ordinal();
	    }
	    
		@Override
		public int getCount() {
			return dateWiseEvtList.getCount();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (getItemViewType(position) == LIST_ITEM_TYPE.PROGRESS.ordinal()) {
				if (convertView == null || convertView.getTag() != LIST_ITEM_TYPE.PROGRESS) {
					convertView = LayoutInflater.from(mContext).inflate(R.layout.list_progress_bar, null);
					convertView.setTag(LIST_ITEM_TYPE.PROGRESS);
				}
				
				if ((loadEvents == null || loadEvents.getStatus() == Status.FINISHED) && 
						isMoreDataAvailable) {
					loadEventsInBackground();
				}
				
			} else if (getItemViewType(position) == LIST_ITEM_TYPE.CONTENT.ordinal()) {
				if (convertView == null || convertView.getTag() != LIST_ITEM_TYPE.CONTENT) {
					convertView = LayoutInflater.from(mContext).inflate(R.layout.fragment_discover_by_category_list_item_evt, null);
					convertView.setTag(LIST_ITEM_TYPE.CONTENT);
				}
				
				final Event event = getItem(position).getEvent();
				((TextView)convertView.findViewById(R.id.txtEvtTitle)).setText(event.getName());
				
				if (event.getSchedule() != null) {
					Schedule schedule = event.getSchedule();
					
					if (schedule.getDates().get(0).isStartTimeAvailable()) {
						String[] timeInArray = ConversionUtil.getTimeInArray(schedule.getDates().get(0).getStartDate());
						
						((TextView)convertView.findViewById(R.id.txtEvtTime)).setText(timeInArray[0]);
						((TextView)convertView.findViewById(R.id.txtEvtTimeAMPM)).setText(" " + timeInArray[1]);
						convertView.findViewById(R.id.imgEvtTime).setVisibility(View.VISIBLE);
						
					} else {
						((TextView)convertView.findViewById(R.id.txtEvtTime)).setText("");
						((TextView)convertView.findViewById(R.id.txtEvtTimeAMPM)).setText("");
						convertView.findViewById(R.id.imgEvtTime).setVisibility(View.INVISIBLE);
					}
					((TextView)convertView.findViewById(R.id.txtEvtLocation)).setText(schedule.getVenue().getName());
				}
				
				String key = event.getKey(ImgResolution.LOW);
				Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
				if (bitmap != null) {
			        ((ImageView)convertView.findViewById(R.id.imgEvent)).setImageBitmap(bitmap);
			        
			    } else {
			    	((ImageView)convertView.findViewById(R.id.imgEvent)).setImageBitmap(null);
			    	AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
			        asyncLoadImg.loadImg((ImageView) convertView.findViewById(R.id.imgEvent), 
			        		ImgResolution.LOW, (AdapterView) parent, position, event);
			    }
				
				convertView.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						((EventListener)FragmentUtil.getActivity(VenueEventsListFragment.this)).onEventSelected(event);
					}
				});
				
			} else if (getItemViewType(position) == LIST_ITEM_TYPE.HEADER.ordinal()) {
				if (convertView == null || convertView.getTag() != LIST_ITEM_TYPE.HEADER) {
					convertView = LayoutInflater.from(mContext).inflate(R.layout.fragment_discover_by_category_list_item_header, null);
					convertView.setTag(LIST_ITEM_TYPE.HEADER);
				}
				((TextView)convertView.findViewById(R.id.txtDate)).setText(getItem(position).getDate());
				int visibility = (position == 0) ? View.INVISIBLE : View.VISIBLE;
				convertView.findViewById(R.id.divider1).setVisibility(visibility);
			}
			
			return convertView;
		}

		@Override
		public EventListItem getItem(int position) {
			return dateWiseEvtList.getItem(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		public int getEventsAlreadyRequested() {
			return eventsAlreadyRequested;
		}

		public void setEventsAlreadyRequested(int eventsAlreadyRequested) {
			this.eventsAlreadyRequested = eventsAlreadyRequested;
		}

		public void setMoreDataAvailable(boolean isMoreDataAvailable) {
			this.isMoreDataAvailable = isMoreDataAvailable;
		}
	}
}
