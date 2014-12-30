package com.wcities.eventseeker.adapter;

import java.util.HashMap;
import java.util.List;

import android.graphics.Typeface;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.wcities.eventseeker.CategoryTitleFragment;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Category;
import com.wcities.eventseeker.custom.view.CategoryTitleLinearLayout;

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
	private ImageView imgCategory;
	private float scale;
	private List<Category> evtCategories;
	
	private final HashMap<Integer, Integer> categoryImgs = new HashMap<Integer, Integer>() {
		{
			put(AppConstants.CATEGORY_ID_START, R.drawable.cat_900);
			put(AppConstants.CATEGORY_ID_START + 1, R.drawable.cat_901);
			put(AppConstants.CATEGORY_ID_START + 2, R.drawable.cat_902);
			put(AppConstants.CATEGORY_ID_START + 3, R.drawable.cat_903);
			put(AppConstants.CATEGORY_ID_START + 4, R.drawable.cat_904);
			put(AppConstants.CATEGORY_ID_START + 5, R.drawable.cat_905);
			put(AppConstants.CATEGORY_ID_START + 6, R.drawable.cat_906);
			put(AppConstants.CATEGORY_ID_START + 7, R.drawable.cat_907);
			put(AppConstants.CATEGORY_ID_START + 8, R.drawable.cat_908);
			put(AppConstants.CATEGORY_ID_START + 9, R.drawable.cat_909);
			put(AppConstants.CATEGORY_ID_START + 10, R.drawable.cat_910);
			put(AppConstants.CATEGORY_ID_START + 11, R.drawable.cat_911);
		}
	};

	public CatTitlesAdapter(FragmentManager fm, ViewPager viewPager, List<Category> evtCategories, 
			ImageView imgCategory) {
		super(fm);
		this.fm = fm;
		this.viewPager = viewPager;
		this.evtCategories = evtCategories;
		this.imgCategory = imgCategory;
	}

	public void updateViews(ViewPager viewPager, ImageView imgCategory) {
		this.viewPager = viewPager;
		this.imgCategory = imgCategory;
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
			cur = getRootView(position + 1);
			next = getRootView(position + 2);

			cur.setScaleBoth(BIG_SCALE - DIFF_SCALE * positionOffset);
			float scalePrevNext = SMALL_SCALE + DIFF_SCALE * positionOffset;
			prev.setScaleBoth(scalePrevNext);
			next.setScaleBoth(scalePrevNext);
			
			if (positionOffset > 0.9f) {
				cur.findViewById(R.id.vHorLine).setVisibility(View.INVISIBLE);
				((TextView)cur.findViewById(R.id.txtTitle)).setTypeface(null, Typeface.NORMAL);
				next.findViewById(R.id.vHorLine).setVisibility(View.VISIBLE);
				((TextView)next.findViewById(R.id.txtTitle)).setTypeface(null, Typeface.BOLD);
				CategoryTitleLinearLayout next2next = getRootView(position + 3);
				next2next.findViewById(R.id.vHorLine).setVisibility(View.INVISIBLE);
				((TextView)next2next.findViewById(R.id.txtTitle)).setTypeface(null, Typeface.NORMAL);
				
			} else {
				prev.findViewById(R.id.vHorLine).setVisibility(View.INVISIBLE);
				((TextView)prev.findViewById(R.id.txtTitle)).setTypeface(null, Typeface.NORMAL);
				cur.findViewById(R.id.vHorLine).setVisibility(View.VISIBLE);
				((TextView)cur.findViewById(R.id.txtTitle)).setTypeface(null, Typeface.BOLD);
				next.findViewById(R.id.vHorLine).setVisibility(View.INVISIBLE);
				((TextView)next.findViewById(R.id.txtTitle)).setTypeface(null, Typeface.NORMAL);
			}
		}
	}

	@Override
	public void onPageSelected(int position) {
		// 1 is added in position because viewpager counts from left hand side whereas we want centered page img
		position = (position + 1) % AppConstants.TOTAL_CATEGORIES;
		int catId = evtCategories.get(position).getId();
		//Log.d(TAG, "position = " + position + ", catId = " + catId);
		imgCategory.setImageResource(categoryImgs.get(catId));
	}
	
	@Override
	public void onPageScrollStateChanged(int state) {}
	
	@Override
	public float getPageWidth(int position) {
		return 0.33f;
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
