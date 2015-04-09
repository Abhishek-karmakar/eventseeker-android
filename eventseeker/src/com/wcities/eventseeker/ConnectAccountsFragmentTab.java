package com.wcities.eventseeker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.wcities.eventseeker.GeneralDialogFragment.DialogBtnClickListener;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.app.EventSeekr.EventSeekrListener;
import com.wcities.eventseeker.asynctask.GetAuthToken;
import com.wcities.eventseeker.asynctask.LoadMyEventsCount;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.constants.Enums.Service;
import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.custom.fragment.ListFragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.interfaces.DrawerListFragmentListener;
import com.wcities.eventseeker.interfaces.SyncArtistListener;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.ServiceAccount;

public class ConnectAccountsFragmentTab extends ListFragmentLoadableFromBackStack implements EventSeekrListener, 
		AsyncTaskListener<Object>, DialogBtnClickListener, SyncArtistListener {
	
    private static final String TAG = ConnectAccountsFragmentTab.class.getName();
    
    private static final String TXT_BTN_SKIP_DIALOG = "Skip";
    protected static final String TXT_BTN_CANCEL_DIALOG = "Cancel";
    private static String TXT_BTN_SKIP = "SKIP";
    private String TXT_BTN_CONTINUE;
    
    private static final String DIALOG_FRAGMENT_TAG_SKIP = "skipDialog";    

	private List<Service> listAvailableServices;
	
	private AccountsListAdapter listAdapter;
	private List<ServiceAccount> serviceAccounts;
	private LoadMyEventsCount loadMyEventsCount;
	
	private boolean isProgressVisible;
	
	private RelativeLayout rltLayoutProgress;
	
	private Resources res;
	
	private Handler handler;
	private int syncInProgressCount;

	private boolean isFirstTimeLaunch, isFromSpotify;
	
    public interface ConnectAccountsFragmentListener {
    	public void onServiceSelected(Service service, Bundle args, boolean addToBackStack);
    }
    
    @Override
    public void onAttach(Activity activity) {
    	super.onAttach(activity);
    	if (!(activity instanceof ConnectAccountsFragmentListener)) {
    		throw new ClassCastException(activity.toString() + " must implement ConnectAccountsFragmentListener");
    	}
    	
    	//Log.d(TAG, "onAttach() - " + this);
    }
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		/**
		 * need to get isFirstTimeLaunch value before calling super.onCreate() since we need it to 
		 * decide on screenName to be sent to google analytics 
		 */
		EventSeekr eventSeekr = ((EventSeekr) FragmentUtil.getActivity(this).getApplication());
		isFirstTimeLaunch = eventSeekr.getFirstTimeLaunch();
		
		super.onCreate(savedInstanceState);
		//Log.d(TAG, "onCreate() - " + this);
		setRetainInstance(true);
		
		eventSeekr.registerListener(this);
		eventSeekr.updateFirstTimeLaunch(false);
		
		res = getResources();
		
		TXT_BTN_CONTINUE = res.getString(R.string.btn_continue);
		TXT_BTN_SKIP = res.getString(R.string.skip);
		
		handler = new Handler(Looper.getMainLooper());
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		//Log.d(TAG, "onCreateView() - " + this);
		View v = LayoutInflater.from(FragmentUtil.getActivity(this)).inflate(R.layout.fragment_connect_accounts, null);
		rltLayoutProgress = (RelativeLayout) v.findViewById(R.id.rltLayoutProgress);
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if (serviceAccounts == null) {
			serviceAccounts = new ArrayList<ServiceAccount>();
			
			listAdapter = new AccountsListAdapter(FragmentUtil.getActivity(this));
			
			loadAvailableService();
			
		} else {
			listAdapter.setmInflater(FragmentUtil.getActivity(this));
		}

		setListAdapter(listAdapter);
        getListView().setDivider(null);
        
        if (isProgressVisible) {
			showProgress();
		}
	}
	
	@Override
	public void onResume() {
		super.onResume(AppConstants.INVALID_INDEX, 
				FragmentUtil.getResources(this).getString(R.string.navigation_drawer_item_sync_accounts));
	}
	
	@Override
	public void onDestroy() {
		//Log.d(TAG, "onDestroy()");
		super.onDestroy();
		if (loadMyEventsCount != null && loadMyEventsCount.getStatus() != Status.FINISHED) {
			loadMyEventsCount.cancel(true);
		}
		((EventSeekr)FragmentUtil.getActivity(this).getApplication()).unregisterListener(this);
	}
	
	private void loadAvailableService() {
		if (getArguments() != null && getArguments().containsKey(BundleKeys.REQ_CODE_SPOTIFY)) {
			getArguments().remove(BundleKeys.REQ_CODE_SPOTIFY);
			isFromSpotify = true;
		}
		LoadAvailableService loadAvailableService = new LoadAvailableService() {
			
			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				showProgress();
			}
			
			@Override
			protected void onPostExecute(List<Service> result) {
				super.onPostExecute(result);

				if (isAdded()) {
					listAvailableServices = result;
					loadServiceAccountItems();
					if (isFromSpotify) {
						for (ServiceAccount serviceAcc : serviceAccounts) {
							//Log.d(TAG, "for - " + serviceAcc.name);
							if (serviceAcc.name.equals(Service.Spotify.getStr(ConnectAccountsFragmentTab.this))) {
								//Log.d(TAG, "set in progress");
								serviceAcc.isInProgress = true;
							}
						}
						onArtistSyncStarted(false);
						
					} else {
						listAdapter.notifyDataSetChanged();
					}
					
					dismissProgress();
				}
			}
		};
		if (FragmentUtil.getActivity(this).getIntent().hasExtra(BundleKeys.IS_FROM_NOTIFICATION)) {
			loadAvailableService.setAddSrcFromNotification(true);
			FragmentUtil.getActivity(this).getIntent().removeExtra(BundleKeys.IS_FROM_NOTIFICATION);
		}
		AsyncTaskUtil.executeAsyncTask(loadAvailableService, true);
	}
	
	private class LoadAvailableService extends AsyncTask<Void, Void, List<Service>> {

		private boolean addSrcFromNotification;

		public void setAddSrcFromNotification(boolean addSrcFromNotification) {
			this.addSrcFromNotification = addSrcFromNotification;
		}
		
		@Override
		protected List<Service> doInBackground(Void... params) {
			List<Service> list = new ArrayList<Service>();
			try {
				UserInfoApi userInfoApi = new UserInfoApi(Api.OAUTH_TOKEN);
				userInfoApi.setSrcFromNotification(addSrcFromNotification);
				JSONObject jsonObject = userInfoApi.getAvailableSyncServices();
				
				UserInfoApiJSONParser userInfoApiJSONParser = new UserInfoApiJSONParser();
				list = userInfoApiJSONParser.getAvailableSyncServiceList(jsonObject);
				
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return list;
		}
	}
	
	private void loadServiceAccountItems() {
		EventSeekr eventSeekr = (EventSeekr) FragmentUtil.getActivity(this).getApplication();
        	
		Service[] connectAccountsItemTitles = Service.values();
		
        for (int i = 0; i < connectAccountsItemTitles.length; i++) {
        		
        	Service service = connectAccountsItemTitles[i];
        		
        	/**
        	 * the following two lines are written above the 'if' condition of Google Play Music Service
        	 * because irrespective of we are using or not the Google Play Music Service we have to 
        	 * initialize it as UNSYNC, so that we can get proper value of isAnyServiceSynced when we call 
        	 * isAnyAccountSynced() method to show 'skip' or 'continue' button
        	 */
        	ServiceAccount serviceAccount = new ServiceAccount();
        	serviceAccount.count = eventSeekr.getSyncCount(service);
        	if (service.isService() && !listAvailableServices.contains(service)) {
        		continue;
        	}
			serviceAccount.name = service.getStr(this);
			serviceAccount.normalDrawable = service.getNormalDrwResId();
			serviceAccount.pressedDrawable = service.getPressedDrwResId();
			serviceAccounts.add(serviceAccount);
		}
        
        // add null representing Continue button
        // serviceAccounts.add(null);
        
	}
	
	private void showProgress() {
		getListView().setVisibility(View.GONE);
    	rltLayoutProgress.setVisibility(View.VISIBLE);
		isProgressVisible = true;
    }
	
	private void dismissProgress() {
		getListView().setVisibility(View.VISIBLE);
		rltLayoutProgress.setVisibility(View.GONE);
		isProgressVisible = false;
	}
	
	private class AccountsListAdapter extends BaseAdapter {
		
		private LayoutInflater mInflater;
		private Resources res;
		
	    public AccountsListAdapter(Context context) {
	        mInflater = LayoutInflater.from(context);
	        res = context.getResources();
	    }
	    
	    public void setmInflater(Context context) {
	        mInflater = LayoutInflater.from(context);
	        res = context.getResources();
		}
	    
	    private Drawable createStateListDrawableFrom(int normalStateDrawableResId, int pressedStateDrawableResId) {
	    	StateListDrawable drawable = new StateListDrawable();
	    	drawable.addState(new int[]{android.R.attr.state_pressed}, res.getDrawable(pressedStateDrawableResId));
	    	drawable.addState(new int[]{}, res.getDrawable(normalStateDrawableResId));
	    	return drawable;
	    }

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			//Log.d(TAG, "getView(), pos = " + position);
			
			final ServiceAccount serviceAccount = getItem(position);
			
			if (serviceAccount.name.equals(Service.Button.getStr(ConnectAccountsFragmentTab.this))) {
				
				// it's for Continue button
				convertView = mInflater.inflate(R.layout.connect_accounts_continue, null);
				Button btnContinue = (Button) convertView.findViewById(R.id.btnContinue);
				if (((EventSeekr)FragmentUtil.getActivity(ConnectAccountsFragmentTab.this).getApplication())
						.isAnyAccountSynced()) {
					btnContinue.setText(TXT_BTN_CONTINUE);
					
				} else {
					btnContinue.setText(TXT_BTN_SKIP);
				}
				btnContinue.setOnClickListener(onBtnContinueClickListener);
				
			} else if(serviceAccount.name.equals(Service.Title.getStr(ConnectAccountsFragmentTab.this))) {
				/*convertView = mInflater.inflate(R.layout.connect_accounts_txt_list_item, null);*/
				convertView = mInflater.inflate(R.layout.connect_accounts_list_item_top, null);
				convertView.setTag("");
				
				TextView txtSyncCount = (TextView) convertView.findViewById(R.id.txtSyncCount);
				txtSyncCount.setText(((EventSeekr) FragmentUtil.getActivity(ConnectAccountsFragmentTab.this)
						.getApplication()).getTotalSyncCount() + "");
				txtSyncCount.setBackgroundResource(serviceAccount.isInProgress ? 0 : R.drawable.ic_circle_bg);

				//Log.d(TAG, "serviceAccount.isInProgress = " + serviceAccount.isInProgress);
				convertView.findViewById(R.id.prgBrSyncArtist)
					.setVisibility(serviceAccount.isInProgress ? View.VISIBLE : View.GONE);
				
				
			} else {
				//Log.d(TAG, "setting Title : " + serviceAccount.name);
				final AccountViewHolder holder;
				if (convertView == null || !(convertView.getTag() instanceof AccountViewHolder)) {
					convertView = mInflater.inflate(R.layout.connect_accounts_list_item, null);
					holder = new AccountViewHolder();
					holder.txtServiceName = (TextView) convertView.findViewById(R.id.txtServiceName);
					holder.imgCorrect = (ImageView) convertView.findViewById(R.id.imgCorrect);
					convertView.setTag(holder);
					
				} else {
					holder = (AccountViewHolder) convertView.getTag();
				}
				
				holder.txtServiceName.setCompoundDrawablesWithIntrinsicBounds(
						createStateListDrawableFrom(serviceAccount.normalDrawable, serviceAccount.pressedDrawable), 
						null, null, null);
				holder.txtServiceName.setText(serviceAccount.name);
				holder.txtServiceName.setOnClickListener(new TextView.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						onItemClick(serviceAccount);
					}
				});
				
				if (serviceAccount.count != EventSeekr.UNSYNC_COUNT && !serviceAccount.isInProgress) {
					holder.imgCorrect.setVisibility(View.VISIBLE);
				
				} else {
					holder.imgCorrect.setVisibility(View.INVISIBLE);					
				}
				
				convertView.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						holder.txtServiceName.performClick();
					}
				});
			}
			
			return convertView;
		}

		@Override
		public ServiceAccount getItem(int position) {
			return serviceAccounts.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public int getCount() {
			//Log.d(TAG, "count = " + serviceAccounts.size());
			return serviceAccounts.size();
		}
		
		private void onItemClick(final ServiceAccount serviceAccount) {
			//Log.d(TAG, "Service name = " + serviceAccount.name);
			Service service = Service.getService(serviceAccount.name, ConnectAccountsFragmentTab.this);
			//Log.d(TAG, "onItemClick(), serviceId = " + serviceId);
			
			if (service == Service.Title) {
				return;
			}
			
			switch (service) {
				
			case GooglePlay:
				Intent intent = AccountPicker.newChooseAccountIntent(null, null, new String[] {GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE},
				         true, null, null, null, null);
				startActivityForResult(intent, AppConstants.REQ_CODE_GOOGLE_ACCOUNT_CHOOSER_FOR_GOOGLE_MUSIC);
				break;
				
			case Spotify:
				/**
				 * Not passing ConnectAccountsFragment.this as serializable in bundle, 
				 * because passing it as serializable to SpotifyActivity changes the actual fragment address, it 
				 * requires marking many instance variables as transient & then on call to onArtistSyncStarted()
				 * it throws exception (NullPointer). It's unable to find activity for this changed fragment address
				 * in FragmentUtil's getResources() called from getStr() of service enum which in turn is called from
				 * onArtistSyncStarted(). Hence for this case we are using startActivityForResult() from onServiceSelected().
				 */
				Bundle bundle = new Bundle();
				bundle.putSerializable(BundleKeys.SERVICE_ACCOUNTS, serviceAccount);
				((ConnectAccountsFragmentListener)FragmentUtil.getActivity(ConnectAccountsFragmentTab.this))
					.onServiceSelected(service, bundle, true);
				break;

			default:
				bundle = new Bundle();
				bundle.putString(BundleKeys.SYNC_ARTIST_LISTENER, FragmentUtil.getTag(ConnectAccountsFragmentTab.this));
				bundle.putSerializable(BundleKeys.SERVICE_ACCOUNTS, serviceAccount);
				((ConnectAccountsFragmentListener)FragmentUtil.getActivity(ConnectAccountsFragmentTab.this))
					.onServiceSelected(service, bundle, true);
				break;
			}
		}
		
		private class AccountViewHolder {
			private ImageView /*imgService,*/ imgCorrect/*imgPlus, imgProgressBar*/;
			private TextView txtServiceName/*, txtCount*/;
		}
	}
	
	private OnClickListener onBtnContinueClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			if (((Button)v).getText().equals(TXT_BTN_CONTINUE)) {
				onContinueClick();
				
			} else {
				GeneralDialogFragment generalDialogFragment = GeneralDialogFragment.newInstance(
						ConnectAccountsFragmentTab.this,
						res.getString(R.string.are_you_sure), res.getString(R.string.connecting_account_allow_us), 
						TXT_BTN_CANCEL_DIALOG, TXT_BTN_SKIP_DIALOG, false);
				generalDialogFragment.show(((ActionBarActivity) FragmentUtil.getActivity(ConnectAccountsFragmentTab.this))
						.getSupportFragmentManager(), DIALOG_FRAGMENT_TAG_SKIP);
			}
		}
	};

	private void onContinueClick() {
		String wcitiesId = ((EventSeekr)FragmentUtil.getActivity(this).getApplication()).getWcitiesId();
		
		if (wcitiesId != null) {
			showProgress();
			double[] latLon = DeviceUtil.getLatLon(FragmentUtil.getApplication(this));

			loadMyEventsCount = new LoadMyEventsCount(Api.OAUTH_TOKEN, wcitiesId, latLon[0], latLon[1], new AsyncTaskListener<Integer>() {
				
				@Override
				public void onTaskCompleted(Integer... params) {
					Log.d(TAG, "params[0] = " + params[0]);
					if (params[0] > 0) {
						((DrawerListFragmentListener)FragmentUtil.getActivity(ConnectAccountsFragmentTab.this))
							.onDrawerItemSelected(AppConstants.INDEX_NAV_ITEM_MY_EVENTS, null);
						
					} else {
						((DrawerListFragmentListener)FragmentUtil.getActivity(ConnectAccountsFragmentTab.this))
							.onDrawerItemSelected(AppConstants.INDEX_NAV_ITEM_DISCOVER, null);
					}
				}
			});
			loadMyEventsCount.execute();
			
		} else {
			((DrawerListFragmentListener)FragmentUtil.getActivity(ConnectAccountsFragmentTab.this))
				.onDrawerItemSelected(AppConstants.INDEX_NAV_ITEM_DISCOVER, null);
		}
	}
	
	@Override
	public void doPositiveClick(String dialogTag) {
		if (dialogTag.equals(DIALOG_FRAGMENT_TAG_SKIP)) {
			onContinueClick();
		}
	}

	@Override
	public void doNegativeClick(String dialogTag) {
		if (dialogTag.equals(DIALOG_FRAGMENT_TAG_SKIP)) {
			DialogFragment dialogFragment = (DialogFragment) getChildFragmentManager().findFragmentByTag(DIALOG_FRAGMENT_TAG_SKIP);
			if (dialogFragment != null) {
				dialogFragment.dismiss();
			}
		}
	}

	@Override
	public void onSyncCountUpdated(final Service service) {
		//Log.d(TAG, "onSyncCountUpdated");
		syncInProgressCount = (syncInProgressCount > 0) ? syncInProgressCount - 1 : 0;
		FragmentUtil.getActivity(this).runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				//Log.d(TAG, "run");
				if (serviceAccounts != null) {
					//Log.d(TAG, "serviceAccounts != null");
					for (ServiceAccount serviceAccount : serviceAccounts) {
						//Log.d(TAG, "serviceAccount = " + serviceAccount.name);
						if (serviceAccount != null && service.isOf(serviceAccount.name, ConnectAccountsFragmentTab.this)) {
							//Log.d(TAG, "serviceAccount is not in progress - " + serviceAccount.name);
							serviceAccount.isInProgress = false;
							serviceAccount.count = 
								((EventSeekr)FragmentUtil.getActivity(ConnectAccountsFragmentTab.this).getApplication())
								.getSyncCount(service);
							break;
						}
					}
				}
				
				/**
				 * syncInProgressCount check added to handle case where user syncs more than 1 service
				 * one by one in quick succession & suppose if 1st service syncing is in progress, but 
				 * 2nd has finished. In this case then progress should still display.
				 */
				if (syncInProgressCount == 0) {
					for (ServiceAccount serviceAcc : serviceAccounts) {
						if (serviceAcc.name.equals(Service.Title.getStr(ConnectAccountsFragmentTab.this))) {
							serviceAcc.isInProgress = false;
						}
					}
				}
				
				if (listAdapter != null) {
					listAdapter.notifyDataSetChanged();
				}
			}
		});
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Log.d(TAG, "onActivityResult(), requestCode = " + requestCode +
		// ", resultCode = " + resultCode);
		if (requestCode == AppConstants.REQ_CODE_GOOGLE_ACCOUNT_CHOOSER_FOR_GOOGLE_MUSIC && resultCode == Activity.RESULT_OK) {
			String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
			new GetAuthToken(this, this).execute(accountName);
			
		} else if (requestCode == AppConstants.REQ_CODE_SPOTIFY && resultCode == Activity.RESULT_OK) {
			onArtistSyncStarted(false);
		}
	}

	@Override
	public void onTaskCompleted(Object... params) {
		//Log.d(TAG, "onTaskCompleted()");
		if (params[0] instanceof String) {
			//Log.d(TAG, "onTaskCompleted(), string");
			String authToken = (String) params[0];
			
			if (authToken != null && !TextUtils.isEmpty(authToken)) {
				Bundle args = new Bundle();
				args.putString(BundleKeys.AUTH_TOKEN, authToken);
				
				for (ServiceAccount serviceAccount : serviceAccounts) {
					if (serviceAccount != null && serviceAccount.name.equals(Service.GooglePlay.getStr(this))) { 
						serviceAccount.isInProgress = true;
						break;
					}
				}
				args.putString(BundleKeys.SYNC_ARTIST_LISTENER, FragmentUtil.getTag(ConnectAccountsFragmentTab.this));
				((ConnectAccountsFragmentListener)FragmentUtil.getActivity(ConnectAccountsFragmentTab.this))
            			.onServiceSelected(Service.GooglePlay, args, true);
			}
			
		} else if (params[0] instanceof Intent) {
			//Log.d(TAG, "onTaskCompleted(), intent");
			Intent intent = (Intent) params[0];
			int requestCode = (Integer) params[1];
            startActivityForResult(intent, requestCode);
		}
	}

	@Override
	public void onArtistSyncStarted(boolean doBackPress) {
		syncInProgressCount++;
		
		if (doBackPress) {
			// Without using handler back pressed call is not working, hence using handler here.
			handler.post(new Runnable() {
				
				@Override
				public void run() {
					FragmentUtil.getActivity(ConnectAccountsFragmentTab.this).onBackPressed();
				}
			});
		}
		
		//Log.d(TAG, "onArtistSyncStarted() - " + this);
		for (ServiceAccount serviceAcc : serviceAccounts) {
			//Log.d(TAG, "for - " + serviceAcc.name);
			if (serviceAcc.name.equals(Service.Title.getStr(ConnectAccountsFragmentTab.this))) {
				//Log.d(TAG, "set in progress");
				serviceAcc.isInProgress = true;
			}
		}
		listAdapter.notifyDataSetChanged();
	}

	@Override
	public String getScreenName() {
		return isFirstTimeLaunch ? ScreenNames.SYNC_ACCOUNTS : ScreenNames.SYNC_ACCOUNTS_SETTINGS;
	}
}
