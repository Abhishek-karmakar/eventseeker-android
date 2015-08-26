package com.wcities.eventseeker;

import android.app.Activity;
import android.view.View;
import android.webkit.WebView;

/*import com.freethinking.beats.sdk.data.Artist;
import com.freethinking.beats.sdk.data.Artists;
import com.freethinking.beats.sdk.data.Me;
import com.freethinking.beats.sdk.mappers.ArtistsMapper;
import com.freethinking.beats.sdk.mappers.MeMapper;
import com.freethinking.beats.sdk.network.NetworkAdapter;
import com.freethinking.beats.sdk.network.NetworkParts;
import com.freethinking.beats.sdk.network.UrlFactory;
import com.freethinking.beats.sdk.utility.ApplicationData;
import com.wcities.eventseeker.constants.AppConstants;*/

public class BeatsMusicActivity extends Activity {

	private WebView webView;
	private View prgBrLogin;

    /*protected Me me;
    protected Artists artists;

    private MyArtistsNetworkRequest myArtistsNetworkRequest;
    private MeNetworkRequest meNetworkRequest;

    /*@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beats_music);
        
        me = new Me();
        artists = new Artists();
        
        prgBrLogin = findViewById(R.id.prgBrLogin);
        
        webView = (WebView) findViewById(R.id.wbvBeatsMusicLogin);
        webView.setWebViewClient(new WebViewClient() {
        	

			public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            	Toast.makeText(BeatsMusicActivity.this, description, Toast.LENGTH_SHORT).show();
            }
            
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
            	boolean isOverridedUrl = false;

            	if (url.contains(AppConstants.BEATS_MUSIC_REDIRECT_URI) && url.contains("access_token")) {
            		isOverridedUrl = true;
            		
            		Uri uri = Uri.parse(url);
            		String accessToken = uri.getQueryParameter("access_token");
            		String expiresIn = uri.getQueryParameter("expires_in");
            		int accessExpiresAt = expiresIn == null ? 0 : Integer.parseInt(expiresIn);
            				
            		*//**
            		 * All these data is being updated to SharedPreference as the sdk is using these info by directly 
            		 * accessing it from SharePreference
            		 *//*
            		String preferencesKey = ApplicationData.getStorePreferencesKey(getApplicationContext());
            		getSharedPreferences(preferencesKey, MODE_PRIVATE).edit().putString("access_token", accessToken).commit();
					getSharedPreferences(preferencesKey, MODE_PRIVATE).edit().putLong("access_expires_at", System.currentTimeMillis() + (1000 * accessExpiresAt)).commit();

					*//**
					 * Getting the UserId
					 *//*
            		meNetworkRequest = new MeNetworkRequest(getApplicationContext(), accessToken);
            		meNetworkRequest.execute(UrlFactory.me());
            	}
            	
                return isOverridedUrl;
            }
            
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
            	super.onPageStarted(view, url, favicon);
            	prgBrLogin.setVisibility(View.VISIBLE);
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
            	super.onPageFinished(view, url);
            	prgBrLogin.setVisibility(View.GONE);
            }
        });
        
        webView.loadUrl("https://partner.api.beatsmusic.com/v1/oauth2/authorize?"
        		+ "response_type=token"
        		+ "&redirect_uri=" + AppConstants.BEATS_MUSIC_REDIRECT_URI 
        		+ "&client_id=" + UrlFactory.clientID(this));
    }

    public void onComplete(ArrayList<String> artistNameLst) {
        Intent returnIntent = new Intent();
        returnIntent.putExtra(AppConstants.LIST_OF_ARTISTS_NAMES, artistNameLst);
        setResult(RESULT_OK, returnIntent);
        finish();
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	if (meNetworkRequest != null) {
    		meNetworkRequest.cancel(true);
    	}
    	
    	if (myArtistsNetworkRequest != null) {
    		myArtistsNetworkRequest.cancel(true);
    	}
    }
    
    protected class MeNetworkRequest extends NetworkAdapter {

        private String accessToken;

		public MeNetworkRequest(Context context, String accessToken) {
            super(context, new MeMapper(), NetworkParts.RequestType.GET, new HashMap<String, String>(), me, AppConstants.BEATS_MUSIC_REDIRECT_URI);
            this.accessToken = accessToken;
        }

        @Override
        protected Boolean authRequired() {
            return true;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            String userId = me.getResult().getUserContext();
            *//**
    		 * All these data is being updated to SharedPreference as the sdk is using these info by directly 
    		 * accessing it from SharePreference
    		 *//*
            String preferencesKey = ApplicationData.getStorePreferencesKey(getApplicationContext());
			getSharedPreferences(preferencesKey, MODE_PRIVATE).edit().putString("user_id", userId).commit();

    		*//**
    		 * Getting the User's Music Artists
    		 *//*
    		myArtistsNetworkRequest = new MyArtistsNetworkRequest(getApplicationContext());
    		myArtistsNetworkRequest.execute(UrlFactory.usersMusicLibraryArtists(userId, accessToken));
        }
    }
    
    protected class MyArtistsNetworkRequest extends NetworkAdapter {
    	
    	private final String TAG = MyArtistsNetworkRequest.class.getSimpleName();

		public MyArtistsNetworkRequest(Context context) {
    		super(context, new ArtistsMapper(), NetworkParts.RequestType.GET, new HashMap<String, String>(), artists);
    	}
    	
    	@Override
    	protected void onPostExecute(String result) {
    		super.onPostExecute(result);
    		ArrayList<String> artistNameLst = new ArrayList<String>();
    		*//**
    		 * before adding it to List check if the Artist is already present in the list because sometimes it 
    		 * gives duplicates in a list. And this the SDK's bug
    		 *//*
    		List<Artist> artistsLst = artists.getArtists();
    		for (Artist artist : artistsLst) {
    			String name = artist.getName();
				if (!artistsLst.contains(name)) {
    				artistNameLst.add(name);
    			}
    		}
    		
    		onComplete(artistNameLst);
    	}
    }*/
}
