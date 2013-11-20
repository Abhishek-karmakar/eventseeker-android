package com.wcities.eventseeker.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.util.Log;

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
}
