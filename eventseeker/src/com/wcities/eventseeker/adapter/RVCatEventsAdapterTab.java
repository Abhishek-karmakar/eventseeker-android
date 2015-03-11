package com.wcities.eventseeker.adapter;

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.adapter.RVCatEventsAdapterTab.ViewHolder;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;

public class RVCatEventsAdapterTab extends Adapter<ViewHolder> implements DateWiseEventParentAdapterListener {

	private static final String TAG = RVCatEventsAdapterTab.class.getSimpleName();

	private static final int INVALID = -1;
	
	private List<Event> eventList;
	
	private int eventsAlreadyRequested;
	private boolean isMoreDataAvailable = true;
	
	private int openPos = INVALID;
	
	private AsyncTask<Void, Void, List<Event>> loadDateWiseEvents;
	private LoadItemsInBackgroundListener mListener;
	private RecyclerView recyclerView;
	
	private BitmapCache bitmapCache;
	
	private static enum ViewType {
		PROGRESS, EVENT;
	};
	
	static class ViewHolder extends RecyclerView.ViewHolder {
		
		private ImageView imgEvt;
			
		public ViewHolder(View itemView) {
			super(itemView);
			imgEvt = (ImageView) itemView.findViewById(R.id.imgEvt);
		}
	}

	public RVCatEventsAdapterTab(List<Event> eventList, AsyncTask<Void, Void, List<Event>> loadDateWiseEvents,
			LoadItemsInBackgroundListener mListener) {
		this.eventList = eventList;
		this.loadDateWiseEvents = loadDateWiseEvents;
		this.mListener = mListener;
		
		bitmapCache = BitmapCache.getInstance();
	}

	@Override
	public int getItemCount() {
		Log.d(TAG, "getItemCount() - " + eventList.size());
		return eventList.size();
	}
	
	@Override
	public int getItemViewType(int position) {
		if (eventList.get(position) == null) {
			return ViewType.PROGRESS.ordinal();
			
		} else {
			return ViewType.EVENT.ordinal();
		}
	}
	
	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		recyclerView = (RecyclerView) parent;
		
		View v;
		
		ViewType vType = ViewType.values()[viewType];
		switch (vType) {
		
		case PROGRESS:
			v = LayoutInflater.from(parent.getContext()).inflate(R.layout.progress_bar_eventseeker_fixed_ht, parent, 
					false);
			break;
			
		case EVENT:
			v = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_item_event_tab, parent, false);
			break;
			
		default:
			v = null;
			break;
		}
		
        return new ViewHolder(v);
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, int position) {
		//Log.d(TAG, "onBindViewHolder() - pos = " + position);
		Event event = eventList.get(position);
		if (event == null) {
			// progress indicator
			if ((loadDateWiseEvents == null || loadDateWiseEvents.getStatus() == Status.FINISHED) && 
					isMoreDataAvailable) {
				//Log.d(TAG, "onBindViewHolder(), pos = " + position);
				mListener.loadItemsInBackground();
			}
			
		} else {
			//Log.d(TAG, "else");
			BitmapCacheable bitmapCacheable = null;
			/**
			 * added this try catch as if event will not have valid url and schedule object then
			 * the below line may cause NullPointerException. So, added the try-catch and added the
			 * null check for bitmapCacheable on following statements.
			 */
			try {
				bitmapCacheable = event.doesValidImgUrlExist() ? event : event.getSchedule().getVenue();
				
			} catch (NullPointerException e) {
				e.printStackTrace();
			}
			
			if (bitmapCacheable != null) {
				String key = bitmapCacheable.getKey(ImgResolution.LOW);
				Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
				if (bitmap != null) {
			        holder.imgEvt.setImageBitmap(bitmap);
			        
			    } else {
			    	holder.imgEvt.setImageBitmap(null);
			    	AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
			        asyncLoadImg.loadImg(holder.imgEvt, ImgResolution.LOW, recyclerView, position, bitmapCacheable);
			    }
			}
		}
	}
	
	public void reset() {
		openPos = INVALID;
		setEventsAlreadyRequested(0);
		setMoreDataAvailable(true);
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
		// TODO Auto-generated method stub
	}

	@Override
	public void setLoadDateWiseEvents(AsyncTask<Void, Void, List<Event>> loadDateWiseEvents) {
		this.loadDateWiseEvents = loadDateWiseEvents;
	}
}
