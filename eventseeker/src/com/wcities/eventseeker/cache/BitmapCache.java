package com.wcities.eventseeker.cache;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.util.Log;

public class BitmapCache {
	
	private static final String TAG = "BitmapCache";
	
	private static BitmapCache bitmapCache;
	private LruCache<String, Bitmap> mMemoryCache;
	
	private BitmapCache() {
		// Get max available VM memory, exceeding this amount will throw an
	    // OutOfMemory exception. Stored in kilobytes as LruCache takes an
	    // int in its constructor.
	    final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

	    // Use 1/8th of the available memory for this memory cache.
	    final int cacheSize = maxMemory / 8;
	    //Log.i(TAG, "cacheSize="+cacheSize);
	    mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
	        @Override
	        protected int sizeOf(String key, Bitmap bitmap) {
	            // The cache size will be measured in kilobytes rather than
	            // number of items.
	            return bitmap.getRowBytes() * bitmap.getHeight() / 1024;
	        }
	    };
	}
	
	public static BitmapCache getInstance() {
		if (bitmapCache == null) {
			synchronized (BitmapCache.class) {
				if (bitmapCache == null) {
					bitmapCache = new BitmapCache();
				}
			}
		}
		return bitmapCache;
	}
	
	public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
		synchronized (mMemoryCache) {
			if (getBitmapFromMemCache(key) == null) {
				if (bitmap != null) {
					mMemoryCache.put(key, bitmap);
				}
		    }
		}
		//Log.i(TAG, "cache size = " + mMemoryCache.size());
	}
	
	public Bitmap getBitmapFromMemCache(String key) {
	    return mMemoryCache.get(key);
	}
}
