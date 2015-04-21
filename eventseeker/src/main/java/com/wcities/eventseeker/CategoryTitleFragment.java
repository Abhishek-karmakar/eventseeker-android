package com.wcities.eventseeker;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wcities.eventseeker.adapter.CatTitlesAdapter;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.custom.view.CategoryTitleLinearLayout;

public class CategoryTitleFragment extends Fragment {
	
	protected static final String TAG = CategoryTitleFragment.class.getSimpleName();

	public static final CategoryTitleFragment newInstance(String category, float scale, int pos) {
		//Log.d(TAG, "newInstance()");
		CategoryTitleFragment categoryTitleFragment = new CategoryTitleFragment();
		Bundle b = new Bundle();
		b.putString(BundleKeys.CATEGORY, category);
		b.putFloat(BundleKeys.SCALE, scale);
		b.putInt(BundleKeys.POS, pos);
		categoryTitleFragment.setArguments(b);
		return categoryTitleFragment;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		LinearLayout l = (LinearLayout) inflater.inflate(R.layout.cat_title, container, false);
		
		String category = getArguments().getString(BundleKeys.CATEGORY);
		((TextView) l.findViewById(R.id.txtTitle)).setText(category);
		
		CategoryTitleLinearLayout lnrLytRoot = (CategoryTitleLinearLayout) l.findViewById(R.id.lnrLytRoot);
		
		float scale = getArguments().getFloat(BundleKeys.SCALE);
		if (scale == CatTitlesAdapter.BIG_SCALE) {
			lnrLytRoot.findViewById(R.id.vHorLine).setVisibility(View.VISIBLE);
		}
		lnrLytRoot.setScaleBoth(scale);
		
		l.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				((DiscoverFragment)getParentFragment()).onCatTitleClicked(getArguments().getInt(BundleKeys.POS));
			}
		});
		
		return l;
	}
}
