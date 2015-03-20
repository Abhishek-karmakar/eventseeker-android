package com.freethinking.beats.sdk.network;

import android.content.Context;
import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freethinking.beats.sdk.data.Authorization;
import com.freethinking.beats.sdk.data.AuthorizationRequest;
import com.freethinking.beats.sdk.data.BaseJson;
import com.freethinking.beats.sdk.login.LoginActivity;
import com.freethinking.beats.sdk.mappers.AuthorizationMapper;
import com.freethinking.beats.sdk.mappers.CommonMapper;
import com.freethinking.beats.sdk.utility.ApplicationData;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

public class NetworkParts {

    public enum RequestType {
        GET,
        PUT,
        POST,
        DELETE
    }

    protected Boolean authRequired;
    protected RequestType type;
    protected String url;
    protected Map<String, String> headers;
    protected BaseJson response;
    protected CommonMapper mapper;
    protected Context context;
    protected boolean canceled;
    protected String redirectUri;

    /**
     * This is used only by the HTTP Client. Do not modify this.
     */
    private StringEntity body;

    public NetworkParts(Context context, CommonMapper mapper, RequestType type, Map<String, String> headers, BaseJson json) {
        super();
        this.authRequired = false;
        this.mapper = mapper;
        this.type = type;
        this.headers = headers;
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json");
        this.response = json;
        this.context = context;
    }

    public NetworkParts(Context context, CommonMapper mapper, RequestType type, Map<String, String> headers, BaseJson json, String redirectUri) {
    	this(context, mapper, type, headers, json);
    	this.redirectUri = redirectUri;
    }

    public NetworkParts(Context context, CommonMapper mapper, RequestType type, Map<String, String> headers, String body, BaseJson response) {
        this(context, mapper, type, headers, response);
        try {
            this.body = new StringEntity(body);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public NetworkParts(Context context, CommonMapper mapper, RequestType type, Map<String, String> headers, BaseJson body, BaseJson response) {
        this(context, mapper, type, headers, response);
        try {
            ObjectMapper jsonSerializer = new ObjectMapper();
            String baseJsonString = jsonSerializer.writeValueAsString(body);
            this.body = new StringEntity(baseJsonString);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public Boolean authRequired() {
        return authRequired;
    }

    public void setAuthRequired(Boolean bool) {
        authRequired = bool;
    }

    private void makeRefreshRequest(AuthorizationRequest authorizationRequest) {
        try {
            Authorization authorization = new Authorization();
            ObjectMapper jsonSerializer = new ObjectMapper();
            String baseJsonString = jsonSerializer.writeValueAsString(authorizationRequest);
            StringEntity body = new StringEntity(baseJsonString);

            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response;
            String responseString;

            HttpPost post = new HttpPost(UrlFactory.obtainToken());
            for (String key : headers.keySet()) {
                post.addHeader(key, headers.get(key));
            }
            post.setEntity(body);
            response = httpclient.execute(post);

            StatusLine statusLine = response.getStatusLine();

            if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                out.close();
                responseString = out.toString();
            } else {
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }

            authorization.fillIn(new AuthorizationMapper().parseJson(responseString));

            String preferencesKey = ApplicationData.getStorePreferencesKey(context.getApplicationContext());
            context.getSharedPreferences(preferencesKey, Context.MODE_PRIVATE).edit().putString("access_token", authorization.getResult().getAccessToken()).commit();
            context.getSharedPreferences(preferencesKey, Context.MODE_PRIVATE).edit().putString("refresh_token", authorization.getResult().getRefreshToken()).commit();
            context.getSharedPreferences(preferencesKey, Context.MODE_PRIVATE).edit().putLong("access_expires_at", System.currentTimeMillis() + (1000 * authorization.getResult().getExpiresIn())).commit();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            String answer = e.getLocalizedMessage();
            Log.d("network manager", answer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cancel() {

    }

    public void before() {

    }

    public String during(String url) {
        if (this.url == null) {
            this.url = url;
        }
        String responseString = null;
        if (!canceled) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response;

            if (authRequired()) {
                String preferencesKey = ApplicationData.getStorePreferencesKey(context.getApplicationContext());
                Long accessExpires = context.getSharedPreferences(preferencesKey, Context.MODE_PRIVATE).getLong("access_expires_at", System.currentTimeMillis());
                if (accessExpires < System.currentTimeMillis()) {
                    String code = context.getSharedPreferences(preferencesKey, Context.MODE_PRIVATE).getString("refresh_token", "");
                    AuthorizationRequest body = new AuthorizationRequest(UrlFactory.clientSecret(context), UrlFactory.clientID(context), 
                    		redirectUri, code, "refresh_token", true);
                    makeRefreshRequest(body);
                }

                headers.put("Authorization", "Bearer " + 
                context.getSharedPreferences(preferencesKey, Context.MODE_PRIVATE).getString("access_token", ""));
            }

            try {
                switch (type) {
                    case GET:
                        HttpGet get = new HttpGet(url);
                        for (String key : headers.keySet()) {
                            get.addHeader(key, headers.get(key));
                        }
                        response = httpclient.execute(get);
                        break;
                    case PUT:
                        HttpPut put = new HttpPut(url);
                        for (String key : headers.keySet()) {
                            put.addHeader(key, headers.get(key));
                        }
                        put.setEntity(body);
                        response = httpclient.execute(put);
                        break;
                    case POST:
                        HttpPost post = new HttpPost(url);
                        for (String key : headers.keySet()) {
                            post.addHeader(key, headers.get(key));
                        }
                        post.setEntity(body);
                        response = httpclient.execute(post);
                        break;
                    case DELETE:
                        HttpDelete delete = new HttpDelete(url);
                        for (String key : headers.keySet()) {
                            delete.addHeader(key, headers.get(key));
                        }
                        response = httpclient.execute(delete);
                        break;
                    default:
                        response = httpclient.execute(new HttpGet(url));
                }
                StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    out.close();
                    responseString = out.toString();
                } else {
                    response.getEntity().getContent().close();
                    throw new IOException(statusLine.getReasonPhrase());
                }
            } catch (ClientProtocolException e) {
                //TODO Handle problems...
            } catch (IOException e) {
                String answer = e.getLocalizedMessage();
                Log.d("network manager", answer);
            }
        }
        return responseString;
    }

    public void after(String jsonString) {
        try {
            response.fillIn(mapper.parseJson(jsonString));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Boolean loggedIn(Context context) {
        String preferencesKey = ApplicationData.getStorePreferencesKey(context.getApplicationContext());
        String accessToken = context.getSharedPreferences(preferencesKey, Context.MODE_PRIVATE).getString("access_token", null);
        return accessToken != null;
    }

    public static String userId(Context context) {
        String preferencesKey = ApplicationData.getStorePreferencesKey(context.getApplicationContext());
        return context.getSharedPreferences(preferencesKey, Context.MODE_PRIVATE).getString("user_id", null);
    }

    public static String accessToken(Context context) {
        String preferencesKey = ApplicationData.getStorePreferencesKey(context.getApplicationContext());
        return context.getSharedPreferences(preferencesKey, Context.MODE_PRIVATE).getString("access_token", null);
    }

    public static String refreshToken(Context context) {
        String preferencesKey = ApplicationData.getStorePreferencesKey(context.getApplicationContext());
        return context.getSharedPreferences(preferencesKey, Context.MODE_PRIVATE).getString("refresh_token", null);
    }
}
