package com.wcities.eventseeker.custom.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.GridView;

public class ExpandableGridView extends GridView {
	
	private static final String TAG = ExpandableGridView.class.getName();
	
	boolean expanded = false;

    public ExpandableGridView(Context context) {
        super(context);
    }

    public ExpandableGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ExpandableGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public boolean isExpanded() {
        return expanded;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (isExpanded()) {
            // Calculate entire height by providing a very large height hint.
            // But do not use the highest 2 bits of this integer; those are
            // reserved for the MeasureSpec mode.
        	int height = MeasureSpec.getSize(heightMeasureSpec);
        	//Log.d(TAG, "Expanded, height = " + height + ", mode = " + MeasureSpec.getMode(heightMeasureSpec));
            int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
            super.onMeasure(widthMeasureSpec, expandSpec);

            ViewGroup.LayoutParams params = getLayoutParams();
            params.height = getMeasuredHeight();
            
        } else {
        	int height = MeasureSpec.getSize(heightMeasureSpec);
        	//Log.d(TAG, "non-expanded, height = " + height + ", mode = " + MeasureSpec.getMode(heightMeasureSpec));
        	int contractSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            super.onMeasure(widthMeasureSpec, contractSpec);
        }
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }
}
