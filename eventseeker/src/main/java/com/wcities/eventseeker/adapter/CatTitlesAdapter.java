package com.wcities.eventseeker.adapter;

import android.graphics.Typeface;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.TextView;

import com.wcities.eventseeker.CategoryTitleFragment;
import com.wcities.eventseeker.DiscoverFragment;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Category;
import com.wcities.eventseeker.custom.view.CategoryTitleLinearLayout;

import java.util.List;

public class CatTitlesAdapter extends FragmentPagerAdapter implements ViewPager.OnPageChangeListener {

	private static final String TAG = CatTitlesAdapter.class.getSimpleName();
	
	public final static float BIG_SCALE = 1.0f;
	private final static float SMALL_SCALE = 0.7f;
	private final static float DIFF_SCALE = BIG_SCALE - SMALL_SCALE;
	private static final int LOOPS = 1000;
	public final static int FIRST_PAGE = AppConstants.TOTAL_CATEGORIES * LOOPS / 2;
	
	private CategoryTitleLinearLayout cur = null;
	private CategoryTitleLinearLayout next = null, prev = null;
	private FragmentManager fm;
	private ViewPager viewPager;
	private float scale;
	private List<Category> evtCategories;
	private int selectedCatId;
	private DiscoverFragment discoverFragment;
	
	public CatTitlesAdapter(FragmentManager fm, ViewPager viewPager, List<Category> evtCategories, 
			DiscoverFragment discoverFragment) {
		super(fm);
		this.fm = fm;
		this.viewPager = viewPager;
		this.evtCategories = evtCategories;
		this.discoverFragment = discoverFragment;
	}

	@Override
	public Fragment getItem(int position) {
		//Log.d(TAG, "getItem(), pos = " + position);
        // make the first pager bigger than others
        if (position == FIRST_PAGE)
        	scale = BIG_SCALE;     	
        else
        	scale = SMALL_SCALE;
        
        int actualPosition = position % AppConstants.TOTAL_CATEGORIES;
        return CategoryTitleFragment.newInstance(evtCategories.get(actualPosition).getName(), scale, position);
	}

	@Override
	public int getCount() {
		return AppConstants.TOTAL_CATEGORIES * LOOPS;
	}
	
	/*@Override
	public int getItemPosition(Object object) {
		return POSITION_NONE;
	}*/

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {	
		//Log.d(TAG, "onPageScrolled(), position = " + position + ", " + "positionOffset = " + positionOffset);
		if (positionOffset >= 0f && positionOffset <= 1f) {
			prev = getRootView(position);
			if (prev == null) {
				return;
			}
			
			if (position + 2 < getCount()) {
				cur = getRootView(position + 1);
				next = getRootView(position + 2);
				
			} else if (position + 1 < getCount()) {
				cur = getRootView(position + 1);
				next = null;
				
			} else {
				cur = next = null;
			}

			if (cur != null) {
				cur.setScaleBoth(BIG_SCALE - DIFF_SCALE * positionOffset);
			}
			float scalePrevNext = SMALL_SCALE + DIFF_SCALE * positionOffset;
			prev.setScaleBoth(scalePrevNext);
			if (next != null) {
				next.setScaleBoth(scalePrevNext);
			}
			
			if (positionOffset > 0.9f) {
				if (cur != null) {
					cur.findViewById(R.id.vHorLine).setVisibility(View.INVISIBLE);
					((TextView)cur.findViewById(R.id.txtTitle)).setTypeface(null, Typeface.NORMAL);
				}
				if (next != null) {
					next.findViewById(R.id.vHorLine).setVisibility(View.VISIBLE);
					((TextView)next.findViewById(R.id.txtTitle)).setTypeface(null, Typeface.BOLD);
				}
				if (position + 3 < getCount()) {
					CategoryTitleLinearLayout next2next = getRootView(position + 3);
					next2next.findViewById(R.id.vHorLine).setVisibility(View.INVISIBLE);
					((TextView)next2next.findViewById(R.id.txtTitle)).setTypeface(null, Typeface.NORMAL);
				}
				
			} else {
				prev.findViewById(R.id.vHorLine).setVisibility(View.INVISIBLE);
				((TextView)prev.findViewById(R.id.txtTitle)).setTypeface(null, Typeface.NORMAL);
				if (cur != null) {
					cur.findViewById(R.id.vHorLine).setVisibility(View.VISIBLE);
					((TextView)cur.findViewById(R.id.txtTitle)).setTypeface(null, Typeface.BOLD);
				}
				if (next != null) {
					next.findViewById(R.id.vHorLine).setVisibility(View.INVISIBLE);
					((TextView)next.findViewById(R.id.txtTitle)).setTypeface(null, Typeface.NORMAL);
				}
			}
		}
	}

	@Override
	public void onPageSelected(int position) {
		// 1 is added in position because viewpager counts from left hand side whereas we want centered page img
		position = (position + 1) % AppConstants.TOTAL_CATEGORIES;
		int newCatId = evtCategories.get(position).getId();
		//Log.d(TAG, "position = " + position + ", catId = " + catId);
		if (selectedCatId != newCatId) {
			selectedCatId = newCatId;
			discoverFragment.onCatChanged(selectedCatId);
		}
	}
	
	@Override
	public void onPageScrollStateChanged(int state) {}
	
	@Override
	public float getPageWidth(int position) {
		return 0.33f;
	}
	
	public int getSelectedCatId() {
		return selectedCatId;
	}

	private CategoryTitleLinearLayout getRootView(int position) {
		View v = fm.findFragmentByTag(getFragmentTag(position)).getView();
		// on changing orientation, this v becomes null
		if (v != null) {
			return (CategoryTitleLinearLayout)v.findViewById(R.id.lnrLytRoot);
		}
		return null;
	}
	
	private String getFragmentTag(int position) {
	    return "android:switcher:" + viewPager.getId() + ":" + position;
	}
}
