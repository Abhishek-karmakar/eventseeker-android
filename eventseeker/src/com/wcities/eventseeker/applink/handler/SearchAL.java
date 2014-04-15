package com.wcities.eventseeker.applink.handler;

import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.applink.interfaces.ESIProxyALM;

public class SearchAL extends ESIProxyALM {

	private static final String TAG = SearchAL.class.getName();
	private static SearchAL instance;
	private EventSeekr context;

	public SearchAL(EventSeekr context) {
		this.context = context;
	}

	public static ESIProxyALM getInstance(EventSeekr context) {
		if (instance == null) {
			instance = new SearchAL(context);
		}
		return instance;
	}
	
	@Override
	public void onStartInstance() {
		
	}
	
	@Override
	public void onStopInstance() {
		
	}

}
