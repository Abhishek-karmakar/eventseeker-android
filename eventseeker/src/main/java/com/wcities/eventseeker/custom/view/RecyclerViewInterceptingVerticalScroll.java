package com.wcities.eventseeker.custom.view;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class RecyclerViewInterceptingVerticalScroll extends RecyclerView {
	
	private float xDistance, yDistance, lastX, lastY;

	public RecyclerViewInterceptingVerticalScroll(Context context) {
		super(context);
	}
	
	public RecyclerViewInterceptingVerticalScroll(Context context,
			AttributeSet attrs) {
		super(context, attrs);
	}
	
	public RecyclerViewInterceptingVerticalScroll(Context context,
			AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
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
			//Log.i(TAG, "In Action Move");
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
