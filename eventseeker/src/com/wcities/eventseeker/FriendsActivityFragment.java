package com.wcities.eventseeker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.R.color;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView.RecyclerListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.Session.StatusCallback;
import com.facebook.SessionState;
import com.wcities.eventseeker.DrawerListFragment.DrawerListFragmentListener;
import com.wcities.eventseeker.SettingsFragment.OnSettingsItemClickedListener;
import com.wcities.eventseeker.SettingsFragment.SettingsItem;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.api.UserInfoApi.Tracktype;
import com.wcities.eventseeker.api.UserInfoApi.Type;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingItemType;
import com.wcities.eventseeker.api.UserInfoApi.UserTrackingType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.asynctask.UserTracker;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Date;
import com.wcities.eventseeker.core.Event.Attending;
import com.wcities.eventseeker.core.FriendNewsItem;
import com.wcities.eventseeker.custom.fragment.PublishEventListFragment;
import com.wcities.eventseeker.custom.fragment.PublishEventListFragmentLoadableFromBackStack;
import com.wcities.eventseeker.custom.view.CircleImageView;
import com.wcities.eventseeker.custom.view.ResizableImageView;
import com.wcities.eventseeker.interfaces.CustomSharedElementTransitionSource;
import com.wcities.eventseeker.interfaces.EventListener;
import com.wcities.eventseeker.interfaces.PublishListener;
import com.wcities.eventseeker.interfaces.ReplaceFragmentListener;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.FbUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.VersionUtil;
import com.wcities.eventseeker.util.ViewUtil;
import com.wcities.eventseeker.viewdata.SharedElement;
import com.wcities.eventseeker.viewdata.SharedElementPosition;

public class FriendsActivityFragment extends PublishEventListFragmentLoadableFromBackStack implements 
		StatusCallback, OnClickListener, PublishListener, CustomSharedElementTransitionSource {
	
	private static final String TAG = FriendsActivityFragment.class.getName();
	
	private static enum PublishRequest {
		LIKE,
		COMMENT;
	}
	
	// List of additional write permissions being requested
	private static final List<String> PERMISSIONS = Arrays.asList("publish_actions", "publish_stream");
	// Request code for facebook reauthorization requests. 
	private static final int FACEBOOK_REAUTH_ACTIVITY_CODE = 100; 
	// Flag to represent if we are waiting for extended permissions
	private boolean pendingLikeOrComment = false;
	// Strings required to build like request graph path 
	private String fbPostId;
	// indicates which type of request is pending to be executed
	private PublishRequest publishRequest;
	
	private LoadFriendsNews loadFriendsNews;
	private FriendActivityListAdapter friendActivityListAdapter;

	//private int orientation;
	private String wcitiesId;
	private List<FriendNewsItem> friendNewsItems;

	private int itemsAlreadyRequested;
	private boolean isMoreDataAvailable = true;
	private int firstVisibleActivityItemPosition;
	
	/*private boolean isTablet;
	private boolean is7InchTabletInPortrait;*/
	private View rltRootNoContentFound;	
	private RelativeLayout rltLytPrgsBar;

	/**
	 * Using its instance variable since otherwise calling getResources() directly from fragment from 
	 * callback methods is dangerous in a sense that it may throw java.lang.IllegalStateException: 
	 * Fragment not attached to Activity, if user has already left this fragment & 
	 * then changed the orientation.
	 */
	private Resources res;
	private List<View> hiddenViews;
	private boolean isOnPushedToBackStackCalled;
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (!(activity instanceof DrawerListFragmentListener)) {
            throw new ClassCastException(activity.toString() + " must implement DrawerListFragmentListener");
        }
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		
		if (wcitiesId == null) {
			wcitiesId = ((EventSeekr)FragmentUtil.getActivity(this).getApplication()).getWcitiesId();
		}
		
		res = FragmentUtil.getResources(this);
		hiddenViews = new ArrayList<View>();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View v = inflater.inflate(R.layout.fragment_friends_activity_list, null);
		
		/**
		 * add extra top margin (equal to statusbar height) since we are removing vStatusBar from onStart() 
		 * even though we want search screen to have this statusbar. We had to mark VStatusBar as GONE from 
		 * onStart() so that on transition from any search child fragment (SearchArtists/SearchEvents/SearchVenues)
		 * to corresponding details screen doesn't cause jumping effect on search screen, as we remove vStatusBar 
		 * on detail screen when this search screen is visible in the background
		 */
		if (VersionUtil.isApiLevelAbove18()) {
			Resources res = FragmentUtil.getResources(this);
			RelativeLayout rltLayoutRoot = (RelativeLayout) v.findViewById(R.id.rltLayoutRoot);
			rltLayoutRoot.setPadding(0, res.getDimensionPixelSize(R.dimen.common_t_mar_pad_for_all_layout) 
					+ ViewUtil.getStatusBarHeight(res), 0, 0);
		}
		
		rltRootNoContentFound = v.findViewById(R.id.rltRootNoContentFound);
		rltLytPrgsBar = (RelativeLayout) v.findViewById(R.id.rltLytPrgsBar);
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if (friendNewsItems == null) {
			friendNewsItems = new ArrayList<FriendNewsItem>();
			friendActivityListAdapter = new FriendActivityListAdapter(FragmentUtil.getActivity(this), this);
	        
			friendNewsItems.add(null);
			loadFriendsNewsInBackground();
			
		} else {
			if (friendNewsItems.isEmpty()) {
				showNoFriendsActivityFound();
			}
			friendActivityListAdapter.setmInflater(FragmentUtil.getActivity(this));
		}

		setListAdapter(friendActivityListAdapter);
		getListView().setDivider(null);
		getListView().setScrollingCacheEnabled(false);
		
		final int pos = firstVisibleActivityItemPosition;
		
		getListView().post(new Runnable() {
			
			@Override
			public void run() {
				// TODO: remove following try-catch handling if not required
				try {
					setSelection(pos);
					
				} catch (IllegalStateException e) {
					Log.e(TAG, "" + e.getMessage());
					e.printStackTrace();
				}
			}
		});
		
		getListView().setRecyclerListener(new RecyclerListener() {
			
			@Override
			public void onMovedToScrapHeap(View view) {
				freeUpBitmapMemory(view);
			}
		});
	}
	
	@Override
	public void onStart() {
		if (!isOnTop()) {
			callOnlySuperOnStart = true;
			super.onStart();
			return;
		}
		
		super.onStart();
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		ma.setToolbarElevation(0);
		/**
		 * Even though we want status bar in this case, mark it gone to have smoother transition to detail fragment
		 * & prevent jumping effect on search screen, caused due to removal of status bar on detail screen when this 
		 * search screen is visible in background.
		 */
		ma.setVStatusBarVisibility(View.GONE, AppConstants.INVALID_ID);
		ma.setVStatusBarLayeredVisibility(View.VISIBLE, R.color.colorPrimaryDark);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		//Log.d(TAG, "onStop()");
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		ma.setToolbarElevation(ma.getResources().getDimensionPixelSize(R.dimen.action_bar_elevation));
		ma.setVStatusBarVisibility(View.VISIBLE, R.color.colorPrimaryDark);
		ma.setVStatusBarLayeredVisibility(View.GONE, AppConstants.INVALID_ID);
	}
	
	@Override
	public void onDestroyView() {
		//Log.d(TAG, "onDestroyView()");
		
		firstVisibleActivityItemPosition = getListView().getFirstVisiblePosition();
		
		for (int i = getListView().getFirstVisiblePosition(), j = 0; 
				i <= getListView().getLastVisiblePosition(); i++, j++) {
			freeUpBitmapMemory(getListView().getChildAt(j));
		}
		
		Session session = Session.getActiveSession();
		if (session != null) {
			//Log.d(TAG, "removeCallback");
			session.removeCallback(this);
		}
		super.onDestroyView();
	}
	
	private void freeUpBitmapMemory(View view) {
		if (view.getTag() instanceof FriendActivityListAdapter.FriendNewsItemViewHolder) {
			FriendActivityListAdapter.FriendNewsItemViewHolder holder = 
					(FriendActivityListAdapter.FriendNewsItemViewHolder) view.getTag();

			ResizableImageView imgPhoto = holder.imgEvt;
			imgPhoto.setImageBitmap(null);
		}
	}
	
	private void loadFriendsNewsInBackground() {
		loadFriendsNews = new LoadFriendsNews();
		AsyncTaskUtil.executeAsyncTask(loadFriendsNews, true);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		//Log.d(TAG, "onActivityResult(), requestCode = " + requestCode);
		if (isPendingAnnounce()) {
			super.onActivityResult(requestCode, resultCode, data);
			
		} else {
			// for like/comment functionality
			Session session = Session.getActiveSession();
	        if (session != null) {
	        	//Log.d(TAG, "session!=null");
	            session.onActivityResult(FragmentUtil.getActivity(this), requestCode, resultCode, data);
	        }
		}
	}
	
	private class LoadFriendsNews extends AsyncTask<Void, Void, List<FriendNewsItem>> {
		
		private static final int FRIENDS_NEWS_LIMIT = 10;
		
		@Override
		protected List<FriendNewsItem> doInBackground(Void... params) {
			List<FriendNewsItem> tmpFriendNewsItems = new ArrayList<FriendNewsItem>();
			UserInfoApi userInfoApi = new UserInfoApi(Api.OAUTH_TOKEN);
			userInfoApi.setLimit(FRIENDS_NEWS_LIMIT);
			userInfoApi.setAlreadyRequested(itemsAlreadyRequested);
			userInfoApi.setUserId(wcitiesId);
			userInfoApi.setTracktype(Tracktype.event);

			try {
				JSONObject jsonObject = userInfoApi.getMyProfileInfoFor(Type.friendsfeed);
				UserInfoApiJSONParser jsonParser = new UserInfoApiJSONParser();
				
				tmpFriendNewsItems = jsonParser.getFriendNews(jsonObject);
				
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				
			} catch (IOException e) {
				e.printStackTrace();
				
			} catch (JSONException e) {
				e.printStackTrace();
			}

			return tmpFriendNewsItems;
		}
		
		@Override
		protected void onPostExecute(List<FriendNewsItem> tmpFriendNewsItems) {
			int totalNewItems = tmpFriendNewsItems.size();
			
			for (Iterator<FriendNewsItem> iterator = tmpFriendNewsItems.iterator(); iterator.hasNext();) {
				FriendNewsItem friendNewsItem = iterator.next();
				/**
				 * Venue startTime is null by default for events which were tracked in past but afterwards deleted 
				 * from server due to some reason. We need not show such feeds.
				 */
				if (friendNewsItem.getAttending() == Attending.NOT_GOING || friendNewsItem.getStartTime() == null) {
					iterator.remove();
				}
			}
			
			if (tmpFriendNewsItems.size() > 0) {
				friendNewsItems.addAll(friendNewsItems.size() - 1, tmpFriendNewsItems);
				itemsAlreadyRequested += totalNewItems;
				
				if (totalNewItems < FRIENDS_NEWS_LIMIT) {
					isMoreDataAvailable = false;
					friendNewsItems.remove(friendNewsItems.size() - 1);
				}
				
			} else {
				isMoreDataAvailable = false;
				friendNewsItems.remove(friendNewsItems.size() - 1);
				if (friendNewsItems.isEmpty()) {
					showNoFriendsActivityFound();
				}
			}
			
			friendActivityListAdapter.notifyDataSetChanged();
			// to remove full screen progressbar
			rltLytPrgsBar.setVisibility(View.INVISIBLE);
		}
    }
	
	private class FriendActivityListAdapter extends BaseAdapter {
		
		private static final int MAX_FB_CALL_COUNT_FOR_SAME_EVT = 20;
		
	    private LayoutInflater mInflater;
	    private EventSeekr eventSeekr;
	    
	    private FriendNewsItem newsItemPendingPublish;
		private CheckBox newsItemPendingPublishChkBoxSave;
		private int fbCallCountForSameEvt = 0;
		private Context mContext;
		
		private CustomSharedElementTransitionSource customSharedElementTransitionSource;

	    public FriendActivityListAdapter(Context context, 
	    		CustomSharedElementTransitionSource customSharedElementTransitionSource) {
	    	mContext = context;
	        mInflater = LayoutInflater.from(context);
	        eventSeekr = (EventSeekr)context.getApplicationContext();
	        
			this.customSharedElementTransitionSource = customSharedElementTransitionSource;
	    }
	    
	    public void setmInflater(Context context) {
	    	mContext = context;
	        mInflater = LayoutInflater.from(context);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			FriendNewsItem item = getItem(position);
			if (item == null) {
				if (convertView == null || convertView.getTag() instanceof FriendNewsItemViewHolder) {
					convertView = mInflater.inflate(R.layout.progress_bar_eventseeker_fixed_ht, null);
					convertView.setTag(AppConstants.TAG_PROGRESS_INDICATOR);
					convertView.setBackgroundColor(getResources().getColor(R.color.root_lnr_layout_bg_friends_activity_list_item));
				}
				
				if (friendNewsItems.size() == 1) {
					rltLytPrgsBar.setVisibility(View.VISIBLE);
					convertView.setVisibility(View.INVISIBLE);
					
				} else {
					convertView.setVisibility(View.VISIBLE);
				}
				
				if ((loadFriendsNews == null || loadFriendsNews.getStatus() == Status.FINISHED) && 
						isMoreDataAvailable) {
					loadFriendsNewsInBackground();
				}
				
			} else {
				FriendNewsItemViewHolder holder;
				if (convertView == null || !(convertView.getTag() instanceof FriendNewsItemViewHolder)) {
					convertView = mInflater.inflate(R.layout.item_list_friends_activity, null);
					holder = new FriendNewsItemViewHolder();
					
					RelativeLayout rltLayoutNewsItemContainer = holder.rltLayoutNewsItemContainer 
							= (RelativeLayout) convertView.findViewById(R.id.rltLayoutNewsItemContainer);
					holder.imgCrclFriend = (CircleImageView) rltLayoutNewsItemContainer.findViewById(R.id.imgCrclFriend);
					holder.imgEvt = (ResizableImageView) rltLayoutNewsItemContainer.findViewById(R.id.imgEvt);
					holder.txtTitle = (TextView) rltLayoutNewsItemContainer.findViewById(R.id.txtTitle);
					holder.txtVenue = (TextView) rltLayoutNewsItemContainer.findViewById(R.id.txtVenue);
					holder.lnrSave = (LinearLayout) rltLayoutNewsItemContainer.findViewById(R.id.lnrSave);
					holder.lnrLike = (LinearLayout) rltLayoutNewsItemContainer.findViewById(R.id.lnrLike);
					holder.lnrComment = (LinearLayout) rltLayoutNewsItemContainer.findViewById(R.id.lnrComment);
					holder.lnrTickets = (LinearLayout) rltLayoutNewsItemContainer.findViewById(R.id.lnrTickets);
					holder.chkSave = (CheckBox) rltLayoutNewsItemContainer.findViewById(R.id.chkSave);
					holder.chkBtnBuy = (CheckBox) rltLayoutNewsItemContainer.findViewById(R.id.chkBuyTickets);
					
					convertView.setTag(holder);
					
				} else {
					holder = (FriendNewsItemViewHolder) convertView.getTag();
				}
				
				holder.setContent(item, parent, position);
			}
			
			return convertView;
		}

		@Override
		public FriendNewsItem getItem(int position) {
			return friendNewsItems.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public int getCount() {
			return friendNewsItems.size();
		}
		
		private class FriendNewsItemViewHolder {
			
			private RelativeLayout rltLayoutNewsItemContainer;
			private CircleImageView imgCrclFriend;
			private ResizableImageView imgEvt;
			private TextView txtTitle, txtVenue;
			private LinearLayout lnrSave, lnrLike, lnrComment, lnrTickets;
			private CheckBox chkSave, chkBtnBuy;
			
			private void setContent(Object listItem, ViewGroup parent, int pos) {
				setNewsItemContent((FriendNewsItem) listItem, parent, pos);
			}
			
			private void setNewsItemContent(final FriendNewsItem item, ViewGroup parent, int pos) {
				txtTitle.setText(res.getString(R.string.saved, item.getFriend().getName(), item.getTrackName()));
				
				String key = item.getFriend().getKey(ImgResolution.LOW);
				BitmapCache bitmapCache = BitmapCache.getInstance();
				Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
				if (bitmap != null) {
					imgCrclFriend.setImageBitmap(bitmap);
					
				} else {
					imgCrclFriend.setImageResource(R.drawable.placeholder);
					AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
					asyncLoadImg.loadFBUserImg(imgCrclFriend, ImgResolution.LOW, (AdapterView) parent, pos, item.getFriend());
				}

				key = item.getKey(ImgResolution.LOW);
		        bitmapCache = BitmapCache.getInstance();
				bitmap = bitmapCache.getBitmapFromMemCache(key);
				if (bitmap != null) {
					imgEvt.setImageBitmap(bitmap);
			        
			    } else {
					imgEvt.setImageResource(R.drawable.placeholder);
			        AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
			        asyncLoadImg.loadImg(imgEvt, ImgResolution.LOW, (AdapterView) parent, pos, item);
			    }
								
				Date date = item.getStartTime();
				String strDate = "";
				if (date != null) {
					strDate = ConversionUtil.getDayForFriendsActivity(date.getStartDate()) + " @ ";
				}
				txtVenue.setText(strDate + item.getVenueName().toUpperCase());
				
				if (item.getFbPostId() != null) {
					lnrLike.setVisibility(View.VISIBLE);
					lnrComment.setVisibility(View.VISIBLE);
					lnrTickets.setVisibility(View.GONE);
					rltLayoutNewsItemContainer.setPadding(0, 0, 0, 0);
					
					lnrLike.setOnClickListener(new View.OnClickListener() {
						
						@Override
						public void onClick(View v) {
							//Log.d(TAG, "like onClick()");
							fbPostId = item.getFbPostId();
							publishRequest = PublishRequest.LIKE;
							handlePublish();
						}
					});
					
					lnrComment.setOnClickListener(new View.OnClickListener() {
						
						@Override
						public void onClick(View v) {
							fbPostId = item.getFbPostId();
							publishRequest = PublishRequest.COMMENT;
							handlePublish();
						}
					});
					
				} else {
					lnrLike.setVisibility(View.GONE);
					lnrComment.setVisibility(View.GONE);
					lnrTickets.setVisibility(View.VISIBLE);

					setBuyTickets(item);
				}
				setSaveLyt(item);
				rltLayoutNewsItemContainer.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						/*((EventListener)FragmentUtil.getActivity(FriendsActivityFragment.this))
							.onEventSelected(item.toEvent());*/
						List<SharedElement> sharedElements = new ArrayList<SharedElement>();

						int[] loc = ViewUtil.getLocationOnScreen(imgEvt, mContext.getResources());
						SharedElementPosition sharedElementPosition = new SharedElementPosition(loc[0], 
								loc[1], imgEvt.getWidth(), imgEvt.getHeight());
						
						SharedElement sharedElement = new SharedElement(sharedElementPosition, imgEvt);
						sharedElements.add(sharedElement);
						customSharedElementTransitionSource.addViewsToBeHidden(imgEvt);
						
						((EventListener) mContext).onEventSelected(item.toEvent(), sharedElements);

						customSharedElementTransitionSource.onPushedToBackStack();
					}
				});
			}
			
			private void setBuyTickets(final FriendNewsItem item) {
				
				if (item.getBookingUrl() != null) {
					chkBtnBuy.setEnabled(true);
					chkBtnBuy.setTextColor(res.getColor(color.black));

				} else {
					chkBtnBuy.setEnabled(false);
					chkBtnBuy.setTextColor(res.getColor(R.color.btn_buy_tickets_disabled_txt_color));
				}
				
				lnrTickets.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View arg0) {
						if (item.getBookingUrl() != null) {
							Bundle args = new Bundle();
							args.putString(BundleKeys.URL, item.getBookingUrl());
							onBuyTicketClicked(args);
						}
					}
				});
			}
			
			private void setSaveLyt(final FriendNewsItem item) {
				updateAttendingChkSave(item, chkSave);
				lnrSave.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						onSaveClick(item);
					}
				});
				
				/**
				 * If user clicks on going or wantToGo & changes orientation instantly before call to 
				 * onPublishPermissionGranted(), then we need to update both CheckBoxes with right 
				 * CheckBox pointers in new orientation
				 */
				if (newsItemPendingPublish == item) {
					newsItemPendingPublishChkBoxSave = chkSave;
				}
			}
			
			private void onSaveClick(FriendNewsItem item) {
				Attending userAttending = item.getUserAttending() == Attending.SAVED ? 
						Attending.NOT_GOING : Attending.SAVED;
				if (userAttending == Attending.NOT_GOING) {
					item.setUserAttending(userAttending);
					updateAttendingChkSave(item, chkSave);
					new UserTracker(Api.OAUTH_TOKEN, eventSeekr, UserTrackingItemType.event, item.getTrackId(), 
							item.getUserAttending().getValue(), UserTrackingType.Add).execute();
					
				} else {
					/**
					 * call to updateAttendingChkBoxes() to negate the click event for now on CheckBox, 
					 * since it's handled after checking fb/google publish permission
					 */
					updateAttendingChkSave(item, chkSave);
					
					if (eventSeekr.getFbUserId() != null) {
						item.setNewUserAttending(userAttending);
						newsItemPendingPublish = item;
						newsItemPendingPublishChkBoxSave = chkSave;
						fbCallCountForSameEvt = 0;
						//NOTE: THIS CAN BE TESTED WITH PODUCTION BUILD ONLY
						FbUtil.handlePublishFriendNewsItem(FriendsActivityFragment.this, FriendsActivityFragment.this, 
								AppConstants.PERMISSIONS_FB_PUBLISH_EVT_OR_ART, AppConstants.REQ_CODE_FB_PUBLISH_EVT_OR_ART, item);
						
					} else if (eventSeekr.getGPlusUserId() != null) {
						item.setNewUserAttending(userAttending);
						newsItemPendingPublish = item;
						newsItemPendingPublishChkBoxSave = chkSave;
						((PublishEventListFragment)FriendsActivityFragment.this).setFriendNewsItem(newsItemPendingPublish);
						((PublishEventListFragment)FriendsActivityFragment.this).handlePublishEvent();
					}
				}
			}
		}
		
		public void call(Session session, SessionState state, Exception exception) {
			//Log.d(TAG, "call()");
			fbCallCountForSameEvt++;
			/**
			 * To prevent infinite loop when network is off & we are calling requestPublishPermissions() of FbUtil.
			 */
			if (fbCallCountForSameEvt < MAX_FB_CALL_COUNT_FOR_SAME_EVT) {
				FbUtil.call(session, state, exception, FriendsActivityFragment.this, FriendsActivityFragment.this, 
						AppConstants.PERMISSIONS_FB_PUBLISH_EVT_OR_ART, AppConstants.REQ_CODE_FB_PUBLISH_EVT_OR_ART, 
						newsItemPendingPublish);
				
			} else {
				fbCallCountForSameEvt = 0;
				FriendsActivityFragment.this.setPendingAnnounce(false);
			}
		}
		
		public void onPublishPermissionGranted() {
			updateAttendingChkSave(newsItemPendingPublish, newsItemPendingPublishChkBoxSave);
		}
		
		private void updateAttendingChkSave(FriendNewsItem item, CheckBox chkSave) {
			chkSave.setChecked(item.getUserAttending() == Attending.SAVED);			
		}
		
	}
	
	private void onBuyTicketClicked(Bundle args) {
		((ReplaceFragmentListener)FragmentUtil.getActivity(this)).replaceByFragment(
				AppConstants.FRAGMENT_TAG_WEB_VIEW, args);
	}
	
	private static class AddCommentDialogFragment extends DialogFragment {

	    /**
	     * Create a new instance of AddCommentDialogFragment
	     */
	    @SuppressLint("ValidFragment")
		static AddCommentDialogFragment newInstance() {
	        return new AddCommentDialogFragment();
	    }

	    @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {
	    	final EditText input = new EditText(FragmentUtil.getActivity(this));
	    	LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
	    	lp.setMargins(0, ConversionUtil.toPx(getResources(), 50), 0, 0);
	    	input.setLayoutParams(lp);
	    	// w/o following line on samsung galaxy S it's showing black text on black background
	    	input.setBackgroundColor(Color.WHITE);
	    	// w/o following line on nexus 5 it's showing white text on almost white background
	    	input.setTextColor(Color.BLACK);
	    	
	        return new AlertDialog.Builder(FragmentUtil.getActivity(this))
	                .setTitle(getResources().getString(R.string.add_comment))
	                .setView(input)
	                .setPositiveButton(getResources().getString(R.string.add),
	                    new DialogInterface.OnClickListener() {
	                        public void onClick(DialogInterface dialog, int whichButton) {
	                            ((FriendsActivityFragment)getParentFragment()).doPositiveClickToAddComment(
	                            		input.getText().toString());
	                        }
	                    }
	                )
	                .setNegativeButton(R.string.cancel, null)
	                .create();
	    }
	    
	    @Override
	    public void onDestroyView() {
	    	//Log.d(TAG, "dialog retain instance = " + getRetainInstance());
	    	/**
	    	 * We add this code to stop our dialog from being dismissed on rotation, 
	    	 * due to a bug with the compatibility library.
	    	 * If it's a top level fragment then we need to update this if condition as
	    	 * "if (getDialog() != null && getRetainInstance())"
	    	 */
	        if (getDialog() != null) {
	            getDialog().setDismissMessage(null);
	        }
	        super.onDestroyView();
	    }
	}
	
	private boolean canPublishNow() {
		pendingLikeOrComment = false;
	    Session session = Session.getActiveSession();

	    if (session == null) {
	    	//Log.d(TAG, "session=null");
	    	session = new Session(FragmentUtil.getActivity(this));
			Session.setActiveSession(session);
	    }
	    
	    //Log.d(TAG, "active session, state=" + session.getState().name());
	    if (!session.isOpened()) {
	    	//Log.d(TAG, "session is not opened");
	    	pendingLikeOrComment = true; // Mark that we are currently waiting for opening of session
    		Session.openActiveSession(FragmentUtil.getActivity(this), this, true, this);
    		return false;
	    }

	    if (!hasPublishPermission()) {
	    	//Log.d(TAG, "publish permission is not there");
	    	pendingLikeOrComment = true; // Mark that we are currently waiting for confirmation of publish permissions
	        session.addCallback(this); 
	        requestPublishPermissions(session, PERMISSIONS, FACEBOOK_REAUTH_ACTIVITY_CODE);
	        return false;
	    }
	    
	    return true;
	}
	
	private void handlePublish() {
		//Log.i(TAG, "handlePublish()");
		if (canPublishNow()) {
			if (publishRequest == PublishRequest.LIKE) {
				postLikeRequest();
				
			} else if (publishRequest == PublishRequest.COMMENT) {
				showAddCommentDialog();
			}
		}
	}
	
	private boolean hasPublishPermission() {
		//Log.d(TAG, "hasPublishPermission()");
        Session session = Session.getActiveSession();
        /*List<String> permissions = session.getPermissions();
        for (Iterator<String> iterator = permissions.iterator(); iterator.hasNext();) {
			Log.d(TAG, iterator.next());
		}*/
        return session != null && session.getPermissions().containsAll(PERMISSIONS);
    }
	
	private void requestPublishPermissions(Session session, List<String> permissions,
		    int requestCode) {
		//Log.d(TAG, "requestPublishPermissions()");
        Session.NewPermissionsRequest reauthRequest = new Session.NewPermissionsRequest(this, permissions)
        	.setRequestCode(requestCode);
        session.requestNewPublishPermissions(reauthRequest);
	}
	
	private void postLikeRequest() {
		//Log.d(TAG, "postLikeRequest()");
		Toast.makeText(FragmentUtil.getActivity(this), R.string.sending_like_request, Toast.LENGTH_SHORT).show();
		Request likeRequest = Request.newPostRequest(Session.getActiveSession(), fbPostId + "/likes", null, new Request.Callback() {

			         @Override
			         public void onCompleted(Response response) {
			                Log.i(TAG, response.toString());
			         }
				});
		likeRequest.executeAsync();
	}
	
	private void postCommentRequest(String comment) {
		//Log.d(TAG, "postCommentRequest()");
		Toast.makeText(FragmentUtil.getActivity(this), R.string.posting_comment, Toast.LENGTH_SHORT).show();
		Bundle parameters = new Bundle();
		parameters.putString("message", comment);
		Request commentRequest = new Request(Session.getActiveSession(), fbPostId + "/comments", 
				parameters, HttpMethod.POST, new Request.Callback() {

			         @Override
			         public void onCompleted(Response response) {
			                Log.i(TAG, response.toString());
			         }
				});
		commentRequest.executeAsync();
	}
	
	private void showAddCommentDialog() {
	    DialogFragment newFragment = AddCommentDialogFragment.newInstance();
	    newFragment.show(getChildFragmentManager(), "dialog");
	}

	private void doPositiveClickToAddComment(String comment) {
	    // Do stuff here.
	    postCommentRequest(comment);
	}

	/**
	 * Called when additional permission request is completed successfully.
	 */
	private void tokenUpdated() {
		//Log.d(TAG, "tokenUpdated()");
	    // Check if a publish action is in progress
	    // awaiting a successful reauthorization
	    if (pendingLikeOrComment) {
	        
	    	if (hasPublishPermission()) {
	    		// Publish the action
	    		handlePublish();
	    		
	    	} else {
	    		// user has denied the permission
	    		pendingLikeOrComment = false;
	    	}
	    }
	}
	
	private void sessionOpened() {
		//Log.d(TAG, "sessionOpened()");
		// Check if a publish action is in progress
	    // awaiting a successful reauthorization
	    if (pendingLikeOrComment) {
	        // Publish the action
	    	handlePublish();
	    }
	}
	
	protected void onSessionStateChange(final Session session, SessionState state, Exception exception) {
		//Log.d(TAG, "onSessionStateChange() state = " + state.name());
	    if (session != null && session.isOpened()) {
	    	//Log.d(TAG, "session != null && session.isOpened(), state = " + state.name());
	    	if (state.equals(SessionState.OPENED)) {
	    		Log.d(TAG, "OPENED");
	    		// Session opened 
	            // so try publishing once more.
	    		sessionOpened();
	    		
	    	} else if (state.equals(SessionState.OPENED_TOKEN_UPDATED)) {
	    		Log.d(TAG, "OPENED_TOKEN_UPDATED");
	            // Session updated with new permissions
	            // so try publishing once more.
	            tokenUpdated();
	        }
	    }
	}

	@Override
	public void call(Session session, SessionState state, Exception exception) {
		//Log.d(TAG, "call()");
		if (pendingLikeOrComment) {
			onSessionStateChange(session, state, exception);
			
		} else {
			friendActivityListAdapter.call(session, state, exception);
		}
	}
	
	private void showNoFriendsActivityFound() {
		/**
		 * try-catch is used to handle case where even before we get call back
		 * to this function, user leaves this screen.
		 */
		try {
			getListView().setVisibility(View.GONE);

		} catch (IllegalStateException e) {
			Log.e(TAG, "" + e.getMessage());
			e.printStackTrace();
		}

		TextView txtNoContentMsg = (TextView) rltRootNoContentFound.findViewById(R.id.txtNoItemsMsg);
		txtNoContentMsg.setText(R.string.events_are_better_with_friends);
		txtNoContentMsg.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_list_follow, 0, 0);

		((ImageView) rltRootNoContentFound.findViewById(R.id.imgPhone))
			.setImageDrawable(res.getDrawable(R.drawable.ic_friends_activity_no_content));

		rltRootNoContentFound.setVisibility(View.VISIBLE);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.btnAction:
			((OnSettingsItemClickedListener) FragmentUtil.getActivity(this))
				.onSettingsItemClicked(SettingsItem.INVITE_FRIENDS, null);
			break;

		default:
			break;
		}
	}

	@Override
	public void onPublishPermissionGranted() {
		friendActivityListAdapter.onPublishPermissionGranted();
	}

	@Override
	public String getScreenName() {
		return "Friend News Screen";
	}

	@Override
	public void addViewsToBeHidden(View... views) {
		for (int i = 0; i < views.length; i++) {
			hiddenViews.add(views[i]);
		}
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
		 * to remove facebook callback. Not calling onStop() to prevent toolbar color changes occurring in between
		 * the transition
		 */
		super.onStop();
		
		isOnPushedToBackStackCalled = true;
	}

	@Override
	public void onPoppedFromBackStack() {
		if (isOnPushedToBackStackCalled) {
			isOnPushedToBackStackCalled = false;
			
			// to update statusbar visibility
			onStart();
			// to call onFragmentResumed(Fragment) of MainActivity (to update title, current fragment tag, etc.)
			onResume();
			
			for (Iterator<View> iterator = hiddenViews.iterator(); iterator.hasNext();) {
				View view = iterator.next();
				view.setVisibility(View.VISIBLE);
			}
			hiddenViews.clear();
		}
	}

	@Override
	public boolean isOnTop() {
		return !isOnPushedToBackStackCalled;
	}	
}
