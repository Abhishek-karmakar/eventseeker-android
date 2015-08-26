package com.wcities.eventseeker.bosch.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.interfaces.EventListener;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.ViewUtil;

import java.util.List;

public class BoschEventListAdapter extends BaseAdapter {

	private List<Event> events;
	private LayoutInflater inflater;
    private BitmapCache bitmapCache;
    private Fragment fragment;
    
	public BoschEventListAdapter(Context context, List<Event> events, Fragment fragment) {
		this.events = events;
		this.fragment = fragment;
		this.inflater = LayoutInflater.from(context);
		bitmapCache = BitmapCache.getInstance();
	}
	
	@Override
	public Object getItem(int position) {
		return events.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		VHEventList vhEventList = null;
		
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.bosch_list_item_event, null);
		
			vhEventList = new VHEventList();
			
			vhEventList.imgEvent = (ImageView) convertView.findViewById(R.id.imgEvent);
			vhEventList.txtTitle = (TextView) convertView.findViewById(R.id.txtTitle);
			vhEventList.txtEvtLocation = (TextView) convertView.findViewById(R.id.txtEvtLocation);
			vhEventList.txtEvtLocation.setVisibility(View.GONE);
			/* 20 August, 2015
			After discussing the issue of text cut off with amir,
			we came across the solution of marquee effect for title*/
			final VHEventList finalVhEventList = vhEventList;
			vhEventList.txtTitle.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
				@Override
				public void onLayoutChange(View v, int left, int top, int right,
				int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
					finalVhEventList.txtTitle.setSelected(true);
				}
			});
			
			convertView.setTag(vhEventList);
		
		} else {
			vhEventList = (VHEventList) convertView.getTag();
		}
		
		final Event event = (Event) getItem(position);

		String key = event.getKey(ImgResolution.LOW);
		Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
		
		if (bitmap != null) {
			vhEventList.imgEvent.setImageBitmap(bitmap);
			
	    } else {
	    	vhEventList.imgEvent.setImageBitmap(null);
	    	
	    	AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
	        asyncLoadImg.loadImg(vhEventList.imgEvent, ImgResolution.LOW, event);
	    }
		
		vhEventList.txtTitle.setText(event.getName());
		
		convertView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				((EventListener)FragmentUtil.getActivity(fragment)).onEventSelected(event);
			}
			
		});
		
		ViewUtil.updateViewColor((FragmentUtil.getActivity(fragment)).getResources(), convertView);
		return convertView;
	}

	@Override
	public int getCount() {
		return events.size();
	}
	
	private class VHEventList {
		
		public ImageView imgEvent;
		public TextView txtTitle;
		public TextView txtEvtLocation;
		
	}
	
}
