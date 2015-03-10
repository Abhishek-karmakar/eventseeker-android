package com.wcities.eventseeker.adapter;

import java.util.List;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Category;

public class CatTitlesAdapterTab extends Adapter<CatTitlesAdapterTab.ViewHolder> {
	
	private static final String TAG = CatTitlesAdapterTab.class.getSimpleName();
	
	private static final int LOOPS = 1000;

	private List<Category> evtCategories;
	
	public CatTitlesAdapterTab(List<Category> evtCategories) {
		this.evtCategories = evtCategories;
	}

	static class ViewHolder extends RecyclerView.ViewHolder {
		
		private TextView txtTitle;

		public ViewHolder(View itemView) {
			super(itemView);
			this.txtTitle = (TextView) itemView.findViewById(R.id.txtTitle);
		}
	}

	@Override
	public int getItemCount() {
		return AppConstants.TOTAL_CATEGORIES * LOOPS;
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.cat_title, parent, false);
		ViewHolder vh = new ViewHolder(v);
		return vh;
	}

	@Override
	public void onBindViewHolder(CatTitlesAdapterTab.ViewHolder holder, int pos) {
		holder.txtTitle.setText(evtCategories.get(pos % AppConstants.TOTAL_CATEGORIES).getName());
	}
}
