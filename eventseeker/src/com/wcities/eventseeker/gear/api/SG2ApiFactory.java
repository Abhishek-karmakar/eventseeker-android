package com.wcities.eventseeker.gear.api;

import java.util.HashMap;
import java.util.Map;

import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.gear.interfaces.SG2Api;
import com.wcities.eventseeker.gear.interfaces.SG2Api.SG2ApiCallType;

public class SG2ApiFactory {
	
	private static Map<SG2ApiCallType, SG2Api> sg2ApiMap = new HashMap<SG2Api.SG2ApiCallType, SG2Api>();
	
	public static final int NOT_SPECIFIED = -1;
	
	public static SG2Api getSG2ApiInstance(EventSeekr eventSeekr, SG2ApiCallType callType, int eventIndex) {
		SG2Api sg2Api = null;
		
		if (eventIndex != NOT_SPECIFIED && sg2ApiMap.containsKey(callType)) {
			sg2Api = sg2ApiMap.get(callType);
			return sg2Api;
		}
		
		switch (callType) {
		
		case myevents:
			sg2Api = new SG2LoadMyEvents(eventSeekr);
			break;

		default:
			break;
		}
		
		sg2ApiMap.put(callType, sg2Api);
		
		return sg2Api;
	}
	
	public static void clearMap() {
		sg2ApiMap.clear();
	}
}
