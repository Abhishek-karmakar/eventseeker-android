package com.wcities.eventseeker.core.registration;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;

import com.wcities.eventseeker.app.EventSeekr;

public abstract class Registration {

	protected EventSeekr eventSeekr;

	public Registration(EventSeekr eventSeekr) {
		this.eventSeekr = eventSeekr;
	}
	
	public abstract int register() throws ClientProtocolException, IOException, JSONException;
}
