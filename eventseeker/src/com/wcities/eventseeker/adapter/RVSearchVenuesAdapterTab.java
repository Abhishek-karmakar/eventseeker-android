package com.wcities.eventseeker.adapter;

import java.lang.ref.WeakReference;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.AdapterDataObserver;
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

import com.wcities.eventseeker.NavigationActivityTab;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.SearchVenuesFragmentTab;
import com.wcities.eventseeker.WebViewActivityTab;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Venue;
import com.wcities.eventseeker.interfaces.FullScrnProgressListener;
import com.wcities.eventseeker.interfaces.VenueAdapterListener;
import com.wcities.eventseeker.interfaces.VenueListenerTab;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.VersionUtil;
import com.wcities.eventseeker.util.ViewUtil;

public class RVSearchVenuesAdapterTab<T> extends RVAdapterBase<RVSearchVenuesAdapterTab.ViewHolder> implements VenueAdapterListener<T> {
	
	private static final String TAG = RVSearchVenuesAdapterTab.class.getSimpleName();
	
	private static final int INVALID = -1;
	
	private SearchVenuesFragmentTab searchVenuesFragmentTab;
	private List<Venue> venueList;
	private AsyncTask<T, Void, List<Venue>> loadVenues;
	
	private RecyclerView recyclerView;
	private WeakReference<RecyclerView> weakRecyclerView;
	
	private int venuesAlreadyRequested;
	private boolean isMoreDataAvailable = true, isVisible = true;
	
	private BitmapCache bitmapCache;
	private Handler handler;
	
	private int lnrSliderContentW, openPos = INVALID, rltLytDetailsW = INVALID;
	
	private static enum ViewType {
		VENUE;
	};

	public static class ViewHolder extends RecyclerView.ViewHolder {
		
		private RelativeLayout rltLytRoot, rltLytDetails, rltLytBtm, rltLytRootPrgs;
		private LinearLayout lnrSliderContent;
		private TextView txtVenueTitle, txtLoc;
		private ImageView imgVenue, imgHandle, imgPhone, imgWeb, imgDrive;

		public ViewHolder(View itemView) {
			super(itemView);
			
			rltLytRootPrgs = (RelativeLayout) itemView.findViewById(R.id.rltLytRootPrgs);
			rltLytRoot = (RelativeLayout) itemView.findViewById(R.id.rltLytRoot);
			rltLytBtm = (RelativeLayout) itemView.findViewById(R.id.rltLytBtm);
			rltLytDetails = (RelativeLayout) itemView.findViewById(R.id.rltLytDetails);
			lnrSliderContent = (LinearLayout) itemView.findViewById(R.id.lnrSliderContent);
			
			txtVenueTitle = (TextView) itemView.findViewById(R.id.txtVenueTitle);
			txtLoc = (TextView) itemView.findViewById(R.id.txtLoc);
			
			imgVenue = (ImageView) itemView.findViewById(R.id.imgVenue);
			imgHandle = (ImageView) itemView.findViewById(R.id.imgHandle);
			imgPhone = (ImageView) itemView.findViewById(R.id.imgPhone);
			imgWeb = (ImageView) itemView.findViewById(R.id.imgWeb);
			imgDrive = (ImageView) itemView.findViewById(R.id.imgDrive);
		}
		
		private boolean isSliderClose() {
        	RelativeLayout.LayoutParams rltLytContentLP = (RelativeLayout.LayoutParams) rltLytDetails.getLayoutParams();
			return (rltLytContentLP.leftMargin == 0);
        }
	}

	public RVSearchVenuesAdapterTab(SearchVenuesFragmentTab searchVenuesFragmentTab) {
		this.searchVenuesFragmentTab = searchVenuesFragmentTab;
		
		venueList = searchVenuesFragmentTab.getVenueList();
		bitmapCache = BitmapCache.getInstance();
		handler = new Handler(Looper.getMainLooper());
		
		Resources res = FragmentUtil.getResources(searchVenuesFragmentTab);
		lnrSliderContentW = res.getDimensionPixelSize(R.dimen.lnr_slider_content_w_rv_item_event_tab);
	}
	
	@Override
	public int getItemCount() {
		return venueList.size();
	}
	
	@Override
	public int getItemViewType(int position) {
		return ViewType.VENUE.ordinal();
	}
	
	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		if (recyclerView != parent) {
			recyclerView = (RecyclerView) parent;
			weakRecyclerView = new WeakReference<RecyclerView>(recyclerView);
		}
		
		View v;
		
		ViewType vType = ViewType.values()[viewType];
		switch (vType) {
		
		case VENUE:
			v = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_item_venue_tab, parent, false);
			break;
			
		default:
			v = null;
			break;
		}
		
        return new ViewHolder(v);
	}

	@Override
	public void onBindViewHolder(final ViewHolder holder, final int position) {
		final Venue venue = venueList.get(position);
		if (venue == null) {
			// progress indicator

			holder.itemView.setVisibility(View.VISIBLE);
			
			if (venueList.size() == 1) {
				// no events loaded yet
				((FullScrnProgressListener) searchVenuesFragmentTab).displayFullScrnProgress();
				
			} else {
				holder.rltLytRootPrgs.setVisibility(View.VISIBLE);
				holder.rltLytRoot.setVisibility(View.INVISIBLE);
			}
			
			if ((loadVenues == null || loadVenues.getStatus() == Status.FINISHED) && 
					isMoreDataAvailable) {
				//Log.d(TAG, "onBindViewHolder(), pos = " + position);
				searchVenuesFragmentTab.loadItemsInBackground();
			}
			
		} else {
			if (venue.getId() == AppConstants.INVALID_ID) {
				searchVenuesFragmentTab.displayNoItemsFound();
				holder.itemView.setVisibility(View.INVISIBLE);
				
			} else {
				holder.itemView.setVisibility(View.VISIBLE);
				holder.rltLytRootPrgs.setVisibility(View.INVISIBLE);
				holder.rltLytRoot.setVisibility(View.VISIBLE);
				
				holder.txtVenueTitle.setText(venue.getName());
				ViewCompat.setTransitionName(holder.txtVenueTitle, "txtVenueTitleSearch" + position);
				
				holder.txtLoc.setText(getVenueAddress(venue));
				
				if (!isVisible) {
					// free memory
					holder.imgVenue.setImageBitmap(null);
					
				} else {
					String key = venue.getKey(ImgResolution.LOW);
					// set tag to compare it in AsyncLoadImg before setting bitmap to imageview
			    	holder.imgVenue.setTag(key);
	
					Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
					if (bitmap != null) {
						//Log.d(TAG, "bitmap != null");
				        holder.imgVenue.setImageBitmap(bitmap);
				        
				    } else {
				    	holder.imgVenue.setImageBitmap(null);
				    	AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
				        asyncLoadImg.loadImg(holder.imgVenue, ImgResolution.LOW, weakRecyclerView, position, venue);
				    }
				}
				
				ViewCompat.setTransitionName(holder.imgVenue, "imgVenueSearch" + position);
				
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
				
				final Resources res = FragmentUtil.getResources(searchVenuesFragmentTab);
				
				holder.rltLytBtm.setOnTouchListener(new OnTouchListener() {
					
					int MIN_SWIPE_DISTANCE_X = ConversionUtil.toPx(res, 50);
					int MAX_CLICK_DISTANCE = ConversionUtil.toPx(res, 4);
					int pointerX = 0, initX = 0, pointerY = 0, initY = 0, maxMovedOnX = 0, maxMovedOnY = 0;
					boolean isSliderOpenInititally;
					private boolean letParentHandleTouchEvent = false;
					
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
							boolean isDirectionLeftToRight = (mEvent.getRawX() - initX) > 0;
							if (isDirectionLeftToRight && !isSliderOpenInititally 
									|| !isDirectionLeftToRight && isSliderOpenInititally) {
								letParentHandleTouchEvent = true;
								recyclerView.getParent().requestDisallowInterceptTouchEvent(false);
								return false;								
							}
							recyclerView.getParent().requestDisallowInterceptTouchEvent(true);
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
							if (letParentHandleTouchEvent) {
								letParentHandleTouchEvent = false;//resetting the value
								//return false;
							}
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
									if (ViewUtil.isPointInsideView(mEvent.getRawX(), mEvent.getRawY(), holder.imgPhone)) {
										onImgPhoneClick(holder, venue);
											
									} else if (ViewUtil.isPointInsideView(mEvent.getRawX(), mEvent.getRawY(), holder.imgWeb)) {
										onImgWebClick(holder, venue);
										
									} else if (ViewUtil.isPointInsideView(mEvent.getRawX(), mEvent.getRawY(), holder.imgDrive)) {
										onImgDriveClick(holder, venue);
										
									} else if (ViewUtil.isPointInsideView(mEvent.getRawX(), mEvent.getRawY(), holder.rltLytBtm)) {
										/**
										 * This block is added to consider row click as event click even when
										 * slider is open (openPos == position); otherwise it won't do anything 
										 * on clicking outside the slider when it's open
										 */
										onVenueClick(holder, venue);
									}
									
								} else if (ViewUtil.isPointInsideView(mEvent.getRawX(), mEvent.getRawY(), holder.rltLytBtm)) {
									onVenueClick(holder, venue);
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
				
				holder.imgVenue.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						onVenueClick(holder, venue);
					}
				});
			}
		}
	}
	
	private void onVenueClick(final ViewHolder holder, final Venue venue) {
		holder.rltLytBtm.setPressed(true);
		handler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				//Log.d(TAG, "AT issue event = " + event);
				((VenueListenerTab) FragmentUtil.getActivity(searchVenuesFragmentTab)).onVenueSelected(venue, 
						holder.imgVenue, holder.txtVenueTitle);
				holder.rltLytBtm.setPressed(false);
			}
		}, 200);
	}
	
	private void onImgPhoneClick(final ViewHolder holder, final Venue venue) {
		holder.imgPhone.setPressed(true);
		handler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				holder.imgPhone.setPressed(false);
				
				if (venue.getPhone() != null) {
					Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + venue.getPhone()));
					searchVenuesFragmentTab.startActivity(Intent.createChooser(intent, "Call..."));
					
				} else {
					Toast.makeText(FragmentUtil.getActivity(searchVenuesFragmentTab), R.string.phone_number_not_available, 
							Toast.LENGTH_SHORT).show();
				}
			}
		}, 200);
	}
	
	private void onImgWebClick(final ViewHolder holder, final Venue venue) {
		holder.imgWeb.setPressed(true);
		handler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				holder.imgWeb.setPressed(false);
				
				if (venue.getUrl() != null) {
					Intent intent = new Intent(FragmentUtil.getApplication(searchVenuesFragmentTab), WebViewActivityTab.class);
					intent.putExtra(BundleKeys.URL, venue.getUrl());
					searchVenuesFragmentTab.startActivity(intent);
					
				} else {
					Toast.makeText(FragmentUtil.getActivity(searchVenuesFragmentTab), R.string.web_address_isnt_available, 
							Toast.LENGTH_SHORT).show();
				}
			}
		}, 200);
	}
	
	private void onImgDriveClick(final ViewHolder holder, final Venue venue) {
		holder.imgDrive.setPressed(true);
		handler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				holder.imgDrive.setPressed(false);
				
				Intent intent = new Intent(FragmentUtil.getApplication(searchVenuesFragmentTab), NavigationActivityTab.class);
				intent.putExtra(BundleKeys.VENUE, venue);
				searchVenuesFragmentTab.startActivity(intent);
			}
		}, 200);
	}
	
	private void onHandleClick(final ViewHolder holder, final int position) {
		//Log.d(TAG, "onHandleClick()");
		holder.imgHandle.setImageResource(R.drawable.ic_more_slider_pressed);
		
		if (holder.isSliderClose()) {
			// slider is close, so open it
			//Log.d(TAG, "open slider");
			Animation slide = AnimationUtils.loadAnimation(FragmentUtil.getActivity(
					searchVenuesFragmentTab), R.anim.slide_in_from_left);
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
					searchVenuesFragmentTab), android.R.anim.slide_out_right);
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
	
	private String getVenueAddress(Venue venue) {
		String location = "";
		if (venue.getAddress() != null) {
			if (venue.getAddress().getAddress1() != null) {
				location += venue.getAddress().getAddress1();
			}

			if (venue.getAddress().getAddress2() != null) {
				if (location.length() != 0) {
					location += ", ";
				}
				location += venue.getAddress().getAddress2();
			}
			
			if (venue.getAddress().getCity() != null) {
				location += ", " + venue.getAddress().getCity();
			} 
		}
		return location;
	}

	// to update values which should change on orientation change
	public void onActivityCreated() {
		rltLytDetailsW = INVALID;
	}
	
	public void reset() {
		openPos = INVALID;
		setVenuesAlreadyRequested(0);
		setMoreDataAvailable(true);
	}

	public void setVisible(boolean isVisible) {
		this.isVisible = isVisible;
	}

	@Override
	public void setMoreDataAvailable(boolean isMoreDataAvailable) {
		this.isMoreDataAvailable = isMoreDataAvailable;
	}

	@Override
	public void setVenuesAlreadyRequested(int venuesAlreadyRequested) {
		this.venuesAlreadyRequested = venuesAlreadyRequested;
	}

	@Override
	public int getVenuesAlreadyRequested() {
		return venuesAlreadyRequested;
	}

	@Override
	public void updateContext(Context context) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setLoadVenues(AsyncTask<T, Void, List<Venue>> loadVenues) {
		this.loadVenues = loadVenues;
	}
}
