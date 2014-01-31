package com.wcities.eventseeker.bosch.adapter;

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
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener;
import com.wcities.eventseeker.interfaces.EventListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.viewdata.DateWiseEventList.LIST_ITEM_TYPE;

public class BoschLazyLoadingEventListAdapter extends BaseAdapter implements DateWiseEventParentAdapterListener {
	
	private Context mContext;
	private BitmapCache bitmapCache;
	
	private List<Event> eventList;
	private AsyncTask<Void, Void, List<Event>> loadDateWiseMyEvents;
	
	private int eventsAlreadyRequested;
	private boolean isMoreDataAvailable = true;
	
	private LoadItemsInBackgroundListener mListener;
	
	public BoschLazyLoadingEventListAdapter(Context context, List<Event> eventList,
			AsyncTask<Void, Void, List<Event>> loadDateWiseEvents, LoadItemsInBackgroundListener mListener) {
		mContext = context;
		bitmapCache = BitmapCache.getInstance();
		this.eventList = eventList;
		this.loadDateWiseMyEvents = loadDateWiseEvents;
		this.mListener = mListener;
	}

	@Override
	public int getCount() {
		return eventList.size();
	}

	@Override
	public Event getItem(int position) {
		return eventList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final Event event = getItem(position);
		if (event == null) {
			if (convertView == null	|| convertView.getTag() != AppConstants.TAG_PROGRESS_INDICATOR) {
				convertView = LayoutInflater.from(mContext).inflate(R.layout.list_progress_bar, null);
				convertView.setTag(LIST_ITEM_TYPE.PROGRESS);
			}
			
			if ((loadDateWiseMyEvents == null || loadDateWiseMyEvents.getStatus() == Status.FINISHED) 
					&& isMoreDataAvailable) {
				mListener.loadItemsInBackground();
			}

		} else if (event.getId() == AppConstants.INVALID_ID) {
			convertView = LayoutInflater.from(mContext).inflate(R.layout.list_no_items_found, null);
			((TextView)convertView).setText("No Event found.");
			convertView.setTag("");
			
		} else {
			EventViewHolder eventViewHolder;
			
			if (convertView == null || convertView.getTag() != AppConstants.TAG_CONTENT) {
				convertView = LayoutInflater.from(mContext).inflate(R.layout.bosch_list_item_event, null);
				
				eventViewHolder = new EventViewHolder();
				eventViewHolder.imgEvent = (ImageView) convertView.findViewById(R.id.imgEvent);
				eventViewHolder.txtTitle = (TextView) convertView.findViewById(R.id.txtTitle); 
				convertView.findViewById(R.id.txtEvtLocation).setVisibility(View.GONE);
				
				convertView.setTag(eventViewHolder);
				
			} else {
				eventViewHolder = (EventViewHolder) convertView.getTag();
			}
			
			String key = event.getKey(ImgResolution.LOW);
			Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
			
			if (bitmap != null) {
				eventViewHolder.imgEvent.setImageBitmap(bitmap);
				
		    } else {
		    	eventViewHolder.imgEvent.setImageBitmap(null);
		    	AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
		        asyncLoadImg.loadImg(eventViewHolder.imgEvent, ImgResolution.LOW, (AdapterView) parent, 
		        		position, event);
		    }
			
			eventViewHolder.txtTitle.setText(event.getName());
			
			convertView.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					((EventListener)mContext).onEventSelected(event);
				}
			});
		}
		return convertView;
	}

	@Override
	public int getEventsAlreadyRequested() {
		return eventsAlreadyRequested;
	}

	@Override
	public void setMoreDataAvailable(boolean isMoreDataAvailable) {
		this.isMoreDataAvailable = isMoreDataAvailable;
	}

	@Override
	public void setEventsAlreadyRequested(int eventsAlreadyRequested) {
		this.eventsAlreadyRequested = eventsAlreadyRequested;
	}

	@Override
	public void updateContext(Context context) {
		mContext = context;		
	}

	@Override
	public void setLoadDateWiseEvents(AsyncTask<Void, Void, List<Event>> loadDateWiseEvents) {
		loadDateWiseMyEvents = loadDateWiseEvents;
	}

	private class EventViewHolder {
		public ImageView imgEvent;
		public TextView txtTitle;
	}
}
