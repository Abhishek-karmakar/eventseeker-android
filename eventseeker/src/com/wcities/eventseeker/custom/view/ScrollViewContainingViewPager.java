package com.wcities.eventseeker.custom.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ScrollView;

public class ScrollViewContainingViewPager extends ScrollView {
	
	private static final String TAG = ScrollViewContainingViewPager.class.getSimpleName();
	private float xDistance, yDistance, lastX, lastY;
	
    public ScrollViewContainingViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
    	
		switch (ev.getAction()) {
		
		case MotionEvent.ACTION_DOWN:
			xDistance = yDistance = 0f;
			lastX = ev.getX();
			lastY = ev.getY();
			break;
			
		case MotionEvent.ACTION_MOVE:
			Log.i(TAG, "In Action Move");
			final float curX = ev.getX();
			final float curY = ev.getY();
			xDistance += Math.abs(curX - lastX);
			yDistance += Math.abs(curY - lastY);
			lastX = curX;
			lastY = curY;
			if (xDistance > yDistance) {
				return false;
			}
		}

        return super.onInterceptTouchEvent(ev);
    }
}
