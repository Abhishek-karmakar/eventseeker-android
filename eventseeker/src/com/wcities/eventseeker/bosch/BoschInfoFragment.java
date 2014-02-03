package com.wcities.eventseeker.bosch;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Bundle;
import android.text.TextUtils.TruncateAt;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.bosch.BoschMainActivity.OnCarStationaryStatusChangedListener;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.util.FragmentUtil;

public class BoschInfoFragment extends FragmentLoadableFromBackStack implements View.OnClickListener, 
		OnCarStationaryStatusChangedListener {

	private static final int SCROLL_Y_BY = 100;
	private Artist artist;
	private Event event;
	private Venue venue;
	
	private ScrollView scrlContent;
	private TextView txtDescription;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getArguments().containsKey(BundleKeys.ARTIST)) {
			artist = (Artist) getArguments().getSerializable(BundleKeys.ARTIST);
		} else if (getArguments().containsKey(BundleKeys.EVENT)) {
			event = (Event) getArguments().getSerializable(BundleKeys.EVENT);
		} else if (getArguments().containsKey(BundleKeys.VENUE)) {
			venue = (Venue) getArguments().getSerializable(BundleKeys.VENUE);
		}
		
		((BoschMainActivity) FragmentUtil.getActivity(this)).registerOnCarStationaryStatusChangedListener(this);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		
		View view = inflater.inflate(R.layout.fragment_bosch_info, null);
		
		scrlContent = (ScrollView) view.findViewById(R.id.scrlContent);

		view.findViewById(R.id.btnUp).setOnClickListener(this);
		view.findViewById(R.id.btnDown).setOnClickListener(this);
				
		String description = null, address = null;
		com.wcities.eventseeker.core.Date date = null;
		
		if (artist != null) {
			
			((TextView) view.findViewById(R.id.txtAddressLabel)).setVisibility(View.GONE);
			((TextView) view.findViewById(R.id.txtAddress)).setVisibility(View.GONE);
			
			description = artist.getDescription();
		
		} else {
			if (event != null) {
				date = event.getSchedule().getDates().get(0);
				address = event.getSchedule().getVenue().getFormatedAddress();
				description = event.getDescription();

				if (description == null) {
					description = event.getSchedule().getVenue().getLongDesc();
				}
			
			} else if (venue != null) {
				address = venue.getFormatedAddress();
				description = venue.getLongDesc();
			}
			((TextView) view.findViewById(R.id.txtAddress)).setText(address);
		}
		
		if (date == null) {
			((TextView) view.findViewById(R.id.txtDate)).setVisibility(View.GONE);
		
		} else {
			SimpleDateFormat sdf = date.isStartTimeAvailable() ? new SimpleDateFormat("EEEE MMMM d, h:mm a") :
				new SimpleDateFormat("EEEE MMMM d");
			((TextView) view.findViewById(R.id.txtDate)).setText(sdf.format(date.getStartDate()));
		}
		
		if (description == null) {
			description = "Description Unavailable";
		}
		
		txtDescription = (TextView) view.findViewById(R.id.txtDescription);
		txtDescription.setText(description);
		updateDescriptionLines();
		
		return view;
	}

	@Override
	public void onResume() {
		String title;
		if (artist != null) {
			title = artist.getName();
		} else if (event != null) {
			title = event.getName();
		} else {
			title = venue.getName();
		}
		
		super.onResume(AppConstants.INVALID_INDEX, title);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		((BoschMainActivity) FragmentUtil.getActivity(this)).unRegisterOnCarStationaryStatusChangedListener();
	}
	
	private void updateDescriptionLines() {
		if (AppConstants.IS_CAR_STATIONARY) {
			txtDescription.setMaxLines(Integer.MAX_VALUE);
			txtDescription.setEllipsize(null);

		} else {
			txtDescription.setMaxLines(2);
			txtDescription.setEllipsize(TruncateAt.END);
		}
	}
	
	@Override
	public void onClick(View v) {
		
		switch (v.getId()) {

			case R.id.btnUp:
				scrlContent.scrollBy(0, -1 * SCROLL_Y_BY);
				break;

			case R.id.btnDown:
				scrlContent.scrollBy(0, SCROLL_Y_BY);
				break;
		
		}
		
	}

	@Override
	public void onCarStationaryStatusChanged(boolean isStationary) {
		updateDescriptionLines();
	}
	
}
