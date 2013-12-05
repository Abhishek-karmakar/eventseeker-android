package com.wcities.eventseeker;

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.wcities.eventseeker.ArtistDetailsFragment.FooterTxt;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Schedule;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener;
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener.LoadEventsInBackgroundListener;
import com.wcities.eventseeker.interfaces.EventListener;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.DateWiseEventList;
import com.wcities.eventseeker.viewdata.DateWiseEventList.EventListItem;
import com.wcities.eventseeker.viewdata.DateWiseEventList.LIST_ITEM_TYPE;

public class ArtistEventsFragment extends ArtistEventsParentFragment {

	private static final String TAG = ArtistEventsFragment.class.getName();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return super.onCreateView(inflater, container, savedInstanceState);
	}
	
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	protected void updateFollowingFooter() {

		if (artist.getAttending() == null || 
				((EventSeekr) FragmentUtil.getActivity(this).getApplication()).getWcitiesId() == null) {
			
			fragmentArtistDetailsFooter.setVisibility(View.GONE);
		
		} else {
		
			fragmentArtistDetailsFooter.setVisibility(View.VISIBLE);

			switch (artist.getAttending()) {

				case Tracked:
					imgFollow.setImageDrawable(getResources().getDrawable(R.drawable.following));
					txtFollow.setText(FooterTxt.Following.name());
					break;
	
				case NotTracked:
					imgFollow.setImageDrawable(getResources().getDrawable(R.drawable.follow));
					txtFollow.setText(FooterTxt.Follow.name());
					break;
	
				default:
					break;
			}
		}
	}

	protected static class ArtistEventsListAdapter extends BaseAdapter implements DateWiseEventParentAdapterListener{

		private LayoutInflater mInflater;
		private BitmapCache bitmapCache;
		private DateWiseEventList dateWiseEventList;
		private Context context;
		private LoadEventsInBackgroundListener mListener;
		private static final String TAG_CONTENT = "content";
		private static final String TAG_HEADER = "header";
		
		public ArtistEventsListAdapter(Context context, List<Event> list, LoadEventsInBackgroundListener listener) {

			mInflater = LayoutInflater.from(context);
			bitmapCache = BitmapCache.getInstance();
			dateWiseEventList = new DateWiseEventList();
			this.context = context;
			this.mListener = listener;
			setDataSet(list);
		}
		
		public void setDataSet(List<Event> list) {
			dateWiseEventList.addEventListItems(list, null);
		}

		@Override
		public int getCount() {
			return dateWiseEventList.getCount();
		}

		@Override
		public Object getItem(int position) {
			return dateWiseEventList.getItem(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			EventListItem eventListItem = dateWiseEventList.getItem(position);
				
			if (dateWiseEventList.getItemViewType(position) == LIST_ITEM_TYPE.CONTENT) {

			if(convertView == null || !convertView.getTag().equals(TAG_CONTENT)) {
				convertView = mInflater.inflate(R.layout.fragment_discover_by_category_list_item_evt, null);
				convertView.setTag(TAG_CONTENT);
			}	

			final Event event = eventListItem.getEvent();
			((TextView) convertView.findViewById(R.id.txtEvtTitle)).setText(event.getName());
			
			if (event.getSchedule() != null) {
				Schedule schedule = event.getSchedule();

				if (schedule.getDates().get(0).isStartTimeAvailable()) {
					String[] timeInArray = 
						ConversionUtil.getTimeInArray(schedule.getDates().get(0).getStartDate());

						((TextView) convertView.findViewById(R.id.txtEvtTime)).setText(timeInArray[0]);
						((TextView) convertView.findViewById(R.id.txtEvtTimeAMPM)).setText(" " + timeInArray[1]);
						convertView.findViewById(R.id.imgEvtTime).setVisibility(View.VISIBLE);

					} else {
						((TextView) convertView.findViewById(R.id.txtEvtTime)).setText("");
						((TextView) convertView.findViewById(R.id.txtEvtTimeAMPM)).setText("");
						convertView.findViewById(R.id.imgEvtTime).setVisibility(View.INVISIBLE);
					}
						((TextView) convertView.findViewById(R.id.txtEvtLocation)).setText(schedule.getVenue().getName());
				}

				ImageView imgVenue = (ImageView) convertView.findViewById(R.id.imgEvent);
				Venue venue = event.getSchedule().getVenue();
				String key = venue.getKey(ImgResolution.LOW);
				
				Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
				if (bitmap != null) {
					imgVenue.setImageBitmap(bitmap);
				} else {
					imgVenue.setImageBitmap(null);
					AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
					asyncLoadImg.loadImg(imgVenue, ImgResolution.LOW, venue);
				}

				convertView.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
						((EventListener)context).onEventSelected(event);
					}
				});

			} else {
				if(convertView == null || !convertView.getTag().equals(TAG_HEADER)) {
					convertView = mInflater.inflate(R.layout.fragment_discover_by_category_list_item_header, null);
				}
				((TextView) convertView.findViewById(R.id.txtDate)).setText(eventListItem.getDate());
				int visibility = (position == 0) ? View.INVISIBLE : View.VISIBLE;
				convertView.findViewById(R.id.divider1).setVisibility(visibility);
				convertView.setTag(TAG_HEADER);
			}

			return convertView;
			
		}

		@Override
		public int getEventsAlreadyRequested() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void setMoreDataAvailable(boolean isMoreDataAvailable) {
			// TODO Auto-generated method stub
		}

		@Override
		public void setEventsAlreadyRequested(int eventsAlreadyRequested) {
			// TODO Auto-generated method stub
		}

		@Override
		public void updateContext(Context context) {
			// TODO Auto-generated method stub
		}

		@Override
		public void setLoadDateWiseEvents(AsyncTask<Void, Void, List<Event>> loadDateWiseEvents) {
			// TODO Auto-generated method stub
		}

	}

	@Override
	protected DateWiseEventParentAdapterListener getAdapterInstance() {
		return new ArtistEventsListAdapter(FragmentUtil.getActivity(this), artist.getEvents(), this);
	}

}
