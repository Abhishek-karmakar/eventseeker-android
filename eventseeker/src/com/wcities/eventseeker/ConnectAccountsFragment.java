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
import android.os.AsyncTask.Status;
import android.os.Bundle;
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
import com.wcities.eventseeker.FbLogInFragment.FbLogInFragmentListener;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.app.EventSeekr.EventSeekrListener;
import com.wcities.eventseeker.asynctask.GetAuthToken;
import com.wcities.eventseeker.asynctask.LoadMyEventsCount;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.custom.fragment.ListFragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.AsyncTaskListener;
import com.wcities.eventseeker.interfaces.ReplaceFragmentListener;
import com.wcities.eventseeker.util.DeviceUtil;
import com.wcities.eventseeker.util.FbUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.ViewUtil.AnimationUtil;

public class ConnectAccountsFragment extends ListFragmentLoadableFromBackStack implements EventSeekrListener, 
		AsyncTaskListener<Object> {
	
    private static final String TAG = ConnectAccountsFragment.class.getName();
    
    private static final String FB_LOGIN = "Facebook Log In";
    private static final String FB_LOGOUT = "Facebook Log Out";
    
    private boolean isFirstTimeLaunch;
    
    public static enum Service {
    	Title(0,"Title",R.drawable.placeholder),
    	Facebook(1,"Facebook",R.drawable.facebook_colored),
    	Blank(2,"Blank",R.drawable.placeholder),
    	GooglePlay(3,"Google Play",R.drawable.google_play),
    	DeviceLibrary(4,"Device Library",R.drawable.devicelibrary),
    	Twitter(5,"Twitter",R.drawable.twitter_colored),
    	//Spotify,
    	Rdio(6,"Rdio",R.drawable.rdio),
    	Lastfm(7,"Last.fm",R.drawable.lastfm),
    	Pandora(8,"Pandora",R.drawable.pandora),
    	Button(9,"Button",R.drawable.placeholder);
    	
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
	
	private boolean fbLoggedIn, isProgressVisible;
	
	private LinearLayout lnrLayoutProgress;

    private Session.StatusCallback statusCallback = new SessionStatusCallback();
    
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
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		//Log.d(TAG, "onActivityResult(), requestCode = " + requestCode + ", resultCode = " + resultCode);
		if (requestCode == AppConstants.REQ_CODE_GOOGLE_ACCOUNT_CHOOSER && resultCode == Activity.RESULT_OK) {
			String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
			new GetAuthToken(this, this).execute(accountName);
			
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
        		
        	if((isFirstTimeLaunch && service.equals(Service.Facebook))
        			|| (isFirstTimeLaunch && service.equals(Service.Blank))
        			|| (!isFirstTimeLaunch && service.equals(Service.Title))) {
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
        Log.d(TAG, "session state = " + session.getState().name());
        if (session.isOpened()) {
        	Log.d(TAG, "session is opened");
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
    	                	bundle.putString(BundleKeys.WCITIES_ID, user.getId());
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
				btnContinue.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
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
										((FbLogInFragmentListener)FragmentUtil.getActivity(ConnectAccountsFragment.this))
											.replaceFbLoginFragmentBy(AppConstants.FRAGMENT_TAG_MY_EVENTS);
										
									} else {
										((FbLogInFragmentListener)FragmentUtil.getActivity(ConnectAccountsFragment.this))
											.replaceFbLoginFragmentBy(AppConstants.FRAGMENT_TAG_DISCOVER);
									}
								}
							});
							loadMyEventsCount.execute();
							
						} else {
							((FbLogInFragmentListener)FragmentUtil.getActivity(ConnectAccountsFragment.this))
								.replaceFbLoginFragmentBy(AppConstants.FRAGMENT_TAG_DISCOVER);
						}
					}
				});
				
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
				
				if(!isFirstTimeLaunch && Service.Facebook.isOf(serviceAccount.name)) {
					if(fbLoggedIn) {
						holder.txtServiceName.setText(FB_LOGOUT);
		        	} else {
		        		holder.txtServiceName.setText(FB_LOGIN);
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
			
			if (service != Service.Facebook && service != Service.Blank 
					&& eventSeekr.getWcitiesId() == null) {
				
				String text = (eventSeekr.getFbUserId() == null) ? 
						"Please login with facebook before you sync accounts from other services" :
							"Syncing facebook account...Please Wait...";
				Toast.makeText(eventSeekr, text, Toast.LENGTH_LONG).show();
				return;
			}
			
			switch (service) {
			
			case Facebook:
				if (fbLoggedIn) {
					FbUtil.callFacebookLogout((EventSeekr)FragmentUtil.getActivity(ConnectAccountsFragment.this).getApplication());
					//serviceAccounts.get(0).name = FB_LOGIN;
					fbLoggedIn = false;
					listAdapter.notifyDataSetChanged();
					
				} else {
					FbUtil.onClickLogin(ConnectAccountsFragment.this, statusCallback);
				}
				break;
				
			case Blank:
				break;
				
			case Twitter:
				Log.d(TAG, "twitter");
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
                        	Log.d(TAG, "twitter run");
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
				startActivityForResult(intent, AppConstants.REQ_CODE_GOOGLE_ACCOUNT_CHOOSER);
				break;

			default:
				Bundle bundle = new Bundle();
				bundle.putSerializable(BundleKeys.SERVICE_ACCOUNTS, serviceAccount);
				((ConnectAccountsFragmentListener)FragmentUtil.getActivity(ConnectAccountsFragment.this)).onServiceSelected(service, bundle, true);
				break;
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
	
	private class SessionStatusCallback implements Session.StatusCallback {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            updateView();
        }
    }

	@Override
	public void onSyncCountUpdated(final Service service) {
		Log.d(TAG, "onSyncCountUpdated");
		FragmentUtil.getActivity(this).runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				
				Log.d(TAG, "run");
				
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
			/*@Override
			public void run() {
				Log.d(TAG, "run");
				Log.d(TAG, "service.ordinal() : " + service.ordinal());
				if (serviceAccounts != null && serviceAccounts.size() > service.ordinal()) {
					Log.d(TAG, "serviceAccounts != null ");
					serviceAccounts.get(service.ordinal()).count = ((EventSeekr)FragmentUtil.getActivity(ConnectAccountsFragment.this).getApplication())
							.getSyncCount(service);
					serviceAccounts.get(service.ordinal()).isInProgress = false;
				}
				if (listAdapter != null) {
					Log.d(TAG, "listAdapter != null ");
					listAdapter.notifyDataSetChanged();
				}
			}*/		
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
}
