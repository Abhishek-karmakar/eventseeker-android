package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.widget.Toast;

import com.android.gm.api.GoogleMusicApi;
import com.android.gm.api.exception.InvalidGooglePlayMusicAccountException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.wcities.eventseeker.ConnectAccountsFragment.Service;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.asynctask.SyncArtists;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.SyncArtistListener;
import com.wcities.eventseeker.util.FragmentUtil;

public class GooglePlayMusicFragment extends FragmentLoadableFromBackStack  {

	private static final String TAG = GooglePlayMusicFragment.class.getSimpleName();
	
	private SyncArtistListener syncArtistListener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);

		syncArtistListener = (SyncArtistListener) getArguments().getSerializable(BundleKeys.SYNC_ARTIST_LISTENER);
		syncArtistListener.onArtistSyncStarted(true);
		
		String authToken = getArguments().getString(BundleKeys.AUTH_TOKEN);
		GetArtists getArtists = new GetArtists(this, syncArtistListener);
		getArtists.execute(authToken);
	}
	
	private static class GetArtists extends AsyncTask<String, Void, List<String>> {

		private Fragment fragment;
		
		private SyncArtistListener syncArtistListener;

	    public GetArtists(Fragment fragment, SyncArtistListener syncArtistListener) {
			this.fragment = fragment;
			this.syncArtistListener = syncArtistListener;
		}

		@Override
	    protected List<String> doInBackground(String... params) {
	        List<String> artistNames = new ArrayList<String>();
	        try {
	        	String authToken = params[0];
                GoogleMusicApi.createInstance(FragmentUtil.getActivity(fragment));

                boolean success = GoogleMusicApi.login(FragmentUtil.getActivity(fragment), authToken);
                
                if (!success) {
                	//Log.d(TAG, "!success");
                    GoogleAuthUtil.invalidateToken(FragmentUtil.getActivity(fragment), authToken);
                    return null;
                }
	            
	            //List<Song> songs = GoogleMusicApi.getAllSongs(FragmentUtil.getActivity(fragment));
                artistNames = GoogleMusicApi.getAllArtistsNames(FragmentUtil.getActivity(fragment));
                
	            //Log.d(TAG, "songs list size = " + songs.size());
	            /*for (Iterator<Song> iterator = artistNames.iterator(); iterator.hasNext();) {
					Song song = iterator.next();
					if (!artistNames.contains(song.getArtist())) {
						artistNames.add(song.getArtist());
						//Log.d(TAG, "artist norm = " + song.getArtistNorm() + ", artist = " + song.getArtist());
					}
				}*/
	            
	        } catch (JSONException e) {
				e.printStackTrace();
				
			} catch (InvalidGooglePlayMusicAccountException e) {
				// custom exception defined in gmusic api by us.
				e.printStackTrace();
				return null;
				
			} catch (Exception e) {
				// Unable to parse the response
				Log.e(TAG, "Unable to parse the response from google play music");
				e.printStackTrace();
			}

	        return artistNames;
	    }
	    
	    @Override
	    protected void onPostExecute(List<String> artistNames) {
	    	super.onPostExecute(artistNames);
	    	if (artistNames != null) {
				new SyncArtists(Api.OAUTH_TOKEN, artistNames, (EventSeekr) FragmentUtil.getActivity(fragment).getApplication(), 
						Service.GooglePlay, /*fragment,*/ Service.GooglePlay.getArtistSource()).execute();
				
			} else {
				Toast.makeText(FragmentUtil.getActivity(fragment).getApplication(), 
                		R.string.couldnt_login_to_google_play_music, Toast.LENGTH_LONG).show();
				((EventSeekr)FragmentUtil.getActivity(fragment).getApplication()).setSyncCount(
						Service.GooglePlay, EventSeekr.UNSYNC_COUNT);
			}
	    }
	}

	@Override
	public String getScreenName() {
		return "Google Play Sync Screen";
	}
}
