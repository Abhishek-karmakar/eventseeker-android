package com.wcities.eventseeker;

import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Date;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Schedule;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.VersionUtil;
import com.wcities.eventseeker.util.ViewUtil;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class EventDetailsFragmentTab extends Fragment {

	private Event event;
	
	private ImageView imgEvt;
	private TextView txtEvtTitle, txtEvtTime;
	
	private int limitScrollAt, actionBarElevation;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setHasOptionsMenu(true);
		setRetainInstance(true);
		
		actionBarElevation = FragmentUtil.getResources(this).getDimensionPixelSize(R.dimen.action_bar_elevation);
		
		Bundle args = getArguments();
		if (event == null) {
			//Log.d(TAG, "event = null");
			event = (Event) args.getSerializable(BundleKeys.EVENT);
			event.getFriends().clear();
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_event_details_tab, container, false);
		
		imgEvt = (ImageView) rootView.findViewById(R.id.imgEvt);
		txtEvtTitle = (TextView) rootView.findViewById(R.id.txtEvtTitle);
		txtEvtTitle.setText(event.getName());
		// for marquee to work
		txtEvtTitle.setSelected(true);
		ViewCompat.setTransitionName(txtEvtTitle, getArguments().getString(BundleKeys.TRANSITION_NAME_SHARED_TEXT));
		
		txtEvtTime = (TextView) rootView.findViewById(R.id.txtEvtTime);
		updateEventSchedule();
		
		updateEventImg();
		ViewCompat.setTransitionName(imgEvt, getArguments().getString(BundleKeys.TRANSITION_NAME_SHARED_IMAGE));
		
		BaseActivityTab baseActivityTab = (BaseActivityTab) FragmentUtil.getActivity(this);
		baseActivityTab.updateTitle("Title title");
		baseActivityTab.updateSubTitle("Subtitle subtitle");
		return rootView;
	}
	
	private void calculateScrollLimit() {
		Resources res = FragmentUtil.getResources(this);
		limitScrollAt = res.getDimensionPixelSize(R.dimen.img_evt_ht_event_details_tab) - res.getDimensionPixelSize(
				R.dimen.floating_double_line_toolbar_ht);
		
		int actionBarTitleTextSize = res.getDimensionPixelSize(R.dimen.abc_text_size_title_material_toolbar);
		int txtEvtTitleTextSize = res.getDimensionPixelSize(R.dimen.txt_evt_title_txt_size_event_details);
		/*minTitleScale = actionBarTitleTextSize / (float) txtEvtTitleTextSize;
		
		int txtEvtTitleDestinationX = res.getDimensionPixelSize(R.dimen.txt_toolbar_title_pos_all_details);
		txtEvtTitleSourceX = res.getDimensionPixelSize(R.dimen.rlt_lyt_txt_evt_title_pad_l_event_details);
		
		txtEvtTitleDiffX = txtEvtTitleDestinationX - txtEvtTitleSourceX;*/
	}
	
	private void updateEventImg() {
		//Log.d(TAG, "updateEventImg(), url = " + event.getLowResImgUrl());
		if (event.doesValidImgUrlExist()) {
			//Log.d(TAG, "updateEventImg(), ValidImgUrlExist");
			String key = event.getKey(ImgResolution.LOW);
	        BitmapCache bitmapCache = BitmapCache.getInstance();
			Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
			if (bitmap != null) {
				imgEvt.setImageBitmap(bitmap);
		        
		    } else {
		    	imgEvt.setImageBitmap(null);
		    	AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
		        asyncLoadImg.loadImg(imgEvt, ImgResolution.LOW, event);
		    }
		}
	}
	
	private void updateEventSchedule() {
		Schedule schedule = event.getSchedule();
		if (schedule != null) {
			if (schedule.getVenue() != null) {
				/*txtEvtLoc.setText(event.getSchedule().getVenue().getName());
				txtEvtLoc.setOnClickListener(this);
				txtVenue.setText(event.getSchedule().getVenue().getName());
				txtVenue.setOnClickListener(this);*/
			}
			
			if (schedule.getDates().size() > 0) {
				Date date = schedule.getDates().get(0);
				txtEvtTime.setText(ConversionUtil.getDateTime(date.getStartDate(), date.isStartTimeAvailable()));
			}
		}
	}
}
