package com.wcities.eventseeker;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.wcities.eventseeker.EventDetailsFragment1.EventDetailsFragmentChildListener;
import com.wcities.eventseeker.adapter.ArtistListAdapter;
import com.wcities.eventseeker.analytics.GoogleAnalyticsTracker;
import com.wcities.eventseeker.analytics.IGoogleAnalyticsTracker;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingItemType;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.UserTracker;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Event.Attending;
import com.wcities.eventseeker.core.Schedule;
import com.wcities.eventseeker.custom.fragment.PublishEventListFragment;
import com.wcities.eventseeker.interfaces.ReplaceFragmentListener;
import com.wcities.eventseeker.util.FbUtil;
import com.wcities.eventseeker.util.FragmentUtil;

import java.util.ArrayList;
import java.util.List;

public class EventFeaturingFragment extends PublishEventListFragment implements OnClickListener, 
		EventDetailsFragmentChildListener {
	
	private static final String TAG = EventFeaturingFragment.class.getName();
	private static final int MAX_FB_CALL_COUNT_FOR_SAME_EVT = 20;
	
	private View lnrLayoutTickets;
	private CheckBox chkBoxGoing, chkBoxWantToGo;
	private Button btnBuyTickets;
	private ArtistListAdapter<Void> artistListAdapter;
	private List<Artist> artistList;
	private ImageView imgBuyTickets;
	private TextView txtBuyTickets;
	private boolean isTablet;
	private int fbCallCountForSameEvt = 0;
	private Resources res;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		event = (Event) getArguments().getSerializable(BundleKeys.EVENT);
		isTablet = ((MainActivity)FragmentUtil.getActivity(this)).isTablet();
		res = getResources(); 
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_event_featuring, null);
		
		lnrLayoutTickets = v.findViewById(R.id.lnrLayoutTickets);
		if(isTablet) {
			imgBuyTickets =  (ImageView) v.findViewById(R.id.imgBuyTickets);
			txtBuyTickets =  (TextView) v.findViewById(R.id.txtBuyTickets);
		} else {
			btnBuyTickets = (Button) v.findViewById(R.id.btnBuyTickets);
		}
		lnrLayoutTickets.setOnClickListener(this);
		
		updateEventScheduleVisibility();
		
		chkBoxGoing = (CheckBox) v.findViewById(R.id.chkBoxGoing);
		chkBoxWantToGo = (CheckBox) v.findViewById(R.id.chkBoxWantToGo);

		chkBoxGoing.setOnClickListener(this);
		chkBoxWantToGo.setOnClickListener(this);
		
		updateAttendingChkBoxes();
		
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if (artistList == null) {
			artistList = new ArrayList<Artist>();
			artistList.addAll(event.getArtists());
			if (artistList.isEmpty()) {
				// add dummy item to indicate loading progress
				artistList.add(null);
			}
			artistListAdapter = new ArtistListAdapter<Void>(FragmentUtil.getActivity(this), artistList, null, null);
			artistListAdapter.setMoreDataAvailable(false);
			
		} else {
			artistListAdapter.updateContext(FragmentUtil.getActivity(this));
		}
		
		setListAdapter(artistListAdapter);
        getListView().setDivider(null);
	}
	
	private void updateBtnBuyTicketsEnabled(boolean enabled) {
		lnrLayoutTickets.setEnabled(enabled);

		if (enabled) {
			if (isTablet) {
				txtBuyTickets.setTextColor(res.getColor(android.R.color.white));
				imgBuyTickets.setImageDrawable(res.getDrawable(R.drawable.tickets));
			} else {
				btnBuyTickets.setTextColor(res.getColor(android.R.color.white));
				btnBuyTickets.setCompoundDrawablesWithIntrinsicBounds(
						res.getDrawable(R.drawable.tickets), null, null, null);
			}
		} else {
			if (isTablet) {
				txtBuyTickets.setTextColor(res.getColor(R.color.btn_buy_tickets_disabled_txt_color));
				imgBuyTickets.setImageDrawable(res.getDrawable(R.drawable.tickets_disabled));
			} else {
				btnBuyTickets.setTextColor(res.getColor(R.color.btn_buy_tickets_disabled_txt_color));
				btnBuyTickets.setCompoundDrawablesWithIntrinsicBounds(
						res.getDrawable(R.drawable.tickets_disabled), null,null, null);
			}
		}
	}
	
	private void updateEventScheduleVisibility() {
		Schedule schedule = event.getSchedule();
		if (schedule == null || schedule.getBookingInfos().isEmpty()) {
			updateBtnBuyTicketsEnabled(false);
		}
	}
	
	private void updateAttendingChkBoxes() {
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

		default:
			break;
		}
	}
	
	private void onChkBoxClicked(CheckBox chkBox, Attending newAttending) {
		EventSeekr eventSeekr = (EventSeekr) FragmentUtil.getActivity(EventFeaturingFragment.this).getApplication();

		if (eventSeekr.getWcitiesId() != null) {
			/**
			 * call to updateAttendingChkBoxes() to negate the click event for now on checkbox, 
			 * since it's handled after checking fb publish permission
			 */
			//Log.d(TAG, "call updateAttendingChkBoxes");
			updateAttendingChkBoxes();
			
			if (newAttending == Attending.NOT_GOING) {
				event.setAttending(newAttending);
				new UserTracker(Api.OAUTH_TOKEN, eventSeekr, UserTrackingItemType.event, event.getId(), event.getAttending().getValue(), 
                		UserTrackingType.Add).execute();
    			((EventDetailsFragment1) getParentFragment()).onEventAttendingUpdated();
				
			} else {
				if (eventSeekr.getFbUserId() != null) {
					fbCallCountForSameEvt = 0;
					event.setNewAttending(newAttending);
					FbUtil.handlePublishEvent(this, this, AppConstants.PERMISSIONS_FB_PUBLISH_EVT_OR_ART, event);
					
				} else if (eventSeekr.getGPlusUserId() != null) {
					event.setNewAttending(newAttending);
					handlePublishEvent();
				}
			}
			
		} else {
			chkBox.setChecked(false);
			Toast.makeText(FragmentUtil.getActivity(this), getResources().getString(R.string.pls_login_to_track_evt), 
					Toast.LENGTH_LONG).show();
		}
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.lnrLayoutTickets:
			Bundle args = new Bundle();
			args.putString(BundleKeys.URL, event.getSchedule().getBookingInfos().get(0).getBookingUrl());
			((ReplaceFragmentListener)FragmentUtil.getActivity(this)).replaceByFragment(
					AppConstants.FRAGMENT_TAG_WEB_VIEW, args);
			/**
			 * added on 15-12-2014
			 */
			GoogleAnalyticsTracker.getInstance().sendEvent(FragmentUtil.getApplication(this), 
					((IGoogleAnalyticsTracker)getParentFragment()).getScreenName(), 
					GoogleAnalyticsTracker.EVENT_LABEL_TICKETS_BUTTON, 
					com.wcities.eventseeker.analytics.GoogleAnalyticsTracker.Type.Event.name(), null, event.getId());
			break;
			
		case R.id.chkBoxGoing:
			Attending newAttending = event.getAttending() == Attending.GOING ? Attending.NOT_GOING : Attending.GOING;
			onChkBoxClicked(chkBoxGoing, newAttending);
			break;
			
		case R.id.chkBoxWantToGo:
			newAttending = event.getAttending() == Attending.WANTS_TO_GO ? Attending.NOT_GOING : Attending.WANTS_TO_GO;
			onChkBoxClicked(chkBoxWantToGo, newAttending);
			break;
			
		default:
			break;
		}
	}

	@Override
	public void onEventUpdatedByEventDetailsFragment() {
		Log.d(TAG, "onEventUpdatedByEventDetailsFragment()");
		updateEventScheduleVisibility();

		/**
		 * It's possible that initially when we added all the artists in onActivityCreated() there are artists but not with 
		 * all data (including image url), hence we refill it here.
		 */
		artistList.clear();
		artistList.addAll(event.getArtists());
		if(artistList.isEmpty()) {
			artistList.add(new Artist(AppConstants.INVALID_ID, null));
		}
		artistListAdapter.notifyDataSetChanged();
		
		if (event.getSchedule() != null && !event.getSchedule().getBookingInfos().isEmpty()) {
			updateBtnBuyTicketsEnabled(true);
		}
		updateAttendingChkBoxes();		
	}

	@Override
	public void onEventAttendingUpdated() {
		updateAttendingChkBoxes();	
	}
	
	/*@Override
	public void call(Session session, SessionState state, Exception exception) {
		//Log.d(TAG, "call()");
		fbCallCountForSameEvt++;
		*//**
		 * To prevent infinite loop when network is off & we are calling requestPublishPermissions() of FbUtil.
		 *//*
		if (fbCallCountForSameEvt < MAX_FB_CALL_COUNT_FOR_SAME_EVT) {
			FbUtil.call(session, state, exception, this, this, AppConstants.PERMISSIONS_FB_PUBLISH_EVT_OR_ART, AppConstants.REQ_CODE_FB_PUBLISH_EVT_OR_ART, 
					event);
			
		} else {
			fbCallCountForSameEvt = 0;
			setPendingAnnounce(false);
		}
	}*/

	@Override
	public void onPublishPermissionGranted() {
		((EventDetailsFragment1)getParentFragment()).onEventAttendingUpdated();
	}

	@Override
	public void onSuccess(LoginResult loginResult) {

	}

	@Override
	public void onCancel() {

	}

	@Override
	public void onError(FacebookException e) {

	}
}
