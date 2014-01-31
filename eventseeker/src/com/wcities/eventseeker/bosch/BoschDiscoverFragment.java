package com.wcities.eventseeker.bosch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.app.EventSeekr;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.core.Category;
import com.wcities.eventseeker.custom.fragment.FragmentLoadableFromBackStack;
import com.wcities.eventseeker.interfaces.ReplaceFragmentListener;
import com.wcities.eventseeker.util.FragmentUtil;
import com.wcities.eventseeker.util.GeoUtil;
import com.wcities.eventseeker.util.GeoUtil.GeoUtilListener;

public class BoschDiscoverFragment extends FragmentLoadableFromBackStack implements OnClickListener, 
		GeoUtilListener {
	
	private static final String TAG = BoschDiscoverFragment.class.getSimpleName();

	private GridView grdEvtCategories;
	
	private List<Category> evtCategories;
	private EvtCategoriesGridAdapter evtCategoriesGridAdapter;
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (!(activity instanceof ReplaceFragmentListener)) {
			throw new ClassCastException(activity.toString() + " must implement ReplaceFragmentListener");
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		buildEvtCategories();
		
		View view = inflater.inflate(R.layout.fragment_bosch_discover, null);
		grdEvtCategories = (GridView) view.findViewById(R.id.grdEvtCategories);
		if (evtCategoriesGridAdapter == null) {
			evtCategoriesGridAdapter = new EvtCategoriesGridAdapter();
		}
		grdEvtCategories.setAdapter(evtCategoriesGridAdapter);
		
		view.findViewById(R.id.btnUp).setOnClickListener(this);
		view.findViewById(R.id.btnDown).setOnClickListener(this);
		
		return view;
	}
	
	@Override
	public void onResume() {
		String cityName = GeoUtil.getCityName(this, (EventSeekr) FragmentUtil.getActivity(this).getApplication());
		super.onResume(AppConstants.INVALID_INDEX, buildTitle(cityName));
	}
	
	private String buildTitle(String cityName) {
		return (cityName == null) ? "Discover" : cityName;
	}
	
	private void buildEvtCategories() {
		evtCategories = new ArrayList<Category>();
		int categoryIdStart = AppConstants.CATEGORY_ID_START;
		String[] categoryNames = new String[] { "Concerts", "Theater", "Sport Events", "Arts & Museums", 
				"Dance", "Clubbing & Nightlife", "Educational", "Festivals & Fairs", "Family", "Community", 
				"Business & Tech", "Tours" };
		for (int i = 0; i < AppConstants.TOTAL_CATEGORIES; i++) {
			evtCategories.add(new Category(categoryIdStart + i, categoryNames[i]));
		}
	}
	
	private class EvtCategoriesGridAdapter extends BaseAdapter {

		private final HashMap<Integer, Integer> categoryImgs = new HashMap<Integer, Integer>() {
			{
				put(AppConstants.CATEGORY_ID_START, R.drawable.cat_900);
				put(AppConstants.CATEGORY_ID_START + 1, R.drawable.cat_901);
				put(AppConstants.CATEGORY_ID_START + 2, R.drawable.cat_902);
				put(AppConstants.CATEGORY_ID_START + 3, R.drawable.cat_903);
				put(AppConstants.CATEGORY_ID_START + 4, R.drawable.cat_904);
				put(AppConstants.CATEGORY_ID_START + 5, R.drawable.cat_905);
				put(AppConstants.CATEGORY_ID_START + 6, R.drawable.cat_906);
				put(AppConstants.CATEGORY_ID_START + 7, R.drawable.cat_907);
				put(AppConstants.CATEGORY_ID_START + 8, R.drawable.cat_908);
				put(AppConstants.CATEGORY_ID_START + 9, R.drawable.cat_909);
				put(AppConstants.CATEGORY_ID_START + 10, R.drawable.cat_910);
				put(AppConstants.CATEGORY_ID_START + 11, R.drawable.cat_911);
			}
		};

		@Override
		public int getCount() {
			return evtCategories.size();
		}

		@Override
		public Object getItem(int position) {
			return evtCategories.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			GridItemCategoryHolder holder;
			if (convertView == null) {
				convertView = LayoutInflater.from(FragmentUtil.getActivity(BoschDiscoverFragment.this))
						.inflate(R.layout.bosch_grid_category, null);

				holder = new GridItemCategoryHolder();
				holder.imgCategory = (ImageView) convertView.findViewById(R.id.imgCategory);
				holder.txtCategory = (TextView) convertView.findViewById(R.id.txtCategory);

				convertView.setTag(holder);

			} else {
				holder = (GridItemCategoryHolder) convertView.getTag();
			}

			final Category category = evtCategories.get(position);
			holder.txtCategory.setText(category.getName());
			int catId = category.getId();
			Drawable drawable = (categoryImgs.containsKey(catId)) ? getResources().getDrawable(categoryImgs.get(catId)) : null;
			holder.imgCategory.setImageDrawable(drawable);
			convertView.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Bundle args = new Bundle();
					args.putSerializable(BundleKeys.CATEGORY, category);
					((ReplaceFragmentListener)FragmentUtil.getActivity(BoschDiscoverFragment.this))
						.replaceByFragment(BoschDiscoverByCategoryFragment.class.getSimpleName(), args);
				}
			});
			return convertView;
		}

		private class GridItemCategoryHolder {
			private TextView txtCategory;
			private ImageView imgCategory;
		}
	}

	@Override
	public void onClick(View v) {
		
		switch (v.getId()) {
		
		case R.id.btnUp:
			if (android.os.Build.VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB) {
				int offset = getResources().getDimensionPixelSize(R.dimen.b_grd_evt_categories_pad_fragment_bosch_discover);
				grdEvtCategories.smoothScrollToPositionFromTop(grdEvtCategories.getFirstVisiblePosition() - 3, offset);
			    
			} else {
				grdEvtCategories.setSelection(grdEvtCategories.getFirstVisiblePosition() - 3);
			}
			break;
			
		case R.id.btnDown:
			if (android.os.Build.VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB) {
				int offset = getResources().getDimensionPixelSize(R.dimen.b_grd_evt_categories_pad_fragment_bosch_discover);
				grdEvtCategories.smoothScrollToPositionFromTop(grdEvtCategories.getFirstVisiblePosition() + 3, offset);
			    
			} else {
				grdEvtCategories.setSelection(grdEvtCategories.getFirstVisiblePosition() + 3);
			}
			break;

		default:
			break;
		}
	}
	
	@Override
	public void onAddressSearchCompleted(String strAddress) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onCitySearchCompleted(String city) {
		if (city != null && city.length() != 0) {
			((BoschMainActivity)FragmentUtil.getActivity(this)).updateTitleForFragment(buildTitle(city), 
					getClass().getSimpleName());
		}
	}

	@Override
	public void onLatlngSearchCompleted(Address address) {
		// TODO Auto-generated method stub
	}
}
