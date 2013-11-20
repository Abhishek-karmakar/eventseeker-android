package com.wcities.eventseeker;

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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.wcities.eventseeker.ConnectAccountsFragment.Service;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.SyncArtists;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.util.FragmentUtil;

import de.umass.lastfm.Artist;
import de.umass.lastfm.Caller;
import de.umass.lastfm.Result;
import de.umass.lastfm.User;

public class LastfmFragment extends FragmentLoadableFromBackStack implements OnClickListener {

	private static final String TAG = LastfmFragment.class.getName();
	
	private ProgressBar progressBar;
	private TextView txtLoading;
	private EditText edtUserCredential;
	private Button btnRetrieveArtists;
	
	private boolean isLoading;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_service_enter_credentials_layout, null);
		
		edtUserCredential = (EditText) v.findViewById(R.id.edtUserCredential);
		btnRetrieveArtists = (Button) v.findViewById(R.id.btnRetrieveArtists);
		progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
		txtLoading = (TextView) v.findViewById(R.id.txtLoading);
		
		TextView txtServiceTitle = (TextView) v.findViewById(R.id.txtServiceTitle);
		txtServiceTitle.setText(getResources().getString(R.string.title_lastfm));
		txtServiceTitle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.lastfm, 0, 0, 0);
		
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
	
	private void updateVisibility() {
		int visibilityDesc = isLoading ? View.GONE : View.VISIBLE;
		edtUserCredential.setVisibility(visibilityDesc);
		btnRetrieveArtists.setVisibility(visibilityDesc);
		
		int visibilityLoading = !isLoading ? View.GONE : View.VISIBLE;
		progressBar.setVisibility(visibilityLoading);
		txtLoading.setVisibility(visibilityLoading);
	}
	
	private void searchUserId(final String userId) {
		if (userId == null || userId.isEmpty()) {
			return;
		}		
		
		isLoading = true;
		updateVisibility();
		
		Caller.getInstance().setCache(null);
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				final List<String> artistNames = new ArrayList<String>();
				Collection<Artist> lastFmArtists = User.getTopArtists(userId, AppConstants.LASTFM_API_KEY);
				
				Result result = Caller.getInstance().getLastResult();
				if (!result.isSuccessful()) {
					isLoading = false;
					FragmentUtil.getActivity(LastfmFragment.this).runOnUiThread(new Runnable() {
						
						@Override
						public void run() {
							updateVisibility();
							Toast toast = Toast.makeText(FragmentUtil.getActivity(LastfmFragment.this), "User name could not be found", Toast.LENGTH_SHORT);
							toast.setGravity(Gravity.CENTER, 0, -100);
							toast.show();
						}
					});
					
					Log.e("MSG", "" + result.getErrorMessage());
					return;
				}
					
				for (Artist obj: lastFmArtists) {
					Log.d(TAG, "name = " + obj.getName());
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
		if (artistNames != null && !artistNames.isEmpty()) {
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
			searchUserId(edtUserCredential.getText().toString().trim());
			break;
			
		case R.id.edtUserCredential:
			edtUserCredential.selectAll();
			break;

		default:
			break;
		}
	}
}
