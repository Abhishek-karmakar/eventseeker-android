package com.wcities.eventseeker.util;

import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ListView;
import android.widget.TextView;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.constants.AppConstants;

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

	public static void updateViewColor(Resources res, View v) {
		int txtColor; 
		int listDividerBgId;
		if (AppConstants.IS_NIGHT_MODE_ENABLED) {
			txtColor = android.R.color.white;
			listDividerBgId = R.drawable.info_divider;
			
		} else {
			txtColor = R.color.eventseeker_bosch_theme_grey;
			listDividerBgId = android.R.color.black;
		}
		
		try {
			if (v instanceof ViewGroup) {
				ViewGroup vg = (ViewGroup) v;
				for (int i = 0; i < vg.getChildCount(); i++) {
					View child = vg.getChildAt(i);
					updateViewColor(res, child);
				}

			} else if (v instanceof TextView) {
				((TextView) v).setTextColor(res.getColor(txtColor));
				
			} else if (v.getId() == R.id.listDivider) {
				v.setBackgroundResource(listDividerBgId);
			}

		} catch (Exception e) {
		}
	}
	
	public static class AnimationUtil {

		public static void startRotationToView(View view, float toDegrees, 
				float fromDegrees, float pivotX, float pivotY, int duration) {

			RotateAnimation animation = new RotateAnimation(toDegrees,
					fromDegrees, Animation.RELATIVE_TO_SELF, pivotX,
					Animation.RELATIVE_TO_SELF, pivotY);
			
			animation.setRepeatCount(Animation.INFINITE);
			animation.setDuration(duration);
			animation.setInterpolator(new LinearInterpolator());
			
			view.startAnimation(animation);
			
		}
		
		public static void stopRotationToView(View view) {
			view.clearAnimation();
		}
		
	}
	
}
