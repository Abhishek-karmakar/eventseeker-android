package com.wcities.eventseeker.bosch;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.wcities.eventseeker.R;
import com.wcities.eventseeker.bosch.BoschMainActivity.OnDisplayModeChangedListener;
import com.wcities.eventseeker.bosch.custom.fragment.BoschFragmentLoadableFromBackStack;
import com.wcities.eventseeker.bosch.interfaces.BoschEditTextListener;
import com.wcities.eventseeker.constants.AppConstants;
import com.wcities.eventseeker.constants.BundleKeys;
import com.wcities.eventseeker.util.FragmentUtil;

public class BoschSearchFragment extends BoschFragmentLoadableFromBackStack implements OnClickListener, 
		OnDisplayModeChangedListener, BoschEditTextListener {

	private static final String TAG = BoschSearchFragment.class.getName();
	
	private EditText edtSearch;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_bosch_search, null);
		
		view.findViewById(R.id.btnSearch).setOnClickListener(this);
		edtSearch = (EditText) view.findViewById(R.id.edtSearch);
		edtSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_SEARCH) {
					onSearchClicked();
		            return true;
		        }
				return false;
			}
		});
		
		updateColors();
		return view;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		((BoschMainActivity) FragmentUtil.getActivity(this)).onFragmentResumed(this, 
			BoschMainActivity.INDEX_NAV_ITEM_SEARCH, getResources().getString(R.string.title_search));
	}
	
	@Override
	public void onClick(View v) {
		
		switch (v.getId()) {

			case R.id.btnSearch:
				onSearchClicked();
				break;
		}
	}
		
	private void onSearchClicked() {
			
		String query = edtSearch.getText().toString().trim();
		query = query.replace("\\n", "");
			
		if(query.equals("")) {
			return;
		}

		Bundle args = new Bundle();
		args.putString(BundleKeys.QUERY, query);
			
		((BoschMainActivity) FragmentUtil.getActivity(this))
			.replaceByFragment(BoschSearchResultFragment.class.getSimpleName(), args);

	}

	@Override
	public void onDisplayModeChanged(boolean isNightModeEnabled) {
		updateColors();		
	}

	private void updateColors() {
		if (AppConstants.IS_NIGHT_MODE_ENABLED) {
			edtSearch.setBackgroundResource(R.drawable.bg_edt_search_night_mode);
			edtSearch.setTextColor(getResources().getColor(android.R.color.white));
			edtSearch.setHintTextColor(getResources().getColor(android.R.color.white));
		} else {
			edtSearch.setBackgroundResource(R.drawable.bg_edt_search);
			edtSearch.setTextColor(getResources().getColor(R.color.eventseeker_bosch_theme_grey));			
			edtSearch.setHintTextColor(getResources().getColor(R.color.eventseeker_bosch_theme_grey));			
		}		
	}

	@Override
	public EditText getEditText() {
		return edtSearch;
	}
}