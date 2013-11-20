package com.wcities.eventseeker.adapter;

import android.content.Context;
import android.graphics.Bitmap;
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
import com.wcities.eventseeker.asynctask.LoadDateWiseEvents;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Schedule;
import com.wcities.eventseeker.interfaces.EventListener;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.viewdata.DateWiseEventList;
import com.wcities.eventseeker.viewdata.DateWiseEventList.EventListItem;
import com.wcities.eventseeker.viewdata.DateWiseEventList.LIST_ITEM_TYPE;

public class DateWiseEventListAdapter extends BaseAdapter {

	private Context mContext;
    private BitmapCache bitmapCache;
    private DateWiseEventList dateWiseEvtList;
    private LoadDateWiseEvents loadDateWiseEvents;
    private int eventsAlreadyRequested;
	private boolean isMoreDataAvailable = true;
	private DateWiseEventListAdapterListener mListener;
	
	public interface DateWiseEventListAdapterListener {
		public void loadEventsInBackground();
	}

    public DateWiseEventListAdapter(Context context, DateWiseEventList dateWiseEvtList, 
    		LoadDateWiseEvents loadDateWiseEvents, DateWiseEventListAdapterListener mListener) {
    	mContext = context;
        bitmapCache = BitmapCache.getInstance();
        this.dateWiseEvtList = dateWiseEvtList;
        this.loadDateWiseEvents = loadDateWiseEvents;
        this.mListener = mListener;
    }
    
    public void updateContext(Context context) {
    	mContext = context;
	}

	public void setLoadDateWiseEvents(LoadDateWiseEvents loadDateWiseEvents) {
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
				mListener.loadEventsInBackground();
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
					((EventListener) mContext).onEventSelected(event);
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