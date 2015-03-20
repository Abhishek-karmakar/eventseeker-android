package com.wcities.eventseeker.custom.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class TextViewBlockingSetAlpha extends TextView {
	
	public TextViewBlockingSetAlpha(Context context) {
		super(context);
	}
	
	public TextViewBlockingSetAlpha(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public TextViewBlockingSetAlpha(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}
	
	public TextViewBlockingSetAlpha(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	@Override
	public void setAlpha(float alpha) {
		return;
	}
}
