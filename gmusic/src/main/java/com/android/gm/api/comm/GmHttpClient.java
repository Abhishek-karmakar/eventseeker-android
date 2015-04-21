package com.android.gm.api.comm;

import org.apache.http.HttpEntity;

import com.android.gm.api.lib.SyncHttpClient;

import android.content.Context;

public class GmHttpClient extends SyncHttpClient
{

	public GmHttpClient()
	{
		super();
	}

	public String post(Context context, String url, HttpEntity entity, String contentType)
	{
		post(context, url, entity, contentType, responseHandler);
		return result;
	}

	@Override
	public String onRequestFailed(Throwable error, String content)
	{
		return null;
	}
}
