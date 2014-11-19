package com.wcities.eventseeker;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.viewpagerindicator.CirclePageIndicator;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.util.FragmentUtil;

public class LauncherFragment extends FragmentLoadableFromBackStack implements OnClickListener {

	private static final String TAG = LauncherFragment.class.getSimpleName();
	
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
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_launcher, null);
		
		view.findViewById(R.id.btnLogin).setOnClickListener(this);
		view.findViewById(R.id.btnSignUp).setOnClickListener(this);

		ActionBarActivity activity = (ActionBarActivity) FragmentUtil.getActivity(this);

		ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewPager);
		viewPager.setAdapter(new TextViewPagerAdapter(activity, activity.getSupportFragmentManager()));

		CirclePageIndicator pageIndicator = (CirclePageIndicator) view.findViewById(R.id.pageIndicator);
		pageIndicator.setViewPager(viewPager);
		
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");
		((ActionBarActivity) FragmentUtil.getActivity(this)).getSupportActionBar().hide();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		Log.d(TAG, "onPause");
		((ActionBarActivity) FragmentUtil.getActivity(this)).getSupportActionBar().show();
	}
	
	@Override
	public void onClick(View v) {
		
		switch (v.getId()) {
		
		case R.id.btnLogin:
			((MainActivity) FragmentUtil.getActivity(this)).replaceByFragment(AppConstants.FRAGMENT_TAG_GET_STARTED, null);
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
			PagerTitle pagerTitle = PagerTitle.values()[position];
			
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
	/*private class TextViewPagerAdapter extends PagerAdapter {

		private Context context;

		public TextViewPagerAdapter(Context context) {
			this.context = context;
		}
		
		@Override
		public int getCount() {
			return PagerTitle.values().length;
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			return super.instantiateItem(container, position);
		}
		
		@Override
		public boolean isViewFromObject(View view, Object obj) {
			return view == obj;
		}
		
	}*/
	
}
