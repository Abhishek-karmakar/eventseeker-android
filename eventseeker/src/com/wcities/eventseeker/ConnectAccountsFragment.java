package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.List;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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
import android.widget.Toast;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;
import com.wcities.eventseeker.FbLogInFragment.FbLogInFragmentListener;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.app.EventSeekr.EventSeekrListener;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.custom.fragment.ListFragmentLoadableFromBackStack;
import com.wcities.eventseeker.util.FbUtil;
import com.wcities.eventseeker.util.FragmentUtil;

public class ConnectAccountsFragment extends ListFragmentLoadableFromBackStack implements EventSeekrListener {
	
    private static final String TAG = ConnectAccountsFragment.class.getName();
    
    private static final String FB_LOGIN = "Facebook Log In";
    private static final String FB_LOGOUT = "Facebook Log Out";
    
    private static final int CONTINUE_POS = 8;
    
    public static enum Service {
    	Facebook,
    	Blank,
    	DeviceLibrary,
    	Twitter,
    	//Spotify,
    	Rdio,
    	Lastfm,
    	Pandora;
    }
    
	private AccountsListAdapter listAdapter;
	private List<ServiceAccount> serviceAccounts;
	
	private int orientation;
	private boolean fbLoggedIn;
	
    private Session.StatusCallback statusCallback = new SessionStatusCallback();
    
    public interface ConnectAccountsFragmentListener {
    	public void onServiceSelected(Service service, Bundle args);
    }
    
    @Override
    public void onAttach(Activity activity) {
    	super.onAttach(activity);
    	if (!(activity instanceof ConnectAccountsFragmentListener)) {
    		throw new ClassCastException(activity.toString() + " must implement ConnectAccountsFragmentListener");
    	}
    	
    	Log.d(TAG, "onAttach()");
    	((EventSeekr)FragmentUtil.getActivity(this).getApplication()).registerListener(this);
    }
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate()");
		setRetainInstance(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		orientation = getResources().getConfiguration().orientation;
		
		View v = LayoutInflater.from(FragmentUtil.getActivity(this)).inflate(R.layout.fragment_connect_accounts_list, null);
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
	public void onDetach() {
		super.onDetach();
		Log.d(TAG, "onDetach()");
		((EventSeekr)FragmentUtil.getActivity(this).getApplication()).unregisterListener(this);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.d(TAG, "onActivityResult()");
		if (!fbLoggedIn) {
			Session.getActiveSession().onActivityResult(FragmentUtil.getActivity(this), requestCode, resultCode, data);
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
		String[] connectAccountsItemTitles = getResources().getStringArray(R.array.connect_accounts_item_titles);
		TypedArray connectAccountsItemIcons = getResources().obtainTypedArray(R.array.connect_accounts_item_icons);
		
        for (int i = 0; i < connectAccountsItemTitles.length; i++) {
			ServiceAccount serviceAccount = new ServiceAccount();
			serviceAccount.name = connectAccountsItemTitles[i];
			serviceAccount.drawable = connectAccountsItemIcons.getDrawable(i);
			serviceAccount.count = eventSeekr.getSyncCount(Service.values()[i]);
			serviceAccounts.add(serviceAccount);
		}
        
        // add null representing Continue button
        serviceAccounts.add(null);
        
        fbLoggedIn = FbUtil.hasUserLoggedInBefore(FragmentUtil.getActivity(this).getApplicationContext());
		if (fbLoggedIn) {
			serviceAccounts.get(0).name = FB_LOGOUT;
        	
        } else {
			serviceAccounts.get(0).name = FB_LOGIN;
        }
        connectAccountsItemIcons.recycle();
	}
	
	private void updateView() {
		Log.d(TAG, "updateView()");
        final Session session = Session.getActiveSession();
        if (session.isOpened()) {
        	Log.d(TAG, "session is opened");
        	FbUtil.makeMeRequest(session, new Request.GraphUserCallback() {

    			@Override
    			public void onCompleted(GraphUser user, Response response) {
    				// If the response is successful
    	            if (session == Session.getActiveSession()) {
    	                if (user != null) {
    	                	((EventSeekr) (FragmentUtil.getActivity(ConnectAccountsFragment.this)).getApplicationContext()).updateFbUserId(user.getId());
    	                }
    	                serviceAccounts.get(0).name = FB_LOGOUT;
    					fbLoggedIn = true;
    					listAdapter.notifyDataSetChanged();
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
			ServiceAccount serviceAccount = getItem(position);
			if (serviceAccount == null) {
				// it's for Continue button
				convertView = mInflater.inflate(R.layout.connect_accounts_continue, null);
				Button btnContinue = (Button) convertView.findViewById(R.id.btnContinue);
				btnContinue.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						((FbLogInFragmentListener)FragmentUtil.getActivity(ConnectAccountsFragment.this))
								.replaceFbLoginFragmentBy(AppConstants.FRAGMENT_TAG_DISCOVER);
					}
				});
				
			} else {
				AccountViewHolder holder;
				if (convertView == null || !(convertView.getTag() instanceof AccountViewHolder)) {
					convertView = mInflater.inflate(R.layout.connect_accounts_list_item, null);
					holder = new AccountViewHolder();
					holder.rltLayoutServiceDetails = (RelativeLayout) convertView.findViewById(R.id.rltLayoutServiceDetails);
					holder.imgService = (ImageView) convertView.findViewById(R.id.imgService);
					holder.txtServiceName = (TextView) convertView.findViewById(R.id.txtServiceName);
					holder.txtCount = (TextView) convertView.findViewById(R.id.txtCount);
					holder.imgPlus = (ImageView) convertView.findViewById(R.id.imgPlus);
					convertView.setTag(holder);
					
				} else {
					holder = (AccountViewHolder) convertView.getTag();
				}
				
				holder.imgService.setImageDrawable(serviceAccount.drawable);
				holder.txtServiceName.setText(serviceAccount.name);
				if (serviceAccount.count != EventSeekr.UNSYNC_COUNT) {
					holder.txtCount.setText(serviceAccount.count + "");
					holder.txtCount.setVisibility(View.VISIBLE);
					holder.imgPlus.setVisibility(View.GONE);
					
				} else {
					holder.txtCount.setVisibility(View.GONE);
					if (serviceAccount.name.equals(FB_LOGOUT)) {
						holder.imgPlus.setVisibility(View.GONE);
						
					} else {
						holder.imgPlus.setVisibility(View.VISIBLE);
					}
				}
				
				if (position == Service.Blank.ordinal()) {
					holder.rltLayoutServiceDetails.setVisibility(View.INVISIBLE);
					
				} else {
					holder.rltLayoutServiceDetails.setVisibility(View.VISIBLE);
				}
			}
			
			convertView.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					//Log.d(TAG, "onClick()");
					onItemClick(position);
				}
			});
			
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
		
		private void onItemClick(int pos) {
			Log.d(TAG, "onItemClick(), pos = " + pos);
			
			final Service service = Service.values()[pos];
			
			EventSeekr eventSeekr = (EventSeekr) FragmentUtil.getActivity(ConnectAccountsFragment.this).getApplication();
			if (service != Service.Facebook && service != Service.Blank 
					&& eventSeekr.getWcitiesId() == null) {
				Toast.makeText(eventSeekr, "Please login with facebook before you sync accounts from other services", Toast.LENGTH_LONG).show();
				return;
			}
			
			switch (service) {
			
			case Facebook:
				if (fbLoggedIn) {
					FbUtil.callFacebookLogout((EventSeekr)FragmentUtil.getActivity(ConnectAccountsFragment.this).getApplication());
					serviceAccounts.get(0).name = FB_LOGIN;
					fbLoggedIn = false;
					listAdapter.notifyDataSetChanged();
					
				} else {
					FbUtil.onClickLogin(ConnectAccountsFragment.this, statusCallback);
				}
				break;
				
			case Blank:
				break;
				
			case Twitter:
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
                            final RequestToken requestToken = twitter.getOAuthRequestToken(AppConstants.TWITTER_CALLBACK_URL);
                            
                            FragmentUtil.getActivity(ConnectAccountsFragment.this).runOnUiThread(new Runnable() {
								
								@Override
								public void run() {
									 Bundle args = new Bundle();
			                         args.putString(BundleKeys.URL, requestToken.getAuthenticationURL());
			                         args.putSerializable(BundleKeys.TWITTER, twitter);
			                         ((ConnectAccountsFragmentListener)FragmentUtil.getActivity(ConnectAccountsFragment.this)).onServiceSelected(service, args);
								}
							});
                           
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                thread.start();
                
				break;

			default:
				((ConnectAccountsFragmentListener)FragmentUtil.getActivity(ConnectAccountsFragment.this)).onServiceSelected(service, null);
				break;
			}
		}
		
		private class AccountViewHolder {
			private RelativeLayout rltLayoutServiceDetails;
			private ImageView imgService, imgPlus;
			private TextView txtServiceName, txtCount;
		}
	}
	
	private static class ServiceAccount {
		private Drawable drawable;
		private String name;
		private int count;
	}
	
	private class SessionStatusCallback implements Session.StatusCallback {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            updateView();
        }
    }

	@Override
	public void onSyncCountUpdated(final Service service) {
		FragmentUtil.getActivity(this).runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				if (serviceAccounts != null && serviceAccounts.size() > service.ordinal()) {
					serviceAccounts.get(service.ordinal()).count = ((EventSeekr)FragmentUtil.getActivity(ConnectAccountsFragment.this).getApplication())
							.getSyncCount(service);
				}
				if (listAdapter != null) {
					listAdapter.notifyDataSetChanged();
				}
			}
		});
	}
}
