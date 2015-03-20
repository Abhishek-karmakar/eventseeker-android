package com.wcities.eventseeker.custom.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import com.wcities.eventseeker.R;

public class ResizableImageView extends ImageView {

	private static final String TAG = ResizableImageView.class.getName();
	
	private boolean mRemoveXtraHeight, mCompressAsPerWidth, mBlockSetAlpha;
	
	public ResizableImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initCustomParams(attrs);
	}

	public ResizableImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initCustomParams(attrs);
	}
	
	@Override
	public void setAlpha(float alpha) {
		if (mBlockSetAlpha) {
			return;
		}
		super.setAlpha(alpha);
	}

	private void initCustomParams(AttributeSet attrs) { 
	    TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ResizableImageView, 
	    		0, 0);
	    try {
	    	mRemoveXtraHeight = a.getBoolean(R.styleable.ResizableImageView_removeXtraHeight, false);
	    	mCompressAsPerWidth = a.getBoolean(R.styleable.ResizableImageView_compressAsPerWidth, false);
	    	mBlockSetAlpha = a.getBoolean(R.styleable.ResizableImageView_blockSetAlpha, false);
	    	
	    } finally {
		    //Don't forget this
		    a.recycle();
	    }
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		Drawable d = getDrawable();

		if (d != null) {
			// ceil not round - avoid thin vertical gaps along the left/right edges
			int width = MeasureSpec.getSize(widthMeasureSpec);
			//Log.d(TAG, "getIntrinsicHeight = " + d.getIntrinsicHeight());

			int height;
			if (mCompressAsPerWidth) {
				// Compress considering incoming width & maintaining 4:3 aspect ratio
				height = (int) Math.ceil((float) width * 3.0f / 4.0f);
				
			} else {
				// Compress considering drawable width
				height = (int) Math.ceil((float) width * (float) d.getIntrinsicHeight() / (float) d.getIntrinsicWidth());
			}

			if (mRemoveXtraHeight && height > d.getIntrinsicHeight()) {
				height = d.getIntrinsicHeight();
			}
			
			setMeasuredDimension(width, height);
			
		} else {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		}
	}
}
