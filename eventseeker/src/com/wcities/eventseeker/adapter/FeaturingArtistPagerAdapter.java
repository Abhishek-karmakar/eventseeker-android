package com.wcities.eventseeker.adapter;

import java.util.ArrayList;
import java.util.List;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.TextView;

import com.wcities.eventseeker.FeaturingArtistFragment;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.custom.view.FeaturingArtistRelativeLayout;

public class FeaturingArtistPagerAdapter extends FragmentPagerAdapter implements ViewPager.OnPageChangeListener {
	
	private static final String TAG = FeaturingArtistPagerAdapter.class.getSimpleName();
	
	public final static float BIG_SCALE = 1.0f;
	private final static float SMALL_SCALE = 0.8f;
	private final static float DIFF_SCALE = BIG_SCALE - SMALL_SCALE;
	
	private FragmentManager fm;
	private ViewPager viewPager;
	private float scale;
	private List<Artist> artists;
	private int currentPosition = 0;

	public FeaturingArtistPagerAdapter(FragmentManager fm, List<Artist> artists, ViewPager viewPager) {
		super(fm);
		this.fm = fm;
		this.viewPager = viewPager;
		this.artists = (artists != null) ? artists : new ArrayList<Artist>();
	}

	public void setViewPager(ViewPager viewPager) {
		this.viewPager = viewPager;
	}

	@Override
	public Fragment getItem(int position) {
		//Log.d(TAG, "getItem(), pos = " + position);
		if (position == 0)
        	scale = BIG_SCALE;     	
        else
        	scale = SMALL_SCALE;

        return FeaturingArtistFragment.newInstance(artists.get(position), scale);
	}

	@Override
	public int getCount() {
		return artists.size();
	}

	@Override
	public void onPageScrollStateChanged(int arg0) {}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
		/**
		 * positionOffset value is 0 for centered position & swiping towards left it increases this value
		 * gradually from 0 to 0.99 & then position value increases by 1 & positionOffset becomes again 0 for this new 
		 * center position, which was on right side until now.
		 * Similarly, swiping towards right, position value instantly decreases by 1 & positionOffset starts from
		 * 0.99, goes on decreasing upto 0 at which point this left page now occupies centered position. 
		 */
		//Log.d(TAG, "onPageScrolled(), position = " + position + ", " + "positionOffset = " + positionOffset);
		if (positionOffset >= 0f && positionOffset <= 1f) {
			
			FeaturingArtistRelativeLayout cur = getRootView(position);
			// fragment is null in case if artists size is 0. In such case onPageScrolled() is still called.
			if (cur == null) {
				return;
			}
			
			FeaturingArtistRelativeLayout next = null, prev = null;
			if (position + 1 < getCount()) {
				next = getRootView(position +1);
			}
			if (position - 1 >= 0) {
				prev = getRootView(position - 1);
			}
			
			cur.setScaleBoth(BIG_SCALE - DIFF_SCALE * positionOffset);
			if (next != null) {
				next.setScaleBoth(SMALL_SCALE + DIFF_SCALE * positionOffset);
			}

			if (positionOffset < 0.5f) {
				cur.findViewById(R.id.vTranslucentLayer).setVisibility(View.GONE);
				((TextView)cur.findViewById(R.id.txtArtistName)).setVisibility(View.VISIBLE);
				
				if (next != null) {
					next.findViewById(R.id.vTranslucentLayer).setVisibility(View.VISIBLE);
					((TextView)next.findViewById(R.id.txtArtistName)).setVisibility(View.GONE);
				}
				if (prev != null) {
					prev.findViewById(R.id.vTranslucentLayer).setVisibility(View.VISIBLE);
					((TextView)prev.findViewById(R.id.txtArtistName)).setVisibility(View.GONE);
				}
				
			} else {
				cur.findViewById(R.id.vTranslucentLayer).setVisibility(View.VISIBLE);
				((TextView)cur.findViewById(R.id.txtArtistName)).setVisibility(View.GONE);
				
				if (next != null) {
					next.findViewById(R.id.vTranslucentLayer).setVisibility(View.GONE);
					((TextView)next.findViewById(R.id.txtArtistName)).setVisibility(View.VISIBLE);
				}
				if (prev != null) {
					prev.findViewById(R.id.vTranslucentLayer).setVisibility(View.VISIBLE);
					((TextView)prev.findViewById(R.id.txtArtistName)).setVisibility(View.GONE);
				}
			}
		}
	}

	@Override
	public void onPageSelected(int position) {
		currentPosition = position;
	}
	
	public int getCurrentPosition() {
		return currentPosition;
	}

	private FeaturingArtistRelativeLayout getRootView(int position) {
		Fragment fragment = fm.findFragmentByTag(getFragmentTag(position));
		/**
		 * fragment is null in case if artists size is 0. In such case onPageScrolled() is still called which
		 * in turn calls getRootView()
		 */
		if (fragment != null) {
			View v = fragment.getView();
			// on changing orientation, this v becomes null
			if (v != null) {
				return (FeaturingArtistRelativeLayout)v.findViewById(R.id.rltLytRoot);
			}
		}
		return null;
	}
	
	private String getFragmentTag(int position) {
	    return "android:switcher:" + viewPager.getId() + ":" + position;
	}
}
