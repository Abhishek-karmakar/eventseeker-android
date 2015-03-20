package com.wcities.eventseeker;

import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

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

public class BeatsMusicFragment extends FragmentLoadableFromBackStack {

	private static final String TAG = BeatsMusicFragment.class.getName();
	
	private ServiceAccount serviceAccount;

	private SyncArtistListener syncArtistListener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		
		serviceAccount = (ServiceAccount) getArguments().getSerializable(BundleKeys.SERVICE_ACCOUNTS);

		String tag = getArguments().getString(BundleKeys.SYNC_ARTIST_LISTENER);
		syncArtistListener = (SyncArtistListener) ((BaseActivity) FragmentUtil.getActivity(this)).getFragmentByTag(tag);
		
		Intent intent = new Intent(FragmentUtil.getApplication(this), BeatsMusicActivity.class);
		startActivityForResult(intent, AppConstants.REQ_CODE_BEATS);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (data != null && data.hasExtra(AppConstants.LIST_OF_ARTISTS_NAMES)) {			
			ArrayList<String> artistNames = (ArrayList<String>) data.getSerializableExtra(AppConstants.LIST_OF_ARTISTS_NAMES);
			startSyncing(artistNames);
		}
	}
	
	private void startSyncing(ArrayList<String> artistNames) {		
		serviceAccount.isInProgress = true;
		syncArtistListener.onArtistSyncStarted(true);
		
		Log.d(TAG, "artists size = " + artistNames.size());
		if (artistNames != null) {
			/**
			 * TODO: Uncomment this for Syncing the Beats Music Artist
			  new SyncArtists(Api.OAUTH_TOKEN, artistNames, (EventSeekr) FragmentUtil.getActivity(this).getApplication(), 
					Service.Beats, this, Service.Beats.getArtistSource()).execute();*/
			
		} else {
			FragmentUtil.getActivity(this).onBackPressed();
		}
	}

	@Override
	public String getScreenName() {
		return ScreenNames.BEATS_MUSIC;
	}
}
