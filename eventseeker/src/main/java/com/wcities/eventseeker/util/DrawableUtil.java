package com.wcities.eventseeker.util;

import android.graphics.drawable.Drawable;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class DrawableUtil {

	public static Drawable getDrawable(String url) {
		Drawable drawable = null;
		try {
			drawable = Drawable.createFromStream((InputStream) new URL(url).getContent(), "url");
			
		} catch (MalformedURLException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return drawable;
	}
}
