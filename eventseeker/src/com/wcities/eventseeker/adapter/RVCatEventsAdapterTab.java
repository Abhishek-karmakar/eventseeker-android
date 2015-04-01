package com.wcities.eventseeker.adapter;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
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

import com.facebook.Session;
import com.facebook.SessionState;
import com.wcities.eventseeker.BaseActivityTab;
import com.wcities.eventseeker.DiscoverActivityTab;
import com.wcities.eventseeker.DiscoverFragmentTab;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.ShareViaDialogFragment;
import com.wcities.eventseeker.WebViewActivityTab;
import com.wcities.eventseeker.adapter.RVCatEventsAdapterTab.ViewHolder;
import com.wcities.eventseeker.analytics.GoogleAnalyticsTracker;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingItemType;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
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
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener;
import com.wcities.eventseeker.interfaces.EventListenerTab;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.FbUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.VersionUtil;
import com.wcities.eventseeker.util.ViewUtil;

public class RVCatEventsAdapterTab extends Adapter<ViewHolder> implements DateWiseEventParentAdapterListener {

	private static final String TAG = RVCatEventsAdapterTab.class.getSimpleName();

	private static final int INVALID = -1;
	
	private DiscoverFragmentTab discoverFragmentTab;
	private List<Event> eventList;
	
	private int eventsAlreadyRequested;
	private boolean isMoreDataAvailable = true;
	
	private int lnrSliderContentW, openPos = INVALID, rltLytDetailsW = INVALID;
	
	private AsyncTask<Void, Void, List<Event>> loadDateWiseEvents;
	private LoadItemsInBackgroundListener mListener;
	private RecyclerView recyclerView;
	
	private BitmapCache bitmapCache;
	private Handler handler;
	
	private int fbCallCountForSameEvt = 0;
	private RVCatEventsAdapterTab.ViewHolder holderPendingPublish;
	private Event eventPendingPublish;
	
	private static enum ViewType {
		EVENT;
	};
	
	static class ViewHolder extends RecyclerView.ViewHolder {
		
		private ImageView imgEvt, imgHandle, imgTicket, imgSave, imgShare;
		private TextView txtEvtTitle, txtEvtLoc, txtEvtTime;
		private RelativeLayout rltLytRoot, rltLytDetails, rltLytBtm, rltLytRootPrgs;
		private LinearLayout lnrSliderContent;
			
		public ViewHolder(View itemView) {
			super(itemView);
			rltLytRootPrgs = (RelativeLayout) itemView.findViewById(R.id.rltLytRootPrgs);
			
			imgEvt = (ImageView) itemView.findViewById(R.id.imgEvt);
			imgHandle = (ImageView) itemView.findViewById(R.id.imgHandle);
			
			txtEvtTitle = (TextView) itemView.findViewById(R.id.txtEvtTitle);
			txtEvtLoc = (TextView) itemView.findViewById(R.id.txtEvtLoc);
			txtEvtTime = (TextView) itemView.findViewById(R.id.txtEvtTime);
			
			rltLytRoot = (RelativeLayout) itemView.findViewById(R.id.rltLytRoot);
			rltLytBtm = (RelativeLayout) itemView.findViewById(R.id.rltLytBtm);
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

	public RVCatEventsAdapterTab(List<Event> eventList, AsyncTask<Void, Void, List<Event>> loadDateWiseEvents,
			LoadItemsInBackgroundListener mListener, DiscoverFragmentTab discoverFragmentTab) {
		this.eventList = eventList;
		this.loadDateWiseEvents = loadDateWiseEvents;
		this.mListener = mListener;
		this.discoverFragmentTab = discoverFragmentTab;
		
		bitmapCache = BitmapCache.getInstance();
		handler = new Handler(Looper.getMainLooper());
		
		Resources res = FragmentUtil.getResources(discoverFragmentTab);
		lnrSliderContentW = res.getDimensionPixelSize(R.dimen.lnr_slider_content_w_rv_item_event_tab);
	}

	@Override
	public int getItemCount() {
		//Log.d(TAG, "getItemCount() - " + eventList.size());
		return eventList.size();
	}
	
	@Override
	public int getItemViewType(int position) {
		return ViewType.EVENT.ordinal();
	}
	
	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		recyclerView = (RecyclerView) parent;
		
		View v;
		
		ViewType vType = ViewType.values()[viewType];
		switch (vType) {
		
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
	public void onBindViewHolder(final ViewHolder holder, final int position) {
		//Log.d(TAG, "onBindViewHolder() - pos = " + position);
		final Event event = eventList.get(position);
		if (event == null) {
			// progress indicator
			
			if (eventList.size() == 1) {
				// no events loaded yet
				discoverFragmentTab.setCenterProgressBarVisibility(View.VISIBLE);
				holder.itemView.setVisibility(View.INVISIBLE);
				
			} else {
				holder.itemView.setVisibility(View.VISIBLE);
				holder.rltLytRootPrgs.setVisibility(View.VISIBLE);
				holder.rltLytRoot.setVisibility(View.INVISIBLE);
			}
			
			if ((loadDateWiseEvents == null || loadDateWiseEvents.getStatus() == Status.FINISHED) && 
					isMoreDataAvailable) {
				//Log.d(TAG, "onBindViewHolder(), pos = " + position);
				mListener.loadItemsInBackground();
			}
			
		} else {
			//Log.d(TAG, "else");
			holder.itemView.setVisibility(View.VISIBLE);
			holder.rltLytRootPrgs.setVisibility(View.INVISIBLE);
			holder.rltLytRoot.setVisibility(View.VISIBLE);
			
			/**
			 * If user clicks on save & changes orientation before call to onPublishPermissionGranted(), 
			 * then we need to update holderPendingPublish with right holder pointer in new orientation
			 */
			if (eventPendingPublish == event) {
				holderPendingPublish = holder;
			}
			
			holder.txtEvtTitle.setText(event.getName());
			ViewCompat.setTransitionName(holder.txtEvtTitle, "txtEvtTitleDiscover" + position);
			
			if (event.getSchedule() != null) {
				Schedule schedule = event.getSchedule();
				Date date = schedule.getDates().get(0);
				holder.txtEvtTime.setText(ConversionUtil.getDateTime(date.getStartDate(), date.isStartTimeAvailable(), true, false, false));
				
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
					//Log.d(TAG, "bitmap != null");
			        holder.imgEvt.setImageBitmap(bitmap);
			        
			    } else {
			    	holder.imgEvt.setImageBitmap(null);
			    	AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
			        asyncLoadImg.loadImg(holder.imgEvt, ImgResolution.LOW, recyclerView, position, bitmapCacheable);
			    }
			}
			
			ViewCompat.setTransitionName(holder.imgEvt, "imgEvtDiscover" + position);

			final Resources res = FragmentUtil.getResources(discoverFragmentTab);
			if (event.getSchedule() == null || event.getSchedule().getBookingInfos().isEmpty()) {
				holder.imgTicket.setImageDrawable(res.getDrawable(R.drawable.ic_tickets_unavailable_slider));
				holder.imgTicket.setEnabled(false);
				
			} else {
				holder.imgTicket.setImageDrawable(res.getDrawable(R.drawable.ic_tickets_available_slider));
				holder.imgTicket.setEnabled(true);
			}
			
			updateImgSaveSrc(holder, event, res);
			
			if (rltLytDetailsW == INVALID) {
				holder.rltLytBtm.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
		            @Override
		            public void onGlobalLayout() {
						RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) holder.rltLytDetails.getLayoutParams();

						if (lp.width != RelativeLayout.LayoutParams.MATCH_PARENT) {
							if (VersionUtil.isApiLevelAbove15()) {
								holder.rltLytBtm.getViewTreeObserver().removeOnGlobalLayoutListener(this);

							} else {
								holder.rltLytBtm.getViewTreeObserver().removeGlobalOnLayoutListener(this);
							}
						}
						
						/**
						 * we change rltLytContent width from MATCH_PARENT to fixed value, because 
						 * otherwise on sliding the row, its width goes on increasing (extra width 
						 * added due to negative left margin on sliding the row) hence text starts expanding
						 * along the width while sliding which looks weird.
						 */
						rltLytDetailsW = lp.width = holder.rltLytBtm.getWidth();
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
			
			holder.rltLytBtm.setOnTouchListener(new OnTouchListener() {
				
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
						holder.rltLytBtm.setPressed(true);
						
						holder.lnrSliderContent.setVisibility(View.VISIBLE);
						
						int newX = (int) mEvent.getRawX();
						int dx = newX - pointerX;
						
						int scrollX = rltLytDetailsLP.leftMargin + dx;
						//Log.d(TAG, "move, rltLytContentLP.leftMargin = " + rltLytContentLP.leftMargin + ", lnrDrawerContentW = " + lnrDrawerContentW);
						if (scrollX >= -lnrSliderContentW && scrollX <= 0) {
							holder.imgHandle.setImageResource(R.drawable.ic_more_slider_pressed);
							
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
						holder.rltLytBtm.setPressed(false);
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
									
								} else if (ViewUtil.isPointInsideView(mEvent.getRawX(), mEvent.getRawY(), holder.rltLytBtm)) {
									/**
									 * This block is added to consider row click as event click even when
									 * slider is open (openPos == position); otherwise it won't do anything 
									 * on clicking outside the slider when it's open
									 */
									onEventClick(holder, event);
								}
								
							} else if (ViewUtil.isPointInsideView(mEvent.getRawX(), mEvent.getRawY(), holder.rltLytBtm)) {
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
			
			holder.imgEvt.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					onEventClick(holder, event);
				}
			});
		}
	}
	
	// to update values which should change on orientation change
	public void onActivityCreated() {
		rltLytDetailsW = INVALID;
	}
	
	public void reset() {
		openPos = INVALID;
		setEventsAlreadyRequested(0);
		setMoreDataAvailable(true);
	}
	
	private void onImgTicketClick(final ViewHolder holder, final Event event) {
		holder.imgTicket.setPressed(true);
		handler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				holder.imgTicket.setPressed(false);
				
				EventSeekr eventSeekr = FragmentUtil.getApplication(discoverFragmentTab);
				BaseActivityTab baseActivityTab = (BaseActivityTab) FragmentUtil.getActivity(discoverFragmentTab);
						
				Intent intent = new Intent(eventSeekr, WebViewActivityTab.class);
				intent.putExtra(BundleKeys.URL, event.getSchedule().getBookingInfos().get(0).getBookingUrl());
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
				
				EventSeekr eventSeekr = (EventSeekr) FragmentUtil.getActivity(discoverFragmentTab).getApplication();
				if (event.getAttending() == Attending.SAVED) {
					event.setAttending(Attending.NOT_GOING);
					new UserTracker(Api.OAUTH_TOKEN, eventSeekr, UserTrackingItemType.event, event.getId(), 
							event.getAttending().getValue(), UserTrackingType.Add).execute();
	    			updateImgSaveSrc(holder, event, FragmentUtil.getResources(discoverFragmentTab));
					
				} else {
					discoverFragmentTab.setEvent(event);
					eventPendingPublish = event;
					holderPendingPublish = holder;
					
					if (eventSeekr.getGPlusUserId() != null) {
						event.setNewAttending(Attending.SAVED);
						discoverFragmentTab.handlePublishEvent();
						
					} else {
						fbCallCountForSameEvt = 0;
						event.setNewAttending(Attending.SAVED);
						//NOTE: THIS CAN BE TESTED WITH PODUCTION BUILD ONLY
						FbUtil.handlePublishEvent(discoverFragmentTab, discoverFragmentTab, AppConstants.PERMISSIONS_FB_PUBLISH_EVT_OR_ART, 
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
						ScreenNames.DISCOVER);
				/**
				 * Passing activity fragment manager, since using this fragment's child fragment manager 
				 * doesn't retain dialog on orientation change
				 */
				shareViaDialogFragment.show(((BaseActivityTab)FragmentUtil.getActivity(discoverFragmentTab))
						.getSupportFragmentManager(), FragmentUtil.getTag(ShareViaDialogFragment.class));
			}
		}, 200);
	}
	
	private void onEventClick(final ViewHolder holder, final Event event) {
		holder.rltLytBtm.setPressed(true);
		handler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				//Log.d(TAG, "AT issue event = " + event);
				((EventListenerTab) FragmentUtil.getActivity(discoverFragmentTab)).onEventSelected(event, 
						holder.imgEvt, holder.txtEvtTitle);
				holder.rltLytBtm.setPressed(false);
			}
		}, 200);
	}
	
	private void updateImgSaveSrc(ViewHolder holder, Event event, Resources res) {
		//Log.d(TAG, "updateImgSaveSrc() - event name = " + event.getName() + ", attending = " 
				//+ event.getAttending().getValue());
		int drawableId = (event.getAttending() == Attending.SAVED) ? R.drawable.ic_saved_event_slider 
				: R.drawable.ic_unsaved_event_slider;
		holder.imgSave.setImageDrawable(res.getDrawable(drawableId));
	}
	
	private void openSlider(ViewHolder holder, int position, boolean isUserInitiated) {
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
			Animation slide = AnimationUtils.loadAnimation(FragmentUtil.getActivity(
					discoverFragmentTab), R.anim.slide_in_from_left);
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
					discoverFragmentTab), android.R.anim.slide_out_right);
			slide.setAnimationListener(new AnimationListener() {
				
				@Override
				public void onAnimationStart(Animation animation) {}
				
				@Override
				public void onAnimationRepeat(Animation animation) {}
				
				@Override
				public void onAnimationEnd(Animation animation) {
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
	
	private void updateOpenPos(int openPos, ViewGroup parent) {
		//Log.d(TAG, "openPos = " + openPos);
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
	
	public void call(Session session, SessionState state, Exception exception) {
		//Log.i(TAG, "call()");
		fbCallCountForSameEvt++;
		/**
		 * To prevent infinite loop when network is off & we are calling requestPublishPermissions() of FbUtil.
		 */
		if (fbCallCountForSameEvt < AppConstants.MAX_FB_CALL_COUNT_FOR_SAME_EVT_OR_ART) {
			FbUtil.call(session, state, exception, discoverFragmentTab, discoverFragmentTab, AppConstants.PERMISSIONS_FB_PUBLISH_EVT_OR_ART, 
					AppConstants.REQ_CODE_FB_PUBLISH_EVT_OR_ART, eventPendingPublish);
			
		} else {
			fbCallCountForSameEvt = 0;
			discoverFragmentTab.setPendingAnnounce(false);
		}
	}

	public void onPublishPermissionGranted() {
		//Log.d(TAG, "onPublishPermissionGranted()");
		updateImgSaveSrc(holderPendingPublish, eventPendingPublish, FragmentUtil.getResources(discoverFragmentTab));
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
