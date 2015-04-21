package com.wcities.eventseeker.custom.view;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.wcities.eventseeker.adapter.CatTitlesAdapter;

public class CategoryTitleLinearLayout extends LinearLayout {
	
	private float scale = CatTitlesAdapter.BIG_SCALE;

	public CategoryTitleLinearLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		// following call is needed so that call to invalidate() from setScaleBoth() can invoke onDraw() 
		setWillNotDraw(false);
	}

	public CategoryTitleLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		// following call is needed so that call to invalidate() from setScaleBoth() can invoke onDraw() 
		setWillNotDraw(false);
	}

	public CategoryTitleLinearLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		// following call is needed so that call to invalidate() from setScaleBoth() can invoke onDraw() 
		setWillNotDraw(false);
	}

	public CategoryTitleLinearLayout(Context context) {
		super(context);
		// following call is needed so that call to invalidate() from setScaleBoth() can invoke onDraw()
		setWillNotDraw(false);
	}

	public void setScaleBoth(float scale) {
		this.scale = scale;
		this.invalidate(); 	// If you want to see the scale every time you set
							// scale you need to have this line here, 
							// invalidate() function will call onDraw(Canvas)
							// to redraw the view for you
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		// The main mechanism to display scale animation, you can customize it
		// as your needs
		int w = this.getWidth();
		int h = this.getHeight();
		//Log.d(VIEW_LOG_TAG, "h = " + h);
		canvas.scale(scale, scale, w/2, h);
		
		super.onDraw(canvas);
	}
}
