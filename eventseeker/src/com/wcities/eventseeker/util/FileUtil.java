package com.wcities.eventseeker.util;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.wcities.eventseeker.constants.AppConstants;

public class FileUtil {

	private static final String TAG = FileUtil.class.getName();

	/**
	 * Loads a file from the inputstream and returns the content wrapped in a byte-array. It then closes the inputstream
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static final byte[] load(final InputStream in) throws IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		load(in, baos);
		return baos.toByteArray();
	}
	
	/**
	 * Reads data from the inputstream and returns the content in the outputstream. It then closes the inputstream
	 * @param in
	 * @param out
	 * @throws IOException
	 */
	public static void load(final InputStream in, final OutputStream out) throws IOException {
		try {
			final byte readBuf[] = new byte[1024];
			int readCnt = in.read(readBuf);
			while (0 < readCnt) {
				out.write(readBuf, 0, readCnt);
				readCnt = in.read(readBuf);
			}
			
		} finally {
			try {
				in.close();
				
			} catch (final Exception e) {
				Log.e(TAG, "Exception: " + e.getMessage());
			}
		}
	}
	
	public static final File createTempShareImgFile(Application application, Bitmap bitmap) {
		if (application.getExternalFilesDir(Environment.DIRECTORY_PICTURES) != null) {
			File shareImgFolder = new File(application.getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath() 
					+ AppConstants.TMP_SHARE_IMG_FOLDER);
			if (!shareImgFolder.exists()) {
				shareImgFolder.mkdir();
			}
			
		    FileOutputStream fos = null;
		    try {
		    	File pictureFile = new File(shareImgFolder.getAbsolutePath(), System.currentTimeMillis() + ".jpg");
				fos = new FileOutputStream(pictureFile);
			    bitmap.compress(CompressFormat.JPEG, 100, fos);
	
			    //Log.d(TAG, "path = " + pictureFile.getAbsolutePath());
			    return pictureFile;
			    
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
		}
	    
		return null;
	}
	
	public static void deleteShareImgCacheInBackground(final Application application) {
		new AsyncTask<Void, Void, Void>() {
	    	
	        @Override
	        protected Void doInBackground(Void... params) {
	        	if (application.getExternalFilesDir(Environment.DIRECTORY_PICTURES) != null) {
		        	File dir = new File(application.getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath() 
		        			+ AppConstants.TMP_SHARE_IMG_FOLDER);
		    		if (dir != null && dir.isDirectory()) {
		    			File[] files = dir.listFiles();
						for (int i = 0; i < files.length; i++) {
							Log.d(TAG, "file = " + files[i].getAbsolutePath());
							files[i].delete();
						}
		    		}
	        	}
	    		
	            return null;
	        }
	        
	    }.execute();
	}
}
