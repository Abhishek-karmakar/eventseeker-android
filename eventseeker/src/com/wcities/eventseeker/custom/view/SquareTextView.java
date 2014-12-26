package com.wcities.eventseeker.custom.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.TextView;

public class SquareTextView extends TextView {
	
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
		setMeasuredDimension(size, size);
		setGravity(Gravity.CENTER);
	}
}
