package com.wcities.eventseeker;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wcities.eventseeker.adapter.CatTitlesAdapterTab;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Category;
import com.wcities.eventseeker.util.FragmentUtil;

public class DiscoverFragmentTab extends Fragment {
	
	private static final String TAG = DiscoverFragmentTab.class.getSimpleName();

	private List<Category> evtCategories;
	
	private RecyclerView recyclerVCategories;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		//Log.d(TAG, "onCreateView()");
		View v = inflater.inflate(R.layout.fragment_discover, container, false);
		
		recyclerVCategories = (RecyclerView) v.findViewById(R.id.recyclerVCategories);
		// use a linear layout manager
		RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(FragmentUtil.getActivity(this));
		((LinearLayoutManager)layoutManager).setOrientation(LinearLayoutManager.HORIZONTAL);
		recyclerVCategories.setLayoutManager(layoutManager);
		
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if (evtCategories == null) {
			buildEvtCategories();
			
			CatTitlesAdapterTab catTitlesAdapterTab = new CatTitlesAdapterTab(evtCategories);
			recyclerVCategories.setAdapter(catTitlesAdapterTab);
		}
	}
	
	private void buildEvtCategories() {
		evtCategories = new ArrayList<Category>();
		int categoryIdStart = AppConstants.CATEGORY_ID_START;
		String[] categoryNames = getResources().getStringArray(R.array.evt_category_titles);
		for (int i = 0; i < AppConstants.TOTAL_CATEGORIES; i++) {
			evtCategories.add(new Category(categoryIdStart + i, categoryNames[i]));
		}
	}
}
