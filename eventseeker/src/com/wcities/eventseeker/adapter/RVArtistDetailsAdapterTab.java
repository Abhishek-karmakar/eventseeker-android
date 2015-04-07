package com.wcities.eventseeker.adapter;

import java.lang.ref.WeakReference;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.AdapterDataObserver;
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

import com.facebook.Session;
import com.facebook.SessionState;
import com.wcities.eventseeker.ArtistDetailsFragmentTab;
import com.wcities.eventseeker.BaseActivityTab;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.ShareViaDialogFragment;
import com.wcities.eventseeker.WebViewActivityTab;
import com.wcities.eventseeker.adapter.RVVenueDetailsAdapterTab.ViewHolder;
import com.wcities.eventseeker.analytics.GoogleAnalyticsTracker;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingItemType;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.asynctask.LoadArtistEvents;
import com.wcities.eventseeker.asynctask.UserTracker;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Date;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.core.Schedule;
import com.wcities.eventseeker.core.Event.Attending;
import com.wcities.eventseeker.interfaces.DateWiseEventParentAdapterListener;
import com.wcities.eventseeker.interfaces.EventListenerTab;
import com.wcities.eventseeker.interfaces.LoadItemsInBackgroundListener;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.FbUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class RVArtistDetailsAdapterTab extends Adapter<RVArtistDetailsAdapterTab.ViewHolder> implements 
		DateWiseEventParentAdapterListener {
	
	private static final String TAG = RVArtistDetailsAdapterTab.class.getSimpleName();
	
	private static final int EXTRA_TOP_DUMMY_ITEM_COUNT = 2;
	private static final int MAX_LINES_ARTIST_DESC = 3;
	private static final int EXTRA_TOP_DUMMY_ITEM_COUNT_AFTER_DETAILS_LOADED = 5;
	
	private RecyclerView recyclerView;
	private WeakReference<RecyclerView> weakRecyclerView;
	
	private ArtistDetailsFragmentTab artistDetailsFragmentTab;
	private Artist artist;
	private boolean isArtistDescExpanded;
	
	private VideoPagerAdapter videoPagerAdapter;
	private FriendsRVAdapter friendsRVAdapter;
	
	private List<Event> eventList;
	private LoadArtistEvents loadArtistEvents;
	private boolean isMoreDataAvailable = true;
	private int eventsAlreadyRequested;
	
	private BitmapCache bitmapCache;
	
	private int fbCallCountForSameEvt = 0;
	private RVArtistDetailsAdapterTab.ViewHolder holderPendingPublish;
	private Event eventPendingPublish;
	
	private AdapterDataObserver adapterDataObserver;
	
	private static enum ViewType {
		IMG, DESC, VIDEOS, FRIENDS, UPCOMING_EVENTS_TITLE, PROGRESS, EVENT;
		
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
		
		private ViewPager vPagerVideos;
		
		private RecyclerView recyclerVFriends;
		
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
			
			vPagerVideos = (ViewPager) itemView.findViewById(R.id.vPagerVideos);
			
			recyclerVFriends = (RecyclerView) itemView.findViewById(R.id.recyclerVFriends);
			
			imgEvt = (ImageView) itemView.findViewById(R.id.imgEvt);
			txtEvtTitle = (TextView) itemView.findViewById(R.id.txtEvtTitle);
            txtEvtTime = (TextView) itemView.findViewById(R.id.txtEvtTime);
            txtEvtLoc = (TextView) itemView.findViewById(R.id.txtEvtLoc);
            imgTicket = (ImageView) itemView.findViewById(R.id.imgTicket);
            imgSave = (ImageView) itemView.findViewById(R.id.imgSave);
            imgShare = (ImageView) itemView.findViewById(R.id.imgShare);
		}
	}
	
	public RVArtistDetailsAdapterTab(ArtistDetailsFragmentTab artistDetailsFragmentTab) {
		this.artistDetailsFragmentTab = artistDetailsFragmentTab;
		artist = artistDetailsFragmentTab.getArtist();
		eventList = artistDetailsFragmentTab.getEventList();
		bitmapCache = BitmapCache.getInstance();
	}
	
	/**
	 * Need to unregister manually because otherwise using same adapter on orientation change results in
	 * multiple time registrations w/o unregistration, due to which we need to manually 
	 * call unregisterAdapterDataObserver if it tries to register with new observer when already some older
	 * observer is registered. W/o having this results in multiple observers holding cardview & imgEvt memory.
	 */
	@Override
	public void registerAdapterDataObserver(AdapterDataObserver observer) {
		if (adapterDataObserver != null) {
			unregisterAdapterDataObserver(adapterDataObserver);
		}
        super.registerAdapterDataObserver(observer);
        adapterDataObserver = observer;
    }

	@Override
	public int getItemViewType(int position) {
		if (position == ViewType.IMG.ordinal()) {
			return ViewType.IMG.ordinal();
			
		} else if (position == ViewType.DESC.ordinal()) {
			return ViewType.DESC.ordinal();
			
		} else if (position == ViewType.VIDEOS.ordinal()) {
			return ViewType.VIDEOS.ordinal();
			
		} else if (position == ViewType.FRIENDS.ordinal()) {
			return ViewType.FRIENDS.ordinal();
			
		} else if (position == ViewType.UPCOMING_EVENTS_TITLE.ordinal()) {
			return ViewType.UPCOMING_EVENTS_TITLE.ordinal();
			
		} else if (eventList.get(position - EXTRA_TOP_DUMMY_ITEM_COUNT_AFTER_DETAILS_LOADED) == null) {
			return ViewType.PROGRESS.ordinal();
			
		} else {
			return ViewType.EVENT.ordinal();
		}
	}

	@Override
	public int getItemCount() {
		return artistDetailsFragmentTab.isAllDetailsLoaded() ? (EXTRA_TOP_DUMMY_ITEM_COUNT_AFTER_DETAILS_LOADED + 
				eventList.size()) : EXTRA_TOP_DUMMY_ITEM_COUNT;
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
			v = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_item_img_artist_artist_details, 
					parent, false);
			break;
			
		case DESC:
			v = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_item_desc_tab, parent, false);
			break;
			
		case VIDEOS:
			v = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_item_videos_artist_details, 
					parent, false);
			break;
			
		case FRIENDS:
			v = LayoutInflater.from(parent.getContext()).inflate(R.layout.include_friends, parent, false);
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
	public void onBindViewHolder(final ViewHolder holder, int position) {
		if (position == ViewType.IMG.ordinal()) {
			// nothing to do
			
		} else if (position == ViewType.DESC.ordinal()) {
			updateDescVisibility(holder);
			
		} else if (position == ViewType.VIDEOS.ordinal()) {
			//Log.d(TAG, "onBindViewHolder(), pos = " + position);
			if (videoPagerAdapter == null) {
				videoPagerAdapter = new VideoPagerAdapter(artistDetailsFragmentTab.childFragmentManager(), 
						artist.getVideos(), artist.getId());
				
			} else {
				if (videoPagerAdapter.areFragmentsDetached()) {
					videoPagerAdapter.attachFragments();
				}
			}
			
			holder.vPagerVideos.setAdapter(videoPagerAdapter);
			holder.vPagerVideos.setOnPageChangeListener(videoPagerAdapter);
			
			// Necessary or the pager will only have one extra page to show make this at least however many pages you can see
			holder.vPagerVideos.setOffscreenPageLimit(7);
			
			// Set margin for pages as a negative number, so a part of next and previous pages will be showed
			Resources res = FragmentUtil.getResources(artistDetailsFragmentTab);
			holder.vPagerVideos.setPageMargin(res.getDimensionPixelSize(R.dimen.rlt_lyt_root_w_video) - res
					.getDimensionPixelSize(R.dimen.floating_window_w) + res.getDimensionPixelSize(
					R.dimen.v_pager_videos_margin_l_rv_item_videos_artist_details) + res.getDimensionPixelSize(
							R.dimen.v_pager_videos_margin_r_rv_item_videos_artist_details));
			
			// Set current item to the middle page so we can fling to both directions left and right
			holder.vPagerVideos.setCurrentItem(videoPagerAdapter.getCurrentPosition());
			
			updateVideosVisibility(holder);
			
		} else if (position == ViewType.FRIENDS.ordinal()) {
			if (friendsRVAdapter == null) {
				friendsRVAdapter = new FriendsRVAdapter(artist.getFriends());
			} 
			
			// use a linear layout manager
			RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(FragmentUtil.getActivity(artistDetailsFragmentTab), 
					LinearLayoutManager.HORIZONTAL, false);
			holder.recyclerVFriends.setLayoutManager(layoutManager);
			
			holder.recyclerVFriends.setAdapter(friendsRVAdapter);
			
			updateFriendsVisibility(holder);
			
		} else if (position == ViewType.UPCOMING_EVENTS_TITLE.ordinal()) {
			if (eventList.isEmpty()) {
				setViewGone(holder);
			}
			
		} else {
			final Event event = eventList.get(position - EXTRA_TOP_DUMMY_ITEM_COUNT_AFTER_DETAILS_LOADED);
			if (event == null) {
				// progress indicator
				
				if ((loadArtistEvents == null || loadArtistEvents.getStatus() == Status.FINISHED) && 
						isMoreDataAvailable) {
					//Log.d(TAG, "onBindViewHolder(), pos = " + position);
					((LoadItemsInBackgroundListener) artistDetailsFragmentTab).loadItemsInBackground();
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
				ViewCompat.setTransitionName(holder.txtEvtTitle, "txtEvtTitleArtistDetails" + position);
				
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
				        asyncLoadImg.loadImg(holder.imgEvt, ImgResolution.LOW, weakRecyclerView, position, bitmapCacheable);
				    }
				}
				ViewCompat.setTransitionName(holder.imgEvt, "imgEvtArtistDetails" + position);
				
				holder.itemView.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						((EventListenerTab) FragmentUtil.getActivity(artistDetailsFragmentTab)).onEventSelected(event, 
								holder.imgEvt, holder.txtEvtTitle);
					}
				});
				
				Resources res = FragmentUtil.getResources(artistDetailsFragmentTab);
				if (event.getSchedule() == null || event.getSchedule().getBookingInfos().isEmpty()) {
					holder.imgTicket.setImageDrawable(res.getDrawable(R.drawable.ic_tickets_unavailable));
					holder.imgTicket.setEnabled(false);
					
				} else {
					holder.imgTicket.setImageDrawable(res.getDrawable(R.drawable.ic_tickets_available));
					holder.imgTicket.setEnabled(true);
				}
				
				updateImgSaveSrc(holder, event, res);
				
				holder.imgTicket.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						EventSeekr eventSeekr = FragmentUtil.getApplication(artistDetailsFragmentTab);
								
						Intent intent = new Intent(eventSeekr, WebViewActivityTab.class);
						intent.putExtra(BundleKeys.URL, event.getSchedule().getBookingInfos().get(0).getBookingUrl());
						artistDetailsFragmentTab.startActivity(intent);
						
						GoogleAnalyticsTracker.getInstance().sendEvent(eventSeekr, ((BaseActivityTab) 
								FragmentUtil.getActivity(artistDetailsFragmentTab)).getScreenName(), 
								GoogleAnalyticsTracker.EVENT_LABEL_TICKETS_BUTTON, 
								GoogleAnalyticsTracker.Type.Event.name(), null, event.getId());
					}
				});
				
				holder.imgSave.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						EventSeekr eventSeekr = (EventSeekr) FragmentUtil.getActivity(artistDetailsFragmentTab).getApplication();
						if (event.getAttending() == Attending.SAVED) {
							event.setAttending(Attending.NOT_GOING);
							new UserTracker(Api.OAUTH_TOKEN, eventSeekr, UserTrackingItemType.event, event.getId(), 
									event.getAttending().getValue(), UserTrackingType.Add).execute();
			    			updateImgSaveSrc(holder, event, FragmentUtil.getResources(artistDetailsFragmentTab));
							
						} else {
							artistDetailsFragmentTab.setEvent(event);
							eventPendingPublish = event;
							holderPendingPublish = holder;
							
							if (eventSeekr.getGPlusUserId() != null) {
								event.setNewAttending(Attending.SAVED);
								artistDetailsFragmentTab.handlePublishEvent();
								
							} else {
								fbCallCountForSameEvt = 0;
								event.setNewAttending(Attending.SAVED);
								//NOTE: THIS CAN BE TESTED WITH PODUCTION BUILD ONLY
								FbUtil.handlePublishEvent(artistDetailsFragmentTab, artistDetailsFragmentTab, AppConstants.PERMISSIONS_FB_PUBLISH_EVT_OR_ART, 
										AppConstants.REQ_CODE_FB_PUBLISH_EVT_OR_ART, event);
							}
						}
					}
				});
				
				holder.imgShare.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						ShareViaDialogFragment shareViaDialogFragment = ShareViaDialogFragment.newInstance(event, 
								ScreenNames.ARTIST_DETAILS);
						/**
						 * Passing activity fragment manager, since using this fragment's child fragment manager 
						 * doesn't retain dialog on orientation change
						 */
						shareViaDialogFragment.show(((BaseActivityTab)FragmentUtil.getActivity(artistDetailsFragmentTab))
								.getSupportFragmentManager(), FragmentUtil.getTag(ShareViaDialogFragment.class));
					}
				});
			}
		}
	}
	
	private void updateImgSaveSrc(ViewHolder holder, Event event, Resources res) {
		int drawableId = (event.getAttending() == Attending.SAVED) ? R.drawable.ic_saved_event
				: R.drawable.ic_unsaved_event;
		holder.imgSave.setImageDrawable(res.getDrawable(drawableId));
	}
	
	private void updateDescVisibility(ViewHolder holder) {
		if (artistDetailsFragmentTab.isAllDetailsLoaded()) {
			artistDetailsFragmentTab.setVNoContentBgVisibility(View.INVISIBLE);
			if (artist.getDescription() != null) {
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
			artistDetailsFragmentTab.setVNoContentBgVisibility(View.VISIBLE);
			holder.rltRootDesc.setBackgroundColor(Color.TRANSPARENT);
			holder.rltLytPrgsBar.setVisibility(View.VISIBLE);
			holder.txtDesc.setVisibility(View.GONE);
			holder.imgDown.setVisibility(View.GONE);
			holder.vHorLine.setVisibility(View.GONE);
		}
	}
	
	private void makeDescVisible(final ViewHolder holder) {
		holder.txtDesc.setText(Html.fromHtml(artist.getDescription()));
		holder.imgDown.setVisibility(View.VISIBLE);
		holder.imgDown.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//Log.d(TAG, "totalScrolled  = " + holder.itemView.getTop());
				if (isArtistDescExpanded) {
					collapseArtistDesc(holder);
					
					/**
					 * update scrolled distance after collapse, because sometimes it can happen that view becamse scrollable only
					 * due to expanded description after which if user collapses it, then based on recyclerview
					 * height it automatically resettles itself such that recyclerview again becomes unscrollable.
					 * Accordingly we need to reset scrolled amount, artist img & title
					 */
					artistDetailsFragmentTab.getHandler().post(new Runnable() {
						
						@Override
						public void run() {
							artistDetailsFragmentTab.onScrolled(0, true);
						}
					});
					
				} else {
					expandArtistDesc(holder);
				}
				//Log.d(TAG, "totalScrolled after  = " + holder.itemView.getTop());
			}
		});
		
		if (isArtistDescExpanded) {
			expandArtistDesc(holder);
			
		} else {
			collapseArtistDesc(holder);
		}
	}
	
	private void collapseArtistDesc(ViewHolder holder) {
		holder.txtDesc.setMaxLines(MAX_LINES_ARTIST_DESC);
		holder.txtDesc.setEllipsize(TruncateAt.END);
		holder.imgDown.setImageDrawable(FragmentUtil.getResources(artistDetailsFragmentTab).getDrawable(
				R.drawable.ic_description_expand));
		isArtistDescExpanded = false;
	}
	
	private void expandArtistDesc(ViewHolder holder) {
		holder.txtDesc.setMaxLines(Integer.MAX_VALUE);
		holder.txtDesc.setEllipsize(null);
		holder.imgDown.setImageDrawable(FragmentUtil.getResources(artistDetailsFragmentTab).getDrawable(
				R.drawable.ic_description_collapse));
		isArtistDescExpanded = true;
	}
	
	private void setViewGone(ViewHolder holder) {
		RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) holder.itemView.getLayoutParams();
		lp.height = 0;
		holder.itemView.setLayoutParams(lp);
	}
	
	private void updateVideosVisibility(ViewHolder holder) {
		if (!artist.getVideos().isEmpty()) {
			videoPagerAdapter.notifyDataSetChanged();
			
		} else {
			setViewGone(holder);
		}
	}
	
	private void updateFriendsVisibility(ViewHolder holder) {
		if (!artist.getFriends().isEmpty()) {
			friendsRVAdapter.notifyDataSetChanged();
			
		} else {
			setViewGone(holder);
		}
	}
	
	public void detachFragments() {
		videoPagerAdapter.detachFragments();
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
		this.loadArtistEvents = (LoadArtistEvents) loadDateWiseEvents;
	}
	
	public void call(Session session, SessionState state, Exception exception) {
		//Log.i(TAG, "call()");
		fbCallCountForSameEvt++;
		/**
		 * To prevent infinite loop when network is off & we are calling requestPublishPermissions() of FbUtil.
		 */
		if (fbCallCountForSameEvt < AppConstants.MAX_FB_CALL_COUNT_FOR_SAME_EVT_OR_ART) {
			FbUtil.call(session, state, exception, artistDetailsFragmentTab, artistDetailsFragmentTab, AppConstants.PERMISSIONS_FB_PUBLISH_EVT_OR_ART, 
					AppConstants.REQ_CODE_FB_PUBLISH_EVT_OR_ART, eventPendingPublish);
			
		} else {
			fbCallCountForSameEvt = 0;
			artistDetailsFragmentTab.setPendingAnnounce(false);
		}
	}

	public void onPublishPermissionGranted() {
		//Log.d(TAG, "onPublishPermissionGranted()");
		updateImgSaveSrc(holderPendingPublish, eventPendingPublish, FragmentUtil.getResources(artistDetailsFragmentTab));
	}
}
