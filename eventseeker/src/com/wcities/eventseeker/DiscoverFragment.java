package com.wcities.eventseeker;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;
import com.wcities.eventseeker.adapter.CatTitlesAdapter;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.custom.view.ObservableScrollView;
import com.wcities.eventseeker.custom.view.ObservableScrollView.Callbacks;
import com.wcities.eventseeker.util.ConversionUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.VersionUtil;

public class DiscoverFragment extends FragmentLoadableFromBackStack implements Callbacks {
	
	private static final String TAG = DiscoverFragment.class.getSimpleName();
	
	private static final int TRANSLATION_Z_DP = 10;
	private static final int UNSCROLLED = -1;
	
	private ImageView imgCategory;
	private ViewPager viewPagerCatTitles;
	
	private int toolbarSize, prevScrollY = UNSCROLLED;
	private float limitScrollAt, translationZPx;
	private boolean isTranslationZApplied;
	private String title = "";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setHasOptionsMenu(true);
		setRetainInstance(true);
		
		toolbarSize = FragmentUtil.getResources(this).getDimensionPixelSize(R.dimen.action_bar_ht);
		translationZPx = ConversionUtil.toPx(FragmentUtil.getResources(this), TRANSLATION_Z_DP);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		//Log.d(TAG, "onCreateView()");
		View v = inflater.inflate(R.layout.fragment_discover, container, false);
		
		imgCategory = (ImageView) v.findViewById(R.id.imgCategory);
		viewPagerCatTitles = (ViewPager) v.findViewById(R.id.pagerCatTitles);
		
		final ObservableScrollView obsrScrlV = (ObservableScrollView) v.findViewById(R.id.obsrScrlV);
		obsrScrlV.setCallbacks(this);
		
		obsrScrlV.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                    	//Log.d(TAG, "onGlobalLayout()");
						if (VersionUtil.isApiLevelAbove15()) {
							obsrScrlV.getViewTreeObserver().removeOnGlobalLayoutListener(this);

						} else {
							obsrScrlV.getViewTreeObserver().removeGlobalOnLayoutListener(this);
						}
                        onScrollChanged(obsrScrlV.getScrollY(), true);
                    }
                });
		
		return v;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		//Log.d(TAG, "onStart(), prevScrollY = " + prevScrollY);
		((MainActivity) FragmentUtil.getActivity(this)).setVStatusBarVisibility(View.GONE);
		if (prevScrollY != UNSCROLLED) {
			onScrollChanged(prevScrollY, true);
		}
	}
	
	private void onScrollChanged(int scrollY, boolean forceUpdate) {
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		// Translate image
		ViewHelper.setTranslationY(imgCategory, scrollY / 2);
		
		// Translate tabs
		if (limitScrollAt == 0) {
			float pagerTop = viewPagerCatTitles.getTop();
			limitScrollAt = pagerTop - toolbarSize;
			
			if (VersionUtil.isApiLevelAbove18()) {
				limitScrollAt -= ma.getStatusBarHeight();
			}
		}
		float translationY = (scrollY >= limitScrollAt) ? (scrollY - limitScrollAt) : 0;
		
		//Log.d(TAG, "scrollY = " + scrollY + ", limitScrollAt = " + limitScrollAt + ", translationY = " + translationY + ", forceUpdate = " + forceUpdate);
		ViewHelper.setTranslationY(viewPagerCatTitles, translationY);
		
		if ((!isTranslationZApplied || forceUpdate) && scrollY >= limitScrollAt) {
			//Log.d(TAG, "translation apply z");
			ObjectAnimator elevateAnim = ObjectAnimator.ofFloat(viewPagerCatTitles, "translationZ", 0.0f, translationZPx);
			elevateAnim.setDuration(100);
			elevateAnim.start();
			
			ma.setVStatusBarLayeredVisibility(View.VISIBLE);
			ma.setVStatusBarLayeredColor(R.color.colorPrimaryDark);
			ma.setToolbarBg(ma.getResources().getColor(R.color.colorPrimary));
			viewPagerCatTitles.setBackgroundColor(ma.getResources().getColor(R.color.colorPrimary));
			ma.setToolbarElevation(0);
			
			title = ma.getResources().getString(R.string.title_discover);
			ma.updateTitle(title);
			
			isTranslationZApplied = true;
			
		} else if ((isTranslationZApplied || forceUpdate) && scrollY < limitScrollAt) {
			//Log.d(TAG, "translation remove z");
			ObjectAnimator elevateAnim = ObjectAnimator.ofFloat(viewPagerCatTitles, "translationZ", translationZPx, 0.0f);
			elevateAnim.setDuration(100);
			elevateAnim.start();
			
			ma.setVStatusBarLayeredVisibility(View.GONE);
			ma.setToolbarBg(Color.TRANSPARENT);
			viewPagerCatTitles.setBackgroundResource(R.drawable.bg_pager_cat_titles);
			ma.setToolbarElevation(ma.getResources().getDimensionPixelSize(R.dimen.action_bar_elevation));
			
			title = "";
			ma.updateTitle(title);
			
			isTranslationZApplied = false;
		}
		
		prevScrollY = scrollY;
	}
	
	public String getCurrentTitle() {
		return title;
	}

	@Override
	public String getScreenName() {
		return "Discover Screen";
	}

	@Override
	public void onScrollChanged(int scrollY) {
		onScrollChanged(scrollY, false);
	}
	
	@Override
	public void onDownMotionEvent() {
		// TODO Auto-generated method stub
	}

	@Override
	public void onUpOrCancelMotionEvent() {
		// TODO Auto-generated method stub
	}
}