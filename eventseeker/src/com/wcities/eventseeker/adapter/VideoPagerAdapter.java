package com.wcities.eventseeker.adapter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.view.ViewGroup;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.VideoFragment;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Video;
import com.wcities.eventseeker.custom.view.RelativeLayoutCenterScale;

public class VideoPagerAdapter extends FragmentStatePagerAdapter implements OnPageChangeListener {
	
	private static final String TAG = VideoPagerAdapter.class.getSimpleName();
	
	public final static float BIG_SCALE = 1.0f;
	private final static float DIFF_SCALE = BIG_SCALE - RelativeLayoutCenterScale.SMALL_SCALE;
	
	private float scale;
	private int artistId;
	List<Video> videos;
	private int currentPosition = 0;
	private FragmentManager fm;
	protected HashMap<Integer, WeakReference<VideoFragment>> fragmentReferences;
	private boolean fragmentsDetached;

	public VideoPagerAdapter(FragmentManager fm, List<Video> videos, int artistId) {
		super(fm);
		this.fm = fm;
		this.videos = (videos != null) ? videos : new ArrayList<Video>();
		this.artistId = artistId;
		fragmentReferences = new HashMap<Integer, WeakReference<VideoFragment>>();
		if (videos != null) {
			/**
			 * initializing the current position at 2nd place if videos are more than 1, 
			 * So that there is no space before the 1st item.
			 */
			currentPosition = (videos.size() > 1) ? 1 : 0;
		}
	}

	@Override
	public Fragment getItem(int position) {
		//Log.d(TAG, "getItem(), pos = " + position);
        scale = RelativeLayoutCenterScale.SMALL_SCALE;
    	VideoFragment videoFragment = VideoFragment.newInstance(videos.get(position), artistId, scale, position);
    	fragmentReferences.put(position, new WeakReference<VideoFragment>(videoFragment));
    	return videoFragment;
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
	
	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		super.destroyItem(container, position, object);
		fragmentReferences.remove(position);
	}
	
	public int getCurrentPosition() {
		return currentPosition;
	}

	public boolean areFragmentsDetached() {
		return fragmentsDetached;
	}

	public void setFragmentsDetached(boolean fragmentsDetached) {
		this.fragmentsDetached = fragmentsDetached;
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
	
	public void detachFragments() {
		//Log.d(TAG, "detachFragments()");
		for (Iterator<WeakReference<VideoFragment>> iterator = fragmentReferences.values().iterator(); iterator.hasNext();) {
			Fragment fragment = iterator.next().get();
			if (fragment != null) {
				//Log.d(TAG, "fragment != null, tag = " + fragment.getTag());
				fm.beginTransaction().detach(fragment).commit();
		        fm.executePendingTransactions();
			}
		}
		fragmentsDetached = true;
	}
	
	public void attachFragments() {
		//Log.d(TAG, "attachFragments()");
		for (Iterator<WeakReference<VideoFragment>> iterator = fragmentReferences.values().iterator(); iterator.hasNext();) {
			Fragment fragment = iterator.next().get();
			if (fragment != null) {
				//Log.d(TAG, "fragment != null");
				/**
				 * Rather than comparing fragment index with currentPosition, we have to compare actual position
				 * sent to fragment via arguments, because otherwise fragment positions returned by fragmentReferences.values()
				 * is not same as their positions in the video list.
				 */
				if (fragment.getArguments().getInt(BundleKeys.POS) == currentPosition) {
					//Log.d(TAG, "currentPosition = " + i);
					/**
					 * After orientation change we need to set BIG_SCALE value for centered fragment
					 * otherwise all views will have SMALL_SCALE which is set by default from getItem().
					 */
					fragment.getArguments().putFloat(BundleKeys.SCALE, BIG_SCALE);
				}
				fm.beginTransaction().attach(fragment).commit();
			}
		}
		fragmentsDetached = false;
	}
}
