package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
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
import com.wcities.eventseeker.SearchFragment.SearchFragmentChildListener;
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
import com.wcities.eventseeker.core.Date;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Event.Attending;
import com.wcities.eventseeker.core.Schedule;
import com.wcities.eventseeker.custom.fragment.PublishEventFragment;
import com.wcities.eventseeker.interfaces.CustomSharedElementTransitionSource;
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener;
import com.wcities.eventseeker.interfaces.EventListener;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.interfaces.ReplaceFragmentListener;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FbUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.VersionUtil;
import com.wcities.eventseeker.util.ViewUtil;
import com.wcities.eventseeker.viewdata.SharedElement;
import com.wcities.eventseeker.viewdata.SharedElementPosition;

public class SearchEventsFragment extends PublishEventFragment implements LoadItemsInBackgroundListener, 
		SearchFragmentChildListener, CustomSharedElementTransitionSource {

	private static final String TAG = SearchEventsFragment.class.getName();
	private static final String FRAGMENT_TAG_SHARE_VIA_DIALOG = ShareViaDialogFragment.class.getSimpleName();

	private static final int TRANSLATION_Z_DP = 10;
	private static final int MILES_LIMIT = 10000;
	
	private EventListAdapter eventListAdapter;
	private double[] latLon;


	private String query;
	private List<Event> eventList;
	private LoadEvents loadEvents;
	
	private RecyclerView recyclerVEvents;
	
	private float translationZPx;

	private Handler handler;
	
	private int imgEventPadL, imgEventPadR, imgEventPadT, imgEventPadB;
	
	private List<View> hiddenViews;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Resources res = FragmentUtil.getResources(this);
		translationZPx = ConversionUtil.toPx(res, TRANSLATION_Z_DP);
		handler = new Handler(Looper.getMainLooper());
		
		hiddenViews = new ArrayList<View>();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		calculateDimensions();
		
		View v = inflater.inflate(R.layout.fragment_search_events, null);
		recyclerVEvents = (RecyclerView) v.findViewById(R.id.recyclerVEvents);

		// use a linear layout manager
		RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(FragmentUtil.getActivity(this));
		recyclerVEvents.setLayoutManager(layoutManager);
		
		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (eventList == null) {
			
			Bundle args = getArguments();
			if (args != null && args.containsKey(BundleKeys.QUERY)) {				
				eventList = new ArrayList<Event>();
				eventList.add(null);

				eventListAdapter = new EventListAdapter(null, eventList, this, this);
				
				query = args.getString(BundleKeys.QUERY);
				loadItemsInBackground();
			}
			
		} else {
			// to update values which should change on orientation change
			eventListAdapter.onActivityCreated();
		}
		
		recyclerVEvents.setAdapter(eventListAdapter);
	}

	@Override
	public void loadItemsInBackground() {
		if (latLon == null) {
			latLon = DeviceUtil.getLatLon(FragmentUtil.getApplication(this));
		}
		
		Calendar c = Calendar.getInstance();
		String startDate = ConversionUtil.getDay(c);
		c.add(Calendar.YEAR, 1);
		String endDate = ConversionUtil.getDay(c);
		
		loadEvents = new LoadEvents(Api.OAUTH_TOKEN, eventList, eventListAdapter, query,
				latLon[0], latLon[1], MILES_LIMIT, null, startDate, endDate);
		eventListAdapter.setLoadDateWiseEvents(loadEvents);
		AsyncTaskUtil.executeAsyncTask(loadEvents, true);
	}
	
	private void calculateDimensions() {
		Resources res = FragmentUtil.getResources(this);
		imgEventPadL = res.getDimensionPixelSize(R.dimen.img_event_pad_l_list_item_discover);
		imgEventPadR = res.getDimensionPixelSize(R.dimen.img_event_pad_r_list_item_discover);
		imgEventPadT = res.getDimensionPixelSize(R.dimen.img_event_pad_t_list_item_discover);
		imgEventPadB = res.getDimensionPixelSize(R.dimen.img_event_pad_b_list_item_discover);
	}

	private void refresh(String newQuery) {
		// if user selection has changed then only reset the list
		if (query == null || !query.equals(newQuery)) {
			query = newQuery;
			eventListAdapter.setEventsAlreadyRequested(0);
			eventListAdapter.setMoreDataAvailable(true);

			if (loadEvents != null && loadEvents.getStatus() != Status.FINISHED) {
				loadEvents.cancel(true);
			}

			eventList.clear();
			eventList.add(null);
			eventListAdapter.notifyDataSetChanged();

			loadItemsInBackground();
		}
	}

	@Override
	public void onQueryTextSubmit(String query) {
		refresh(query);
	}

	@Override
	public void call(Session session, SessionState state, Exception exception) {		
		eventListAdapter.call(session, state, exception);
	}

	@Override
	public void onPublishPermissionGranted() {
		eventListAdapter.onPublishPermissionGranted();
	}
	
	protected static class EventListAdapter extends RecyclerView.Adapter<EventListAdapter.ViewHolder> implements 
			DateWiseEventParentAdapterListener {

		private static final int INVALID = -1;
		
		private AsyncTask<Void, Void, List<Event>> loadDateWiseEvents;
		private List<Event> eventList;
		private int eventsAlreadyRequested;
		private boolean isMoreDataAvailable = true;
		private BitmapCache bitmapCache;
		private LoadItemsInBackgroundListener mListener;
		private SearchEventsFragment searchEventFragment;
		private RecyclerView recyclerView;
		private int openPos = INVALID;
		private int rltLytContentInitialMarginL, lnrSliderContentW, imgEventW, rltLytContentW = INVALID;
		
		private int fbCallCountForSameEvt = 0;
		private EventListAdapter.ViewHolder holderPendingPublish;
		private Event eventPendingPublish;
		
		private static enum ViewType {
			PROGRESS, CONTENT, NO_ITEMS
		};
		
		private static class ViewHolder extends RecyclerView.ViewHolder {
			
			private View root, vHandle;
		    private TextView txtEvtTitle, txtEvtTime, txtEvtLocation, txtNoItemsFound;
		    private ImageView imgEvent, imgTicket, imgSave, imgShare;
		    private LinearLayout lnrSliderContent;
		    private RelativeLayout rltLytRoot, rltLytContent;
		    
		    public ViewHolder(View root) {
		        super(root);
		        this.root = root;
		        txtEvtTitle = (TextView) root.findViewById(R.id.txtEvtTitle);
		        txtEvtTime = (TextView) root.findViewById(R.id.txtEvtTime);
		        txtEvtLocation = (TextView) root.findViewById(R.id.txtEvtLocation);
		        imgEvent = (ImageView) root.findViewById(R.id.imgEvent);
		        vHandle = root.findViewById(R.id.vHandle);
		        lnrSliderContent = (LinearLayout) root.findViewById(R.id.lnrSliderContent);
		        rltLytRoot = (RelativeLayout) root.findViewById(R.id.rltLytRoot);
		        rltLytContent = (RelativeLayout) root.findViewById(R.id.rltLytContent);
		        imgTicket = (ImageView) root.findViewById(R.id.imgTicket);
		        imgSave = (ImageView) root.findViewById(R.id.imgSave);
		        imgShare = (ImageView) root.findViewById(R.id.imgShare);
		        txtNoItemsFound = (TextView) root.findViewById(R.id.txtNoItemsFound);
		    }
		    
		    private boolean isSliderClose(int rltLytContentInitialMarginL) {
		    	RelativeLayout.LayoutParams rltLytContentLP = (RelativeLayout.LayoutParams) rltLytContent.getLayoutParams();
				return (rltLytContentLP.leftMargin == rltLytContentInitialMarginL);
		    }
		}
		
		public EventListAdapter(AsyncTask<Void, Void, List<Event>> loadDateWiseEvents, List<Event> eventList, 
				LoadItemsInBackgroundListener mListener, SearchEventsFragment searchEventFragment) {
			this.loadDateWiseEvents = loadDateWiseEvents;
			this.eventList = eventList;
			this.mListener = mListener;
			this.searchEventFragment = searchEventFragment;
			bitmapCache = BitmapCache.getInstance();
			Resources res = FragmentUtil.getResources(searchEventFragment);
			rltLytContentInitialMarginL = res.getDimensionPixelSize(R.dimen.rlt_lyt_content_margin_l_list_item_discover);
			lnrSliderContentW = res.getDimensionPixelSize(R.dimen.lnr_slider_content_w_list_item_discover);
			imgEventW = res.getDimensionPixelSize(R.dimen.img_event_w_list_item_discover);
		}
		
		@Override
		public int getItemViewType(int position) {
			//Log.d(TAG, "getItemViewType() - pos = " + position);
			if (eventList.get(position) == null) {
				return ViewType.PROGRESS.ordinal();
			
			} else if (eventList.get(position).getId() == AppConstants.INVALID_ID) {
				return ViewType.NO_ITEMS.ordinal();				
				
			} else {
				return ViewType.CONTENT.ordinal();
			}
		}
		
		@Override
		public int getItemCount() {
			return eventList.size();
		}
		
		@Override
		public void onBindViewHolder(final ViewHolder holder, final int position) {
		//Log.d(TAG, "onBindViewHolder(), pos = " + position);
		
			final Event event = eventList.get(position);
			
			if (event == null) {
				// progress indicator
				
				if ((loadDateWiseEvents == null || loadDateWiseEvents.getStatus() == Status.FINISHED) && 
						isMoreDataAvailable) {
					//Log.d(TAG, "onBindViewHolder(), pos = " + position);
					mListener.loadItemsInBackground();
				}
				
			} else {
				if (event.getId() == AppConstants.INVALID_ID) {
					holder.txtNoItemsFound.setText(R.string.no_event_found);
					return;
				}
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
					holder.txtEvtTime.setText(ConversionUtil.getDateTime(date.getStartDate(), date.isStartTimeAvailable()));
					
					String venueName = (schedule.getVenue() != null) ? schedule.getVenue().getName() : "";
					holder.txtEvtLocation.setText(venueName);
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
				        holder.imgEvent.setImageBitmap(bitmap);
				        
				    } else {
				    	holder.imgEvent.setImageBitmap(null);
				    	AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
				        asyncLoadImg.loadImg(holder.imgEvent, ImgResolution.LOW, recyclerView, position, bitmapCacheable);
				    }
				}
				//ViewCompat.setTransitionName(holder.imgEvent, TransitionName.DISCOVER_IMG_EVT + position);
				
				final Resources res = FragmentUtil.getResources(searchEventFragment);
				if (event.getSchedule() == null || event.getSchedule().getBookingInfos().isEmpty()) {
					holder.imgTicket.setImageDrawable(res.getDrawable(R.drawable.tickets_disabled));
					holder.imgTicket.setEnabled(false);
					
				} else {
					holder.imgTicket.setImageDrawable(res.getDrawable(R.drawable.tic_blue));
					holder.imgTicket.setEnabled(true);
				}
				
				updateImgSaveSrc(holder, event, res);
				
				if (rltLytContentW == INVALID) {
					/**
					 * Setting global layout listener on rltLytRoot instead of rltLytContent, because 
					 * on nexus 5, when orientation changes from portrait to landscape onGlobalLayout() 
					 * of rltLytContent returns wrong width (126px less than actual width)
					 */
					holder.rltLytRoot.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			            @Override
			            public void onGlobalLayout() {
							RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) holder.rltLytContent.getLayoutParams();
		
							/**
							 * Following condition is to prevent above mentioned situation for nexus 5, 
							 * where orientation change from portrait to landscape returns less width (by 126px)
							 * for first time even when global layout listener is set on rltLytRoot.
							 */
							if (lp.width != RelativeLayout.LayoutParams.MATCH_PARENT) {
								if (VersionUtil.isApiLevelAbove15()) {
									holder.rltLytRoot.getViewTreeObserver().removeOnGlobalLayoutListener(this);
		
								} else {
									holder.rltLytRoot.getViewTreeObserver().removeGlobalOnLayoutListener(this);
								}
							}
							
							rltLytContentW = lp.width = (holder.rltLytRoot.getWidth() - imgEventW);
							holder.rltLytContent.setLayoutParams(lp);
			            	/*Log.d(TAG, "onGlobalLayout(), rltLytContentW = " + rltLytContentW + 
			            			", holder.rltLytRoot.getWidth() = " + holder.rltLytRoot.getWidth());*/
		
							if (openPos == position) {
								/**
								 * Now since we know fixed height of rltLytContent, we can update its 
								 * left margin in layoutParams by calling openSlider() which was delayed 
								 * until now [by condition if (rltLytContentW != INVALID) at end of 
								 * onBindViewHolder()].
								 */
								openSlider(holder, position, false);
							}
			            }
			        });
					
				} else {
					RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) holder.rltLytContent.getLayoutParams();
					lp.width = rltLytContentW;
					//Log.d(TAG, "else, rltLytContentW = " + rltLytContentW);
					holder.rltLytContent.setLayoutParams(lp);
				} 
				
				holder.rltLytRoot.setOnTouchListener(new OnTouchListener() {
					
					int MIN_SWIPE_DISTANCE_X = ConversionUtil.toPx(res, 50);
					int MAX_CLICK_DISTANCE = ConversionUtil.toPx(res, 4);
					int pointerX = 0, initX = 0, pointerY = 0, initY = 0;
					boolean isSliderOpenInititally;
					boolean letParentHandleTouchEvent = false;
					int actionMoveCount = 0;
					
					@Override
					public boolean onTouch(View v, MotionEvent mEvent) {
						RelativeLayout.LayoutParams rltLytContentLP = (RelativeLayout.LayoutParams) holder.rltLytContent.getLayoutParams();
						RelativeLayout.LayoutParams lnrSliderContentLP = (RelativeLayout.LayoutParams) holder.lnrSliderContent.getLayoutParams();
						
						switch (mEvent.getAction()) {
						
						case MotionEvent.ACTION_DOWN:
							recyclerView.getParent().requestDisallowInterceptTouchEvent(true);
							//Log.d(TAG, "down, x = " + mEvent.getRawX() + ", y = " + mEvent.getRawY());
							initX = pointerX = (int) mEvent.getRawX();
							initY = pointerY = (int) mEvent.getRawY();
							isSliderOpenInititally = !holder.isSliderClose(rltLytContentInitialMarginL);
							actionMoveCount = 0;
							return true;
						
						case MotionEvent.ACTION_MOVE:
							boolean isDirectionLeftToRight = (mEvent.getRawX() - initX) > 0;
							if (isDirectionLeftToRight && !isSliderOpenInititally 
									|| !isDirectionLeftToRight && isSliderOpenInititally) {
								letParentHandleTouchEvent = true;
								recyclerView.getParent().requestDisallowInterceptTouchEvent(false);
								return false;								
							}
							recyclerView.getParent().requestDisallowInterceptTouchEvent(true);
							//Log.d(TAG, "move");
							holder.rltLytRoot.setPressed(true);
							
							actionMoveCount++;
							holder.lnrSliderContent.setVisibility(View.VISIBLE);
							
							int newX = (int) mEvent.getRawX();
							int dx = newX - pointerX;
							
							int scrollX = rltLytContentLP.leftMargin - rltLytContentInitialMarginL + dx;
							//Log.d(TAG, "move, rltLytContentLP.leftMargin = " + rltLytContentLP.leftMargin + ", lnrDrawerContentW = " + lnrDrawerContentW);
							if (scrollX >= (0 - lnrSliderContentW) && scrollX <= 0) {
								ViewCompat.setElevation(holder.imgEvent, searchEventFragment.translationZPx);
								
								rltLytContentLP.leftMargin = rltLytContentInitialMarginL + scrollX;
								//Log.d(TAG, "onTouch(), ACTION_MOVE");
								holder.rltLytContent.setLayoutParams(rltLytContentLP);
								
								lnrSliderContentLP.rightMargin = rltLytContentInitialMarginL 
										- rltLytContentLP.leftMargin - lnrSliderContentW;
								holder.lnrSliderContent.setLayoutParams(lnrSliderContentLP);
								
								pointerX = newX;
							}
							pointerY = (int) mEvent.getRawY();
							break;
							
						case MotionEvent.ACTION_UP:
						case MotionEvent.ACTION_CANCEL:
							holder.rltLytRoot.setPressed(false);
							if (letParentHandleTouchEvent) {
								letParentHandleTouchEvent = false;//resetting the value
								//return false;
							}
						    //Log.d(TAG, "up, action = " + mEvent.getAction() + ", x = " + mEvent.getRawX() + ", y = " + mEvent.getRawY());
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
								
								// consider click event
								if (actionMoveCount <= 2) {
									if (mEvent.getAction() == MotionEvent.ACTION_CANCEL) {
										break;
									}
									
									if (Math.abs(initX - pointerX) > MAX_CLICK_DISTANCE || 
											Math.abs(initY - pointerY) > MAX_CLICK_DISTANCE) {
										break;
									}
									
									/**
									 * Handle click event.
									 * We do it here instead of implementing onClick listener because then onClick listener
									 * of child element would block onTouch event on its parent (rltLytRoot) 
									 * if this onTouch starts from such a child view 
									 */
									if (ViewUtil.isPointInsideView(mEvent.getRawX(), mEvent.getRawY(), holder.vHandle)) {
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
											
										} else if (ViewUtil.isPointInsideView(mEvent.getRawX(), mEvent.getRawY(), holder.rltLytRoot)) {
											/**
											 * This block is added to consider row click as event click even when
											 * slider is open (openPos == position); otherwise it won't do anything 
											 * on clicking outside the slider when it's open
											 */
											onEventClick(holder, event, position);
										}
										
									} else if (ViewUtil.isPointInsideView(mEvent.getRawX(), mEvent.getRawY(), holder.rltLytRoot)) {
										onEventClick(holder, event, position);
									}
								}
							}
							
							break;
						}
						return true;
					}
				});
				
				if (rltLytContentW != INVALID) {
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
		
		private void updateImgSaveSrc(ViewHolder holder, Event event, Resources res) {
			//Log.d(TAG, "updateImgSaveSrc() - event name = " + event.getName() + ", attending = " + event.getAttending().getValue());
			int drawableId = (event.getAttending() == Attending.SAVED) ? R.drawable.checked_blue : R.drawable.calendar;
			holder.imgSave.setImageDrawable(res.getDrawable(drawableId));
		}
		
		private void openSlider(ViewHolder holder, int position, boolean isUserInitiated) {
			ViewCompat.setElevation(holder.imgEvent, searchEventFragment.translationZPx);
			holder.lnrSliderContent.setVisibility(View.VISIBLE);
			
			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) holder.lnrSliderContent.getLayoutParams();
			lp.rightMargin = 0;
			holder.lnrSliderContent.setLayoutParams(lp);
			
			lp = (RelativeLayout.LayoutParams) holder.rltLytContent.getLayoutParams();
			lp.leftMargin = rltLytContentInitialMarginL - lnrSliderContentW;
			holder.rltLytContent.setLayoutParams(lp);
			//Log.d(TAG, "openSlider()");
			
			if (isUserInitiated) {
				updateOpenPos(position, recyclerView);
			}
		}
		
		private void closeSlider(ViewHolder holder, int position, boolean isUserInitiated) {
			holder.lnrSliderContent.setVisibility(View.INVISIBLE);
			
			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) holder.lnrSliderContent.getLayoutParams();
			lp.rightMargin = 0 - lnrSliderContentW;;
			holder.lnrSliderContent.setLayoutParams(lp);
			
			lp = (RelativeLayout.LayoutParams) holder.rltLytContent.getLayoutParams();
			lp.leftMargin = rltLytContentInitialMarginL;
			holder.rltLytContent.setLayoutParams(lp);
			//Log.d(TAG, "closeSlider()");
			
			ViewCompat.setElevation(holder.imgEvent, 0);
			
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
		
		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			//Log.d(TAG, "onCreateViewHolder(), viewType = " + viewType);
			View v;
			
			recyclerView = (RecyclerView) parent;
			
			switch (viewType) {
			
			case 0:
				v = LayoutInflater.from(parent.getContext()).inflate(R.layout.progress_bar_eventseeker_fixed_ht, parent, false);
				break;
				
			case 1:
				v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list_search_event, parent, false);
				break;

			case 2:
				v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_no_items_found, parent, false);
				break;
				
			default:
				v = null;
				break;
			}
			
			ViewHolder vh = new ViewHolder(v);
		    return vh;
		}
		
		private void onHandleClick(final ViewHolder holder, final int position) {
			//Log.d(TAG, "onHandleClick()");
			holder.vHandle.setPressed(true);
			
			if (holder.isSliderClose(rltLytContentInitialMarginL)) {
				// slider is close, so open it
				//Log.d(TAG, "open slider");
				ViewCompat.setElevation(holder.imgEvent, searchEventFragment.translationZPx);
				
				Animation slide = AnimationUtils.loadAnimation(FragmentUtil.getActivity(
						searchEventFragment), R.anim.slide_in_from_left);
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
						holder.vHandle.setPressed(false);
						
						RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) 
								holder.rltLytContent.getLayoutParams();
						lp.leftMargin -= holder.lnrSliderContent.getWidth();
						holder.rltLytContent.setLayoutParams(lp);
						//Log.d(TAG, "isSliderClose");
						
						updateOpenPos(position, recyclerView);
					}
				});
				holder.lnrSliderContent.startAnimation(slide);
				
			} else {
				// slider is open, so close it
				//Log.d(TAG, "close slider");
				Animation slide = AnimationUtils.loadAnimation(FragmentUtil.getActivity(
						searchEventFragment), android.R.anim.slide_out_right);
				slide.setAnimationListener(new AnimationListener() {
					
					@Override
					public void onAnimationStart(Animation animation) {}
					
					@Override
					public void onAnimationRepeat(Animation animation) {}
					
					@Override
					public void onAnimationEnd(Animation animation) {
						ViewCompat.setElevation(holder.imgEvent, 0);
						
						holder.vHandle.setPressed(false);
						
						holder.lnrSliderContent.setVisibility(View.INVISIBLE);
						RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) holder.lnrSliderContent.getLayoutParams();
						lp.rightMargin = 0 - lnrSliderContentW;;
						holder.lnrSliderContent.setLayoutParams(lp);
						
						lp = (RelativeLayout.LayoutParams) holder.rltLytContent.getLayoutParams();
						lp.leftMargin += holder.lnrSliderContent.getWidth();
						holder.rltLytContent.setLayoutParams(lp);
						//Log.d(TAG, "!isSliderClose");
						
						updateOpenPos(INVALID, null);
					}
				});
				holder.lnrSliderContent.startAnimation(slide);
			}
		}
		
		private void onEventClick(final ViewHolder holder, final Event event, final int position) {
			holder.rltLytRoot.setPressed(true);
			
			searchEventFragment.handler.postDelayed(new Runnable() {
				
				@Override
				public void run() {
					List<SharedElement> sharedElements = new ArrayList<SharedElement>();
					
					int[] loc = ViewUtil.getLocationOnScreen(holder.itemView, FragmentUtil.getResources(searchEventFragment));
					
					SharedElementPosition sharedElementPosition = new SharedElementPosition(searchEventFragment.imgEventPadL, 
							loc[1] + searchEventFragment.imgEventPadT, 
							holder.imgEvent.getWidth() - searchEventFragment.imgEventPadL - searchEventFragment.imgEventPadR, 
							holder.imgEvent.getHeight() - searchEventFragment.imgEventPadT - searchEventFragment.imgEventPadB);
					SharedElement sharedElement = new SharedElement(sharedElementPosition, holder.imgEvent);
					sharedElements.add(sharedElement);
					searchEventFragment.addViewsToBeHidden(holder.imgEvent);
					
					//Log.d(TAG, "AT issue event = " + event);
					((EventListener) FragmentUtil.getActivity(searchEventFragment)).onEventSelected(event, sharedElements);
					
					searchEventFragment.onPushedToBackStack();
					
					holder.rltLytRoot.setPressed(false);
				}
			}, 200);
		}
		
		private void onImgTicketClick(final ViewHolder holder, final Event event) {
			holder.imgTicket.setPressed(true);
			searchEventFragment.handler.postDelayed(new Runnable() {
				
				@Override
				public void run() {
					holder.imgTicket.setPressed(false);
					Bundle args = new Bundle();
					args.putString(BundleKeys.URL, event.getSchedule().getBookingInfos().get(0).getBookingUrl());
					((ReplaceFragmentListener) FragmentUtil.getActivity(searchEventFragment)).replaceByFragment(
							AppConstants.FRAGMENT_TAG_WEB_VIEW, args);
					/**
					 * added on 15-12-2014
					 */
					GoogleAnalyticsTracker.getInstance().sendEvent(FragmentUtil.getApplication(searchEventFragment), 
							((SearchFragment) searchEventFragment.getParentFragment()).getScreenName(), 
							GoogleAnalyticsTracker.EVENT_LABEL_TICKETS_BUTTON, 
							GoogleAnalyticsTracker.Type.Event.name(), null, event.getId());
				}
			}, 200);
		}
		
		private void onImgSaveClick(final ViewHolder holder, final Event event) {
			holder.imgSave.setPressed(true);
			searchEventFragment.handler.postDelayed(new Runnable() {
				
				@Override
				public void run() {
					holder.imgSave.setPressed(false);
					
					EventSeekr eventSeekr = (EventSeekr) FragmentUtil.getActivity(searchEventFragment).getApplication();
					if (event.getAttending() == Attending.SAVED) {
						event.setAttending(Attending.NOT_GOING);
						new UserTracker(Api.OAUTH_TOKEN, eventSeekr, UserTrackingItemType.event, event.getId(), 
								event.getAttending().getValue(), UserTrackingType.Add).execute();
		    			updateImgSaveSrc(holder, event, FragmentUtil.getResources(searchEventFragment));
						
					} else {
						searchEventFragment.event = eventPendingPublish = event;
						holderPendingPublish = holder;
						
						if (eventSeekr.getGPlusUserId() != null) {
							event.setNewAttending(Attending.SAVED);
							searchEventFragment.handlePublishEvent();
							
						} else {
							fbCallCountForSameEvt = 0;
							event.setNewAttending(Attending.SAVED);
							//NOTE: THIS CAN BE TESTED WITH PODUCTION BUILD ONLY
							FbUtil.handlePublishEvent(searchEventFragment, searchEventFragment, AppConstants.PERMISSIONS_FB_PUBLISH_EVT_OR_ART, 
									AppConstants.REQ_CODE_FB_PUBLISH_EVT_OR_ART, event);
						}
					}
				}
			}, 200);
		}
		
		private void onImgShareClick(final ViewHolder holder, final Event event) {
			holder.imgShare.setPressed(true);
			searchEventFragment.handler.postDelayed(new Runnable() {
				
				@Override
				public void run() {
					holder.imgShare.setPressed(false);
					
					ShareViaDialogFragment shareViaDialogFragment = ShareViaDialogFragment.newInstance(event, 
							((SearchFragment) searchEventFragment.getParentFragment()).getScreenName());
					/**
					 * Passing activity fragment manager, since using this fragment's child fragment manager 
					 * doesn't retain dialog on orientation change
					 */
					shareViaDialogFragment.show(((FragmentActivity)FragmentUtil.getActivity(searchEventFragment))
							.getSupportFragmentManager(), FRAGMENT_TAG_SHARE_VIA_DIALOG);
				}
			}, 200);
		}
		
		// to update values which should change on orientation change
		private void onActivityCreated() {
			rltLytContentW = INVALID;
			Resources res = FragmentUtil.getResources(searchEventFragment);
			rltLytContentInitialMarginL = res.getDimensionPixelSize(R.dimen.rlt_lyt_content_margin_l_list_item_discover);
			lnrSliderContentW = res.getDimensionPixelSize(R.dimen.lnr_slider_content_w_list_item_discover);
		}
		
		private void call(Session session, SessionState state, Exception exception) {
			//Log.i(TAG, "call()");
			fbCallCountForSameEvt++;
			/**
			 * To prevent infinite loop when network is off & we are calling requestPublishPermissions() of FbUtil.
			 */
			if (fbCallCountForSameEvt < AppConstants.MAX_FB_CALL_COUNT_FOR_SAME_EVT_OR_ART) {
				FbUtil.call(session, state, exception, searchEventFragment, searchEventFragment, AppConstants.PERMISSIONS_FB_PUBLISH_EVT_OR_ART, 
						AppConstants.REQ_CODE_FB_PUBLISH_EVT_OR_ART, eventPendingPublish);
				
			} else {
				fbCallCountForSameEvt = 0;
				searchEventFragment.setPendingAnnounce(false);
			}
		}
		
		private void onPublishPermissionGranted() {
			//Log.d(TAG, "onPublishPermissionGranted()");
			updateImgSaveSrc(holderPendingPublish, eventPendingPublish, FragmentUtil.getResources(searchEventFragment));
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
		}
		
		@Override
		public void setLoadDateWiseEvents(AsyncTask<Void, Void, List<Event>> loadDateWiseEvents) {
			this.loadDateWiseEvents = loadDateWiseEvents;
		}
	}

	@Override
	public void onPoppedFromBackStack() {
		// to update statusbar visibility
		getParentFragment().onStart();
		// to call onFragmentResumed(Fragment) of MainActivity (to update title, current fragment tag, etc.)
		getParentFragment().onResume();
		
		for (Iterator<View> iterator = hiddenViews.iterator(); iterator.hasNext();) {
			View view = iterator.next();
			view.setVisibility(View.VISIBLE);
		}
		hiddenViews.clear();
	}

	@Override
	public void hideSharedElements() {
		for (Iterator<View> iterator = hiddenViews.iterator(); iterator.hasNext();) {
			View view = iterator.next();
			view.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	public void onPushedToBackStack() {
		/**
		 * here, no need to call super.onStop() (to remove fb callback), since we are not calling onStart() 
		 * on this fragment from onPoppedFromBackStack(), which would have added fb callback again
		 */
		//super.onStop();
	}

	@Override
	public void addViewsToBeHidden(View... views) {
		for (int i = 0; i < views.length; i++) {
			hiddenViews.add(views[i]);
		}
	}
}
