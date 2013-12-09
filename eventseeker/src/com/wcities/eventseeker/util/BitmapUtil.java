package com.wcities.eventseeker.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class BitmapUtil {
	
	private static final String TAG = "BitmapUtil";

	public static Bitmap getBitmap(String url) throws FileNotFoundException {
		//Log.i(TAG, "getBitmap()");
		Bitmap bitmap = null;
		InputStream is = null;
		
		try {
			is = (InputStream) new URL(url).getContent();
			//Log.i(TAG, "got is");
			bitmap = BitmapFactory.decodeStream(is);
			//Log.i(TAG, "got bitmap");
			
			/*URL imageUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection)imageUrl.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            is = conn.getInputStream();
            bitmap = BitmapFactory.decodeStream(is);*/
			
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
