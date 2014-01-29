package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONException;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.gm.api.GoogleMusicApi;
import com.android.gm.api.exception.InvalidGooglePlayMusicAccountException;
import com.android.gm.api.model.Song;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.wcities.eventseeker.ConnectAccountsFragment.Service;
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
		
		((TextView)v.findViewById(R.id.txtLoading)).setText("Syncing Google Play");
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
	            
	            List<Song> songs = GoogleMusicApi.getAllSongs(FragmentUtil.getActivity(fragment));
	            //Log.d(TAG, "songs list size = " + songs.size());
	            for (Iterator<Song> iterator = songs.iterator(); iterator.hasNext();) {
					Song song = iterator.next();
					if (!artistNames.contains(song.getArtist())) {
						artistNames.add(song.getArtist());
						//Log.d(TAG, "artist norm = " + song.getArtistNorm() + ", artist = " + song.getArtist());
					}
				}
	            
	        } catch (JSONException e) {
				e.printStackTrace();
				
			} catch (InvalidGooglePlayMusicAccountException e) {
				// custom exception defined in gmusic api by us.
				e.printStackTrace();
				return null;
			}

	        return artistNames;
	    }
	    
	    @Override
	    protected void onPostExecute(List<String> artistNames) {
	    	super.onPostExecute(artistNames);
	    	if (artistNames != null) {
				new SyncArtists(artistNames, (EventSeekr) FragmentUtil.getActivity(fragment).getApplication(), 
						Service.GooglePlay, fragment).execute();
				
			} else {
				Toast.makeText(FragmentUtil.getActivity(fragment).getApplication(), 
                		"Could not login to google play music account. Please make sure you are using right "
                		+ "google play music account.", Toast.LENGTH_LONG).show();
				((EventSeekr)FragmentUtil.getActivity(fragment).getApplication()).setSyncCount(
						Service.GooglePlay, EventSeekr.UNSYNC_COUNT);
				FragmentUtil.getActivity(fragment).onBackPressed();
			}
	    }
	}
}
