package com.wcities.eventseeker.util;

import android.content.res.Resources;
import android.view.ViewGroup;
import android.widget.ListView;

public class ViewUtil {

	protected static final String TAG = ViewUtil.class.getName();

	public static void updateListViewHeight(ListView listView, Resources res, int dimenId) {
		ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = res.getDimensionPixelSize(dimenId) * listView.getAdapter().getCount();
        listView.setLayoutParams(params);
	}
	
	/*public static void changeClickableArea(final View view, final int topByPixels, final int leftByPixels, 
			final int rightByPixels, final int bottomByPixels) {
		final View parent = (View) view.getParent();
		parent.post(new Runnable() {
		    // Post in the parent's message queue to make sure the parent
		    // lays out its children before we call getHitRect()
		    public void run() {
		        Rect r = new Rect();
		        view.getHitRect(r);
		        Log.d(TAG, "top = " + r.top + ", l = " + r.left + ", r = " + r.right + ", b = " + r.bottom);
		        r.top += topByPixels;
		        r.left += leftByPixels;
		        r.right += rightByPixels;
		        r.bottom += bottomByPixels;
		        Log.d(TAG, "top = " + r.top + ", l = " + r.left + ", r = " + r.right + ", b = " + r.bottom);
		        parent.setTouchDelegate(new TouchDelegate(r, view));
		    }
		});
	}*/
}
