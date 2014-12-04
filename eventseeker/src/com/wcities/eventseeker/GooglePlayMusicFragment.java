package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
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
import com.wcities.eventseeker.interfaces.OnFragmentAliveListener;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.ViewUtil.AnimationUtil;

public class GooglePlayMusicFragment extends FragmentLoadableFromBackStack implements OnClickListener, 
		OnFragmentAliveListener {

	private static final String TAG = GooglePlayMusicFragment.class.getSimpleName();
	
	private ImageView imgProgressBar;
	
	private boolean isAlive;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		isAlive = true;

		String authToken = getArguments().getString(BundleKeys.AUTH_TOKEN);
		GetArtists getArtists = new GetArtists(this);
		getArtists.execute(authToken);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate( R.layout.fragment_service_enter_credentials_layout, null);

		v.findViewById(R.id.rltMainView).setVisibility(View.GONE);
		v.findViewById(R.id.rltSyncAccount).setVisibility(View.VISIBLE);

		imgProgressBar = (ImageView) v.findViewById(R.id.progressBar);
		((ImageView) v.findViewById(R.id.imgAccount)).setImageResource(R.drawable.google_play_big);
		
		((TextView)v.findViewById(R.id.txtLoading)).setText(R.string.syncing_google_play);
		v.findViewById(R.id.btnConnectOtherAccuonts).setOnClickListener(this);
		
		AnimationUtil.startRotationToView(imgProgressBar, 0f, 360f, 0.5f, 0.5f, 1000);
		
		return v;
	}

	@Override
	public boolean isAlive() {
		return isAlive;
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		AnimationUtil.stopRotationToView(imgProgressBar);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		isAlive = false;
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.btnConnectOtherAccuonts:
			FragmentUtil.getActivity(this).onBackPressed();
			break;

		default:
			break;
		}
	}
	
	private static class GetArtists extends AsyncTask<String, Void, List<String>> {

		private Fragment fragment;

	    public GetArtists(Fragment fragment) {
			this.fragment = fragment;
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
						Service.GooglePlay, fragment, Service.GooglePlay.getArtistSource()).execute();
				
			} else {
				Toast.makeText(FragmentUtil.getActivity(fragment).getApplication(), 
                		R.string.couldnt_login_to_google_play_music, Toast.LENGTH_LONG).show();
				((EventSeekr)FragmentUtil.getActivity(fragment).getApplication()).setSyncCount(
						Service.GooglePlay, EventSeekr.UNSYNC_COUNT);
				FragmentUtil.getActivity(fragment).onBackPressed();
			}
	    }
	}

	@Override
	public String getScreenName() {
		return "Google Play Sync Screen";
	}
}
