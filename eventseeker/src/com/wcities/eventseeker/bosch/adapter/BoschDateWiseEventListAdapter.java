package com.wcities.eventseeker.bosch.adapter;

import java.text.SimpleDateFormat;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Schedule;
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener;
import com.wcities.eventseeker.interfaces.EventListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.viewdata.DateWiseEventList;
import com.wcities.eventseeker.viewdata.DateWiseEventList.EventListItem;
import com.wcities.eventseeker.viewdata.DateWiseEventList.LIST_ITEM_TYPE;

public class BoschDateWiseEventListAdapter extends BaseAdapter implements DateWiseEventParentAdapterListener {

	private static final String DATE_FORMAT = "EEE, MMMM d";
	
	private Context mContext;
    private BitmapCache bitmapCache;
    private DateWiseEventList dateWiseEvtList;
    private AsyncTask<Void, Void, List<Event>> loadDateWiseEvents;
    private int eventsAlreadyRequested;
	private boolean isMoreDataAvailable = true;
	private LoadItemsInBackgroundListener mListener;
	
    public BoschDateWiseEventListAdapter(Context context, DateWiseEventList dateWiseEvtList, 
    		AsyncTask<Void, Void, List<Event>> loadDateWiseEvents, LoadItemsInBackgroundListener mListener) {
    	mContext = context;
        bitmapCache = BitmapCache.getInstance();
        this.dateWiseEvtList = dateWiseEvtList;
        this.loadDateWiseEvents = loadDateWiseEvents;
        this.mListener = mListener;
    }
    
    @Override
    public void updateContext(Context context) {
    	mContext = context;
	}

    @Override
	public void setLoadDateWiseEvents(AsyncTask<Void, Void, List<Event>> loadDateWiseEvents) {
		this.loadDateWiseEvents = loadDateWiseEvents;
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
			
			if ((loadDateWiseEvents == null || loadDateWiseEvents.getStatus() == Status.FINISHED) && 
					isMoreDataAvailable) {
				mListener.loadItemsInBackground();
			}
			
		} else if (getItemViewType(position) == LIST_ITEM_TYPE.NO_EVENTS.ordinal()) {

			final Event event = getItem(position).getEvent();

			if (event.getId() == AppConstants.INVALID_ID) {
				convertView = LayoutInflater.from(mContext).inflate(R.layout.list_no_items_found, null);
				((TextView)convertView).setText("No Event Found.");
				convertView.setTag("");
			} 
			
		} else if (getItemViewType(position) == LIST_ITEM_TYPE.CONTENT.ordinal()) {
			if (convertView == null || convertView.getTag() != LIST_ITEM_TYPE.CONTENT) {
				convertView = LayoutInflater.from(mContext).inflate(
						R.layout.bosch_fragment_discover_by_category_list_item_evt, null);
				convertView.setTag(LIST_ITEM_TYPE.CONTENT);
			}
			
			final Event event = getItem(position).getEvent();
			String title = event.getName();
			
			if (event.getSchedule() != null) {
				Schedule schedule = event.getSchedule();
				
				if (schedule.getDates().get(0).isStartTimeAvailable()) {
					title += " @ " + new SimpleDateFormat("ha").format(schedule.getDates().get(0)
							.getStartDate()).toLowerCase();
				}
				((TextView)convertView.findViewById(R.id.txtEvtLocation)).setText(schedule.getVenue().getName() 
						+ ", " + schedule.getVenue().getAddress().getCity());
			}
			((TextView)convertView.findViewById(R.id.txtTitle)).setText(title);
			
			BitmapCacheable bitmapCacheable = event.doesValidImgUrlExist() ? event : event.getSchedule().getVenue();  
			
			String key = bitmapCacheable.getKey(ImgResolution.LOW);
			Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
			if (bitmap != null) {
		        ((ImageView)convertView.findViewById(R.id.imgEvent)).setImageBitmap(bitmap);
		        
		    } else {
		    	((ImageView)convertView.findViewById(R.id.imgEvent)).setImageBitmap(null);
		    	AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
		        asyncLoadImg.loadImg((ImageView) convertView.findViewById(R.id.imgEvent), 
		        		ImgResolution.LOW, (AdapterView) parent, position, bitmapCacheable);
		    }
			
			convertView.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					//((EventListener) mContext).onEventSelected(event);
				}
			});
			
		} else if (getItemViewType(position) == LIST_ITEM_TYPE.HEADER.ordinal()) {
			if (convertView == null || convertView.getTag() != LIST_ITEM_TYPE.HEADER) {
				convertView = LayoutInflater.from(mContext).inflate(
						R.layout.bosch_fragment_discover_by_category_list_item_header, null);
				convertView.setTag(LIST_ITEM_TYPE.HEADER);
			}
			((TextView)convertView.findViewById(R.id.txtDate)).setText(getItem(position).getDate());
		}
		
		return convertView;
	}

	@Override
	public EventListItem getItem(int position) {
		return dateWiseEvtList.getItem(position, DATE_FORMAT);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public int getEventsAlreadyRequested() {
		return eventsAlreadyRequested;
	}

	@Override
	public void setEventsAlreadyRequested(int eventsAlreadyRequested) {
		this.eventsAlreadyRequested = eventsAlreadyRequested;
	}

	@Override
	public void setMoreDataAvailable(boolean isMoreDataAvailable) {
		this.isMoreDataAvailable = isMoreDataAvailable;
	}

}
