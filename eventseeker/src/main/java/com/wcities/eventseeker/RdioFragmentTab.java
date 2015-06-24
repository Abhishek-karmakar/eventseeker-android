package com.wcities.eventseeker;

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
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.rdio.android.core.RdioApiResponse;
import com.rdio.android.core.RdioService_Api;
import com.rdio.android.sdk.OAuth2Credential;
import com.rdio.android.sdk.Rdio;
import com.rdio.android.sdk.RdioListener;
import com.rdio.android.sdk.RdioService;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.SyncArtists;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.constants.Enums.Service;
import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.SyncArtistListener;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.viewdata.ServiceAccount;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RdioFragmentTab extends FragmentLoadableFromBackStack implements OnClickListener, RdioListener {

	private static final String TAG = RdioFragmentTab.class.getSimpleName();
	
    private static final int SEARCH_LIMIT = 250;

	private EditText edtUserCredential;
	private Button btnRetrieveArtists;
	
	private ServiceAccount serviceAccount;

	private SyncArtistListener syncArtistListener;
	
	private Rdio rdio;
	private RdioService rdioService;

	private Resources res;

	private boolean onApiServiceReadyCalled, isDestroyed;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		serviceAccount = (ServiceAccount) getArguments().getSerializable(BundleKeys.SERVICE_ACCOUNTS);
		//Log.d(TAG, "onCreate : SerciveAccount" + serviceAccount);

		String tag = getArguments().getString(BundleKeys.SYNC_ARTIST_LISTENER);
		syncArtistListener = (SyncArtistListener) 
				((BaseActivity) FragmentUtil.getActivity(this)).getFragmentByTag(tag);

		// Initialize our API object
		rdio = new Rdio(AppConstants.RDIO_CLIENT_ID, AppConstants.RDIO_CLIENT_SECRET, null,
				FragmentUtil.getApplication(this), this);
		/**
		 * try-catch included for api < 16, because in fact this rdio api supports minSdk 16.
		 * In lower apis, it throws:
		 * java.lang.NoClassDefFoundError: android.media.MediaCodec$BufferInfo
		 * at com.rdio.android.audioplayer.RdioAudioPlayer.initializeMediaDecoder(RdioAudioPlayer.java:241)
		 */
		try {
			rdio.requestApiService();

		} catch (NoClassDefFoundError e) {
			e.printStackTrace();
		}

		res = FragmentUtil.getResources(this);
		//Log.d(TAG, "onCreate() done");
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_service_enter_credentials_layout, null);
		
		edtUserCredential = (EditText) v.findViewById(R.id.edtUserCredential);
		btnRetrieveArtists = (Button) v.findViewById(R.id.btnRetrieveArtists);

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

		return v;
	}
	
	@Override
	public void onResume() {
		super.onResume(AppConstants.INVALID_INDEX, FragmentUtil.getResources(this).getString(R.string.title_rdio));
	}
	
	@Override
	public void onDestroy() {
		//Log.d(TAG, "Cleaning up..");
		/**
		 * If we call cleanupRdio() without this check, it throws NullPointerException from rdio api
		 * internal code where it requires handler which becomes null on cleanup().
		 */
		if (onApiServiceReadyCalled) {
			cleanupRdio();
		}
		isDestroyed = true;
		super.onDestroy();
	}

	private void cleanupRdio() {
		// Make sure to call the cleanup method on the API object
		if (rdio != null) {
			/**
			 * try-catch included for api < 16, because in fact this rdio api supports minSdk 16.
			 * In lower apis, it throws:
			 * java.lang.NullPointerException
			 * at com.rdio.android.sdk.internal.RdioInternal.cleanup(RdioInternal.java:124)
			 */
			try {
				rdio.cleanup();

			} catch (NullPointerException e) {
				e.printStackTrace();
			}
		}
	}

	private void searchUserId(String userId) {
		//Log.d(TAG, "searchUserId");
		if (userId == null || userId.length() == 0 || rdioService == null) {
			return;
		}
		
		serviceAccount.isInProgress = true;
		syncArtistListener.onArtistSyncStarted(true);
		
		RdioService_Api.ResponseListener responseListener = new RdioService_Api.ResponseListener() {

			@Override
			public void onResponse(RdioApiResponse rdioApiResponse) {
				//Log.d(TAG, "onResponse() - " + rdioApiResponse.getResult().toString());
				try {
					JSONObject userInfo = rdioApiResponse.getResult();
					if (userInfo == null || (userInfo.has("result") && userInfo.isNull("result"))) {
						//Log.d(TAG, "throw exception");
						throw new Exception(res.getString(R.string.user_name_could_not_be_found));
					}

					String userKey = userInfo.getString("key");
					//Log.d(TAG, "userKey = " + userKey);

					rdioService.getHeavyRotation(userKey, RdioService_Api.GetHeavyRotation_type.Artists, false,
							SEARCH_LIMIT, 0, SEARCH_LIMIT, null, false, null, new RdioService_Api.ResponseListener() {
								@Override
								public void onResponse(RdioApiResponse rdioApiResponse) {
									//Log.d(TAG, "heavy rotation onResponse() - " + rdioApiResponse.getResult().toString());
									List<String> artistNames = new ArrayList<String>();
									JSONArray media = rdioApiResponse.getResult();
									// sometimes rdio lib returns null as result
									if (media != null) {
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
									}
									apiCallFinished(artistNames);
								}
							});

				} catch (Exception e) {
					//Log.d(TAG, "3");

					Toast toast = Toast.makeText(FragmentUtil.getActivity(RdioFragmentTab.this),
							R.string.user_name_could_not_be_found, Toast.LENGTH_SHORT);
					if (toast != null) {
						toast.setGravity(Gravity.CENTER, 0, -100);
						toast.show();
					}

					EventSeekr eventSeekr = (EventSeekr) FragmentUtil.getActivity(RdioFragmentTab.this).getApplicationContext();
					eventSeekr.setSyncCount(Service.Rdio, EventSeekr.UNSYNC_COUNT);
					Log.e(TAG, "Failed to handle JSONObject: " + e.toString());
				}
			}
		};

		if (userId.contains("@")) {
			rdioService.findUser(userId, null, null, false, null, responseListener);

		} else {
			rdioService.findUser(null, userId, null, false, null, responseListener);
		}
	}
	
	private void apiCallFinished(List<String> artistNames) {
		//Log.d(TAG, "onApiFailure");
		//Log.d(TAG, "artists size = " + artistNames.size());
		if (artistNames != null) {
			new SyncArtists(Api.OAUTH_TOKEN, artistNames, (EventSeekr) FragmentUtil.getActivity(this).getApplication(), 
					Service.Rdio, /*this,*/ Service.Rdio.getArtistSource()).execute();
			
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
			
		default:
			break;
		}
	}

	@Override
	public String getScreenName() {
		return ScreenNames.RDIO_SYNC;
	}

	@Override
	public void onRdioReadyForPlayback() {

	}

	@Override
	public void onRdioUserPlayingElsewhere() {

	}

	@Override
	public void onRdioAuthorised(OAuth2Credential oAuth2Credential) {
		//Log.d(TAG, "onRdioAuthorised()");
	}

	@Override
	public void onError(Rdio.RdioError rdioError, String s) {
		//Log.e(TAG, "Oh no, we just got an error : " + rdioError + " w/ msg " + s);
	}

	@Override
	public void onApiServiceReady(RdioService rdioService) {
		//Log.d(TAG, "onApiServiceReady()");
		this.rdioService = rdioService;
		onApiServiceReadyCalled = true;
		if (isDestroyed) {
			//Log.d(TAG, "onApiServiceReady() isDestroyed = true");
			cleanupRdio();
		}
	}
}
