package com.wcities.eventseeker;

import android.R.color;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
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

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.wcities.eventseeker.SettingsFragment.OnSettingsItemClickedListener;
import com.wcities.eventseeker.analytics.GoogleAnalyticsTracker;
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
import com.wcities.eventseeker.constants.Enums.PublishRequest;
import com.wcities.eventseeker.constants.Enums.SettingsItem;
import com.wcities.eventseeker.core.Event.Attending;
import com.wcities.eventseeker.core.FriendNewsItem;
import com.wcities.eventseeker.custom.fragment.PublishEventListFragment;
import com.wcities.eventseeker.custom.view.CircleImageView;
import com.wcities.eventseeker.custom.view.ResizableImageView;
import com.wcities.eventseeker.interfaces.EventListenerTab;
import com.wcities.eventseeker.interfaces.PublishListener;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.FbUtil;
import com.wcities.eventseeker.util.FragmentUtil;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class FriendsActivityFragmentTab extends PublishEventListFragment implements 
		/*StatusCallback,*/ OnClickListener, PublishListener, GeneralDialogFragment.DialogBtnClickListener {
	
	private static final String TAG = FriendsActivityFragmentTab.class.getName();
	
	// List of additional write permissions being requested
	private static final List<String> PERMISSIONS = Arrays.asList("publish_actions");
	// Request code for facebook reauthorization requests. 
	private static final int FACEBOOK_REAUTH_ACTIVITY_CODE = 100; 
	// Flag to represent if we are waiting for extended permissions
	private boolean pendingLikeOrComment = false;
	// Strings required to build like request graph path 
	private String fbPostId;
	// indicates which type of request is pending to be executed
	private PublishRequest publishRequest;
	private CallbackManager callbackManager;
	
	private LoadFriendsNews loadFriendsNews;
	private FriendActivityListAdapter friendActivityListAdapter;

	//private int orientation;
	private String wcitiesId;
	private List<FriendNewsItem> friendNewsItems;

	private int itemsAlreadyRequested;
	private boolean isMoreDataAvailable = true;
	private int firstVisibleActivityItemPosition;
	
	private View rltRootNoContentFound;	
	private RelativeLayout rltLytPrgsBar;
	private ImageView imgPrgOverlay;

	private Resources res;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		
		if (wcitiesId == null) {
			wcitiesId = ((EventSeekr)FragmentUtil.getActivity(this).getApplication()).getWcitiesId();
		}
		
		res = FragmentUtil.getResources(this);
		callbackManager = CallbackManager.Factory.create();
		LoginManager.getInstance().registerCallback(callbackManager, this);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View v = inflater.inflate(R.layout.fragment_friends_activity_list, null);
		
		/**
		 * Since we are using same layout res as that of Mobile and in Mobile layout bg color is gray.
		 */
		v.setBackgroundColor(Color.WHITE);
		
		rltRootNoContentFound = v.findViewById(R.id.rltRootNoContentFound);
		rltLytPrgsBar = (RelativeLayout) v.findViewById(R.id.rltLytPrgsBar);
		imgPrgOverlay = (ImageView) rltLytPrgsBar.findViewById(R.id.imgPrgOverlay);
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if (friendNewsItems == null) {
			friendNewsItems = new ArrayList<FriendNewsItem>();
			friendActivityListAdapter = new FriendActivityListAdapter(this);
	        
			friendNewsItems.add(null);
			loadFriendsNewsInBackground();
			
		} else {
			if (friendNewsItems.isEmpty()) {
				showNoFriendsActivityFound();
			}
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
	public void onDestroyView() {
		//Log.d(TAG, "onDestroyView()");
		
		firstVisibleActivityItemPosition = getListView().getFirstVisiblePosition();
		
		for (int i = getListView().getFirstVisiblePosition(), j = 0; 
				i <= getListView().getLastVisiblePosition(); i++, j++) {
			freeUpBitmapMemory(getListView().getChildAt(j));
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
		if (FragmentUtil.getActivity(this).getIntent().hasExtra(BundleKeys.IS_FROM_NOTIFICATION)) {
			loadFriendsNews.setAddSrcFromNotification(true);
			FragmentUtil.getActivity(this).getIntent().removeExtra(BundleKeys.IS_FROM_NOTIFICATION);
		}
		AsyncTaskUtil.executeAsyncTask(loadFriendsNews, true);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		//Log.d(TAG, "onActivityResult(), requestCode = " + requestCode);
		if (isPendingAnnounce()) {
			super.onActivityResult(requestCode, resultCode, data);
			
		} else {
			// for like/comment functionality
			callbackManager.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	public void doPositiveClick(String dialogTag) {
		if (AppConstants.DIALOG_FRAGMENT_TAG_EVENT_SAVED.equals(dialogTag)) {
			addEventToCalendar();
		}
	}

	@Override
	public void doNegativeClick(String dialogTag) {

	}

	private class LoadFriendsNews extends AsyncTask<Void, Void, List<FriendNewsItem>> {
		
		private static final int FRIENDS_NEWS_LIMIT = 10;
		private boolean addSrcFromNotification;
		
		public void setAddSrcFromNotification(boolean addSrcFromNotification) {
			this.addSrcFromNotification = addSrcFromNotification;
		}
		
		@Override
		protected List<FriendNewsItem> doInBackground(Void... params) {
			List<FriendNewsItem> tmpFriendNewsItems = new ArrayList<FriendNewsItem>();
			UserInfoApi userInfoApi = new UserInfoApi(Api.OAUTH_TOKEN);
			userInfoApi.setLimit(FRIENDS_NEWS_LIMIT);
			userInfoApi.setAlreadyRequested(itemsAlreadyRequested);
			userInfoApi.setUserId(wcitiesId);
			userInfoApi.setTracktype(Tracktype.event);
			userInfoApi.setSrcFromNotification(addSrcFromNotification);
			
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
				if (friendNewsItem.getAttending() == Attending.NOT_GOING || friendNewsItem.getSchedule().getDates().size() == 0) {
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
			// free up memory
			imgPrgOverlay.setBackgroundResource(0);
			imgPrgOverlay.setVisibility(View.GONE);
			rltLytPrgsBar.setVisibility(View.GONE);
		}
    }
	
	private class FriendActivityListAdapter extends BaseAdapter {
		
	    private FriendNewsItem newsItemPendingPublish;
		private CheckBox newsItemPendingPublishChkBoxSave;
		private Fragment fragment;

	    public FriendActivityListAdapter(Fragment fragment) {
	    	this.fragment = fragment;
	    }
	    
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			FriendNewsItem item = getItem(position);
			LayoutInflater inflater = LayoutInflater.from(FragmentUtil.getActivity(fragment));
			if (item == null) {
				if (convertView == null || convertView.getTag() instanceof FriendNewsItemViewHolder) {
					convertView = inflater.inflate(R.layout.progress_bar_eventseeker_fixed_ht, null);
					convertView.setTag(AppConstants.TAG_PROGRESS_INDICATOR);
					convertView.setBackgroundColor(getResources().getColor(R.color.root_lnr_layout_bg_friends_activity_list_item));
				}
				
				if (friendNewsItems.size() == 1) {
					rltLytPrgsBar.setVisibility(View.VISIBLE);
					imgPrgOverlay.setVisibility(View.VISIBLE);
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
					convertView = inflater.inflate(R.layout.item_list_friends_activity_tab, null);
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

				ViewCompat.setTransitionName(imgEvt, "imgEvtFriendsActivity" + pos);

				if (item.getSchedule() != null) {
					String strDate = item.getSchedule().getDateRangeOrDateToDisplay(
							FragmentUtil.getApplication(fragment), true, false, false);
					if (!strDate.trim().equals("")) {
						strDate = strDate + " @ ";
					}
					txtVenue.setText(strDate.toUpperCase() + item.getSchedule().getVenue().getName().toUpperCase());
				}
				
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
						((EventListenerTab) FragmentUtil.getActivity(fragment)).onEventSelected(item.toEvent(),
								imgEvt, null);
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
							onBuyTicketClicked(item);
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
				EventSeekr eventSeekr = FragmentUtil.getApplication(fragment);
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

					item.setNewUserAttending(userAttending);
					newsItemPendingPublish = item;
					newsItemPendingPublishChkBoxSave = chkSave;
					/**
					 * setting friendNewsItem in PublishEventListFragment is required
					 * 1) for if - where it's used from handlePublishEvent()
					 * 2) for else - to add event to calendar
					 */
					((PublishEventListFragment)FriendsActivityFragmentTab.this).setFriendNewsItem(newsItemPendingPublish);

					if (eventSeekr.getGPlusUserId() != null) {
						((PublishEventListFragment)FriendsActivityFragmentTab.this).handlePublishEvent();

					} else {
						//NOTE: THIS CAN BE TESTED WITH PODUCTION BUILD ONLY
						FbUtil.handlePublishFriendNewsItem(FriendsActivityFragmentTab.this, FriendsActivityFragmentTab.this, 
								AppConstants.PERMISSIONS_FB_PUBLISH_EVT_OR_ART, item);
					}
				}
			}
		}
		
		public void onSuccess(LoginResult loginResult) {
			//Log.d(TAG, "onSuccess()");
			FbUtil.handlePublishFriendNewsItem(FriendsActivityFragmentTab.this, FriendsActivityFragmentTab.this,
					AppConstants.PERMISSIONS_FB_PUBLISH_EVT_OR_ART, newsItemPendingPublish);
		}
		
		public void onPublishPermissionGranted() {
			updateAttendingChkSave(newsItemPendingPublish, newsItemPendingPublishChkBoxSave);
		}
		
		private void updateAttendingChkSave(FriendNewsItem item, CheckBox chkSave) {
			chkSave.setChecked(item.getUserAttending() == Attending.SAVED);			
		}
	}
	
	private void onBuyTicketClicked(FriendNewsItem item) {
		EventSeekr eventSeekr = FragmentUtil.getApplication(this);
		BaseActivityTab baseActivityTab = (BaseActivityTab) FragmentUtil.getActivity(this);
				
		Intent intent = new Intent(eventSeekr, WebViewActivityTab.class);
		intent.putExtra(BundleKeys.URL, item.getBookingUrl() + "&lang="
                + ((EventSeekr) FragmentUtil.getApplication(this)).getLocale().getLocaleCode());
		baseActivityTab.startActivity(intent);
		
		GoogleAnalyticsTracker.getInstance().sendEvent(eventSeekr, 
				baseActivityTab.getScreenName(), GoogleAnalyticsTracker.EVENT_LABEL_TICKETS_BUTTON, 
				GoogleAnalyticsTracker.Type.Event.name(), null, item.getTrackId());
	}

    @SuppressLint("ValidFragment")
	private static class AddCommentDialogFragment extends DialogFragment {

	    /**
	     * Create a new instance of AddCommentDialogFragment
	     */
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
	                            ((FriendsActivityFragmentTab)getParentFragment()).doPositiveClickToAddComment(
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
		if (!FbUtil.hasPermission(PERMISSIONS)) {
			Log.d(TAG, "no permission");
			pendingLikeOrComment = true; // Mark that we are currently waiting for opening of session
			// we get top level parent fragment here since onActivityResult() is not called in nested fragments.
			LoginManager.getInstance().logInWithPublishPermissions(this, PERMISSIONS);
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
	
	private void postLikeRequest() {
		//Log.d(TAG, "postLikeRequest()");
		Toast.makeText(FragmentUtil.getActivity(this), R.string.sending_like_request, Toast.LENGTH_SHORT).show();
		GraphRequest likeRequest = GraphRequest.newPostRequest(AccessToken.getCurrentAccessToken(), fbPostId + "/likes", null, new GraphRequest.Callback() {
			@Override
			public void onCompleted(GraphResponse graphResponse) {
				Log.i(TAG, graphResponse.toString());
			}
		});
		likeRequest.executeAsync();
	}
	
	private void postCommentRequest(String comment) {
		//Log.d(TAG, "postCommentRequest()");
		Toast.makeText(FragmentUtil.getActivity(this), R.string.posting_comment, Toast.LENGTH_SHORT).show();
		Bundle parameters = new Bundle();
		parameters.putString("message", comment);
		GraphRequest commentRequest = GraphRequest.newPostRequest(AccessToken.getCurrentAccessToken(), fbPostId + "/comments",
				null, new GraphRequest.Callback() {
					@Override
					public void onCompleted(GraphResponse graphResponse) {
						Log.i(TAG, graphResponse.toString());
					}
				});
		commentRequest.setParameters(parameters);
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

	@Override
	public void onSuccess(LoginResult loginResult) {
		Log.d(TAG, "onSuccess()");
		if (pendingLikeOrComment) {
			handlePublish();

		} else {
			friendActivityListAdapter.onSuccess(loginResult);
		}
	}

	@Override
	public void onCancel() {
		Log.d(TAG, "onCancel()");
		pendingLikeOrComment = false;
	}

	@Override
	public void onError(FacebookException e) {
		Log.d(TAG, "onError()");
		pendingLikeOrComment = false;
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
		if (FragmentUtil.getActivity(this) != null) {
			// if user has not left the screen (activity)
			friendActivityListAdapter.onPublishPermissionGranted();
			showAddToCalendarDialog(this);
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		//Log.d(TAG, "onDestroy()");
		if (loadFriendsNews != null && loadFriendsNews.getStatus() != Status.FINISHED) {
			loadFriendsNews.cancel(true);
		}
	}
}
