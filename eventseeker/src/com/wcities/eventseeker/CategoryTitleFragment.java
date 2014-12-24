package com.wcities.eventseeker;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wcities.eventseeker.adapter.CatTitlesAdapter;
import com.wcities.eventseeker.custom.view.CategoryTitleLinearLayout;

public class CategoryTitleFragment extends Fragment {
	
	public static final CategoryTitleFragment newInstance(int pos, float scale) {
		CategoryTitleFragment categoryTitleFragment = new CategoryTitleFragment();
		Bundle b = new Bundle();
		b.putInt("pos", pos);
		b.putFloat("scale", scale);
		categoryTitleFragment.setArguments(b);
		return categoryTitleFragment;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (container == null) {
			return null;
		}
		
		LinearLayout l = (LinearLayout) inflater.inflate(R.layout.cat_title, container, false);
		
		int pos = this.getArguments().getInt("pos");
		TextView txtTitle = (TextView) l.findViewById(R.id.txtTitle);
		txtTitle.setText("POSITION" + pos);
		
		CategoryTitleLinearLayout lnrLytRoot = (CategoryTitleLinearLayout) l.findViewById(R.id.lnrLytRoot);
		float scale = this.getArguments().getFloat("scale");
		if (scale == CatTitlesAdapter.BIG_SCALE) {
			lnrLytRoot.findViewById(R.id.vHorLine).setVisibility(View.VISIBLE);
		}
		lnrLytRoot.setScaleBoth(scale);
		
		return l;
	}
}
