package com.wcities.eventseeker;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.Session;
import com.facebook.SessionState;
import com.wcities.eventseeker.adapter.RVArtistDetailsAdapterTab;
import com.wcities.eventseeker.api.Api;
import com.wcities.eventseeker.asynctask.AsyncLoadImg;
import com.wcities.eventseeker.asynctask.LoadArtistDetails;
import com.wcities.eventseeker.asynctask.LoadArtistDetails.OnArtistUpdatedListener;
import com.wcities.eventseeker.cache.BitmapCache;
import com.wcities.eventseeker.cache.BitmapCacheable.ImgResolution;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Artist;
import com.wcities.eventseeker.custom.fragment.PublishEventFragmentRetainingChildFragmentManager;
import com.wcities.eventseeker.util.AsyncTaskUtil;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.VersionUtil;

public class ArtistDetailsFragmentTab extends PublishEventFragmentRetainingChildFragmentManager implements OnArtistUpdatedListener {
	
	private static final String TAG = ArtistDetailsFragmentTab.class.getSimpleName();
	
	private static final int UNSCROLLED = -1;
	
	private String title = "";
	
	private int totalScrolledDy = UNSCROLLED; // indicates layout not yet created
	private int actionBarElevation, limitScrollAt;
	
	private Artist artist;
	private boolean allDetailsLoaded;
	
	private ImageView imgArtist;
	private TextView txtArtistTitle;
	private RelativeLayout rltLytTxtArtistTitle;
	private View vNoContentBG;
	private RecyclerView recyclerVArtists;
	
	private RVArtistDetailsAdapterTab rvArtistDetailsAdapterTab;
	
	private Handler handler;
	
	private LoadArtistDetails loadArtistDetails;
	
	private OnGlobalLayoutListener onGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
        	//Log.d(TAG, "onGlobalLayout()");
			if (VersionUtil.isApiLevelAbove15()) {
				recyclerVArtists.getViewTreeObserver().removeOnGlobalLayoutListener(this);

			} else {
				recyclerVArtists.getViewTreeObserver().removeGlobalOnLayoutListener(this);
			}
			
			onScrolled(0, true, true);
        }
    };
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		
		actionBarElevation = FragmentUtil.getResources(this).getDimensionPixelSize(R.dimen.action_bar_elevation);
		
		if (artist == null) {
			//Log.d(TAG, "event = null");
			artist = (Artist) getArguments().getSerializable(BundleKeys.ARTIST);
			artist.getVideos().clear();
			artist.getFriends().clear();
			
			loadArtistDetails = new LoadArtistDetails(Api.OAUTH_TOKEN, artist, this, this);
			AsyncTaskUtil.executeAsyncTask(loadArtistDetails, true);
		}
		
		handler = new Handler(Looper.getMainLooper());
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_artist_details_tab, container, false);
		
		imgArtist = (ImageView) rootView.findViewById(R.id.imgArtist);
		updateArtistImg();
		if (getArguments().containsKey(BundleKeys.TRANSITION_NAME_SHARED_IMAGE)) {
			ViewCompat.setTransitionName(imgArtist, getArguments().getString(BundleKeys.TRANSITION_NAME_SHARED_IMAGE));
		}
		
		rltLytTxtArtistTitle = (RelativeLayout) rootView.findViewById(R.id.rltLytTxtArtistTitle);
		
		txtArtistTitle = (TextView) rootView.findViewById(R.id.txtArtistTitle);
		txtArtistTitle.setText(artist.getName());
		// for marquee to work
		txtArtistTitle.setSelected(true);
		ViewCompat.setTransitionName(txtArtistTitle, getArguments().getString(BundleKeys.TRANSITION_NAME_SHARED_TEXT));
		
		vNoContentBG = rootView.findViewById(R.id.vNoContentBG);
		
		recyclerVArtists = (RecyclerView) rootView.findViewById(R.id.recyclerVArtists);
		// use a linear layout manager
		RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(FragmentUtil.getActivity(this));
		recyclerVArtists.setLayoutManager(layoutManager);
		
		recyclerVArtists.setOnScrollListener(new RecyclerView.OnScrollListener() {
	    	
	    	@Override
	    	public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
	    		super.onScrolled(recyclerView, dx, dy);
	    		//Log.d(TAG, "onScrolled - dx = " + dx + ", dy = " + dy);
	    		ArtistDetailsFragmentTab.this.onScrolled(dy, false, false);
	    	}
		});
		
		recyclerVArtists.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
		
		return rootView;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (rvArtistDetailsAdapterTab == null) {
			//eventList = new ArrayList<Event>();
			/**
			 * 16-02-2015
			 * This check is added as per discussion with Amir Sir, the upcoming events should be queried
			 * only if Artist is on Tour.
			 */
			/*if (artist.isOntour()) {
				eventList.add(null);
			}*/
			rvArtistDetailsAdapterTab = new RVArtistDetailsAdapterTab(this);
			
		} else {
			rvArtistDetailsAdapterTab.detachFragments();
		}
		
		recyclerVArtists.setAdapter(rvArtistDetailsAdapterTab);
	}
	
	public void onScrolled(int dy, boolean forceUpdate, boolean chkForOpenDrawer) {
		
	}

	public String getTitle() {
		return title;
	}
	
	public boolean isAllDetailsLoaded() {
		return allDetailsLoaded;
	}
	
	public void setVNoContentBgVisibility(int visibility) {
		vNoContentBG.setVisibility(visibility);
	}

	public Artist getArtist() {
		return artist;
	}
	
	public Handler getHandler() {
		return handler;
	}

	private void updateArtistImg() {
		//Log.d(TAG, "updateEventImg(), url = " + event.getLowResImgUrl());
		if (artist.doesValidImgUrlExist()) {
			String key = artist.getKey(ImgResolution.LOW);
	        BitmapCache bitmapCache = BitmapCache.getInstance();
			Bitmap bitmap = bitmapCache.getBitmapFromMemCache(key);
			if (bitmap != null) {
		        imgArtist.setImageBitmap(bitmap);
		        
		    } else {
		    	imgArtist.setImageBitmap(null);
		    	AsyncLoadImg asyncLoadImg = AsyncLoadImg.getInstance();
		        asyncLoadImg.loadImg(imgArtist, ImgResolution.LOW, artist);
		    }
		}
	}

	@Override
	public void onArtistUpdated() {
		allDetailsLoaded = true;
		updateArtistImg();
		/*fabSave.setSelected(artist.getAttending() == Artist.Attending.Tracked);
		fabSave.setVisibility(View.VISIBLE);
		
		fabArtistNews.setVisibility(View.VISIBLE);
		
		if (artist.isOntour() && eventList != null && eventList.isEmpty()) {
			eventList.add(null);
		}*/
		rvArtistDetailsAdapterTab.notifyDataSetChanged();
	}

	@Override
	public void onPublishPermissionGranted() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void call(Session session, SessionState state, Exception exception) {
		// TODO Auto-generated method stub
		
	}
}
