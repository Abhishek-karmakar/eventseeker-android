package com.wcities.eventseeker.gear.interfaces;

public interface SG2Api {
	
	public static enum SG2ApiCallType {
		myevents;
		
		public static SG2ApiCallType getCallType(String strCallType) {
			SG2ApiCallType[] callTypes = SG2ApiCallType.values();
			for (int i = 0; i < callTypes.length; i++) {
				if (callTypes[i].name().equals(strCallType)) {
					return callTypes[i];
				}
			}
			return null;
		}
	}
	
	public byte[] execute(Integer... params);
}
