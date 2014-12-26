package com.wcities.eventseeker;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wcities.eventseeker.adapter.CatTitlesAdapter;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.custom.view.CategoryTitleLinearLayout;

public class CategoryTitleFragment extends Fragment {
	
	protected static final String TAG = CategoryTitleFragment.class.getSimpleName();

	public static final CategoryTitleFragment newInstance(String category, float scale) {
		//Log.d(TAG, "newInstance()");
		CategoryTitleFragment categoryTitleFragment = new CategoryTitleFragment();
		Bundle b = new Bundle();
		b.putString(BundleKeys.CATEGORY, category);
		b.putFloat(BundleKeys.SCALE, scale);
		categoryTitleFragment.setArguments(b);
		return categoryTitleFragment;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		LinearLayout l = (LinearLayout) inflater.inflate(R.layout.cat_title, container, false);
		
		String category = getArguments().getString(BundleKeys.CATEGORY);
		((TextView) l.findViewById(R.id.txtTitle)).setText(category);
		
		CategoryTitleLinearLayout lnrLytRoot = (CategoryTitleLinearLayout) l.findViewById(R.id.lnrLytRoot);
		
		float scale = this.getArguments().getFloat(BundleKeys.SCALE);
		if (scale == CatTitlesAdapter.BIG_SCALE) {
			lnrLytRoot.findViewById(R.id.vHorLine).setVisibility(View.VISIBLE);
		}
		lnrLytRoot.setScaleBoth(scale);
		
		return l;
	}
}
