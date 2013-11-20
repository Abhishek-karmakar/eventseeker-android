package com.wcities.eventseeker.asynctask;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.ImageView;

import com.wcities.eventseeker.asynctask.AsyncLoadImg.ImgDetails;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.util.BitmapUtil;
import com.wcities.eventseeker.widget.EventseekerWidget;

public class AsyncLoadImg extends AsyncTask<Void, ImgDetails, Void> {

	/*private static final String TAG = "AsyncLoadImg";
	
	private ImageView imageView;
	private BitmapCache bitmapCache;
	private String key;
	private AdapterView adapterView;
	private int pos;
	
	*//**
	 * @param imageView
	 * @param key
	 *//*
	public AsyncLoadImg(ImageView imageView, String key) {
		this.imageView = imageView;
		this.key = key;
		this.bitmapCache = BitmapCache.getInstance();
	}

	*//**
	 * Use this constructor only for an adapterview
	 * @param imageView
	 * @param key
	 * @param adapterView applicable only when it's called by an adapterview (for example listView)
	 * @param pos item position for which this AsyncTask is going to load image. It's applicable only when called 
	 * by an adapterview (for example listView)
	 *//*
	public AsyncLoadImg(ImageView imageView, String key, AdapterView adapterView, int pos) {
		this.imageView = imageView;
		this.key = key;
		this.adapterView = adapterView;
		this.pos = pos;
		this.bitmapCache = BitmapCache.getInstance();
	}
	
	@Override
	protected Bitmap doInBackground(String... params) {
		//Log.i(TAG, "url for img = " + params[0]);
		Bitmap bitmap = BitmapUtil.getBitmap(params[0]);
		bitmapCache.addBitmapToMemoryCache(key, bitmap);
		return bitmap;
	}
	
	@Override
	protected void onPostExecute(Bitmap bitmap) {
		if (bitmap != null) {
			if (adapterView == null) {
				imageView.setImageBitmap(bitmap);
				
			} else {
				if (pos <= adapterView.getLastVisiblePosition() && pos >= adapterView.getFirstVisiblePosition()) {
					imageView.setImageBitmap(bitmap);
				}
			}
		}
	}*/
	
	private static final String TAG = "AsyncLoadImg";

	private static AsyncLoadImg asyncLoadImg;
	private BitmapCache bitmapCache;
	private List<ImgDetails> imgDetailsList;
	private Context context;

	protected class ImgDetails {
		
		private ImageView imageView;
		private String key;
		private AdapterView adapterView;
		private int pos;
		private Bitmap bitmap;
		private long id;
		private int widgetId;
		private AsyncLoadImageListener listener;
		
		/**
		 * list of urls in order of their priorities. First url will be attempted first. 
		 * On unsuccessful response remaining urls will be tried out in order.
		 */
		private List<String> urls;
		
		/**
		 * @return returns true if key, adapterView & pos are matching, otherwise false
		 */
		@Override
		public boolean equals(Object o) {
			ImgDetails imgDetails = (ImgDetails) o;
			if (key.equals(imgDetails.key) && adapterView == imgDetails.adapterView && 
					pos == imgDetails.pos) {
				return true;
			}
			return super.equals(o);
		}
	}
	
	public interface AsyncLoadImageListener {
		public void onImageLoaded();
		public void onImageCouldNotBeLoaded();
	}
	
	/**
	 * @param imageView
	 * @param key
	 */
	private AsyncLoadImg() {
		this.bitmapCache = BitmapCache.getInstance();
		imgDetailsList = new ArrayList<AsyncLoadImg.ImgDetails>();
	}
	
	public static AsyncLoadImg getInstance() {
		//Log.i(TAG, "getInstance()");
		if (asyncLoadImg == null || asyncLoadImg.getStatus() == Status.FINISHED) {
			synchronized (AsyncLoadImg.class) {
				if (asyncLoadImg == null || asyncLoadImg.getStatus() == Status.FINISHED) {
					Log.i(TAG, "create new asyncTask");
					asyncLoadImg = new AsyncLoadImg();
				}
			}
		}
		return asyncLoadImg;
	}
	
	public void loadImg(ImageView imageView, ImgResolution imgResolution, AdapterView adapterView, 
			int pos, BitmapCacheable bitmapCacheable) {
		//Log.i(TAG, "loadImg() for pos = " + pos);
		ImgDetails imgDetails = new ImgDetails();
		imgDetails.imageView = imageView;
		imgDetails.key = bitmapCacheable.getKey(imgResolution);
		imgDetails.adapterView = adapterView;
		imgDetails.pos = pos;
		imgDetails.urls = getUrlsInOrder(bitmapCacheable, imgResolution);
		
		startExecution(imgDetails);
	}
	
	public void loadImg(ImageView imageView, ImgResolution imgResolution, BitmapCacheable bitmapCacheable) {
		//Log.i(TAG, "loadImg()");
		ImgDetails imgDetails = new ImgDetails();
		imgDetails.imageView = imageView;
		imgDetails.key = bitmapCacheable.getKey(imgResolution);
		imgDetails.urls = getUrlsInOrder(bitmapCacheable, imgResolution);
		
		startExecution(imgDetails);
	}
	
	public void loadImg(Context context, ImgResolution imgResolution, BitmapCacheable bitmapCacheable, int widgetId) {
		//Log.i(TAG, "loadImg()");
		ImgDetails imgDetails = new ImgDetails();
		this.context = context;
		imgDetails.key = bitmapCacheable.getKey(imgResolution);
		imgDetails.urls = getUrlsInOrder(bitmapCacheable, imgResolution);
		imgDetails.id = ((Event)bitmapCacheable).getId();
		imgDetails.widgetId = widgetId;
		
		startExecution(imgDetails);
	}
	
	public void loadImg(ImageView imageView, ImgResolution imgResolution, BitmapCacheable bitmapCacheable, 
			AsyncLoadImageListener listener) {
		//Log.i(TAG, "loadImg()");
		ImgDetails imgDetails = new ImgDetails();
		imgDetails.imageView = imageView;
		imgDetails.key = bitmapCacheable.getKey(imgResolution);
		imgDetails.urls = getUrlsInOrder(bitmapCacheable, imgResolution);
		imgDetails.listener = listener;
		
		startExecution(imgDetails);
	}
	
	public void loadImg(ImgResolution imgResolution, BitmapCacheable bitmapCacheable, AsyncLoadImageListener listener) {
		//Log.i(TAG, "loadImg()");
		ImgDetails imgDetails = new ImgDetails();
		imgDetails.key = bitmapCacheable.getKey(imgResolution);
		imgDetails.urls = getUrlsInOrder(bitmapCacheable, imgResolution);
		imgDetails.listener = listener;
		
		startExecution(imgDetails);
	}
	
	private void startExecution(ImgDetails imgDetails) {
		synchronized (imgDetailsList) {
			//Log.i(TAG, "asyncLoadImg.getStatus() = " + asyncLoadImg.getStatus());
			if (asyncLoadImg.getStatus() == Status.PENDING) {
				//Log.i(TAG, "status is pending, execute now");
				asyncLoadImg.imgDetailsList.add(0, imgDetails);
				asyncLoadImg.execute();
				
			} else if (asyncLoadImg.getStatus() == Status.FINISHED) {
				//Log.i(TAG, "status is finished, get new instance & execute now");
				asyncLoadImg = getInstance();
				asyncLoadImg.imgDetailsList.add(0, imgDetails);
				asyncLoadImg.execute();
				
			} else {
				asyncLoadImg.imgDetailsList.add(0, imgDetails);
				//Log.i(TAG, "don't execute");
			}
		}
	}

	/*@Override
	protected void onPreExecute() {
		super.onPreExecute();
		Log.i(TAG, "onPreExecute()");
	}*/
	
	@Override
	protected Void doInBackground(Void... params) {
		//Log.i(TAG, "doInBackground(), imgDetailsList size = " + imgDetailsList.size());
		ImgDetails imgDetails;
		
		while (!imgDetailsList.isEmpty()) {
			//Log.i(TAG, "in while, next");
			imgDetails = imgDetailsList.remove(0);
			Bitmap bitmap = bitmapCache.getBitmapFromMemCache(imgDetails.key);
			
			if (bitmap == null) {
				for (Iterator<String> iterator = imgDetails.urls.iterator(); iterator.hasNext();) {
					String url = iterator.next();
					Log.i(TAG, "url for img = " + url);
					try {
						imgDetails.bitmap = BitmapUtil.getBitmap(url);
						//Log.i(TAG, "done getting bitmap");
						break;
						
					} catch (FileNotFoundException e) {
						//Log.i(TAG, "MalformedURLException");
						e.printStackTrace();
						// continue for trying with next url
					}
				}
				
				//Log.i(TAG, "bitmap="+imgDetails.bitmap+", for pos="+imgDetails.pos);
				//Log.i(TAG, "addBitmapToMemoryCache");
				bitmapCache.addBitmapToMemoryCache(imgDetails.key, imgDetails.bitmap);
				//Log.i(TAG, "publishProgress");
				
			} else {
				imgDetails.bitmap = bitmap;
			}
			publishProgress(imgDetails);
		}
		//Log.i(TAG, "finished while");
		return null;
	}
	
	@Override
	protected void onProgressUpdate(ImgDetails... values) {
		//Log.i(TAG, "onProgressUpdate()");
		ImgDetails imgDetails = values[0];
		if (imgDetails.bitmap != null) {
			//Log.i(TAG, "bitmap is not null");
			
			if (imgDetails.adapterView == null) {
				if (imgDetails.widgetId == 0) {
					//Log.i(TAG, "adapterView is null, imgDetails.imageView = " + imgDetails.imageView);
					if (imgDetails.imageView != null) {
						imgDetails.imageView.setImageBitmap(imgDetails.bitmap);
					}
					if (imgDetails.listener != null) {
						imgDetails.listener.onImageLoaded();
					}
					
				} else {
					// for widgets
					Intent intent = new Intent();
					intent.setAction(EventseekerWidget.WIDGET_UPDATE)
					.putExtra(BundleKeys.WIDGET_UPDATE_TYPE, EventseekerWidget.UpdateType.REFRESH_IMAGE)
					.putExtra(BundleKeys.EVENT_ID, imgDetails.id)
					.putExtra(BundleKeys.WIDGET_ID, imgDetails.widgetId);
					
					context.sendBroadcast(intent);
				}
				
			} else {
				/*Log.i(TAG, "onProgressUpdate() else, pos="+imgDetails.pos + ", lastPos=" + imgDetails.adapterView.getLastVisiblePosition()
						+ ", firstPos=" + imgDetails.adapterView.getFirstVisiblePosition());*/
				if (imgDetails.pos <= imgDetails.adapterView.getLastVisiblePosition() && 
						imgDetails.pos >= imgDetails.adapterView.getFirstVisiblePosition()) {
					imgDetails.imageView.setImageBitmap(imgDetails.bitmap);
					//Log.i(TAG, "onProgressUpdate() bitmap set");
				}
			}
			
		} else if (imgDetails.listener != null) {
			imgDetails.listener.onImageCouldNotBeLoaded();
		}
	}
	
	private List<String> getUrlsInOrder(BitmapCacheable bitmapCacheable, ImgResolution imgResolution) {
		List<String> urls = new ArrayList<String>();
		
		switch (imgResolution) {
		
		case MOBILE:
			urls.add(bitmapCacheable.getMobiResImgUrl());
			urls.add(bitmapCacheable.getLowResImgUrl());
			urls.add(bitmapCacheable.getHighResImgUrl());
			break;
			
		case LOW:
			urls.add(bitmapCacheable.getLowResImgUrl());
			urls.add(bitmapCacheable.getHighResImgUrl());
			urls.add(bitmapCacheable.getMobiResImgUrl());
			break;

		case HIGH:
			urls.add(bitmapCacheable.getHighResImgUrl());
			//Log.i(TAG, "high res=" + bitmapCacheable.getHighResImgUrl());
			urls.add(bitmapCacheable.getLowResImgUrl());
			//Log.i(TAG, "low res=" + bitmapCacheable.getLowResImgUrl());
			urls.add(bitmapCacheable.getMobiResImgUrl());
			//Log.i(TAG, "mobi res=" + bitmapCacheable.getMobiResImgUrl());
			break;
			
		default:
			urls.add(bitmapCacheable.getMobiResImgUrl());
			break;
		}
		return urls;
	}
}
