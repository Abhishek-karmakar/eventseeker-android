package com.wcities.eventseeker.util;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;

import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.interfaces.ConnectionFailureListener;

public class AsyncTaskUtil {

	private static final String TAG = AsyncTaskUtil.class.getName();

	public static <Params, T extends AsyncTask<Params, ?, ?>> boolean executeAsyncTask(T asyncTask, 
			boolean runParallel, Params... params) {
		
		ConnectionFailureListener connectionFailureListener = EventSeekr.getConnectionFailureListener();
		if (connectionFailureListener != null) {
			if (!NetworkUtil.getConnectivityStatus((Context) connectionFailureListener)) {
				connectionFailureListener.onConnectionFailure();
				return false;
			}
		}
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && runParallel) {
			asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
			
		} else {
			asyncTask.execute(params);
		}
		return true;
	}
}
