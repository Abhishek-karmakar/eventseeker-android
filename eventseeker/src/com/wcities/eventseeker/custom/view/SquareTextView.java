package com.wcities.eventseeker.custom.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;

public class SquareTextView extends TextView {
	
	private static final String TAG = SquareTextView.class.getName();

	public SquareTextView(Context context) {
		super(context);
	}
	
	public SquareTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SquareTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int size = (getMeasuredWidth() > getMeasuredHeight()) ? getMeasuredWidth() : getMeasuredHeight();
		Log.d(TAG, "SquareTextView size: " + size);
		setMeasuredDimension(size, size);
		setGravity(Gravity.CENTER);
	}
}
