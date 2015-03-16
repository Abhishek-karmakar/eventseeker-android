package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

import com.drivemode.spotify.ClientConfig;
import com.drivemode.spotify.SpotifyApi;
import com.drivemode.spotify.SpotifyApi.AuthenticationListener;
import com.drivemode.spotify.models.ArtistSimple;
import com.drivemode.spotify.models.Pager;
import com.drivemode.spotify.models.Playlist;
import com.drivemode.spotify.models.PlaylistTrack;
import com.drivemode.spotify.models.User;
import com.wcities.eventseeker.ConnectAccountsFragment.ServiceAccount;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.SyncArtists;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.constants.Enums.Service;

public class SpotifyActivity extends Activity implements AuthenticationListener {
	
	private static final String TAG = SpotifyActivity.class.getSimpleName();
	
	private static final int PLAYLIST_LIMIT = 50;
	private static final int TRACKS_LIMIT = 100;
	
	private ServiceAccount serviceAccount;
	private boolean isOnCreateOrOnNewIntentCalled;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Log.d(TAG, "onCreate() - " + this);
		super.onCreate(savedInstanceState);
		
		if (!((EventSeekr)getApplication()).isTablet()) {
			setRequestedOrientation(Configuration.ORIENTATION_PORTRAIT);
		}
		
		Bundle args = getIntent().getExtras();
		serviceAccount = (ServiceAccount) args.getSerializable(BundleKeys.SERVICE_ACCOUNTS);
		
		SpotifyApi.initialize(getApplication(), new ClientConfig.Builder()
        .setClientId(AppConstants.SPOTIFY_CLIENT_ID)
        .setClientSecret(AppConstants.SPOTIFY_CLIENT_SECRET)
        .setRedirectUri(AppConstants.SPOTIFY_REDIRECT_URI)
        .build());
		
		SpotifyApi.getInstance().authorize(this, new String[] {}, true);
		isOnCreateOrOnNewIntentCalled = true;
	}
	
	@Override
	protected void onResume() {
		//Log.d(TAG, "onResume()");
		super.onResume();
		if (!isOnCreateOrOnNewIntentCalled) {
			finish();
			
		} else {
			isOnCreateOrOnNewIntentCalled = false;
		}
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		//Log.d(TAG, "onNewIntent() - " + this);
		super.onNewIntent(intent);
		isOnCreateOrOnNewIntentCalled = true;
		if (intent.getData() != null && intent.getData().toString().contains(AppConstants.SPOTIFY_REDIRECT_URI)) {
			//Log.d(TAG, "getIntent().getData().toString() = " + intent.getData().toString());
			SpotifyApi.getInstance().onCallback(intent.getData(), this);
		}
	}
	
	@Override
	public void onReady() {
		//Log.d(TAG, "onReady()");
		/**
		 * If user clicks again on spotify authentication screen & if by that time this activity has finished, 
		 * it might start new activity where serviceAccount won't be available, so just return from here
		 */
		if (serviceAccount == null) {
			finish();
			return;
		}
		serviceAccount.isInProgress = true;
		setResult(RESULT_OK);
		finish();
		
		SpotifyApi.getInstance().getApiService().getMe(new Callback<User>() {
			
			private List<Playlist> playLists = new ArrayList<Playlist>();
			
			@Override
			public void success(final User user, Response arg1) {
				//Log.d(TAG, "success(), id = " + user.id);
				getPlayLists(user.id, 0, PLAYLIST_LIMIT, playLists);
			}
			
			@Override
			public void failure(RetrofitError arg0) {
				//Log.d(TAG, "failure()");
				((EventSeekr) getApplicationContext()).setSyncCount(Service.Spotify, EventSeekr.UNSYNC_COUNT);
			}
		});
	}
	
	private void getPlayLists(final String userId, final int offset, final int limit, final List<Playlist> playLists) {
		SpotifyApi.getInstance().getApiService().getPlaylists(userId, offset, limit, new Callback<Pager<Playlist>>() {
			
			@Override
			public void success(Pager<Playlist> playListsPager, Response arg1) {
				//Log.d(TAG, "success(), playLists count = " + playListsPager.items.size());
				playLists.addAll(playListsPager.items);
				if (playListsPager.total > offset + limit) {
					getPlayLists(userId, offset + limit, limit, playLists);
					
				} else {
					handlePlayLists(playLists, userId);
				}
			}
			
			@Override
			public void failure(RetrofitError arg0) {
				//Log.d(TAG, "failure");
				handlePlayLists(playLists, userId);
			}
		});
	}
	
	private void handlePlayLists(List<Playlist> playLists, String userId) {
		final List<String> artistNames = new ArrayList<String>();
		getArtistNames(userId, 0, TRACKS_LIMIT, artistNames, playLists.iterator(), null);
	}
	
	private void getArtistNames(final String userId, final int offset, final int limit, final List<String> artistNames, 
			final Iterator<Playlist> playlistIterator, String prevPlaylistId) {
		final String playlistId;
		
		if (offset == 0) {
			if (!playlistIterator.hasNext()) {
				//Log.d(TAG, "sync artists");
				new SyncArtists(Api.OAUTH_TOKEN, artistNames, (EventSeekr) getApplication(), 
						Service.Spotify, Service.Spotify.getArtistSource()).execute();
				return;
			}
			
			Playlist playlist = playlistIterator.next();
			playlistId = playlist.id;
			//Log.d(TAG, "playlist name = " + playlist.name);
			
		} else {
			playlistId = prevPlaylistId;
		}
		
		SpotifyApi.getInstance().getApiService().getPlaylistTracks(userId, playlistId, offset, 
				limit, new Callback<Pager<PlaylistTrack>>() {
			
			@Override
			public void success(Pager<PlaylistTrack> playlistTracks, Response arg1) {
				//Log.d(TAG, "success(), playlist id = " + playlistId + ", tracks count = " + playlistTracks.items.size());

				for (Iterator<PlaylistTrack> iterator = playlistTracks.items.iterator(); iterator
						.hasNext();) {
					PlaylistTrack playlistTrack = iterator.next();
					
					for (Iterator<ArtistSimple> iterator2 = playlistTrack.track.artists.iterator(); iterator2
							.hasNext();) {
						ArtistSimple artistSimple = iterator2.next();
						//Log.d(TAG, "artist name = " + artistSimple.name);
						if (!artistNames.contains(artistSimple.name)) {
							artistNames.add(artistSimple.name);
						}
					}
				}
				if (playlistTracks.total > offset + limit) {
					getArtistNames(userId, offset + limit, limit, artistNames, playlistIterator, playlistId);
					
				} else {
					getArtistNames(userId, 0, limit, artistNames, playlistIterator, playlistId);
				}
			}
			
			@Override
			public void failure(RetrofitError arg0) {
				//Log.d(TAG, "failure");
				getArtistNames(userId, 0, limit, artistNames, playlistIterator, playlistId);
			}
		});
	}

	@Override
	public void onError() {
		finish();
		//Log.d(TAG, "onError()");
	}
}
