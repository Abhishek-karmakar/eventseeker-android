package com.wcities.eventseeker.core.registration;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;

public interface IRegistration {
	public int register() throws ClientProtocolException, IOException, JSONException;
}
