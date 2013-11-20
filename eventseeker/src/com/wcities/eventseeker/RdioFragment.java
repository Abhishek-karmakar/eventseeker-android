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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.rdio.android.api.Rdio;
import com.rdio.android.api.RdioApiCallback;
import com.rdio.android.api.RdioListener;
import com.wcities.eventseeker.ConnectAccountsFragment.Service;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.SyncArtists;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.util.FragmentUtil;

public class RdioFragment extends FragmentLoadableFromBackStack implements OnClickListener, RdioListener {

	private static final String TAG = RdioFragment.class.getName();
	
	private static final String PREF_ACCESSTOKEN = "prefs.accesstoken";
    private static final String PREF_ACCESSTOKENSECRET = "prefs.accesstokensecret";
    
    private static final int SEARCH_LIMIT = 250;

    private String accessToken = null;
    private String accessTokenSecret = null;
    
	private ProgressBar progressBar;
	private TextView txtLoading;
	private EditText edtUserCredential;
	private Button btnRetrieveArtists;
	
	private boolean isLoading;
	
	private static Rdio rdio;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		
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

            // Initialise our API object
            rdio = new Rdio(AppConstants.RDIO_KEY, AppConstants.RDIO_SECRET, accessToken, accessTokenSecret,
					FragmentUtil.getActivity(this), this);      
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_service_enter_credentials_layout, null);
		
		edtUserCredential = (EditText) v.findViewById(R.id.edtUserCredential);
		btnRetrieveArtists = (Button) v.findViewById(R.id.btnRetrieveArtists);
		progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
		txtLoading = (TextView) v.findViewById(R.id.txtLoading);
		
		updateVisibility();
		
		btnRetrieveArtists.setOnClickListener(this);
		
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
		Log.d(TAG, "Cleaning up..");
		// Make sure to call the cleanup method on the API object
		if (rdio != null) {
			rdio.cleanup();
		}
		super.onDestroy();
	}
	
	private void updateVisibility() {
		int visibilityDesc = isLoading ? View.GONE : View.VISIBLE;
		edtUserCredential.setVisibility(visibilityDesc);
		btnRetrieveArtists.setVisibility(visibilityDesc);
		
		int visibilityLoading = !isLoading ? View.GONE : View.VISIBLE;
		progressBar.setVisibility(visibilityLoading);
		txtLoading.setVisibility(visibilityLoading);
	}
	
	private void searchUserId(String userId) {
		if (userId == null || userId.isEmpty()) {
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
						isLoading = true;
						updateVisibility();
						throw new Exception("User name could not be found.");
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
							
								Log.d(TAG, "result = " + result.toString());
								JSONArray media = result.getJSONArray("result");
								for (int i = 0; i < media.length(); i++) {
									try {
										JSONObject object = media.getJSONObject(i);
										String artistName = object.getString("name");
										if (!artistNames.contains(artistName)) {
											artistNames.add(artistName);
										}
										
									} catch (Exception e) {
										continue;
									}
								}
								apiCallFinished(artistNames);
										
							} catch (JSONException e) {
								apiCallFinished(artistNames);
							}
						}
					});
					
				} catch (Exception e) {
					isLoading = false;
					updateVisibility();
					
					Toast toast = Toast.makeText(FragmentUtil.getActivity(RdioFragment.this), "User name could not be found", 
							Toast.LENGTH_SHORT);
					toast.setGravity(Gravity.CENTER, 0, -100);
					toast.show();
					Log.e(TAG, "Failed to handle JSONObject: ", e);
				}
			}

			@Override
			public void onApiFailure(String methodName, Exception e) {
				isLoading = false;
				updateVisibility();
				
				Toast toast = Toast.makeText(FragmentUtil.getActivity(RdioFragment.this), "User name could not be found", 
						Toast.LENGTH_SHORT);
				toast.setGravity(Gravity.CENTER, 0, -100);
				toast.show();
				Log.e(TAG, "Failed to handle JSONObject: ", e);
				Log.e(TAG, "getHeavyRotation failed. ", e);
			}
		});
	}
	
	private void apiCallFinished(List<String> artistNames) {
		Log.d(TAG, "artists size = " + artistNames.size());
		if (artistNames != null && !artistNames.isEmpty()) {
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
			searchUserId(edtUserCredential.getText().toString().trim());
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
}