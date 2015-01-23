package com.wcities.eventseeker.interfaces;

import android.view.View;

public interface CustomSharedElementTransitionSource {
	public void addViewsToBeHidden(View... views);
	public void hideSharedElements();
	
	public void onPushedToBackStack();
	public void onPoppedFromBackStack();
}
