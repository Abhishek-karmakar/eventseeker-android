package com.wcities.eventseeker;

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

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.umass.lastfm.Artist;
import de.umass.lastfm.Caller;
import de.umass.lastfm.Result;
import de.umass.lastfm.User;

public class LastfmFragment extends FragmentLoadableFromBackStack implements OnClickListener {

	private static final String TAG = LastfmFragment.class.getName();
	
	private EditText edtUserCredential;
	private Button btnRetrieveArtists;
	
	private ServiceAccount serviceAccount;

	private SyncArtistListener syncArtistListener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		serviceAccount = (ServiceAccount) getArguments().getSerializable(BundleKeys.SERVICE_ACCOUNTS);

		String tag = getArguments().getString(BundleKeys.SYNC_ARTIST_LISTENER);
		syncArtistListener = (SyncArtistListener) 
				((BaseActivity) FragmentUtil.getActivity(this)).getFragmentByTag(tag);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_service_enter_credentials_layout, null);

		TextView txtServiceTitle = (TextView) v.findViewById(R.id.txtServiceTitle);
		txtServiceTitle.setText(getResources().getString(R.string.title_lastfm));
		txtServiceTitle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.lastfm, 0, 0, 0);
		
		edtUserCredential = (EditText) v.findViewById(R.id.edtUserCredential);
		edtUserCredential.setHint(R.string.user_name);
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
		
		btnRetrieveArtists = (Button) v.findViewById(R.id.btnRetrieveArtists);
		btnRetrieveArtists.setOnClickListener(this);		
		
		return v;
	}
	
	private void searchUserId(final String userId) {
		if (userId == null || userId.length() == 0) {
			return;
		}		
		
		serviceAccount.isInProgress = true;
		syncArtistListener.onArtistSyncStarted(true);
		
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
					final Result rslt = result;

					FragmentUtil.getActivity(LastfmFragment.this).runOnUiThread(new Runnable() {
						
						@Override
						public void run() {
							int msg = R.string.connection_lost;
							if (rslt != null) {
								Log.e("MSG", "" + rslt.getErrorMessage());
								msg = R.string.user_name_could_not_be_found;
							}

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
	
	private void apiCallFinished(List<String> artistNames) {
		//Log.d(TAG, "artists size = " + artistNames.size());
		if (artistNames != null) {
			new SyncArtists(Api.OAUTH_TOKEN, artistNames, (EventSeekr) FragmentUtil.getActivity(this).getApplication(), 
					Service.Lastfm, /*this,*/ Service.Lastfm.getArtistSource()).execute();
			
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
	public String getScreenName() {
		return ScreenNames.LAST_FM_SYNC;
	}
}
