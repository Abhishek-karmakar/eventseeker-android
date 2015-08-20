package com.wcities.eventseeker.util;

import android.content.res.Resources;
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
		int tabDividerBgId;
		int tabBtnTxtColor;
		int lnrTabBarBg;
		int txtHeaderColor;

		listDividerBgId = R.color.v_list_divider_color;
		if (AppConstants.IS_NIGHT_MODE_ENABLED) {
			txtColor = android.R.color.white;
			tabBtnTxtColor = android.R.color.white;
			tabDividerBgId = android.R.color.white;
			lnrTabBarBg = R.drawable.tab_bar_rounded_corners_night_mode;
			txtHeaderColor = android.R.color.white;
		} else {
			txtColor = R.color.eventseeker_bosch_theme_grey;
			//listDividerBgId = android.R.color.black;
			//listDividerBgId = R.color.v_list_divider_color;
			tabBtnTxtColor = android.R.color.black;
			tabDividerBgId = R.color.eventseeker_bosch_theme_grey;
			lnrTabBarBg = R.drawable.tab_bar_rounded_corners;			
			txtHeaderColor = android.R.color.black;
		}
		try {
			if (v instanceof ViewGroup) {
				ViewGroup vg = (ViewGroup) v;
				
				if (vg.getId() == R.id.tabBar) {
					vg.setBackgroundResource(lnrTabBarBg);	
				}
				
				for (int i = 0; i < vg.getChildCount(); i++) {
					View child = vg.getChildAt(i);
					updateViewColor(res, child);
				}

			} else if (v.getId() == R.id.txtHeaderDate) {
				((TextView) v).setTextColor(res.getColor(txtHeaderColor));				
				
			} else if (v.getId() == R.id.btnTab1 || v.getId() == R.id.btnTab2 || v.getId() == R.id.btnTab3) {
				((TextView) v).setTextColor(res.getColor(tabBtnTxtColor));
				
			} else if (v instanceof TextView) {
				((TextView) v).setTextColor(res.getColor(txtColor));

			} else if (v.getId() == R.id.vDivider1 || v.getId() == R.id.vDivider2) {
				v.setBackgroundColor(res.getColor(tabDividerBgId));
			
			} else if (v.getId() == R.id.listDivider) {
				v.setBackgroundResource(listDividerBgId);

			}

		} catch (Exception e) {
		}
	}
	
	public static boolean isPointInsideView(float x, float y, View view) {
	    int location[] = new int[2];
	    view.getLocationOnScreen(location);
	    int viewX = location[0];
	    int viewY = location[1];

	    //point is inside view bounds
	    /*Log.d(TAG, "Point lies in view ? " + 
	    		((x > viewX && x < (viewX + view.getWidth())) && (y > viewY && y < (viewY + view.getHeight()))));*/
	    return (x > viewX && x < (viewX + view.getWidth())) && (y > viewY && y < (viewY + view.getHeight()));
	}
	
	/**
	 * @param view
	 * @param res
	 * @return int[] containing left & top co-ordinates. It subtracts statusbar height for api <= 18
	 */
	/*public static int[] getLocation(View view, Resources res) {
		int[] location = new int[2];
		view.getLocationOnScreen(location);
		if (!VersionUtil.isApiLevelAbove18()) {
			location[1] -= getStatusBarHeight(res);
		}
		return location;
	}*/
	
	/**
	 * Referred from link: http://mrtn.me/blog/2012/03/17/get-the-height-of-the-status-bar-in-android/
	 * @return
	 */
	public static int getStatusBarHeight(Resources res) {
		int result = 0;
		int resourceId = res.getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = res.getDimensionPixelSize(resourceId);
		}
		return result;
	}
	
	/**
	 * @param view
	 * @param res
	 * @return location of screen for view where status bar height is subtracted from topY for apis <= 18
	 */
	public static int[] getLocationOnScreen(View view, Resources res) {
		int[] loc = new int[2];
		view.getLocationOnScreen(loc);
		if (!VersionUtil.isApiLevelAbove18()) {
			loc[1] -= getStatusBarHeight(res);
		}
		return loc;
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
