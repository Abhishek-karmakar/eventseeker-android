package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

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

public class DeviceLibraryFragmentTab extends FragmentLoadableFromBackStack implements OnClickListener {
	
	private static final String TAG = DeviceLibraryFragmentTab.class.getName();
	
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
		View v = inflater.inflate(R.layout.fragment_device_library, null);
		((Button) v.findViewById(R.id.btnRetrieveArtists)).setOnClickListener(this);
		return v;
	}
	
	@Override
	public void onResume() {
		super.onResume(AppConstants.INVALID_INDEX, 
				FragmentUtil.getResources(this).getString(R.string.title_device_library));
	}
	
	private void searchDeviceLirbary() {
		syncArtistListener.onArtistSyncStarted(true);
		new UpdateArtistsTask().execute();
	}
	
	private void apiCallFinished(List<String> artists) {
		if (artists != null) {
			//Log.d(TAG, "artists size = " + artists.size());
			new SyncArtists(Api.OAUTH_TOKEN, artists, (EventSeekr) FragmentUtil.getActivity(this).getApplication(), 
					Service.DeviceLibrary, /*this,*/ Service.DeviceLibrary.getArtistSource()).execute();
			
		} else {
			FragmentUtil.getActivity(this).onBackPressed();
		}
	}
	
	private class UpdateArtistsTask extends AsyncTask<Void, Void, List<String>> {
		
		@Override
		protected List<String> doInBackground(Void... params) {
			try {
				return getDeviceArtists();
			} catch (Exception e) {
				e.printStackTrace();
				
				EventSeekr eventSeekr = (EventSeekr) FragmentUtil.getActivity(DeviceLibraryFragmentTab.this).getApplicationContext();
				eventSeekr.setSyncCount(Service.DeviceLibrary, EventSeekr.UNSYNC_COUNT);
			} 
			return null;
		}
		
		@Override
		protected void onPostExecute(List<String> artists){
			apiCallFinished(artists);
		}
		
		private List<String> getDeviceArtists() {
			ContentResolver contentResolver = FragmentUtil.getActivity(DeviceLibraryFragmentTab.this).getContentResolver();
			
			List<String> artists = new ArrayList<String>();

			String[] proj = { MediaStore.Audio.Artists._ID,
					MediaStore.Audio.Artists.ARTIST,
					MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
					MediaStore.Audio.Artists.NUMBER_OF_TRACKS };
			Cursor musicCursor = contentResolver.query(
					MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, proj, null,
					null, MediaStore.Audio.Artists.ARTIST + " ASC");

			if (musicCursor != null) {
				try {
					if (musicCursor.moveToFirst()) {
						do {
							String artistName = musicCursor.getString(1);
							/*final int albumsCount = musicCursor.getInt(2);
							final int tracksCount = musicCursor.getInt(3);
							Log.d("ArtistUpdateActivity", "Artist: " + artistName
									+ ", Albums: " + albumsCount + ", Tracks: "
									+ tracksCount);*/
							if (artistName != null && artistName.length() != 0) {
								artists.add(artistName);
							}
							
						} while (musicCursor.moveToNext());
					}

				} finally {
					musicCursor.close();
				}
			}
			return artists;
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.btnRetrieveArtists:
			serviceAccount.isInProgress = true;
			searchDeviceLirbary();
			break;

		default:
			break;
		}
	}

	@Override
	public String getScreenName() {
		return ScreenNames.DEVICE_LIBRARY;
	}
	
}
