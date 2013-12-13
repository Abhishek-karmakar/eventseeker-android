package com.wcities.eventseeker;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wcities.eventseeker.EventDetailsFragment.EventDetailsFragmentChildListener;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingItemType;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.asynctask.AsyncLoadImg.AsyncLoadImageListener;
import com.wcities.eventseeker.asynctask.UserTracker;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Address;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Event.Attending;
import com.wcities.eventseeker.core.Friend;
import com.wcities.eventseeker.core.Schedule;
import com.wcities.eventseeker.custom.view.ExpandableGridView;
import com.wcities.eventseeker.interfaces.ReplaceFragmentListener;
import com.wcities.eventseeker.interfaces.VenueListener;
import com.wcities.eventseeker.util.FragmentUtil;

public class EventInfoFragment extends Fragment implements OnClickListener, EventDetailsFragmentChildListener, 
		AsyncLoadImageListener {
	
	private static final String TAG = EventInfoFragment.class.getName();

	private static final int MAX_FRIENDS_GRID = 3;
	private static final int MAX_LINES_EVENT_DESC_PORTRAIT = 3;
	private static final int MAX_LINES_EVENT_DESC_LANDSCAPE = 5;

	private Event event;
	private boolean isEvtDescExpanded, isFriendsGridExpanded;
	private int orientation;
	private boolean allDetailsLoaded, isImgLoaded;
	private String wcitiesId;

	private Resources res;
	private ProgressBar progressBar, progressBar2;
	private ImageView imgEvt;
	private ExpandableGridView grdVFriends;
	private TextView txtViewAll;
	private RelativeLayout rltLayoutEvtDesc, rltLayoutLoadedContent;
	private TextView txtEvtDesc, txtAddress, txtEvtTime;
	private ImageView imgDown, imgRight;
	private RelativeLayout rltLayoutFriends, rltLayoutAddress;
	private LinearLayout lnrLayoutTickets;
	private CheckBox chkBoxGoing, chkBoxWantToGo;
	private Button btnBuyTickets;
	private View vDummyAddress;
	
	private FriendsGridAdapter friendsGridAdapter;
	
	private boolean isTablet;

	private TextView txtVenue;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		event = (Event) getArguments().getSerializable(BundleKeys.EVENT);
		wcitiesId = ((EventSeekr)FragmentUtil.getActivity(this).getApplication()).getWcitiesId();
		//Log.d(TAG, "lat = " + event.getSchedule().getVenue().getAddress().getLat() + ", lon = " + event.getSchedule().getVenue().getAddress().getLon());
		res = getResources();
		
		isTablet = ((MainActivity)FragmentUtil.getActivity(this)).isTablet();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		//Log.d(TAG, "onCreateView() - " + (System.currentTimeMillis() / 1000));
		orientation = res.getConfiguration().orientation;
		
		View v = inflater.inflate(R.layout.fragment_event_info, null);
		
		progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
		rltLayoutLoadedContent = (RelativeLayout) v.findViewById(R.id.rltLayoutLoadedContent);
		if (orientation == Configuration.ORIENTATION_PORTRAIT) {
			progressBar2 = (ProgressBar) v.findViewById(R.id.progressBar2);
		}
		
		imgEvt = (ImageView) v.findViewById(R.id.imgEvt);
		
		updateEventImg();
		
		((TextView)v.findViewById(R.id.txtEvtTitle)).setText(event.getName());
		
		if(isTablet) {
			
			txtVenue = (TextView)v.findViewById(R.id.txtVenue);
			txtVenue.setText(event.getSchedule().getVenue().getName());
			txtVenue.setOnClickListener(this);
		}
		
		lnrLayoutTickets = (LinearLayout) v.findViewById(R.id.lnrLayoutTickets);
		btnBuyTickets = (Button) v.findViewById(R.id.btnBuyTickets);
		lnrLayoutTickets.setOnClickListener(this);
		
		txtEvtTime = (TextView) v.findViewById(R.id.txtEvtTime);
		rltLayoutEvtDesc = (RelativeLayout) v.findViewById(R.id.rltLayoutDesc);
		txtEvtDesc = (TextView) v.findViewById(R.id.txtDesc);
		imgDown = (ImageView) v.findViewById(R.id.imgDown);
		
		if(isTablet) {
		
			imgDown.setVisibility(View.GONE);
			expandEvtDesc();
			
		}
			
		updateDescVisibility();
			
		rltLayoutAddress = (RelativeLayout) v.findViewById(R.id.rltLayoutAddress);

		txtAddress = (TextView) v.findViewById(R.id.txtAddress);
		vDummyAddress = v.findViewById(R.id.vDummyAddress);

		updateEventScheduleVisibility();
		
		grdVFriends = (ExpandableGridView) v.findViewById(R.id.grdVFriends);
		if (friendsGridAdapter == null) {
			friendsGridAdapter = new FriendsGridAdapter();
		}
		grdVFriends.setAdapter(friendsGridAdapter);
		
		rltLayoutFriends = (RelativeLayout) v.findViewById(R.id.rltLayoutFriends);
		txtViewAll = (TextView) v.findViewById(R.id.txtViewAll);
		imgRight = (ImageView) v.findViewById(R.id.imgRight);
		
		updateFriendsVisibility();
		
		chkBoxGoing = (CheckBox) v.findViewById(R.id.chkBoxGoing);
		chkBoxWantToGo = (CheckBox) v.findViewById(R.id.chkBoxWantToGo);

		if (wcitiesId != null) {
			chkBoxGoing.setOnClickListener(this);
			chkBoxWantToGo.setOnClickListener(this);
			
			updateAttendingChkBoxes();
			
		} else {
			chkBoxGoing.setEnabled(false);
			chkBoxWantToGo.setEnabled(false);
		}
		
		return v;
	}
	
	private void updateEventScheduleVisibility() {
		Schedule schedule = event.getSchedule();
		if (isImgLoaded && allDetailsLoaded && schedule != null) {
			rltLayoutAddress.setVisibility(View.VISIBLE);
			
			if (schedule.getDates().size() > 0) {
				com.wcities.eventseeker.core.Date date = schedule.getDates().get(0);
				
				DateFormat dateFormat;
				
				if(isTablet) {
					dateFormat = date.isStartTimeAvailable() ? new SimpleDateFormat("EEEE MMMM d, h:mm a") :
						new SimpleDateFormat("EEEE MMMM d");
				} else {
					dateFormat = date.isStartTimeAvailable() ? new SimpleDateFormat("MMMM dd, yyyy h:mm a") :
						new SimpleDateFormat("MMMM dd, yyyy");
				}				
				
				txtEvtTime.setText(dateFormat.format(date.getStartDate()));
				txtEvtTime.setSelected(true);
				
			}
			
			if (schedule.getBookingInfos().isEmpty()) {
				updateBtnBuyTicketsEnabled(false);
			} 
			
			if (isTablet) {
				txtAddress.setText(updateAddressTxt(event.getSchedule().getVenue().getAddress()));
			} else {
				txtAddress.setText(event.getSchedule().getVenue().getName());
			}			
			txtAddress.setOnClickListener(this);

			AddressMapFragment fragment = (AddressMapFragment) getChildFragmentManager().findFragmentByTag(
					AppConstants.FRAGMENT_TAG_ADDRESS_MAP);
	        if (fragment == null) {
	        	addAddressMapFragment();
	        }
        	vDummyAddress.setOnClickListener(this);
        	vDummyAddress.bringToFront();
			
		} else {
			int visibility = (orientation == Configuration.ORIENTATION_PORTRAIT) ? View.GONE : View.INVISIBLE;
			rltLayoutAddress.setVisibility(visibility);
			updateBtnBuyTicketsEnabled(false);
		}
	}
	
	private String updateAddressTxt(Address adrs) {
		String address = "";
		if (adrs != null) {
			if (adrs.getAddress1() != null) {
				address += adrs.getAddress1();
			}
			
			if (adrs.getCity() != null) {
				if (!address.isEmpty()) {
					address += ", ";
				}
				address += adrs.getCity();
			} 
			
			if (adrs.getCountry() != null) {
				if (!address.isEmpty()) {
					address += ", ";
				}
				address += adrs.getCountry().getName();
			} 
		}
		return address;
	}
	
	private void updateEventImg() {
		//Log.d(TAG, "updateEventImg(), url = " + event.getLowResImgUrl());
		if (event.doesValidImgUrlExist() || allDetailsLoaded) {
			String key = event.getKey(ImgResolution.LOW);
	        BitmapCache bitmapCache = BitmapCache.getInstance();
			Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
			if (bitmap != null) {
		        imgEvt.setImageBitmap(bitmap);
		        isImgLoaded = true;
		        
		    } else {
		    	imgEvt.setImageBitmap(null);
		    	AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
		        asyncLoadImg.loadImg(imgEvt, ImgResolution.LOW, event, this);
		        isImgLoaded = false;
		    }
		}
		updateProgressBarVisibility();
	}
	
	private void updateProgressBarVisibility() {
		if (isImgLoaded) {
			rltLayoutLoadedContent.setVisibility(View.VISIBLE);
			progressBar.setVisibility(View.GONE);
			
			if (progressBar2 != null) {
				if (allDetailsLoaded) {
					progressBar2.setVisibility(View.GONE);
					
				} else {
					progressBar2.setVisibility(View.VISIBLE);
				}
			}
			
		} else {
			rltLayoutLoadedContent.setVisibility(View.GONE);
			progressBar.setVisibility(View.VISIBLE);
		}
	}
	
	private void makeDescVisible() {
		rltLayoutEvtDesc.setVisibility(View.VISIBLE);
		imgDown.setOnClickListener(this);

		txtEvtDesc.setText(event.getDescription());

		if (isEvtDescExpanded) {
			expandEvtDesc();
			
		} else {
			collapseEvtDesc();
		}
	}
	
	private void updateDescVisibility() {

		if (isTablet) {

			if (event.getDescription() == null) {

				rltLayoutEvtDesc.setVisibility(View.GONE);

			} else {
				makeDescVisible();
			}

		} else if (orientation == Configuration.ORIENTATION_PORTRAIT) {

			if (!isImgLoaded || !allDetailsLoaded
					|| event.getDescription() == null) {
				rltLayoutEvtDesc.setVisibility(View.GONE);

			} else {
				makeDescVisible();
			}

		} else {
			if (event.getDescription() == null) {
				
				/**
				 * For landscape orientation, since friends section height
				 * depends on description section height or minHeight, we don't
				 * make description section visibility GONE. Rather keep it
				 * invisible keeping space occupied but components remaining
				 * hidden.
				 */
				rltLayoutEvtDesc.setVisibility(View.INVISIBLE);

			} else {
				makeDescVisible();
			}
		}
	}
		
	private void updateBtnBuyTicketsEnabled(boolean enabled) {
		lnrLayoutTickets.setEnabled(enabled);
		if (enabled) {
			if(isTablet) {
				btnBuyTickets.setTextColor(res.getColor(R.color.txt_color_include_fragment_event_details_footer_tab));
				btnBuyTickets.setCompoundDrawablesWithIntrinsicBounds(res.getDrawable(R.drawable.tic_blue), null, null, null);				
			} else {
				btnBuyTickets.setTextColor(res.getColor(android.R.color.white));
				btnBuyTickets.setCompoundDrawablesWithIntrinsicBounds(res.getDrawable(R.drawable.tickets), null, null, null);
			}
		} else {
			btnBuyTickets.setTextColor(res.getColor(R.color.btn_buy_tickets_disabled_txt_color));
			btnBuyTickets.setCompoundDrawablesWithIntrinsicBounds(res.getDrawable(R.drawable.tickets_disabled), null, null, 
					null);
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
	
	private void updateFriendsVisibility() {
		if (wcitiesId == null) {
			rltLayoutFriends.setVisibility(View.GONE);

		} else if (isTablet) {

			if (event.getFriends().isEmpty()) {
				rltLayoutFriends.setVisibility(View.GONE);
			} else {
				rltLayoutFriends.setVisibility(View.VISIBLE);

				if (event.getFriends().size() > MAX_FRIENDS_GRID) {
					RelativeLayout rltLayoutViewAll = 
							(RelativeLayout) rltLayoutFriends.findViewById(R.id.rltLayoutViewAll);
					rltLayoutViewAll.setVisibility(View.VISIBLE);
					rltLayoutViewAll.setOnClickListener(this);

					expandOrCollapseFriendsGrid();
				}
			}

		} else if (orientation == Configuration.ORIENTATION_PORTRAIT) {

			if (!isImgLoaded || !allDetailsLoaded || event.getFriends().isEmpty()) {

				rltLayoutFriends.setVisibility(View.GONE);

			} else {
				rltLayoutFriends.setVisibility(View.VISIBLE);

				if (event.getFriends().size() > MAX_FRIENDS_GRID) {
					RelativeLayout rltLayoutViewAll = 
							(RelativeLayout) rltLayoutFriends.findViewById(R.id.rltLayoutViewAll);
					rltLayoutViewAll.setVisibility(View.VISIBLE);
					rltLayoutViewAll.setOnClickListener(this);

					expandOrCollapseFriendsGrid();
				}
			}

		} else {
			if (event.getFriends().isEmpty()) {

				// Remove entire bottom part since neither description nor
				// friends are available
				if (event.getDescription() == null) {
					rltLayoutFriends.setVisibility(View.GONE);
					rltLayoutEvtDesc.setVisibility(View.GONE);

				} else {
					/**
					 * Use INVISIBLE instead of GONE to restrain description
					 * section space to half the screen width; otherwise it will
					 * expand horizontally to fill screen width
					 */
					rltLayoutFriends.setVisibility(View.INVISIBLE);
					rltLayoutEvtDesc.setVisibility(View.VISIBLE);
				}

			} else {
				rltLayoutFriends.setVisibility(View.VISIBLE);
			}
		}
	}
	
	private void expandOrCollapseFriendsGrid() {
		if (isFriendsGridExpanded) {
			txtViewAll.setText(res.getString(R.string.txt_view_less));
			imgRight.setImageResource(R.drawable.less);
			grdVFriends.setExpanded(isFriendsGridExpanded);
			
		} else {
			txtViewAll.setText(res.getString(R.string.txt_view_all));
			imgRight.setImageResource(R.drawable.down);
			grdVFriends.setExpanded(isFriendsGridExpanded);
		}
		
		friendsGridAdapter.notifyDataSetChanged();
	}
	
	private void addAddressMapFragment() {
    	FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        
        AddressMapFragment fragment = new AddressMapFragment();
        fragment.setArguments(getArguments());
        fragmentTransaction.add(R.id.frmLayoutMapContainer, fragment, AppConstants.FRAGMENT_TAG_ADDRESS_MAP);
        try {
        	fragmentTransaction.commit();
        	
        } catch (IllegalStateException e) {
        	/**
        	 * This catch is to prevent possible "java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState"
        	 * when it's called from callback method onEventUpdatedByEventDetailsFragment() & if user has already left this screen.
        	 */
			Log.e(TAG, "IllegalStateException: " + e.getMessage());
			e.printStackTrace();
		}
    } 
	
	private void expandEvtDesc() {
		txtEvtDesc.setMaxLines(Integer.MAX_VALUE);
		txtEvtDesc.setEllipsize(null);
		
		imgDown.setImageDrawable(res.getDrawable(R.drawable.less));

		isEvtDescExpanded = true;
	}
	
	private void collapseEvtDesc() {
		int maxLines = orientation == Configuration.ORIENTATION_PORTRAIT ? MAX_LINES_EVENT_DESC_PORTRAIT : 
			MAX_LINES_EVENT_DESC_LANDSCAPE;
		txtEvtDesc.setMaxLines(maxLines);
		txtEvtDesc.setEllipsize(TruncateAt.END);
		
		imgDown.setImageDrawable(res.getDrawable(R.drawable.down));
		
		isEvtDescExpanded = false;
	}
	
	private void updateAddress() {
		if (event.getSchedule() == null) {
			return;
		}
		
		Address address = event.getSchedule().getVenue().getAddress();
		AddressMapFragment fragment = (AddressMapFragment) getChildFragmentManager().findFragmentByTag(
				AppConstants.FRAGMENT_TAG_ADDRESS_MAP);
        if (address != null && fragment != null) {
        	if (address.getLat() != 0 && address.getLon() != 0) {
            	fragment.updateLatLon(address.getLat(), address.getLon());
        	} else {
        		fragment.updateAddress(address);
        	}
        }
	}
	
	private class FriendsGridAdapter extends BaseAdapter {
		
		private BitmapCache bitmapCache;
		
		public FriendsGridAdapter() {
			this.bitmapCache = BitmapCache.getInstance();
		}

		@Override
		public int getCount() {
			if (orientation == Configuration.ORIENTATION_PORTRAIT || isTablet) {
				return event.getFriends().size() > MAX_FRIENDS_GRID ? 
						(isFriendsGridExpanded ? event.getFriends().size() : MAX_FRIENDS_GRID) : event.getFriends().size();
				
			} else {
				return event.getFriends().size() > MAX_FRIENDS_GRID ? MAX_FRIENDS_GRID : event.getFriends().size();
			}
		}

		@Override
		public Friend getItem(int position) {
			return event.getFriends().get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			Friend friend = getItem(position);
			//Log.d(TAG, "friend = " + friend);
			
			if (friend == null) {
				convertView = LayoutInflater.from(FragmentUtil.getActivity(EventInfoFragment.this)).inflate(R.layout.list_progress_bar, null);
				
			} else {
				GridFriendHolder holder;

				if (convertView == null || convertView.getTag() == null) {
					convertView = LayoutInflater.from(FragmentUtil.getActivity(EventInfoFragment.this)).inflate(R.layout.grid_friend, null);
					
					holder = new GridFriendHolder();
					holder.imgFriend = (ImageView) convertView.findViewById(R.id.imgFriend);
					holder.txtFriendName = (TextView) convertView.findViewById(R.id.txtFriendName);
					
					convertView.setTag(holder);
					
				} else {
					holder = (GridFriendHolder) convertView.getTag();
				}
				
				holder.txtFriendName.setText(friend.getName());
				
				String key = friend.getKey(ImgResolution.LOW);
				Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
				if (bitmap != null) {
					holder.imgFriend.setImageBitmap(bitmap);
			        
			    } else {
			    	holder.imgFriend.setImageBitmap(null);
			        AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
			        asyncLoadImg.loadImg(holder.imgFriend, ImgResolution.LOW, friend);
			    }
			}
			return convertView;
		}
		
		private class GridFriendHolder {
			private TextView txtFriendName;
			private ImageView imgFriend;
		}
    }
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.imgDown:
			if (isEvtDescExpanded) {
				collapseEvtDesc();
				
			} else {
				expandEvtDesc();
			}
			break;
			
		case R.id.rltLayoutViewAll:
			isFriendsGridExpanded = !isFriendsGridExpanded;
			expandOrCollapseFriendsGrid();
			break;
			
		case R.id.lnrLayoutTickets:
			Bundle args = new Bundle();
			args.putString(BundleKeys.URL, event.getSchedule().getBookingInfos().get(0).getBookingUrl());
			((ReplaceFragmentListener)FragmentUtil.getActivity(this)).replaceByFragment(
					AppConstants.FRAGMENT_TAG_TICKET_PROVIDERS, args);
			break;
			
		case R.id.chkBoxGoing:
			Attending attending = event.getAttending() == Attending.GOING ? Attending.NOT_GOING : Attending.GOING;
			event.setAttending(attending);
			updateAttendingChkBoxes();
			new UserTracker((EventSeekr) FragmentUtil.getActivity(this).getApplication(), UserTrackingItemType.event, event.getId(), 
					event.getAttending().getValue(), UserTrackingType.Add).execute();
			((EventDetailsFragment) getParentFragment()).onEventAttendingUpdated();
			break;
			
		case R.id.chkBoxWantToGo:
			attending = event.getAttending() == Attending.WANTS_TO_GO ? Attending.NOT_GOING : Attending.WANTS_TO_GO;
			event.setAttending(attending);
			updateAttendingChkBoxes();
			new UserTracker((EventSeekr) FragmentUtil.getActivity(this).getApplication(), UserTrackingItemType.event, event.getId(), 
					event.getAttending().getValue(), UserTrackingType.Add).execute();
			((EventDetailsFragment) getParentFragment()).onEventAttendingUpdated();
			break;
			
		case R.id.txtAddress:
		case R.id.txtVenue:
		case R.id.vDummyAddress:
			((VenueListener)FragmentUtil.getActivity(this)).onVenueSelected(event.getSchedule().getVenue());
			break;
			
		default:
			break;
		}
	}
	
	private void updateScreen() {
		// if fragment is destroyed (user has pressed back)
		if (FragmentUtil.getActivity(this) == null) {
			return;
		}
		
		updateEventImg();
		updateDescVisibility();
		updateEventScheduleVisibility();
		updateAddress();
		
		updateFriendsVisibility();
		friendsGridAdapter.notifyDataSetChanged();
		
		if (event.getSchedule() != null && !event.getSchedule().getBookingInfos().isEmpty()) {
			updateBtnBuyTicketsEnabled(true);
		}
		updateAttendingChkBoxes();	
	}

	@Override
	public void onEventUpdatedByEventDetailsFragment() {
		Log.d(TAG, "onEventUpdatedByEventDetailsFragment()");
		allDetailsLoaded = true;
		updateScreen();
	}
	
	@Override
	public void onEventAttendingUpdated() {
		updateAttendingChkBoxes();	
	}

	@Override
	public void onImageLoaded() {
		Log.d(TAG, "onImageLoaded()");
		isImgLoaded = true;
		updateScreen();
	}

	@Override
	public void onImageCouldNotBeLoaded() {
		isImgLoaded = true;
		updateScreen();
	}
}
