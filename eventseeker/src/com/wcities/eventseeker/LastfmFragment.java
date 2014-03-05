package com.wcities.eventseeker;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

import de.umass.lastfm.Artist;
import de.umass.lastfm.Caller;
import de.umass.lastfm.Result;
import de.umass.lastfm.User;

public class LastfmFragment extends FragmentLoadableFromBackStack implements OnClickListener, OnFragmentAliveListener {

	private static final String TAG = LastfmFragment.class.getName();
	
	//private ProgressBar imgProgressBar;
	private ImageView imgProgressBar, imgAccount;
	private TextView txtLoading;
	private EditText edtUserCredential;
	private Button btnRetrieveArtists, btnConnectOtherAccounts;
	
	private View rltMainView, rltSyncAccount;
	
	private ServiceAccount serviceAccount;

	private boolean isAlive;
	
	private boolean isLoading;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		serviceAccount = (ServiceAccount) getArguments().getSerializable(BundleKeys.SERVICE_ACCOUNTS);
		isAlive = true;
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
		
		TextView txtServiceTitle = (TextView) v.findViewById(R.id.txtServiceTitle);
		txtServiceTitle.setText(getResources().getString(R.string.title_lastfm));
		txtServiceTitle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.lastfm, 0, 0, 0);
		
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
	
	private void updateVisibility() {
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
			txtLoading.setText("Syncing Last.fm");
			imgAccount.setImageResource(R.drawable.lastfm_big);
		} else {
			AnimationUtil.stopRotationToView(imgProgressBar);
		}
	}
	
	
	private void searchUserId(final String userId) {
		if (userId == null || userId.length() == 0) {
			return;
		}		
		
		isLoading = true;
		updateVisibility();
		
		Caller.getInstance().setCache(null);
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				final List<String> artistNames = new ArrayList<String>();
				Collection<Artist> lastFmArtists = null;
				Result result = null;
				try {
					lastFmArtists = User.getTopArtists(userId, AppConstants.LASTFM_API_KEY);
					result = Caller.getInstance().getLastResult();
				} catch (Exception e) {
					/**
					 * If Initially the error was due to User not found and then after internet failure occurs.
					 * Then also 'Result' object was giving 'User not found error'. So, manually checking the cause of issue
					 * and set the Result object as null.
					 */
					if (e instanceof UnknownHostException) {
						result = null;
					}
					e.printStackTrace();
				}

				if (result == null || !result.isSuccessful()) {
					isLoading = false;
					final Result rslt = result;

					FragmentUtil.getActivity(LastfmFragment.this).runOnUiThread(new Runnable() {
						
						@Override
						public void run() {
							String msg = "The Internet connection appears to be offline.";
							if (rslt != null) {
								Log.e("MSG", "" + rslt.getErrorMessage());
								msg = "User name could not be found";
							}

							updateVisibility();
							Toast toast = Toast.makeText(FragmentUtil.getActivity(LastfmFragment.this), msg, Toast.LENGTH_SHORT);
							if(toast != null) {
								toast.setGravity(Gravity.CENTER, 0, -100);
								toast.show();
							}

							EventSeekr eventSeekr = (EventSeekr) FragmentUtil.getActivity(LastfmFragment.this).getApplicationContext();
							eventSeekr.setSyncCount(Service.Lastfm, EventSeekr.UNSYNC_COUNT);
						}
					});
					return;
				}
				for (Artist obj: lastFmArtists) {
					//Log.d(TAG, "name = " + obj.getName());
					artistNames.add(obj.getName());
				}
				FragmentUtil.getActivity(LastfmFragment.this).runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						apiCallFinished(artistNames);
					}
				});
			}
		}).start();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		isAlive = false;
	}

	private void apiCallFinished(List<String> artistNames) {
		//Log.d(TAG, "artists size = " + artistNames.size());
		if (artistNames != null) {
			new SyncArtists(artistNames, (EventSeekr) FragmentUtil.getActivity(this).getApplication(), 
					Service.Lastfm, this).execute();
			
		} else {
			FragmentUtil.getActivity(this).onBackPressed();
		}
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.btnRetrieveArtists:
			serviceAccount.isInProgress = true;
			searchUserId(edtUserCredential.getText().toString().trim());
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
	public boolean isAlive() {
		return isAlive;
	}
}
