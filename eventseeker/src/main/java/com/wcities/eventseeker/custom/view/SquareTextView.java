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
		/**
		 * Without: 
		   'if (heightMeasureSpec > 0) {
						heightMeasureSpec = 0;
			}'
		   When this textView was used in ConnectAccounts ListView, in Galaxy S	device the width of 
		   TextView was expanding infinite and the background image 'Circle' was appearing as 'parallel
		   lines' around Sync Count value. The issue was 'height Mode value = MeasureSpec.EXACTLY'
		   for Galaxy S and in Galaxy S3, this value was coming as 'height Mode: MeasureSpec.UNSPECIFIED'
		   for the 'Call 1'(refer logs below). So, by adding the above code block, we are checking
		   if the 'heightMeasureSpec' is greater than '0' then make it '0' ie. 'MeasureSpec.UNSPECIFIED'.
		   We came to this conclusion by comparing the logs of GS & GS3 logs.
			
			Log:
		 	Galaxy S:
		 		Call 1:
				size: 1073741823
				
				widthMeasureSpec: -2147483192
				heightMeasureSpec: 2147483647
				
				width Mode: -2147483648 MeasureSpec.ATMOST
				height Mode: 1073741824 MeasureSpec.EXACTLY
				
				width Size: 456
				height Size: 1073741823
				
				Call 2:
				size: 143
				
				widthMeasureSpec: -1610612508
				heightMeasureSpec: 0
				
				width Mode: -2147483648 MeasureSpec.ATMOST
				height Mode: 0 MeasureSpec.UNSPECIFIED
				
				width Size: 536871140
				height Size: 0
				
			Galxy S3:
				Call 1:
				size: 187
				widthMeasureSpec: -2147482960
				heightMeasureSpec: 0
				
				width Mode: -2147483648 MeasureSpec.ATMOST
				height Mode: 0 MeasureSpec.UNSPECIFIED
				
				width Size: 688
				height Size: 0
				
				Call 2:
				size: 187
				widthMeasureSpec: 1073742011
				heightMeasureSpec: 0 
				
				width Mode: 1073741824 MeasureSpec.EXACTLY
				height Mode: 0 MeasureSpec.UNSPECIFIED
				
				width Size: 187
				height Size: 0
				
		 	Galaxy S(logs after adding the block):
		 		Call 1:
				size: 143
				
				widthMeasureSpec: -2147483192
				heightMeasureSpec: 0
				
				width Mode: -2147483648
				height Mode: 0
				
				width Size: 456
				height Size: 0
				
				Call 2:
				size: 143
				
				widthMeasureSpec: 1073741967
				heightMeasureSpec: 0
				
				width Mode: 1073741824
				height Mode: 0
				
				width Size: 143
				height Size: 0

		 	
		 */
		if (heightMeasureSpec > 0) {
			heightMeasureSpec = 0;
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int size = (getMeasuredWidth() > getMeasuredHeight()) ? getMeasuredWidth() : getMeasuredHeight();
		//Log.d(TAG, "SquareTextView size: " + size);
		setMeasuredDimension(size, size);
		setGravity(Gravity.CENTER);
	}
}
