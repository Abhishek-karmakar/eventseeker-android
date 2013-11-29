package com.wcities.eventseeker.util;

import android.os.AsyncTask;
import android.os.Build;

public class AsyncTaskUtil {

	private static final String TAG = AsyncTaskUtil.class.getName();

	public static <Params, T extends AsyncTask<Params, ?, ?>> void executeAsyncTask(T asyncTask, 
			boolean runParallel, Params... params) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && runParallel) {
			asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
			
		} else {
			asyncTask.execute(params);
		}
	}
}
