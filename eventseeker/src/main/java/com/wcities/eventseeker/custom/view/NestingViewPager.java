package com.wcities.eventseeker.custom.view;

import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;

public class NestingViewPager extends ViewPager {

	private static final String TAG = NestingViewPager.class.getName();

	public NestingViewPager(final Context context, final AttributeSet attrs) {
		super(context, attrs);
	}

	public NestingViewPager(final Context context) {
		super(context);
	}

	@Override
	protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
		if (VERSION.SDK_INT < VERSION_CODES.ICE_CREAM_SANDWICH && v != this && v instanceof ViewPager) {
			/**
			 * For lower apis viewpager inside viewpager doesn't work. 
			 * Basically child viewpager becomes non-scrollable, so we handle it manually for these versions
			 */
			return true;
		}
		return super.canScroll(v, checkV, dx, x, y);
	}
}
