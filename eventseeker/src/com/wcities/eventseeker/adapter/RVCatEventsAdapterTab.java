package com.wcities.eventseeker.adapter;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.AsyncTask.Status;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.wcities.eventseeker.DiscoverActivityTab;
import com.wcities.eventseeker.DiscoverFragmentTab;
import com.wcities.eventseeker.EventDetailsActivityTab;
import com.wcities.eventseeker.LauncherActivityTab;
import com.wcities.eventseeker.MainActivity;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.adapter.RVCatEventsAdapterTab.ViewHolder;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Date;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Schedule;
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class RVCatEventsAdapterTab extends Adapter<ViewHolder> implements DateWiseEventParentAdapterListener {

	private static final String TAG = RVCatEventsAdapterTab.class.getSimpleName();

	private static final int INVALID = -1;
	
	private DiscoverFragmentTab discoverFragmentTab;
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
		private TextView txtEvtTitle, txtEvtLoc, txtEvtTime;
			
		public ViewHolder(View itemView) {
			super(itemView);
			imgEvt = (ImageView) itemView.findViewById(R.id.imgEvt);
			txtEvtTitle = (TextView) itemView.findViewById(R.id.txtEvtTitle);
			txtEvtLoc = (TextView) itemView.findViewById(R.id.txtEvtLoc);
			txtEvtTime = (TextView) itemView.findViewById(R.id.txtEvtTime);
		}
	}

	public RVCatEventsAdapterTab(List<Event> eventList, AsyncTask<Void, Void, List<Event>> loadDateWiseEvents,
			LoadItemsInBackgroundListener mListener, DiscoverFragmentTab discoverFragmentTab) {
		this.eventList = eventList;
		this.loadDateWiseEvents = loadDateWiseEvents;
		this.mListener = mListener;
		this.discoverFragmentTab = discoverFragmentTab;
		
		bitmapCache = BitmapCache.getInstance();
	}

	@Override
	public int getItemCount() {
		//Log.d(TAG, "getItemCount() - " + eventList.size());
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
	public void onBindViewHolder(final ViewHolder holder, int position) {
		//Log.d(TAG, "onBindViewHolder() - pos = " + position);
		final Event event = eventList.get(position);
		if (event == null) {
			// progress indicator
			if ((loadDateWiseEvents == null || loadDateWiseEvents.getStatus() == Status.FINISHED) && 
					isMoreDataAvailable) {
				//Log.d(TAG, "onBindViewHolder(), pos = " + position);
				mListener.loadItemsInBackground();
			}
			
		} else {
			//Log.d(TAG, "else");
			holder.txtEvtTitle.setText(event.getName());
			
			if (event.getSchedule() != null) {
				Schedule schedule = event.getSchedule();
				Date date = schedule.getDates().get(0);
				holder.txtEvtTime.setText(ConversionUtil.getDateTime(date.getStartDate(), date.isStartTimeAvailable()));
				
				String venueName = (schedule.getVenue() != null) ? schedule.getVenue().getName() : "";
				holder.txtEvtLoc.setText(venueName);
			}

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
				// set tag to compare it in AsyncLoadImg before setting bitmap to imageview
		    	holder.imgEvt.setTag(key);

				Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
				if (bitmap != null) {
			        holder.imgEvt.setImageBitmap(bitmap);
			        
			    } else {
			    	holder.imgEvt.setImageBitmap(null);
			    	AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
			        asyncLoadImg.loadImg(holder.imgEvt, ImgResolution.LOW, recyclerView, position, bitmapCacheable);
			    }
			}
			
			ViewCompat.setTransitionName(holder.imgEvt, "imageTransition" + position);
			
			holder.itemView.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Activity activity = FragmentUtil.getActivity(discoverFragmentTab);
					Intent intent = new Intent(FragmentUtil.getApplication(discoverFragmentTab), 
							EventDetailsActivityTab.class);
					intent.putExtra(BundleKeys.EVENT, event);
					intent.putExtra("SharedName", ViewCompat.getTransitionName(holder.imgEvt));
					//FragmentUtil.getActivity(discoverFragmentTab).startActivity(intent);
					
					ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, 
							Pair.create((View)holder.imgEvt, ViewCompat.getTransitionName(holder.imgEvt)));
                    ActivityCompat.startActivity(activity, intent, options.toBundle());
				}
			});
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
