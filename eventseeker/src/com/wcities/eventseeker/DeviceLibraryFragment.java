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
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wcities.eventseeker.ConnectAccountsFragment.Service;
import com.wcities.eventseeker.ConnectAccountsFragment.ServiceAccount;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.SyncArtists;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.OnFragmentAliveListener;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.ViewUtil.AnimationUtil;

public class DeviceLibraryFragment extends FragmentLoadableFromBackStack implements OnClickListener, OnFragmentAliveListener {
	
	private static final String TAG = DeviceLibraryFragment.class.getName();
	
	private ImageView imgProgressBar;
	private RelativeLayout rltMainView, rltSyncAccount;
	private TextView txtLoading, txtServiceDesc;
	private Button btnRetrieveArtists, btnConnectOtherAccounts;
	private ImageView imgAccount;
	
	private boolean isLoading;

	private ServiceAccount serviceAccount;

	private boolean isAlive;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		serviceAccount = (ServiceAccount) getArguments().getSerializable(BundleKeys.SERVICE_ACCOUNTS);
		isAlive = true;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_device_library, null);
		
		rltMainView = (RelativeLayout) v.findViewById(R.id.rltMainView);
		rltSyncAccount = (RelativeLayout) v.findViewById(R.id.rltSyncAccount);
		
		txtServiceDesc = (TextView) v.findViewById(R.id.txtServiceDesc);
		btnRetrieveArtists = (Button) v.findViewById(R.id.btnRetrieveArtists);
		
		imgAccount = (ImageView) v.findViewById(R.id.imgAccount);
		imgProgressBar = (ImageView) v.findViewById(R.id.progressBar);
		txtLoading = (TextView) v.findViewById(R.id.txtLoading);
		btnConnectOtherAccounts = (Button) v.findViewById(R.id.btnConnectOtherAccuonts);
		
		updateVisibility();
		
		btnRetrieveArtists.setOnClickListener(this);
		btnConnectOtherAccounts.setOnClickListener(this);
		
		return v;
	}
	
	private void updateVisibility() {
		int visibilityDesc = isLoading ? View.GONE : View.VISIBLE;
		/*txtServiceDesc.setVisibility(visibilityDesc);
		btnRetrieveArtists.setVisibility(visibilityDesc);*/
		rltMainView.setVisibility(visibilityDesc);
		
		int visibilityLoading = !isLoading ? View.GONE : View.VISIBLE;
		/*progressBar.setVisibility(visibilityLoading);
		txtLoading.setVisibility(visibilityLoading);*/
		rltSyncAccount.setVisibility(visibilityLoading);
		
		if (isLoading) {
			AnimationUtil.startRotationToView(imgProgressBar, 0f, 360f, 0.5f, 0.5f, 1000);
			txtLoading.setText(R.string.syncing_device_lib);
			imgAccount.setImageResource(R.drawable.devicelibrary_big);
		} else {
			AnimationUtil.stopRotationToView(imgProgressBar);			
		}
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
				
				EventSeekr eventSeekr = (EventSeekr) FragmentUtil.getActivity(DeviceLibraryFragment.this).getApplicationContext();
				eventSeekr.setSyncCount(Service.DeviceLibrary, EventSeekr.UNSYNC_COUNT);
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

		case R.id.btnConnectOtherAccuonts:
			FragmentUtil.getActivity(this).onBackPressed();
			break;

		default:
			break;
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		isAlive = false;
	}

	@Override
	public boolean isAlive() {
		return isAlive;
	}
	
}
