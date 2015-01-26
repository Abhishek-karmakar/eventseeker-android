package com.wcities.eventseeker.adapter;

import java.util.ArrayList;
import java.util.List;

import com.wcities.eventseeker.FeaturingArtistFragment;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.VideoFragment;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.core.Video;
import com.wcities.eventseeker.custom.view.RelativeLayoutCenterScale;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class VideoPagerAdapter extends FragmentStatePagerAdapter implements OnPageChangeListener {
	
	private static final String TAG = FeaturingArtistPagerAdapter.class.getSimpleName();
	
	private final static float BIG_SCALE = 1.0f;
	private final static float DIFF_SCALE = BIG_SCALE - RelativeLayoutCenterScale.SMALL_SCALE;
	
	private FragmentManager fm;
	private float scale;
	private int artistId;
	List<Video> videos;
	private int currentPosition = 0;

	public VideoPagerAdapter(FragmentManager fm, List<Video> videos, int artistId) {
		super(fm);
		this.fm = fm;
		this.videos = (videos != null) ? videos : new ArrayList<Video>();
		this.artistId = artistId;
	}

	@Override
	public Fragment getItem(int position) {
		//Log.d(TAG, "getItem(), pos = " + position);
        scale = RelativeLayoutCenterScale.SMALL_SCALE;
        return VideoFragment.newInstance(videos.get(position), artistId, scale);
	}

	@Override
	public int getCount() {
		return videos.size();
	}

	@Override
	public void onPageScrollStateChanged(int arg0) {}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
		//Log.d(TAG, "onPageScrolled(), position = " + position + ", " + "positionOffset = " + positionOffset);
		if (position > videos.size() - 1) {
			/**
			 * This chk is required because of onPageScrolled() being called even if 
			 * no videos are there which in turn callls getRootView() which calls instantiateItem() 
			 * causing NullPointerException in instantiateItem()
			 */
			return;
		}
		/**
		 * positionOffset value is 0 for centered position & swiping towards left it increases this value
		 * gradually from 0 to 0.99 & then position value increases by 1 & positionOffset becomes again 0 for this new 
		 * center position, which was on right side until now.
		 * Similarly, swiping towards right, position value instantly decreases by 1 & positionOffset starts from
		 * 0.99, goes on decreasing upto 0 at which point this left page now occupies centered position. 
		 */
		//Log.d(TAG, "onPageScrolled() 2, position = " + position + ", " + "positionOffset = " + positionOffset);
		if (positionOffset >= 0f && positionOffset <= 1f) {
			
			RelativeLayoutCenterScale cur = getRootView(position);
			// fragment is null in case if artists size is 0. In such case onPageScrolled() is still called.
			if (cur == null) {
				return;
			}
			
			RelativeLayoutCenterScale next = null, prev = null;
			if (position + 1 < getCount()) {
				next = getRootView(position +1);
			}
			if (position - 1 >= 0) {
				prev = getRootView(position - 1);
			}
			
			cur.setScaleBoth(BIG_SCALE - DIFF_SCALE * positionOffset);
			if (next != null) {
				next.setScaleBoth(RelativeLayoutCenterScale.SMALL_SCALE + DIFF_SCALE * positionOffset);
			}
			if (prev != null) {
				prev.setScaleBoth(RelativeLayoutCenterScale.SMALL_SCALE + DIFF_SCALE * positionOffset);
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

	private RelativeLayoutCenterScale getRootView(int position) {
		Fragment fragment = (Fragment) instantiateItem(null, position);
		/**
		 * fragment is null in case if artists size is 0. In such case onPageScrolled() is still called which
		 * in turn calls getRootView()
		 */
		if (fragment != null) {
			View v = fragment.getView();
			// on changing orientation, this v becomes null
			if (v != null) {
				return (RelativeLayoutCenterScale)v.findViewById(R.id.rltLytRoot);
			}
		}
		return null;
	}
}
