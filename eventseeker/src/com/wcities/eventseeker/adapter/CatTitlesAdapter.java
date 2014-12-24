package com.wcities.eventseeker.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;

import com.wcities.eventseeker.CategoryTitleFragment;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.custom.view.CategoryTitleLinearLayout;

public class CatTitlesAdapter extends FragmentPagerAdapter implements ViewPager.OnPageChangeListener {

	private static final String TAG = CatTitlesAdapter.class.getSimpleName();
	
	public final static float BIG_SCALE = 1.0f;
	private final static float SMALL_SCALE = 0.7f;
	private final static float DIFF_SCALE = BIG_SCALE - SMALL_SCALE;
	private final static int PAGES = 5;
	public final static int LOOPS = 1000; 
	private final static int FIRST_PAGE = PAGES * LOOPS / 2;
	
	private CategoryTitleLinearLayout cur = null;
	private CategoryTitleLinearLayout next = null, prev = null;
	private FragmentManager fm;
	private float scale;

	public CatTitlesAdapter(FragmentManager fm) {
		super(fm);
		this.fm = fm;
	}

	@Override
	public Fragment getItem(int position) {
		//Log.d(TAG, "getItem(), pos = " + position);
        // make the first pager bigger than others
        if (position == FIRST_PAGE)
        	scale = BIG_SCALE;     	
        else
        	scale = SMALL_SCALE;
        
        position = position % PAGES;
        return CategoryTitleFragment.newInstance(position, scale);
	}

	@Override
	public int getCount() {
		return PAGES * LOOPS;
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {	
		//Log.d(TAG, "onPageScrolled(), position = " + position + ", " + "positionOffset = " + positionOffset);
		if (positionOffset >= 0f && positionOffset <= 1f) {
			prev = getRootView(position);
			cur = getRootView(position + 1);
			next = getRootView(position + 2);

			cur.setScaleBoth(BIG_SCALE - DIFF_SCALE * positionOffset);
			float scalePrevNext = SMALL_SCALE + DIFF_SCALE * positionOffset;
			prev.setScaleBoth(scalePrevNext);
			next.setScaleBoth(scalePrevNext);
			
			if (positionOffset > 0.9f) {
				cur.findViewById(R.id.vHorLine).setVisibility(View.INVISIBLE);
				next.findViewById(R.id.vHorLine).setVisibility(View.VISIBLE);
				getRootView(position + 3).findViewById(R.id.vHorLine).setVisibility(View.INVISIBLE);
				
			} else {
				prev.findViewById(R.id.vHorLine).setVisibility(View.INVISIBLE);
				cur.findViewById(R.id.vHorLine).setVisibility(View.VISIBLE);
				next.findViewById(R.id.vHorLine).setVisibility(View.INVISIBLE);
			}
		}
	}

	@Override
	public void onPageSelected(int position) {}
	
	@Override
	public void onPageScrollStateChanged(int state) {}
	
	@Override
	public float getPageWidth(int position) {
		return 0.33f;
	}
	
	private CategoryTitleLinearLayout getRootView(int position) {
		return (CategoryTitleLinearLayout)fm.findFragmentByTag(this.getFragmentTag(position))
				.getView().findViewById(R.id.lnrLytRoot);
	}
	
	private String getFragmentTag(int position) {
	    return "android:switcher:cat title:" + position;
	}
}
