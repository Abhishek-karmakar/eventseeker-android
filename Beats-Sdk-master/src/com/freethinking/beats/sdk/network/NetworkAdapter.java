package com.freethinking.beats.sdk.network;

import android.content.Context;
import android.os.AsyncTask;

import com.freethinking.beats.sdk.data.BaseJson;
import com.freethinking.beats.sdk.mappers.CommonMapper;
import com.freethinking.beats.sdk.utility.ApplicationData;

import java.util.Map;

public class NetworkAdapter extends AsyncTask<String, Void, String> {

    private NetworkParts parts;

    protected Context context;

    public NetworkAdapter(Context context, CommonMapper mapper, NetworkParts.RequestType type, Map<String, String> headers, BaseJson json) {
        super();
        this.context = context;
        parts = new NetworkParts(context, mapper, type, headers, json);
        parts.setAuthRequired(authRequired());
    }

    public NetworkAdapter(Context context, CommonMapper mapper, NetworkParts.RequestType type, Map<String, String> headers, String body, BaseJson response) {
        super();
        this.context = context;
        parts = new NetworkParts(context, mapper, type, headers, body, response);
        parts.setAuthRequired(authRequired());
    }

    public NetworkAdapter(Context context, CommonMapper mapper, NetworkParts.RequestType type, Map<String, String> headers, BaseJson body, BaseJson response) {
        super();
        this.context = context;
        parts = new NetworkParts(context, mapper, type, headers, body, response);
        parts.setAuthRequired(authRequired());
    }

    public static Boolean loggedIn(Context context) {
        String preferencesKey = ApplicationData.getStorePreferencesKey(context.getApplicationContext());
        String accessToken = context.getSharedPreferences(preferencesKey, Context.MODE_PRIVATE).getString("access_token", null);
        return accessToken != null;
    }

    protected Boolean authRequired() {
        return false;
    }

    @Override
    protected void onPreExecute() {
        parts.before();
    }

    @Override
    protected String doInBackground(String... uri) {
        return parts.during(uri[0]);
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        parts.after(result);
    }
}

