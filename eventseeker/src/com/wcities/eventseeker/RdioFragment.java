package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.rdio.android.api.Rdio;
import com.rdio.android.api.RdioApiCallback;
import com.rdio.android.api.RdioListener;
import com.wcities.eventseeker.ConnectAccountsFragment.Service;
import com.wcities.eventseeker.ConnectAccountsFragment.ServiceAccount;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.SyncArtists;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.OnFragmentAliveListener;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.ViewUtil.AnimationUtil;

public class RdioFragment extends FragmentLoadableFromBackStack implements OnClickListener, RdioListener, OnFragmentAliveListener {

	private static final String TAG = RdioFragment.class.getName();
	
	private static final String PREF_ACCESSTOKEN = "prefs.accesstoken";
    private static final String PREF_ACCESSTOKENSECRET = "prefs.accesstokensecret";
    
    private static final int SEARCH_LIMIT = 250;

    private String accessToken = null;
    private String accessTokenSecret = null;
    
	//private ProgressBar progressBar;
    private ImageView imgProgressBar, imgAccount;
	private TextView txtLoading;
	private EditText edtUserCredential;
	private Button btnRetrieveArtists, btnConnectOtherAccounts;
	
	private View rltMainView, rltSyncAccount;
	
	private boolean isLoading;

	private ServiceAccount serviceAccount;

	private boolean isAlive;
	
	private static Rdio rdio;
	
	private Resources res;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/**
		 * Since there is no way to update activity reference associated with Rdio Api object, we need to create 
		 * new Rdio Api object each time the orientation changes. And if we do 'setRetainInstance(true)' then it will 
		 * try to create 2 instances which is not allowed by Rdio Api. So, we won't do 'setRetainInstance(true)'.
		 */
		//setRetainInstance(true);
		serviceAccount = (ServiceAccount) getArguments().getSerializable(BundleKeys.SERVICE_ACCOUNTS);
		isAlive = true;
		//Log.d(TAG, "onCreate : SerciveAccount" + serviceAccount);

		/**
		 * this is because when orientation got change, before that syncing might be in progress, so the value of
		 * 'serviceAccount.isInProgress' will be true. But now if user doesn't sync again and he goes back then then
		 * there the status would be syncing(arrow will rotate). 
		 */
		serviceAccount.isInProgress = false;
		//Log.d(TAG, "Setting in progress false");
		
		//Log.d(TAG, "rdio : " + rdio);
		
		if (rdio == null) {
            SharedPreferences settings = FragmentUtil.getActivity(this).getPreferences(Context.MODE_PRIVATE);
            accessToken = settings.getString(PREF_ACCESSTOKEN, null);
            accessTokenSecret = settings.getString(PREF_ACCESSTOKENSECRET, null);

            if (accessToken == null || accessTokenSecret == null) {
                // If either one is null, reset both of them
                accessToken = accessTokenSecret = null;
                
            } else {
                Log.d(TAG, "Found cached credentials:");
                Log.d(TAG, "Access token: " + accessToken);
                Log.d(TAG, "Access token secret: " + accessTokenSecret);
            }

            // Initialize our API object
            rdio = new Rdio(AppConstants.RDIO_KEY, AppConstants.RDIO_SECRET, accessToken, accessTokenSecret,
					FragmentUtil.getActivity(this), this);     
            
            res = getResources();
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_service_enter_credentials_layout, null);
		
		rltMainView = v.findViewById(R.id.rltMainView);
		rltSyncAccount = v.findViewById(R.id.rltSyncAccount);
		
		edtUserCredential = (EditText) v.findViewById(R.id.edtUserCredential);
		btnRetrieveArtists = (Button) v.findViewById(R.id.btnRetrieveArtists);

		imgProgressBar = (ImageView) v.findViewById(R.id.progressBar);
		imgAccount = (ImageView) v.findViewById(R.id.imgAccount);
		txtLoading = (TextView) v.findViewById(R.id.txtLoading);
		btnConnectOtherAccounts = (Button) v.findViewById(R.id.btnConnectOtherAccuonts);
		
		updateVisibility();
		
		btnRetrieveArtists.setOnClickListener(this);
		btnConnectOtherAccounts.setOnClickListener(this);
		
		edtUserCredential.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(final TextView v, final int actionId, final KeyEvent event) {
				if (event == null || event.getAction() == KeyEvent.ACTION_DOWN || actionId == EditorInfo.IME_NULL) {
					searchUserId(v.getText().toString().trim());
					return true;
				}
				return false;
			}
		});
		edtUserCredential.setOnClickListener(this);
		
		return v;
	}
	
	@Override
	public void onDestroy() {
		//Log.d(TAG, "Cleaning up..");
		// Make sure to call the cleanup method on the API object
		isAlive = false;
		if (rdio != null) {
			rdio.cleanup();
		}
		super.onDestroy();
	}
	
	private void updateVisibility() {
		//Log.d(TAG, "updateVisibility");
		int visibilityDesc = isLoading ? View.GONE : View.VISIBLE;
		/*edtUserCredential.setVisibility(visibilityDesc);
		btnRetrieveArtists.setVisibility(visibilityDesc);*/
		rltMainView.setVisibility(visibilityDesc);
		
		int visibilityLoading = !isLoading ? View.GONE : View.VISIBLE;
		/*imgProgressBar.setVisibility(visibilityLoading);
		txtLoading.setVisibility(visibilityLoading);*/
		rltSyncAccount.setVisibility(visibilityLoading);
		if(isLoading) {
			AnimationUtil.startRotationToView(imgProgressBar, 0f, 360f, 0.5f, 0.5f, 1000);
			txtLoading.setText(R.string.syncing_rdio);
			imgAccount.setImageResource(R.drawable.rdio_big);
		} else {
			AnimationUtil.stopRotationToView(imgProgressBar);
		}
	}
	
	private void searchUserId(String userId) {
		//Log.d(TAG, "searchUserId");
		
		if (userId == null || userId.length() == 0) {
			return;
		}
		
		final List<String> artistNames = new ArrayList<String>();

		List<NameValuePair> args = new LinkedList<NameValuePair>();
		args.add(new BasicNameValuePair("email", userId));
		
		isLoading = true;
		updateVisibility();
		
		rdio.apiCall("findUser", args, new RdioApiCallback() {
			@Override
			public void onApiSuccess(JSONObject result) {
				try {
					
					if (result == null) {
						isLoading = false;
						updateVisibility();
						throw new Exception(res.getString(R.string.user_name_could_not_be_found));
					}
					
					JSONObject userInfo = result.getJSONObject("result");
					String userKey = userInfo.getString("key"); 

					List<NameValuePair> args = new LinkedList<NameValuePair>();
					args.add(new BasicNameValuePair("user", userKey));
					args.add(new BasicNameValuePair("type", "artists"));
					args.add(new BasicNameValuePair("limit", SEARCH_LIMIT + ""));
					
					rdio.apiCall("getHeavyRotation", args, new RdioApiCallback(){

						@Override
						public void onApiFailure(String methodName, Exception e) {
							apiCallFinished(artistNames);
						}

						@Override
						public void onApiSuccess(JSONObject result) {
							try {
								if (result == null) {
									apiCallFinished(artistNames);
								}
							
								//Log.d(TAG, "result = " + result.toString());
								JSONArray media = result.getJSONArray("result");
								for (int i = 0; i < media.length(); i++) {
									try {
										JSONObject object = media.getJSONObject(i);
										String artistName = object.getString("name");
										if (!artistNames.contains(artistName)) {
											artistNames.add(artistName);
										}
										
									} catch (Exception e) {
										//Log.d(TAG, "1");
										continue;
									}
								}
								apiCallFinished(artistNames);
										
							} catch (JSONException e) {
								//Log.d(TAG, "2");
								apiCallFinished(artistNames);
							}
						}
					});
					
				} catch (Exception e) {
					isLoading = false;
					updateVisibility();
					//Log.d(TAG, "3");

					Toast toast = Toast.makeText(FragmentUtil.getActivity(RdioFragment.this), 
							R.string.user_name_could_not_be_found, Toast.LENGTH_SHORT);
					if(toast != null) {
						toast.setGravity(Gravity.CENTER, 0, -100);
						toast.show();
					}
					
					EventSeekr eventSeekr = (EventSeekr) FragmentUtil.getActivity(RdioFragment.this).getApplicationContext();
					eventSeekr.setSyncCount(Service.Rdio, EventSeekr.UNSYNC_COUNT);
					Log.e(TAG, "Failed to handle JSONObject: " + e.toString());
				}
			}

			/**
			 * The 'onApiFailure' method will be called when user wouldn't be able to connect to the rdio server
			 * and that might be because of internet connection issue.
			 */
			@Override
			public void onApiFailure(String methodName, Exception e) {
				//Log.d(TAG, "onApiFailure");
				isLoading = false;
				updateVisibility();
				
				Toast toast = Toast.makeText(FragmentUtil.getActivity(RdioFragment.this), R.string.connection_lost, 
						Toast.LENGTH_SHORT);
				if(toast != null) {
					toast.setGravity(Gravity.CENTER, 0, -100);
					toast.show();
				}

				EventSeekr eventSeekr = (EventSeekr) FragmentUtil.getActivity(RdioFragment.this).getApplicationContext();
				eventSeekr.setSyncCount(Service.Rdio, EventSeekr.UNSYNC_COUNT);
				Log.e(TAG, "Failed to handle JSONObject: ", e);
				Log.e(TAG, "getHeavyRotation failed. ", e);
			}
		});
	}
	
	private void apiCallFinished(List<String> artistNames) {
		//Log.d(TAG, "onApiFailure");
		//Log.d(TAG, "artists size = " + artistNames.size());
		if (artistNames != null) {
			new SyncArtists(artistNames, (EventSeekr) FragmentUtil.getActivity(this).getApplication(), 
					Service.Rdio, this).execute();
			
		} else {
			FragmentUtil.getActivity(this).onBackPressed();
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.btnRetrieveArtists:
			String userCredential = edtUserCredential.getText().toString().trim();
			if(!userCredential.equals("")) {
				serviceAccount.isInProgress = true;
				//Log.d(TAG, "Setting in progress true");
				searchUserId(userCredential);
			}
			break;
			
		case R.id.btnConnectOtherAccuonts:
			FragmentUtil.getActivity(this).onBackPressed();
			break;
			
		case R.id.edtUserCredential:
			edtUserCredential.selectAll();
			break;

		default:
			break;
		}
	}

	@Override
	public void onRdioAuthorised(String arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRdioReady() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRdioUserAppApprovalNeeded(Intent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRdioUserPlayingElsewhere() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isAlive() {
		return isAlive;
	}

	@Override
	public String getScreenName() {
		return "Rdio Sycn Screen";
	}
}
