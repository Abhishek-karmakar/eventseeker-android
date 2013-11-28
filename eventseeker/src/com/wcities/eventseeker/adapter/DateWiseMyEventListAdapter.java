package com.wcities.eventseeker.adapter;

import java.util.List;

import android.R.color;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.text.TextUtils.TruncateAt;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.facebook.android.R.layout;
import com.wcities.eventseeker.MainActivity;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingItemType;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.asynctask.LoadDateWiseMyEvents;
import com.wcities.eventseeker.asynctask.UserTracker;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Event.Attending;
import com.wcities.eventseeker.core.Schedule;
import com.wcities.eventseeker.interfaces.DateWiseEventListener;
import com.wcities.eventseeker.interfaces.EventListener;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.DateWiseEventList;
import com.wcities.eventseeker.viewdata.DateWiseEventList.EventListItem;
import com.wcities.eventseeker.viewdata.DateWiseEventList.LIST_ITEM_TYPE;

public class DateWiseMyEventListAdapter extends BaseAdapter implements DateWiseEventListener{

	private static final String TAG = DateWiseMyEventListAdapter.class
			.getName();

	private Context mContext;
	private BitmapCache bitmapCache;
	private DateWiseEventList dateWiseEvtList;
	private AsyncTask<Void, Void, List<Event>> loadDateWiseMyEvents;
	private int eventsAlreadyRequested;
	private boolean isMoreDataAvailable = true;
	private DateWiseMyEventListAdapterListener mListener;
	private int orientation;
	private LayoutParams lpImgEvtPort;

	public interface DateWiseMyEventListAdapterListener {
		public void loadEventsInBackground();
	}

	public DateWiseMyEventListAdapter(Context context,
			DateWiseEventList dateWiseEvtList,
			AsyncTask<Void, Void, List<Event>> loadDateWiseEvents,
			DateWiseMyEventListAdapterListener mListener) {
		mContext = context;
		bitmapCache = BitmapCache.getInstance();
		this.dateWiseEvtList = dateWiseEvtList;
		this.loadDateWiseMyEvents = loadDateWiseEvents;
		this.mListener = mListener;
		orientation = mContext.getResources().getConfiguration().orientation;

		DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
		int width = dm.widthPixels < dm.heightPixels ? dm.widthPixels
				: dm.heightPixels;
		int widthPort = width
				- (mContext.getResources().getDimensionPixelSize(
						R.dimen.tab_bar_margin_fragment_custom_tabs) * 2)
				- (mContext.getResources().getDimensionPixelSize(
						R.dimen.img_event_margin_fragment_my_events_list_item) * 2);
		lpImgEvtPort = new LayoutParams(widthPort, widthPort * 3 / 4);
		lpImgEvtPort.topMargin = lpImgEvtPort.leftMargin = lpImgEvtPort.rightMargin = mContext
				.getResources().getDimensionPixelSize(
						R.dimen.img_event_margin_fragment_my_events_list_item);
	}

	public void updateContext(Context context) {
		mContext = context;
		orientation = mContext.getResources().getConfiguration().orientation;
	}

	public void setLoadDateWiseEvents(AsyncTask<Void, Void, List<Event>> loadDateWiseMyEvents) {
		this.loadDateWiseMyEvents = loadDateWiseMyEvents;
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
			if (convertView == null
					|| convertView.getTag() != LIST_ITEM_TYPE.PROGRESS) {
				convertView = LayoutInflater.from(mContext).inflate(
						R.layout.list_progress_bar, null);
				convertView.setTag(LIST_ITEM_TYPE.PROGRESS);
			}

			if ((loadDateWiseMyEvents == null || loadDateWiseMyEvents
					.getStatus() == Status.FINISHED) && isMoreDataAvailable) {
				mListener.loadEventsInBackground();
			}

		} else if (getItemViewType(position) == LIST_ITEM_TYPE.CONTENT
				.ordinal()) {

			if (convertView == null
					|| convertView.getTag() != LIST_ITEM_TYPE.CONTENT) {
				convertView = LayoutInflater.from(mContext).inflate(
						R.layout.fragment_my_events_list_item, null);
				convertView.setTag(LIST_ITEM_TYPE.CONTENT);
			}

			final Event event = getItem(position).getEvent();
			((TextView) convertView.findViewById(R.id.txtEvtTitle))
					.setText(event.getName());

			if (event.getSchedule() != null) {
				Schedule schedule = event.getSchedule();

				if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
					if (schedule.getDates().get(0).isStartTimeAvailable()) {
						String[] timeInArray = ConversionUtil
								.getTimeInArray(schedule.getDates().get(0)
										.getStartDate());

						((TextView) convertView.findViewById(R.id.txtEvtTime))
								.setText(timeInArray[0]);
						((TextView) convertView
								.findViewById(R.id.txtEvtTimeAMPM)).setText(" "
								+ timeInArray[1]);
						convertView.findViewById(R.id.imgEvtTime)
								.setVisibility(View.VISIBLE);

					} else {
						((TextView) convertView.findViewById(R.id.txtEvtTime))
								.setText("");
						((TextView) convertView
								.findViewById(R.id.txtEvtTimeAMPM)).setText("");
						convertView.findViewById(R.id.imgEvtTime)
								.setVisibility(View.INVISIBLE);
					}
				}

				TextView txtEvtLocation = (TextView) convertView
						.findViewById(R.id.txtEvtLocation);
				if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
					txtEvtLocation.setMaxLines(1);
					txtEvtLocation.setEllipsize(TruncateAt.END);
				}
				txtEvtLocation.setText(schedule.getVenue().getName());
			}

			ImageView imgEvent = (ImageView) convertView
					.findViewById(R.id.imgEvent);
			if(!((EventSeekr) mContext.getApplicationContext()).isTablet()) {
				if (orientation == Configuration.ORIENTATION_PORTRAIT) {
					imgEvent.setLayoutParams(lpImgEvtPort);
				}
			}

			String key = event.getKey(ImgResolution.LOW);
			Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
			if (bitmap != null) {
				imgEvent.setImageBitmap(bitmap);

			} else {
				imgEvent.setImageBitmap(null);
				AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
				asyncLoadImg.loadImg(
						(ImageView) convertView.findViewById(R.id.imgEvent),
						ImgResolution.LOW, (AdapterView) parent, position,
						event);
			}

			LinearLayout lnrLayoutTickets = (LinearLayout) convertView
					.findViewById(R.id.lnrLayoutTickets);
			Button btnBuyTickets = (Button) convertView
					.findViewById(R.id.btnBuyTickets);
			final boolean doesBookingUrlExist = (event.getSchedule() != null
					&& !event.getSchedule().getBookingInfos().isEmpty() && event
					.getSchedule().getBookingInfos().get(0).getBookingUrl() != null) ? true
					: false;
			lnrLayoutTickets.setEnabled(doesBookingUrlExist);
			if (doesBookingUrlExist) {
				btnBuyTickets.setTextColor(mContext.getResources().getColor(
						color.black));
				btnBuyTickets.setCompoundDrawablesWithIntrinsicBounds(mContext
						.getResources().getDrawable(R.drawable.tickets_grey),
						null, null, null);

			} else {
				btnBuyTickets.setTextColor(mContext.getResources().getColor(
						R.color.btn_buy_tickets_disabled_txt_color));
				btnBuyTickets.setCompoundDrawablesWithIntrinsicBounds(mContext
						.getResources()
						.getDrawable(R.drawable.tickets_disabled), null, null,
						null);
			}
			lnrLayoutTickets.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					if (doesBookingUrlExist) {
						Intent browserIntent = new Intent(Intent.ACTION_VIEW,
								Uri.parse(event.getSchedule().getBookingInfos()
										.get(0).getBookingUrl()));
						mContext.startActivity(browserIntent);
					}
				}
			});

			final CheckBox chkBoxGoing = (CheckBox) convertView
					.findViewById(R.id.chkBoxGoing);
			final CheckBox chkBoxWantToGo = (CheckBox) convertView
					.findViewById(R.id.chkBoxWantToGo);
			updateAttendingChkBoxes(event, chkBoxGoing, chkBoxWantToGo);

			/*
			 * if (((EventSeekr) mContext.getApplicationContext()).isTablet()) {
			 * int leftPad = (int) mContext .getResources()
			 * .getDimensionPixelSize(
			 * R.dimen.chk_box_pad_fragment_my_events_list_item);
			 * chkBoxGoing.setPadding(leftPad, 0, 0, 0);
			 * chkBoxWantToGo.setPadding(leftPad, 0, 0, 0);
			 * 
			 * }
			 */
			chkBoxGoing.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					Attending attending = event.getAttending() == Attending.GOING ? Attending.NOT_GOING
							: Attending.GOING;
					event.setAttending(attending);
					updateAttendingChkBoxes(event, chkBoxGoing, chkBoxWantToGo);
					new UserTracker((EventSeekr) mContext
							.getApplicationContext(),
							UserTrackingItemType.event, event.getId(), event
									.getAttending().getValue(),
							UserTrackingType.Add).execute();
				}
			});

			chkBoxWantToGo.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					Attending attending = event.getAttending() == Attending.WANTS_TO_GO ? Attending.NOT_GOING
							: Attending.WANTS_TO_GO;
					event.setAttending(attending);
					updateAttendingChkBoxes(event, chkBoxGoing, chkBoxWantToGo);
					new UserTracker((EventSeekr) mContext
							.getApplicationContext(),
							UserTrackingItemType.event, event.getId(), event
									.getAttending().getValue(),
							UserTrackingType.Add).execute();
				}
			});

			convertView.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					((EventListener) mContext).onEventSelected(event);
				}
			});

		} else if (getItemViewType(position) == LIST_ITEM_TYPE.HEADER.ordinal()) {
			if (convertView == null
					|| convertView.getTag() != LIST_ITEM_TYPE.HEADER) {
				convertView = LayoutInflater
						.from(mContext)
						.inflate(
								R.layout.fragment_discover_by_category_list_item_header,
								null);
				convertView.setTag(LIST_ITEM_TYPE.HEADER);
			}
			((TextView) convertView.findViewById(R.id.txtDate))
					.setText(getItem(position).getDate());
			int visibility = (position == 0) ? View.INVISIBLE : View.VISIBLE;
			View view = convertView.findViewById(R.id.divider1);
			view.setVisibility(visibility);
			if (((EventSeekr) mContext.getApplicationContext()).isTablet()) {
				view.setBackgroundResource(R.color.dark_gray);
			}
		}

		return convertView;
	}

	private void updateAttendingChkBoxes(Event event, CheckBox chkBoxGoing,
			CheckBox chkBoxWantToGo) {
		switch (event.getAttending()) {

		case GOING:
			chkBoxGoing.setChecked(true);
			chkBoxWantToGo.setChecked(false);
			break;

		case WANTS_TO_GO:
			chkBoxGoing.setChecked(false);
			chkBoxWantToGo.setChecked(true);
			break;

		case NOT_GOING:
			chkBoxGoing.setChecked(false);
			chkBoxWantToGo.setChecked(false);
			break;

		default:
			break;
		}
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
