package com.wcities.eventseeker.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.Session;
import com.facebook.SessionState;
import com.wcities.eventseeker.AddressMapFragment;
import com.wcities.eventseeker.BaseActivityTab;
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
import com.wcities.eventseeker.util.VersionUtil;
import com.wcities.eventseeker.util.ViewUtil;

import java.lang.ref.WeakReference;
import java.util.List;

public class RVVenueDetailsAdapterTab extends RVAdapterBase<RVVenueDetailsAdapterTab.ViewHolder> implements 
		DateWiseEventParentAdapterListener {
	
	private static final String TAG = RVVenueDetailsAdapterTab.class.getSimpleName();

	private static final int INVALID = -1;
	private static final int EXTRA_TOP_DUMMY_ITEM_COUNT = 2;
	private static final int EXTRA_TOP_DUMMY_ITEM_COUNT_AFTER_DETAILS_LOADED = 4;
	private static final int MAX_LINES_VENUE_DESC = 3;
	
	private RecyclerView recyclerView;
	private WeakReference<RecyclerView> weakRecyclerView;
	
	private VenueDetailsFragmentTab venueDetailsFragmentTab;
	private List<Event> eventList;
	private Venue venue;
	private boolean isVenueDescExpanded, fragmentDetached;
	
	private LoadEvents loadEvents;
	private boolean isMoreDataAvailable = true;
	private int eventsAlreadyRequested;
	
	private BitmapCache bitmapCache;
	
	private int fbCallCountForSameEvt = 0;
	private RVVenueDetailsAdapterTab.ViewHolder holderPendingPublish;
	private Event eventPendingPublish;

	private int lnrSliderContentW, openPos = INVALID, rltLytDetailsW = INVALID;

	private Handler handler;

	private int translationZPx;
	
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
		
		private RelativeLayout rltLytPrgsBar, rltRootDesc, rltLytDetails;
		private TextView txtDesc;
		private ImageView imgDown;
		private View vHorLine, vFabSeparator;
		
		private TextView txtVenue;
		private ImageView fabPhone, fabNavigate, fabWeb, fabFb;
		
		private ImageView imgEvt, imgTicket, imgSave, imgShare, imgHandle;
		private TextView txtEvtTitle, txtEvtTime, txtEvtLoc;
		private LinearLayout lnrSliderContent;

		public ViewHolder(View itemView) {
			super(itemView);
			
			rltRootDesc = (RelativeLayout) itemView.findViewById(R.id.rltRootDesc);
			rltLytPrgsBar = (RelativeLayout) itemView.findViewById(R.id.rltLytPrgsBar);
			txtDesc = (TextView) itemView.findViewById(R.id.txtDesc);
            fabWeb = (ImageView) itemView.findViewById(R.id.fabWeb);
            fabFb = (ImageView) itemView.findViewById(R.id.fabFb);
            vFabSeparator = itemView.findViewById(R.id.vFabSeparator);
			imgDown = (ImageView) itemView.findViewById(R.id.imgDown);
			vHorLine = itemView.findViewById(R.id.vHorLine);
			
			txtVenue = (TextView) itemView.findViewById(R.id.txtVenue);
			fabPhone = (ImageView) itemView.findViewById(R.id.fabPhone);
			fabNavigate = (ImageView) itemView.findViewById(R.id.fabNavigate);
			
			imgEvt = (ImageView) itemView.findViewById(R.id.imgEvt);
			imgHandle = (ImageView) itemView.findViewById(R.id.imgHandle);
			
			txtEvtTitle = (TextView) itemView.findViewById(R.id.txtEvtTitle);
            txtEvtTime = (TextView) itemView.findViewById(R.id.txtEvtTime);
            txtEvtLoc = (TextView) itemView.findViewById(R.id.txtEvtLoc);
            
            rltLytDetails = (RelativeLayout) itemView.findViewById(R.id.rltLytDetails);
            
            lnrSliderContent = (LinearLayout) itemView.findViewById(R.id.lnrSliderContent);
            imgTicket = (ImageView) itemView.findViewById(R.id.imgTicket);
            imgSave = (ImageView) itemView.findViewById(R.id.imgSave);
            imgShare = (ImageView) itemView.findViewById(R.id.imgShare);
		}

		private boolean isSliderClose() {
        	RelativeLayout.LayoutParams rltLytContentLP = (RelativeLayout.LayoutParams) rltLytDetails.getLayoutParams();
			return (rltLytContentLP.leftMargin == 0);
        }
	}

	public RVVenueDetailsAdapterTab(VenueDetailsFragmentTab venueDetailsFragmentTab) {
		this.venueDetailsFragmentTab = venueDetailsFragmentTab;
		eventList = venueDetailsFragmentTab.getEventList();
		venue = venueDetailsFragmentTab.getVenue();
		
		bitmapCache = BitmapCache.getInstance();
		
		handler = new Handler(Looper.getMainLooper());
		
		Resources res = FragmentUtil.getResources(venueDetailsFragmentTab);
		lnrSliderContentW = res.getDimensionPixelSize(R.dimen.lnr_slider_content_w_rv_item_event_tab);
		
		translationZPx = res.getDimensionPixelSize(R.dimen.action_bar_elevation);
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
		
		if (recyclerView != parent) {
			recyclerView = (RecyclerView) parent;
			weakRecyclerView = new WeakReference<RecyclerView>(recyclerView);
		}
		
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
            EventSeekr eventSeekr = FragmentUtil.getApplication(venueDetailsFragmentTab);
            final BaseActivityTab baseActivityTab = (BaseActivityTab) FragmentUtil.getActivity(venueDetailsFragmentTab);
            final Intent intent = new Intent(eventSeekr, WebViewActivityTab.class);

            holder.fabWeb.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    intent.putExtra(BundleKeys.URL, venue.getUrl());
                    baseActivityTab.startActivity(intent);
                }
            });
            holder.fabFb.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    intent.putExtra(BundleKeys.URL, venue.getFbLink());
                    baseActivityTab.startActivity(intent);
                }
            });
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
				ViewCompat.setTransitionName(holder.txtEvtTitle, "txtEvtTitleVenueDetails" + position);
				
				if (event.getSchedule() != null) {
					Schedule schedule = event.getSchedule();
					Date date = schedule.getDates().get(0);
					holder.txtEvtTime.setText(ConversionUtil.getDateTime(FragmentUtil.getApplication(venueDetailsFragmentTab),
                            date.getStartDate(), date.isStartTimeAvailable(), false, true, true));
					
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
				        asyncLoadImg.loadImg(holder.imgEvt, ImgResolution.LOW, weakRecyclerView, position, bitmapCacheable);
				    }
				}
				ViewCompat.setTransitionName(holder.imgEvt, "imgEvtVenueDetails" + position);
				
				holder.itemView.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						((EventListenerTab) FragmentUtil.getActivity(venueDetailsFragmentTab)).onEventSelected(event, 
								holder.imgEvt, holder.txtEvtTitle);
					}
				});
				
				final Resources res = FragmentUtil.getResources(venueDetailsFragmentTab);
				if (event.getSchedule() == null || event.getSchedule().getBookingInfos().isEmpty()) {
					holder.imgTicket.setImageDrawable(res.getDrawable(R.drawable.ic_tickets_unavailable_slider));
					holder.imgTicket.setEnabled(false);
					
				} else {
					holder.imgTicket.setImageDrawable(res.getDrawable(R.drawable.ic_tickets_available_slider));
					holder.imgTicket.setEnabled(true);
				}
				
				updateImgSaveSrc(holder, event, res);
				
				if (rltLytDetailsW == INVALID) {
					holder.rltLytDetails.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			            @Override
			            public void onGlobalLayout() {
							RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) holder.rltLytDetails.getLayoutParams();

							if (lp.width != RelativeLayout.LayoutParams.MATCH_PARENT) {
								if (VersionUtil.isApiLevelAbove15()) {
									holder.rltLytDetails.getViewTreeObserver().removeOnGlobalLayoutListener(this);

								} else {
									holder.rltLytDetails.getViewTreeObserver().removeGlobalOnLayoutListener(this);
								}
							}
							
							/**
							 * we change rltLytContent width from MATCH_PARENT to fixed value, because 
							 * otherwise on sliding the row, its width goes on increasing (extra width 
							 * added due to negative left margin on sliding the row) hence text starts expanding
							 * along the width while sliding which looks weird.
							 */
							rltLytDetailsW = lp.width = holder.rltLytDetails.getWidth();
							holder.rltLytDetails.setLayoutParams(lp);
			            	/*Log.d(TAG, "onGlobalLayout(), rltLytContentW = " + rltLytContentW + 
			            			", holder.rltLytRoot.getWidth() = " + holder.rltLytRoot.getWidth());*/

							if (openPos == position) {
								/**
								 * Now since we know fixed width of rltLytContent, we can update its 
								 * left margin in layoutParams by calling openSlider() which was delayed 
								 * until now [by condition if (rltLytContentW != INVALID) at end of 
								 * onBindViewHolder()].
								 */
								openSlider(holder, position, false);
							}
			            }
			        });
					
				} else {
					/**
					 * we change rltLytContent width from MATCH_PARENT to fixed value, because 
					 * otherwise on sliding the row, its width goes on increasing (extra width 
					 * added due to negative left margin on sliding the row) hence text starts expanding
					 * along the width while sliding which looks weird.
					 */
					RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) holder.rltLytDetails.getLayoutParams();
					lp.width = rltLytDetailsW;
					//Log.d(TAG, "else, rltLytContentW = " + rltLytContentW);
					holder.rltLytDetails.setLayoutParams(lp);
				}
				
				((ViewGroup) holder.rltLytDetails.getParent()).setOnTouchListener(new OnTouchListener() {
					
					int MIN_SWIPE_DISTANCE_X = ConversionUtil.toPx(res, 50);
					int MAX_CLICK_DISTANCE = ConversionUtil.toPx(res, 4);
					int pointerX = 0, initX = 0, pointerY = 0, initY = 0, maxMovedOnX = 0, maxMovedOnY = 0;
					boolean isSliderOpenInititally;
					
					@Override
					public boolean onTouch(View v, MotionEvent mEvent) {
						RelativeLayout.LayoutParams rltLytDetailsLP = (RelativeLayout.LayoutParams) holder.rltLytDetails.getLayoutParams();
						RelativeLayout.LayoutParams lnrSliderContentLP = (RelativeLayout.LayoutParams) holder.lnrSliderContent.getLayoutParams();
						
						switch (mEvent.getAction()) {
						
						case MotionEvent.ACTION_DOWN:
							//Log.d(TAG, "down, x = " + mEvent.getRawX() + ", y = " + mEvent.getRawY());
							initX = pointerX = (int) mEvent.getRawX();
							initY = pointerY = (int) mEvent.getRawY();
							isSliderOpenInititally = !holder.isSliderClose();
							maxMovedOnX = maxMovedOnY = 0;
							return true;
						
						case MotionEvent.ACTION_MOVE:
							//Log.d(TAG, "move");
							holder.rltLytDetails.setPressed(true);
							
							holder.lnrSliderContent.setVisibility(View.VISIBLE);
							
							int newX = (int) mEvent.getRawX();
							int dx = newX - pointerX;
							
							int scrollX = rltLytDetailsLP.leftMargin + dx;
							//Log.d(TAG, "move, rltLytContentLP.leftMargin = " + rltLytContentLP.leftMargin + ", lnrDrawerContentW = " + lnrDrawerContentW);
							if (scrollX >= -lnrSliderContentW && scrollX <= 0) {
								holder.imgHandle.setImageResource(R.drawable.ic_more_slider_pressed);
								ViewCompat.setElevation(holder.imgEvt, translationZPx);
								
								rltLytDetailsLP.leftMargin = scrollX;
								//Log.d(TAG, "onTouch(), ACTION_MOVE");
								holder.rltLytDetails.setLayoutParams(rltLytDetailsLP);
								
								lnrSliderContentLP.rightMargin = - rltLytDetailsLP.leftMargin - lnrSliderContentW;
								holder.lnrSliderContent.setLayoutParams(lnrSliderContentLP);
								
								pointerX = newX;
							}
							pointerY = (int) mEvent.getRawY();
							maxMovedOnX = Math.abs(initX - newX) > maxMovedOnX ? Math.abs(initX - newX) : maxMovedOnX;
							maxMovedOnY = Math.abs(initY - pointerY) > maxMovedOnY ? Math.abs(initY - pointerY) : maxMovedOnY;
							//Log.d(TAG, "maxMovedOnX = " + maxMovedOnX + ", maxMovedOnY = " + maxMovedOnY);
							break;
							
						case MotionEvent.ACTION_UP:
						case MotionEvent.ACTION_CANCEL:
							//Log.d(TAG, "up, action = " + mEvent.getAction() + ", x = " + mEvent.getRawX() + ", y = " + mEvent.getRawY());
							holder.rltLytDetails.setPressed(false);
							boolean isMinSwipeDistanceXTravelled = Math.abs(initX - pointerX) > MIN_SWIPE_DISTANCE_X;
							boolean isSwipedToOpen = (initX > pointerX);
							
							if (isMinSwipeDistanceXTravelled) {
								//Log.d(TAG, "isMinSwipeDistanceXTravelled");
								if (isSwipedToOpen) {
									//Log.d(TAG, "isSwipedToOpen");
									openSlider(holder, position, true);
									
								} else {
									//Log.d(TAG, "!isSwipedToOpen");
									closeSlider(holder, position, true);
								}

							} else {
								//Log.d(TAG, "!isMinSwipeDistanceXTravelled");
								if (isSliderOpenInititally) {
									//Log.d(TAG, "isSliderOpenInititally");
									openSlider(holder, position, true);
									
								} else {
									//Log.d(TAG, "!isSliderOpenInititally");
									closeSlider(holder, position, true);
								}
								
								if (mEvent.getAction() == MotionEvent.ACTION_CANCEL) {
									//Log.d(TAG, "ACTION_CANCEL");
									break;
								}
								
								//Log.d(TAG, "maxMovedOnX = " + maxMovedOnX + ", maxMovedOnY = " + maxMovedOnY);
								if (maxMovedOnX > MAX_CLICK_DISTANCE || maxMovedOnY > MAX_CLICK_DISTANCE) {
									//Log.d(TAG, "< max click distance");
									break;
								}
								
								/**
								 * Handle click event.
								 * We do it here instead of implementing onClick listener because then onClick listener
								 * of child element would block onTouch event on its parent (rltLytRoot) 
								 * if this onTouch starts from such a child view 
								 */
								if (ViewUtil.isPointInsideView(mEvent.getRawX(), mEvent.getRawY(), holder.imgHandle)) {
									onHandleClick(holder, position);

								} else if (openPos == position) { 
									/**
									 * above condition is required, because otherwise these 3 conditions
									 * prevent event click on these positions even if slider is closed
									 */
									if (holder.imgTicket.isEnabled() && ViewUtil.isPointInsideView(
											mEvent.getRawX(), mEvent.getRawY(), holder.imgTicket)) {
										onImgTicketClick(holder, event);
											
									} else if (ViewUtil.isPointInsideView(mEvent.getRawX(), mEvent.getRawY(), holder.imgSave)) {
										onImgSaveClick(holder, event);
										
									} else if (ViewUtil.isPointInsideView(mEvent.getRawX(), mEvent.getRawY(), holder.imgShare)) {
										onImgShareClick(holder, event);
										
									} else if (ViewUtil.isPointInsideView(mEvent.getRawX(), mEvent.getRawY(), holder.rltLytDetails)) {
										/**
										 * This block is added to consider row click as event click even when
										 * slider is open (openPos == position); otherwise it won't do anything 
										 * on clicking outside the slider when it's open
										 */
										onEventClick(holder, event);
									}
									
								} else if (ViewUtil.isPointInsideView(mEvent.getRawX(), mEvent.getRawY(), holder.rltLytDetails)) {
									onEventClick(holder, event);
								}
							}
							
							break;
						}
						return true;
					}
				});
				
				if (rltLytDetailsW != INVALID) {
					/**
					 * If at this point we don't know rltLytContentW, it means onGlobalLayout() for 
					 * rltLytContent is not yet called up where we actually calculate rltLytContentW
					 * & update rltLytContent layoutParams to update width from match_parent to fixed 
					 * value rltLytContentW.
					 * In such case w/o above condition, openSlider() function will change rltLytContent
					 * layoutParams resulting in extended width due to negative left margin & 
					 * width being match_parent. Hence instead, call it from onGlobalLayout().
					 * e.g. - opening slider in portrait & changing to landscape results in handle 
					 * getting overlapped by slider due to extended width of rltLytContent
					 */
					if (openPos == position) {
						//Log.d(TAG, "openPos == " + position);
						openSlider(holder, position, false);
						
					} else {
						closeSlider(holder, position, false);
					}
				}
			}
		}
	}
	
	private void onEventClick(final ViewHolder holder, final Event event) {
		holder.rltLytDetails.setPressed(true);
		handler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				//Log.d(TAG, "AT issue event = " + event);
				((EventListenerTab) FragmentUtil.getActivity(venueDetailsFragmentTab)).onEventSelected(event, 
						holder.imgEvt, holder.txtEvtTitle);
				holder.rltLytDetails.setPressed(false);
			}
		}, 200);
	}
	
	private void updateImgSaveSrc(ViewHolder holder, Event event, Resources res) {
		int drawableId = (event.getAttending() == Attending.SAVED) ? R.drawable.ic_saved_event_slider
				: R.drawable.ic_unsaved_event_slider;
		holder.imgSave.setImageDrawable(res.getDrawable(drawableId));
	}
	
	private void updateAddressMap(ViewHolder holder) {
		holder.txtVenue.setText(venue.getFormatedAddress(false));
		AddressMapFragment fragment = (AddressMapFragment) venueDetailsFragmentTab.childFragmentManager()
				.findFragmentByTag(FragmentUtil.getTag(AddressMapFragment.class));
		//Log.d(TAG, "AddressMapFragment = " + fragment);
        if (fragment == null) {
        	//Log.d(TAG, "call addAddressMapFragment()");
        	addAddressMapFragment();
        	
        } else if (fragmentDetached) {
        	attachFragments();
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
    	FragmentManager fragmentManager = venueDetailsFragmentTab.childFragmentManager();
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
	private void openSlider(ViewHolder holder, int position, boolean isUserInitiated) {
		ViewCompat.setElevation(holder.imgEvt, translationZPx);
		holder.lnrSliderContent.setVisibility(View.VISIBLE);
		
		RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) holder.lnrSliderContent.getLayoutParams();
		lp.rightMargin = 0;
		holder.lnrSliderContent.setLayoutParams(lp);
		
		lp = (RelativeLayout.LayoutParams) holder.rltLytDetails.getLayoutParams();
		lp.leftMargin = -lnrSliderContentW;
		holder.rltLytDetails.setLayoutParams(lp);
		//Log.d(TAG, "openSlider()");
		
		holder.imgHandle.setImageResource(R.drawable.ic_more_slider_pressed);
		if (isUserInitiated) {
			updateOpenPos(position, recyclerView);
		}
	}
	
	private void closeSlider(ViewHolder holder, int position, boolean isUserInitiated) {
		holder.lnrSliderContent.setVisibility(View.INVISIBLE);
		
		RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) holder.lnrSliderContent.getLayoutParams();
		lp.rightMargin = -lnrSliderContentW;;
		holder.lnrSliderContent.setLayoutParams(lp);
		
		lp = (RelativeLayout.LayoutParams) holder.rltLytDetails.getLayoutParams();
		lp.leftMargin = 0;
		holder.rltLytDetails.setLayoutParams(lp);
		//Log.d(TAG, "closeSlider()");

		ViewCompat.setElevation(holder.imgEvt, 0);
		
		holder.imgHandle.setImageResource(R.drawable.ic_more_slider);
		if (isUserInitiated) {
			if (openPos == position) {
				/**
				 * If slider closed is the one which was open & not already closed.
				 * W/o this condition if user tries to close already close slider than call to 
				 * updateOpenPos() will just overwrite openPos value with 'INVALID' (-1), even though
				 * some other row has its slider open.
				 */
				updateOpenPos(INVALID, null);
			}
		}
	}
	
	private void onHandleClick(final ViewHolder holder, final int position) {
		//Log.d(TAG, "onHandleClick()");
		holder.imgHandle.setImageResource(R.drawable.ic_more_slider_pressed);
		
		if (holder.isSliderClose()) {
			// slider is close, so open it
			//Log.d(TAG, "open slider");
			ViewCompat.setElevation(holder.imgEvt, translationZPx);
			
			Animation slide = AnimationUtils.loadAnimation(FragmentUtil.getActivity(
					venueDetailsFragmentTab), R.anim.slide_in_from_left);
			slide.setAnimationListener(new AnimationListener() {
				
				@Override
				public void onAnimationStart(Animation animation) {
					holder.lnrSliderContent.setVisibility(View.VISIBLE);
					
					RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) holder.lnrSliderContent.getLayoutParams();
					lp.rightMargin = 0;
					holder.lnrSliderContent.setLayoutParams(lp);
				}
				
				@Override
				public void onAnimationRepeat(Animation animation) {}
				
				@Override
				public void onAnimationEnd(Animation animation) {
					RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) 
							holder.rltLytDetails.getLayoutParams();
					lp.leftMargin -= holder.lnrSliderContent.getWidth();
					holder.rltLytDetails.setLayoutParams(lp);
					//Log.d(TAG, "isSliderClose");
					
					updateOpenPos(position, recyclerView);
				}
			});
			holder.lnrSliderContent.startAnimation(slide);
			
		} else {
			// slider is open, so close it
			//Log.d(TAG, "close slider");
			Animation slide = AnimationUtils.loadAnimation(FragmentUtil.getActivity(
					venueDetailsFragmentTab), android.R.anim.slide_out_right);
			slide.setAnimationListener(new AnimationListener() {
				
				@Override
				public void onAnimationStart(Animation animation) {}
				
				@Override
				public void onAnimationRepeat(Animation animation) {}
				
				@Override
				public void onAnimationEnd(Animation animation) {
					ViewCompat.setElevation(holder.imgEvt, 0);
					
					holder.imgHandle.setImageResource(R.drawable.ic_more_slider);
					
					holder.lnrSliderContent.setVisibility(View.INVISIBLE);
					RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) holder.lnrSliderContent.getLayoutParams();
					lp.rightMargin = 0 - lnrSliderContentW;;
					holder.lnrSliderContent.setLayoutParams(lp);
					
					lp = (RelativeLayout.LayoutParams) holder.rltLytDetails.getLayoutParams();
					lp.leftMargin += holder.lnrSliderContent.getWidth();
					holder.rltLytDetails.setLayoutParams(lp);
					//Log.d(TAG, "!isSliderClose");
					
					updateOpenPos(INVALID, null);
				}
			});
			holder.lnrSliderContent.startAnimation(slide);
		}
	}
	
	private void onImgTicketClick(final ViewHolder holder, final Event event) {
		holder.imgTicket.setPressed(true);
		handler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				holder.imgTicket.setPressed(false);
				
				EventSeekr eventSeekr = FragmentUtil.getApplication(venueDetailsFragmentTab);
				BaseActivityTab baseActivityTab = (BaseActivityTab) FragmentUtil.getActivity(venueDetailsFragmentTab);
						
				Intent intent = new Intent(eventSeekr, WebViewActivityTab.class);
				intent.putExtra(BundleKeys.URL, event.getSchedule().getBookingInfos().get(0).getBookingUrl()
                    + "&lang=" + FragmentUtil.getApplication(venueDetailsFragmentTab).getLocale().getLocaleCode());
				baseActivityTab.startActivity(intent);
				
				GoogleAnalyticsTracker.getInstance().sendEvent(eventSeekr, 
						baseActivityTab.getScreenName(), GoogleAnalyticsTracker.EVENT_LABEL_TICKETS_BUTTON, 
						GoogleAnalyticsTracker.Type.Event.name(), null, event.getId());
			}
		}, 200);
	}
	
	private void onImgSaveClick(final ViewHolder holder, final Event event) {
		holder.imgSave.setPressed(true);
		handler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				holder.imgSave.setPressed(false);
				
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
		}, 200);
	}
	
	private void onImgShareClick(final ViewHolder holder, final Event event) {
		holder.imgShare.setPressed(true);
		handler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				holder.imgShare.setPressed(false);
				
				ShareViaDialogFragment shareViaDialogFragment = ShareViaDialogFragment.newInstance(event, 
						ScreenNames.ARTIST_DETAILS);
				/**
				 * Passing activity fragment manager, since using this fragment's child fragment manager 
				 * doesn't retain dialog on orientation change
				 */
				shareViaDialogFragment.show(((BaseActivityTab)FragmentUtil.getActivity(venueDetailsFragmentTab))
						.getSupportFragmentManager(), FragmentUtil.getTag(ShareViaDialogFragment.class));
			}
		}, 200);
	}
	
	private void updateOpenPos(int openPos, ViewGroup parent) {
		if (this.openPos == openPos) {
			/**
			 * onBindViewHolder can be called more than once, so no need to execute same code again 
			 * for same position
			 */
			return;
		}
		
		int oldOpenPos = this.openPos;
		this.openPos = openPos;
		
		if (parent != null && oldOpenPos != INVALID) {
			/**
			 * notify to close earlier open slider  
			 * 1) if we are opening another slider (not closing already open one). 
			 * While closing we pass null for parent, and
			 * 2) if some other slider was open before
			 */
			notifyItemChanged(oldOpenPos);
		}
	}
	
	private void updateDescVisibility(final ViewHolder holder) {
		if (venueDetailsFragmentTab.isAllDetailsLoaded()) {
            updateFabLinks(holder);

			venueDetailsFragmentTab.setVNoContentBgVisibility(View.INVISIBLE);

            holder.rltRootDesc.setBackgroundColor(Color.WHITE);
            holder.rltLytPrgsBar.setVisibility(View.GONE);
            holder.imgDown.setVisibility(View.VISIBLE);
            holder.vHorLine.setVisibility(View.VISIBLE);
            holder.fabWeb.setVisibility(View.VISIBLE);
            holder.fabFb.setVisibility(View.VISIBLE);
            holder.vFabSeparator.setVisibility(View.VISIBLE);

			if (venue.getLongDesc() != null) {
				holder.txtDesc.setVisibility(View.VISIBLE);
                holder.txtDesc.setText(Html.fromHtml(venue.getLongDesc()));
			}

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
                         * Accordingly we need to reset scrolled amount, venue img & title
                         */
                        venueDetailsFragmentTab.getHandler().post(new Runnable() {

                            @Override
                            public void run() {
                                venueDetailsFragmentTab.onScrolled(0, true);
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

		} else {
			venueDetailsFragmentTab.setVNoContentBgVisibility(View.VISIBLE);
			holder.rltRootDesc.setBackgroundColor(Color.TRANSPARENT);
			holder.rltLytPrgsBar.setVisibility(View.VISIBLE);
			holder.txtDesc.setVisibility(View.GONE);
			holder.imgDown.setVisibility(View.GONE);
			holder.vHorLine.setVisibility(View.GONE);
		}
	}

    private void updateFabLinks(ViewHolder holder) {
        final Resources res = FragmentUtil.getResources(venueDetailsFragmentTab);
        if (venue.getUrl() == null) {
            holder.fabWeb.setImageDrawable(res.getDrawable(R.drawable.ic_web_link_unavailable));
            holder.fabWeb.setEnabled(false);

        } else {
            holder.fabWeb.setImageDrawable(res.getDrawable(R.drawable.ic_web_link_available));
            holder.fabWeb.setEnabled(true);
        }

        if (venue.getFbLink() == null) {
            holder.fabFb.setImageDrawable(res.getDrawable(R.drawable.ic_facebook_link_unavailable));
            holder.fabFb.setEnabled(false);

        } else {
            holder.fabFb.setImageDrawable(res.getDrawable(R.drawable.ic_facebook_link_available));
            holder.fabFb.setEnabled(true);
        }
    }
	
	private void collapseVenueDesc(ViewHolder holder) {
		holder.txtDesc.setMaxLines(MAX_LINES_VENUE_DESC);
		holder.txtDesc.setEllipsize(TruncateAt.END);
		holder.imgDown.setImageDrawable(FragmentUtil.getResources(venueDetailsFragmentTab).getDrawable(
				R.drawable.ic_description_expand));

        holder.fabWeb.setVisibility(View.GONE);
        holder.fabFb.setVisibility(View.GONE);
        holder.vFabSeparator.setVisibility(View.GONE);

		isVenueDescExpanded = false;
	}
	
	private void expandVenueDesc(ViewHolder holder) {
		holder.txtDesc.setMaxLines(Integer.MAX_VALUE);
		holder.txtDesc.setEllipsize(null);
		holder.imgDown.setImageDrawable(FragmentUtil.getResources(venueDetailsFragmentTab).getDrawable(
				R.drawable.ic_description_collapse));

        holder.fabWeb.setVisibility(View.VISIBLE);
        holder.fabFb.setVisibility(View.VISIBLE);
        holder.vFabSeparator.setVisibility(View.VISIBLE);

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
	
	public void detachFragments() {
		AddressMapFragment fragment = (AddressMapFragment) venueDetailsFragmentTab.childFragmentManager()
				.findFragmentByTag(FragmentUtil.getTag(AddressMapFragment.class));
		if (fragment != null) {
			FragmentManager fragmentManager = venueDetailsFragmentTab.childFragmentManager();
			fragmentManager.beginTransaction().detach(fragment).commit();
			fragmentManager.executePendingTransactions();
		}
		fragmentDetached = true;
	}
	
	public void attachFragments() {
		//Log.d(TAG, "attachFragments()");
		AddressMapFragment fragment = (AddressMapFragment) venueDetailsFragmentTab.childFragmentManager()
				.findFragmentByTag(FragmentUtil.getTag(AddressMapFragment.class));
		
		if (fragment != null) {
			FragmentManager fragmentManager = venueDetailsFragmentTab.childFragmentManager();
			fragmentManager.beginTransaction().attach(fragment).commit();
		}
		fragmentDetached = false;
	}

	// to update values which should change on orientation change
	public void onActivityCreated() {
		rltLytDetailsW = INVALID;
	}
}
