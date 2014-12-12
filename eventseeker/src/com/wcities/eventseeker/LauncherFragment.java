package com.wcities.eventseeker;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.lianghanzhen.endless.viewpager.BannerHandler;
import com.viewpagerindicator.CirclePageIndicator;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.util.FragmentUtil;

public class LauncherFragment extends FragmentLoadableFromBackStack implements OnClickListener {

	private static final String TAG = LauncherFragment.class.getSimpleName();
	
	private BannerHandler bannerHandler;
	
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
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		((ActionBarActivity) FragmentUtil.getActivity(this)).getSupportActionBar().hide();

		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		ma.setDrawerLockMode(true);
		ma.setDrawerIndicatorEnabled(false);
		ma.getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_HOME);
		if (ma.isTabletAndInLandscapeMode()) {
			ma.hideDrawerList();
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_launcher, null);
		
		view.findViewById(R.id.btnLogin).setOnClickListener(this);
		view.findViewById(R.id.btnSignUp).setOnClickListener(this);

		ActionBarActivity activity = (ActionBarActivity) FragmentUtil.getActivity(this);

		ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewPager);
		viewPager.setAdapter(new TextViewPagerAdapter(activity, getChildFragmentManager()));

		CirclePageIndicator pageIndicator = (CirclePageIndicator) view.findViewById(R.id.pageIndicator);
		pageIndicator.setViewPager(viewPager);
		
        bannerHandler = new BannerHandler(viewPager);
        pageIndicator.setOnPageChangeListener(bannerHandler);
		
		return view;
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
		((ActionBarActivity) FragmentUtil.getActivity(this)).getSupportActionBar().show();
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		if (ma.isTabletAndInLandscapeMode()) {
			ma.unHideDrawerList();
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		//Log.d(TAG, "onDestroy()");
		/**
		 * This is required to unlock drawer after login/sign up process completes, because user is navigated 
		 * to my events or discover screen. At this point first we clear back stack from selectItem()
		 * of MainActivity followed by replacing the fragment. After all these calls in main activity, in the end
		 * onStart(), onStop(), onDestroy() of this fragment are called. Hence to negate the effect of onStart()
		 * above which locks the drawer & indicator, we need to unlock these features again below.
		 */
		MainActivity ma = (MainActivity) FragmentUtil.getActivity(this);
		ma.setDrawerLockMode(false);
		ma.setDrawerIndicatorEnabled(true);
		ma.getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_HOME
				| ActionBar.DISPLAY_HOME_AS_UP);
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
		return "First Start Screen";
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
}
