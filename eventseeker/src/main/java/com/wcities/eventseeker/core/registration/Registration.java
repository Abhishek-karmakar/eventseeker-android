package com.wcities.eventseeker.core.registration;

import android.os.Bundle;

import com.wcities.eventseeker.api.UserInfoApi.LoginType;
import com.wcities.eventseeker.app.EventSeekr;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;

import java.io.IOException;

public abstract class Registration {

	protected EventSeekr eventSeekr;

	public Registration(EventSeekr eventSeekr) {
		this.eventSeekr = eventSeekr;
	}
	
	public abstract int perform() throws ClientProtocolException, IOException, JSONException;
	
	public interface RegistrationListener {
    	public void onRegistration(LoginType loginType, Bundle args, boolean addToBackStack);
    }

	public interface RegistrationErrorListener {
		public void onErrorOccured(int errorCode);
	}
}
