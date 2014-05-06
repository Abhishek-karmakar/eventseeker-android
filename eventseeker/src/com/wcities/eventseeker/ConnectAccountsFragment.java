package com.wcities.eventseeker;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
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
import android.widget.Toast;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.Session.StatusCallback;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.wcities.eventseeker.GeneralDialogFragment.DialogBtnClickListener;
import com.wcities.eventseeker.GetStartedFragment.GetStartedFragmentListener;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.api.UserInfoApi;
import com.wcities.eventseeker.api.UserInfoApi.LoginType;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.app.EventSeekr.EventSeekrListener;
import com.wcities.eventseeker.asynctask.GetAuthToken;
import com.wcities.eventseeker.asynctask.LoadMyEventsCount;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.custom.fragment.ListFragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.interfaces.ConnectionFailureListener;
import com.wcities.eventseeker.jsonparser.UserInfoApiJSONParser;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FbUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.GPlusUtil;
import com.wcities.eventseeker.util.NetworkUtil;
import com.wcities.eventseeker.util.ViewUtil.AnimationUtil;

public class ConnectAccountsFragment extends ListFragmentLoadableFromBackStack implements EventSeekrListener, 
		AsyncTaskListener<Object>, ConnectionCallbacks, OnConnectionFailedListener, DialogBtnClickListener {
	
    private static final String TAG = ConnectAccountsFragment.class.getName();
    
    private String FB_SIGN_IN;
    private String FB_SIGN_OUT;
    private String GOOGLE_SIGN_IN;
    private String GOOGLE_SIGN_OUT;
    
    private static final String TXT_BTN_SKIP = "Skip";
    private String TXT_BTN_CONTINUE;
    
	private static final String DIALOG_FRAGMENT_TAG_SKIP = "skipDialog";
	private static final String DIALOG_ALREADY_LOGGED_IN_WITH_OTHER_ACCOUNT = "alreadyLoggedInWithOtherAccount";

	protected static final String TXT_BTN_CANCEL = "Cancel";

	private List<Service> listAvailableServices;
	
    public static enum Service {
    	Title(0, R.string.service_title, R.drawable.placeholder, false),
    	Facebook(1, R.string.service_facebook, R.drawable.facebook_colored, false),
    	GooglePlus(2, R.string.service_google_plus, R.drawable.g_plus_colored, false),
    	Blank(3, R.string.service_blank, R.drawable.placeholder, false),
    	GooglePlay(4, R.string.service_google_play, R.drawable.google_play, true),
    	DeviceLibrary(5, R.string.service_device_library, R.drawable.devicelibrary, true),
    	Twitter(6, R.string.service_twitter, R.drawable.twitter_colored, true),
    	//Spotify,
    	Rdio(7, R.string.service_rdio, R.drawable.rdio, true),
    	Lastfm(8, R.string.service_last_fm, R.drawable.lastfm, true),
    	Pandora(9, R.string.service_pandora, R.drawable.pandora, true),
    	Button(10, R.string.service_button, R.drawable.placeholder, false);
    	
    	private int intId;
    	private int strResId;
    	private int drwResId;
    	private boolean isService;
    	
    	private Service(int intId, int strResId, int drwResId, boolean isService) {
    		this.intId = intId;
    		this.strResId = strResId;
    		this.drwResId = drwResId;
    		this.isService = isService;
		}
    	
    	public int getDrwResId() {
			return drwResId;
		}
    	
    	public String getStr(Fragment fragment) {
			return fragment.getResources().getString(strResId);
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
			return fragment.getResources().getString(strResId);
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
	
	private boolean fbLoggedIn, gPlusSignedIn, isProgressVisible, isFirstTimeLaunch;
	
	private LinearLayout lnrLayoutProgress;
	
	private GoogleApiClient mGoogleApiClient;
	private ConnectionResult mConnectionResult;

	private Resources res;
	private boolean isPermissionDisplayed;
	
	private StatusCallback statusCallback = new SessionStatusCallback();
    
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
		
		mGoogleApiClient = GPlusUtil.createPlusClientInstance(this, this, this);
		res = getResources();
		
		FB_SIGN_IN = res.getString(R.string.fb_login);
		FB_SIGN_OUT = res.getString(R.string.fb_logout);
		GOOGLE_SIGN_IN = res.getString(R.string.google_sign_in);
		GOOGLE_SIGN_OUT = res.getString(R.string.google_sign_out);
		TXT_BTN_CONTINUE = res.getString(R.string.btn_continue);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = LayoutInflater.from(FragmentUtil.getActivity(this)).inflate(R.layout.fragment_connect_accounts, null);
		lnrLayoutProgress = (LinearLayout) v.findViewById(R.id.lnrLayoutProgress);
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
    public void onStart() {
        super.onStart();
        // In starting if user's credentials are available, then this active session will be null.
        if (!fbLoggedIn && Session.getActiveSession() != null) {
        	Session.getActiveSession().addCallback(statusCallback);
        }
    }
	
	@Override
	public void onResume() {
		super.onResume();
		/**
		 * update both values here since if user logs into 2nd account (out of facebook & google plus), then 
		 * we need to logout him from 1st account & hence update lgged in status for both the accounts & display them 
		 * accordingly.
		 */
		fbLoggedIn = FbUtil.hasUserLoggedInBefore(FragmentUtil.getActivity(this).getApplicationContext());
        gPlusSignedIn = GPlusUtil.hasUserLoggedInBefore(FragmentUtil.getActivity(this).getApplicationContext());
	}
	
	@Override
	public void onStop() {
		super.onStop();
		// In starting if user's credentials are available, then this active
		// session will be null.
		if (!fbLoggedIn && Session.getActiveSession() != null) {
			Session.getActiveSession().removeCallback(statusCallback);
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (loadMyEventsCount != null && loadMyEventsCount.getStatus() != Status.FINISHED) {
			loadMyEventsCount.cancel(true);
		}
		if (Session.getActiveSession() != null) {
			Session.getActiveSession().removeCallback(statusCallback);
		}
		((EventSeekr)FragmentUtil.getActivity(this).getApplication()).unregisterListener(this);
		if (mGoogleApiClient.isConnected()) {
			mGoogleApiClient.disconnect();
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		//Log.d(TAG, "onActivityResult(), requestCode = " + requestCode + ", resultCode = " + resultCode);
		if (requestCode == AppConstants.REQ_CODE_GOOGLE_ACCOUNT_CHOOSER_FOR_GOOGLE_MUSIC && resultCode == Activity.RESULT_OK) {
			String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
			new GetAuthToken(this, this).execute(accountName);
			
		} else if (requestCode == AppConstants.REQ_CODE_GOOGLE_PLUS_RESOLVE_ERR || 
        		requestCode == AppConstants.REQ_CODE_GET_GOOGLE_PLAY_SERVICES) {
        	if (resultCode == Activity.RESULT_OK  && !mGoogleApiClient.isConnected()
                    && !mGoogleApiClient.isConnecting()) {
	            connectPlusClient();
        	}
            
        } else {
			super.onActivityResult(requestCode, resultCode, data);
			//Log.d(TAG, "onActivityResult()");
			if (!fbLoggedIn) {
				Session.getActiveSession().onActivityResult(FragmentUtil.getActivity(this), requestCode, resultCode, data);
			}
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (!fbLoggedIn) {
			Session session = Session.getActiveSession();
			Session.saveSession(session, outState);
		}
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
		//String[] connectAccountsItemTitles = getResources().getStringArray(R.array.connect_accounts_item_titles);
		//TypedArray connectAccountsItemIcons = getResources().obtainTypedArray(R.array.connect_accounts_item_icons);
		
        //for (int i = 0; i < connectAccountsItemTitles.length; i++) {
        	
        	//String title = connectAccountsItemTitles[i];
        	
		Service[] connectAccountsItemTitles = Service.values();
		
        for (int i = 0; i < connectAccountsItemTitles.length; i++) {
        		
        	Service service = connectAccountsItemTitles[i];

        	if ((isFirstTimeLaunch && (service.equals(Service.Facebook, this)
        			|| service.equals(Service.Blank, this) || service.equals(Service.GooglePlus, this)))
        			|| (!isFirstTimeLaunch && service.equals(Service.Title, this))) {
        		continue;
        	}
        		
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
        
        fbLoggedIn = FbUtil.hasUserLoggedInBefore(FragmentUtil.getActivity(this).getApplicationContext());
        gPlusSignedIn = GPlusUtil.hasUserLoggedInBefore(FragmentUtil.getActivity(this).getApplicationContext());
        //connectAccountsItemIcons.recycle();
	}
	
	private void showProgress() {
		getListView().setVisibility(View.GONE);
    	lnrLayoutProgress.setVisibility(View.VISIBLE);
		isProgressVisible = true;
    }
	
	private void dismissProgress() {
		getListView().setVisibility(View.VISIBLE);
		lnrLayoutProgress.setVisibility(View.GONE);
		isProgressVisible = false;
	}
	
	private void updateView() {
		//Log.d(TAG, "updateView()");
        final Session session = Session.getActiveSession();
        //Log.d(TAG, "session state = " + session.getState().name());
        if (session.isOpened()) {
        	//Log.d(TAG, "session is opened");
        	if (FbUtil.hasPermission(AppConstants.PERMISSIONS_FB_LOGIN)) {
        		//Log.d(TAG, "has permission");
	        	FbUtil.makeMeRequest(session, new Request.GraphUserCallback() {
	
	    			@Override
	    			public void onCompleted(GraphUser user, Response response) {
	    				//Log.d(TAG, "onCompleted()");
	    				// If the response is successful
	    	            if (session == Session.getActiveSession()) {
	    	            	/**
	    	            	 * 2nd condition !fbLoggedIn is put due to following reason: 
	    	            	 * Sometimes this updateView() method is called twice. 
	    	            	 * For e.g.: For device not having facebook app,
	    	            	 * 1) Login at least once in the app.
	    	            	 * 2) Log out from facebook.
	    	            	 * 3) Go to my events & select going/want to for any event which will in turn ask 
	    	            	 * for facebook login. Complete this process.
	    	            	 * 4) Go to Sync Accounts screen. Select Login with Facebook. It doesn't ask for 
	    	            	 * fb credentials since session is already open but this calls updateView() twice. 
	    	            	 */
	    	                if (user != null && !fbLoggedIn) {
	    	                	//serviceAccounts.get(0).name = FB_LOGOUT;
	    	                	fbLoggedIn = true;
	    	                	listAdapter.notifyDataSetChanged();
	    	                	
	    	                	Bundle bundle = new Bundle();
	    	                	bundle.putSerializable(BundleKeys.LOGIN_TYPE, LoginType.facebook);
	    	                	bundle.putString(BundleKeys.FB_USER_ID, user.getId());
	    	    	        	bundle.putString(BundleKeys.FB_USER_NAME, user.getUsername());
	    	    	        	/**
	    	                	 * this email property requires "email" permission while opening session.
	    	                	 * Email comes null if user has not verified his primary emailId on fb account
	    	                	 */
	    	                	String email = (user.getProperty("email") == null) ? "" : user.getProperty("email").toString();
	    	                	bundle.putString(BundleKeys.FB_EMAIL_ID, email);
	    	                	((ConnectAccountsFragmentListener)FragmentUtil.getActivity(ConnectAccountsFragment.this))
	    	                		.onServiceSelected(Service.Facebook, bundle, true);
	    	                }
	    	            }
	    	            
	    	            if (response.getError() != null) {
	    	                // Handle errors, will do so later.
	    	            	//Log.e(TAG, "error = " + response.getError().getErrorMessage());
	    	            }
	    			}
	    	    });
	        	
        	} else {
        		//Log.d(TAG, "does not have permission, isPermissionDisplayed = " + isPermissionDisplayed);
        		if (!isPermissionDisplayed) {
	        		Log.d(TAG, "request email permission");
	        		FbUtil.requestEmailPermission(session, AppConstants.PERMISSIONS_FB_LOGIN, 
	        				AppConstants.REQ_CODE_FB_LOGIN_EMAIL, this);
	        		isPermissionDisplayed = true;
	        		
        		} else {
        			isPermissionDisplayed = false;
        		}
        	}
        } 
    }
	
	private void connectPlusClient() {
    	//Log.d(TAG, "connectPlusClient()");
    	//Log.d(TAG, "mPlusClient.isConnected() : " + mPlusClient.isConnected());
    	//Log.d(TAG, "mPlusClient.isConnecting() : " + mPlusClient.isConnecting());
    	if (!mGoogleApiClient.isConnected() && !mGoogleApiClient.isConnecting()) {
    		Log.d(TAG, "try connecting");
    		mConnectionResult = null;
    		mGoogleApiClient.connect();
    	}
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
				convertView = mInflater.inflate(R.layout.connect_accounts_txt_list_item, null);
				convertView.setTag("");
				
			} else if(serviceAccount.name.equals(Service.Blank.getStr(ConnectAccountsFragment.this))) {
				
				convertView = mInflater.inflate(R.layout.connect_accounts_list_item, null);
				convertView.findViewById(R.id.rltLayoutServiceDetails).setVisibility(View.INVISIBLE);
				convertView.setTag("");
				
			} else {
				//Log.d(TAG, "setting Title : " + serviceAccount.name);
				AccountViewHolder holder;
				if (convertView == null || !(convertView.getTag() instanceof AccountViewHolder)) {
					convertView = mInflater.inflate(R.layout.connect_accounts_list_item, null);
					holder = new AccountViewHolder();
					//holder.rltLayoutServiceDetails = (RelativeLayout) convertView.findViewById(R.id.rltLayoutServiceDetails);
					holder.imgService = (ImageView) convertView.findViewById(R.id.imgService);
					holder.txtServiceName = (TextView) convertView.findViewById(R.id.txtServiceName);
					holder.txtCount = (TextView) convertView.findViewById(R.id.txtCount);
					holder.imgPlus = (ImageView) convertView.findViewById(R.id.imgPlus);
					holder.imgProgressBar = (ImageView) convertView.findViewById(R.id.progressBar);
					convertView.setTag(holder);
					
				} else {
					holder = (AccountViewHolder) convertView.getTag();
				}
				
				holder.imgService.setImageResource(serviceAccount.drawable);
				
				if (!isFirstTimeLaunch) { 
					if (Service.Facebook.isOf(serviceAccount.name, ConnectAccountsFragment.this)) {
						if (fbLoggedIn) {
							holder.txtServiceName.setText(FB_SIGN_OUT);
							
			        	} else {
			        		holder.txtServiceName.setText(FB_SIGN_IN);
			        	}
						
					} else if (Service.GooglePlus.isOf(serviceAccount.name, ConnectAccountsFragment.this)) {
						if (gPlusSignedIn) {
							holder.txtServiceName.setText(GOOGLE_SIGN_OUT);
							
			        	} else {
			        		holder.txtServiceName.setText(GOOGLE_SIGN_IN);
			        	}
						
					} else {
						holder.txtServiceName.setText(serviceAccount.name);
					}
					
				} else {
					holder.txtServiceName.setText(serviceAccount.name);
				}
				
				if (serviceAccount.isInProgress) {
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
					holder.imgProgressBar.setVisibility(View.INVISIBLE);
					AnimationUtil.stopRotationToView(holder.imgProgressBar);
					
					if (serviceAccount.name.equals(Service.Facebook.getStr(ConnectAccountsFragment.this))) {
						if (fbLoggedIn) {
							holder.imgPlus.setVisibility(View.INVISIBLE);
							
						} else {
							holder.imgPlus.setVisibility(View.VISIBLE);
						}
						
					} else if (serviceAccount.name.equals(Service.GooglePlus.getStr(ConnectAccountsFragment.this))) {
						if (gPlusSignedIn) {
							holder.imgPlus.setVisibility(View.INVISIBLE);
							
						} else {
							holder.imgPlus.setVisibility(View.VISIBLE);
						}
						
					} else {
						holder.imgPlus.setVisibility(View.VISIBLE);
					}
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
			
			EventSeekr eventSeekr = (EventSeekr) FragmentUtil.getActivity(ConnectAccountsFragment.this).getApplication();
			
			if (service == Service.Title) {
				return;
			}
			
			if (service != Service.Facebook && service != Service.GooglePlus && service != Service.Blank 
					&& eventSeekr.getWcitiesId() == null) {
				String text = (eventSeekr.getFbUserId() == null && eventSeekr.getGPlusUserId() == null) ? 
						res.getString(R.string.pls_login) : res.getString(R.string.syncing_your_acc);
				Toast.makeText(eventSeekr, text, Toast.LENGTH_LONG).show();
				return;
			}
			
			switch (service) {
			
			case Facebook:
				if (gPlusSignedIn) {
					GeneralDialogFragment generalDialogFragment = GeneralDialogFragment.newInstance(
							res.getString(R.string.are_you_sure),
							/*res.getString(R.string.already_signed_in_with_google_account),*/
							/**
							 * replace this string in above string id
							 */
							"You are already signed in with Google account. Would you like to sign out from Google and sign in with Facebook account?",
							TXT_BTN_CANCEL, res.getString(R.string.ok));
					generalDialogFragment.show(getChildFragmentManager(), DIALOG_ALREADY_LOGGED_IN_WITH_OTHER_ACCOUNT);
					return;
				}
				
				if (fbLoggedIn) {
					logoutFromFb();
					//serviceAccounts.get(0).name = FB_LOGIN;
					fbLoggedIn = false;
					listAdapter.notifyDataSetChanged();
					
				} else {
					//isGPlusSignInClicked = false;
					signInWithFacebook();
				}
				break;
				
			case GooglePlus:
				//Log.d(TAG, "onClick() - google plus, gPlusSignedIn = " + gPlusSignedIn);
				if (fbLoggedIn) {
					GeneralDialogFragment generalDialogFragment = GeneralDialogFragment.newInstance(
							res.getString(R.string.are_you_sure),
							/*res.getString(R.string.already_signed_in_with_facebook_account),*/
							/**
							 * replace this string in above string id
							 */
							"You are already signed in with Facebook account. Would you like to sign out from Facebook and sign in with Google account?",
							TXT_BTN_CANCEL, res.getString(R.string.ok));
					generalDialogFragment.show(getChildFragmentManager(), DIALOG_ALREADY_LOGGED_IN_WITH_OTHER_ACCOUNT);
					return;
				}
				
				if (gPlusSignedIn) {
					GPlusUtil.callGPlusLogout(mGoogleApiClient, (EventSeekr)FragmentUtil.getActivity(ConnectAccountsFragment.this).getApplication());
					gPlusSignedIn = false;
					listAdapter.notifyDataSetChanged();
					
				} else {
					//isGPlusSignInClicked = true;
	              signInWithGoogle();
				}
				break;
				
			case Blank:
				break;
				
			case Twitter:
				//Log.d(TAG, "twitter");
				ConfigurationBuilder builder = new ConfigurationBuilder();
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
                thread.start();
                
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
		
		private void signInWithFacebook() {
			ConnectionFailureListener connectionFailureListener = 
					((ConnectionFailureListener) FragmentUtil.getActivity(ConnectAccountsFragment.this));
			if (!NetworkUtil.getConnectivityStatus((Context) connectionFailureListener)) {
				connectionFailureListener.onConnectionFailure();
				return;
			}
			FbUtil.onClickLogin(ConnectAccountsFragment.this, statusCallback);			
		}

		protected void signInWithGoogle() {
			int available = GooglePlayServicesUtil.isGooglePlayServicesAvailable(
					FragmentUtil.getActivity(ConnectAccountsFragment.this));
			Log.d(TAG, "available : " + available);
			if (available != ConnectionResult.SUCCESS) {
				GPlusUtil.showDialogForGPlayServiceUnavailability(available, ConnectAccountsFragment.this);
              return;
			}
			
			Log.d(TAG, "mConnectionResult : " + mConnectionResult);
			//Log.d(TAG, "mConnectionResult = " + mConnectionResult);
			// if previously onConnectionFailed() has returned some result, resolve it
			if (mConnectionResult != null) {
	        	//Log.d(TAG, "mConnectionResult is not null");
	            try {
	                mConnectionResult.startResolutionForResult(FragmentUtil.getActivity(
	                		ConnectAccountsFragment.this), AppConstants.REQ_CODE_GOOGLE_PLUS_RESOLVE_ERR);
	                
	            } catch (SendIntentException e) {
	                // Try connecting again.
	                connectPlusClient();
	            }
	            
	        } else {
	        	connectPlusClient();
	        }			
		}

		private class AccountViewHolder {
			private RelativeLayout rltLayoutServiceDetails;
			private ImageView imgService, imgPlus, imgProgressBar;
			private TextView txtServiceName, txtCount;
		}
	}
	
	private void logoutFromFb() {
		FbUtil.callFacebookLogout((EventSeekr)FragmentUtil.getActivity(ConnectAccountsFragment.this).getApplication());
		/**
		 * reset isPermissionDisplayed flag; otherwise if user logs in with facebook, logs out & again
		 * logs in then this doesn't work since after opening session from updateView(), it doesn't have
		 * required permission & even isPermissionDisplayed flag is true due to which it doesn't
		 * take any further steps to request permission.
		 */
		isPermissionDisplayed = false;
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
						res.getString(R.string.are_you_sure), res.getString(R.string.connecting_account_allow_us), 
						TXT_BTN_CANCEL, TXT_BTN_SKIP);
				generalDialogFragment.show(getChildFragmentManager(), DIALOG_FRAGMENT_TAG_SKIP);
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
						((GetStartedFragmentListener)FragmentUtil.getActivity(ConnectAccountsFragment.this))
							.replaceGetStartedFragmentBy(AppConstants.FRAGMENT_TAG_MY_EVENTS);
						
					} else {
						((GetStartedFragmentListener)FragmentUtil.getActivity(ConnectAccountsFragment.this))
							.replaceGetStartedFragmentBy(AppConstants.FRAGMENT_TAG_DISCOVER);
					}
				}
			});
			loadMyEventsCount.execute();
			
		} else {
			((GetStartedFragmentListener)FragmentUtil.getActivity(ConnectAccountsFragment.this))
				.replaceGetStartedFragmentBy(AppConstants.FRAGMENT_TAG_DISCOVER);
		}
	}
	
	@Override
	public void doPositiveClick(String dialogTag) {
		if (dialogTag.equals(DIALOG_ALREADY_LOGGED_IN_WITH_OTHER_ACCOUNT)) {
			if (fbLoggedIn) {
				logoutFromFb();
				fbLoggedIn = false;
				listAdapter.signInWithGoogle();
				
			} else {
				GPlusUtil.callGPlusLogout(mGoogleApiClient, ((EventSeekr) FragmentUtil.getActivity(this).getApplication()));
				gPlusSignedIn = false;
				listAdapter.signInWithFacebook();				
			}
			listAdapter.notifyDataSetChanged();
			
		} else if (dialogTag.equals(DIALOG_FRAGMENT_TAG_SKIP)) {
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
	public void onConnected(Bundle arg0) {
		//Log.d(TAG, "onConnected(), signedIn = " + gPlusSignedIn);
		if (!gPlusSignedIn) {
			//isGPlusSigningIn = false;
			
	        Person currentPerson = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
	        
	        if (currentPerson != null) {
	        	gPlusSignedIn = true;
	        	listAdapter.notifyDataSetChanged();

	            String personId = currentPerson.getId();
	            //Log.d(TAG, "id = " + personId);
	            Bundle bundle = new Bundle();
	            bundle.putSerializable(BundleKeys.LOGIN_TYPE, LoginType.googlePlus);
	        	bundle.putString(BundleKeys.GOOGLE_PLUS_USER_ID, personId);
	        	bundle.putString(BundleKeys.GOOGLE_PLUS_USER_NAME, currentPerson.getDisplayName());
	        	bundle.putString(BundleKeys.GOOGLE_PLUS_EMAIL_ID, Plus.AccountApi.getAccountName(mGoogleApiClient));
	        	
	        	((ConnectAccountsFragmentListener)FragmentUtil.getActivity(ConnectAccountsFragment.this))
        				.onServiceSelected(Service.GooglePlus, bundle, true);
	        }
		}
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		Log.d(TAG, "onConnectionFailed()");
		// Save the result and resolve the connection failure upon a user click.
		mConnectionResult = result;
		//isGPlusSigningIn = false;
		if (mConnectionResult.hasResolution()) {
            try {
				mConnectionResult.startResolutionForResult(FragmentUtil.getActivity(this), AppConstants.REQ_CODE_GOOGLE_PLUS_RESOLVE_ERR);
				
			} catch (SendIntentException e) {
				e.printStackTrace();
				// Try connecting again.
                connectPlusClient();
			}
		}
	}
	
	private class SessionStatusCallback implements Session.StatusCallback {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
        	Log.d(TAG, "call() - state = " + state.name());
            updateView();
        }
    }

	@Override
	public void onConnectionSuspended(int cause) {
		Log.d(TAG, "onConnectionSuspended()");
	}

	@Override
	public String getScreenName() {
		return isFirstTimeLaunch ? "Account Connect Screen" : "Settings Screen";
	}
}
