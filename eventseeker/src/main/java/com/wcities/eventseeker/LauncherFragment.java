package com.wcities.eventseeker;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.lianghanzhen.endless.viewpager.BannerHandler;
import com.viewpagerindicator.CirclePageIndicator;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.ScreenNames;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.util.FragmentUtil;

public class LauncherFragment extends FragmentLoadableFromBackStack implements OnClickListener, Callback {

	private static final String TAG = LauncherFragment.class.getSimpleName();

	private static final int PAGE_DELAY_TIME = 5000;
	
	private BannerHandler bannerHandler;

	private SurfaceView srfvVideo;
	private SurfaceHolder srfcHldr;
	private MediaPlayer mdPlyr;

	private long[] delay;

	private int videoDuration;

	private ImageView imgProxy;
	
	private static enum PagerTitle {
		firstTitle,
		secondTitle,
		thirdTitle,
		forthTitle;
		
		public String getTitle(Context context) {
			return context.getResources().getStringArray(R.array.pagerTitleArray)[ordinal()];
		}
		
		public String getDescription(Context context) {
			return context.getResources().getStringArray(R.array.pagerTitleDescArray)[ordinal()];
		}
		
	}

	
	@Override
	public void onStart() {
		super.onStart();
		//Log.d(TAG, "onStart()");
		((ActionBarActivity) FragmentUtil.getActivity(this)).getSupportActionBar().hide();

		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		ma.setVStatusBarVisibility(View.GONE, AppConstants.INVALID_ID);
		ma.setDrawerLockMode(true);
		ma.setDrawerIndicatorEnabled(false);
		ma.getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_HOME);
		if (ma.isTabletAndInLandscapeMode()) {
			ma.hideDrawerList();
		}
		/**
		 * Start the video
		 */
		createSurface();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);

		delay = new long[PagerTitle.values().length];
		for (int i = 0; i < delay.length; i++) {
			delay[0] = PAGE_DELAY_TIME;
		}
	}
		
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_launcher, null);
		
		srfvVideo = (SurfaceView) view.findViewById(R.id.srfvVideo);
		imgProxy = (ImageView) view.findViewById(R.id.imgProxy);
		
		view.findViewById(R.id.btnLogin).setOnClickListener(this);
		view.findViewById(R.id.btnSignUp).setOnClickListener(this);

		ActionBarActivity activity = (ActionBarActivity) FragmentUtil.getActivity(this);

		ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewPager);
		viewPager.setAdapter(new TextViewPagerAdapter(activity, getChildFragmentManager()));

		CirclePageIndicator pageIndicator = (CirclePageIndicator) view.findViewById(R.id.pageIndicator);
		pageIndicator.setViewPager(viewPager);
		
        bannerHandler = new BannerHandler(viewPager, delay);
        pageIndicator.setOnPageChangeListener(bannerHandler);
		
		return view;
	}
	
	private void createSurface() {
		srfcHldr = srfvVideo.getHolder();
		srfcHldr.addCallback(this);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		bannerHandler.start();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		bannerHandler.stop();
	}
	
	@Override
	public void onStop() {
		super.onStop();
		//Log.d(TAG, "onStop()");
		((ActionBarActivity) FragmentUtil.getActivity(this)).getSupportActionBar().show();
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		ma.setVStatusBarVisibility(View.VISIBLE, R.color.colorPrimaryDark);
		if (ma.isTabletAndInLandscapeMode()) {
			ma.unHideDrawerList();
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		//Log.d(TAG, "onDestroy()");

		imgProxy.setImageResource(0);

		/**
		 * This is required to unlock drawer after login/sign up process completes, because user is navigated 
		 * to my events or discover screen. At this point first we clear back stack from selectItem()
		 * of MainActivity followed by replacing the fragment. After all these calls in main activity, in the end
		 * onStart(), onStop(), onDestroy() of this fragment are called. Hence to negate the effect of onStart()
		 * above which locks the drawer & indicator, we need to unlock these features again below.
		 */
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		ma.setDrawerLockMode(false);
		
		/**
		 * Order of below 2 lines is important. If it's reversed then in above case mentioned, toolbar shows
		 * back arrow even when it should be hamburger icon.
		 */
		ma.getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP);
		ma.setDrawerIndicatorEnabled(true);
	}
	
	@Override
	public void onClick(View v) {
		
		switch (v.getId()) {
		
		case R.id.btnLogin:
			((MainActivity) FragmentUtil.getActivity(this)).replaceByFragment(AppConstants.FRAGMENT_TAG_LOGIN, null);
			break;

		case R.id.btnSignUp:
			((MainActivity) FragmentUtil.getActivity(this)).replaceByFragment(AppConstants.FRAGMENT_TAG_SIGN_UP, null);			
			break;

		default:
			break;
		}
	}

	@Override
	public String getScreenName() {
		return ScreenNames.LAUNCHER;
	}
	
	private class TextViewPagerAdapter extends FragmentStatePagerAdapter {

		private Context context;

		public TextViewPagerAdapter(Context context, FragmentManager fragmentManager) {
			super(fragmentManager);
			this.context = context;
		}

		@Override
		public Fragment getItem(int position) {
			PagerTitle pagerTitle = PagerTitle.values()[position % PagerTitle.values().length];
			
			Bundle args = new Bundle();
			args.putString(AppConstants.LAUNCHER_FRAGMENT_TITLE, pagerTitle.getTitle(context));
			args.putString(AppConstants.LAUNCHER_FRAGMENT_DESC, pagerTitle.getDescription(context));
			return TextFragment.getInstance(args);
		}

		@Override
		public int getCount() {
			return PagerTitle.values().length;
		}
		
	}
	
	public static class TextFragment extends Fragment {
		
		private String title, desc;
		
		public static TextFragment getInstance(Bundle args) {
			Fragment fragment = new TextFragment();
			fragment.setArguments(args);
			return (TextFragment) fragment;
		}
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			Bundle args = getArguments();
			title = args.getString(AppConstants.LAUNCHER_FRAGMENT_TITLE);
			desc = args.getString(AppConstants.LAUNCHER_FRAGMENT_DESC);
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
			View view = inflater.inflate(R.layout.fragment_text, null);
			((TextView) view.findViewById(R.id.txtTitle)).setText(title);
			((TextView) view.findViewById(R.id.txtDesc)).setText(desc);
			return view;
		}
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		playVideo();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		releaseMediaPlayer();
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {}

	private void playVideo() {
		try {
			if (mdPlyr == null) {
				mdPlyr = MediaPlayer.create(FragmentUtil.getActivity(this).getApplicationContext(), R.raw.event_video);
			}
			/**
			 * the below "mdPlyr != null" check is added as in few phones ex. Samsung Galaxy S, the video wasn't being 
			 * played and 'mdPlyr' instance at this position was coming as null. This is because in above line where 
			 * 'mdPlyr' instance is being created 'IOException' occurs and hence the instance remains to be null.
			 */
			if (mdPlyr != null) {
				if (videoDuration > 0) {
					mdPlyr.seekTo(videoDuration - 1000);
				}
				mdPlyr.setScreenOnWhilePlaying(true);				
				mdPlyr.setDisplay(srfcHldr);
				mdPlyr.setOnPreparedListener(new OnPreparedListener() {
					
					@Override
					public void onPrepared(MediaPlayer mediaplayer) {
						int width = mdPlyr.getVideoWidth();
						int height = mdPlyr.getVideoHeight();
						
						if (width != 0 && height != 0) {
							srfcHldr.setFixedSize(width, height);
							mdPlyr.start();
						
						} else {
							hideVideoViewAndShowBG();
						}
					}
				});
				mdPlyr.setOnErrorListener(new OnErrorListener() {
					
					@Override
					public boolean onError(MediaPlayer mp, int what, int extra) {
						hideVideoViewAndShowBG();
						return false;
					}
				});
				mdPlyr.setOnCompletionListener(new OnCompletionListener() {
					
					@Override
					public void onCompletion(MediaPlayer mp) {
						mdPlyr.start();
					}
				});
				
			} else {
				hideVideoViewAndShowBG();
			}

		} catch (IllegalArgumentException e) {
			e.printStackTrace();
	
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
	}

	protected void hideVideoViewAndShowBG() {
		imgProxy.setImageResource(R.drawable.ic_loading_page_img_bg);
		imgProxy.setVisibility(View.VISIBLE);
		srfvVideo.setVisibility(View.GONE);
	}

	private void releaseMediaPlayer() {
		if (mdPlyr != null) {
			videoDuration = mdPlyr.getCurrentPosition();
			mdPlyr.release();
			mdPlyr = null;
		}
	}
}
