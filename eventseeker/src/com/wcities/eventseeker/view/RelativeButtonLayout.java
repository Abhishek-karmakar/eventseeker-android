package com.wcities.eventseeker.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

public class RelativeButtonLayout extends RelativeLayout {

	private static final String TAG = RelativeButtonLayout.class.getSimpleName();

	public RelativeButtonLayout(Context context) {
		super(context);
	}
	
	public RelativeButtonLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public RelativeButtonLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		int childCount = getChildCount();
		for (int i = 0; i < childCount; i++) {
			View v = getChildAt(i);
			v.setPressed(true);
		}
		return super.onInterceptTouchEvent(ev);
	}

}
