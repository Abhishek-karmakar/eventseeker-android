package com.freethinking.beats.sdk.login;

import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.freethinking.beats.sdk.R;
import com.freethinking.beats.sdk.data.Authorization;
import com.freethinking.beats.sdk.data.AuthorizationRequest;
import com.freethinking.beats.sdk.data.Me;
import com.freethinking.beats.sdk.mappers.AuthorizationMapper;
import com.freethinking.beats.sdk.mappers.MeMapper;
import com.freethinking.beats.sdk.network.NetworkAdapter;
import com.freethinking.beats.sdk.network.NetworkParts;
import com.freethinking.beats.sdk.network.UrlFactory;
import com.freethinking.beats.sdk.utility.ApplicationData;

public class LoginActivity extends Activity {

	/*public static final String CALLBACK_URI = "CallBackUri";*/

	public final static String BEATS_MUSIC_CALLBACK = "beatsmusiccallback://com.example.beatsmusicsampleapp;";

	protected WebView webView;

    protected Me me;
    protected Authorization authorization;
    protected MeNetworkRequest networkRequest;
    protected AuthNetworkRequest authNetworkRequest;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //BEATS_MUSIC_CALLBACK = getIntent().getExtras().getString(CALLBACK_URI);
        
        me = new Me();
        authorization = new Authorization();
        
        webView = (WebView) findViewById(R.id.activity_login_web_view);
        webView.setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {}
            
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
            	boolean isOverridedUrl = false;
            	/**
            	 * url over here:
            	 * url : beatsmusiccallback://com.example.beatsmusicsampleapp/services.php?
            	 * scope=umMa&access_token=fy95abr37hpkxxwp9akdwh7n&token_type=bearer&expires_in=1209600&state=
            	 */
            	//Intent returnIntent = new Intent();
            	if (url.contains(BEATS_MUSIC_CALLBACK) && url.contains("access_token")) {
            		isOverridedUrl = true;
            		
            		Uri uri = Uri.parse(url);
            		String accessToken = uri.getQueryParameter("access_token");
            		String expiresIn = uri.getQueryParameter("expires_in");
            		int accessExpiresAt = expiresIn == null ? 0 : Integer.parseInt(expiresIn);
            				
            		String preferencesKey = ApplicationData.getStorePreferencesKey(getApplicationContext());
            		getSharedPreferences(preferencesKey, MODE_PRIVATE).edit().putString("access_token", 
            			accessToken).commit();
					getSharedPreferences(preferencesKey, MODE_PRIVATE).edit().putLong("access_expires_at", 
            			System.currentTimeMillis() + (1000 * accessExpiresAt)).commit();

            		networkRequest = new MeNetworkRequest(getApplicationContext());
            		networkRequest.execute(UrlFactory.me());

            		//returnIntent.putExtra("access_token", accessToken);
            	}
            	
            	//setResult(RESULT_OK, returnIntent);
            	//finish();            	

                return isOverridedUrl;
            	/*if (url.contains(BEATS_MUSIC_CALLBACK) && url.contains("code")) {
                    Uri uri = Uri.parse(url);
                    String code = uri.getQueryParameter("code");
                    String state = uri.getQueryParameter("state");
                    String scope = uri.getQueryParameter("scope");

                    String preferencesKey = ApplicationData.getStorePreferencesKey(getApplicationContext());
                    getSharedPreferences(preferencesKey, MODE_PRIVATE).edit().putString("access_code", code).commit();
                    getSharedPreferences(preferencesKey, MODE_PRIVATE).edit().putString("user_state", state).commit();
                    getSharedPreferences(preferencesKey, MODE_PRIVATE).edit().putString("access_code_scope", scope).commit();

                    AuthorizationRequest body = new AuthorizationRequest(UrlFactory.clientSecret(getApplicationContext()), 
                    		UrlFactory.clientID(getApplicationContext()), BEATS_MUSIC_CALLBACK, code, 
                    		"authorization_code", false);

                    Log.d("ClientId : ", body.getClientId() + "");
                    Log.d("ClientSecret : ", body.getClientSecret() + "");
                    Log.d("Code : ", body.getCode() + "");
                    Log.d("GrantType : ", body.getGrantType() + "");
                    Log.d("RedirectUri : ", body.getRedirectUri() + "");
                    
                    authNetworkRequest = new AuthNetworkRequest(getApplicationContext(), body);
                    authNetworkRequest.execute(UrlFactory.obtainToken());
                    return true;
                } else {
                    return false;
                }*/
            }
        });
        /*webView.loadUrl("https://partner.api.beatsmusic.com/v1/oauth2/authorize?response_type=code&redirect_uri=" 
        		+ BEATS_MUSIC_CALLBACK + "&client_id=" + UrlFactory.clientID(this));*/
        webView.loadUrl("https://partner.api.beatsmusic.com/v1/oauth2/authorize?response_type=token&redirect_uri=" 
        + BEATS_MUSIC_CALLBACK + "&client_id=" + UrlFactory.clientID(this));
    }

    public void completeSignIn() {
        Toast.makeText(this, ApplicationData.getApplicationWelcome(getApplicationContext()), Toast.LENGTH_SHORT).show();
        Intent returnIntent = new Intent();
        String preferencesKey = ApplicationData.getStorePreferencesKey(getApplicationContext());
        returnIntent.putExtra("access_code", getSharedPreferences(preferencesKey, MODE_PRIVATE).getString("access_code", ""));
        returnIntent.putExtra("user_state", getSharedPreferences(preferencesKey, MODE_PRIVATE).getString("user_state", ""));
        returnIntent.putExtra("access_code_scope", getSharedPreferences(preferencesKey, MODE_PRIVATE).getString("access_code_scope", ""));
        returnIntent.putExtra("access_token", getSharedPreferences(preferencesKey, MODE_PRIVATE).getString("access_token", ""));
        returnIntent.putExtra("refresh_token", getSharedPreferences(preferencesKey, MODE_PRIVATE).getString("refresh_token", ""));
        returnIntent.putExtra("access_expires_at", getSharedPreferences(preferencesKey, MODE_PRIVATE).getLong("access_expires_at", System.currentTimeMillis()));
        returnIntent.putExtra("user_id", getSharedPreferences(preferencesKey, MODE_PRIVATE).getString("user_id", ""));
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    protected class MeNetworkRequest extends NetworkAdapter {

        public MeNetworkRequest(Context context) {
            super(context, new MeMapper(), NetworkParts.RequestType.GET, new HashMap<String, String>(), me, BEATS_MUSIC_CALLBACK);
        }

        @Override
        protected Boolean authRequired() {
            return true;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            String preferencesKey = ApplicationData.getStorePreferencesKey(getApplicationContext());
            getSharedPreferences(preferencesKey, MODE_PRIVATE).edit().putString("user_id", me.getResult().getUserContext()).commit();
            completeSignIn();
        }
    }

    protected class AuthNetworkRequest extends NetworkAdapter {

        public AuthNetworkRequest(Context context, AuthorizationRequest body) {
            super(context, new AuthorizationMapper(), NetworkParts.RequestType.POST, 
            		new HashMap<String, String>(), body, authorization);
        }

        @Override
        protected void onPostExecute(String result) {
        	if (result == null) {
        		return;
        	}
            super.onPostExecute(result);

            String preferencesKey = ApplicationData.getStorePreferencesKey(getApplicationContext());
            getSharedPreferences(preferencesKey, MODE_PRIVATE).edit().putString("access_token", authorization.getResult().getAccessToken()).commit();
            getSharedPreferences(preferencesKey, MODE_PRIVATE).edit().putString("refresh_token", authorization.getResult().getRefreshToken()).commit();
            int expiresIn = authorization.getResult().getExpiresIn() == null ? 0 : authorization.getResult().getExpiresIn();
			getSharedPreferences(preferencesKey, MODE_PRIVATE).edit().putLong("access_expires_at", System.currentTimeMillis() + (1000 * expiresIn )).commit();

            networkRequest = new MeNetworkRequest(context);
            networkRequest.execute(UrlFactory.me());
        }
    }
}
