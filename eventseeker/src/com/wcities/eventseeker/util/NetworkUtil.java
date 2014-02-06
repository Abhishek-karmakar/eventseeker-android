package com.wcities.eventseeker.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkUtil {
	  
	public static boolean getConnectivityStatus(Context context) {
	
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	 
		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();	        
		if (null != activeNetwork) {
			return (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) 
	            		|| (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE);
		} 
        return false;
	}
}
