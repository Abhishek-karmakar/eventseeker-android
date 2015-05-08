package com.wcities.eventseeker.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.wcities.eventseeker.cache.BitmapCacheable;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class BitmapUtil {

	private static final String TAG = BitmapUtil.class.getSimpleName();
	private static final int MAX_BITMAP_DIMENSION = 2048;

	public static Bitmap getBitmap(String url) throws FileNotFoundException {
		//Log.i(TAG, "getBitmap()");
		Bitmap bitmap = null;
		InputStream is = null;

		try {
			is = (InputStream) new URL(url).getContent();
			//Log.i(TAG, "got is");
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(is, null, options);

			/**
			 * NOTE: In few of the devices like Nexus 7, Sometimes an error was showing saying 'bitmap is
			 * too large to upload' while setting the bitmap on ImageView. So, scaling the bitmaps max dimen
			 * to be 'MAX_BITMAP_DIMENSION' and keeping the aspect ratio constant, if width or height >
			 * MAX_BITMAP_DIMENSION.
			 */
			if (options.outWidth > MAX_BITMAP_DIMENSION || options.outHeight > MAX_BITMAP_DIMENSION) {
				options.inSampleSize = 4;
			}
			options.inJustDecodeBounds = false;
			is = (InputStream) new URL(url).getContent();
			bitmap = BitmapFactory.decodeStream(is, null, options);

		} catch (MalformedURLException e) {
			e.printStackTrace();
			
		} catch (FileNotFoundException e) {
			throw e;
			
		} catch (IOException e) {
			e.printStackTrace();
			
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		//Log.i(TAG, "return bitmap");
		return bitmap;
	}

	public static Bitmap getFBUserBitmap(String strURL) {
		Bitmap img = null;
		try {
	        HttpGet request = new HttpGet(strURL);

	        HttpClient client = new DefaultHttpClient();
	        HttpResponse response = (HttpResponse)client.execute(request);           
	        
	        HttpEntity entity = response.getEntity();
	        BufferedHttpEntity bufferedEntity = new BufferedHttpEntity(entity);
	        
	        InputStream inputStream = bufferedEntity.getContent();
	        img = BitmapFactory.decodeStream(inputStream);
            
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
		return img;
	}

	public static List<String> getUrlsInOrder(BitmapCacheable bitmapCacheable, ImgResolution imgResolution) {
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
	
	/*public static Uri getImgFileUri(Bitmap bitmap) {
		//return MediaStore.Images.Media.insertImage(contentResolver, bitmap, "Image title", "Image description");
		 
		File rootSdDirectory = Environment.getExternalStorageDirectory();
	    File pictureFile = new File(rootSdDirectory, "share_attachment.jpg");
	    if (pictureFile.exists()) {
	        pictureFile.delete();
	    }
	    
	    FileOutputStream fos = null;
	    try {
			fos = new FileOutputStream(pictureFile);
		    bitmap.compress(CompressFormat.JPEG, 100, fos);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	    Log.d(TAG, "path = " + pictureFile.getAbsolutePath());
	    return Uri.fromFile(pictureFile);
	}*/
}
