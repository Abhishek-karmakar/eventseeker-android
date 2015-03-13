package com.wcities.eventseeker.adapter;

import java.util.List;

import android.graphics.Color;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.wcities.eventseeker.DiscoverFragmentTab;
import com.wcities.eventseeker.R;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.core.Category;
import com.wcities.eventseeker.util.FragmentUtil;

public class RVCatTitlesAdapterTab extends Adapter<RVCatTitlesAdapterTab.ViewHolder> {
	
	private static final String TAG = RVCatTitlesAdapterTab.class.getSimpleName();
	
	private static final int LOOPS = 1000;
	public final static int FIRST_PAGE = AppConstants.TOTAL_CATEGORIES * LOOPS / 2;

	private List<Category> evtCategories;
	private int selectedPos = FIRST_PAGE;
	private int selectedCatId;
	
	private DiscoverFragmentTab discoverFragmentTab;
	
	public RVCatTitlesAdapterTab(List<Category> evtCategories, DiscoverFragmentTab discoverFragmentTab) {
		this.evtCategories = evtCategories;
		this.discoverFragmentTab = discoverFragmentTab;
	}

	static class ViewHolder extends RecyclerView.ViewHolder {
		
		private TextView txtTitle;
		private View vHorLine;

		public ViewHolder(View itemView) {
			super(itemView);
			this.txtTitle = (TextView) itemView.findViewById(R.id.txtTitle);
			this.vHorLine = itemView.findViewById(R.id.vHorLine);
		}
	}

	@Override
	public int getItemCount() {
		return AppConstants.TOTAL_CATEGORIES * LOOPS;
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.cat_title, parent, false);
		return new ViewHolder(v);
	}

	@Override
	public void onBindViewHolder(RVCatTitlesAdapterTab.ViewHolder holder, final int pos) {
		//Log.d(TAG, "onBindViewHolder(), pos = " + pos);
		holder.txtTitle.setText(evtCategories.get(pos % AppConstants.TOTAL_CATEGORIES).getName());
		if (pos == selectedPos) {
			holder.txtTitle.setTextColor(FragmentUtil.getResources(discoverFragmentTab).getColor(R.color.colorPrimary));
			holder.vHorLine.setVisibility(View.VISIBLE);
			
		} else {
			holder.txtTitle.setTextColor(Color.GRAY);
			holder.vHorLine.setVisibility(View.GONE);
		}
		
		holder.itemView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (selectedPos != pos) {
					discoverFragmentTab.onCatTitleClicked(pos);
				}
			}
		});
	}

	public void setSelectedPos(int selectedPos) {
		//Log.d(TAG, "setSelectedPos() - " + selectedPos);
		if (this.selectedPos != selectedPos) {
			int catIndex = selectedPos % AppConstants.TOTAL_CATEGORIES;
			selectedCatId = evtCategories.get(catIndex).getId();
			
			notifyItemChanged(this.selectedPos);
			this.selectedPos = selectedPos;
			notifyItemChanged(this.selectedPos);
			
			/**
			 * not calling onCatChanged() from here, because this setSelectedPos() function is called number 
			 * of times even when scroll is in progress, whereas we need to reset eventList only if 
			 * scroll is completed.
			 */
			//discoverFragmentTab.onCatChanged();
		}
	}

	public int getSelectedPos() {
		return selectedPos;
	}

	public int getSelectedCatId() {
		return selectedCatId;
	}
}
