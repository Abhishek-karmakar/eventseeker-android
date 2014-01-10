package com.wcities.eventseeker;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.api.UserInfoApi.Tracktype;
import com.wcities.eventseeker.api.UserInfoApi.Type;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.core.Date;
import com.wcities.eventseeker.core.Event.Attending;
import com.wcities.eventseeker.core.FriendNewsItem;
import com.wcities.eventseeker.custom.fragment.ListFragmentLoadableFromBackStack;
import com.wcities.eventseeker.custom.view.ResizableImageView;
import com.wcities.eventseeker.interfaces.EventListener;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class FriendsActivityFragment extends ListFragmentLoadableFromBackStack implements StatusCallback {
	
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
	private boolean pendingAnnounce = false;
	// Strings required to build like request graph path 
	private String fbPostId;
	// indicates which type of request is pending to be executed
	private PublishRequest publishRequest;
	
	private LoadFriendsNews loadFriendsNews;
	private FriendActivityListAdapter friendActivityListAdapter;

	private int orientation;
	private String wcitiesId;
	private List<FriendNewsItem> friendNewsItems;

	private int itemsAlreadyRequested;
	private boolean isMoreDataAvailable = true;
	private int firstVisibleActivityItemPosition;
	
	private boolean isTablet;
	private boolean is7InchTabletInPortrait;
	private View rltDummyLyt;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		
		if (wcitiesId == null) {
			wcitiesId = ((EventSeekr)FragmentUtil.getActivity(this).getApplication()).getWcitiesId();
		}
		
		isTablet = ((EventSeekr)FragmentUtil.getActivity(this).getApplicationContext()).isTablet();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		is7InchTabletInPortrait = ((EventSeekr)FragmentUtil.getActivity(this).getApplicationContext())
				.is7InchTabletAndInPortraitMode();
		orientation = getResources().getConfiguration().orientation;
		View v = inflater.inflate(R.layout.fragment_friends_activity_list, null);
		rltDummyLyt = v.findViewById(R.id.rltDummyLyt);
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if (friendNewsItems == null) {
			friendNewsItems = new ArrayList<FriendNewsItem>();
			friendActivityListAdapter = new FriendActivityListAdapter(FragmentUtil.getActivity(this));
	        
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
		
		final int pos;
		if(is7InchTabletInPortrait) {
			pos = firstVisibleActivityItemPosition;
		} else if (orientation == Configuration.ORIENTATION_PORTRAIT && !isTablet) {
			pos = firstVisibleActivityItemPosition;			
		} else {
			pos = (int)Math.floor(firstVisibleActivityItemPosition / 2.0);
		}
		
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
		
		if(is7InchTabletInPortrait) {
			firstVisibleActivityItemPosition = getListView().getFirstVisiblePosition();
		} else if (orientation == Configuration.ORIENTATION_PORTRAIT && !isTablet) {
			firstVisibleActivityItemPosition = getListView().getFirstVisiblePosition();			
		} else {
			firstVisibleActivityItemPosition = getListView().getFirstVisiblePosition() * 2;;
		}
		
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
		Log.d(TAG, "onActivityResult(), requestCode = " + requestCode);
		Session session = Session.getActiveSession();
        if (session != null) {
        	Log.d(TAG, "session!=null");
            session.onActivityResult(FragmentUtil.getActivity(this), requestCode, resultCode, data);
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
				if(friendNewsItems.isEmpty()) {
					/*FriendNewsItem friendNewsItem = new FriendNewsItem();
					friendNewsItem.setFriendId(AppConstants.INVALID_STR_ID);
					friendNewsItems.add(friendNewsItem);*/
					showNoFriendsActivityFound();
				}
			}
			
			
			friendActivityListAdapter.notifyDataSetChanged();
		}

    }
	
	private class FriendActivityListAdapter extends BaseAdapter {
		
		private static final String TAG_PROGRESS_INDICATOR = "progressIndicator";
		
	    private LayoutInflater mInflater;

	    public FriendActivityListAdapter(Context context) {
	        mInflater = LayoutInflater.from(context);
	    }
	    
	    public void setmInflater(Context context) {
	        mInflater = LayoutInflater.from(context);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Object item = getItem(position);
			if (item == null || 
					((item instanceof List) && ((List<FriendNewsItem>)item).get(0) == null)) {
				if (convertView == null || convertView.getTag() instanceof FriendNewsItemViewHolder) {
					convertView = mInflater.inflate(R.layout.list_progress_bar, null);
					convertView.setTag(TAG_PROGRESS_INDICATOR);
					convertView.setBackgroundColor(getResources().getColor(R.color.root_lnr_layout_bg_friends_activity_list_item));
				}
				
				if ((loadFriendsNews == null || loadFriendsNews.getStatus() == Status.FINISHED) && 
						isMoreDataAvailable) {
					loadFriendsNewsInBackground();
				}
				
			} else {
				FriendNewsItemViewHolder holder;
				
				/*if ((item instanceof List)
						&& ((List<FriendNewsItem>)item).get(0).getFriendId().equals(AppConstants.INVALID_STR_ID) 
					|| (item instanceof FriendNewsItem)
						&& ((FriendNewsItem)item).getFriendId().equals(AppConstants.INVALID_STR_ID)) {
					
					convertView = mInflater.inflate(R.layout.list_no_items_found, null);
					((TextView)convertView).setText("No Friends Activity Found.");
					convertView.setTag("");
					return convertView;
					
				} else */if (convertView == null || !(convertView.getTag() instanceof FriendNewsItemViewHolder)) {
					convertView = mInflater.inflate(R.layout.friends_activity_list_item, null);
					holder = new FriendNewsItemViewHolder();
					
					RelativeLayout rltLayoutNewsItemContainer = holder.rltLayoutNewsItemContainer 
							= (RelativeLayout) convertView.findViewById(R.id.rltLayoutNewsItemContainer);
					holder.txtTitle = (TextView) rltLayoutNewsItemContainer.findViewById(R.id.txtTitle);
					holder.imgEvt = (ResizableImageView) rltLayoutNewsItemContainer.findViewById(R.id.imgEvt);
					holder.lnrLayoutBtns = (LinearLayout) rltLayoutNewsItemContainer.findViewById(R.id.lnrLayoutBtns);
					holder.lnrLayoutBtnLike = (LinearLayout) rltLayoutNewsItemContainer.findViewById(R.id.lnrLayoutBtnLike);
					holder.lnrLayoutBtnComment = (LinearLayout) rltLayoutNewsItemContainer.findViewById(R.id.lnrLayoutBtnComment);
					holder.txtEvtTime = (TextView) rltLayoutNewsItemContainer.findViewById(R.id.txtEvtTime);
					holder.txtVenueTitle = (TextView) rltLayoutNewsItemContainer.findViewById(R.id.txtVenueTitle);
					
					RelativeLayout rltLayoutNewsItem2Container = holder.rltLayoutNewsItem2Container 
							= (RelativeLayout) convertView.findViewById(R.id.rltLayoutNewsItemContainer2);
					holder.txtTitle2 = (TextView) rltLayoutNewsItem2Container.findViewById(R.id.txtTitle);
					holder.imgEvt2 = (ResizableImageView) rltLayoutNewsItem2Container.findViewById(R.id.imgEvt);
					holder.lnrLayoutEvtInfo2 = (RelativeLayout) rltLayoutNewsItem2Container.findViewById(R.id.lnrLayoutEvtInfo);
					holder.lnrLayoutBtns2 = (LinearLayout) rltLayoutNewsItem2Container.findViewById(R.id.lnrLayoutBtns);
					holder.lnrLayoutBtnLike2 = (LinearLayout) rltLayoutNewsItem2Container.findViewById(R.id.lnrLayoutBtnLike);
					holder.lnrLayoutBtnComment2 = (LinearLayout) rltLayoutNewsItem2Container.findViewById(R.id.lnrLayoutBtnComment);
					holder.txtEvtTime2 = (TextView) rltLayoutNewsItem2Container.findViewById(R.id.txtEvtTime);
					holder.txtVenueTitle2 = (TextView) rltLayoutNewsItem2Container.findViewById(R.id.txtVenueTitle);
					holder.progressBar2 = (ProgressBar) rltLayoutNewsItem2Container.findViewById(R.id.progressBar);
					
					convertView.setTag(holder);
					
				} else {
					holder = (FriendNewsItemViewHolder) convertView.getTag();
				}
				
				holder.setContent(item, parent, position);
			}
			
			return convertView;
		}

		@Override
		public Object getItem(int position) {

			if (is7InchTabletInPortrait || (orientation == Configuration.ORIENTATION_PORTRAIT && !isTablet)) {
				return friendNewsItems.get(position); // returning an object of FriendNewsItem, NOT A LIST 
				
			} else {
				List<FriendNewsItem> friendsNewsItemList = new ArrayList<FriendNewsItem>();
				friendsNewsItemList.add(friendNewsItems.get(position * 2));
				if (friendNewsItems.size() > position * 2 + 1) {
					friendsNewsItemList.add(friendNewsItems.get(position * 2 + 1));
				}
				return friendsNewsItemList; // returning an object of List<FriendNewsItem>
			}
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public int getCount() {
			//Mithil: In tablet we have to show two elements in both the cases
			if (is7InchTabletInPortrait || (orientation == Configuration.ORIENTATION_PORTRAIT && !isTablet)) {
				return friendNewsItems.size();
			} else {
				return (int) Math.ceil(friendNewsItems.size() / 2.0);
			}
		}
		
		private class FriendNewsItemViewHolder {
			
			private RelativeLayout rltLayoutNewsItemContainer;
			private ResizableImageView imgEvt;
			private TextView txtTitle, txtEvtTime, txtVenueTitle;
			private LinearLayout lnrLayoutBtns, lnrLayoutBtnLike, lnrLayoutBtnComment;
			
			private RelativeLayout rltLayoutNewsItem2Container, lnrLayoutEvtInfo2;
			private ResizableImageView imgEvt2;
			private TextView txtTitle2, txtEvtTime2, txtVenueTitle2;
			private LinearLayout lnrLayoutBtns2, lnrLayoutBtnLike2, lnrLayoutBtnComment2;
			private ProgressBar progressBar2;
			
			private void setContent(Object listItem, ViewGroup parent, int pos) {
				if (listItem instanceof FriendNewsItem) {
					//Log.d(TAG, "ArtistNewsItem");
					rltLayoutNewsItem2Container.setVisibility(View.GONE);
					setNewsItemContent((FriendNewsItem) listItem, parent, pos);
					
				} else {
					List<FriendNewsItem> friendNewsItemList = (List<FriendNewsItem>) listItem;
					setNewsItemContent((FriendNewsItem) friendNewsItemList.get(0), parent, pos);
					
					// Check if we have 2nd non-null item
					if (friendNewsItemList.size() == 2 && friendNewsItemList.get(1) != null) {
						//Log.d(TAG, "not ArtistNewsItem, size = 2");
						rltLayoutNewsItem2Container.setVisibility(View.VISIBLE);
						setProgressBar2Visibility(false);
						setNewsItem2Content((FriendNewsItem) friendNewsItemList.get(1), parent, pos);
						
					} else if ((loadFriendsNews == null || loadFriendsNews.getStatus() == Status.FINISHED) && 
							isMoreDataAvailable) {
						rltLayoutNewsItem2Container.setVisibility(View.VISIBLE);
						setProgressBar2Visibility(true);
						loadFriendsNewsInBackground();
						
					} else {
						//Log.d(TAG, "not ArtistNewsItem, size = 1");
						rltLayoutNewsItem2Container.setVisibility(View.INVISIBLE);
					}
				}
			}
			
			private void setProgressBar2Visibility(boolean isProgress) {
				int progressVisibility, othersVisibility;
				if (isProgress) {
					progressVisibility = View.VISIBLE;
					othersVisibility = View.INVISIBLE;
					
				} else {
					progressVisibility = View.INVISIBLE;
					othersVisibility = View.VISIBLE;
				}
				
				progressBar2.setVisibility(progressVisibility);
				txtTitle2.setVisibility(othersVisibility);
				imgEvt2.setVisibility(othersVisibility);
				lnrLayoutEvtInfo2.setVisibility(othersVisibility);
				lnrLayoutBtns2.setVisibility(othersVisibility);
			}
			
			private void setNewsItemContent(final FriendNewsItem item, ViewGroup parent, int pos) {
				switch (item.getAttending()) {
				case GOING:
					txtTitle.setText(item.getFriendName() + " is going to " + item.getTrackName());
					break;
					
				case WANTS_TO_GO:
					txtTitle.setText(item.getFriendName() + " wants to go to " + item.getTrackName());
					break;

				default:
					break;
				}
				
				String key = item.getKey(ImgResolution.LOW);
		        BitmapCache bitmapCache = BitmapCache.getInstance();
				Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
				if (bitmap != null) {
					imgEvt.setImageBitmap(bitmap);
			        
			    } else {
					imgEvt.setImageResource(R.drawable.placeholder);
			        AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
			        asyncLoadImg.loadImg(imgEvt, ImgResolution.LOW, (AdapterView) parent, 
			        		pos, item);
			    }
				
				txtVenueTitle.setText(item.getVenueName());
				
				Date date = item.getStartTime();
				if (date == null) {
					txtEvtTime.setVisibility(View.INVISIBLE);
					
				} else {
					txtEvtTime.setVisibility(View.VISIBLE);
					
					DateFormat dateFormat = new SimpleDateFormat("EEE MMMM dd, yyyy");
					txtEvtTime.setText(dateFormat.format(date.getStartDate()));
				}
				
				if (item.getFbPostId() != null) {
					lnrLayoutBtns.setVisibility(View.VISIBLE);
					rltLayoutNewsItemContainer.setPadding(0, 0, 0, 0);
					
					lnrLayoutBtnLike.setOnClickListener(new View.OnClickListener() {
						
						@Override
						public void onClick(View v) {
							Log.d(TAG, "like onClick()");
							fbPostId = item.getFbPostId();
							publishRequest = PublishRequest.LIKE;
							handlePublish();
						}
					});
					
					lnrLayoutBtnComment.setOnClickListener(new View.OnClickListener() {
						
						@Override
						public void onClick(View v) {
							fbPostId = item.getFbPostId();
							publishRequest = PublishRequest.COMMENT;
							handlePublish();
						}
					});
					
				} else {
					lnrLayoutBtns.setVisibility(View.GONE);
					rltLayoutNewsItemContainer.setPadding(0, 0, 0, 
							getResources().getDimensionPixelSize(R.dimen.tab_bar_margin_fragment_custom_tabs));
					//lnrLayoutBtnLike.setOnClickListener(null);
					//lnrLayoutBtnComment.setOnClickListener(null);
				}
				rltLayoutNewsItemContainer.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						((EventListener)FragmentUtil.getActivity(FriendsActivityFragment.this)).onEventSelected(item.toEvent());
					}
				});
			}
			
			private void setNewsItem2Content(final FriendNewsItem item, ViewGroup parent, int pos) {
				switch (item.getAttending()) {
				case GOING:
					txtTitle2.setText(item.getFriendName() + " is going to " + item.getTrackName());
					break;
					
				case WANTS_TO_GO:
					txtTitle2.setText(item.getFriendName() + " wants to go to " + item.getTrackName());
					break;

				default:
					break;
				}
				
				String key = item.getKey(ImgResolution.HIGH);
		        BitmapCache bitmapCache = BitmapCache.getInstance();
				Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
				if (bitmap != null) {
					imgEvt2.setImageBitmap(bitmap);
			        
			    } else {
					imgEvt2.setImageResource(R.drawable.placeholder);
			        AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
			        asyncLoadImg.loadImg(imgEvt2, ImgResolution.HIGH, (AdapterView) parent, 
			        		pos, item);
			    }
				
				txtVenueTitle2.setText(item.getVenueName());
				
				Date date = item.getStartTime();
				if (date == null) {
					txtEvtTime2.setVisibility(View.INVISIBLE);
					
				} else {
					txtEvtTime2.setVisibility(View.VISIBLE);
					
					DateFormat dateFormat = new SimpleDateFormat("EEE MMMM dd, yyyy");
					txtEvtTime2.setText(dateFormat.format(date.getStartDate()));
				}
				
				if (item.getFbPostId() != null) {
					lnrLayoutBtns2.setVisibility(View.VISIBLE);
					rltLayoutNewsItem2Container.setPadding(0, 0, 0, 0);
					
					lnrLayoutBtnLike2.setOnClickListener(new View.OnClickListener() {
						
						@Override
						public void onClick(View v) {
							Log.d(TAG, "like onClick()");
							fbPostId = item.getFbPostId();
							publishRequest = PublishRequest.LIKE;
							handlePublish();
						}
					});
					
					lnrLayoutBtnComment2.setOnClickListener(new View.OnClickListener() {
						
						@Override
						public void onClick(View v) {
							fbPostId = item.getFbPostId();
							publishRequest = PublishRequest.COMMENT;
							handlePublish();
						}
					});
					
				} else {
					lnrLayoutBtns2.setVisibility(View.GONE);
					rltLayoutNewsItem2Container.setPadding(0, 0, 0, 
							getResources().getDimensionPixelSize(R.dimen.tab_bar_margin_fragment_custom_tabs));
					//lnrLayoutBtnLike2.setOnClickListener(null);
					//lnrLayoutBtnComment2.setOnClickListener(null);
				}
				rltLayoutNewsItem2Container.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						((EventListener)FragmentUtil.getActivity(FriendsActivityFragment.this)).onEventSelected(item.toEvent());
					}
				});
			}
		}
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
	    	
	        return new AlertDialog.Builder(FragmentUtil.getActivity(this))
	                .setTitle("Add Comment")
	                .setView(input)
	                .setPositiveButton("Add",
	                    new DialogInterface.OnClickListener() {
	                        public void onClick(DialogInterface dialog, int whichButton) {
	                            ((FriendsActivityFragment)getParentFragment()).doPositiveClick(
	                            		input.getText().toString());
	                        }
	                    }
	                )
	                .setNegativeButton("Cancel", null)
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
		pendingAnnounce = false;
	    Session session = Session.getActiveSession();

	    if (session == null) {
	    	Log.d(TAG, "session=null");
	    	session = new Session(FragmentUtil.getActivity(this));
			Session.setActiveSession(session);
	    }
	    
	    Log.d(TAG, "active session, state=" + session.getState().name());
	    if (!session.isOpened()) {
	    	Log.d(TAG, "session is not opened");
	    	pendingAnnounce = true; // Mark that we are currently waiting for opening of session
    		Session.openActiveSession(FragmentUtil.getActivity(this), this, true, this);
    		return false;
	    }

	    if (!hasPublishPermission()) {
	    	Log.d(TAG, "publish permission is not there");
	        pendingAnnounce = true; // Mark that we are currently waiting for confirmation of publish permissions
	        session.addCallback(this); 
	        requestPublishPermissions(session, PERMISSIONS, FACEBOOK_REAUTH_ACTIVITY_CODE);
	        return false;
	    }
	    
	    return true;
	}
	
	private void handlePublish() {
		Log.i(TAG, "handlePublish()");
		if (canPublishNow()) {
			if (publishRequest == PublishRequest.LIKE) {
				postLikeRequest();
				
			} else if (publishRequest == PublishRequest.COMMENT) {
				showAddCommentDialog();
			}
		}
	}
	
	private boolean hasPublishPermission() {
		Log.d(TAG, "hasPublishPermission()");
        Session session = Session.getActiveSession();
        List<String> permissions = session.getPermissions();
        for (Iterator<String> iterator = permissions.iterator(); iterator.hasNext();) {
			Log.d(TAG, iterator.next());
		}
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
		Toast.makeText(FragmentUtil.getActivity(this), "Sending like request...", Toast.LENGTH_SHORT).show();
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
		Toast.makeText(FragmentUtil.getActivity(this), "Posting comment...", Toast.LENGTH_SHORT).show();
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

	private void doPositiveClick(String comment) {
	    // Do stuff here.
	    postCommentRequest(comment);
	}

	/**
	 * Called when additional permission request is completed successfully.
	 */
	private void tokenUpdated() {
		Log.d(TAG, "tokenUpdated()");
	    // Check if a publish action is in progress
	    // awaiting a successful reauthorization
	    if (pendingAnnounce) {
	        
	    	if (hasPublishPermission()) {
	    		// Publish the action
	    		handlePublish();
	    		
	    	} else {
	    		// user has denied the permission
	    		pendingAnnounce = false;
	    	}
	    }
	}
	
	private void sessionOpened() {
		Log.d(TAG, "sessionOpened()");
		// Check if a publish action is in progress
	    // awaiting a successful reauthorization
	    if (pendingAnnounce) {
	        // Publish the action
	    	handlePublish();
	    }
	}
	
	protected void onSessionStateChange(final Session session, SessionState state, Exception exception) {
		Log.d(TAG, "onSessionStateChange() state = " + state.name());
	    if (session != null && session.isOpened()) {
	    	Log.d(TAG, "session != null && session.isOpened(), state = " + state.name());
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
		Log.d(TAG, "call()");
		onSessionStateChange(session, state, exception);
	}
	
	private void showNoFriendsActivityFound() {
		rltDummyLyt.setVisibility(View.VISIBLE);
		if (wcitiesId == null) {
			TextView txtNoItemsFound = (TextView)rltDummyLyt.findViewById(R.id.txtNoItemsFound);
			txtNoItemsFound.setText(getResources().getString(R.string.no_items_found_pls_login) + " your friends activity.");
		}
		getListView().setVisibility(View.GONE);
	}    	

}
