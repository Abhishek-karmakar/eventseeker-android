package com.wcities.eventseeker.asynctask;

import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.LayoutManager;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.ImageView;

import com.wcities.eventseeker.asynctask.AsyncLoadImg.ImgDetails;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.BitmapUtil;
import com.wcities.eventseeker.widget.EventseekerWidget;

public class AsyncLoadImg extends AsyncTask<Void, ImgDetails, Void> {

	private static final String TAG = "AsyncLoadImg";
	
	private static final int MAX_PENDING_REQUEST_LIMIT = 15;

	private static AsyncLoadImg asyncLoadImg;
	private BitmapCache bitmapCache;
	private List<ImgDetails> imgDetailsList;
	private Context context;

	protected class ImgDetails {
		
		private ImageView imageView;
		private String key;
		private AdapterView adapterView;
		private WeakReference<RecyclerView> recyclerView;
		private int pos;
		private Bitmap bitmap;
		private long id;
		private int widgetId;
		private AsyncLoadImageListener listener;
		/**
		 * 22-01-2015:
		 * The isFBUserProfilePic is added because, for friendsactivity screen, we need to download facebook 
		 * profile image of friends by id, for that facebook api call is available (refer FBUtil's 
		 * getFriendImgUrl()). But for that call using normal image downloading code following error was 
		 * occurring:"skia SkImageDecoder::Factory returned null android" So, by referring post from : 
		 * "http://stackoverflow.com/questions/23559736/android-skimagedecoderfactory-returned-null-error"
		 * changed implementation for downloading fb user profile pic and hence to judge whether to download
		 * image for fb user pic or normal(WCities) images added this boolean flag
		 */
		private boolean isFBUserProfilePic;
        private boolean scaleDown;
		
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
		imgDetails.urls = BitmapUtil.getUrlsInOrder(bitmapCacheable, imgResolution);
		
		startExecution(imgDetails);
	}

	public void loadFBUserImg(ImageView imageView, ImgResolution imgResolution, AdapterView adapterView, 
			int pos, BitmapCacheable bitmapCacheable) {
		//Log.i(TAG, "loadImg() for pos = " + pos);
		ImgDetails imgDetails = new ImgDetails();
		imgDetails.imageView = imageView;
		imgDetails.key = bitmapCacheable.getKey(imgResolution);
		imgDetails.adapterView = adapterView;
		imgDetails.pos = pos;
		imgDetails.isFBUserProfilePic = true;
		imgDetails.urls = BitmapUtil.getUrlsInOrder(bitmapCacheable, imgResolution);
		
		startExecution(imgDetails);
		
	}
	
	public void loadImg(ImageView imageView, ImgResolution imgResolution, WeakReference<RecyclerView> recyclerView, 
			int pos, BitmapCacheable bitmapCacheable) {
		//Log.i(TAG, "loadImg() for pos = " + pos);
		ImgDetails imgDetails = new ImgDetails();
		imgDetails.imageView = imageView;
		imgDetails.key = bitmapCacheable.getKey(imgResolution);
		imgDetails.recyclerView = recyclerView;
		imgDetails.pos = pos;
		imgDetails.urls = BitmapUtil.getUrlsInOrder(bitmapCacheable, imgResolution);
		
		startExecution(imgDetails);
	}

    public void loadImg(ImageView imageView, ImgResolution imgResolution, WeakReference<RecyclerView> recyclerView,
                        int pos, BitmapCacheable bitmapCacheable, boolean scaleDown) {
        //Log.i(TAG, "loadImg() for pos = " + pos);
        ImgDetails imgDetails = new ImgDetails();
        imgDetails.imageView = imageView;
        imgDetails.key = bitmapCacheable.getKey(imgResolution);
        imgDetails.recyclerView = recyclerView;
        imgDetails.pos = pos;
        imgDetails.urls = BitmapUtil.getUrlsInOrder(bitmapCacheable, imgResolution);
        imgDetails.scaleDown = scaleDown;

        startExecution(imgDetails);
    }
	
	public void loadImg(ImageView imageView, ImgResolution imgResolution, BitmapCacheable bitmapCacheable) {
		//Log.i(TAG, "loadImg()");
		ImgDetails imgDetails = new ImgDetails();
		imgDetails.imageView = imageView;
		imgDetails.key = bitmapCacheable.getKey(imgResolution);
		imgDetails.urls = BitmapUtil.getUrlsInOrder(bitmapCacheable, imgResolution);
		
		startExecution(imgDetails);
	}
	
	public void loadImg(Context context, ImgResolution imgResolution, BitmapCacheable bitmapCacheable, int widgetId) {
		//Log.i(TAG, "loadImg()");
		ImgDetails imgDetails = new ImgDetails();
		this.context = context;
		imgDetails.key = bitmapCacheable.getKey(imgResolution);
		imgDetails.urls = BitmapUtil.getUrlsInOrder(bitmapCacheable, imgResolution);
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
		imgDetails.urls = BitmapUtil.getUrlsInOrder(bitmapCacheable, imgResolution);
		imgDetails.listener = listener;
		
		startExecution(imgDetails);
	}
	
	public void loadImg(ImgResolution imgResolution, BitmapCacheable bitmapCacheable, AsyncLoadImageListener listener) {
		//Log.i(TAG, "loadImg()");
		ImgDetails imgDetails = new ImgDetails();
		imgDetails.key = bitmapCacheable.getKey(imgResolution);
		imgDetails.urls = BitmapUtil.getUrlsInOrder(bitmapCacheable, imgResolution);
		imgDetails.listener = listener;
		
		startExecution(imgDetails);
	}
	
	private void startExecution(ImgDetails imgDetails) {
		//synchronized (AsyncLoadImg.class) {
		synchronized (imgDetailsList) {
			if (imgDetailsList.size() > MAX_PENDING_REQUEST_LIMIT) {
				imgDetailsList.remove(imgDetailsList.size() - 1);
			}
			//Log.i(TAG, "asyncLoadImg.getStatus() = " + asyncLoadImg.getStatus());
			if (asyncLoadImg.getStatus() == Status.PENDING) {
				//Log.i(TAG, "status is pending, execute now");
				asyncLoadImg.imgDetailsList.add(0, imgDetails);
				AsyncTaskUtil.executeAsyncTask(asyncLoadImg, true);
				
			} else if (asyncLoadImg.getStatus() == Status.FINISHED) {
				//Log.i(TAG, "status is finished, get new instance & execute now");
				asyncLoadImg = getInstance();
				asyncLoadImg.imgDetailsList.add(0, imgDetails);
				AsyncTaskUtil.executeAsyncTask(asyncLoadImg, true);
				
			} else {
				asyncLoadImg.imgDetailsList.add(0, imgDetails);
				//Log.i(TAG, "don't execute");
			}
		}
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		//Log.i(TAG, "doInBackground(), imgDetailsList size = " + imgDetailsList.size());
		ImgDetails imgDetails;
		
		while (!imgDetailsList.isEmpty()) {
			imgDetails = imgDetailsList.remove(0);
			
			/**
			 * If imageview is not currently visible for an adapterview, then skip loading bitmap for such 
			 * imageview
			 */
			/*if (imgDetails.adapterView != null && 
					(imgDetails.pos < imgDetails.adapterView.getFirstVisiblePosition() || 
					imgDetails.pos > imgDetails.adapterView.getLastVisiblePosition())) {
				Log.i(TAG, "continue, fp = " + imgDetails.adapterView.getFirstVisiblePosition() + 
						", lp = " + imgDetails.adapterView.getLastVisiblePosition());
				continue;
			}*/
			/*if (imgDetails.adapterView != null && imgDetails.imageView != null 
					&& !imgDetails.imageView.isShown()) {
				Log.d(TAG, "continue");
				continue;
			}*/
			
			Bitmap bitmap = bitmapCache.getBitmapFromMemCache(imgDetails.key);
			
			if (bitmap == null) {
				for (Iterator<String> iterator = imgDetails.urls.iterator(); iterator.hasNext();) {
					String url = iterator.next();
					Log.i(TAG, "url for img = " + url);
					try {
						if (!imgDetails.isFBUserProfilePic) {
							imgDetails.bitmap = BitmapUtil.getBitmap(url, imgDetails.scaleDown);
							
						} else {
							imgDetails.bitmap = BitmapUtil.getFBUserBitmap(url);
						}
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
		
		//Log.d(TAG, "end while");
		/**
		 * Following call to publishProgress() is required to start a new asynctask in following case.
		 * Say if new asynctask starts & it reaches this point out of while loop without calling
		 * onProgressUpdate() even once due to invisibility of imageviews for which images are to be loaded.
		 * [in code above where we have continue; statement].
		 * Now in this case asynctask status doesn't change to finished (remains running forever) & 
		 * hence startExecution() method cannot create new asynctask for loading new images.
		 * To prevent this we have following call which in turn in starting only crates new asynctask to 
		 * handle pending images to be loaded.
		 */
		//publishProgress();
		
		return null;
	}
	
	@Override
	protected void onProgressUpdate(ImgDetails... values) {
		/*if (values.length == 0) {
			synchronized (AsyncLoadImg.class) {
				*//**
				 * imgDetailsList size can be > 0 since priority of doInBackground() is less & hence after 
				 * leaving while loop of doInBackground() it's possible that call to onProgressUpdate() is delayed
				 * & during this time new images to be loaded are inserted into imgDetailsList.
				 *//*
				Log.d(TAG, "onProgressUpdate() values.length == 0");
				if (imgDetailsList.size() > 0) {
					Log.d(TAG, "onProgressUpdate() new asynctask");
					asyncLoadImg = new AsyncLoadImg();
					asyncLoadImg.imgDetailsList = imgDetailsList;
					AsyncTaskUtil.executeAsyncTask(asyncLoadImg, true);
				}
			}
			return;
		}*/
		
		ImgDetails imgDetails = values[0];
		if (imgDetails.bitmap != null) {
			//Log.i(TAG, "bitmap is not null");
			if (imgDetails.adapterView == null && imgDetails.recyclerView == null) {
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
				//Log.i(TAG, "onProgressUpdate() else");
				if (imgDetails.recyclerView != null) {
					if (imgDetails.recyclerView.get() == null) {
						return;
					}
					LayoutManager lm = imgDetails.recyclerView.get().getLayoutManager();
					
					if (lm instanceof GridLayoutManager) {
						GridLayoutManager glm = (GridLayoutManager) lm;
						/**
						 * Added one more check using key & tag, because otherwise with usage of ViewHolder it
						 * sometimes replaces valid image with new one which user had browsed earlier but which
						 * is loaded now, since position would be still in visible range but category/event is changed.
						 * e.g. - On tablet discover screen switch category instantly, it would load latest correct images
						 * first & then replace these by images for previous category events if we don't add key & tag check. 
						 */
						if (imgDetails.pos <= glm.findLastVisibleItemPosition() && 
								imgDetails.pos >= glm.findFirstVisibleItemPosition() &&
								imgDetails.key.equals(imgDetails.imageView.getTag())) {
							imgDetails.imageView.setImageBitmap(imgDetails.bitmap);
							//Log.i(TAG, "onProgressUpdate() bitmap set, " + imgDetails.imageView.getTag() + ", " + imgDetails.key);
						}
						
					} else if (lm instanceof LinearLayoutManager) {
						LinearLayoutManager llm = (LinearLayoutManager) lm;
						if (imgDetails.pos <= llm.findLastVisibleItemPosition() && 
								imgDetails.pos >= llm.findFirstVisibleItemPosition()) {
							imgDetails.imageView.setImageBitmap(imgDetails.bitmap);
							//Log.i(TAG, "onProgressUpdate() bitmap set");
						}
					}
					
				} else {
					if (imgDetails.pos <= imgDetails.adapterView.getLastVisiblePosition() && 
							imgDetails.pos >= imgDetails.adapterView.getFirstVisiblePosition()) {
						imgDetails.imageView.setImageBitmap(imgDetails.bitmap);
						//Log.i(TAG, "onProgressUpdate() bitmap set");
					}
				}
			}
			
		} else if (imgDetails.listener != null) {
			imgDetails.listener.onImageCouldNotBeLoaded();
		}
	}
}
