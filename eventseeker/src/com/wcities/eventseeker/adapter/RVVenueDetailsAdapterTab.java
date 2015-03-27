package com.wcities.eventseeker.adapter;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.text.Html;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.Session;
import com.facebook.SessionState;
import com.wcities.eventseeker.AddressMapFragment;
import com.wcities.eventseeker.BaseActivityTab;
import com.wcities.eventseeker.DiscoverActivityTab;
import com.wcities.eventseeker.NavigationActivityTab;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.ShareViaDialogFragment;
import com.wcities.eventseeker.VenueDetailsFragmentTab;
import com.wcities.eventseeker.WebViewActivityTab;
import com.wcities.eventseeker.analytics.GoogleAnalyticsTracker;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingItemType;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.asynctask.LoadEvents;
import com.wcities.eventseeker.asynctask.UserTracker;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.core.Date;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Event.Attending;
import com.wcities.eventseeker.core.Schedule;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener;
import com.wcities.eventseeker.interfaces.EventListenerTab;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.FbUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class RVVenueDetailsAdapterTab extends Adapter<RVVenueDetailsAdapterTab.ViewHolder> implements 
		DateWiseEventParentAdapterListener {
	
	private static final String TAG = RVVenueDetailsAdapterTab.class.getSimpleName();

	private static final int EXTRA_TOP_DUMMY_ITEM_COUNT = 2;
	private static final int EXTRA_TOP_DUMMY_ITEM_COUNT_AFTER_DETAILS_LOADED = 4;
	private static final int MAX_LINES_VENUE_DESC = 3;
	
	private RecyclerView recyclerView;
	
	private VenueDetailsFragmentTab venueDetailsFragmentTab;
	private List<Event> eventList;
	private Venue venue;
	private boolean isVenueDescExpanded;
	
	private LoadEvents loadEvents;
	private boolean isMoreDataAvailable = true;
	private int eventsAlreadyRequested;
	
	private BitmapCache bitmapCache;
	
	private int fbCallCountForSameEvt = 0;
	private RVVenueDetailsAdapterTab.ViewHolder holderPendingPublish;
	private Event eventPendingPublish;
	
	private static enum ViewType {
		IMG, DESC, ADDRESS_MAP, UPCOMING_EVENTS_TITLE, PROGRESS, EVENT;
		
		private static ViewType getViewType(int type) {
			ViewType[] viewTypes = ViewType.values();
			for (int i = 0; i < viewTypes.length; i++) {
				if (viewTypes[i].ordinal() == type) {
					return viewTypes[i];
				}
			}
			return null;
		}
	};

	static class ViewHolder extends RecyclerView.ViewHolder {
		
		private RelativeLayout rltLytPrgsBar, rltRootDesc;
		private TextView txtDesc;
		private ImageView imgDown;
		private View vHorLine;
		
		private TextView txtVenue;
		private ImageView fabPhone, fabNavigate;
		
		private ImageView imgEvt;
		private TextView txtEvtTitle, txtEvtTime, txtEvtLoc;
		private ImageView imgTicket, imgSave, imgShare;

		public ViewHolder(View itemView) {
			super(itemView);
			
			rltRootDesc = (RelativeLayout) itemView.findViewById(R.id.rltRootDesc);
			rltLytPrgsBar = (RelativeLayout) itemView.findViewById(R.id.rltLytPrgsBar);
			txtDesc = (TextView) itemView.findViewById(R.id.txtDesc);
			imgDown = (ImageView) itemView.findViewById(R.id.imgDown);
			vHorLine = itemView.findViewById(R.id.vHorLine);
			
			txtVenue = (TextView) itemView.findViewById(R.id.txtVenue);
			fabPhone = (ImageView) itemView.findViewById(R.id.fabPhone);
			fabNavigate = (ImageView) itemView.findViewById(R.id.fabNavigate);
			
			imgEvt = (ImageView) itemView.findViewById(R.id.imgEvt);
			txtEvtTitle = (TextView) itemView.findViewById(R.id.txtEvtTitle);
            txtEvtTime = (TextView) itemView.findViewById(R.id.txtEvtTime);
            txtEvtLoc = (TextView) itemView.findViewById(R.id.txtEvtLoc);
            imgTicket = (ImageView) itemView.findViewById(R.id.imgTicket);
            imgSave = (ImageView) itemView.findViewById(R.id.imgSave);
            imgShare = (ImageView) itemView.findViewById(R.id.imgShare);
		}
	}

	public RVVenueDetailsAdapterTab(VenueDetailsFragmentTab venueDetailsFragmentTab) {
		this.venueDetailsFragmentTab = venueDetailsFragmentTab;
		eventList = venueDetailsFragmentTab.getEventList();
		venue = venueDetailsFragmentTab.getVenue();
		
		bitmapCache = BitmapCache.getInstance();
	}

	@Override
	public int getItemCount() {
		return venueDetailsFragmentTab.isAllDetailsLoaded() ? (EXTRA_TOP_DUMMY_ITEM_COUNT_AFTER_DETAILS_LOADED + 
				eventList.size()) : EXTRA_TOP_DUMMY_ITEM_COUNT;
	}
	
	@Override
	public int getItemViewType(int position) {
		if (position == ViewType.IMG.ordinal()) {
			return ViewType.IMG.ordinal();
			
		} else if (position == ViewType.DESC.ordinal()) {
			return ViewType.DESC.ordinal();
			
		} else if (position == ViewType.ADDRESS_MAP.ordinal()) {
			return ViewType.ADDRESS_MAP.ordinal();
			
		} else if (position == ViewType.UPCOMING_EVENTS_TITLE.ordinal()) {
			return ViewType.UPCOMING_EVENTS_TITLE.ordinal();
			
		} else if (eventList.get(position - EXTRA_TOP_DUMMY_ITEM_COUNT_AFTER_DETAILS_LOADED) == null) {
			return ViewType.PROGRESS.ordinal();
			
		} else {
			return ViewType.EVENT.ordinal();
		}
	}
	
	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		//Log.d(TAG, "onCreateViewHolder(), viewType = " + viewType);
		View v;
		
		recyclerView = (RecyclerView) parent;
		
		switch (ViewType.getViewType(viewType)) {
		
		case IMG:
			v = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_item_img_venue_venue_details, 
					parent, false);
			break;
			
		case DESC:
			v = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_item_desc_tab, parent, false);
			break;
			
		case ADDRESS_MAP:
			v = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_item_address_map_venue_details, 
					parent, false);
			break;
			
		case UPCOMING_EVENTS_TITLE:
			v = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_item_upcoming_events_title_tab, 
					parent, false);
			break;
			
		case PROGRESS:
			v = LayoutInflater.from(parent.getContext()).inflate(R.layout.progress_bar_eventseeker_fixed_ht, parent, 
					false);
			break;
			
		case EVENT:
			v = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_item_upcoming_event, parent, false);
			break;
			
		default:
			v = null;
			break;
		}
		
		ViewHolder vh = new ViewHolder(v);
        return vh;
	}

	@Override
	public void onBindViewHolder(final ViewHolder holder, final int position) {
		if (position == ViewType.IMG.ordinal()) {
			// nothing to do
			
		}  else if (position == ViewType.DESC.ordinal()) {
			updateDescVisibility(holder);
			
		} else if (position == ViewType.ADDRESS_MAP.ordinal()) {
			updateAddressMap(holder);
			
		} else if (position == ViewType.UPCOMING_EVENTS_TITLE.ordinal()) {
			if (eventList.isEmpty()) {
				setViewGone(holder);
			}
			
		} else {
			final Event event = eventList.get(position - EXTRA_TOP_DUMMY_ITEM_COUNT_AFTER_DETAILS_LOADED);
			if (event == null) {
				// progress indicator
				
				if ((loadEvents == null || loadEvents.getStatus() == Status.FINISHED) && isMoreDataAvailable) {
					//Log.d(TAG, "onBindViewHolder(), pos = " + position);
					((LoadItemsInBackgroundListener) venueDetailsFragmentTab).loadItemsInBackground();
				}
				
			} else {
				/**
				 * If user clicks on save & changes orientation before call to onPublishPermissionGranted(), 
				 * then we need to update holderPendingPublish with right holder pointer in new orientation
				 */
				if (eventPendingPublish == event) {
					holderPendingPublish = holder;
				}
				
				holder.txtEvtTitle.setText(event.getName());
				
				if (event.getSchedule() != null) {
					Schedule schedule = event.getSchedule();
					Date date = schedule.getDates().get(0);
					holder.txtEvtTime.setText(ConversionUtil.getDateTime(date.getStartDate(), date.isStartTimeAvailable(), 
							false, true, true));
					
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
					Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
					if (bitmap != null) {
				        holder.imgEvt.setImageBitmap(bitmap);
				        
				    } else {
				    	holder.imgEvt.setImageBitmap(null);
				    	AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
				        asyncLoadImg.loadImg(holder.imgEvt, ImgResolution.LOW, recyclerView, position, bitmapCacheable);
				    }
				}
				
				holder.itemView.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						((EventListenerTab) FragmentUtil.getActivity(venueDetailsFragmentTab)).onEventSelected(event, 
								holder.imgEvt, holder.txtEvtTitle);
					}
				});
				
				Resources res = FragmentUtil.getResources(venueDetailsFragmentTab);
				if (event.getSchedule() == null || event.getSchedule().getBookingInfos().isEmpty()) {
					holder.imgTicket.setImageDrawable(res.getDrawable(R.drawable.ic_tickets_unavailable_slider));
					holder.imgTicket.setEnabled(false);
					
				} else {
					holder.imgTicket.setImageDrawable(res.getDrawable(R.drawable.ic_tickets_available_slider));
					holder.imgTicket.setEnabled(true);
				}
				
				updateImgSaveSrc(holder, event, res);
				
				holder.imgTicket.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						EventSeekr eventSeekr = FragmentUtil.getApplication(venueDetailsFragmentTab);
								
						Intent intent = new Intent(eventSeekr, WebViewActivityTab.class);
						intent.putExtra(BundleKeys.URL, event.getSchedule().getBookingInfos().get(0).getBookingUrl());
						venueDetailsFragmentTab.startActivity(intent);
						
						GoogleAnalyticsTracker.getInstance().sendEvent(eventSeekr, ((BaseActivityTab) 
								FragmentUtil.getActivity(venueDetailsFragmentTab)).getScreenName(), 
								GoogleAnalyticsTracker.EVENT_LABEL_TICKETS_BUTTON, 
								GoogleAnalyticsTracker.Type.Event.name(), null, event.getId());
					}
				});
				
				holder.imgSave.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						EventSeekr eventSeekr = (EventSeekr) FragmentUtil.getActivity(venueDetailsFragmentTab).getApplication();
						if (event.getAttending() == Attending.SAVED) {
							event.setAttending(Attending.NOT_GOING);
							new UserTracker(Api.OAUTH_TOKEN, eventSeekr, UserTrackingItemType.event, event.getId(), 
									event.getAttending().getValue(), UserTrackingType.Add).execute();
			    			updateImgSaveSrc(holder, event, FragmentUtil.getResources(venueDetailsFragmentTab));
							
						} else {
							venueDetailsFragmentTab.setEvent(event);
							eventPendingPublish = event;
							holderPendingPublish = holder;
							
							if (eventSeekr.getGPlusUserId() != null) {
								event.setNewAttending(Attending.SAVED);
								venueDetailsFragmentTab.handlePublishEvent();
								
							} else {
								fbCallCountForSameEvt = 0;
								event.setNewAttending(Attending.SAVED);
								//NOTE: THIS CAN BE TESTED WITH PODUCTION BUILD ONLY
								FbUtil.handlePublishEvent(venueDetailsFragmentTab, venueDetailsFragmentTab, AppConstants.PERMISSIONS_FB_PUBLISH_EVT_OR_ART, 
										AppConstants.REQ_CODE_FB_PUBLISH_EVT_OR_ART, event);
							}
						}
					}
				});
				
				holder.imgShare.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						ShareViaDialogFragment shareViaDialogFragment = ShareViaDialogFragment.newInstance(event, 
								ScreenNames.VENUE_DETAILS);
						/**
						 * Passing activity fragment manager, since using this fragment's child fragment manager 
						 * doesn't retain dialog on orientation change
						 */
						shareViaDialogFragment.show(((BaseActivityTab)FragmentUtil.getActivity(venueDetailsFragmentTab))
								.getSupportFragmentManager(), FragmentUtil.getTag(ShareViaDialogFragment.class));
					}
				});
			}
		}
	}
	
	private void updateImgSaveSrc(ViewHolder holder, Event event, Resources res) {
		int drawableId = (event.getAttending() == Attending.SAVED) ? R.drawable.ic_saved_event_slider 
				: R.drawable.ic_unsaved_event_slider;
		holder.imgSave.setImageDrawable(res.getDrawable(drawableId));
	}
	
	private void updateAddressMap(ViewHolder holder) {
		holder.txtVenue.setText(venue.getFormatedAddress(false));
		AddressMapFragment fragment = (AddressMapFragment) venueDetailsFragmentTab.getChildFragmentManager()
				.findFragmentByTag(FragmentUtil.getTag(AddressMapFragment.class));
		//Log.d(TAG, "AddressMapFragment = " + fragment);
        if (fragment == null) {
        	//Log.d(TAG, "call addAddressMapFragment()");
        	addAddressMapFragment();
        }
        
        holder.fabPhone.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (venue.getPhone() != null) {
					Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + venue.getPhone()));
					venueDetailsFragmentTab.startActivity(Intent.createChooser(intent, "Call..."));
					
				} else {
					Toast.makeText(FragmentUtil.getActivity(venueDetailsFragmentTab), R.string.phone_number_not_available, 
							Toast.LENGTH_SHORT).show();
				}
			}
		});
        
        holder.fabNavigate.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(FragmentUtil.getApplication(venueDetailsFragmentTab), NavigationActivityTab.class);
				intent.putExtra(BundleKeys.VENUE, venue);
				venueDetailsFragmentTab.startActivity(intent);
			}
		});
	}
	
	private void addAddressMapFragment() {
    	FragmentManager fragmentManager = venueDetailsFragmentTab.getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        
        AddressMapFragment fragment = new AddressMapFragment();
        Bundle args = new Bundle();
        args.putSerializable(BundleKeys.VENUE, venue);
        fragment.setArguments(args);
        fragmentTransaction.add(R.id.frmLayoutMapContainer, fragment, FragmentUtil.getTag(fragment));
        try {
        	fragmentTransaction.commit();
        	
        } catch (IllegalStateException e) {
        	/**
        	 * This catch is to prevent possible "java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState"
        	 * when it's called from callback method updateDetailsVisibility() & if user has already left this screen.
        	 */
			Log.e(TAG, "IllegalStateException: " + e.getMessage());
			e.printStackTrace();
		}
    }
	
	private void updateDescVisibility(ViewHolder holder) {
		if (venueDetailsFragmentTab.isAllDetailsLoaded()) {
			venueDetailsFragmentTab.setVNoContentBgVisibility(View.INVISIBLE);
			if (venue.getLongDesc() != null) {
				holder.rltRootDesc.setBackgroundColor(Color.WHITE);
				holder.rltLytPrgsBar.setVisibility(View.GONE);
				holder.txtDesc.setVisibility(View.VISIBLE);
				holder.imgDown.setVisibility(View.VISIBLE);
				holder.vHorLine.setVisibility(View.VISIBLE);
				
				makeDescVisible(holder);
				
			} else {
				setViewGone(holder);
			}
			
		} else {
			venueDetailsFragmentTab.setVNoContentBgVisibility(View.VISIBLE);
			holder.rltRootDesc.setBackgroundColor(Color.TRANSPARENT);
			holder.rltLytPrgsBar.setVisibility(View.VISIBLE);
			holder.txtDesc.setVisibility(View.GONE);
			holder.imgDown.setVisibility(View.GONE);
			holder.vHorLine.setVisibility(View.GONE);
		}
	}
	
	private void makeDescVisible(final ViewHolder holder) {
		holder.txtDesc.setText(Html.fromHtml(venue.getLongDesc()));
		holder.imgDown.setVisibility(View.VISIBLE);
		holder.imgDown.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//Log.d(TAG, "totalScrolled  = " + holder.itemView.getTop());
				if (isVenueDescExpanded) {
					collapseVenueDesc(holder);
					
					/**
					 * update scrolled distance after collapse, because sometimes it can happen that view becamse scrollable only
					 * due to expanded description after which if user collapses it, then based on recyclerview
					 * height it automatically resettles itself such that recyclerview again becomes unscrollable.
					 * Accordingly we need to reset scrolled amount, artist img & title
					 */
					venueDetailsFragmentTab.getHandler().post(new Runnable() {
						
						@Override
						public void run() {
							venueDetailsFragmentTab.onScrolled(0, true, false);
						}
					});
					
				} else {
					expandVenueDesc(holder);
				}
				//Log.d(TAG, "totalScrolled after  = " + holder.itemView.getTop());
			}
		});
		
		if (isVenueDescExpanded) {
			expandVenueDesc(holder);
			
		} else {
			collapseVenueDesc(holder);
		}
	}
	
	private void collapseVenueDesc(ViewHolder holder) {
		holder.txtDesc.setMaxLines(MAX_LINES_VENUE_DESC);
		holder.txtDesc.setEllipsize(TruncateAt.END);
		holder.imgDown.setImageDrawable(FragmentUtil.getResources(venueDetailsFragmentTab).getDrawable(
				R.drawable.ic_description_expand));
		isVenueDescExpanded = false;
	}
	
	private void expandVenueDesc(ViewHolder holder) {
		holder.txtDesc.setMaxLines(Integer.MAX_VALUE);
		holder.txtDesc.setEllipsize(null);
		holder.imgDown.setImageDrawable(FragmentUtil.getResources(venueDetailsFragmentTab).getDrawable(
				R.drawable.ic_description_collapse));
		isVenueDescExpanded = true;
	}
	
	private void setViewGone(ViewHolder holder) {
		RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) holder.itemView.getLayoutParams();
		lp.height = 0;
		holder.itemView.setLayoutParams(lp);
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
		this.loadEvents = (LoadEvents) loadDateWiseEvents;
	}
	
	public void call(Session session, SessionState state, Exception exception) {
		//Log.i(TAG, "call()");
		fbCallCountForSameEvt++;
		/**
		 * To prevent infinite loop when network is off & we are calling requestPublishPermissions() of FbUtil.
		 */
		if (fbCallCountForSameEvt < AppConstants.MAX_FB_CALL_COUNT_FOR_SAME_EVT_OR_ART) {
			FbUtil.call(session, state, exception, venueDetailsFragmentTab, venueDetailsFragmentTab, AppConstants.PERMISSIONS_FB_PUBLISH_EVT_OR_ART, 
					AppConstants.REQ_CODE_FB_PUBLISH_EVT_OR_ART, eventPendingPublish);
			
		} else {
			fbCallCountForSameEvt = 0;
			venueDetailsFragmentTab.setPendingAnnounce(false);
		}
	}

	public void onPublishPermissionGranted() {
		//Log.d(TAG, "onPublishPermissionGranted()");
		updateImgSaveSrc(holderPendingPublish, eventPendingPublish, FragmentUtil.getResources(venueDetailsFragmentTab));
	}
}
