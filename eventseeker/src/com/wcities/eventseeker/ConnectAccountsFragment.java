package com.wcities.eventseeker;

import java.io.IOException;
import java.io.Serializable;
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
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.wcities.eventseeker.DrawerListFragment.DrawerListFragmentListener;
import com.wcities.eventseeker.GeneralDialogFragment.DialogBtnClickListener;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.app.EventSeekr.EventSeekrListener;
import com.wcities.eventseeker.asynctask.GetAuthToken;
import com.wcities.eventseeker.asynctask.LoadMyEventsCount;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.custom.fragment.ListFragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class ConnectAccountsFragment extends ListFragmentLoadableFromBackStack implements EventSeekrListener, 
		AsyncTaskListener<Object>, DialogBtnClickListener {
	
    private static final String TAG = ConnectAccountsFragment.class.getName();
    
    private static final String TXT_BTN_SKIP_DIALOG = "Skip";
    protected static final String TXT_BTN_CANCEL_DIALOG = "Cancel";
    private static String TXT_BTN_SKIP = "SKIP";
    private String TXT_BTN_CONTINUE;
    
    private static final String DIALOG_FRAGMENT_TAG_SKIP = "skipDialog";    

	private List<Service> listAvailableServices;
	
    public static enum Service {
    	Title(0, R.string.service_title, R.drawable.placeholder, false, null),
    	GooglePlay(1, R.string.service_google_play, R.drawable.slctr_btn_google_play, true, "googleplay"),
    	DeviceLibrary(2, R.string.service_device_library, R.drawable.slctr_btn_device_library, true, "devicelibrary"),
    	Twitter(3, R.string.service_twitter, R.drawable.slctr_btn_twitter, true, "twitter"),
    	//Spotify,
    	Rdio(4, R.string.service_rdio, R.drawable.slctr_btn_rdio, true, "rdio"),
    	Lastfm(5, R.string.service_last_fm, R.drawable.slctr_btn_lastfm, true, "lastfm"),
    	Pandora(6, R.string.service_pandora, R.drawable.slctr_btn_pandora, true, "pandora"),
    	Button(7, R.string.service_button, R.drawable.placeholder, false, null);
    	
    	private int intId;
    	private int strResId;
    	private int drwResId;
    	private boolean isService;
    	private String artistSource;
    	
    	private Service(int intId, int strResId, int drwResId, boolean isService, String artistSource) {
    		this.intId = intId;
    		this.strResId = strResId;
    		this.drwResId = drwResId;
    		this.isService = isService;
    		this.artistSource = artistSource;
		}
    	
    	public int getDrwResId() {
			return drwResId;
		}
    	
    	public String getStr(Fragment fragment) {
			return fragment.getResources().getString(strResId);
		}
    	
    	public String getArtistSource() {
    		return artistSource;
    	}
    	
    	public int getIntId() {
			return intId;
		}
    	
    	public boolean equals(Service s, Fragment fragment) {
    		return getStringFromResId(strResId, fragment).equals(s.getStr(fragment));
		}
    	
    	public boolean isOf(String s, Fragment fragment) {
    		return getStringFromResId(strResId, fragment).equals(s);
    	}
    	
    	private String getStringFromResId(int strResId, Fragment fragment) {
			return FragmentUtil.getResources(fragment).getString(strResId);
		}
    	
    	public static int getValueOf(String s, Fragment fragment) {
    		Service[] services = Service.values();
    		for (int i = 0; i < services.length; i++) {
    			Service service = services[i];
    			if(service.isOf(s, fragment)) {
					return service.getIntId();
				}
			}
    		return -1;
    	}
    	
    	public boolean isService() {
			return isService;
		}
    }
    
	private AccountsListAdapter listAdapter;
	private List<ServiceAccount> serviceAccounts;
	private LoadMyEventsCount loadMyEventsCount;
	
	private boolean isProgressVisible, isFirstTimeLaunch;
	
	private RelativeLayout rltLayoutProgress;
	
	private Resources res;
	
    public interface ConnectAccountsFragmentListener {
    	public void onServiceSelected(Service service, Bundle args, boolean addToBackStack);
    }
    
    @Override
    public void onAttach(Activity activity) {
    	super.onAttach(activity);
    	if (!(activity instanceof ConnectAccountsFragmentListener)) {
    		throw new ClassCastException(activity.toString() + " must implement ConnectAccountsFragmentListener");
    	}
    	
    	//Log.d(TAG, "onAttach()");
    }
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		/**
		 * need to get isFirstTimeLaunch value before calling super.onCreate() since we need it to 
		 * decide on screenName to be sent to google analytics 
		 */
		EventSeekr eventSeekr = ((EventSeekr)FragmentUtil.getActivity(this).getApplication());
		isFirstTimeLaunch = eventSeekr.getFirstTimeLaunch();
		
		super.onCreate(savedInstanceState);
		//Log.d(TAG, "onCreate()");
		setRetainInstance(true);
		
		eventSeekr.registerListener(this);
		eventSeekr.updateFirstTimeLaunch(false);
		
		res = getResources();
		
		TXT_BTN_CONTINUE = res.getString(R.string.btn_continue);
		TXT_BTN_SKIP = res.getString(R.string.skip);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
			
			// following if condition in turn starts asynctask to generate wcitiesId if found null
			/*if (((EventSeekr)FragmentUtil.getActivity(this).getApplication()).getWcitiesId(this) != null) {
				loadAvailableService();
				
			} else {
				showProgress();
			}*/
			loadAvailableService();
			//loadServiceAccountItems();
			
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
	public void onDestroy() {
		super.onDestroy();
		if (loadMyEventsCount != null && loadMyEventsCount.getStatus() != Status.FINISHED) {
			loadMyEventsCount.cancel(true);
		}
		((EventSeekr)FragmentUtil.getActivity(this).getApplication()).unregisterListener(this);
	}
	
	private void loadAvailableService() {
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
					listAdapter.notifyDataSetChanged();
					
					dismissProgress();
				}
			}
		};
		AsyncTaskUtil.executeAsyncTask(loadAvailableService, true);
	}
	
	private class LoadAvailableService extends AsyncTask<Void, Void, List<Service>> {

		@Override
		protected List<Service> doInBackground(Void... params) {
			List<Service> list = new ArrayList<ConnectAccountsFragment.Service>();
			try {
				UserInfoApi userInfoApi = new UserInfoApi(Api.OAUTH_TOKEN);
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

        	/*if (!isFirstTimeLaunch && service.equals(Service.Title, this)) {
        		continue;
        	}*/
        		
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
			serviceAccount.drawable = service.getDrwResId();
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

	    public AccountsListAdapter(Context context) {
	        mInflater = LayoutInflater.from(context);
	    }
	    
	    public void setmInflater(Context context) {
	        mInflater = LayoutInflater.from(context);
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			//Log.d(TAG, "getView(), pos = " + position);
			
			final ServiceAccount serviceAccount = getItem(position);
			
			if (serviceAccount.name.equals(Service.Button.getStr(ConnectAccountsFragment.this))) {
				
				// it's for Continue button
				convertView = mInflater.inflate(R.layout.connect_accounts_continue, null);
				Button btnContinue = (Button) convertView.findViewById(R.id.btnContinue);
				if (((EventSeekr)FragmentUtil.getActivity(ConnectAccountsFragment.this).getApplication())
						.isAnyAccountSynced()) {
					btnContinue.setText(TXT_BTN_CONTINUE);
					
				} else {
					btnContinue.setText(TXT_BTN_SKIP);
				}
				btnContinue.setOnClickListener(onBtnContinueClickListener);
				
			} else if(serviceAccount.name.equals(Service.Title.getStr(ConnectAccountsFragment.this))) {
				/*convertView = mInflater.inflate(R.layout.connect_accounts_txt_list_item, null);*/
				convertView = mInflater.inflate(R.layout.connect_accounts_list_item_top, null);
				convertView.setTag("");
				((TextView)convertView.findViewById(R.id.txtSyncCount)).setText(
						((EventSeekr) FragmentUtil.getActivity(ConnectAccountsFragment.this)
								.getApplication()).getTotalSyncCount() + "");
				
			} else {
				//Log.d(TAG, "setting Title : " + serviceAccount.name);
				AccountViewHolder holder;
				if (convertView == null || !(convertView.getTag() instanceof AccountViewHolder)) {
					convertView = mInflater.inflate(R.layout.connect_accounts_list_item, null);
					holder = new AccountViewHolder();
					//holder.rltLayoutServiceDetails = (RelativeLayout) convertView.findViewById(R.id.rltLayoutServiceDetails);
					holder.imgService = (ImageView) convertView.findViewById(R.id.imgService);
					holder.txtServiceName = (TextView) convertView.findViewById(R.id.txtServiceName);
					/*holder.txtCount = (TextView) convertView.findViewById(R.id.txtCount);
					holder.imgPlus = (ImageView) convertView.findViewById(R.id.imgPlus);
					holder.imgProgressBar = (ImageView) convertView.findViewById(R.id.progressBar);*/
					holder.imgCorrect = (ImageView) convertView.findViewById(R.id.imgCorrect);
					convertView.setTag(holder);
					
				} else {
					holder = (AccountViewHolder) convertView.getTag();
				}
				
				//holder.imgService.setImageResource(serviceAccount.drawable);
				holder.imgService.setBackgroundResource(serviceAccount.drawable);
				holder.txtServiceName.setText(serviceAccount.name);
				
				/*if (serviceAccount.isInProgress) {
					holder.imgProgressBar.setVisibility(View.VISIBLE);
					holder.imgPlus.setVisibility(View.INVISIBLE);
					holder.txtCount.setVisibility(View.INVISIBLE);
					AnimationUtil.startRotationToView(holder.imgProgressBar, 0f, 360f, 0.5f, 0.5f, 1000);
					
				} else if (serviceAccount.count != EventSeekr.UNSYNC_COUNT) {
					holder.txtCount.setText(serviceAccount.count + "");
					holder.txtCount.setVisibility(View.VISIBLE);
					holder.imgPlus.setVisibility(View.INVISIBLE);
					holder.imgProgressBar.setVisibility(View.INVISIBLE);
					AnimationUtil.stopRotationToView(holder.imgProgressBar);
					
				} else {
					holder.txtCount.setVisibility(View.INVISIBLE);
					holder.imgPlus.setVisibility(View.VISIBLE);
					holder.imgProgressBar.setVisibility(View.INVISIBLE);
					AnimationUtil.stopRotationToView(holder.imgProgressBar);
				}*/
			
				if (serviceAccount.count != EventSeekr.UNSYNC_COUNT) {
					holder.imgCorrect.setVisibility(View.VISIBLE);
				
				} else {
					holder.imgCorrect.setVisibility(View.INVISIBLE);					
				}
				
				convertView.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						onItemClick(serviceAccount);
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
			int serviceId = Service.getValueOf(serviceAccount.name, ConnectAccountsFragment.this);
			//Log.d(TAG, "onItemClick(), serviceId = " + serviceId);
			
			final Service service = Service.values()[serviceId];
			
			if (service == Service.Title) {
				return;
			}
			
			switch (service) {

			case Twitter:
				//Log.d(TAG, "twitter");
				/*ConfigurationBuilder builder = new ConfigurationBuilder();
	            builder.setOAuthConsumerKey(AppConstants.TWITTER_CONSUMER_KEY);
	            builder.setOAuthConsumerSecret(AppConstants.TWITTER_CONSUMER_SECRET);
	            twitter4j.conf.Configuration configuration = builder.build();

	            TwitterFactory factory = new TwitterFactory(configuration);
	            final Twitter twitter = factory.getInstance();

                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                        	//Log.d(TAG, "twitter run");
                            final RequestToken requestToken = twitter.getOAuthRequestToken(AppConstants.TWITTER_CALLBACK_URL);
                            
                            FragmentUtil.getActivity(ConnectAccountsFragment.this).runOnUiThread(new Runnable() {
								
								@Override
								public void run() {
									 Bundle args = new Bundle();
			                         args.putString(BundleKeys.URL, requestToken.getAuthenticationURL());
			                         args.putSerializable(BundleKeys.TWITTER, twitter);
			                         args.putSerializable(BundleKeys.SERVICE_ACCOUNTS, serviceAccount);
			                         ((ConnectAccountsFragmentListener)FragmentUtil.getActivity(ConnectAccountsFragment.this)).onServiceSelected(service, args, true);
								}
							});
                           
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                thread.start();*/
                
				Bundle args = new Bundle();
                args.putSerializable(BundleKeys.SERVICE_ACCOUNTS, serviceAccount);
				((ConnectAccountsFragmentListener)FragmentUtil.getActivity(ConnectAccountsFragment.this))
						.onServiceSelected(service, args, true);
				break;
				
			case GooglePlay:
				Intent intent = AccountPicker.newChooseAccountIntent(null, null, new String[] {GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE},
				         true, null, null, null, null);
				startActivityForResult(intent, AppConstants.REQ_CODE_GOOGLE_ACCOUNT_CHOOSER_FOR_GOOGLE_MUSIC);
				break;

			default:
				Bundle bundle = new Bundle();
				bundle.putSerializable(BundleKeys.SERVICE_ACCOUNTS, serviceAccount);
				((ConnectAccountsFragmentListener)FragmentUtil.getActivity(ConnectAccountsFragment.this)).onServiceSelected(service, bundle, true);
				break;
			}
		}
		
		private class AccountViewHolder {
			private ImageView imgService, imgCorrect/*imgPlus, imgProgressBar*/;
			private TextView txtServiceName/*, txtCount*/;
		}
	}
	
	public static class ServiceAccount implements Serializable {
		private int drawable;
		private String name;
		private int count;
		public boolean isInProgress;
	}
	
	private OnClickListener onBtnContinueClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			if (((Button)v).getText().equals(TXT_BTN_CONTINUE)) {
				onContinueClick();
				
			} else {
				GeneralDialogFragment generalDialogFragment = GeneralDialogFragment.newInstance(
						ConnectAccountsFragment.this,
						res.getString(R.string.are_you_sure), res.getString(R.string.connecting_account_allow_us), 
						TXT_BTN_CANCEL_DIALOG, TXT_BTN_SKIP_DIALOG, false);
				generalDialogFragment.show(((ActionBarActivity) FragmentUtil.getActivity(ConnectAccountsFragment.this))
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
						((DrawerListFragmentListener)FragmentUtil.getActivity(ConnectAccountsFragment.this))
							.onDrawerItemSelected(MainActivity.INDEX_NAV_ITEM_MY_EVENTS, null);
						
					} else {
						((DrawerListFragmentListener)FragmentUtil.getActivity(ConnectAccountsFragment.this))
							.onDrawerItemSelected(MainActivity.INDEX_NAV_ITEM_DISCOVER, null);
					}
				}
			});
			loadMyEventsCount.execute();
			
		} else {
			((DrawerListFragmentListener)FragmentUtil.getActivity(ConnectAccountsFragment.this))
				.onDrawerItemSelected(MainActivity.INDEX_NAV_ITEM_DISCOVER, null);
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
		Log.d(TAG, "onSyncCountUpdated");
		FragmentUtil.getActivity(this).runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				
				//Log.d(TAG, "run");
				
				if (serviceAccounts != null) {
				
					for (ServiceAccount serviceAccount : serviceAccounts) {
						
						if(serviceAccount != null && service.isOf(serviceAccount.name, ConnectAccountsFragment.this)) { 
							serviceAccount.isInProgress = false;
							serviceAccount.count = 
								((EventSeekr)FragmentUtil.getActivity(ConnectAccountsFragment.this).getApplication())
								.getSyncCount(service);
							break;
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

		} 
	}

	@Override
	public void onTaskCompleted(Object... params) {
		//Log.d(TAG, "onTaskCompleted()");
		if (params.length == 0) {
			loadAvailableService();
			
		} else if (params[0] instanceof String) {
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
				
				((ConnectAccountsFragmentListener)FragmentUtil.getActivity(ConnectAccountsFragment.this))
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
	public String getScreenName() {
		return isFirstTimeLaunch ? "Account Connect Screen" : "Settings Screen";
	}
}
