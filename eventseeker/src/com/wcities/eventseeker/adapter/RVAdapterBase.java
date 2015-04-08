package com.wcities.eventseeker.adapter;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.AdapterDataObserver;
import android.util.Log;

public abstract class RVAdapterBase<U extends RecyclerView.ViewHolder> extends Adapter<U> {
	
	private static final String TAG = RVAdapterBase.class.getSimpleName();
	
	private AdapterDataObserver adapterDataObserver;

	/**
	 * Need to unregister manually because otherwise using same adapter on orientation change results in
	 * multiple time registrations w/o unregistration (from recycler view's internal method setAdapter(), 
	 * due to which we need to manually call unregisterAdapterDataObserver if it tries to register with new 
	 * observer when already some older observer is registered. W/o having this results in multiple observers 
	 * holding cardview & imgView memory. We need to override this method since android doesn't provide any 
	 * getObservers() like method.
	 */
	@Override
	public void registerAdapterDataObserver(AdapterDataObserver observer) {
		if (adapterDataObserver != null) {
			try {
				//Log.d(TAG, "unregisterAdapterDataObserver()");
				unregisterAdapterDataObserver(adapterDataObserver);
				
			} catch (IllegalStateException e) {
				Log.e(TAG, "RecyclerViewDataObserver was not registered");
			}
		}
        super.registerAdapterDataObserver(observer);
        adapterDataObserver = observer;
    }
}
