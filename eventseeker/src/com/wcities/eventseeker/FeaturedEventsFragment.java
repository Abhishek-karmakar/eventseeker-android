package com.wcities.eventseeker;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Date;
import com.wcities.eventseeker.core.Event;
import com.wcities.eventseeker.interfaces.EventListener;
import com.wcities.eventseeker.util.FragmentUtil;

public class FeaturedEventsFragment extends Fragment {
	
	private static final String TAG = FeaturedEventsFragment.class.getName();
	
	// Don't use mListener for viewpager children ever due to following bug mentioned under onClick() of onCreateView().
	//private OnDiscoverChildFragmentItemSelectedListener mListener;
    private BitmapCache bitmapCache;
    
	public static final FeaturedEventsFragment newInstance(Event event) {
		FeaturedEventsFragment featuredEventsFragment = new FeaturedEventsFragment();
		Bundle bundle = new Bundle();
		bundle.putSerializable(BundleKeys.EVENT, event);
		featuredEventsFragment.setArguments(bundle);
		return featuredEventsFragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        bitmapCache = BitmapCache.getInstance();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		//Log.d(TAG, "onCreateView()");
		final Event event = (Event) getArguments().getSerializable(BundleKeys.EVENT);
		View v = inflater.inflate(R.layout.fragment_featured_events, container, false);
		
		ImageView imgFeaturedEvt = (ImageView) v.findViewById(R.id.imgFeaturedEvt);
		
		String key = event.getKey(ImgResolution.LOW);
		Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
		if (bitmap != null) {
	        imgFeaturedEvt.setImageBitmap(bitmap);
	        
	    } else {
	    	AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
	        asyncLoadImg.loadImg(imgFeaturedEvt, ImgResolution.LOW, event);
	    }
		
		TextView txtEvtTitle = (TextView)v.findViewById(R.id.txtEvtTitle);
		txtEvtTitle.setText(event.getName());
		
		Date date = event.getSchedule().getDates().get(0);
		
		DateFormat dateFormat = date.isStartTimeAvailable() ? new SimpleDateFormat("MMMM dd, yyyy h:mm a") :
				new SimpleDateFormat("MMMM dd, yyyy");
		TextView txtEvtSchedule = (TextView)v.findViewById(R.id.txtEvtTime);
		txtEvtSchedule.setText(dateFormat.format(date.getStartDate()));
		
		v.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//Log.d(TAG, "onCLick() mListener = " + mListener);
				/**
				 * Here we can't use mListener directly, because for ViewPager, it throws "IllegalStateException: Activity has been destroyed"
				 * in following case.
				 * Change orientation before ViewPager children start loading & then click on any page
				 * resulting in above mentioned exception while committing fragment replacement
				 * from MainActivity due to following call which would have been 
				 * "mListener.onEventSelected(event);" if 
				 * we use mListener. This happens because onAttach() is not called initially for starting 
				 * orientation & called only second time when orientation changes, but this call passes activity's old
				 * reference with onAttach() which can be figured out by logs. And hence using mListener 
				 * will call following function replaceDiscoverFragmentByFragment() on old activity reference which 
				 * should not be the case. So prefer using "(EventListener)FragmentUtil.getActivity(FeaturedEventsFragment.this)"
				 * instead of mListener.
				 */
				((EventListener)FragmentUtil.getActivity(FeaturedEventsFragment.this)).onEventSelected(event);
			}
		});
		
		//Log.d(TAG, "onCreateView() finished");
		return v;
	}
}
