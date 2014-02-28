package com.wcities.eventseeker;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
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
import com.facebook.SessionState;
import com.facebook.model.GraphUser;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.plus.PlusClient;
import com.google.android.gms.plus.model.people.Person;
import com.wcities.eventseeker.GeneralDialogFragment.DialogBtnClickListener;
import com.wcities.eventseeker.GetStartedFragment.GetStartedFragmentListener;
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
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FbUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.GPlusUtil;
import com.wcities.eventseeker.util.NetworkUtil;
import com.wcities.eventseeker.util.ViewUtil.AnimationUtil;

public class ConnectAccountsFragment extends ListFragmentLoadableFromBackStack implements EventSeekrListener, 
		AsyncTaskListener<Object>, ConnectionCallbacks, OnConnectionFailedListener, DialogBtnClickListener {
	
    private static final String TAG = ConnectAccountsFragment.class.getName();
    
    private static final String FB_SIGN_IN = "Facebook Sign In";
    private static final String FB_SIGN_OUT = "Facebook Sign Out";
    
    private static final String GOOGLE_SIGN_IN = "Google Sign In";
    private static final String GOOGLE_SIGN_OUT = "Google Sign Out";
    
    private static final String TXT_BTN_CONTINUE = "Continue";
    private static final String TXT_BTN_SKIP = "Skip";
    
	private static final String DIALOG_FRAGMENT_TAG_SKIP = "skipDialog";
	private static final String DIALOG_ALREADY_LOGGED_IN_WITH_OTHER_ACCOUNT = "alreadyLoggedInWithOtherAccount";
    
    public static enum Service {
    	Title(0,"Title",R.drawable.placeholder),
    	Facebook(1,"Facebook",R.drawable.facebook_colored),
    	GooglePlus(2,"Google Plus",R.drawable.g_plus_colored),
    	Blank(3,"Blank",R.drawable.placeholder),
    	GooglePlay(4,"Google Play",R.drawable.google_play),
    	DeviceLibrary(5,"Device Library",R.drawable.devicelibrary),
    	Twitter(6,"Twitter",R.drawable.twitter_colored),
    	//Spotify,
    	Rdio(7,"Rdio",R.drawable.rdio),
    	Lastfm(8,"Last.fm",R.drawable.lastfm),
    	Pandora(9,"Pandora",R.drawable.pandora),
    	Button(10,"Button",R.drawable.placeholder);
    	
    	private int intId;
    	private String str;
    	private int drwResId;
    	
    	private Service(int intId, String str, int drwResId) {
    		this.intId = intId;
    		this.str = str;
    		this.drwResId = drwResId;
		}
    	
    	public int getDrwResId() {
			return drwResId;
		}
    	
    	public String getStr() {
			return str;
		}
    	
    	public int getIntId() {
			return intId;
		}
    	
    	public boolean equals(Service s) {
    		return str.equals(s.getStr());
		}
    	
    	public boolean isOf(String s) {
    		return str.equals(s);
    	}
    	
    	public static int getValueOf(String s) {
    		Service[] services = Service.values();
    		for (int i = 0; i < services.length; i++) {
    			Service service = services[i];
    			if(service.isOf(s)) {
					return service.getIntId();
				}
			}
    		return -1;
    	}
    	
    }
    
	private AccountsListAdapter listAdapter;
	private List<ServiceAccount> serviceAccounts;
	private LoadMyEventsCount loadMyEventsCount;
	
	private boolean fbLoggedIn, gPlusSignedIn, isProgressVisible, isFirstTimeLaunch;
	
	private LinearLayout lnrLayoutProgress;
	
	private PlusClient mPlusClient;
	private ConnectionResult mConnectionResult;

    private Session.StatusCallback statusCallback = new SessionStatusCallback();

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
		super.onCreate(savedInstanceState);
		//Log.d(TAG, "onCreate()");
		setRetainInstance(true);
		
		EventSeekr eventSeekr = ((EventSeekr)FragmentUtil.getActivity(this).getApplication());
		eventSeekr.registerListener(this);
		
		isFirstTimeLaunch = eventSeekr.getFirstTimeLaunch();
		eventSeekr.updateFirstTimeLaunch(false);
		
		mPlusClient = GPlusUtil.createPlusClientInstance(this, this, this);
		res = getResources();
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
			
			loadServiceAccountItems();
			
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
		if (mPlusClient.isConnected()) {
			mPlusClient.disconnect();
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
        	if (resultCode == Activity.RESULT_OK  && !mPlusClient.isConnected()
                    && !mPlusClient.isConnecting()) {
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
	
	private void loadServiceAccountItems() {
		EventSeekr eventSeekr = (EventSeekr) FragmentUtil.getActivity(this).getApplication();
		//String[] connectAccountsItemTitles = getResources().getStringArray(R.array.connect_accounts_item_titles);
		//TypedArray connectAccountsItemIcons = getResources().obtainTypedArray(R.array.connect_accounts_item_icons);
		
        //for (int i = 0; i < connectAccountsItemTitles.length; i++) {
        	
        	//String title = connectAccountsItemTitles[i];
        	
		Service[] connectAccountsItemTitles = Service.values();
		
        for (int i = 0; i < connectAccountsItemTitles.length; i++) {
        		
        	Service service = connectAccountsItemTitles[i];
        		
        	if ((isFirstTimeLaunch && (service.equals(Service.Facebook)
        			|| service.equals(Service.Blank) || service.equals(Service.GooglePlus)))
        			|| (!isFirstTimeLaunch && service.equals(Service.Title))) {
        		continue;
        	}
        	
        	if (AppConstants.REMOVE_GOOGLE_PLAY_SYNC && service.equals(Service.GooglePlay)) {
        		continue;
        	}
        	
			ServiceAccount serviceAccount = new ServiceAccount();
			serviceAccount.name = service.getStr();
			serviceAccount.drawable = service.getDrwResId();
			serviceAccount.count = eventSeekr.getSyncCount(service);
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
	
	private void updateView() {
		//Log.d(TAG, "updateView()");
        final Session session = Session.getActiveSession();
        //Log.d(TAG, "session state = " + session.getState().name());
        if (session.isOpened()) {
        	//Log.d(TAG, "session is opened");
        	FbUtil.makeMeRequest(session, new Request.GraphUserCallback() {

    			@Override
    			public void onCompleted(GraphUser user, Response response) {
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
    	                	((ConnectAccountsFragmentListener)FragmentUtil.getActivity(ConnectAccountsFragment.this))
    	                		.onServiceSelected(Service.Facebook, bundle, true);
    	                }
    	            }
    	            
    	            if (response.getError() != null) {
    	                // Handle errors, will do so later.
    	            }
    			}
    	    });
        } 
    }
	
	private void connectPlusClient() {
    	//Log.d(TAG, "connectPlusClient()");
    	//Log.d(TAG, "mPlusClient.isConnected() : " + mPlusClient.isConnected());
    	//Log.d(TAG, "mPlusClient.isConnecting() : " + mPlusClient.isConnecting());
    	if (!mPlusClient.isConnected() && !mPlusClient.isConnecting()) {
    		Log.d(TAG, "try connecting");
    		mConnectionResult = null;
    		mPlusClient.connect();
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
			
			if (serviceAccount.name.equals(Service.Button.getStr())) {
				
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
				
			} else if(serviceAccount.name.equals(Service.Title.getStr())) {
				convertView = mInflater.inflate(R.layout.connect_accounts_txt_list_item, null);
				convertView.setTag("");
				
			} else if(serviceAccount.name.equals(Service.Blank.getStr())) {
				
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
					if (Service.Facebook.isOf(serviceAccount.name)) {
						if (fbLoggedIn) {
							holder.txtServiceName.setText(FB_SIGN_OUT);
							
			        	} else {
			        		holder.txtServiceName.setText(FB_SIGN_IN);
			        	}
						
					} else if (Service.GooglePlus.isOf(serviceAccount.name)) {
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
					
					if (serviceAccount.name.equals(Service.Facebook.getStr())) {
						if (fbLoggedIn) {
							holder.imgPlus.setVisibility(View.INVISIBLE);
							
						} else {
							holder.imgPlus.setVisibility(View.VISIBLE);
						}
						
					} else if (serviceAccount.name.equals(Service.GooglePlus.getStr())) {
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
			int serviceId = Service.getValueOf(serviceAccount.name);
			//Log.d(TAG, "onItemClick(), serviceId = " + serviceId);
			
			final Service service = Service.values()[serviceId];
			
			EventSeekr eventSeekr = (EventSeekr) FragmentUtil.getActivity(ConnectAccountsFragment.this).getApplication();
			
			if (service == Service.Title) {
				return;
			}
			
			if (service != Service.Facebook && service != Service.GooglePlus && service != Service.Blank 
					&& eventSeekr.getWcitiesId() == null) {
				String text = (eventSeekr.getFbUserId() == null || eventSeekr.getGPlusUserId() == null) ? 
						"Please login with facebook or google before you sync accounts from other services" :
							"Syncing your account...Please Wait...";
				Toast.makeText(eventSeekr, text, Toast.LENGTH_LONG).show();
				return;
			}
			
			switch (service) {
			
			case Facebook:
				if (gPlusSignedIn) {
					GeneralDialogFragment generalDialogFragment = GeneralDialogFragment.newInstance(
							res.getString(R.string.are_you_sure),
							res.getString(R.string.already_signed_in_with_google_account),
							"Cancel", "Ok");
					generalDialogFragment.show(getChildFragmentManager(), DIALOG_ALREADY_LOGGED_IN_WITH_OTHER_ACCOUNT);
					return;
				}
				
				if (fbLoggedIn) {
					FbUtil.callFacebookLogout((EventSeekr)FragmentUtil.getActivity(ConnectAccountsFragment.this).getApplication());
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
							res.getString(R.string.already_signed_in_with_facebook_account),
							"Cancel", "Ok");
					generalDialogFragment.show(getChildFragmentManager(), DIALOG_ALREADY_LOGGED_IN_WITH_OTHER_ACCOUNT);
					return;
				}
				
				if (gPlusSignedIn) {
					GPlusUtil.callGPlusLogout(mPlusClient, (EventSeekr)FragmentUtil.getActivity(ConnectAccountsFragment.this).getApplication());
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
				GeneralDialogFragment generalDialogFragment = GeneralDialogFragment.newInstance("Are you sure ?", 
						"Connecting accounts allows us to provide relevant alerts instantly.", "Cancel", "Skip");
				generalDialogFragment.show(getChildFragmentManager(), DIALOG_FRAGMENT_TAG_SKIP);
			}
		}
	};

	private void onContinueClick() {
		String wcitiesId = ((EventSeekr)FragmentUtil.getActivity(ConnectAccountsFragment.this)
				.getApplication()).getWcitiesId();
		
		if (wcitiesId != null) {
			showProgress();
			double[] latLon = DeviceUtil.getLatLon(FragmentUtil.getActivity(ConnectAccountsFragment.this));

			loadMyEventsCount = new LoadMyEventsCount(wcitiesId, latLon[0], latLon[1], new AsyncTaskListener<Integer>() {
				
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
	
	private class SessionStatusCallback implements Session.StatusCallback {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            updateView();
        }
    }
	
	@Override
	public void doPositiveClick(String dialogTag) {
		if (dialogTag.equals(DIALOG_ALREADY_LOGGED_IN_WITH_OTHER_ACCOUNT)) {
			if (fbLoggedIn) {
				FbUtil.callFacebookLogout(((EventSeekr) FragmentUtil.getActivity(this).getApplication()));
				listAdapter.signInWithGoogle();
			} else {
				GPlusUtil.callGPlusLogout(mPlusClient, ((EventSeekr) FragmentUtil.getActivity(this).getApplication()));
				listAdapter.signInWithFacebook();				
			}
		}
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
						
						if(serviceAccount != null && service.isOf(serviceAccount.name)) { 
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
		if (params[0] instanceof String) {
			//Log.d(TAG, "onTaskCompleted(), string");
			String authToken = (String) params[0];
			
			if (authToken != null && !TextUtils.isEmpty(authToken)) {
				Bundle args = new Bundle();
				args.putString(BundleKeys.AUTH_TOKEN, authToken);
				
				for (ServiceAccount serviceAccount : serviceAccounts) {
					if (serviceAccount != null && serviceAccount.name.equals(Service.GooglePlay.str)) { 
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
			
	        Person currentPerson = mPlusClient.getCurrentPerson();
	        
	        if (currentPerson != null) {
	        	gPlusSignedIn = true;
	        	listAdapter.notifyDataSetChanged();

	            String personId = currentPerson.getId();
	            //Log.d(TAG, "id = " + personId);
	            Bundle bundle = new Bundle();
	            bundle.putSerializable(BundleKeys.LOGIN_TYPE, LoginType.googlePlus);
	        	bundle.putString(BundleKeys.GOOGLE_PLUS_USER_ID, personId);
	        	bundle.putString(BundleKeys.GOOGLE_PLUS_USER_NAME, currentPerson.getDisplayName());
	        	bundle.putString(BundleKeys.GOOGLE_PLUS_ACCOUNT_NAME, mPlusClient.getAccountName());
	        	
	        	((ConnectAccountsFragmentListener)FragmentUtil.getActivity(ConnectAccountsFragment.this))
        				.onServiceSelected(Service.GooglePlus, bundle, true);
	        }
		}
	}

	@Override
	public void onDisconnected() {
		Log.d(TAG, "onDisconnected()");
		//isGPlusSigningIn = false;
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
}
