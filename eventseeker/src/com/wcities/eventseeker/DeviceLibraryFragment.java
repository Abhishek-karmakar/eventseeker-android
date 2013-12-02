package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.wcities.eventseeker.ConnectAccountsFragment.Service;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.SyncArtists;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.util.FragmentUtil;

public class DeviceLibraryFragment extends FragmentLoadableFromBackStack implements OnClickListener {
	
	private static final String TAG = DeviceLibraryFragment.class.getName();
	
	private ProgressBar progressBar;
	private TextView txtLoading, txtServiceDesc;
	private Button btnRetrieveArtists;
	
	private boolean isLoading;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_device_library, null);
		
		txtServiceDesc = (TextView) v.findViewById(R.id.txtServiceDesc);
		btnRetrieveArtists = (Button) v.findViewById(R.id.btnRetrieveArtists);
		progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
		txtLoading = (TextView) v.findViewById(R.id.txtLoading);
		
		updateVisibility();
		
		btnRetrieveArtists.setOnClickListener(this);
		
		return v;
	}
	
	private void updateVisibility() {
		int visibilityDesc = isLoading ? View.GONE : View.VISIBLE;
		txtServiceDesc.setVisibility(visibilityDesc);
		btnRetrieveArtists.setVisibility(visibilityDesc);
		
		int visibilityLoading = !isLoading ? View.GONE : View.VISIBLE;
		progressBar.setVisibility(visibilityLoading);
		txtLoading.setVisibility(visibilityLoading);
	}
	
	private void searchDeviceLirbary() {
		isLoading = true;
		updateVisibility();
		new UpdateArtistsTask().execute();
	}
	
	private void apiCallFinished(List<String> artists) {
		if (artists != null) {
			//Log.d(TAG, "artists size = " + artists.size());
			new SyncArtists(artists, (EventSeekr) FragmentUtil.getActivity(this).getApplication(), 
					Service.DeviceLibrary, this).execute();
			
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
				isLoading = false;
				updateVisibility();
				e.printStackTrace();
			} 
			return null;
		}
		
		@Override
		protected void onPostExecute(List<String> artists){
			apiCallFinished(artists);
		}
		
		private List<String> getDeviceArtists() {
			ContentResolver contentResolver = FragmentUtil.getActivity(DeviceLibraryFragment.this).getContentResolver();
			
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
							if (artistName != null && !artistName.isEmpty()) {
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
			searchDeviceLirbary();
			break;

		default:
			break;
		}
	}
}
